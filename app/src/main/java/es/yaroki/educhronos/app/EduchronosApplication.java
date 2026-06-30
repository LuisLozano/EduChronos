package es.yaroki.educhronos.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de arranque del backend Spring Boot (Fase 6).
 *
 * <p>Bloque 1: solo abre el contexto y la BD SQLite local, con el esquema
 * generado por Hibernate (hbm2ddl). Entidades del modelo real, mapper y CRUD
 * llegan en bloques posteriores.
 */
@SpringBootApplication
public class EduchronosApplication {

    public static void main(String[] args) {
        SpringApplication.run(EduchronosApplication.class, args);
    }
}
