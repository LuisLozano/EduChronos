# Por qué se decidió así

Responde a esta pregunta: qué decisiones fijan hoy cómo están el código, la construcción, las pruebas y la entrega de Educhronos, y por qué se tomaron.
Léelo antes de cambiar algo que parezca raro, para saber si se eligió a propósito.

Describe el commit ea5d805b8dbf.

Cada entrada dice qué se decidió, por qué y su sede. Si la sede no da el motivo, lo dice: aquí no se deduce ningún porqué.
En `docs/plan_trabajo_horarios.md`, «fila» es de la tabla «Decisiones permanentes». En los otros dos documentos, la sede es ficha y letra: las letras se repiten entre fichas.
Solo entran decisiones vigentes.

## Stack y estructura

- **El generador usa OR-Tools CP-SAT, en Java.** CP-SAT hace de desdobles y agrupamientos, aquí ubicuos, una restricción estructural; Timefold, una penalización. Sede: `docs/plan_trabajo_horarios.md`, fila Solver y «Por qué OR-Tools sobre Timefold».
- **Backend: Spring Boot 4.1.0 con Java 17.** El plan no da el motivo; `pom.xml` dice que la 3.5 se descartó por su fin de soporte. Sede: `docs/plan_trabajo_horarios.md`, fila Backend.
- **Datos en SQLite, con Hibernate y el dialecto de comunidad.** La Fase 6 pedía una base sin instalación en Windows; SQLite va embebido. Sede: `docs/plan_trabajo_horarios.md`, fila Base de datos y criterios de la Fase 6.
- **Una base es un centro.** Un curso nuevo es una base duplicada. La fuente no da el motivo. Sede: `docs/plan_trabajo_horarios.md`, fila Multi-centro.
- **Interfaz en Angular 21, probada con Vitest, no con Karma.** La fuente no da el motivo. Sede: `docs/plan_trabajo_horarios.md`, fila UI.
- **Maven multimódulo.** solver sin Spring ni Hibernate; app con Spring Boot, persistencia y API REST; el frontend, con frontend-maven-plugin. JPA y solver usan clases distintas y un mapper. La fuente no da el motivo. Sede: `docs/plan_trabajo_horarios.md`, filas Estructura del repositorio y Modelo del solver vs modelo de persistencia.

## El solver

- **El dominio del solver no conoce OR-Tools.** Las variables de CP-SAT están en cpsat; domain no importa cpsat ni io, para aislar OR-Tools. Sede: `docs/plan_trabajo_horarios.md`, «Decisiones tácticas», POJOs del dominio.
- **El solver identifica las entidades por su código.** Sin id sintético ni entidad Centro; el mapper resuelve los códigos, y hay enums solo donde se compara. La fuente no da el motivo. Sede: `docs/plan_trabajo_horarios.md`, «Decisiones tácticas», POJOs del dominio.
- **Leer el JSON y validarlo van separados.** Jackson solo en el cargador, mapper puro; de esto, la fuente no da el motivo. Tres invariantes se validan en la configuración, porque el JSON no trae particiones. Sede: `docs/plan_trabajo_horarios.md`, notas bajo «Decisiones permanentes».
- **Cada instancia tiene tramo e intervalo, sin solapes de profesor, aula ni subgrupo.** La fuente no da el motivo. Sede: `docs/plan_trabajo_horarios.md`, fila Variables del solver.
- **Repartir una actividad en días distintos es regla dura.** Lo es desde la Fase 2, para mantener la factibilidad pura, sin función objetivo. Sede: `docs/plan_trabajo_horarios.md`, «Decisiones tácticas», Distribución temporal en Fase 2, y fila Distribución temporal.
- **El problema valida sus tramos al construirse.** De esto, la fuente no da el motivo. El verificador no reutiliza el cálculo del modelo, para ser un oráculo independiente. Sede: `docs/gestion-historico.md`, O-disponibilidad, I y G.

## Datos y dominio

