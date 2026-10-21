# Documentación técnica de Educhronos

Responde a esta pregunta: qué documentos técnicos hay y en qué orden se leen.
Léelo primero, antes de abrir cualquier otro documento de esta carpeta.

Describe el commit 816e30d6f2f1.

Cada documento dice en su cabecera qué commit describe.
Las rutas entre comillas invertidas son rutas del repo, relativas a la raíz.
Un comprobador las verifica en cada push: `tools/doc-tecnica/comprobar_rutas.py`.

Los documentos de esta carpeta no existen todavía en el commit que describen.
Por eso se enlazan por su nombre y no van entre comillas invertidas.

En orden de lectura:

1. [Cómo está montada la aplicación](arquitectura.md), en arquitectura.md.
   Responde qué piezas forman Educhronos y cómo se hablan: el solver, la aplicación Spring Boot y el frontend Angular.
   Cuenta cómo arranca, dónde viven los datos, cómo se prepara el esquema y el recorrido de una generación.
2. [Dónde está cada cosa](mapa.md), en mapa.md.
   Responde en qué carpeta o fichero vive cada parte, y cuándo hay que ir a ella.
