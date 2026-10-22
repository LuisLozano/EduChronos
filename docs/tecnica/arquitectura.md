# Cómo está montada la aplicación

Responde a esta pregunta: qué piezas forman Educhronos y cómo se hablan entre sí.
Léelo antes de tocar el código por primera vez, o cuando un cambio cruce de una pieza a otra.

Describe el commit 8b27da47f8cf.

## Las tres piezas

Educhronos es un proyecto Maven con dos módulos, declarados en `pom.xml`: el solver y la aplicación.
El frontend no es un módulo Maven: vive dentro del módulo de la aplicación.

El **solver** está en `solver/`. Es una biblioteca Java sobre OR-Tools CP-SAT, sin Spring y sin base de datos.
Sus dependencias están en `solver/pom.xml`. Tiene cuatro paquetes:

- `solver/src/main/java/es/yaroki/educhronos/solver/domain/`: el modelo puro del problema y de la solución (profesores, grupos, actividades, tramos…).
- `solver/src/main/java/es/yaroki/educhronos/solver/cpsat/`: el modelo CP-SAT, su resolución y un verificador independiente de la solución.
- `solver/src/main/java/es/yaroki/educhronos/solver/io/`: la lectura de un problema desde JSON.
- `solver/src/main/java/es/yaroki/educhronos/solver/cli/`: una línea de órdenes que lee un problema de un fichero JSON, busca un horario factible e imprime el resultado.

La **aplicación** está en `app/`. Es una aplicación Spring Boot que depende del solver, de SQLite y de OpenPDF (`app/pom.xml`).
Sus paquetes cuelgan de `app/src/main/java/es/yaroki/educhronos/app/`:

- `app/src/main/java/es/yaroki/educhronos/app/catalog/`: las entidades JPA del catálogo del centro y sus repositorios.
- `app/src/main/java/es/yaroki/educhronos/app/persistence/`: las entidades del horario generado (cabecera, sesiones y guardias) y sus repositorios.
- `app/src/main/java/es/yaroki/educhronos/app/service/`: la lógica de cada recurso, la generación y la prevalidación.
- `app/src/main/java/es/yaroki/educhronos/app/mapper/`: la traducción entre las entidades JPA y el modelo del solver.
- `app/src/main/java/es/yaroki/educhronos/app/web/`: los controladores REST, todos bajo /api, y `app/src/main/java/es/yaroki/educhronos/app/web/dto/`, los objetos que viajan por la API.
- `app/src/main/java/es/yaroki/educhronos/app/curso/`: los cursos (un fichero de base por curso), el curso abierto y la guarda que impide escribir cuando no toca.
- `app/src/main/java/es/yaroki/educhronos/app/config/`: la base de datos, el esquema, la versión y el servicio de la interfaz.
- `app/src/main/java/es/yaroki/educhronos/app/escritorio/`: el arranque del programa instalado (ventana de inicio, navegador, bandeja).
- `app/src/main/java/es/yaroki/educhronos/app/exportacion/`: el horario en CSV y en PDF.

El **frontend** es una aplicación Angular en `app/frontend/`. En la construcción se copia dentro del jar de la aplicación.
Lo hace `app/pom.xml:153-168`: copia la salida de Angular a la carpeta static de las clases del jar.
Así el programa que se instala es un solo jar, con el servidor y la interfaz dentro.

## Cómo arranca

La entrada de la aplicación es el método main de `app/src/main/java/es/yaroki/educhronos/app/EduchronosApplication.java:25-31`.
Ese método decide entre dos arranques.

Si la propiedad de sistema educhronos.escritorio vale exactamente true, arranca el modo escritorio.
La propiedad la lee `app/src/main/java/es/yaroki/educhronos/app/escritorio/ModoEscritorio.java`.
Es el modo del programa instalado.
Entonces manda `app/src/main/java/es/yaroki/educhronos/app/escritorio/Escritorio.java:50`, y lo hace antes de construir nada de Spring.
Resuelve y crea la carpeta de datos, y toma un candado para que no haya dos instancias abiertas.
Si ya había otra instancia, vuelve a ella y termina.
Si no, abre una ventana de «se está iniciando», arranca Spring con el log en un fichero de la carpeta de datos y, cuando la aplicación está lista, abre el navegador y pone un icono en la bandeja.
Cada una de esas tareas tiene su clase en `app/src/main/java/es/yaroki/educhronos/app/escritorio/`.

Sin esa propiedad, main arranca Spring tal cual (`app/src/main/java/es/yaroki/educhronos/app/EduchronosApplication.java:30`), sin ventana, navegador ni bandeja.

