package es.dggc.backoffice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Alfresco cargada desde application.properties.
 * 
 * Spring Boot lee automáticamente todas las propiedades que empiecen por "alfresco"
 * y las mapea a los campos de esta clase.
 * 
 * Ejemplo:
 *   alfresco.base-url=http://localhost:8080/alfresco
 *   → se asigna a baseUrl
 */
@Configuration
@ConfigurationProperties(prefix = "alfresco")
public class AlfrescoProperties {

    private String baseUrl;
    private Api api = new Api();
    private Connection connection = new Connection();

    // ============================================================
    // CLASE ANIDADA: API
    // ============================================================

    public static class Api {
        private String authentication;
        private String core;

        public String getAuthentication() {
            return authentication;
        }

        public void setAuthentication(String authentication) {
            this.authentication = authentication;
        }

        public String getCore() {
            return core;
        }

        public void setCore(String core) {
            this.core = core;
        }
    }

    // ============================================================
    // CLASE ANIDADA: CONNECTION
    // ============================================================

    public static class Connection {
        private int timeout = 5000;

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }

    // ============================================================
    // GETTERS & SETTERS
    // ============================================================

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    /**
     * Construye la URL completa del endpoint de autenticación.
     * @return URL completa, ej: http://localhost:8080/alfresco/api/-default-/public/authentication/versions/1
     */
    public String getAuthenticationUrl() {
        return baseUrl + api.getAuthentication();
    }

    /**
     * Construye la URL completa del endpoint core API.
     * @return URL completa, ej: http://localhost:8080/alfresco/api/-default-/public/alfresco/versions/1
     */
    public String getCoreApiUrl() {
        return baseUrl + api.getCore();
    }
}
