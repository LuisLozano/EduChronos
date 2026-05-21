# Educhronos

Aplicación de gestión de horarios escolares para institutos de
educación secundaria española. Documentación del proyecto en `docs/`.

## Estructura

- `solver/`: motor de planificación basado en OR-Tools CP-SAT (Java 17).
  Independiente de Spring y de la persistencia.
- `app/`: aplicación Spring Boot que orquestará solver, persistencia y UI.
  Se introduce en Fase 6.

## Requisitos

- JDK 17
- Maven 3.9+

## Build

    mvn clean install

## Ejecutar el smoke test del solver

    mvn -pl solver exec:java \
        -Dexec.mainClass=es.yaroki.educhronos.solver.cli.HelloOrTools
