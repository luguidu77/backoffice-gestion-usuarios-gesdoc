package es.dggc.backoffice.service;

import es.dggc.backoffice.config.AlfrescoProperties;
import es.dggc.backoffice.model.dto.AlfrescoPersonResponse;
import es.dggc.backoffice.model.dto.AlfrescoPeopleListResponse;
import es.dggc.backoffice.model.dto.UserListResponse;
import es.dggc.backoffice.model.dto.UserListResponse.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de usuarios en Alfresco.
 * 
 * Utiliza la API REST v1 de Alfresco para listar usuarios.
 */
@Service
public class AlfrescoUserService {

    private static final Logger log = LoggerFactory.getLogger(AlfrescoUserService.class);

    private final RestTemplate restTemplate;
    private final AlfrescoProperties alfrescoProperties;

    public AlfrescoUserService(RestTemplate restTemplate, AlfrescoProperties alfrescoProperties) {
        this.restTemplate = restTemplate;
        this.alfrescoProperties = alfrescoProperties;
    }

    /**
     * Obtiene la lista de usuarios de Alfresco.
     * 
     * @param basicAuthToken Token de autenticación Basic Auth (Base64 de username:password)
     * @param maxItems Número máximo de usuarios a obtener (por defecto 100)
     * @param skipCount Número de usuarios a omitir (para paginación)
     * @return UserListResponse con la lista de usuarios
     */
    public UserListResponse listUsers(String basicAuthToken, Integer maxItems, Integer skipCount) {
        try {
            log.info("Obteniendo lista de usuarios de Alfresco (maxItems={}, skipCount={})", maxItems, skipCount);

            // Construir URL con parámetros de paginación
            String url = alfrescoProperties.getBaseUrl() + 
                         "/api/-default-/public/alfresco/versions/1/people";
            
            if (maxItems != null || skipCount != null) {
                url += "?";
                if (maxItems != null) {
                    url += "maxItems=" + maxItems;
                }
                if (skipCount != null) {
                    if (maxItems != null) url += "&";
                    url += "skipCount=" + skipCount;
                }
            }

            // Crear headers con Basic Auth
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + basicAuthToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Hacer petición GET
            ResponseEntity<AlfrescoPeopleListResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                AlfrescoPeopleListResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                AlfrescoPeopleListResponse alfrescoResponse = response.getBody();
                
                // Convertir respuesta de Alfresco a nuestro DTO simplificado
                List<UserDto> users = new ArrayList<>();
                
                if (alfrescoResponse.getList() != null && alfrescoResponse.getList().getEntries() != null) {
                    users = alfrescoResponse.getList().getEntries().stream()
                        .map(wrapper -> {
                            AlfrescoPersonResponse.PersonEntry person = wrapper.getEntry();
                            return new UserDto(
                                person.getId(),
                                person.getFirstName(),
                                person.getLastName(),
                                person.getEmail(),
                                person.getEnabled()
                            );
                        })
                        .collect(Collectors.toList());
                }

                Integer totalItems = alfrescoResponse.getList() != null && 
                                   alfrescoResponse.getList().getPagination() != null ?
                                   alfrescoResponse.getList().getPagination().getTotalItems() : users.size();
                
                Boolean hasMore = alfrescoResponse.getList() != null && 
                                alfrescoResponse.getList().getPagination() != null ?
                                alfrescoResponse.getList().getPagination().getHasMoreItems() : false;

                log.info("Usuarios obtenidos exitosamente: {} de {} totales", users.size(), totalItems);
                return new UserListResponse(users, totalItems, hasMore);
            }

            log.warn("No se pudo obtener la lista de usuarios. Status: {}", response.getStatusCode());
            return new UserListResponse(new ArrayList<>(), 0, false);

        } catch (HttpClientErrorException e) {
            log.error("Error HTTP al obtener usuarios: {} - {}", e.getStatusCode(), e.getMessage());
            return new UserListResponse(new ArrayList<>(), 0, false);
        } catch (Exception e) {
            log.error("Error inesperado al obtener usuarios: {}", e.getMessage(), e);
            return new UserListResponse(new ArrayList<>(), 0, false);
        }
    }

    /**
     * Obtiene la lista de usuarios con valores por defecto de paginación.
     * 
     * @param basicAuthToken Token de autenticación Basic Auth
     * @return UserListResponse con la lista de usuarios (máximo 100)
     */
    public UserListResponse listUsers(String basicAuthToken) {
        return listUsers(basicAuthToken, 100, 0);
    }
}
