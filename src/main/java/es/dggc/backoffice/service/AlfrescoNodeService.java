package es.dggc.backoffice.service;

import es.dggc.backoffice.config.AlfrescoProperties;
import es.dggc.backoffice.model.dto.AlfrescoNodeListResponse;
import es.dggc.backoffice.model.dto.AlfrescoNodeSingleResponse;
import es.dggc.backoffice.model.dto.DepartmentListResponse;
import es.dggc.backoffice.model.dto.NodePermissionsResponse;
import es.dggc.backoffice.model.dto.NodePermissionsResponse.PermissionEntry;
import es.dggc.backoffice.model.dto.UpdateNodePermissionsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de nodos (archivos y carpetas) en Alfresco.
 */
@Service
public class AlfrescoNodeService {

    private static final Logger log = LoggerFactory.getLogger(AlfrescoNodeService.class);

    private final RestTemplate restTemplate;
    private final AlfrescoProperties alfrescoProperties;

    public AlfrescoNodeService(RestTemplate restTemplate, AlfrescoProperties alfrescoProperties) {
        this.restTemplate = restTemplate;
        this.alfrescoProperties = alfrescoProperties;
    }

    /**
     * Obtiene las carpetas directas del documentLibrary de un sitio.
     * En nuestro modelo, estas carpetas de primer nivel representan los
     * "Departamentos".
     *
     * @param basicAuthToken Token de autenticación Basic Auth
     * @param siteId         ID del sitio (Unidad)
     * @param maxItems       Paginación (maxItems)
     * @param skipCount      Paginación (skipCount)
     * @return DepartmentListResponse
     */
    public DepartmentListResponse listSiteDepartments(String basicAuthToken, String siteId, Integer maxItems,
            Integer skipCount) {
        try {
            log.info("Obteniendo departamentos (carpetas) para el documentLibrary del sitio {}", siteId);

            // Usamos el alias -root- de documentLibrary asociado al sitio
            String url = alfrescoProperties.getBaseUrl() +
                    "/api/-default-/public/alfresco/versions/1/nodes/" +
                    "-root-/children?relativePath=Sites/" + siteId + "/documentLibrary" +
                    "&where=(isFolder=true)";

            // Añadimos la inclusión de permisos para ver la herencia
            url += "&include=permissions";

            if (maxItems != null)
                url += "&maxItems=" + maxItems;
            if (skipCount != null)
                url += "&skipCount=" + skipCount;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuthToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<AlfrescoNodeListResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, AlfrescoNodeListResponse.class);

            AlfrescoNodeListResponse alfrescoResponse = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && alfrescoResponse != null) {
                List<DepartmentListResponse.DepartmentDto> departments = new ArrayList<>();

                if (alfrescoResponse.getList() != null && alfrescoResponse.getList().getEntries() != null) {
                    departments = alfrescoResponse.getList().getEntries().stream()
                            .filter(wrapper -> wrapper.getEntry() != null)
                            .map(wrapper -> {
                                AlfrescoNodeListResponse.NodeEntry node = wrapper.getEntry();

                                boolean inherits = true;
                                if (node.getPermissions() != null) {
                                    inherits = node.getPermissions().isInheritanceEnabled();
                                }

                                return new DepartmentListResponse.DepartmentDto(
                                        node.getId(),
                                        node.getName(),
                                        node.getParentId(),
                                        siteId,
                                        inherits);
                            })
                            .collect(Collectors.toList());
                }

                Integer totalItems = alfrescoResponse.getList() != null &&
                        alfrescoResponse.getList().getPagination() != null
                                ? alfrescoResponse.getList().getPagination().getTotalItems()
                                : departments.size();

                Boolean hasMore = alfrescoResponse.getList() != null &&
                        alfrescoResponse.getList().getPagination() != null
                                ? alfrescoResponse.getList().getPagination().isHasMoreItems()
                                : false;

                return new DepartmentListResponse(departments, totalItems, hasMore);
            }

