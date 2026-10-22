# Documentación técnica de Educhronos

Responde a esta pregunta: qué documentos técnicos hay y en qué orden se leen.
Léelo primero, antes de abrir cualquier otro documento de esta carpeta.

Describe el commit 30b8d67a9b49.

Cada documento dice en su cabecera qué commit describe.
Las rutas entre comillas invertidas son rutas del repo, relativas a la raíz.
Un comprobador las verifica en cada push: `tools/doc-tecnica/comprobar_rutas.py`.
Las rutas del código se comprueban en el commit que cita el documento.
Las de esta documentación, las que empiezan por `docs/tecnica/`, se comprueban en el árbol de trabajo, tal como está.

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
