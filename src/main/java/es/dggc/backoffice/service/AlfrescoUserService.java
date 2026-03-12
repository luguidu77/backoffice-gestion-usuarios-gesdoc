package es.dggc.backoffice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.dggc.backoffice.config.AlfrescoProperties;
import es.dggc.backoffice.model.dto.AlfrescoPersonResponse;
import es.dggc.backoffice.model.dto.AlfrescoPeopleListResponse;
import es.dggc.backoffice.model.dto.AlfrescoSiteMembersListResponse;
import es.dggc.backoffice.model.dto.AlfrescoGroupListResponse;
import es.dggc.backoffice.model.dto.AlfrescoUserSiteListResponse;
import es.dggc.backoffice.model.dto.UserListResponse;
import es.dggc.backoffice.model.dto.GroupListResponse;
import es.dggc.backoffice.model.dto.GroupListResponse.GroupDto;
import es.dggc.backoffice.model.dto.UserListResponse.UserDto;
import es.dggc.backoffice.model.dto.UserSiteMembershipListResponse;
import es.dggc.backoffice.model.dto.UserSiteMembershipListResponse.UserSiteMembershipDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de usuarios en Alfresco.
 * 
 * Utiliza la API REST v1 de Alfresco para listar usuarios.
 */
@Service
public class AlfrescoUserService {

