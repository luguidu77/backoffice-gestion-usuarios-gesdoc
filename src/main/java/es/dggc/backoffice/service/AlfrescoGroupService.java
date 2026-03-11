package es.dggc.backoffice.service;

import es.dggc.backoffice.config.AlfrescoProperties;
import es.dggc.backoffice.model.dto.AlfrescoGroupListResponse;
import es.dggc.backoffice.model.dto.AlfrescoGroupMembersListResponse;
import es.dggc.backoffice.model.dto.GroupAdminResponse;
import es.dggc.backoffice.model.dto.GroupAdminResponse.GroupItem;
import es.dggc.backoffice.model.dto.GroupAdminResponse.GroupListResponse;
import es.dggc.backoffice.model.dto.GroupAdminResponse.GroupMemberItem;
import es.dggc.backoffice.model.dto.GroupAdminResponse.GroupMembersResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para consulta y administración de grupos de Alfresco (GROUP_*).
 */
@Service
public class AlfrescoGroupService {

    private static final Logger log = LoggerFactory.getLogger(AlfrescoGroupService.class);

    private final RestTemplate restTemplate;
    private final AlfrescoProperties alfrescoProperties;

    public AlfrescoGroupService(RestTemplate restTemplate, AlfrescoProperties alfrescoProperties) {
        this.restTemplate = restTemplate;
        this.alfrescoProperties = alfrescoProperties;
    }

    /**
     * Lista los grupos de Alfresco.
     * Por defecto filtra only isRoot=true para mostrar los grupos raíz principales.
     *
     * @param basicAuthToken Token Basic Auth
     * @param onlyRoot       Si true, solo devuelve grupos raíz (isRoot=true)
     * @param searchTerm     Filtro de búsqueda por displayName (opcional)
     * @param maxItems       Paginación
     * @param skipCount      Paginación
     */
    public GroupListResponse listGroups(String basicAuthToken, boolean onlyRoot, String searchTerm,
            Integer maxItems, Integer skipCount) {

        log.info("Listando grupos de Alfresco (onlyRoot={}, searchTerm={})", onlyRoot, searchTerm);

        String coreBase = alfrescoProperties.getBaseUrl()
                + alfrescoProperties.getApi().getCore();

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(coreBase + "/groups")
                .queryParam("orderBy", "displayName ASC")
                .queryParam("maxItems", maxItems != null ? maxItems : 200)
                .queryParam("skipCount", skipCount != null ? skipCount : 0);

        if (onlyRoot) {
            builder.queryParam("where", "(isRoot=true)");
        }

        String url = builder.build().toUriString();
        log.info("URL grupos: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + basicAuthToken);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            ResponseEntity<AlfrescoGroupListResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET,
                    new HttpEntity<>(headers), AlfrescoGroupListResponse.class);

            if (response.getBody() != null && response.getBody().getList() != null
                    && response.getBody().getList().getEntries() != null) {

                List<GroupItem> groups = response.getBody().getList().getEntries().stream()
                        .map(w -> {
                            AlfrescoGroupListResponse.GroupEntry e = w.getEntry();
                            return new GroupItem(
                                    e.getId(),
                                    e.getDisplayName() != null ? e.getDisplayName() : e.getId(),
                                    Boolean.TRUE.equals(e.getIsRoot()),
                                    0);
                        })
                        .filter(g -> searchTerm == null || searchTerm.isBlank()
                                || g.getDisplayName().toLowerCase().contains(searchTerm.toLowerCase())
                                || g.getId().toLowerCase().contains(searchTerm.toLowerCase()))
                        .collect(Collectors.toList());

                return new GroupListResponse(groups, groups.size(), false);
            }

            return new GroupListResponse(new ArrayList<>(), 0, false);

        } catch (HttpClientErrorException e) {
            log.error("Error HTTP al listar grupos [{}]: {} - {}", url, e.getStatusCode(), e.getResponseBodyAsString());
            // Propagar 401/403 al controlador
            if (e.getStatusCode() == HttpStatus.FORBIDDEN
                    || e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw e;
            }
            throw new RuntimeException("Alfresco respondió " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Error al listar grupos [{}]: {}", url, e.getMessage(), e);
            throw new RuntimeException("Error al contactar con Alfresco: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene los miembros (usuarios y subgrupos) de un grupo.
     * GET /groups/{groupId}/members
     */
    public GroupMembersResponse getGroupMembers(String basicAuthToken, String groupId,
            Integer maxItems, Integer skipCount) {
        try {
            log.info("Obteniendo miembros del grupo {}", groupId);

            String url = alfrescoProperties.getBaseUrl() +
                    "/api/-default-/public/alfresco/versions/1/groups/" + groupId + "/members" +
                    "?maxItems=" + (maxItems != null ? maxItems : 200) +
                    "&skipCount=" + (skipCount != null ? skipCount : 0);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuthToken);

            ResponseEntity<AlfrescoGroupMembersListResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET,
                    new HttpEntity<>(headers), AlfrescoGroupMembersListResponse.class);

            if (response.getBody() != null && response.getBody().getList() != null
                    && response.getBody().getList().getEntries() != null) {

                List<GroupMemberItem> members = response.getBody().getList().getEntries().stream()
                        .map(w -> {
                            AlfrescoGroupMembersListResponse.MemberEntry e = w.getEntry();
                            return new GroupMemberItem(
                                    e.getId(),
                                    e.getDisplayName() != null ? e.getDisplayName() : e.getId(),
                                    e.getMemberType());
                        })
                        .collect(Collectors.toList());

                // Nombre legible del grupo: quitar prefijo GROUP_ si existe
                String displayName = groupId.startsWith("GROUP_") ? groupId.substring(6) : groupId;

                return new GroupMembersResponse(groupId, displayName, members, members.size());
            }

            return new GroupMembersResponse(groupId, groupId, new ArrayList<>(), 0);

        } catch (HttpClientErrorException e) {
            log.error("Error HTTP al obtener miembros del grupo {}: {}", groupId, e.getMessage());
            return new GroupMembersResponse(groupId, groupId, new ArrayList<>(), 0);
        } catch (Exception e) {
            log.error("Error al obtener miembros del grupo {}: {}", groupId, e.getMessage(), e);
            return new GroupMembersResponse(groupId, groupId, new ArrayList<>(), 0);
        }
    }
}
