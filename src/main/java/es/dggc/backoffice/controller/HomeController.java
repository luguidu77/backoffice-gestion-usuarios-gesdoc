package es.dggc.backoffice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador web que entrega el frontend Angular.
 *
 * Forward para rutas SPA (evita 404 en /users, /dashboard, etc.).
 * Las rutas /api/** quedan fuera de este controlador.
 */
@Controller
public class HomeController {

    /**
     * Forward al index del frontend empaquetado en static/.
     */
    @GetMapping(value = {
        "/",
        "/{path:^(?!api$|error$)[^\\.]*}",
        "/**/{path:^(?!api$|error$)[^\\.]*}"
    })
    public String home() {
        return "forward:/index.html";
    }

}