- **Una plaza admite varios profesores, sin titular ni apoyo.** Desde el principio, para no migrar esquema ni modelo con la codocencia. Sede: `docs/plan_trabajo_horarios.md`, «Decisiones tácticas», DTO de plaza multi-profesor, y fila Plaza ↔ Profesor.
- **La unidad del solver es el subgrupo.** Cada uno es de un grupo y un desdoble es una plaza con varios: así cabe entero el caso de la Fase 3. Del resto, la fuente no da el motivo. Sede: `docs/plan_trabajo_horarios.md`, filas Unidad atómica e Identidad de Subgrupo; `docs/gestion_proyecto.md`, §4, «Decisión arquitectónica consciente», Frontera Fase 2→3.
- **El profesor de cada plaza lo pone una persona.** La fuente no da el motivo. Sede: `docs/plan_trabajo_horarios.md`, fila Asignación profesor↔plaza.
- **No se guarda si una actividad es desdoble o agrupamiento.** Se infiere de sus plazas. Sede: `docs/plan_trabajo_horarios.md`, fila Campo Actividad.tipo.
- **Una actividad es CLASE, REUNION o FUNCION.** Las que no son clase no tienen alumnos y pueden ir sin aula, como las reuniones oficiales; su hora la pone el generador. Del tipo, la fuente no da el motivo. Sede: `docs/gestion-historico.md`, O-datos-centro, D, E y F.
- **El PDC es un grupo virtual con padre y alta propia.** El alta normal crea solo ordinarios: es la frontera entre dos objetivos. Sede: `docs/plan_trabajo_horarios.md`, fila PDC; `docs/gestion_proyecto.md`, §4, «Decisión arquitectónica consciente», D-S104-tipo.
- **La tutoría se marca en la actividad.** Religión y PTVE van por grupo o transversales, según el centro. La fuente no da el motivo. Sede: `docs/plan_trabajo_horarios.md`, filas Marca de tutoría y Religión/PTVE.
- **La disponibilidad es una tabla con tipo DURA o BLANDA y peso.** El peso no se expone hasta que el solver lo lea. De la tabla, la fuente no da el motivo. Sede: `docs/plan_trabajo_horarios.md`, fila Restricciones del profesorado; `docs/gestion_proyecto.md`, §4, «Limitación conocida», D-F8.5-E-a.
- **Los totales declarados son opcionales y avisan en las listas, en ambos sentidos.** Las horas configuradas son de clase; la sobrecarga cuenta todo, porque una reunión ocupa. Un cargo admite varios: el centro tiene tres jefes de estudio. Sede: `docs/gestion-historico.md`, O-datos-centro, A, C, G y J.
- **Cada base es un curso.** Los pines no pasan al curso nuevo: son ajustes de un horario. El solo lectura es de la aplicación: el del fichero falla y no vale en Windows. El curso cambia en caliente: el e2e usa una URL explícita. Sede: `docs/gestion-historico.md`, O-curso, A, B, D, E y F.
- **El esquema se migra con scripts propios.** `app/src/main/resources/schema.sql` crea una base vacía; una existente sube por `app/src/main/resources/esquema/`, porque un create table if not exists no llega a ella. Flyway se descartó: pesaba sin retorno. La versión se mira antes que nada, para no escribir en una base posterior. Sede: `docs/plan_trabajo_horarios.md`, fila Base de datos; `docs/gestion-historico.md`, O-base-tecnica, A, G y H.
- **Hibernate no crea las tablas.** El dialecto no emite claves ajenas en el DDL. Cada conexión las activa aparte. Sede: `docs/plan_trabajo_horarios.md`, fila Base de datos.
- **Guardar un horario va en una transacción real.** Una llamada interna no pasaba por el proxy y su anotación no se aplicaba. Sede: `docs/gestion-historico.md`, O-base-tecnica, D e I.
- **Cada generación deja rastro en el log.** Antes ni un 503 ni un 422 dejaban nada. Sede: `docs/gestion-historico.md`, O-base-tecnica, C y K.
- **El usuario elige cuánto dura la generación: de 10 a 60 minutos.** Parar en la primera solución daba horarios tres o cuatro veces peores. El 60 lo eligió el usuario frente al arquitecto. El backend acepta cualquier tiempo: los tests usan 5 s. Sede: `docs/plan_trabajo_horarios.md`, fila Presupuesto del solver; `docs/gestion-historico.md`, O-pre-demo, M, N y R.
- **Solo hay una generación a la vez.** La segunda se rechaza en un método sincronizado, para no dejar hueco entre comprobar y marcar. Sede: `docs/gestion-historico.md`, O-pre-demo, O.
- **Las reglas del bloqueo se validan dos veces, a propósito.** Unificarlas obligaría a cargar todo el catálogo para un pin. Sede: `docs/gestion_proyecto.md`, §4, «Decisión arquitectónica consciente», D-F8.2b-iv-a.
- **El 422 de la prevalidación reutiliza el cuerpo del fallo de generación.** La fuente no da el motivo. Sede: `docs/gestion-historico.md`, O-disponibilidad, H.
- **La aplicación deja configurar bien y avisa si no.** La fuente no da el motivo. Sede: `docs/gestion-historico.md`, O-aulas, AA.

