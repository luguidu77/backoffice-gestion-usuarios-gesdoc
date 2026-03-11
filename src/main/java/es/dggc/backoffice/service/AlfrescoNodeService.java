package es.dggc.backoffice.service;

import es.dggc.backoffice.config.AlfrescoProperties;
import es.dggc.backoffice.model.dto.AlfrescoNodeListResponse;
import es.dggc.backoffice.model.dto.DepartmentListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
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

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                AlfrescoNodeListResponse alfrescoResponse = response.getBody();
                List<DepartmentListResponse.DepartmentDto> departments = new ArrayList<>();

                if (alfrescoResponse.getList() != null && alfrescoResponse.getList().getEntries() != null) {
                    departments = alfrescoResponse.getList().getEntries().stream()
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
}
