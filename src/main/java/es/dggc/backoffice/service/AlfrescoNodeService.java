package es.dggc.backoffice.service;

import es.dggc.backoffice.config.AlfrescoProperties;
import es.dggc.backoffice.model.dto.AlfrescoNodeListResponse;
import es.dggc.backoffice.model.dto.AlfrescoNodeSingleResponse;
import es.dggc.backoffice.model.dto.AlfrescoSitesListResponse;
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
        String effectiveSiteId = siteId;

        try {
            log.info("Obteniendo departamentos (carpetas) para el documentLibrary del sitio {}", siteId);

            AlfrescoNodeListResponse alfrescoResponse = requestDepartments(basicAuthToken, effectiveSiteId, maxItems, skipCount);
            return mapDepartments(alfrescoResponse, effectiveSiteId);

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                String resolvedSiteId = resolveSiteIdByMetadata(basicAuthToken, siteId);
                if (resolvedSiteId != null && !resolvedSiteId.equalsIgnoreCase(siteId)) {
                    try {
                        log.info("Reintentando departamentos con siteId resuelto {} para solicitud original {}", resolvedSiteId,
                                siteId);
                        AlfrescoNodeListResponse retryResponse = requestDepartments(basicAuthToken, resolvedSiteId, maxItems,
                                skipCount);
                        return mapDepartments(retryResponse, resolvedSiteId);
                    } catch (Exception retryException) {
                        log.error("Error en reintento de departamentos para sitio {}: {}", resolvedSiteId,
                                retryException.getMessage());
                    }
                }
            }

            log.error("Error HTTP al obtener departamentos del sitio {}: {} - {}", siteId, e.getStatusCode(),
                    e.getMessage());
            return new DepartmentListResponse(new ArrayList<>(), 0, false);
        } catch (Exception e) {
            log.error("Error inesperado al obtener departamentos del sitio {}: {}", siteId, e.getMessage(), e);
            return new DepartmentListResponse(new ArrayList<>(), 0, false);
        }
    }

    private AlfrescoNodeListResponse requestDepartments(String basicAuthToken, String siteId, Integer maxItems,
            Integer skipCount) {
        String documentLibraryNodeId = resolveDocumentLibraryNodeId(basicAuthToken, siteId);

        String url = alfrescoProperties.getBaseUrl() +
            "/api/-default-/public/alfresco/versions/1/nodes/" +
            documentLibraryNodeId +
            "/children?where=(isFolder=true)&include=permissions";

        if (maxItems != null) {
            url += "&maxItems=" + maxItems;
        }
        if (skipCount != null) {
            url += "&skipCount=" + skipCount;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + basicAuthToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<AlfrescoNodeListResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, AlfrescoNodeListResponse.class);

        return response.getBody() != null ? response.getBody() : new AlfrescoNodeListResponse();
    }

    private String resolveDocumentLibraryNodeId(String basicAuthToken, String siteId) {
        String url = alfrescoProperties.getBaseUrl() +
                "/api/-default-/public/alfresco/versions/1/sites/" + siteId + "/containers/documentLibrary";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + basicAuthToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<AlfrescoNodeSingleResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, AlfrescoNodeSingleResponse.class);

        AlfrescoNodeSingleResponse body = response.getBody();
        if (body == null || body.getEntry() == null || body.getEntry().getId() == null
                || body.getEntry().getId().trim().isEmpty()) {
            throw new RuntimeException("No se pudo resolver el contenedor documentLibrary para el sitio " + siteId);
        }

        return body.getEntry().getId().trim();
    }

    private DepartmentListResponse mapDepartments(AlfrescoNodeListResponse alfrescoResponse, String siteId) {
        List<DepartmentListResponse.DepartmentDto> departments = new ArrayList<>();

        if (alfrescoResponse != null && alfrescoResponse.getList() != null
                && alfrescoResponse.getList().getEntries() != null) {
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

        Integer totalItems = alfrescoResponse != null && alfrescoResponse.getList() != null
                && alfrescoResponse.getList().getPagination() != null
                        ? alfrescoResponse.getList().getPagination().getTotalItems()
                        : departments.size();

        Boolean hasMore = alfrescoResponse != null && alfrescoResponse.getList() != null
                && alfrescoResponse.getList().getPagination() != null
                        ? alfrescoResponse.getList().getPagination().isHasMoreItems()
                        : false;

        return new DepartmentListResponse(departments, totalItems, hasMore);
    }

    private String resolveSiteIdByMetadata(String basicAuthToken, String providedSiteId) {
        try {
            String url = alfrescoProperties.getBaseUrl() +
                    "/api/-default-/public/alfresco/versions/1/sites?maxItems=1000&skipCount=0";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuthToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<AlfrescoSitesListResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, AlfrescoSitesListResponse.class);

            AlfrescoSitesListResponse body = response.getBody();
            if (body == null || body.getList() == null || body.getList().getEntries() == null) {
                return null;
            }

            String normalizedInput = normalizeKey(providedSiteId);

            for (AlfrescoSitesListResponse.SiteEntryWrapper wrapper : body.getList().getEntries()) {
                AlfrescoSitesListResponse.SiteEntry entry = wrapper.getEntry();
                if (entry == null) {
                    continue;
                }

                if (equalsIgnoreCase(entry.getId(), providedSiteId)) {
                    return entry.getId();
                }

                if (normalizedInput.equals(normalizeKey(entry.getTitle()))
                        || normalizedInput.equals(normalizeKey(entry.getDescription()))
                        || normalizedInput.equals(normalizeKey(entry.getId()))) {
                    return entry.getId();
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo resolver siteId alternativo para {}: {}", providedSiteId, e.getMessage());
        }

        return null;
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.toUpperCase()
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
                .replace("Ü", "U")
                .replace("Ñ", "N")
                .replaceAll("[^A-Z0-9]", "");
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

    // ──────────────────────────────────────────────────────────────────────────
    // GESTIÓN DE DEPARTAMENTOS (CARPETAS)
    // ──────────────────────────────────────────────────────────────────────────

    public DepartmentListResponse.DepartmentDto createDepartment(String basicAuthToken, String siteId, String name) {
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isEmpty()) {
            throw new RuntimeException("El nombre del departamento es obligatorio");
        }

        try {
            String documentLibraryNodeId = resolveDocumentLibraryNodeId(basicAuthToken, siteId);
            String url = alfrescoProperties.getBaseUrl() +
                    "/api/-default-/public/alfresco/versions/1/nodes/" + documentLibraryNodeId + "/children";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuthToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("name", normalizedName);
            body.put("nodeType", "cm:folder");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<AlfrescoNodeSingleResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, AlfrescoNodeSingleResponse.class);

            AlfrescoNodeSingleResponse payload = response.getBody();
            if (payload == null || payload.getEntry() == null) {
                throw new RuntimeException("Alfresco no devolvió datos al crear el departamento");
            }

            AlfrescoNodeListResponse.NodeEntry entry = payload.getEntry();
            boolean inherits = entry.getPermissions() == null || entry.getPermissions().isInheritanceEnabled();

            return new DepartmentListResponse.DepartmentDto(
                    entry.getId(),
                    entry.getName(),
                    entry.getParentId(),
                    siteId,
                    inherits);
        } catch (Exception e) {
            log.error("Error creando departamento '{}' en sitio {}: {}", normalizedName, siteId, e.getMessage(), e);
            throw new RuntimeException("No se pudo crear el departamento", e);
        }
    }

    public void renameDepartment(String basicAuthToken, String nodeId, String newName) {
        String normalizedName = newName == null ? "" : newName.trim();
        if (normalizedName.isEmpty()) {
            throw new RuntimeException("El nuevo nombre es obligatorio");
        }

        try {
            String url = alfrescoProperties.getBaseUrl() +
                    "/api/-default-/public/alfresco/versions/1/nodes/" + nodeId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuthToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("name", normalizedName);

            restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(body, headers), Void.class);
        } catch (Exception e) {
            log.error("Error renombrando departamento {} a '{}': {}", nodeId, normalizedName, e.getMessage(), e);
            throw new RuntimeException("No se pudo renombrar el departamento", e);
        }
    }

    public void deleteDepartment(String basicAuthToken, String nodeId) {
        try {
            String url = alfrescoProperties.getBaseUrl() +
                    "/api/-default-/public/alfresco/versions/1/nodes/" + nodeId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuthToken);

            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        } catch (Exception e) {
            log.error("Error eliminando departamento {}: {}", nodeId, e.getMessage(), e);
            throw new RuntimeException("No se pudo eliminar el departamento", e);
        }
    }
}