## Aulas

- **Las reglas de aula son datos y el sistema deduce las aulas posibles.** Con candidatas a mano no elegiría con reglas. El aula escrita en una clase manda. Sede: `docs/plan_trabajo_horarios.md`, fila Asignación aula↔plaza; `docs/gestion-historico.md`, O-aulas, B y L.
- **Una asignatura nombra sus aulas, exclusivas o preferidas.** Sin tipos, porque el centro nombra aulas concretas. Una exclusiva es regla dura. Un aula «para profesoras concretas» es preferida de su materia, porque lo es por la materia. Sede: `docs/gestion-historico.md`, O-aulas, G, H, I y K.
- **El aula del grupo es dura en sus clases propias.** No cuesta factibilidad, pues un grupo no compite consigo mismo, y así lo hace el horario oficial. Una clase puede ir al aula de cualquiera de sus grupos; una solo de PDC no hereda la del padre, ocupada. Sede: `docs/gestion-historico.md`, O-aulas, C, Q y AC.
- **Lo que el centro pide «si es posible» es preferencia blanda, de peso 1.** Vale en clases del grupo entero, porque si no el aula del grupo la anulaba. Sin preferidas no se penaliza: no hay regla del centro. El diagnóstico la suma aparte: mover una clase no cambia su aula. Sede: `docs/gestion-historico.md`, O-aulas, D, J, R, S, T y U.
- **«No se usa» gana siempre, y una clase sin aula posible impide generar.** La prevalidación mira la carga de aulas. Capacidad vacía es sin límite. Un horario guardado conserva su aula si cambian las reglas. Del resto, la fuente no da el motivo. Sede: `docs/gestion-historico.md`, O-aulas, M, N, O, P y AB.

## Guardias

- **Las guardias se reparten tras las clases, sin CP-SAT.** Así no encarecen la generación, admiten el recreo y se recalculan tras un ajuste. Sede: `docs/gestion-historico.md`, O-guardias, A y R.
- **El mínimo por tramo es un dato del centro: la aplicación es general.** Vale 4 si no se guarda, se edita sobre la lista de profesores y cuenta ordinarias; toda actividad ocupa. El número por profesor es exacto; de esto, la fuente no da el motivo. Sede: `docs/gestion-historico.md`, O-guardias, B, D, F, J, K y O.
- **Si no hay reparto, no se guarda nada.** Un mínimo mayor que la suma es ERROR de la prevalidación, por ser condición necesaria demostrable; el diagnóstico lo mira con código propio. Un ajuste a mano lo recalcula todo y sin reparto se rechaza. Sede: `docs/gestion-historico.md`, O-guardias, G, I, P, Q, S, V y W.
- **Las guardias se ven por profesor y en un horario propio.** En su página van códigos: con nombres, la celda más llena no cabe. Van al final del CSV, aparte, para añadir tipos sin cambiar columnas. Sede: `docs/gestion-historico.md`, O-guardias, E, Y, Z y AA.
- **Biblioteca y convivencia son horas de un profesor, no guardias.** Son pocas, de personas concretas y sin cobertura por tramo. El recreo, todo tramo no lectivo, copia a las ordinarias; sus mínimos valen 0 sin guardar, y su coherencia la juzga la prevalidación. Sede: `docs/gestion-historico.md`, O-guardias-especiales, A, B, E, F, I, J y L.
- **El reparto se equilibra con un flujo de coste mínimo.** La guardia lleva tipo: ordinaria, recreo o biblioteca. La fuente no da el motivo. Sede: `docs/gestion-historico.md`, O-guardias-especiales, G, M, O, P y U.

