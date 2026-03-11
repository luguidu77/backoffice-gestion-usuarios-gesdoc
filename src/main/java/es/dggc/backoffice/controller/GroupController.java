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
}
