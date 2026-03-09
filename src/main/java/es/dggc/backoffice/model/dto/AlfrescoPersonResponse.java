package es.dggc.backoffice.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO que mapea la respuesta de Alfresco al obtener información de un usuario.
 * 
 * Endpoint: GET /api/-default-/public/alfresco/versions/1/people/{personId}
 * 
 * Ejemplo de respuesta JSON de Alfresco:
 * {
 *   "entry": {
 *     "id": "admin",
 *     "firstName": "Administrator",
 *     "lastName": "",
 *     "email": "admin@alfresco.com",
 *     "enabled": true
 *   }
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlfrescoPersonResponse {

    private PersonEntry entry;

    public PersonEntry getEntry() {
        return entry;
    }

    public void setEntry(PersonEntry entry) {
        this.entry = entry;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PersonEntry {
        private String id;
        private String firstName;
        private String lastName;
        private String email;
        private Boolean enabled;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }
}
