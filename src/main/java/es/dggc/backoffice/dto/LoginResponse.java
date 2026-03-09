package es.dggc.backoffice.dto;

/**
 * DTO para la respuesta de login exitoso.
 * Contiene la información del usuario autenticado y sus credenciales
 * para futuras peticiones a Alfresco.
 */
public class LoginResponse {

    private String username;
    private String displayName;
    private String email;
    private String ticket;          // Ticket de Alfresco (si se usa autenticación por ticket)
    private boolean success;
    private String message;

    // Constructores
    public LoginResponse() {}

    public LoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Builder pattern para construcción fluida
    public static LoginResponse success(String username, String displayName, String email, String ticket) {
        LoginResponse response = new LoginResponse();
        response.setSuccess(true);
        response.setUsername(username);
        response.setDisplayName(displayName);
        response.setEmail(email);
        response.setTicket(ticket);
        response.setMessage("Autenticación exitosa");
        return response;
    }

    public static LoginResponse error(String message) {
        LoginResponse response = new LoginResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }

    // Getters y Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
