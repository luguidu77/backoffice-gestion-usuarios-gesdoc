package es.dggc.backoffice.controller;

import es.dggc.backoffice.model.dto.LoginRequest;
import es.dggc.backoffice.model.dto.LoginResponse;
import es.dggc.backoffice.service.AlfrescoAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para operaciones de autenticación.
 * 
 * Utiliza Basic Authentication (sin tickets de Alfresco).
 * 
 * Endpoints:
 *   POST /api/auth/login - Iniciar sesión (retorna token Basic Auth)
 *   POST /api/auth/logout - Cerrar sesión
 *   GET /api/auth/validate - Validar token
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AlfrescoAuthService alfrescoAuthService;

    public AuthController(AlfrescoAuthService alfrescoAuthService) {
        this.alfrescoAuthService = alfrescoAuthService;
    }

    /**
     * Endpoint de login.
     * 
     * POST /api/auth/login
     * Body: { "username": "admin", "password": "admin" }
     * 
     * Retorna un token Basic Auth (username:password en Base64) que debe
     * incluirse en todas las peticiones subsecuentes.
     * 
     * @param loginRequest Credenciales del usuario
     * @return LoginResponse con token Basic Auth y datos del usuario si es exitoso
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        log.info("Solicitud de login para usuario: {}", loginRequest.getUsername());

        // Validar que los campos no estén vacíos
        if (loginRequest.getUsername() == null || loginRequest.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new LoginResponse(false, "El nombre de usuario es obligatorio"));
        }

        if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new LoginResponse(false, "La contraseña es obligatoria"));
        }

        // Autenticar con Alfresco
        LoginResponse response = alfrescoAuthService.login(
            loginRequest.getUsername(),
            loginRequest.getPassword()
        );

        // Retornar respuesta apropiada según el resultado
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    /**
     * Endpoint de logout.
     * 
     * POST /api/auth/logout
     * Body: { "ticket": "base64_token" }
     * 
     * Con Basic Auth, el token se invalida en el cliente.
     * 
     * @param requestBody Map con el token
     * @return Confirmación del logout
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@RequestBody LogoutRequest requestBody) {
        String ticket = requestBody.getTicket();
        
        if (ticket == null || ticket.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new LogoutResponse(false, "El token es obligatorio"));
        }

        log.info("Solicitud de logout");

        boolean success = alfrescoAuthService.logout(ticket);

        if (success) {
            return ResponseEntity.ok(new LogoutResponse(true, "Sesión cerrada correctamente"));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new LogoutResponse(false, "Error al cerrar sesión"));
        }
    }

    /**
     * Endpoint para validar un token.
     * 
     * GET /api/auth/validate?ticket=base64_token
     * 
     * @param ticket Token Basic Auth a validar
     * @return Estado de validez del token
     */
    @GetMapping("/validate")
    public ResponseEntity<ValidationResponse> validateTicket(@RequestParam String ticket) {
        if (ticket == null || ticket.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(new ValidationResponse(false, "El token es obligatorio"));
        }

        log.debug("Validando token Basic Auth");

        boolean valid = alfrescoAuthService.validateTicket(ticket);

        if (valid) {
            return ResponseEntity.ok(new ValidationResponse(true, "Token válido"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ValidationResponse(false, "Token inválido o expirado"));
        }
    }

    // ========== Clases internas para respuestas ==========

    /**
     * Respuesta del endpoint de logout.
     */
    public static class LogoutResponse {
        private boolean success;
        private String message;

        public LogoutResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
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

    /**
     * Request del endpoint de logout.
     */
    public static class LogoutRequest {
        private String ticket;

        public String getTicket() {
            return ticket;
        }

        public void setTicket(String ticket) {
            this.ticket = ticket;
        }
    }

    /**
     * Respuesta del endpoint de validación.
     */
    public static class ValidationResponse {
        private boolean valid;
        private String message;

        public ValidationResponse(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
