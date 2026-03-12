package es.dggc.backoffice.service;

import es.dggc.backoffice.config.AlfrescoProperties;
import es.dggc.backoffice.model.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Servicio para autenticación y operaciones con Alfresco.
 * 
 * Utiliza Basic Authentication directamente sin tickets.
 * 
 * Responsabilidades:
 * - Validar credenciales contra Alfresco usando Basic Auth
 * - Obtener información de usuarios
 * - Generar tokens de autenticación (username:password en Base64)
 * 
 * Documentación de Alfresco REST API v1:
 * https://api-explorer.alfresco.com/api-explorer/
 */
@Service
public class AlfrescoAuthService {

    private static final Logger log = LoggerFactory.getLogger(AlfrescoAuthService.class);
    private static final Pattern SITE_MANAGER_GROUP_PATTERN = Pattern.compile("^GROUP_site_(.+)_SiteManager$", Pattern.CASE_INSENSITIVE);

    private final RestTemplate restTemplate;
    private final AlfrescoProperties alfrescoProperties;

    public AlfrescoAuthService(RestTemplate restTemplate, AlfrescoProperties alfrescoProperties) {
        this.restTemplate = restTemplate;
        this.alfrescoProperties = alfrescoProperties;
    }

    /**
     * Autentica un usuario en Alfresco usando Basic Authentication.
     * 
     * @param username Usuario de Alfresco
     * @param password Contraseña del usuario
     * @return LoginResponse con el token Basic Auth y datos del usuario si el login
     *         es exitoso
     */
    public LoginResponse login(String username, String password) {
        try {
            log.info("Intentando autenticar usuario: {}", username);

            // 1. Validar credenciales obteniendo información del usuario
            AlfrescoPersonResponse.PersonEntry person = getPersonInfoWithCredentials(username, password);

            if (person == null) {
                log.warn("No se pudo autenticar el usuario: {}", username);
                return new LoginResponse(false, "Credenciales inválidas");
            }

            log.info("Usuario autenticado exitosamente: {}", username);

            // 2. Generar token Basic Auth (username:password en Base64)
            String basicAuthToken = generateBasicAuthToken(username, password);

            // 3. Obtener los grupos del usuario
            List<String> groups = getPersonGroupsWithCredentials(username, basicAuthToken);
            List<String> managedSiteIdsByGroups = extractManagedSiteIdsFromGroups(groups);
            List<String> managedSiteIdsBySites = getManagedSiteIdsWithCredentials(username, basicAuthToken);
            List<String> managedSiteIds = mergeNormalizedSiteIds(managedSiteIdsByGroups, managedSiteIdsBySites);
            boolean isGlobalAdmin = groups.stream()
                    .anyMatch(groupId -> "GROUP_ALFRESCO_ADMINISTRATORS".equalsIgnoreCase(groupId));
            String role = isGlobalAdmin
                    ? "GLOBAL_ADMIN"
                    : (!managedSiteIds.isEmpty() ? "UNIT_ADMIN" : "READ_ONLY");

            // 4. Construir respuesta
            LoginResponse response = new LoginResponse(
                    true,
                    basicAuthToken,
                    username,
                    person.getFirstName() != null ? person.getFirstName() : username,
                    person.getLastName() != null ? person.getLastName() : "",
                    person.getEmail() != null ? person.getEmail() : "",
                    groups);
            response.setManagedSiteIds(managedSiteIds);
            response.setRole(role);
            log.info(
                    "Rol derivado para {}: {} (groups={}, managedByGroups={}, managedBySites={}, managedFinal={})",
                    username,
                    role,
                    groups.size(),
                    managedSiteIdsByGroups.size(),
                    managedSiteIdsBySites.size(),
                    managedSiteIds.size());

            log.info("Login exitoso para usuario: {}", username);
            return response;

        } catch (HttpClientErrorException e) {
            log.error("Error de autenticación para usuario {}: {} - {}",
                    username, e.getStatusCode(), e.getMessage());
            return new LoginResponse(false, "Credenciales inválidas");
        } catch (Exception e) {
            log.error("Error inesperado durante la autenticación: {}", e.getMessage(), e);
            return new LoginResponse(false, "Error al conectar con Alfresco");
        }
    }

