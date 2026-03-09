package es.dggc.backoffice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración para la integración con Alfresco REST API v1.
 * 
 * Define un RestTemplate configurado con timeouts apropiados
 * para comunicarse con el repositorio Alfresco.
 */
@Configuration
public class AlfrescoConfig {

    @Value("${alfresco.connection-timeout:10000}")
    private int connectionTimeout;

    @Value("${alfresco.read-timeout:30000}")
    private int readTimeout;

    /**
     * RestTemplate configurado para llamadas a Alfresco.
     * 
     * Incluye:
     * - Connection timeout: tiempo máximo para establecer conexión
     * - Read timeout: tiempo máximo esperando respuesta del servidor
     * 
     * @return RestTemplate configurado
     */
    @Bean
    public RestTemplate alfrescoRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectionTimeout);
        factory.setReadTimeout(readTimeout);
        
        return new RestTemplate(factory);
    }
}
