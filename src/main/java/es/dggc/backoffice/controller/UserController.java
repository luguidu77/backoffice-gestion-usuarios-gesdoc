package es.dggc.backoffice.controller;

import es.dggc.backoffice.model.dto.UserListResponse;
import es.dggc.backoffice.service.AlfrescoUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para operaciones de gestión de usuarios.
 * 
 * Endpoints:
 *   GET /api/users - Listar usuarios de Alfresco
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
     *   - maxItems: número máximo de usuarios a obtener (por defecto 100)
     *   - skipCount: número de usuarios a omitir (para paginación, por defecto 0)
     * 
     * @param authHeader Header de autorización (Basic Auth token)
     * @param maxItems Número máximo de usuarios a obtener
     * @param skipCount Número de usuarios a omitir
     * @return UserListResponse con la lista de usuarios
     */
    @GetMapping
    public ResponseEntity<UserListResponse> listUsers(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "maxItems", required = false, defaultValue = "100") Integer maxItems,
            @RequestParam(value = "skipCount", required = false, defaultValue = "0") Integer skipCount
    ) {
        log.info("Solicitud de listado de usuarios (maxItems={}, skipCount={})", maxItems, skipCount);

        // Validar header de autorización
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            log.warn("Solicitud sin header de autorización válido");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new UserListResponse());
        }

        // Extraer token (quitar "Basic ")
        String token = authHeader.substring(6);

        // Obtener lista de usuarios
        UserListResponse response = alfrescoUserService.listUsers(token, maxItems, skipCount);

        if (response.getUsers() != null && !response.getUsers().isEmpty()) {
            log.info("Listado de usuarios exitoso: {} usuarios", response.getUsers().size());
            return ResponseEntity.ok(response);
        } else {
            log.warn("No se encontraron usuarios o error en la petición");
            return ResponseEntity.ok(response); // Devolver lista vacía con 200 OK
        }
    }

    /**
     * Endpoint para obtener información de un usuario específico.
     * 
     * GET /api/users/{userId}
     * Header: Authorization: Basic {token}
     * 
     * @param authHeader Header de autorización
     * @param userId ID del usuario a buscar
     * @return UserDto con información del usuario
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String userId
    ) {
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
}