    /**
     * Genera un token de Basic Authentication.
     * 
     * @param username Usuario
     * @param password Contraseña
     * @return Token en Base64
     */
    private String generateBasicAuthToken(String username, String password) {
        String credentials = username + ":" + password;
        return Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    /**
     * Obtiene la información de un usuario desde Alfresco usando credenciales.
     * 
     * GET /api/-default-/public/alfresco/versions/1/people/{personId}
     * Header: Authorization: Basic {base64(username:password)}
     * 
     * @param username ID del usuario
     * @param password Contraseña del usuario
     * @return Información del usuario o null si falla
     */
    private AlfrescoPersonResponse.PersonEntry getPersonInfoWithCredentials(String username, String password) {
        try {
            String url = alfrescoProperties.getCoreApiUrl() + "/people/" + username;

            // Configurar headers con Basic Auth
            HttpHeaders headers = new HttpHeaders();
            String basicAuthToken = generateBasicAuthToken(username, password);
            headers.set("Authorization", "Basic " + basicAuthToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            // Hacer la petición a Alfresco
            ResponseEntity<AlfrescoPersonResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    AlfrescoPersonResponse.class);

            AlfrescoPersonResponse personBody = response.getBody();
            if (personBody != null) {
                return personBody.getEntry();
            }

            return null;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED ||
                    e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.warn("Credenciales incorrectas para usuario: {}", username);
            } else {
                log.error("Error HTTP al obtener usuario: {}", e.getMessage());
            }
            return null;
        } catch (Exception e) {
            log.warn("No se pudo obtener información del usuario {}: {}", username, e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene los grupos a los que pertenece un usuario.
     * 
     * GET /api/-default-/public/alfresco/versions/1/people/{personId}/groups
     */
    private List<String> getPersonGroupsWithCredentials(String username, String basicAuthToken) {
        try {
            String url = alfrescoProperties.getCoreApiUrl() + "/people/" + username + "/groups?maxItems=1000";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuthToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<AlfrescoGroupListResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    AlfrescoGroupListResponse.class);

            AlfrescoGroupListResponse groupsBody = response.getBody();
            if (groupsBody != null && groupsBody.getList() != null
                    && groupsBody.getList().getEntries() != null) {
                return groupsBody.getList().getEntries().stream()
                        .filter(wrapper -> wrapper.getEntry() != null)
                        .map(wrapper -> wrapper.getEntry().getId())
                        .collect(Collectors.toList());
            }

            return new ArrayList<>();
        } catch (Exception e) {
            log.warn("No se pudieron obtener los grupos del usuario {}: {}", username, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Obtiene los IDs de sitio donde el usuario tiene rol SiteManager.
     *
     * GET /api/-default-/public/alfresco/versions/1/people/{personId}/sites
     */
    private List<String> getManagedSiteIdsWithCredentials(String username, String basicAuthToken) {
        try {
            String url = alfrescoProperties.getCoreApiUrl() + "/people/" + username + "/sites?maxItems=500&skipCount=0";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuthToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<AlfrescoUserSiteListResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    AlfrescoUserSiteListResponse.class);

            AlfrescoUserSiteListResponse body = response.getBody();
            if (body == null || body.getList() == null || body.getList().getEntries() == null) {
                return new ArrayList<>();
            }

            Set<String> managedIds = new LinkedHashSet<>();
            body.getList().getEntries().stream()
                    .filter(wrapper -> wrapper != null && wrapper.getEntry() != null)
                    .map(AlfrescoUserSiteListResponse.UserSiteEntryWrapper::getEntry)
                    .filter(entry -> "SiteManager".equalsIgnoreCase(entry.getRole() != null ? entry.getRole().trim() : ""))
                    .forEach(entry -> {
                        String siteId = entry.getSite() != null && entry.getSite().getId() != null
                                ? entry.getSite().getId()
                                : entry.getId();
                        if (siteId != null && !siteId.trim().isEmpty()) {
                            managedIds.add(siteId.trim());
                        }
                    });

            return new ArrayList<>(managedIds);
        } catch (Exception e) {
            log.warn("No se pudieron obtener los sitios administrados del usuario {}: {}", username, e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<String> extractManagedSiteIdsFromGroups(List<String> groups) {
        if (groups == null || groups.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> managedIds = new LinkedHashSet<>();
        for (String groupId : groups) {
            if (groupId == null || groupId.trim().isEmpty()) {
                continue;
            }
            Matcher matcher = SITE_MANAGER_GROUP_PATTERN.matcher(groupId.trim());
            if (matcher.matches()) {
                String siteId = matcher.group(1);
                if (siteId != null && !siteId.trim().isEmpty()) {
                    managedIds.add(siteId.trim());
                }
            }
        }
        return new ArrayList<>(managedIds);
    }

    private List<String> mergeNormalizedSiteIds(List<String> first, List<String> second) {
        Set<String> merged = new LinkedHashSet<>();
        appendNormalizedSiteIds(merged, first);
        appendNormalizedSiteIds(merged, second);
        return new ArrayList<>(merged);
    }

    private void appendNormalizedSiteIds(Set<String> target, List<String> source) {
        if (source == null || source.isEmpty()) {
            return;
        }

        for (String siteId : source) {
            if (siteId == null || siteId.trim().isEmpty()) {
                continue;
            }
            target.add(siteId.trim());
        }
    }

    /**
     * Valida un token de Basic Authentication.
     * 
     * Decodifica el token y verifica las credenciales contra Alfresco.
     * 
     * @param basicAuthToken Token en Base64
     * @return true si el token es válido, false si no
     */
    public boolean validateTicket(String basicAuthToken) {
        try {
            // Decodificar el token
            byte[] decodedBytes = Base64.getDecoder().decode(basicAuthToken);
            String credentials = new String(decodedBytes);
            String[] parts = credentials.split(":", 2);

            if (parts.length != 2) {
                log.debug("Token inválido: formato incorrecto");
                return false;
            }

            String username = parts[0];

            // Validar contra Alfresco
            String url = alfrescoProperties.getCoreApiUrl() + "/people/" + username;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuthToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    String.class);

            return response.getStatusCode() == HttpStatus.OK;

        } catch (HttpClientErrorException e) {
            log.debug("Token inválido o expirado: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error al validar token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Cierra sesión (con Basic Auth no hay tickets que eliminar).
     * 
     * @param basicAuthToken Token a invalidar
     * @return siempre true (el cliente debe eliminar el token)
     */
    public boolean logout(String basicAuthToken) {
        try {
            log.info("Sesión cerrada (Basic Auth - el cliente eliminará el token)");
            return true;
        } catch (Exception e) {
            log.error("Error al cerrar sesión: {}", e.getMessage());
            return false;
        }
    }
}