## Interfaz

- **La disponibilidad se pinta con pincel.** Rotar por clic no da resultado claro en un día mezclado. El recreo no se pinta: en él no se programa. Símbolos en SVG, porque Unicode puede salir como emoji. Horas en las filas, como pide su criterio. Sede: `docs/gestion-historico.md`, O-disponibilidad, B, C, E, K, N y O.
- **Mientras se genera, la aplicación se bloquea en la pestaña.** Las listas de más de 10 opciones llevan filtro. La fuente no da el motivo. Sede: `docs/gestion-historico.md`, O-pre-demo, P y Q.
- **La rejilla expande las celdas densas, no las achica.** Con expansión, recortar pocas celdas es densidad. El aviso de ocupación ve solo la entidad de la vista: verlo todo repetiría las reglas de no solape. El recreo es una banda; en pantalla, solo el primero. Sede: `docs/gestion_proyecto.md`, §4, «Decisión arquitectónica consciente», palancas de altura y D-F8.6-B-b; `docs/gestion-historico.md`, O-guardias-especiales, V.
- **Las pantallas se piensan para 1920 px.** Se retiró un ancho máximo en vez de subirlo: Actividades no cabía. Desde una entidad se va a sus actividades; buscar un grupo no trae su PDC. Del resto, la fuente no da el motivo. Sede: `docs/gestion-historico.md`, O-interfaz, A, C, D, G y H.

## Construcción, versión y CI

- **La versión sale del tag y se ve bajo la marca.** El `pom.xml` lleva una revisión variable que la CI rellena con el tag, porque el pom repetía versión en dos entregas. Sede: `docs/plan_trabajo_horarios.md`, nota Versión de la aplicación; `docs/gestion-historico.md`, O-base-tecnica, B, J y L.
- **Las pruebas Java se corren desde la raíz.** Desde app usarían el solver instalado, no el del árbol. Sede: `docs/plan_trabajo_horarios.md`, fila Invocación de suites.
- **La CI tiene jobs independientes en un runner fijado.** Así un rojo no oculta otro, y la imagen solo cambia con un commit. El repositorio es público: runners gratuitos. El comprobador de rutas corre en cada push; de esto, la fuente no da el motivo. Sede: `docs/gestion-historico.md`, O-ci, A e I; `docs/gestion_proyecto.md`, O-doc-tecnica, DR.
- **El zip se publica en la Release del tag, sin humo.** Un artefacto de Actions caduca. La huella del jar va por otro canal que la entrega. El humo no llega al solver con base vacía, y el arranque lo prueba la aceptación. Sede: `docs/gestion-historico.md`, O-ci, B, D y G.

## Pruebas

- **El centro de la aceptación es pequeño y versionado.** Su tamaño lo fija el paso 2 del guion. Tiene una actividad de dos tramos, porque el producto deja crear bloques. Oráculos tal cual. Sede: `docs/gestion-historico.md`, O-aceptación, A, B y C.
- **«Windows limpio» es una cuenta estándar nueva en la máquina virtual.** Desinstalar por script solo borra lo conocido. Sede: `docs/gestion-historico.md`, O-aceptación, G.
- **La aceptación pone a 0 el mínimo de guardias ordinarias.** Con cuatro profesores ningún número llega a cuatro por tramo. Sede: `docs/gestion-historico.md`, O-guardias-especiales, D.
- **El e2e no genera horario.** Lo archivado se prueba en la JVM. Sede: `docs/gestion-historico.md`, O-curso, C.
- **El comprobador falla si una ruta no existe en el commit citado.** Si existe ahí y no en HEAD, avisa; las de esta carpeta se miran en el árbol. La fuente no da el motivo. Sede: `docs/gestion_proyecto.md`, O-doc-tecnica, DS y DT.

