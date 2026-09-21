package es.yaroki.educhronos.app;

import es.yaroki.educhronos.app.escritorio.Escritorio;
import es.yaroki.educhronos.app.escritorio.ModoEscritorio;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de arranque del backend Spring Boot (Fase 6).
 *
 * <p>Bloque 1: solo abre el contexto y la BD SQLite local, con el esquema
 * generado por Hibernate (hbm2ddl). Entidades del modelo real, mapper y CRUD
 * llegan en bloques posteriores.
 *
 * <p>Desde S154 hay DOS arranques. El corriente —el de siempre, y el que usan la suite, el
 * e2e y {@code mvn spring-boot:run}— es la línea de abajo, intacta. El del bundle de
 * escritorio lo activa la propiedad de sistema {@link ModoEscritorio#PROPIEDAD}, que pone
 * {@code jpackage}, y se va entero a {@link Escritorio}: instancia única, log en fichero,
 * navegador y bandeja. La bifurcación vive aquí, en {@code main}, porque el modo escritorio
 * tiene que decidirse ANTES de construir nada de Spring.
 */
@SpringBootApplication
public class EduchronosApplication {

    public static void main(String[] args) {
        if (ModoEscritorio.activo()) {
            Escritorio.arrancar(args);
            return;
        }
        SpringApplication.run(EduchronosApplication.class, args);
    }
}
