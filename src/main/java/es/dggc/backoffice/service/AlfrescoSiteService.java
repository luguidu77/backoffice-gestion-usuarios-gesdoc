package es.dggc.backoffice.service;

import es.dggc.backoffice.config.AlfrescoProperties;
import es.dggc.backoffice.model.dto.AlfrescoSitesListResponse;
import es.dggc.backoffice.model.dto.AlfrescoUserSiteListResponse;
import es.dggc.backoffice.model.dto.SiteListResponse;
import es.dggc.backoffice.model.dto.SiteListResponse.SiteDto;
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
 * Servicio para gestión de sitios en Alfresco.
 *
 * Utiliza la API REST v1 de Alfresco para listar sitios.
 */
@Service
public class AlfrescoSiteService {

    private static final Logger log = LoggerFactory.getLogger(AlfrescoSiteService.class);

    private final RestTemplate restTemplate;
    private final AlfrescoProperties alfrescoProperties;

    public AlfrescoSiteService(RestTemplate restTemplate, AlfrescoProperties alfrescoProperties) {
        this.restTemplate = restTemplate;
        this.alfrescoProperties = alfrescoProperties;
    }

    /**
     * Obtiene la lista de sitios de Alfresco.
     *
     * @param basicAuthToken Token de autenticación Basic Auth (Base64 de
     *                       username:password)
     * @param maxItems       Número máximo de sitios a obtener
     * @param skipCount      Número de sitios a omitir (para paginación)
     * @return SiteListResponse con la lista de sitios
     */
    public SiteListResponse listSites(String basicAuthToken, Integer maxItems, Integer skipCount) {
        try {
            log.info("Obteniendo lista de sitios de Alfresco (maxItems={}, skipCount={})", maxItems, skipCount);

            // Construir URL con parámetros de paginación
            String url = alfrescoProperties.getBaseUrl() +
                    "/api/-default-/public/alfresco/versions/1/sites";

            if (maxItems != null || skipCount != null) {
                url += "?";
                if (maxItems != null) {
                    url += "maxItems=" + maxItems;
                }
                if (skipCount != null) {
                    if (maxItems != null)
                        url += "&";
                    url += "skipCount=" + skipCount;
                }
            }

            // Crear headers con Basic Auth
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuthToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Hacer petición GET
            ResponseEntity<AlfrescoSitesListResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    AlfrescoSitesListResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                AlfrescoSitesListResponse alfrescoResponse = response.getBody();

                List<SiteDto> sites = new ArrayList<>();

                if (alfrescoResponse != null && alfrescoResponse.getList() != null
                        && alfrescoResponse.getList().getEntries() != null) {
                    sites = alfrescoResponse.getList().getEntries().stream()
                            .map(wrapper -> {
                                AlfrescoSitesListResponse.SiteEntry entry = wrapper.getEntry();
                                return new SiteDto(
                                        entry.getId(),
                                        entry.getTitle(),
                                        entry.getDescription(),
                                        entry.getVisibility());
                            })
                            .collect(Collectors.toList());
                }

                Integer totalItems = (alfrescoResponse != null && alfrescoResponse.getList() != null &&
                        alfrescoResponse.getList().getPagination() != null)
                                ? alfrescoResponse.getList().getPagination().getTotalItems()
                                : sites.size();

                Boolean hasMore = (alfrescoResponse != null && alfrescoResponse.getList() != null &&
                        alfrescoResponse.getList().getPagination() != null)
                                ? alfrescoResponse.getList().getPagination().getHasMoreItems()
                                : false;

                log.info("Sitios obtenidos exitosamente: {} de {} totales", sites.size(), totalItems);
                return new SiteListResponse(sites, totalItems, hasMore);
            }

            log.warn("No se pudo obtener la lista de sitios. Status: {}", response.getStatusCode());
            return new SiteListResponse(new ArrayList<>(), 0, false);

        } catch (HttpClientErrorException e) {
            log.error("Error HTTP al obtener sitios: {} - {}", e.getStatusCode(), e.getMessage());
            return new SiteListResponse(new ArrayList<>(), 0, false);
        } catch (Exception e) {
            log.error("Error inesperado al obtener sitios: {}", e.getMessage(), e);
            return new SiteListResponse(new ArrayList<>(), 0, false);
        }
    }

    /**
     * Obtiene la lista de sitios de Alfresco a los que pertenece un usuario.
     * Útil para Unit Admins que solo deben ver los sitios que gestionan o donde
     * participan.
     */
    public SiteListResponse listUserSites(String basicAuthToken, String userId, Integer maxItems, Integer skipCount) {
        try {
            log.info("Obteniendo lista de sitios para el usuario {} (maxItems={}, skipCount={})", userId, maxItems,
                    skipCount);

            String url = alfrescoProperties.getBaseUrl() +
                    "/api/-default-/public/alfresco/versions/1/people/" + userId + "/sites";

            if (maxItems != null || skipCount != null) {
                url += "?";
                if (maxItems != null) {
                    url += "maxItems=" + maxItems;
                }
                if (skipCount != null) {
                    if (maxItems != null)
                        url += "&";
                    url += "skipCount=" + skipCount;
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuthToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<AlfrescoUserSiteListResponse> response;
            try {
                response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        AlfrescoUserSiteListResponse.class);
            } catch (HttpClientErrorException.NotFound e) {
                log.warn("El usuario {} no fue encontrado o no tiene sitios asignados.", userId);
                return new SiteListResponse(new ArrayList<>(), 0, false);
            }

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                AlfrescoUserSiteListResponse alfrescoResponse = response.getBody();

                List<SiteDto> sites = new ArrayList<>();

                if (alfrescoResponse != null && alfrescoResponse.getList() != null
                        && alfrescoResponse.getList().getEntries() != null) {
                    sites = alfrescoResponse.getList().getEntries().stream()
                            .map(wrapper -> {
                                AlfrescoSitesListResponse.SiteEntry siteEntry = wrapper.getEntry().getSite();
                                return new SiteDto(
                                        siteEntry.getId(),
                                        siteEntry.getTitle(),
                                        siteEntry.getDescription(),
                                        siteEntry.getVisibility());
                            })
                            .collect(Collectors.toList());
                }

                Integer totalItems = (alfrescoResponse != null && alfrescoResponse.getList() != null &&
                        alfrescoResponse.getList().getPagination() != null)
                                ? alfrescoResponse.getList().getPagination().getTotalItems()
                                : sites.size();

                Boolean hasMore = (alfrescoResponse != null && alfrescoResponse.getList() != null &&
                        alfrescoResponse.getList().getPagination() != null)
                                ? alfrescoResponse.getList().getPagination().getHasMoreItems()
                                : false;

                log.info("Sitios obtenidos exitosamente para el usuario {}: {} de {} totales", userId, sites.size(),
                        totalItems);
                return new SiteListResponse(sites, totalItems, hasMore);
            }

            log.warn("No se pudo obtener la lista de sitios para el usuario {}. Status: {}", userId,
                    response.getStatusCode());
            return new SiteListResponse(new ArrayList<>(), 0, false);

        } catch (HttpClientErrorException e) {
            log.error("Error HTTP al obtener sitios del usuario {}: {} - {}", userId, e.getStatusCode(),
                    e.getMessage());
            return new SiteListResponse(new ArrayList<>(), 0, false);
        } catch (Exception e) {
            log.error("Error inesperado al obtener sitios del usuario {}: {}", userId, e.getMessage(), e);
            return new SiteListResponse(new ArrayList<>(), 0, false);
        }
    }
}
