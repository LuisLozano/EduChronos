# Documentación técnica de Educhronos

Responde a esta pregunta: qué es Educhronos, en qué punto está, y qué documentos técnicos hay y en qué orden se leen.
Léelo primero, antes de abrir cualquier otro documento de esta carpeta.

Describe el commit 30b8d67a9b49.

Cada documento dice en su cabecera qué commit describe.
Las rutas entre comillas invertidas son rutas del repo, relativas a la raíz.
Un comprobador las verifica en cada push: `tools/doc-tecnica/comprobar_rutas.py`.
Las rutas del código se comprueban en el commit que cita el documento.
Las de esta documentación, las que empiezan por `docs/tecnica/`, se comprueban en el árbol de trabajo, tal como está.

## Qué es Educhronos

Educhronos hace los horarios de un instituto de educación secundaria español.
La usa el propio centro: la maneja la secretaria, que es profesora del centro, y la instala alguien del centro en su ordenador.
Es un programa de escritorio para Windows; al abrirlo, aparece solo en el navegador, en una página local.
Lo define §1 de `docs/gestion_proyecto.md`, y el manual de uso, `docs/manual/manual.md`, dice a quién va dirigido.

El centro mete sus datos por pantallas: profesores, aulas, asignaturas, grupos, desdobles, tutores y la disponibilidad del profesorado.
Con ellos genera un horario que respeta las restricciones duras y penaliza las blandas, lo retoca a mano, lo saca en PDF y CSV y, al acabar el año, duplica el curso para el siguiente.
Esa cadena, entera y sobre el programa de Windows, es el estado final del proyecto, en §1.

## Qué cubre y qué no

Cubre esa cadena y, desde el estado final 2, que el centro prepare su curso 2026/2027 a partir del de 2025/2026 y compare el horario obtenido con el oficial (§1).
Ese curso es un banco de prueba: en 2026/2027 Educhronos no se usa en producción.
La aplicación deja montar una configuración correcta y avisa cuando no lo es, pero no construye por el centro su configuración (§1).
Una base es un centro: no hay varios centros en una misma base.
La instalación deja fuera la firma de código, Windows 10, la desinstalación y el MSI, como dice la ficha de O-uso-centro en §3.
Lo que se ha decidido no hacer está en §4, «Limitación conocida», y resumido en `docs/tecnica/decisiones.md`.

## En qué punto está

La última versión publicada con tag es la 0.5.0, una Release definitiva (`docs/gestion-historico.md`, ficha de O-manual, decisión CV).
Los hitos H1 a H4 están terminados, y el primer estado final ya se alcanzó (§1 y §2 de `docs/gestion_proyecto.md`).
Del hito H5 están cerrados todos los objetivos menos O-uso-centro, que cumple 7 de sus 8 condiciones: falta el uso real en el centro, que espera fecha (§3).
No está en producción.

## Errores conocidos

Los fallos y carencias conocidos se registran como deudas.
§4 de `docs/gestion_proyecto.md` las clasifica una a una, con su objetivo y si bloquean; el texto entero de cada una está en «Deuda consciente VIVA» de `docs/plan_trabajo_horarios.md`.
Algunas se notan al usar la aplicación: por ejemplo, dar de baja a un profesor que tiene clases se queda a medias.
Antes de abrir una incidencia nueva se busca ahí si ya es conocida.

## Cómo se definió

No hay un documento de requisitos aparte.
El plan, `docs/plan_trabajo_horarios.md`, definió el proyecto por fases, de la 0 a la 12, cada una con sus criterios de verificación.
El modelo de datos se validó en papel en la Fase 1 y está en `docs/modelo_datos_fase1.md`.
El requisito de conjunto es el estado final de §1, que `docs/guion-aceptacion.md` ejecuta como una sola cadena.
Desde la Fase 8 se planifica por hitos y objetivos (§2 y §3 de `docs/gestion_proyecto.md`), y cada objetivo escribe su criterio de terminado sobre medición al abrirse.

## Los documentos

En orden de lectura:

1. Cómo está montada la aplicación, en `docs/tecnica/arquitectura.md`.
   Responde qué piezas forman Educhronos y cómo se hablan: el solver, la aplicación Spring Boot y el frontend Angular.
   Cuenta cómo arranca, dónde viven los datos, cómo se prepara el esquema y el recorrido de una generación.
2. Dónde está cada cosa, en `docs/tecnica/mapa.md`.
   Responde en qué carpeta o fichero vive cada parte, y cuándo hay que ir a ella.
3. Cómo se construye, en `docs/tecnica/construir.md`.
   Responde qué hace falta para construir el jar, con qué orden y qué deja la construcción.
4. Cómo se prueba, en `docs/tecnica/probar.md`.
   Responde qué suites hay, cómo se corre cada una y cuántos tests da.
5. Cómo se publica e instala, en `docs/tecnica/publicar.md`.
   Responde cómo sale una versión hacia el centro, qué hace el workflow del zip de Windows y cómo se instala.
6. Artefactos generados, en `docs/tecnica/generados.md`.
   Responde qué ficheros salen de un programa y cómo se regenera cada uno: la referencia del solver y los índices.
7. Por qué se decidió así, en `docs/tecnica/decisiones.md`.
   Responde qué decisiones fijan hoy el código, la construcción, las pruebas y la entrega, y por qué se tomaron, con la sede de cada una.
8. Los documentos que ya existían, en `docs/tecnica/documentos.md`.
   Responde qué es cada uno de los 30 documentos técnicos anteriores a esta carpeta: vigente, histórico o absorbido por otro.
