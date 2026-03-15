package es.dggc.backoffice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.dggc.backoffice.config.AlfrescoProperties;
import es.dggc.backoffice.model.dto.AlfrescoPersonResponse;
import es.dggc.backoffice.model.dto.AlfrescoPeopleListResponse;
import es.dggc.backoffice.model.dto.AlfrescoSiteMembersListResponse;
import es.dggc.backoffice.model.dto.AlfrescoGroupListResponse;
import es.dggc.backoffice.model.dto.AlfrescoUserSiteListResponse;
import es.dggc.backoffice.model.dto.AlfrescoNodeListResponse;
import es.dggc.backoffice.model.dto.AlfrescoNodeSingleResponse;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.text.Normalizer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
    private static final String AUDIT_ROOT_FOLDER = "Backoffice-Reasignaciones-Auditoria";

    private static class UserActivitySummary {
        private String firstActivityAt;
        private String lastActivityAt;
        private int reassignmentCount;
    }

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
                                        person.getEnabled(),
                                        wrapper.getEntry().getRole());
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

            String normalizedTerm = normalizeSearchableText(term);
            if (normalizedTerm.isEmpty()) {
                return listUsers(basicAuthToken, maxItems, skipCount);
            }

            int effectiveMaxItems = (maxItems != null && maxItems > 0) ? maxItems : 100;
            int effectiveSkipCount = (skipCount != null && skipCount >= 0) ? skipCount : 0;

            int scanSkip = 0;
            int scanPageSize = 500;
            int totalMatches = 0;
            List<UserDto> pagedMatches = new ArrayList<>();

            while (true) {
                UserListResponse page = listUsers(basicAuthToken, scanPageSize, scanSkip);
                List<UserDto> users = page.getUsers() != null ? page.getUsers() : Collections.<UserDto>emptyList();

                if (users.isEmpty()) {
                    break;
                }

                for (UserDto user : users) {
                    if (!matchesUserSearchTerm(user, normalizedTerm)) {
                        continue;
                    }

                    if (totalMatches >= effectiveSkipCount && pagedMatches.size() < effectiveMaxItems) {
                        pagedMatches.add(user);
                    }
                    totalMatches++;
                }

                scanSkip += users.size();

                boolean sourceHasMore = Boolean.TRUE.equals(page.getHasMore());
                if (!sourceHasMore) {
                    break;
                }
            }

            boolean hasMore = (effectiveSkipCount + pagedMatches.size()) < totalMatches;
            return new UserListResponse(pagedMatches, totalMatches, hasMore);
        } catch (Exception e) {
            log.error("Error al buscar usuarios con término {}: {}", term, e.getMessage());
            return new UserListResponse(new ArrayList<>(), 0, false);
        }
    }

    public UserDto updateUserEnabled(String basicAuthToken, String userId, boolean enabled) {
        try {
            String encodedUserId = URLEncoder.encode(userId.trim(), "UTF-8");
            String url = alfrescoProperties.getBaseUrl()
                    + "/api/-default-/public/alfresco/versions/1/people/"
                    + encodedUserId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuthToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("enabled", enabled);

            ResponseEntity<AlfrescoPersonResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    new HttpEntity<Map<String, Object>>(payload, headers),
                    AlfrescoPersonResponse.class);

            AlfrescoPersonResponse body = response.getBody();
            if (body == null || body.getEntry() == null) {
                return new UserDto(userId, "", "", "", enabled);
            }

            AlfrescoPersonResponse.PersonEntry person = body.getEntry();
            return new UserDto(
                    person.getId(),
                    person.getFirstName(),
                    person.getLastName(),
                    person.getEmail(),
                    person.getEnabled());
        } catch (Exception e) {
            throw new RuntimeException("No se pudo actualizar el estado del usuario " + userId + ".", e);
        }
    }

    private boolean matchesUserSearchTerm(UserDto user, String normalizedTerm) {
        if (user == null || normalizedTerm == null || normalizedTerm.isEmpty()) {
            return false;
        }

        String id = normalizeSearchableText(user.getId());
        String email = normalizeSearchableText(user.getEmail());
        String firstName = normalizeSearchableText(user.getFirstName());
        String lastName = normalizeSearchableText(user.getLastName());
        String fullName = normalizeSearchableText((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName()));

        return id.contains(normalizedTerm)
                || email.contains(normalizedTerm)
                || firstName.contains(normalizedTerm)
                || lastName.contains(normalizedTerm)
                || fullName.contains(normalizedTerm);
    }

    private String normalizeSearchableText(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]", "");

        return normalized.trim();
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
         * Guarda el PDF justificante de una reasignacion de unidad y su metadata en Alfresco.
         * Estructura: /Shared/Backoffice-Reasignaciones-Auditoria/YYYY/MM
         */
    public Map<String, Object> storeUnitReassignmentProof(
            String basicAuthToken,
            String userId,
            MultipartFile file,
            String operationMode,
            List<String> fromUnitIds,
            List<String> targetUnitIds,
            List<String> finalUnitIds,
            String transferFromUnitId) {

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
        String normalizedTransferFrom = (transferFromUnitId != null && !transferFromUnitId.trim().isEmpty())
            ? transferFromUnitId.trim().toUpperCase()
            : null;

        String timestamp = LocalDateTime.now().format(PROOF_TS_FORMAT);
        String storedFileName = safeUserId + "_" + timestamp + ".pdf";
        String metadataFileName = safeUserId + "_" + timestamp + ".json";

        try {
            YearMonth currentMonth = YearMonth.now();
            String yearToken = String.valueOf(currentMonth.getYear());
            String monthToken = String.format("%02d", currentMonth.getMonthValue());

            String auditFolderId = ensureAuditFolderPath(
                    basicAuthToken,
                    new String[] { "Shared", AUDIT_ROOT_FOLDER, yearToken, monthToken });

            Map<String, Object> metadata = new LinkedHashMap<String, Object>();
            metadata.put("userId", userId);
            metadata.put("operationMode", normalizedMode);
            metadata.put("fromUnitIds", normalizedFrom);
            metadata.put("targetUnitIds", normalizedTarget);
            metadata.put("finalUnitIds", normalizedFinal);
            metadata.put("transferFromUnitId", normalizedTransferFrom);
            metadata.put("originalFileName", originalFileName);
            metadata.put("storedFileName", storedFileName);
            metadata.put("size", file.getSize());
            metadata.put("createdAt", LocalDateTime.now().toString());

            AlfrescoNodeListResponse.NodeEntry pdfNode = uploadContentToFolder(
                    basicAuthToken,
                    auditFolderId,
                    storedFileName,
                    file.getBytes(),
                    "application/pdf");

            byte[] metadataBytes = OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(metadata)
                    .getBytes(StandardCharsets.UTF_8);

            AlfrescoNodeListResponse.NodeEntry metadataNode = uploadContentToFolder(
                    basicAuthToken,
                    auditFolderId,
                    metadataFileName,
                    metadataBytes,
                    "application/json");

            metadata.put("storedPath", "alfresco://node/" + (pdfNode != null ? pdfNode.getId() : ""));
            metadata.put("metadataPath", "alfresco://node/" + (metadataNode != null ? metadataNode.getId() : ""));
            metadata.put("folderId", auditFolderId);
            metadata.put("pdfNodeId", pdfNode != null ? pdfNode.getId() : "");
            metadata.put("metadataNodeId", metadataNode != null ? metadataNode.getId() : "");
            metadata.put("storage", "ALFRESCO");

            return metadata;
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar el justificante PDF.", e);
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("No se pudo guardar el justificante PDF en Alfresco (" + e.getStatusCode() + ").", e);
        }
    }

    public Map<String, Object> listUnitReassignmentAudits(
            String basicAuthToken,
            String userIdFilter,
            String modeFilter,
            String fromDate,
            String toDate,
            String sort,
            boolean includeMetadata,
            Integer maxItems,
            Integer skipCount) {

        int effectiveMaxItems = (maxItems != null && maxItems > 0) ? maxItems : 100;
        int effectiveSkipCount = (skipCount != null && skipCount >= 0) ? skipCount : 0;

        boolean hasFilter = (userIdFilter != null && !userIdFilter.trim().isEmpty())
            || (fromDate != null && !fromDate.trim().isEmpty())
            || (toDate != null && !toDate.trim().isEmpty());

        if (!hasFilter) {
            return buildAuditListResponse(new ArrayList<Map<String, Object>>(), effectiveMaxItems, effectiveSkipCount);
        }

        List<Map<String, Object>> collected = new ArrayList<Map<String, Object>>();
        String normalizedModeFilter = modeFilter != null ? modeFilter.trim().toUpperCase() : "";
        LocalDate parsedFromDate = parseDateOrNull(fromDate);
        LocalDate parsedToDate = parseDateOrNull(toDate);
        try {
            String sharedId = findChildFolderIdByName(basicAuthToken, "-root-", "Shared");
            if (sharedId == null) {
                return buildAuditListResponse(collected, effectiveMaxItems, effectiveSkipCount);
            }

            String auditRootId = findChildFolderIdByName(basicAuthToken, sharedId, AUDIT_ROOT_FOLDER);
            if (auditRootId == null) {
                return buildAuditListResponse(collected, effectiveMaxItems, effectiveSkipCount);
            }

            List<AlfrescoNodeListResponse.NodeEntry> yearFolders = listFolderChildren(basicAuthToken, auditRootId);
            yearFolders.sort(Comparator.comparing(AlfrescoNodeListResponse.NodeEntry::getName).reversed());

            for (AlfrescoNodeListResponse.NodeEntry yearFolder : yearFolders) {
                List<AlfrescoNodeListResponse.NodeEntry> monthFolders = listFolderChildren(basicAuthToken, yearFolder.getId());
                monthFolders.sort(Comparator.comparing(AlfrescoNodeListResponse.NodeEntry::getName).reversed());

                for (AlfrescoNodeListResponse.NodeEntry monthFolder : monthFolders) {
                    List<AlfrescoNodeListResponse.NodeEntry> files = listFileChildren(basicAuthToken, monthFolder.getId());
                    Map<String, AlfrescoNodeListResponse.NodeEntry> jsonByBaseName = new LinkedHashMap<String, AlfrescoNodeListResponse.NodeEntry>();
                    for (AlfrescoNodeListResponse.NodeEntry fileNode : files) {
                        String fileName = fileNode.getName();
                        if (fileName != null && fileName.toLowerCase().endsWith(".json")) {
                            jsonByBaseName.put(removeFileExtension(fileName), fileNode);
                        }
                    }

                    for (AlfrescoNodeListResponse.NodeEntry fileNode : files) {
                        String fileName = fileNode.getName();
                        if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
                            continue;
                        }

                        if (userIdFilter != null && !userIdFilter.trim().isEmpty()) {
                            String safeFilter = sanitizeToken(userIdFilter);
                            if (!fileName.toUpperCase().startsWith(safeFilter.toUpperCase() + "_")) {
                                continue;
                            }
                        }

                        String baseName = removeFileExtension(fileName);
                        AlfrescoNodeListResponse.NodeEntry jsonNode = jsonByBaseName.get(baseName);
                        Map<String, Object> metadata = jsonNode != null
                                ? loadAuditMetadataNode(basicAuthToken, jsonNode.getId())
                                : Collections.<String, Object>emptyMap();

                        String operationMode = metadata.get("operationMode") != null
                                ? String.valueOf(metadata.get("operationMode"))
                                : "";
                        if (!normalizedModeFilter.isEmpty() && !normalizedModeFilter.equalsIgnoreCase(operationMode)) {
                            continue;
                        }

                        LocalDate createdDate = parseCreatedDateFromMetadata(metadata);
                        if (!matchesDateRange(createdDate, parsedFromDate, parsedToDate)) {
                            continue;
                        }

                        Map<String, Object> entry = new LinkedHashMap<String, Object>();
                        entry.put("fileName", fileName);
                        entry.put("nodeId", fileNode.getId());
                        entry.put("metadataNodeId", jsonNode != null ? jsonNode.getId() : "");
                        entry.put("folder", yearFolder.getName() + "/" + monthFolder.getName());
                        entry.put("userId", extractUserIdFromAuditFileName(fileName));
                        entry.put("operationMode", operationMode);
                        entry.put("createdAt", metadata.get("createdAt"));
                        if (includeMetadata) {
                            entry.put("metadata", metadata);
                        }
                        collected.add(entry);
                    }
                }
            }

            applyAuditSort(collected, sort);

            return buildAuditListResponse(collected, effectiveMaxItems, effectiveSkipCount);
        } catch (Exception e) {
            log.error("Error listando auditorias de reasignaciones: {}", e.getMessage(), e);
            return buildAuditListResponse(collected, effectiveMaxItems, effectiveSkipCount);
        }
    }

    public Map<String, Object> listUserOriginAudits(
            String basicAuthToken,
            String searchTerm,
            String status,
            Integer maxItems,
            Integer skipCount) {

        int effectiveMaxItems = (maxItems != null && maxItems > 0) ? maxItems : 20;
        int effectiveSkipCount = (skipCount != null && skipCount >= 0) ? skipCount : 0;
        String normalizedTerm = normalizeSearchableText(searchTerm);

        if (normalizedTerm.isEmpty()) {
            return buildAuditListResponse(new ArrayList<Map<String, Object>>(), effectiveMaxItems, effectiveSkipCount);
        }

        try {
            List<UserDto> allUsers = loadAllUsers(basicAuthToken);
            String normalizedStatus = status != null ? status.trim().toLowerCase() : "all";

            List<UserDto> filtered = allUsers.stream()
                    .filter(user -> matchesUserOriginFilters(user, normalizedTerm, normalizedStatus))
                    .sorted(Comparator.comparing(UserDto::getId, Comparator.nullsLast(String::compareToIgnoreCase)))
                    .collect(Collectors.toList());

            int total = filtered.size();
            int fromIndex = Math.min(effectiveSkipCount, total);
            int toIndex = Math.min(fromIndex + effectiveMaxItems, total);
            List<UserDto> pagedUsers = filtered.subList(fromIndex, toIndex);

            Map<String, UserActivitySummary> activityByUser = buildUserActivitySummary(basicAuthToken);
            List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();

            for (UserDto user : pagedUsers) {
                Map<String, String> directInfo = loadUserOriginDirectInfo(basicAuthToken, user.getId());
                UserActivitySummary summary = activityByUser.get(user.getId() != null ? user.getId().toUpperCase() : "");

                String createdAt = firstNonEmpty(
                        directInfo.get("createdAt"),
                        summary != null ? summary.firstActivityAt : null);

                String createdBy = firstNonEmpty(
                        directInfo.get("createdBy"),
                        summary != null && summary.firstActivityAt != null ? "Auditoría de reasignaciones" : null,
                        "No disponible");

                String source = directInfo.get("createdAt") != null
                        ? "ALFRESCO_PERSON"
                        : (summary != null && summary.firstActivityAt != null ? "AUDITORIA_REASIGNACIONES" : "NO_DISPONIBLE");

                Map<String, Object> entry = new LinkedHashMap<String, Object>();
                entry.put("userId", user.getId());
                entry.put("fullName", user.getFullName());
                entry.put("email", user.getEmail());
                entry.put("enabled", user.getEnabled());
                entry.put("createdAt", createdAt != null ? createdAt : "");
                entry.put("createdBy", createdBy);
                entry.put("source", source);
                entry.put("lastActivityAt", summary != null && summary.lastActivityAt != null ? summary.lastActivityAt : "");
                entry.put("reassignmentCount", summary != null ? summary.reassignmentCount : 0);
                items.add(entry);
            }

            Map<String, Object> response = new LinkedHashMap<String, Object>();
            response.put("items", items);
            response.put("totalItems", total);
            response.put("hasMore", toIndex < total);
            response.put("skipCount", effectiveSkipCount);
            response.put("maxItems", effectiveMaxItems);
            return response;
        } catch (Exception e) {
            log.error("Error listando auditoria de fecha/creador de usuario: {}", e.getMessage(), e);
            return buildAuditListResponse(new ArrayList<Map<String, Object>>(), effectiveMaxItems, effectiveSkipCount);
        }
    }

    private List<UserDto> loadAllUsers(String basicAuthToken) {
        List<UserDto> allUsers = new ArrayList<UserDto>();
        int skip = 0;
        int pageSize = 500;

        while (true) {
            UserListResponse page = listUsers(basicAuthToken, pageSize, skip);
            List<UserDto> users = page.getUsers() != null ? page.getUsers() : Collections.<UserDto>emptyList();
            if (users.isEmpty()) {
                break;
            }

            allUsers.addAll(users);
            skip += users.size();

            if (!Boolean.TRUE.equals(page.getHasMore())) {
                break;
            }
        }

        return allUsers;
    }

    private boolean matchesUserOriginFilters(UserDto user, String normalizedTerm, String normalizedStatus) {
        if (user == null) {
            return false;
        }

        if ("active".equals(normalizedStatus) && !Boolean.TRUE.equals(user.getEnabled())) {
            return false;
        }
        if ("inactive".equals(normalizedStatus) && Boolean.TRUE.equals(user.getEnabled())) {
            return false;
        }

        if (normalizedTerm == null || normalizedTerm.isEmpty()) {
            return true;
        }

        String byId = normalizeSearchableText(user.getId());
        String byEmail = normalizeSearchableText(user.getEmail());
        String byName = normalizeSearchableText(user.getFullName());

        return byId.contains(normalizedTerm)
                || byEmail.contains(normalizedTerm)
                || byName.contains(normalizedTerm);
    }

    private Map<String, String> loadUserOriginDirectInfo(String basicAuthToken, String userId) {
        Map<String, String> data = new LinkedHashMap<String, String>();
        if (userId == null || userId.trim().isEmpty()) {
            return data;
        }

        try {
            String encodedUserId = URLEncoder.encode(userId.trim(), "UTF-8");
            String url = alfrescoProperties.getBaseUrl()
                    + "/api/-default-/public/alfresco/versions/1/people/"
                    + encodedUserId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuthToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<Void>(headers),
                    Map.class);

            Map body = response.getBody();
            if (body == null) {
                return data;
            }

            Object entryObject = body.get("entry");
            if (!(entryObject instanceof Map)) {
                return data;
            }

            Map entry = (Map) entryObject;
            String createdAt = firstNonEmpty(
                    asString(entry.get("createdAt")),
                    asString(entry.get("createdOn")),
                    asString(entry.get("created")));
            String createdBy = firstNonEmpty(
                    asString(entry.get("createdByUser")),
                    asString(entry.get("createdBy")),
                    asString(entry.get("creator")));

            if (createdAt != null && !createdAt.isEmpty()) {
                data.put("createdAt", createdAt);
            }
            if (createdBy != null && !createdBy.isEmpty()) {
                data.put("createdBy", createdBy);
            }
        } catch (Exception ex) {
            // Sin bloqueo: se usará fallback de auditoría si existe.
        }

        return data;
    }

    private Map<String, UserActivitySummary> buildUserActivitySummary(String basicAuthToken) {
        Map<String, UserActivitySummary> byUser = new LinkedHashMap<String, UserActivitySummary>();

        try {
            Map<String, Object> auditsResponse = listUnitReassignmentAudits(
                    basicAuthToken,
                    null,
                    null,
                    null,
                    null,
                    "newest",
                    true,
                    100000,
                    0);

            Object itemsObj = auditsResponse.get("items");
            if (!(itemsObj instanceof List)) {
                return byUser;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) itemsObj;
            for (Map<String, Object> item : items) {
                if (item == null) {
                    continue;
                }

                String userId = asString(item.get("userId"));
                String createdAt = asString(item.get("createdAt"));
                if (userId == null || userId.trim().isEmpty()) {
                    continue;
                }

                String key = userId.trim().toUpperCase();
                UserActivitySummary summary = byUser.get(key);
                if (summary == null) {
                    summary = new UserActivitySummary();
                    byUser.put(key, summary);
                }

                summary.reassignmentCount++;

                if (createdAt != null && !createdAt.trim().isEmpty()) {
                    if (summary.firstActivityAt == null || createdAt.compareTo(summary.firstActivityAt) < 0) {
                        summary.firstActivityAt = createdAt;
                    }
                    if (summary.lastActivityAt == null || createdAt.compareTo(summary.lastActivityAt) > 0) {
                        summary.lastActivityAt = createdAt;
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("No se pudo construir resumen de actividad de usuarios desde auditoría: {}", ex.getMessage());
        }

        return byUser;
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String firstNonEmpty(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private Map<String, Object> loadAuditMetadataNode(String basicAuthToken, String nodeId) {
        try {
            String url = alfrescoProperties.getBaseUrl()
                    + "/api/-default-/public/alfresco/versions/1/nodes/"
                    + nodeId
                    + "/content";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuthToken);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<Void>(headers),
                    String.class);

            String body = response.getBody();
            if (body == null || body.trim().isEmpty()) {
                return Collections.<String, Object>emptyMap();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = OBJECT_MAPPER.readValue(body, LinkedHashMap.class);
            return metadata != null ? metadata : Collections.<String, Object>emptyMap();
        } catch (Exception e) {
            return Collections.<String, Object>emptyMap();
        }
    }

    private String removeFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0) {
            return fileName;
        }
        return fileName.substring(0, dotIndex);
    }

    private LocalDate parseDateOrNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LocalDate parseCreatedDateFromMetadata(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        Object createdAt = metadata.get("createdAt");
        if (createdAt == null) {
            return null;
        }
        String createdAtString = String.valueOf(createdAt).trim();
        if (createdAtString.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(createdAtString).toLocalDate();
        } catch (DateTimeParseException ex) {
            try {
                return LocalDate.parse(createdAtString);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private boolean matchesDateRange(LocalDate createdDate, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null && toDate == null) {
            return true;
        }
        if (createdDate == null) {
            return false;
        }
        if (fromDate != null && createdDate.isBefore(fromDate)) {
            return false;
        }
        if (toDate != null && createdDate.isAfter(toDate)) {
            return false;
        }
        return true;
    }

    private void applyAuditSort(List<Map<String, Object>> items, String sort) {
        String normalizedSort = sort != null ? sort.trim().toLowerCase() : "newest";
        if ("oldest".equals(normalizedSort)) {
            items.sort(Comparator.comparing(item -> String.valueOf(item.get("fileName"))));
            return;
        }
        if ("user-asc".equals(normalizedSort)) {
            items.sort(Comparator.comparing(item -> String.valueOf(item.get("userId"))));
            return;
        }
        if ("user-desc".equals(normalizedSort)) {
            items.sort((a, b) -> String.valueOf(b.get("userId")).compareTo(String.valueOf(a.get("userId"))));
            return;
        }

        items.sort((a, b) -> String.valueOf(b.get("fileName")).compareTo(String.valueOf(a.get("fileName"))));
    }

    public ResponseEntity<byte[]> downloadUnitReassignmentAuditPdf(String basicAuthToken, String nodeId) {
        String url = alfrescoProperties.getBaseUrl()
                + "/api/-default-/public/alfresco/versions/1/nodes/"
                + nodeId
                + "/content?attachment=true";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + basicAuthToken);

        ResponseEntity<byte[]> sourceResponse = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<Void>(headers),
                byte[].class);

        HttpHeaders forwardHeaders = new HttpHeaders();
        MediaType sourceType = sourceResponse.getHeaders().getContentType();
        forwardHeaders.setContentType(sourceType != null ? sourceType : MediaType.APPLICATION_PDF);

        List<String> sourceDisposition = sourceResponse.getHeaders().get("Content-Disposition");
        if (sourceDisposition != null && !sourceDisposition.isEmpty()) {
            forwardHeaders.put("Content-Disposition", sourceDisposition);
        } else {
            forwardHeaders.set("Content-Disposition", "attachment; filename=auditoria-reasignacion.pdf");
        }

        return new ResponseEntity<byte[]>(sourceResponse.getBody(), forwardHeaders, sourceResponse.getStatusCode());
    }

    private Map<String, Object> buildAuditListResponse(List<Map<String, Object>> items, int maxItems, int skipCount) {
        int total = items.size();
        int fromIndex = Math.min(skipCount, total);
        int toIndex = Math.min(fromIndex + maxItems, total);
        List<Map<String, Object>> paged = items.subList(fromIndex, toIndex);

        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("items", new ArrayList<Map<String, Object>>(paged));
        response.put("totalItems", total);
        response.put("hasMore", toIndex < total);
        response.put("skipCount", skipCount);
        response.put("maxItems", maxItems);
        return response;
    }

    private String extractUserIdFromAuditFileName(String fileName) {
        if (fileName == null) {
            return "";
        }
        int split = fileName.indexOf('_');
        if (split <= 0) {
            return fileName;
        }
        return fileName.substring(0, split);
    }

    private String ensureAuditFolderPath(String basicAuthToken, String[] segments) {
        String currentParentId = "-root-";
        for (String segment : segments) {
            String cleanName = (segment != null) ? segment.trim() : "";
            if (cleanName.isEmpty()) {
                continue;
            }

            String existingId = findChildFolderIdByName(basicAuthToken, currentParentId, cleanName);
            if (existingId != null) {
                currentParentId = existingId;
                continue;
            }

            AlfrescoNodeListResponse.NodeEntry created = createFolder(basicAuthToken, currentParentId, cleanName);
            if (created == null || created.getId() == null || created.getId().trim().isEmpty()) {
                throw new RuntimeException("No se pudo crear carpeta de auditoria: " + cleanName);
            }
            currentParentId = created.getId();
        }

        return currentParentId;
    }

    private String findChildFolderIdByName(String basicAuthToken, String parentNodeId, String folderName) {
        List<AlfrescoNodeListResponse.NodeEntry> folders = listFolderChildren(basicAuthToken, parentNodeId);
        for (AlfrescoNodeListResponse.NodeEntry entry : folders) {
            if (entry.getName() != null && entry.getName().equalsIgnoreCase(folderName)) {
                return entry.getId();
            }
        }
        return null;
    }

    private List<AlfrescoNodeListResponse.NodeEntry> listFolderChildren(String basicAuthToken, String parentNodeId) {
        String url = alfrescoProperties.getBaseUrl()
                + "/api/-default-/public/alfresco/versions/1/nodes/"
                + parentNodeId
                + "/children?where=(isFolder=true)&maxItems=1000&skipCount=0";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + basicAuthToken);

        ResponseEntity<AlfrescoNodeListResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<Void>(headers),
                AlfrescoNodeListResponse.class);

        if (response.getBody() == null || response.getBody().getList() == null || response.getBody().getList().getEntries() == null) {
            return new ArrayList<AlfrescoNodeListResponse.NodeEntry>();
        }

        return response.getBody().getList().getEntries().stream()
                .filter(wrapper -> wrapper != null && wrapper.getEntry() != null)
                .map(AlfrescoNodeListResponse.NodeEntryWrapper::getEntry)
                .collect(Collectors.toList());
    }

    private List<AlfrescoNodeListResponse.NodeEntry> listFileChildren(String basicAuthToken, String parentNodeId) {
        String url = alfrescoProperties.getBaseUrl()
                + "/api/-default-/public/alfresco/versions/1/nodes/"
                + parentNodeId
                + "/children?where=(isFile=true)&maxItems=1000&skipCount=0";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + basicAuthToken);

        ResponseEntity<AlfrescoNodeListResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<Void>(headers),
                AlfrescoNodeListResponse.class);

        if (response.getBody() == null || response.getBody().getList() == null || response.getBody().getList().getEntries() == null) {
            return new ArrayList<AlfrescoNodeListResponse.NodeEntry>();
        }

        return response.getBody().getList().getEntries().stream()
                .filter(wrapper -> wrapper != null && wrapper.getEntry() != null)
                .map(AlfrescoNodeListResponse.NodeEntryWrapper::getEntry)
                .collect(Collectors.toList());
    }

    private AlfrescoNodeListResponse.NodeEntry createFolder(String basicAuthToken, String parentNodeId, String folderName) {
        String url = alfrescoProperties.getBaseUrl()
                + "/api/-default-/public/alfresco/versions/1/nodes/"
                + parentNodeId
                + "/children";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + basicAuthToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("name", folderName);
        payload.put("nodeType", "cm:folder");

        ResponseEntity<AlfrescoNodeSingleResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<Map<String, Object>>(payload, headers),
                AlfrescoNodeSingleResponse.class);

        return response.getBody() != null ? response.getBody().getEntry() : null;
    }

    private AlfrescoNodeListResponse.NodeEntry uploadContentToFolder(
            String basicAuthToken,
            String parentNodeId,
            String fileName,
            byte[] content,
            String mimeType) {

        String url = alfrescoProperties.getBaseUrl()
                + "/api/-default-/public/alfresco/versions/1/nodes/"
                + parentNodeId
                + "/children?autoRename=true";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + basicAuthToken);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource fileResource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType(mimeType));
        HttpEntity<ByteArrayResource> fileEntity = new HttpEntity<ByteArrayResource>(fileResource, fileHeaders);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
        body.add("filedata", fileEntity);
        body.add("name", fileName);
        body.add("nodeType", "cm:content");

        ResponseEntity<AlfrescoNodeSingleResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<MultiValueMap<String, Object>>(body, headers),
                AlfrescoNodeSingleResponse.class);

        return response.getBody() != null ? response.getBody().getEntry() : null;
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
        if ("DEPARTMENTS".equals(normalized)) {
            return "DEPARTMENTS";
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
