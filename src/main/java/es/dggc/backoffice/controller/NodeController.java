package es.dggc.backoffice.controller;

import es.dggc.backoffice.model.dto.DepartmentListResponse;
import es.dggc.backoffice.model.dto.NodePermissionsResponse;
import es.dggc.backoffice.model.dto.UpdateNodePermissionsRequest;
import es.dggc.backoffice.service.AlfrescoNodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/nodes")
@CrossOrigin(origins = "*")
public class NodeController {

    private static final Logger log = LoggerFactory.getLogger(NodeController.class);

    private final AlfrescoNodeService nodeService;

    public NodeController(AlfrescoNodeService nodeService) {
        this.nodeService = nodeService;
    }

    /**
     * Endpoint para listar los 'Departamentos' (carpetas hijas directas del
     * documentLibrary de un Sitio)
     *
     * GET /api/nodes/sites/{siteId}/departments
     * Header: Authorization: Basic {token}
     */
    @GetMapping("/sites/{siteId}/departments")
    public ResponseEntity<DepartmentListResponse> listDepartments(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String siteId,
            @RequestParam(value = "maxItems", required = false, defaultValue = "100") Integer maxItems,
            @RequestParam(value = "skipCount", required = false, defaultValue = "0") Integer skipCount) {

        log.info("Solicitud de listado de departamentos para el sitio {} (maxItems={}, skipCount={})", siteId, maxItems,
                skipCount);

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            log.warn("Solicitud sin header de autorización válido");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new DepartmentListResponse());
        }

        String token = authHeader.substring(6);
        DepartmentListResponse response = nodeService.listSiteDepartments(token, siteId, maxItems, skipCount);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene los permisos del nodo (locales y heredados).
     *
     * GET /api/nodes/{nodeId}/permissions
     * Header: Authorization: Basic {token}
     */
    @GetMapping("/{nodeId}/permissions")
    public ResponseEntity<NodePermissionsResponse> getNodePermissions(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String nodeId) {

        log.info("Solicitud de permisos para el nodo {}", nodeId);

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(6);
        NodePermissionsResponse response = nodeService.getNodePermissions(token, nodeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Actualiza los permisos de un nodo.
     * Puede cambiar la herencia y/o reemplazar la lista de permisos locales.
     *
     * PUT /api/nodes/{nodeId}/permissions
     * Header: Authorization: Basic {token}
     * Body: { "isInheritanceEnabled": bool, "locallySet": [...] }
     */
    @PutMapping("/{nodeId}/permissions")
    public ResponseEntity<?> updateNodePermissions(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String nodeId,
            @RequestBody UpdateNodePermissionsRequest request) {

        log.info("Actualización de permisos para el nodo {}", nodeId);

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autorizado");
        }

        try {
            String token = authHeader.substring(6);
            NodePermissionsResponse response = nodeService.updateNodePermissions(token, nodeId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error actualizando permisos del nodo {}: {}", nodeId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/sites/{siteId}/departments")
    public ResponseEntity<?> createDepartment(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String siteId,
            @RequestBody Map<String, String> request) {

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autorizado");
        }

        String name = request != null ? request.get("name") : null;
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("El nombre del departamento es obligatorio");
        }

        try {
            String token = authHeader.substring(6);
            DepartmentListResponse.DepartmentDto created = nodeService.createDepartment(token, siteId, name);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            log.error("Error creando departamento en sitio {}: {}", siteId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{nodeId}/name")
    public ResponseEntity<?> renameDepartment(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String nodeId,
            @RequestBody Map<String, String> request) {

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autorizado");
        }

        String name = request != null ? request.get("name") : null;
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("El nuevo nombre es obligatorio");
        }

        try {
            String token = authHeader.substring(6);
            nodeService.renameDepartment(token, nodeId, name);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error renombrando departamento {}: {}", nodeId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/{nodeId}")
    public ResponseEntity<?> deleteDepartment(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String nodeId) {

        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autorizado");
        }

        try {
            String token = authHeader.substring(6);
            nodeService.deleteDepartment(token, nodeId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Error eliminando departamento {}: {}", nodeId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
