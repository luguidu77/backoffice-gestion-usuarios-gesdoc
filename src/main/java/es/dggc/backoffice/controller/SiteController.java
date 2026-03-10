package es.dggc.backoffice.controller;

import es.dggc.backoffice.model.dto.SiteListResponse;
import es.dggc.backoffice.service.AlfrescoSiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     *
     * @param authHeader Header de autorización (Basic Auth token)
     * @param maxItems   Número máximo de sitios a obtener
     * @param skipCount  Número de sitios a omitir
     * @return SiteListResponse con la lista de sitios
     */
    @GetMapping
    public ResponseEntity<SiteListResponse> listSites(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "maxItems", required = false, defaultValue = "100") Integer maxItems,
            @RequestParam(value = "skipCount", required = false, defaultValue = "0") Integer skipCount) {
        log.info("Solicitud de listado de sitios (maxItems={}, skipCount={})", maxItems, skipCount);

        // Validar header de autorización
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            log.warn("Solicitud sin header de autorización válido");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new SiteListResponse());
        }

        // Extraer token (quitar "Basic ")
        String token = authHeader.substring(6);

        // Obtener lista de sitios
        SiteListResponse response = alfrescoSiteService.listSites(token, maxItems, skipCount);

        log.info("Listado de sitios: {} sitios devueltos", response.getSites().size());
        return ResponseEntity.ok(response);
    }
}
