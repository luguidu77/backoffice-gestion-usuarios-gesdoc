package es.dggc.backoffice.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO que mapea la respuesta de Alfresco al crear un ticket.
 * 
 * Endpoint: POST /api/-default-/public/authentication/versions/1/tickets
 * 
 * Ejemplo de respuesta JSON de Alfresco:
 * {
 *   "entry": {
 *     "id": "TICKET_abc123...",
 *     "userId": "admin"
 *   }
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlfrescoTicketResponse {

    private EntryData entry;

    public EntryData getEntry() {
        return entry;
    }

    public void setEntry(EntryData entry) {
        this.entry = entry;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EntryData {
        private String id;      // El ticket
        private String userId;  // El username

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
    }
}