    private static final Logger log = LoggerFactory.getLogger(AlfrescoUserService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter PROOF_TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final RestTemplate restTemplate;
    private final AlfrescoProperties alfrescoProperties;

    public AlfrescoUserService(RestTemplate restTemplate, AlfrescoProperties alfrescoProperties) {
        this.restTemplate = restTemplate;
        this.alfrescoProperties = alfrescoProperties;
    }

    /**
     * Obtiene la lista de usuarios de Alfresco.
     * 
     * @param basicAuthToken Token de autenticación Basic Auth (Base64 de
     *                       username:password)
     * @param maxItems       Número máximo de usuarios a obtener (por defecto 100)
     * @param skipCount      Número de usuarios a omitir (para paginación)
     * @return UserListResponse con la lista de usuarios
     */
    public UserListResponse listUsers(String basicAuthToken, Integer maxItems, Integer skipCount) {
        try {
            log.info("Obteniendo lista de usuarios de Alfresco (maxItems={}, skipCount={})", maxItems, skipCount);

            // Construir URL con parámetros de paginación
            String url = alfrescoProperties.getBaseUrl() +
                    "/api/-default-/public/alfresco/versions/1/people";

            if (maxItems != null || skipCount != null) {
                url += "?";
                if (maxItems != null) {
                    url += "maxItems=" + maxItems;
                }
                if (skipCount != null) {
                    if (maxItems != null)
                        url += "&";
                    url += "skipCount=" + skipCount;
                }
            }

            // Crear headers con Basic Auth
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuthToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Hacer petición GET
            ResponseEntity<AlfrescoPeopleListResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    AlfrescoPeopleListResponse.class);

            AlfrescoPeopleListResponse alfrescoResponse = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && alfrescoResponse != null) {

                // Convertir respuesta de Alfresco a nuestro DTO simplificado
                List<UserDto> users = new ArrayList<>();

                if (alfrescoResponse.getList() != null && alfrescoResponse.getList().getEntries() != null) {
                    users = alfrescoResponse.getList().getEntries().stream()
                            .filter(wrapper -> wrapper.getEntry() != null)
                            .map(wrapper -> {
                                AlfrescoPersonResponse.PersonEntry person = wrapper.getEntry();
                                return new UserDto(
                                        person.getId(),
                                        person.getFirstName(),
                                        person.getLastName(),
                                        person.getEmail(),
                                        person.getEnabled());
                            })
                            .collect(Collectors.toList());
                }

                Integer totalItems = alfrescoResponse.getList() != null &&
                        alfrescoResponse.getList().getPagination() != null
                                ? alfrescoResponse.getList().getPagination().getTotalItems()
                                : users.size();

                Boolean hasMore = alfrescoResponse.getList() != null &&
                        alfrescoResponse.getList().getPagination() != null
                                ? alfrescoResponse.getList().getPagination().getHasMoreItems()
                                : false;

                log.info("Usuarios obtenidos exitosamente: {} de {} totales", users.size(), totalItems);
                return new UserListResponse(users, totalItems, hasMore);
            }

            log.warn("No se pudo obtener la lista de usuarios. Status: {}", response.getStatusCode());
            return new UserListResponse(new ArrayList<>(), 0, false);

        } catch (HttpClientErrorException e) {
            // Propagar 401/403 al controlador para que el frontend reciba el error correcto
            if (e.getStatusCode() == HttpStatus.FORBIDDEN
                    || e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw e;
            }
            log.error("Error HTTP al obtener usuarios: {} - {}", e.getStatusCode(), e.getMessage());
            return new UserListResponse(new ArrayList<>(), 0, false);
        } catch (Exception e) {
            log.error("Error inesperado al obtener usuarios: {}", e.getMessage(), e);
            return new UserListResponse(new ArrayList<>(), 0, false);
        }
    }

    /**
     * Obtiene la lista de usuarios con valores por defecto de paginación.
     * 
     * @param basicAuthToken Token de autenticación Basic Auth
     * @return UserListResponse con la lista de usuarios (máximo 100)
     */
    public UserListResponse listUsers(String basicAuthToken) {
        return listUsers(basicAuthToken, 100, 0);
    }

    /**
     * Obtiene la lista de miembros de un sitio específico.
     */
    public UserListResponse listSiteMembers(String basicAuthToken, String siteId, Integer maxItems, Integer skipCount) {
        try {
            log.info("Obteniendo miembros del sitio {} de Alfresco (maxItems={}, skipCount={})", siteId, maxItems,
                    skipCount);

            String url = alfrescoProperties.getBaseUrl() +
                    "/api/-default-/public/alfresco/versions/1/sites/" + siteId + "/members";

            url += "?include=person"; // Importante para obtener los detalles del usuario
            if (maxItems != null)
                url += "&maxItems=" + maxItems;
            if (skipCount != null)
                url += "&skipCount=" + skipCount;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuthToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<AlfrescoSiteMembersListResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, AlfrescoSiteMembersListResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                AlfrescoSiteMembersListResponse alfrescoResponse = response.getBody();
                List<UserDto> users = new ArrayList<>();

                if (alfrescoResponse != null && alfrescoResponse.getList() != null
                        && alfrescoResponse.getList().getEntries() != null) {
                    users = alfrescoResponse.getList().getEntries().stream()
                            .filter(wrapper -> wrapper.getEntry() != null && wrapper.getEntry().getPerson() != null)
                            .map(wrapper -> {
                                AlfrescoPersonResponse.PersonEntry person = wrapper.getEntry().getPerson();
                                return new UserDto(
                                        person.getId(),
                                        person.getFirstName(),
                                        person.getLastName(),
                                        person.getEmail(),
                                        person.getEnabled());
                            })
                            .collect(Collectors.toList());
                }

                Integer totalItems = (alfrescoResponse != null && alfrescoResponse.getList() != null &&
                        alfrescoResponse.getList().getPagination() != null)
                                ? alfrescoResponse.getList().getPagination().getTotalItems()
                                : users.size();

                Boolean hasMore = (alfrescoResponse != null && alfrescoResponse.getList() != null &&
                        alfrescoResponse.getList().getPagination() != null)
                                ? alfrescoResponse.getList().getPagination().getHasMoreItems()
                                : false;

                return new UserListResponse(users, totalItems, hasMore);
            }
            return new UserListResponse(new ArrayList<>(), 0, false);
        } catch (Exception e) {
            log.error("Error al obtener miembros del sitio {}: {}", siteId, e.getMessage());
            return new UserListResponse(new ArrayList<>(), 0, false);
        }
    }

    /**
     * Busca usuarios por un término.
     */
    public UserListResponse searchUsers(String basicAuthToken, String term, Integer maxItems, Integer skipCount) {
        try {
            log.info("Buscando usuarios con término '{}' (maxItems={}, skipCount={})", term, maxItems, skipCount);

            String url = alfrescoProperties.getBaseUrl() +
                    "/api/-default-/public/alfresco/versions/1/queries/people?term=" + term.trim().toUpperCase();

            if (maxItems != null)
                url += "&maxItems=" + maxItems;
            if (skipCount != null)
                url += "&skipCount=" + skipCount;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuthToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<AlfrescoPeopleListResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, AlfrescoPeopleListResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                AlfrescoPeopleListResponse alfrescoResponse = response.getBody();
                List<UserDto> users = new ArrayList<>();

                if (alfrescoResponse != null && alfrescoResponse.getList() != null
                        && alfrescoResponse.getList().getEntries() != null) {
                    users = alfrescoResponse.getList().getEntries().stream()
                            .filter(wrapper -> wrapper.getEntry() != null)
                            .map(wrapper -> {
                                AlfrescoPersonResponse.PersonEntry person = wrapper.getEntry();
                                return new UserDto(
                                        person.getId(),
                                        person.getFirstName(),
                                        person.getLastName(),
                                        person.getEmail(),
                                        person.getEnabled());
                            })
                            .collect(Collectors.toList());
                }

                Integer totalItems = (alfrescoResponse != null && alfrescoResponse.getList() != null &&
                        alfrescoResponse.getList().getPagination() != null)
                                ? alfrescoResponse.getList().getPagination().getTotalItems()
                                : users.size();

                Boolean hasMore = (alfrescoResponse != null && alfrescoResponse.getList() != null &&
                        alfrescoResponse.getList().getPagination() != null)
                                ? alfrescoResponse.getList().getPagination().getHasMoreItems()
                                : false;

                return new UserListResponse(users, totalItems, hasMore);
            }
            return new UserListResponse(new ArrayList<>(), 0, false);
        } catch (Exception e) {
            log.error("Error al buscar usuarios con término {}: {}", term, e.getMessage());
            return new UserListResponse(new ArrayList<>(), 0, false);
        }
    }

    /**
     * Obtiene los grupos a los que pertenece un usuario (Detalle completo).
     */
    public GroupListResponse getPersonGroups(String basicAuthToken, String userId) {
        try {
            log.info("Obteniendo grupos del usuario {}", userId);

            String url = alfrescoProperties.getBaseUrl() +
                    "/api/-default-/public/alfresco/versions/1/people/" + userId + "/groups?maxItems=1000";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuthToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<AlfrescoGroupListResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    AlfrescoGroupListResponse.class);

            AlfrescoGroupListResponse personGroupsBody = response.getBody();
            if (personGroupsBody != null && personGroupsBody.getList() != null
                    && personGroupsBody.getList().getEntries() != null) {

                List<GroupDto> groups = personGroupsBody.getList().getEntries().stream()
                        .filter(wrapper -> wrapper.getEntry() != null)
                        .map(wrapper -> {
                            AlfrescoGroupListResponse.GroupEntry entry = wrapper.getEntry();
                            return new GroupDto(
                                    entry.getId(),
                                    entry.getDisplayName(),
                                    entry.getIsRoot());
                        })
                        .collect(Collectors.toList());

                return new GroupListResponse(groups, groups.size());
            }

            return new GroupListResponse(new ArrayList<>(), 0);
        } catch (Exception e) {
            log.error("Error al obtener los grupos del usuario {}: {}", userId, e.getMessage());
            return new GroupListResponse(new ArrayList<>(), 0);
        }
    }

    /**
     * Obtiene los sitios a los que pertenece un usuario, junto con su rol en cada uno.
     * Llama a GET /people/{userId}/sites de la API de Alfresco.
     *
     * @param basicAuthToken Token Basic Auth (Base64 de username:password)
     * @param userId         ID del usuario
     * @return UserSiteMembershipListResponse con los sitios y roles del usuario
     */
    public UserSiteMembershipListResponse getUserSites(String basicAuthToken, String userId) {
        try {
            log.info("Obteniendo sitios del usuario {}", userId);

            String url = alfrescoProperties.getBaseUrl() +
                    "/api/-default-/public/alfresco/versions/1/people/" + userId + "/sites?maxItems=500&skipCount=0";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuthToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<AlfrescoUserSiteListResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    AlfrescoUserSiteListResponse.class);

            AlfrescoUserSiteListResponse sitesBody = response.getBody();
            if (sitesBody != null && sitesBody.getList() != null
                    && sitesBody.getList().getEntries() != null) {

                List<UserSiteMembershipDto> memberships = sitesBody.getList().getEntries().stream()
                        .filter(wrapper -> wrapper.getEntry() != null)
                        .map(wrapper -> {
                            AlfrescoUserSiteListResponse.UserSiteEntry entry = wrapper.getEntry();
                            String siteId = entry.getSite() != null ? entry.getSite().getId() : entry.getId();
                            String siteTitle = entry.getSite() != null
                                    ? (entry.getSite().getDescription() != null && !entry.getSite().getDescription().trim().isEmpty()
                                            ? entry.getSite().getDescription()
                                            : (entry.getSite().getTitle() != null && !entry.getSite().getTitle().trim().isEmpty()
                                                    ? entry.getSite().getTitle()
                                                    : siteId))
                                    : siteId;
                            String visibility = entry.getSite() != null
                                    ? entry.getSite().getVisibility()
                                    : null;
                            return new UserSiteMembershipDto(siteId, siteTitle, entry.getRole(), visibility);
                        })
                        .collect(Collectors.toList());

                return new UserSiteMembershipListResponse(memberships, memberships.size());
            }

            return new UserSiteMembershipListResponse(new ArrayList<>(), 0);
        } catch (Exception e) {
            log.error("Error al obtener los sitios del usuario {}: {}", userId, e.getMessage());
            return new UserSiteMembershipListResponse(new ArrayList<>(), 0);
        }
    }

    /**
     * Guarda el PDF justificante de una reasignacion de unidad y su metadata.
     * El archivo se persiste en target/unit-reassignment-proofs.
     */
    public Map<String, Object> storeUnitReassignmentProof(
            String userId,
            MultipartFile file,
            String operationMode,
            List<String> fromUnitIds,
            List<String> targetUnitIds,
            List<String> finalUnitIds) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Debes adjuntar un PDF justificante.");
        }

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : "justificante.pdf");

        String contentType = file.getContentType() != null ? file.getContentType().trim().toLowerCase() : "";
        boolean looksLikePdf = "application/pdf".equals(contentType) || originalFileName.toLowerCase().endsWith(".pdf");
        if (!looksLikePdf) {
            throw new IllegalArgumentException("El justificante debe ser un archivo PDF.");
        }

        String safeUserId = sanitizeToken(userId);
        String normalizedMode = normalizeOperationMode(operationMode);
        List<String> normalizedFrom = normalizeUnitIds(fromUnitIds);
        List<String> normalizedTarget = normalizeUnitIds(targetUnitIds);
        List<String> normalizedFinal = normalizeUnitIds(finalUnitIds);

        String timestamp = LocalDateTime.now().format(PROOF_TS_FORMAT);
        String storedFileName = safeUserId + "_" + timestamp + ".pdf";

        Path baseDir = Paths.get(System.getProperty("user.dir"), "target", "unit-reassignment-proofs");
        Path filePath = baseDir.resolve(storedFileName).normalize();
        Path metadataPath = baseDir.resolve(safeUserId + "_" + timestamp + ".json").normalize();

        try {
            Files.createDirectories(baseDir);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            Map<String, Object> metadata = new LinkedHashMap<String, Object>();
            metadata.put("userId", userId);
            metadata.put("operationMode", normalizedMode);
            metadata.put("fromUnitIds", normalizedFrom);
            metadata.put("targetUnitIds", normalizedTarget);
            metadata.put("finalUnitIds", normalizedFinal);
            metadata.put("originalFileName", originalFileName);
            metadata.put("storedFileName", storedFileName);
            metadata.put("storedPath", filePath.toString());
            metadata.put("metadataPath", metadataPath.toString());
            metadata.put("size", file.getSize());
            metadata.put("createdAt", LocalDateTime.now().toString());

            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(metadataPath.toFile(), metadata);
            return metadata;
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el justificante PDF.", e);
        }
    }

    private String sanitizeToken(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "unknown";
        }
        return value.trim().replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private String normalizeOperationMode(String operationMode) {
        if (operationMode == null) {
            return "ADD";
        }
        String normalized = operationMode.trim().toUpperCase();
        if ("TRANSFER".equals(normalized)) {
            return "TRANSFER";
        }
        return "ADD";
    }

    private List<String> normalizeUnitIds(List<String> unitIds) {
        if (unitIds == null || unitIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> normalized = new ArrayList<String>();
        for (String unitId : unitIds) {
            if (unitId == null) {
                continue;
            }
            String clean = unitId.trim().toUpperCase();
            if (clean.isEmpty()) {
                continue;
            }
            if (!normalized.contains(clean)) {
                normalized.add(clean);
            }
        }
        return normalized;
    }
}
