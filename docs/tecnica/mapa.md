# Dónde está cada cosa

Responde a esta pregunta: en qué carpeta o fichero del repo vive cada parte, y cuándo hay que ir a ella.
Léelo cuando busques dónde hacer un cambio; para entender cómo encajan las piezas, lee antes `docs/tecnica/arquitectura.md`.

Describe el commit 30b8d67a9b49.

Cada entrada da la ruta, qué hay y cuándo se toca.
Donde hay muchos ficheros iguales no se enumeran: se dice qué patrón siguen.

## Raíz del repo

El repo es git y está en GitHub, en https://github.com/LuisLozano/EduChronos (`docs/empaquetado.md:85`).
Se trabaja en la rama main, y las versiones salen de tags puestos sobre ella (`docs/empaquetado.md:78-80`).

- `pom.xml`: el proyecto Maven padre. Declara los módulos solver y app, las versiones de las dependencias y los plugins. Se toca al subir una versión o añadir un plugin común.
- `README.md`: la presentación corta del proyecto.
- `.gitattributes`: obliga a que `scripts/Instalar.cmd` tenga finales de línea CRLF en todo checkout.
- `.gitignore`: lo que git no versiona.
- `.github/`: la configuración de GitHub; solo contiene los workflows.
- `app/`: el módulo de la aplicación Spring Boot, con el frontend dentro. Sus dependencias, en `app/pom.xml`.
- `solver/`: el módulo del solver, una biblioteca sin Spring. Sus dependencias, en `solver/pom.xml`.
- `docs/`: la documentación del proyecto. En la raíz de esa carpeta están, entre otros, la gestión (`docs/gestion_proyecto.md`), el plan (`docs/plan_trabajo_horarios.md`), el método (`docs/metodo.md`) y la bitácora de sesiones (`docs/bitacora-sesiones.md`).
- `scripts/`: guiones sueltos de empaquetado, instalación, verificación y mantenimiento de documentos. Ver su sección.
- `tools/`: herramientas con sus propios tests, una por carpeta. Ver su sección.

## Segundo nivel

- `.github/workflows/`: los tres workflows de GitHub Actions. Ver su sección.
- `app/src/`: el código Java de la aplicación (`app/src/main/java/`), sus recursos (`app/src/main/resources/`) y sus tests (`app/src/test/`).
- `app/frontend/`: el proyecto Angular. El código está en `app/frontend/src/`, los tests de extremo a extremo en `app/frontend/e2e/` y la configuración en `app/frontend/angular.json` y `app/frontend/package.json`. `app/frontend/proxy.conf.json` reenvía /api al backend cuando se trabaja con el servidor de desarrollo de Angular.
- `solver/src/`: el código Java del solver (`solver/src/main/java/`) y sus tests (`solver/src/test/java/`). Los problemas de prueba están en `solver/src/test/resources/fixtures/`, y el esquema JSON de un problema en `solver/src/test/resources/schema/problema-horario.schema.json`.
- `docs/actas-aceptacion/`: las actas de aceptación, una por fichero. Se añade una por cada aceptación nueva.
- `docs/horario-referencia/`: la transcripción del horario real del instituto de referencia, que sirve de banco de pruebas. Su `docs/horario-referencia/README.md` explica la jerarquía de fuentes. Los datos de 2026/2027 están en `docs/horario-referencia/2026-2027/`. Seis ficheros de esa carpeta llevan espacios en el nombre: no se pueden citar entre comillas invertidas.
- `docs/manual/`: el manual de uso (`docs/manual/manual.md`) y sus capturas (`docs/manual/capturas/`).
- `docs/tecnica/`: esta documentación técnica. Empieza por su índice, `docs/tecnica/README.md`. Se toca cuando cambia lo que describe.
- `tools/base-partida/`, `tools/carga-centro/`, `tools/doc-tecnica/`, `tools/manual/` y `tools/referencia-solver/`: ver la sección de herramientas.

Las rutas de esta documentación se comprueban así: las del código, en el commit que cita cada documento; las de `docs/tecnica/`, en el árbol de trabajo, tal como está.

Ante un fallo, el log del programa instalado está en su carpeta de datos (`docs/tecnica/arquitectura.md`, «Cómo arranca»), y el paquete afectado se busca en las secciones que siguen.
Antes de abrir una incidencia, §4 de `docs/gestion_proyecto.md` dice si el fallo ya es una deuda conocida.

## Paquetes Java de la aplicación

Todos cuelgan de `app/src/main/java/es/yaroki/educhronos/app/`. Sus tests están en la misma ruta bajo `app/src/test/java/`.

