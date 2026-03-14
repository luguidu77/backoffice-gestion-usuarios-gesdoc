package es.dggc.backoffice.controller;

import es.dggc.backoffice.model.dto.UserListResponse;
import es.dggc.backoffice.model.dto.GroupListResponse;
import es.dggc.backoffice.model.dto.UserSiteMembershipListResponse;
import es.dggc.backoffice.service.AlfrescoUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para operaciones de gestión de usuarios.
 * 
 * Endpoints:
 * GET /api/users - Listar usuarios de Alfresco
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final AlfrescoUserService alfrescoUserService;

    public UserController(AlfrescoUserService alfrescoUserService) {
        this.alfrescoUserService = alfrescoUserService;
    }

    /**
     * Endpoint para listar usuarios.
     * 
     * GET /api/users
     * Header: Authorization: Basic {token}
     * 
     * Parámetros opcionales:
     * - maxItems: número máximo de usuarios a obtener (por defecto 100)
     * - skipCount: número de usuarios a omitir (para paginación, por defecto 0)
     * 
     * @param authHeader Header de autorización (Basic Auth token)
     * @param maxItems   Número máximo de usuarios a obtener
     * @param skipCount  Número de usuarios a omitir
     * @param siteId     ID del sitio por el que filtrar (opcional)
     * @param searchTerm Término de búsqueda (opcional)
     * @return UserListResponse con la lista de usuarios
     */
    @GetMapping
    public ResponseEntity<UserListResponse> listUsers(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "maxItems", required = false, defaultValue = "100") Integer maxItems,
            @RequestParam(value = "skipCount", required = false, defaultValue = "0") Integer skipCount,
            @RequestParam(value = "siteId", required = false) String siteId,
            @RequestParam(value = "searchTerm", required = false) String searchTerm) {
        log.info("Solicitud de listado de usuarios (maxItems={}, skipCount={}, siteId={}, searchTerm={})",
                maxItems, skipCount, siteId, searchTerm);

        // Validar header de autorización
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            log.warn("Solicitud sin header de autorización válido");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserListResponse());
        }

        // Extraer token (quitar "Basic ")
        String token = authHeader.substring(6);

        try {
            // Obtener lista de usuarios según parámetros
            UserListResponse response;
            if (siteId != null && !siteId.trim().isEmpty()) {
                response = alfrescoUserService.listSiteMembers(token, siteId, maxItems, skipCount);
            } else if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                response = alfrescoUserService.searchUsers(token, searchTerm, maxItems, skipCount);
            } else {
                response = alfrescoUserService.listUsers(token, maxItems, skipCount);
            }

            if (response.getUsers() != null && !response.getUsers().isEmpty()) {
                log.info("Listado de usuarios exitoso: {} usuarios", response.getUsers().size());
            }
            return ResponseEntity.ok(response);

        } catch (HttpClientErrorException e) {
            log.warn("Acceso denegado por Alfresco al listar usuarios: {}", e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).body(new UserListResponse());
        }
    }

    /**
     * Endpoint para obtener información de un usuario específico.
     * 
     * GET /api/users/{userId}
     * Header: Authorization: Basic {token}
     * 
     * @param authHeader Header de autorización
     * @param userId     ID del usuario a buscar
     * @return UserDto con información del usuario
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String userId) {
        log.info("Solicitud de información de usuario: {}", userId);

        // Validar header de autorización
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            log.warn("Solicitud sin header de autorización válido");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("No autorizado");
        }

        // TODO: Implementar obtención de usuario individual
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("Endpoint no implementado aún");
    }

    /**
     * Endpoint para obtener los grupos a los que pertenece un usuario.
     * 
     * GET /api/users/{userId}/groups
     * Header: Authorization: Basic {token}
     * 
     * @param authHeader Header de autorización
     * @param userId     ID del usuario
     * @return GroupListResponse con la lista de grupos
     */
    @GetMapping("/{userId}/groups")
    public ResponseEntity<GroupListResponse> getUserGroups(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String userId) {
        log.info("Solicitud de grupos para usuario: {}", userId);

        // Validar header de autorización
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            log.warn("Solicitud sin header de autorización válido");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new GroupListResponse());
        }

        // Extraer token
        String token = authHeader.substring(6);

        GroupListResponse response = alfrescoUserService.getPersonGroups(token, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para obtener los sitios (unidades) a los que pertenece un usuario, con su rol en cada uno.
     *
     * GET /api/users/{userId}/sites
     * Header: Authorization: Basic {token}
     *
     * @param authHeader Header de autorización
     * @param userId     ID del usuario
     * @return UserSiteMembershipListResponse con los sitios y roles del usuario
     */
    @GetMapping("/{userId}/sites")
    public ResponseEntity<UserSiteMembershipListResponse> getUserSites(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String userId) {
        log.info("Solicitud de sitios para usuario: {}", userId);

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            log.warn("Solicitud sin header de autorización válido");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new UserSiteMembershipListResponse());
        }

        String token = authHeader.substring(6);

        UserSiteMembershipListResponse response = alfrescoUserService.getUserSites(token, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para subir un PDF justificante de reasignacion de unidad.
     *
     * POST /api/users/{userId}/unit-reassignment-proof
     * Content-Type: multipart/form-data
     */
    @PostMapping(path = "/{userId}/unit-reassignment-proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadUnitReassignmentProof(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "operationMode", required = false) String operationMode,
            @RequestParam(value = "fromUnitIds", required = false) List<String> fromUnitIds,
            @RequestParam(value = "targetUnitIds", required = false) List<String> targetUnitIds,
            @RequestParam(value = "finalUnitIds", required = false) List<String> finalUnitIds,
            @RequestParam(value = "transferFromUnitId", required = false) String transferFromUnitId) {

        log.info("Solicitud de subida de justificante de reasignacion de unidad para usuario {}", userId);

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            log.warn("Solicitud sin header de autorizacion valido");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.<String, Object>emptyMap());
        }

        try {
            String token = authHeader.substring(6);
            Map<String, Object> response = alfrescoUserService.storeUnitReassignmentProof(
                    token,
                    userId,
                    file,
                    operationMode,
                    fromUnitIds,
                    targetUnitIds,
                    finalUnitIds,
                    transferFromUnitId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Justificante invalido para usuario {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Collections.<String, Object>singletonMap("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error guardando justificante para usuario {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.<String, Object>singletonMap("message", "No se pudo guardar el justificante."));
        }
    }

    /**
     * Endpoint para consultar auditorias de reasignaciones.
     *
     * GET /api/users/unit-reassignment-audits
     */
    @GetMapping("/unit-reassignment-audits")
    public ResponseEntity<Map<String, Object>> listUnitReassignmentAudits(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "mode", required = false) String mode,
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @RequestParam(value = "toDate", required = false) String toDate,
            @RequestParam(value = "sort", required = false, defaultValue = "newest") String sort,
            @RequestParam(value = "includeMetadata", required = false, defaultValue = "false") boolean includeMetadata,
            @RequestParam(value = "maxItems", required = false, defaultValue = "100") Integer maxItems,
            @RequestParam(value = "skipCount", required = false, defaultValue = "0") Integer skipCount) {

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.<String, Object>emptyMap());
        }

        try {
            String token = authHeader.substring(6);
            Map<String, Object> response = alfrescoUserService.listUnitReassignmentAudits(
                    token,
                    userId,
                    mode,
                    fromDate,
                    toDate,
                    sort,
                    includeMetadata,
                    maxItems,
                    skipCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error consultando auditorias de reasignaciones: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.<String, Object>singletonMap("message", "No se pudieron consultar las auditorias."));
        }
    }

    /**
     * Descarga/visualiza un PDF de auditoría de reasignación por nodeId.
     *
     * GET /api/users/unit-reassignment-audits/{nodeId}/content
     */
    @GetMapping("/unit-reassignment-audits/{nodeId}/content")
    public ResponseEntity<byte[]> downloadUnitReassignmentAuditPdf(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String nodeId) {

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String token = authHeader.substring(6);
            return alfrescoUserService.downloadUnitReassignmentAuditPdf(token, nodeId);
        } catch (HttpClientErrorException e) {
            log.warn("Error descargando auditoria {}: {}", nodeId, e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (Exception e) {
            log.error("Error descargando auditoria {}: {}", nodeId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