En los dos casos, el servidor escucha solo en 127.0.0.1 (`app/src/main/resources/application.properties:62`).
El mismo servidor sirve la API y la interfaz.
`app/src/main/java/es/yaroki/educhronos/app/config/RutasSpaConfig.java` es el único manejador de los ficheros estáticos, porque `app/src/main/resources/application.properties:67` apaga el de Spring Boot.
Para las rutas de la interfaz que no son ficheros devuelve la página de Angular; la decisión está en `app/src/main/java/es/yaroki/educhronos/app/config/ResolvedorRutasSpa.java`.

Spring descubre dos post-procesadores del entorno por `app/src/main/resources/META-INF/spring.factories`: el que sitúa la base y el que publica la versión.
Dos autoconfiguraciones propias se registran en `app/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`: las claves foráneas de SQLite y el DataSource conmutable.

## Dónde viven los datos

Los datos viven en una carpeta del usuario, no junto al programa.
La decide `app/src/main/java/es/yaroki/educhronos/app/config/CarpetaDatos.java:78`.
En Windows es Educhronos dentro de LOCALAPPDATA.
En el resto de sistemas es educhronos dentro de XDG_DATA_HOME, o dentro de .local/share en la carpeta personal si esa variable no está.

Cada curso es un fichero SQLite en esa carpeta.
El fichero que se abre por defecto es educhronos.db.
Si en la carpeta hay un fichero curso-abierto, su contenido nombra la base que se abre (`app/src/main/java/es/yaroki/educhronos/app/config/CarpetaDatos.java:174`).

La URL de la base la publica `app/src/main/java/es/yaroki/educhronos/app/config/RutaBaseDatosEnvironmentPostProcessor.java` antes de que exista el DataSource.
Si spring.datasource.url ya trae un valor, no hace nada.
Así trabajan los tests y el arranque con una base elegida a mano.

El DataSource es `app/src/main/java/es/yaroki/educhronos/app/config/BaseConmutable.java`, registrado por `app/src/main/java/es/yaroki/educhronos/app/config/BaseConmutableConfig.java`.
Es un DataSource estable que por debajo cambia de pool.
Abrir otro curso sustituye el pool (`app/src/main/java/es/yaroki/educhronos/app/config/BaseConmutable.java:106`) sin rehacer el contexto de Spring.
Lo pide `app/src/main/java/es/yaroki/educhronos/app/curso/CursoService.java`, y la base nueva se abre con `app/src/main/java/es/yaroki/educhronos/app/config/FabricaDeBases.java`.
Toda conexión tiene encendidas las claves foráneas de SQLite (`app/src/main/java/es/yaroki/educhronos/app/config/SqliteForeignKeysConfig.java`).

## El esquema y sus migraciones

Hibernate no crea ni valida el esquema: `app/src/main/resources/application.properties:27` lo deja en none.
El esquema lo prepara `app/src/main/java/es/yaroki/educhronos/app/config/PreparadorEsquema.java`.
Lo llama `app/src/main/java/es/yaroki/educhronos/app/config/InicializadorEsquema.java` al arrancar, y la fábrica de bases al abrir otro curso.

La versión del esquema se guarda en la propia base, en PRAGMA user_version.
La versión de este commit es la 8 (`app/src/main/java/es/yaroki/educhronos/app/config/PreparadorEsquema.java:74`).
El preparador aplica estas reglas:

- Una base con un número mayor que 8 se rechaza sin tocarla.
- Una base vacía recibe el esquema completo de `app/src/main/resources/schema.sql` y queda sellada con la versión 8.
- Una base con número 0 y con tablas es anterior a las versiones: recibe `app/src/main/resources/esquema/001.sql` y queda en la 1.
- Desde ahí recibe, uno a uno y cada uno en su transacción, los scripts de `app/src/main/resources/esquema/` que le falten, del `app/src/main/resources/esquema/002.sql` al `app/src/main/resources/esquema/008.sql`.

## El recorrido de una generación

Una generación empieza en la vista del horario, `app/frontend/src/app/components/horario-view/horario-view.ts`.
El botón de generar llama a generar() (`app/frontend/src/app/components/horario-view/horario-view.ts:777`).
Ese método pide confirmación con el diálogo de `app/frontend/src/app/components/confirmar-generacion/`.
Después muestra el diálogo de espera de `app/frontend/src/app/components/generando-dialogo/` y llama al servicio (`app/frontend/src/app/components/horario-view/horario-view.ts:839-844`).
Mientras dura la generación, la guarda de `app/frontend/src/app/components/horario-view/sin-generacion-en-curso.ts` no deja salir de la vista.

El servicio es `app/frontend/src/app/services/horario.service.ts`.
Hace un POST a /api/horarios con el tiempo máximo en segundos (`app/frontend/src/app/services/horario.service.ts:29`).

En el servidor lo recibe `app/src/main/java/es/yaroki/educhronos/app/web/HorarioController.java:93-111`.
El controlador llama al servicio de generación y traduce sus fallos a respuestas HTTP.

