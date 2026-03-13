package es.dggc.backoffice.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
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

    private static final String CERT_ALIAS = "alfresco-server-cert";

    @Value("${alfresco.connection-timeout:10000}")
    private int connectionTimeout;

    @Value("${alfresco.read-timeout:30000}")
    private int readTimeout;

    @Value("${alfresco.ssl.enabled:true}")
    private boolean sslEnabled;

    @Value("${alfresco.ssl.certificate:}")
    private String sslCertificate;

    private final ResourceLoader resourceLoader;

    public AlfrescoConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

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
        if (sslEnabled && sslCertificate != null && !sslCertificate.trim().isEmpty()) {
            return buildSslRestTemplate();
        }

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectionTimeout);
        factory.setReadTimeout(readTimeout);
        
        return new RestTemplate(factory);
    }

    private RestTemplate buildSslRestTemplate() {
        try (InputStream certificateStream = openCertificateStream(sslCertificate.trim())) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            Certificate certificate = certificateFactory.generateCertificate(certificateStream);

            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            trustStore.setCertificateEntry(CERT_ALIAS, certificate);

            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(trustStore, null)
                    .build();

            SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                    sslContext,
                    new String[] { "TLSv1.2", "TLSv1.3" },
                    null,
                    SSLConnectionSocketFactory.getDefaultHostnameVerifier());

            HttpClient httpClient = HttpClients.custom()
                    .setSSLSocketFactory(socketFactory)
                    .build();

            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
            factory.setConnectTimeout(connectionTimeout);
            factory.setReadTimeout(readTimeout);

            return new RestTemplate(factory);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo inicializar SSL para Alfresco con el certificado configurado: " + sslCertificate, ex);
        }
    }

    private InputStream openCertificateStream(String certificateLocation) throws IOException {
        if (certificateLocation.startsWith("classpath:") || certificateLocation.startsWith("file:")) {
            Resource resource = resourceLoader.getResource(certificateLocation);
            return resource.getInputStream();
        }

        return new FileInputStream(certificateLocation);
    }
}