- `app/src/main/java/es/yaroki/educhronos/app/EduchronosApplication.java`: la entrada de la aplicación. Se toca solo si cambia el arranque.
- `app/src/main/java/es/yaroki/educhronos/app/catalog/`: el catálogo del centro. Cada entidad JPA tiene su fichero, y su repositorio, cuando lo tiene, lleva el mismo nombre más Repository. Los nueve enumerados (días, cargos, tipos de aula, de grupo, de actividad…) van en su propio fichero. Se toca al cambiar el modelo de datos del centro, junto con el esquema.
- `app/src/main/java/es/yaroki/educhronos/app/persistence/`: el horario generado. Están la cabecera (`app/src/main/java/es/yaroki/educhronos/app/persistence/HorarioGenerado.java`), las sesiones y las guardias, con sus repositorios. Se toca cuando cambia lo que se guarda de un horario.
- `app/src/main/java/es/yaroki/educhronos/app/service/`: la lógica. Hay un servicio por recurso, con el nombre del recurso más Service. Las excepciones que acaban en Exception son los rechazos que traducen los controladores. La generación vive en `app/src/main/java/es/yaroki/educhronos/app/service/GeneradorHorarioService.java`, y el reparto de guardias en `app/src/main/java/es/yaroki/educhronos/app/service/ViaRepartoGuardias.java` y `app/src/main/java/es/yaroki/educhronos/app/service/RepartoGuardias.java`. Se toca al cambiar una regla de negocio.
- `app/src/main/java/es/yaroki/educhronos/app/mapper/`: los cuatro traductores entre el catálogo JPA y el solver, de ida (catálogo, bloqueos y aulas posibles) y de vuelta (la solución). Se toca cuando cambia el modelo del solver o el del catálogo.
- `app/src/main/java/es/yaroki/educhronos/app/web/`: los controladores REST. Hay uno por recurso, con el nombre del recurso más Controller, y cada uno traduce sus propias excepciones. `app/src/main/java/es/yaroki/educhronos/app/web/MapeoFalloSolver.java` traduce el estado del solver a la respuesta de un fallo. Se toca al añadir o cambiar un endpoint.
- `app/src/main/java/es/yaroki/educhronos/app/web/dto/`: los 66 ficheros del paquete. 22 acaban en Request y son cuerpos de petición o partes de uno. 43 acaban en DTO y son, sobre todo, lo que devuelve la API; uno, el de la configuración de guardias, también entra como cuerpo. `app/src/main/java/es/yaroki/educhronos/app/web/dto/ReferenciaInstancia.java` es la referencia a una instancia de actividad por su código y su índice. Se toca con el endpoint que los usa.
- `app/src/main/java/es/yaroki/educhronos/app/curso/`: los cursos. Están el servicio de cursos, el duplicado de un curso, el estado del curso abierto en memoria y el filtro que rechaza escrituras cuando no toca (`app/src/main/java/es/yaroki/educhronos/app/curso/GuardaSoloLectura.java`). Se toca al cambiar cómo se abre, se crea o se archiva un curso.
- `app/src/main/java/es/yaroki/educhronos/app/config/`: la carpeta de datos, el DataSource conmutable, las claves foráneas, el esquema y sus migraciones, la versión y el servicio de la interfaz. Se toca cuando cambia algo del arranque o de la base.
- `app/src/main/java/es/yaroki/educhronos/app/escritorio/`: el modo escritorio del programa instalado. Hay una clase por tarea: instancia única, ventana de inicio, navegador, bandeja, diálogos y evitar la suspensión en Windows. Se toca cuando cambia cómo arranca o se cierra el programa instalado.
- `app/src/main/java/es/yaroki/educhronos/app/exportacion/`: el horario en CSV (`app/src/main/java/es/yaroki/educhronos/app/exportacion/HorarioCsv.java`) y en PDF (`app/src/main/java/es/yaroki/educhronos/app/exportacion/HorarioPdf.java`), los dos a partir de la proyección. Se toca al cambiar una exportación.

Los recursos de la aplicación están en `app/src/main/resources/`: la configuración (`app/src/main/resources/application.properties`), el esquema completo (`app/src/main/resources/schema.sql`), las migraciones (`app/src/main/resources/esquema/`), las fuentes del PDF (`app/src/main/resources/fuentes/`) y los registros de Spring (`app/src/main/resources/META-INF/`). Un cambio de esquema toca a la vez `app/src/main/resources/schema.sql`, una migración nueva y la versión de `app/src/main/java/es/yaroki/educhronos/app/config/PreparadorEsquema.java`.

## Paquetes Java del solver

Todos cuelgan de `solver/src/main/java/es/yaroki/educhronos/solver/`.

