package es.dggc.backoffice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST de comprobación de vida (healthcheck).
 *
 * @RestController = @Controller + @ResponseBody
 *   Todos los métodos devuelven directamente el cuerpo de la respuesta HTTP,
 *   no el nombre de una vista/template.
 *
 * Endpoint expuesto:
 *   GET http://localhost:8085/ping  →  devuelve exactamente: OK BACKOFFICE
 *
 * Útil para:
 *   - Verificar desde terminal que el servidor responde
 *   - Healthcheck automático desde el frontend Angular
 *   - Prueba rápida desde el navegador
 *
 * Cómo probarlo:
 *   Navegador  : http://localhost:8085/ping
 *   curl       : curl http://localhost:8085/ping
 *   PowerShell : Invoke-RestMethod http://localhost:8085/ping
 */
@RestController
public class PingController {

    /**
     * Endpoint de comprobación.
     *
     * ResponseEntity<String> permite controlar el código HTTP de respuesta.
     * ResponseEntity.ok("...") devuelve HTTP 200 con el texto especificado.
     *
     * @return 200 OK con cuerpo "OK BACKOFFICE"
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        // Respuesta exacta solicitada: OK BACKOFFICE
        return ResponseEntity.ok("OK BACKOFFICE");
    }

}
