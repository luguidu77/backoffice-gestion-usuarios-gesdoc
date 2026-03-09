package es.dggc.backoffice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de arranque de la aplicación Backoffice.
 *
 * @SpringBootApplication es un atajo que activa tres anotaciones a la vez:
 *   - @Configuration       → esta clase puede definir beans de Spring
 *   - @EnableAutoConfiguration → Spring Boot configura automáticamente Tomcat,
 *                                Spring MVC, Jackson, etc., según las
 *                                dependencias del classpath
 *   - @ComponentScan       → escanea este paquete y todos sus sub-paquetes
 *                            buscando @Controller, @Service, @Repository, etc.
 *
 * Cómo arrancar la aplicación:
 *   1. Desde el IDE (VS Code / IntelliJ / Eclipse):
 *        Botón derecho sobre esta clase → Run As → Java Application
 *        (o usar el botón "Run" que aparece encima del método main)
 *
 *   2. Con Maven desde la terminal (sin empaquetar):
 *        mvn spring-boot:run
 *
 *   3. Empaquetando primero y luego ejecutando el jar:
 *        mvn clean package -DskipTests
 *        java -jar target/backoffice-test.jar
 */
@SpringBootApplication
public class BackofficeApplication {

    public static void main(String[] args) {
        // SpringApplication.run arranca el contexto de Spring Boot:
        //   1. Crea el contexto de aplicación (ApplicationContext)
        //   2. Arranca el servidor Tomcat embebido en el puerto de application.properties
        //   3. Registra todos los controladores, servicios y beans
        SpringApplication.run(BackofficeApplication.class, args);
    }

}