- `solver/src/main/java/es/yaroki/educhronos/solver/domain/`: el modelo puro del problema y de la solución, una clase por concepto. Se toca cuando el solver necesita un dato nuevo.
- `solver/src/main/java/es/yaroki/educhronos/solver/cpsat/`: el modelo CP-SAT (`solver/src/main/java/es/yaroki/educhronos/solver/cpsat/ModeloCpSat.java`), la entrada del solver (`solver/src/main/java/es/yaroki/educhronos/solver/cpsat/SolverHorario.java`) y el verificador de la solución (`solver/src/main/java/es/yaroki/educhronos/solver/cpsat/VerificadorSolucion.java`). Se toca al añadir o cambiar una regla del horario.
- `solver/src/main/java/es/yaroki/educhronos/solver/io/`: la lectura de un problema en JSON. Cada parte del JSON tiene su clase, con el nombre más Dto. La carga la hace `solver/src/main/java/es/yaroki/educhronos/solver/io/ProblemaHorarioJsonLoader.java`. Se toca cuando cambia el formato JSON del problema.
- `solver/src/main/java/es/yaroki/educhronos/solver/cli/`: la línea de órdenes del solver (`solver/src/main/java/es/yaroki/educhronos/solver/cli/Main.java`) y lo que imprime. Se toca al cambiar esa salida.

## Carpetas del frontend

Todas cuelgan de `app/frontend/src/app/`. La entrada del frontend es `app/frontend/src/main.ts`.

- `app/frontend/src/app/`: en su raíz están el componente de la aplicación (`app/frontend/src/app/app.ts`, con su plantilla y su estilo), la configuración (`app/frontend/src/app/app.config.ts`) y las rutas (`app/frontend/src/app/app.routes.ts`). Se toca al añadir una pantalla.
- `app/frontend/src/app/services/`: un servicio por recurso de la API, con el nombre del recurso más .service.ts y su spec al lado. Se toca cuando cambia un endpoint.
- `app/frontend/src/app/models/`: las interfaces de los datos de la API, con el nombre del recurso más .model.ts. Se toca con el DTO que reflejan.
- `app/frontend/src/app/horario/`: lógica pura de la vista del horario, sin componentes ni servicios (proyección, pines, recreo, diagnóstico, ajuste, reparto, título y oculto). Cada fichero tiene su spec.
- `app/frontend/src/app/catalogo/`: lógica pura de las listas del catálogo: búsqueda, pertenencia y la columna de horas.
- `app/frontend/src/app/testing/`: datos fabricados para los specs.
- `app/frontend/src/app/components/`: los componentes, uno por carpeta. Cada componente tiene su .ts, su .html, su .css y casi siempre su spec. Las pantallas de datos siguen el mismo molde: una lista con el nombre de la entidad más -lista y un formulario con -form que se abre en diálogo.

Las carpetas de componentes:

- `app/frontend/src/app/components/landing/`: la portada.
- `app/frontend/src/app/components/configuracion/`: la sección de configuración, con el índice de sus ocho pantallas.
- `app/frontend/src/app/components/jornada/`: la jornada, y el diálogo que confirma reemplazarla.
- `app/frontend/src/app/components/profesores/`: lista y formulario de profesores. Dentro, `app/frontend/src/app/components/profesores/disponibilidad/` es el diálogo de disponibilidad sobre la rejilla de la jornada, y `app/frontend/src/app/components/profesores/minimo-guardias/` son los mínimos de guardia del centro.
- `app/frontend/src/app/components/aulas/`, `app/frontend/src/app/components/niveles/` y `app/frontend/src/app/components/subgrupos/`: lista y formulario de cada entidad.
- `app/frontend/src/app/components/asignaturas/`: lista y formulario de asignaturas, y el diálogo de sus aulas.
- `app/frontend/src/app/components/grupos/`: lista y formulario de grupos, y los diálogos de tutoría, PDC y replicación.
- `app/frontend/src/app/components/actividades/`: lista y editor de actividades.
- `app/frontend/src/app/components/horario-view/`: la vista del horario y la guarda que no deja salir mientras se genera.
- `app/frontend/src/app/components/horario-grid/`: la rejilla de días por tramos que pinta las sesiones ya filtradas.
- `app/frontend/src/app/components/panel-prevalidacion/`: el panel con el resultado de la prevalidación.
- `app/frontend/src/app/components/confirmar-generacion/` y `app/frontend/src/app/components/generando-dialogo/`: los diálogos de antes y de durante una generación.
- `app/frontend/src/app/components/confirmar-borrado/`: el diálogo genérico de confirmar un borrado.
- `app/frontend/src/app/components/curso-barra/` y `app/frontend/src/app/components/cursos-dialogo/`: el curso abierto en la barra y el selector de cursos.
- `app/frontend/src/app/components/version-linea/`: la versión de la aplicación en la barra.
- `app/frontend/src/app/components/cabecera-lista/` y `app/frontend/src/app/components/estado-lista/`: la cabecera y los estados (error, carga, vacío) compartidos por las listas.
- `app/frontend/src/app/components/filtro-opciones/`: el campo de filtro de un selector con muchas opciones.
- `app/frontend/src/app/components/lista-doble/`: la lista doble para elegir varias entidades.