            return new DepartmentListResponse(new ArrayList<>(), 0, false);

        } catch (HttpClientErrorException e) {
            log.error("Error HTTP al obtener departamentos del sitio {}: {} - {}", siteId, e.getStatusCode(),
                    e.getMessage());
            return new DepartmentListResponse(new ArrayList<>(), 0, false);
        } catch (Exception e) {
            log.error("Error inesperado al obtener departamentos del sitio {}: {}", siteId, e.getMessage(), e);
            return new DepartmentListResponse(new ArrayList<>(), 0, false);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GESTIÓN DE PERMISOS
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Obtiene los permisos actuales de un nodo (locales y heredados).
     * GET /nodes/{nodeId}?include=permissions
     */
    public NodePermissionsResponse getNodePermissions(String basicAuthToken, String nodeId) {
        try {
            log.info("Obteniendo permisos del nodo {}", nodeId);

            String url = alfrescoProperties.getBaseUrl() +
                    "/api/-default-/public/alfresco/versions/1/nodes/" + nodeId + "?include=permissions";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuthToken);

            ResponseEntity<AlfrescoNodeSingleResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), AlfrescoNodeSingleResponse.class);

            AlfrescoNodeSingleResponse nodeBody = response.getBody();
            if (nodeBody != null && nodeBody.getEntry() != null) {
                AlfrescoNodeListResponse.NodeEntry node = nodeBody.getEntry();
                AlfrescoNodeListResponse.PermissionsInfo perms = node.getPermissions();

                boolean inherits = perms == null || perms.isInheritanceEnabled();
                List<PermissionEntry> locallySet = mapPermissions(perms != null ? perms.getLocallySet() : null);
                List<PermissionEntry> inherited = mapPermissions(perms != null ? perms.getInherited() : null);

                return new NodePermissionsResponse(node.getId(), node.getName(), inherits, locallySet, inherited);
            }

            return new NodePermissionsResponse(nodeId, nodeId, true, new ArrayList<>(), new ArrayList<>());
        } catch (HttpClientErrorException e) {
            log.error("Error HTTP al obtener permisos del nodo {}: {}", nodeId, e.getMessage());
            return new NodePermissionsResponse(nodeId, nodeId, true, new ArrayList<>(), new ArrayList<>());
        } catch (Exception e) {
            log.error("Error al obtener permisos del nodo {}: {}", nodeId, e.getMessage(), e);
            return new NodePermissionsResponse(nodeId, nodeId, true, new ArrayList<>(), new ArrayList<>());
        }
    }

    /**
     * Actualiza los permisos de un nodo.
     * Reemplaza el array completo de permisos locales y/o actualiza la herencia.
     * PUT /nodes/{nodeId}
     */
    public NodePermissionsResponse updateNodePermissions(String basicAuthToken, String nodeId,
            UpdateNodePermissionsRequest request) {
        try {
            log.info("Actualizando permisos del nodo {} (inheritance={})", nodeId, request.getIsInheritanceEnabled());

            String url = alfrescoProperties.getBaseUrl() +
                    "/api/-default-/public/alfresco/versions/1/nodes/" + nodeId + "?include=permissions";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuthToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Construir el body que espera Alfresco
            Map<String, Object> permissionsBody = new HashMap<>();
            if (request.getIsInheritanceEnabled() != null) {
                permissionsBody.put("isInheritanceEnabled", request.getIsInheritanceEnabled());
            }
            if (request.getLocallySet() != null) {
                List<Map<String, String>> locallySet = new ArrayList<>();
                for (UpdateNodePermissionsRequest.PermissionItem item : request.getLocallySet()) {
                    Map<String, String> p = new HashMap<>();
                    p.put("authorityId", item.getAuthorityId());
                    p.put("name", item.getName());
                    p.put("accessStatus", item.getAccessStatus());
                    locallySet.add(p);
                }
                permissionsBody.put("locallySet", locallySet);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("permissions", permissionsBody);

            ResponseEntity<AlfrescoNodeSingleResponse> response = restTemplate.exchange(
                    url, HttpMethod.PUT, new HttpEntity<>(body, headers), AlfrescoNodeSingleResponse.class);

            AlfrescoNodeSingleResponse updatedBody = response.getBody();
            if (updatedBody != null && updatedBody.getEntry() != null) {
                AlfrescoNodeListResponse.NodeEntry node = updatedBody.getEntry();
                AlfrescoNodeListResponse.PermissionsInfo perms = node.getPermissions();

                boolean inherits = perms == null || perms.isInheritanceEnabled();
                List<PermissionEntry> locallySet = mapPermissions(perms != null ? perms.getLocallySet() : null);
                List<PermissionEntry> inherited = mapPermissions(perms != null ? perms.getInherited() : null);

                return new NodePermissionsResponse(node.getId(), node.getName(), inherits, locallySet, inherited);
            }

            // Si la actualización fue correcta pero sin cuerpo, releer
            return getNodePermissions(basicAuthToken, nodeId);

        } catch (HttpClientErrorException e) {
            log.error("Error HTTP al actualizar permisos del nodo {}: {} - {}", nodeId, e.getStatusCode(), e.getMessage());
            throw new RuntimeException("Error al actualizar permisos: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Error al actualizar permisos del nodo {}: {}", nodeId, e.getMessage(), e);
            throw new RuntimeException("Error al actualizar permisos del nodo", e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<PermissionEntry> mapPermissions(List<AlfrescoNodeListResponse.PermissionElement> raw) {
        if (raw == null) return new ArrayList<>();
        return raw.stream().map(p -> {
            String authorityId = p.getAuthorityId();
            String type = authorityId != null && authorityId.startsWith("GROUP_") ? "GROUP" : "USER";
            // Nombre legible: quitar prefijo GROUP_ si existe
            String displayName = authorityId != null && authorityId.startsWith("GROUP_")
                    ? authorityId.substring(6) : authorityId;
            return new PermissionEntry(authorityId, displayName, type, p.getName(), p.getAccessStatus());
        }).collect(Collectors.toList());
    }
}
