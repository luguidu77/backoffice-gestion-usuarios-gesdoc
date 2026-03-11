package es.dggc.backoffice.model.dto;

/**
 * DTO para la respuesta de login exitoso.
 * 
 * Contiene el ticket de Alfresco y la información básica del usuario.
 * El frontend guardará el ticket y lo enviará en peticiones posteriores.
 */
public class LoginResponse {

    private boolean success;
    private String ticket;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String message;
    private java.util.List<String> groups;

    // ============================================================
    // CONSTRUCTORES
    // ============================================================

    public LoginResponse() {
    }

    public LoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public LoginResponse(boolean success, String ticket, String username,
            String firstName, String lastName, String email,
            java.util.List<String> groups) {
        this.success = success;
        this.ticket = ticket;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.groups = groups;
    }

    // ============================================================
    // GETTERS & SETTERS
    // ============================================================

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public java.util.List<String> getGroups() {
        return groups;
    }

    public void setGroups(java.util.List<String> groups) {
        this.groups = groups;
    }
}