## Guiones de scripts

- `scripts/empaquetar-linux.sh`: construye el jar y arma la carpeta de entrega que se lleva a Windows.
- `scripts/empaquetar-windows.ps1`: en Windows, construye con jpackage la aplicación a partir de esa entrega.
- `scripts/Instalar.cmd`: el instalador que viaja en el zip. Copia el programa a la carpeta del usuario y crea el acceso directo.
- `scripts/probar-instalar.ps1`: prueba `scripts/Instalar.cmd` en ocho casos sobre carpetas temporales.
- `scripts/arrancar-backend.py`: arranca y para el backend de desarrollo contra una base SQLite concreta.
- `scripts/oraculo-exportacion.py`: verifica un CSV exportado contra lo que dice la base.
- `scripts/verificar-leyenda-pdf.py`: verifica la leyenda de un PDF de horario contra la base.
- `scripts/calcular-recortes.py`: cuenta las celdas de la rejilla que se recortarían, y es el ejemplo de autoprueba del proyecto.
- `scripts/volcar-sesiones-por-grupo.py`: saca de una base el volcado que espera `scripts/calcular-recortes.py`.
- `scripts/regenerar-indice.py`: regenera el índice de los documentos grandes de docs. Se corre en cada cierre de sesión.
- `scripts/verificar-cierre.py`: la verificación mecánica del cierre de sesión.
- `scripts/s122-doc-normas-e-indice.sh`: un guion de un solo uso de la sesión 122. Se conserva y no se vuelve a correr.

## Herramientas de tools

Cada herramienta tiene sus tests en una carpeta tests a su lado.

- `tools/base-partida/preparar_base_partida.py`: escribe el nombre del curso en una copia de la base de partida que viaja con el programa.
- `tools/doc-tecnica/comprobar_rutas.py`: comprueba las rutas de esta documentación. Las del código, en el commit que cita cada documento; las de `docs/tecnica/`, en el árbol de trabajo.
- `tools/referencia-solver/generar_referencia.py`: genera `docs/referencia-codigo-solver.md` desde el código del solver. Se corre cuando cambia `solver/src/`; cómo, en `docs/tecnica/generados.md`.
- `tools/manual/capturas.mjs`: hace las capturas del manual de uso y comprueba sus textos contra la aplicación.
- `tools/manual/generar-pdf.sh` y `tools/manual/manual.css`: generan el PDF del manual con su hoja de estilo.
- `tools/carga-centro/`: llevan a Educhronos los datos de un centro a partir de los PDF y las hojas de su programa de horarios. Sus tests están en `tools/carga-centro/tests/`, con `tools/carga-centro/tests/paginas_sinteticas.py` como páginas fabricadas. Sus guiones:
  - `tools/carga-centro/extraer-horario.py` y `tools/carga-centro/extraer-nombres.py`: sacan de los PDF la vista de grupos y los nombres completos.
  - `tools/carga-centro/cruzar-grupos-profesores.py`: cruza la vista de grupos con la de profesores.
  - `tools/carga-centro/codigos_grupo.py`: la forma corta del nombre de un grupo.
  - `tools/carga-centro/parche-aulas.py`: lo que cambia en las aulas el retoque a mano de la vista de profesores.
  - `tools/carga-centro/derivar-catalogo.py`, `tools/carga-centro/derivar-guardias.py` y `tools/carga-centro/derivar-reglas-aulas.py`: derivan el catálogo, las guardias y las reglas de aulas.
  - `tools/carga-centro/aplicar-tutores-oficiales.py`: aplica al catálogo la lista oficial de tutores.
  - `tools/carga-centro/cargar-centro.py`, `tools/carga-centro/cargar-guardias.py` y `tools/carga-centro/cargar-reglas-aulas.py`: cargan todo eso en una base por la API REST.
  - `tools/carga-centro/verificar-conservacion.py` y `tools/carga-centro/comparar-con-oficial.py`: comparan el horario generado con el del centro.

## Workflows

- `.github/workflows/tests.yml`: en cada push a main. Corre los tests Java, los de Angular, los de extremo a extremo y el job docs, que pasa este comprobador de rutas. Se toca al añadir una suite.
- `.github/workflows/instalador.yml`: en los push a main que tocan `scripts/Instalar.cmd`, `scripts/probar-instalar.ps1`, `.gitattributes` o el propio workflow, o a mano. Corre la autoprueba de `scripts/probar-instalar.ps1` en Windows.
- `.github/workflows/bundle.yml`: con un tag que empieza por v, o a mano. Construye el zip de Windows y, con tag, lo publica en la Release.
