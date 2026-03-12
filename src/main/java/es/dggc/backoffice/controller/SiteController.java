package es.dggc.backoffice.controller;

import es.dggc.backoffice.model.dto.SiteListResponse;
import es.dggc.backoffice.service.AlfrescoSiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Controlador REST para operaciones de gestión de sitios.
 *
 * Endpoints:
 * GET /api/sites - Listar sitios de Alfresco
 */
@RestController
@RequestMapping("/api/sites")
@CrossOrigin(origins = "*")
public class SiteController {

    private static final Logger log = LoggerFactory.getLogger(SiteController.class);

    private final AlfrescoSiteService alfrescoSiteService;

    public SiteController(AlfrescoSiteService alfrescoSiteService) {
        this.alfrescoSiteService = alfrescoSiteService;
    }

    /**
     * Endpoint para listar sitios.
     *
     * GET /api/sites
     * Header: Authorization: Basic {token}
     *
     * Parámetros opcionales:
     * - maxItems: número máximo de sitios a obtener (por defecto 100)
     * - skipCount: número de sitios a omitir (para paginación, por defecto 0)
     * - role: rol del usuario (GLOBAL_ADMIN o UNIT_ADMIN)
     * - userId: username si es UNIT_ADMIN (para buscar sus sitios)
     *
     * @param authHeader Header de autorización (Basic Auth token)
     * @param maxItems   Número máximo de sitios a obtener
     * @param skipCount  Número de sitios a omitir
     * @param role       Rol del usuario
     * @param userId     ID del usuario actual
     * @return SiteListResponse con la lista de sitios
     */
    @GetMapping
    public ResponseEntity<SiteListResponse> listSites(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "maxItems", required = false, defaultValue = "100") Integer maxItems,
            @RequestParam(value = "skipCount", required = false, defaultValue = "0") Integer skipCount,
            @RequestParam(value = "role", required = false, defaultValue = "GLOBAL_ADMIN") String role,
            @RequestParam(value = "userId", required = false) String userId) {
        log.info("Solicitud de listado de sitios (maxItems={}, skipCount={}, role={}, userId={})", maxItems, skipCount,
                role, userId);

        // Validar header de autorización
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            log.warn("Solicitud sin header de autorización válido");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new SiteListResponse());
        }

        // Extraer token (quitar "Basic ")
        String token = authHeader.substring(6);

        // Obtener lista de sitios según el rol
        SiteListResponse response;
        if ("UNIT_ADMIN".equals(role) && userId != null && !userId.trim().isEmpty()) {
            // El UNIT_ADMIN solo ve los sitios a los que pertenece
            response = alfrescoSiteService.listUserSites(token, userId, maxItems, skipCount);
        } else {
            // El GLOBAL_ADMIN ve todos los sitios del repositorio
            response = alfrescoSiteService.listSites(token, maxItems, skipCount);
        }

        log.info("Listado de sitios: {} sitios devueltos", response.getSites().size());
        return ResponseEntity.ok(response);
    }

    /**
     * Asigna rol de SiteManager a un usuario en una unidad concreta.
     *
     * PUT /api/sites/{siteId}/admins/{userId}
     */
    @PutMapping("/{siteId}/admins/{userId}")
    public ResponseEntity<Void> assignUnitAdmin(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String siteId,
            @PathVariable String userId) {

        log.info("Solicitud para asignar SiteManager en site {} al usuario {}", siteId, userId);

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            log.warn("Solicitud sin header de autorizacion valido");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(6);

        try {
            alfrescoSiteService.assignSiteManager(token, siteId, userId);
            return ResponseEntity.noContent().build();
        } catch (HttpClientErrorException e) {
            log.warn("Error HTTP asignando SiteManager a {} en {}: {}", userId, siteId, e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (RuntimeException e) {
            log.error("Error asignando SiteManager a {} en {}: {}", userId, siteId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Asigna rol de usuario de sitio (SiteCollaborator) a un usuario en una unidad concreta.
     *
     * PUT /api/sites/{siteId}/users/{userId}
     */
    @PutMapping("/{siteId}/users/{userId}")
    public ResponseEntity<Void> assignSiteUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String siteId,
            @PathVariable String userId) {

        log.info("Solicitud para asignar SiteCollaborator en site {} al usuario {}", siteId, userId);

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            log.warn("Solicitud sin header de autorizacion valido");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(6);

        try {
            alfrescoSiteService.assignSiteUser(token, siteId, userId);
            return ResponseEntity.noContent().build();
        } catch (HttpClientErrorException e) {
            log.warn("Error HTTP asignando SiteCollaborator a {} en {}: {}", userId, siteId, e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (RuntimeException e) {
            log.error("Error asignando SiteCollaborator a {} en {}: {}", userId, siteId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Elimina a un usuario de una unidad (sitio).
     *
     * DELETE /api/sites/{siteId}/users/{userId}
     */
    @DeleteMapping("/{siteId}/users/{userId}")
    public ResponseEntity<Void> removeSiteUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String siteId,
            @PathVariable String userId) {

        log.info("Solicitud para eliminar usuario {} del site {}", userId, siteId);

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            log.warn("Solicitud sin header de autorizacion valido");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(6);

        try {
            alfrescoSiteService.removeSiteUser(token, siteId, userId);
            return ResponseEntity.noContent().build();
        } catch (HttpClientErrorException e) {
            log.warn("Error HTTP eliminando usuario {} de {}: {}", userId, siteId, e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (RuntimeException e) {
            log.error("Error eliminando usuario {} de {}: {}", userId, siteId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
