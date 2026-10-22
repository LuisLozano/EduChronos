# Los documentos que ya existían

Responde a esta pregunta: qué es cada uno de los documentos técnicos que había en el repo antes de esta carpeta, y si sigue sirviendo.
Léelo cuando abras uno de ellos y no sepas si lo que dice sigue siendo así.

Describe el commit f974c1fa7996.

Son los 30 documentos .md que el recuento de S244 contó fuera de `docs/tecnica/` y fuera de los cinco de gestión.
Los cinco de gestión son `docs/gestion_proyecto.md`, `docs/plan_trabajo_horarios.md`, `docs/metodo.md`, `docs/bitacora-sesiones.md` y `docs/gestion-historico.md`.
Ninguno de los 30 se ha reescrito: la tabla solo dice qué es cada uno.
Uno, la copia duplicada del resumen de extracción, se borró en el commit que describe este documento; por eso su ruta va sin comillas invertidas.

Cada documento tiene una de tres clases:

- **Vigente:** describe algo que sigue siendo así y se consulta hoy.
- **Histórico:** registra algo que pasó, como un acta o el informe de una sesión. No se actualiza.
- **Absorbido:** su contenido vivo ya está en otro documento, que el motivo nombra.

| Ruta | Clase | Motivo |
|---|---|---|
| `README.md` | absorbido | Su estructura, requisitos y construcción están en `docs/tecnica/arquitectura.md` y `docs/tecnica/construir.md`. |
| `app/frontend/README.md` | absorbido | Es la plantilla de Angular CLI; cómo se construye y se prueba el frontend está en `docs/tecnica/construir.md` y `docs/tecnica/probar.md`. |
| `docs/actas-aceptacion/acta-base-partida-s241.md` | histórico | Acta de la base de partida, de S241. |
| `docs/actas-aceptacion/acta-demo-bundle.md` | histórico | Acta de la demo con la secretaria, de O-demo-bundle. |
| `docs/actas-aceptacion/acta-humo-s234.md` | histórico | Acta del humo de S234 con v0.4.0-rc.1. |
| `docs/actas-aceptacion/acta-instalador-s237.md` | histórico | Acta del instalador, de S237. |
| `docs/actas-aceptacion/acta-manual-s242.md` | histórico | Acta del manual de uso, de S242. |
| `docs/actas-aceptacion/acta-prueba-secretaria.md` | histórico | Acta de la prueba corta de la secretaria. |
| `docs/actas-aceptacion/acta-s172.md` | histórico | Acta de la aceptación de S172. |
| `docs/actas-aceptacion/acta-s177.md` | histórico | Acta de la aceptación de S177. |
| `docs/actas-aceptacion/acta-sin-suspension-s238.md` | histórico | Acta de la generación sin suspensión, de S238. |
| `docs/diseno-navegacion.md` | histórico | Medición de S122 que fundó O-navegación, objetivo ya terminado. |
| `docs/empaquetado.md` | vigente | Cómo se construye el bundle y guía de distribución; `docs/tecnica/publicar.md` remite a él. |
| `docs/guia-incidencias.md` | vigente | Guía que va impresa con el USB y cierra el manual. |
| `docs/guion-aceptacion.md` | vigente | Guion que ejecuta como una sola cadena los seis pasos del estado final. |
| `docs/horario-referencia/2026-2027/INFORME-COMPARACION.md` | histórico | Comparación del horario generado con el oficial, de S228. |
| `docs/horario-referencia/2026-2027/INFORME-CONSERVACION-CATALOGO.md` | vigente | Salida de `tools/carga-centro/derivar-catalogo.py` sobre los volcados versionados de 2026/2027. |
| `docs/horario-referencia/2026-2027/INFORME-CRUCE-GRUPOS-PROFESORES.md` | histórico | Foto del cruce de S197; `tools/carga-centro/cruzar-grupos-profesores.py` escribe donde se le pida, no con este nombre. |
| `docs/horario-referencia/2026-2027/INFORME-NO-REPRESENTABLE.md` | histórico | Lo que el horario oficial tiene y Educhronos no representa, medido en S199. |
| `docs/horario-referencia/2026-2027/METRICAS-COMPARACION.md` | histórico | Métricas fijadas antes de la comparación de S228. |
| `docs/horario-referencia/ESPECIFICACION-CATALOGO.md` | vigente | Define las reglas del catálogo tal como las aplica `tools/carga-centro/derivar-catalogo.py`. |
| `docs/horario-referencia/INFORME-RECONCILIACION.md` | histórico | Cruce de grupos con aulas del horario de 2025/2026, de S115. |
| `docs/horario-referencia/README.md` | vigente | Describe la carpeta del horario de referencia, el banco de pruebas con datos reales. |
| `docs/horario-referencia/RESUMEN-EXTRACCION.md` | histórico | Cómo se extrajo con pdfplumber el volcado de 2025/2026; hoy extrae `tools/carga-centro/extraer-horario.py`. |
| docs/RESUMEN-EXTRACCION.md | absorbido | Era una copia idéntica de `docs/horario-referencia/RESUMEN-EXTRACCION.md`; se borró. |
| `docs/horario-referencia/tutores-oficiales-centro.md` | histórico | Registro de los tutores oficiales que entregó el centro en S137. |
| `docs/manual/manual.md` | vigente | Manual de uso de la versión 0.5.0, que viaja con el USB. |
| `docs/modelo_datos_fase1.md` | vigente | Modelo de datos de la Fase 1; el código aún cita sus secciones. |
| `docs/referencia-codigo-solver.md` | vigente | Índice de la API del solver, generado del código; cómo se regenera, en `docs/tecnica/generados.md`. |
| `docs/salvedades-demo.md` | histórico | Acompañó la demostración del horario al centro. |
