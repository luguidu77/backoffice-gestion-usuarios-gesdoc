package es.dggc.backoffice.config;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;

/**
 * Configuración de RestTemplate para comunicarse con Alfresco.
 * 
 * RestTemplate es la clase de Spring para hacer peticiones HTTP/HTTPS.
 * La configuramos aquí como un @Bean para poder:
 *   - Configurar timeouts
 *   - Aceptar certificados SSL autofirmados (común en entornos de preproducción)
 *   - Reutilizarla en todo el proyecto
 *   - Añadir interceptors si fuera necesario
 * 
 * IMPORTANTE: Esta configuración deshabilita la verificación SSL para entornos
 * de desarrollo/preproducción. En producción, usar certificados válidos.
 */
@Configuration
public class RestTemplateConfig {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateConfig.class);

    private final AlfrescoProperties alfrescoProperties;

    public RestTemplateConfig(AlfrescoProperties alfrescoProperties) {
        this.alfrescoProperties = alfrescoProperties;
    }

    /**
     * Crea un RestTemplate configurado para comunicarse con Alfresco.
     * 
     * Acepta certificados SSL autofirmados o no confiables.
     * 
     * @return RestTemplate configurado con timeouts y SSL sin validación
     */
    @Bean
    public RestTemplate restTemplate() {
        try {
            // Crear SSLContext que acepta todos los certificados (incluyendo autofirmados)
            SSLContext sslContext = SSLContextBuilder
                .create()
                .loadTrustMaterial((chain, authType) -> true) // Aceptar todos los certificados
                .build();

            // Crear SSLConnectionSocketFactory sin validación de hostname
            SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                sslContext,
                NoopHostnameVerifier.INSTANCE // No verificar hostname
            );

            // Crear HttpClient con la configuración SSL personalizada
            CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(socketFactory)
                .build();

            // Crear factory con timeouts configurados
            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
            factory.setConnectTimeout(alfrescoProperties.getConnection().getTimeout());
            factory.setReadTimeout(alfrescoProperties.getConnection().getTimeout());

            log.info("RestTemplate configurado para aceptar certificados SSL autofirmados");
            log.info("Timeouts: connect={}ms, read={}ms", 
                alfrescoProperties.getConnection().getTimeout(),
                alfrescoProperties.getConnection().getTimeout());

            return new RestTemplate(factory);

        } catch (Exception e) {
            log.error("Error configurando RestTemplate con SSL: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo configurar RestTemplate", e);
        }
    }
}
