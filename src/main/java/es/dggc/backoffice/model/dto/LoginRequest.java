package es.dggc.backoffice.model.dto;

/**
 * DTO para la petición de login.
 * 
 * El frontend envía este objeto al endpoint POST /api/auth/login
 * conteniendo las credenciales del usuario.
 */
public class LoginRequest {

    private String username;
    private String password;

    // ============================================================
    // CONSTRUCTORES
    // ============================================================

    public LoginRequest() {
    }

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // ============================================================
    // GETTERS & SETTERS
    // ============================================================

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "LoginRequest{username='" + username + "'}";
    }
}
