package es.dggc.backoffice.controller;

import es.dggc.backoffice.model.dto.GroupAdminResponse.GroupListResponse;
import es.dggc.backoffice.model.dto.GroupAdminResponse.GroupMembersResponse;
import es.dggc.backoffice.service.AlfrescoGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import java.util.Map;

/**
 * Controlador REST para la administración de grupos de Alfresco.
 *
 * Endpoints:
 *   GET /api/groups                     – Listado de grupos
 *   GET /api/groups/{groupId}/members   – Miembros de un grupo
 */
@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*")
public class GroupController {

    private static final Logger log = LoggerFactory.getLogger(GroupController.class);

    private final AlfrescoGroupService groupService;

    public GroupController(AlfrescoGroupService groupService) {
        this.groupService = groupService;
    }

    /**
     * Lista los grupos de Alfresco.
     *
     * GET /api/groups
     * Params: onlyRoot (bool, default true), searchTerm, maxItems, skipCount
     */
    @GetMapping
    public ResponseEntity<GroupListResponse> listGroups(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "onlyRoot", required = false, defaultValue = "false") boolean onlyRoot,
            @RequestParam(value = "searchTerm", required = false) String searchTerm,
            @RequestParam(value = "maxItems", required = false, defaultValue = "200") Integer maxItems,
            @RequestParam(value = "skipCount", required = false, defaultValue = "0") Integer skipCount) {

        log.info("Solicitud de listado de grupos (onlyRoot={}, searchTerm={})", onlyRoot, searchTerm);

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String token = authHeader.substring(6);
            GroupListResponse response = groupService.listGroups(token, onlyRoot, searchTerm, maxItems, skipCount);
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException e) {
            log.warn("Acceso denegado por Alfresco al listar grupos: {}", e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (RuntimeException e) {
            log.error("Error listando grupos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtiene los miembros de un grupo dado.
     *
     * GET /api/groups/{groupId}/members
     */
    @GetMapping("/{groupId}/members")
    public ResponseEntity<GroupMembersResponse> getGroupMembers(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String groupId,
            @RequestParam(value = "maxItems", required = false, defaultValue = "200") Integer maxItems,
            @RequestParam(value = "skipCount", required = false, defaultValue = "0") Integer skipCount) {

        log.info("Solicitud de miembros del grupo {}", groupId);

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(6);
        GroupMembersResponse response = groupService.getGroupMembers(token, groupId, maxItems, skipCount);
        return ResponseEntity.ok(response);
    }

    /**
     * Anade un usuario (PERSON) a un grupo.
     *
     * POST /api/groups/{groupId}/members/{userId}
     */
    @PostMapping("/{groupId}/members/{userId}")
    public ResponseEntity<Void> addUserToGroup(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String groupId,
            @PathVariable String userId) {

        log.info("Solicitud para anadir usuario {} al grupo {}", userId, groupId);

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String token = authHeader.substring(6);
            groupService.addPersonToGroup(token, groupId, userId);
            return ResponseEntity.noContent().build();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                log.info("Usuario {} ya pertenece al grupo {}. Se considera exito idempotente.", userId, groupId);
                return ResponseEntity.noContent().build();
            }
            log.warn("Error HTTP anadiendo usuario {} a grupo {}: {}", userId, groupId, e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (RuntimeException e) {
            log.error("Error anadiendo usuario {} a grupo {}: {}", userId, groupId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Quita un usuario (PERSON) de un grupo.
     *
     * DELETE /api/groups/{groupId}/members/{userId}
     */
    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<Void> removeUserFromGroup(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String groupId,
            @PathVariable String userId) {

        log.info("Solicitud para quitar usuario {} del grupo {}", userId, groupId);

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String token = authHeader.substring(6);
            groupService.removePersonFromGroup(token, groupId, userId);
            return ResponseEntity.noContent().build();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.info("Usuario {} no estaba en el grupo {}. Se considera exito idempotente.", userId, groupId);
                return ResponseEntity.noContent().build();
            }
            log.warn("Error HTTP quitando usuario {} de grupo {}: {}", userId, groupId, e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (RuntimeException e) {
            log.error("Error quitando usuario {} de grupo {}: {}", userId, groupId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Variante robusta para alta de membresia evitando userId en path.
     *
     * POST /api/groups/memberships
     * Body: { "groupId": "...", "userId": "..." }
     */
    @PostMapping("/memberships")
    public ResponseEntity<Void> addUserToGroupMembership(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) Map<String, String> body) {

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String groupId = body != null ? body.get("groupId") : null;
        String userId = body != null ? body.get("userId") : null;
        if (groupId == null || groupId.trim().isEmpty() || userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            String token = authHeader.substring(6);
            groupService.addPersonToGroup(token, groupId, userId);
            return ResponseEntity.noContent().build();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                log.info("Usuario {} ya pertenece al grupo {}. Se considera exito idempotente.", userId, groupId);
                return ResponseEntity.noContent().build();
            }
            log.warn("Error HTTP anadiendo usuario {} a grupo {}: {}", userId, groupId, e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (RuntimeException e) {
            log.error("Error anadiendo usuario {} a grupo {}: {}", userId, groupId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Variante robusta para baja de membresia evitando userId en path.
     *
     * DELETE /api/groups/memberships?groupId=...&userId=...
     */
    @DeleteMapping("/memberships")
    public ResponseEntity<Void> removeUserFromGroupMembership(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam("groupId") String groupId,
            @RequestParam("userId") String userId) {

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (groupId == null || groupId.trim().isEmpty() || userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            String token = authHeader.substring(6);
            groupService.removePersonFromGroup(token, groupId, userId);
            return ResponseEntity.noContent().build();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.info("Usuario {} no estaba en el grupo {}. Se considera exito idempotente.", userId, groupId);
                return ResponseEntity.noContent().build();
            }
            log.warn("Error HTTP quitando usuario {} de grupo {}: {}", userId, groupId, e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (RuntimeException e) {
            log.error("Error quitando usuario {} de grupo {}: {}", userId, groupId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