El servicio es `app/src/main/java/es/yaroki/educhronos/app/service/GeneradorHorarioService.java`.
Su método generar está en la línea 340 y hace esto, en orden:

1. Pide permiso al estado del curso (`app/src/main/java/es/yaroki/educhronos/app/curso/EstadoCurso.java`). Si hay un cambio de curso, un duplicado u otra generación en marcha, rechaza esta.
2. Carga el catálogo de la base y lo traduce al modelo del solver con `app/src/main/java/es/yaroki/educhronos/app/mapper/CatalogoMapper.java:93`. Las aulas posibles de cada clase las calcula `app/src/main/java/es/yaroki/educhronos/app/mapper/DeduccionAulas.java`, y los bloqueos manuales los traduce `app/src/main/java/es/yaroki/educhronos/app/mapper/BloqueoMapper.java`.
3. Prevalida el problema con `app/src/main/java/es/yaroki/educhronos/app/service/PrevalidacionService.java`. Si hay algún error, no llama al solver.
4. Resuelve con el solver (`app/src/main/java/es/yaroki/educhronos/app/service/GeneradorHorarioService.java:466-470`). Si la petición no trae tiempo, usa educhronos.solver.max-segundos, que vale 600 (`app/src/main/resources/application.properties:85`).
5. Reparte las guardias con `app/src/main/java/es/yaroki/educhronos/app/service/ViaRepartoGuardias.java`. Si no llega al mínimo, no guarda nada.
6. Guarda el horario en una transacción (`app/src/main/java/es/yaroki/educhronos/app/service/GeneradorHorarioService.java:567-593`). `app/src/main/java/es/yaroki/educhronos/app/mapper/SolucionMapper.java:98` convierte la solución en sesiones.

Dentro del solver, la entrada es `solver/src/main/java/es/yaroki/educhronos/solver/cpsat/SolverHorario.java:114`.
Construye el modelo con objetivo en `solver/src/main/java/es/yaroki/educhronos/solver/cpsat/ModeloCpSat.java` y lo resuelve con CP-SAT.
Devuelve el estado, el objetivo y la solución.

De vuelta, el controlador responde con la proyección del horario recién guardado.
La construye el método proyectar de `app/src/main/java/es/yaroki/educhronos/app/service/GeneradorHorarioService.java:672`.
Es una lista plana de sesiones por día y tramo, en `app/src/main/java/es/yaroki/educhronos/app/web/dto/HorarioProyeccionDTO.java`.
La vista la pinta con la lógica de `app/frontend/src/app/horario/`.
La misma proyección es la entrada de las exportaciones de `app/src/main/java/es/yaroki/educhronos/app/exportacion/`.

## Quién fija las reglas duras y blandas

Las reglas del generador están en el código del solver, no en los datos.
Las duras, como no solapar a un profesor, un aula, un subgrupo o un grupo, están en `solver/src/main/java/es/yaroki/educhronos/solver/cpsat/ReglaDura.java`; las blandas, como las ventanas o el aula no preferida, en `solver/src/main/java/es/yaroki/educhronos/solver/cpsat/ReglaBlanda.java`.
Sus pesos son constantes de `solver/src/main/java/es/yaroki/educhronos/solver/cpsat/ModeloCpSat.java:70-112`: todos valen 1 y no se han calibrado con datos del centro.
Lo que decide el centro es la disponibilidad de cada profesor: en su rejilla, «No puede» es una restricción dura y «Prefiere no», una blanda (`app/frontend/src/app/components/profesores/disponibilidad/disponibilidad-dialogo.ts:36-37`).

## El frontend

El frontend es Angular, escrito en TypeScript. Arranca en `app/frontend/src/main.ts`, con la configuración de `app/frontend/src/app/app.config.ts:7-13`: el cliente HTTP y el enrutador.
Las rutas están en `app/frontend/src/app/app.routes.ts:31-55`:

- La ruta vacía es la portada.
- configuracion agrupa las ocho pantallas de datos: jornada, profesores, aulas, asignaturas, niveles, grupos, subgrupos y actividades. Sin hijo, va a jornada.
- horario muestra el horario vigente del curso abierto, y horario/ seguido de un id muestra uno concreto.
- Cualquier otra ruta vuelve a la portada.

Las pantallas son componentes de `app/frontend/src/app/components/`.
Los datos llegan por los servicios de `app/frontend/src/app/services/`.
Cada servicio del frontend pide por HttpClient a URLs relativas bajo /api; 10 de los 21 fijan la base en una constante base, como `app/frontend/src/app/services/actividad.service.ts:21`.
En el jar, la interfaz se sirve desde el mismo origen, así que la ruta relativa resuelve sola (`app/frontend/src/app/services/horario.service.ts:8-10`).
Al lado del servidor de desarrollo de Angular, `app/frontend/proxy.conf.json` reenvía /api al backend.
