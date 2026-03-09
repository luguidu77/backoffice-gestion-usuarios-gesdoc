package es.dggc.backoffice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controlador web que sirve la página HTML principal de la aplicación.
 *
 * Diferencia con PingController:
 *   - PingController usa @RestController → devuelve datos (texto/JSON) para APIs
 *   - HomeController  usa @Controller    → pensado para devolver vistas/páginas HTML
 *
 * @ResponseBody aquí indica que el String devuelto ES literalmente el cuerpo
 * de la respuesta HTTP (el HTML), no el nombre de una plantilla Thymeleaf.
 * Se usa así para no añadir la dependencia de Thymeleaf al proyecto.
 *
 * Endpoint expuesto:
 *   GET http://localhost:8085/  →  muestra página HTML de bienvenida
 *
 * Nota:
 *   Spring Boot también sirve automáticamente cualquier fichero que esté en
 *   src/main/resources/static/index.html cuando se pide GET /
 *   Este controlador sobreescribe ese comportamiento para poder añadir
 *   lógica dinámica en el futuro (ej: datos del usuario, versión, etc.)
 */
@Controller
public class HomeController {

    /**
     * Devuelve la página HTML principal de bienvenida.
     *
     * El HTML está inline (en vez de en un fichero .html separado) para
     * mantener el proyecto sin dependencias de motor de plantillas.
     *
     * @return String con el HTML completo de la página
     */
    @GetMapping("/")
    @ResponseBody
    public String home() {
        return "<!DOCTYPE html>\n"
             + "<html lang=\"es\">\n"
             + "<head>\n"
             + "  <meta charset=\"UTF-8\">\n"
             + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
             + "  <title>Backoffice Test</title>\n"
             + "  <style>\n"
             + "    * { box-sizing: border-box; margin: 0; padding: 0; }\n"
             + "    body {\n"
             + "      font-family: 'Segoe UI', Arial, sans-serif;\n"
             + "      background: #f0f4f8;\n"
             + "      display: flex;\n"
             + "      justify-content: center;\n"
             + "      align-items: center;\n"
             + "      min-height: 100vh;\n"
             + "    }\n"
             + "    .card {\n"
             + "      background: #ffffff;\n"
             + "      border-radius: 8px;\n"
             + "      box-shadow: 0 2px 12px rgba(0,0,0,0.1);\n"
             + "      padding: 48px 64px;\n"
             + "      text-align: center;\n"
             + "      max-width: 480px;\n"
             + "      width: 90%;\n"
             + "    }\n"
             + "    h1 { color: #2c5282; font-size: 1.6rem; margin-bottom: 16px; }\n"
             + "    .badge {\n"
             + "      display: inline-block;\n"
             + "      background: #48bb78;\n"
             + "      color: white;\n"
             + "      padding: 6px 16px;\n"
             + "      border-radius: 20px;\n"
             + "      font-weight: 600;\n"
             + "      font-size: 0.9rem;\n"
             + "      margin-bottom: 24px;\n"
             + "    }\n"
             + "    p { color: #718096; margin-bottom: 8px; }\n"
             + "    a {\n"
             + "      color: #4299e1;\n"
             + "      text-decoration: none;\n"
             + "      font-weight: 600;\n"
             + "    }\n"
             + "    a:hover { text-decoration: underline; }\n"
             + "    .divider { border: none; border-top: 1px solid #e2e8f0; margin: 20px 0; }\n"
             + "  </style>\n"
             + "</head>\n"
             + "<body>\n"
             + "  <div class=\"card\">\n"
             + "    <div class=\"badge\">Puerto 8085 &#x2714;</div>\n"
             + "    <h1>Backoffice de prueba funcionando</h1>\n"
             + "    <hr class=\"divider\">\n"
             + "    <p>Servicio REST disponible:</p>\n"
             + "    <p><a href=\"/ping\">/ping</a> &rarr; devuelve OK BACKOFFICE</p>\n"
             + "    <hr class=\"divider\">\n"
             + "    <p style=\"font-size:0.8rem; color:#a0aec0\">Java 8 + Spring Boot 2.7 + Tomcat embebido</p>\n"
             + "  </div>\n"
             + "</body>\n"
             + "</html>";
    }

}