## Entrega e instalación

- **Se entrega un app-image con `scripts/Instalar.cmd`, sin MSI.** Sin desinstalación ni entrada en «Aplicaciones instaladas». El MSI por usuario instalaba en la carpeta de datos. Sede: `docs/plan_trabajo_horarios.md`, fila Empaquetado Windows; `docs/gestion_proyecto.md`, O-uso-centro, BP.
- **El zip llega por USB, sin firma.** El USB no marca el fichero como descargado; la carpeta compartida sí, y SmartScreen lo para. Sede: `docs/gestion_proyecto.md`, §4, «Decisión arquitectónica consciente», sin firma, y O-uso-centro, BL; `docs/gestion-historico.md`, O-ci, C, y O-demo-bundle, K.
- **Solo Windows 11.** El PC del centro lo es; solo se probó ahí. Sede: `docs/gestion_proyecto.md`, §4, «Limitación conocida», Windows 10, y O-uso-centro, BM.
- **La base de partida es la de 2025/2026, con nombre de curso.** Lo pone `tools/base-partida/preparar_base_partida.py`, y viaja junto al instalador. La fuente no da el motivo. Sede: `docs/gestion_proyecto.md`, O-uso-centro, BS, CJ y CK.
- **Mientras genera, se pide a Windows no suspender ni apagar la pantalla.** Una generación no debe cortarse por suspensión. JNA de OR-Tools, sin mezclar versiones. Sede: `docs/plan_trabajo_horarios.md`, nota JNA; `docs/gestion_proyecto.md`, O-uso-centro, BX y condición 4.
- **El USB lleva el zip, su huella, la base de partida y el manual en PDF.** Incluye la instalación y acaba en la guía de incidencias. Capturas con Playwright: Claude in Chrome no captura. Del resto, la fuente no da el motivo. Sede: `docs/gestion-historico.md`, O-manual, CS, CT, CU y CW.

## Herramientas y datos del centro

- **Las herramientas de Python usan solo la biblioteca estándar.** El único entorno virtual se rompió con otro Python del sistema. Sus tests, fuera de la CI: no lo pedía el objetivo de entonces. Sede: `docs/plan_trabajo_horarios.md`, fila Herramientas de datos del centro.
- **El catálogo del centro se deriva con un script.** El centro sacó tres versiones en una semana. Se cruza entre grupos y profesores: no hay horario completo por aulas. Las guardias, igual. El Excel de aulas es dato de prueba, no especificación. Sede: `docs/gestion-historico.md`, O-carga-2026, D y E; O-guardias, AD; O-aulas, A.
- **Los horarios del centro se versionan: son públicos.** Los documentos internos no, y `docs/horario-referencia/2026-2027/reglas-aulas.json` va sin nombres de personas. Sede: `docs/gestion_proyecto.md`, §4, «Decisión arquitectónica consciente», D-nombres-reales-en-repo; `docs/gestion-historico.md`, O-carga-2026, B, y O-aulas, Z.

## Limitaciones conocidas

Sede: `docs/gestion_proyecto.md`, §4, «Limitación conocida», en la fila que se nombra.

- **El puerto 8080 es fijo.** Con uno libre, la segunda instancia tendría que saber cuál usó la primera. Fila: Puerto 8080 fijo.
- **Cerrar durante una generación tarda de 35 a 40 s y la pierde.** Se revisó y no bloquea. Fila: D-apagado-espera-generacion.
- **El dialecto de comunidad no clasifica los fallos de clave ajena.** Depende de Hibernate. Fila: D-F8.5-E-c.
- **El modelo de la Fase 1 simplifica a la espera de datos reales o de fases futuras.** Fila de esas simplificaciones.
- **El perfil de escala compite por la CPU.** No bloquea la suite rápida. Fila: D25.
