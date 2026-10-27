# Cómo se construye

Responde a esta pregunta: qué hace falta para construir Educhronos y qué deja la construcción.
Léelo antes de construir el jar por primera vez, o cuando la construcción falle.

Describe el commit 8c9c6ec75310.

## Requisitos

Hace falta Java 17. La CI usa la distribución Temurin (`.github/workflows/tests.yml:18-21`).
Hace falta Maven 3.6.0 o superior, por el plugin que construye el frontend (`pom.xml:48-49`).
El frontend usa Angular 22.2, con Node 22.23.1, la versión que fija `app/frontend/.nvmrc`, y npm 11.21.1 (`app/frontend/package.json:13`).
El npm que trae Node 22.23.1, el 10.9.8, no instala Angular 22: su npm install falla dentro de npm.
Para construir el jar no hace falta tener Node instalado: Maven baja el suyo, como se cuenta abajo.
Node y npm sí hacen falta para las pruebas del frontend, en `docs/tecnica/probar.md`.

## La orden

Desde la raíz del repo:

```bash
mvn clean package -DskipTests
```

Es la misma orden que usa el empaquetado de Linux (`docs/empaquetado.md:197`).
La opción -DskipTests salta los tests; se corren aparte, como cuenta `docs/tecnica/probar.md`.

## Qué hace

El proyecto Maven tiene dos módulos: el solver y la aplicación (`docs/tecnica/arquitectura.md`).
Maven construye primero el solver y después la aplicación, que depende de él.

En el módulo de la aplicación, la fase prepare-package construye el frontend (`app/pom.xml:111-152`).
Primero instala Node v22.23.1 y npm 11.21.1 dentro de la carpeta target del módulo, no en el sistema.
Después ejecuta npm ci y npm run build en `app/frontend/`.
En la misma fase copia la salida de Angular a la carpeta static de las clases (`app/pom.xml:154-175`).
Así la interfaz queda dentro del jar y la sirve el mismo servidor.

En la fase package, el plugin de Spring Boot rehace el jar como jar ejecutable, con todas sus dependencias dentro (`app/pom.xml:97-109`).
Sin esa ejecución, package dejaría un jar plano que no arranca.

Los tests de Java no construyen Angular: el frontend se construye en prepare-package, que mvn test no alcanza (`app/pom.xml:113-115`).

## Qué deja

El resultado es app/target/app-0.0.0-dev.jar.
El nombre sale del artefacto, app, y de la versión, 0.0.0-dev, que fija la propiedad revision de `pom.xml:22`.
Ningún pom cambia ese nombre.
Al lado queda app/target/app-0.0.0-dev.jar.original, el jar plano de antes de rehacerlo.

El empaquetado de una versión pasa la versión del tag a Maven con -Drevision=X.Y.Z, y el jar lleva entonces ese número (`scripts/empaquetar-linux.sh:106-108`).
Eso se cuenta en `docs/tecnica/publicar.md`.

## Arrancar

El arranque no va en un bloque de órdenes, porque no termina: deja el servidor escuchando.
Para desarrollo se usa `scripts/arrancar-backend.py`; con --con-frontend arranca también ng serve.
Arranca el backend contra una base SQLite que se le indica y deja anotado su grupo de procesos para pararlo después.
Su uso, sus códigos de salida y lo que escribe están en su cabecera (`scripts/arrancar-backend.py:2-56`).

## El bundle de Windows

El programa que se instala en el centro no es el jar suelto: es un zip para Windows con el jar y un Java propio.
Cómo se construye y se publica ese zip está en `docs/tecnica/publicar.md`.
