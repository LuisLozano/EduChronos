# Por qué se decidió así

Responde a esta pregunta: qué decisiones fijan hoy cómo están el código, la construcción, las pruebas y la entrega de Educhronos, y por qué se tomaron.
Léelo antes de cambiar algo que parezca raro, para saber si se eligió a propósito.

Describe el commit ea5d805b8dbf.

Cada entrada dice qué se decidió, por qué y su sede. Si la sede no da el motivo, lo dice: aquí no se deduce ningún porqué.
En `docs/plan_trabajo_horarios.md`, «fila» es una fila de la tabla «Decisiones permanentes»; otra sección se nombra.
En `docs/gestion_proyecto.md` y `docs/gestion-historico.md`, la sede es la ficha del objetivo y la letra, porque las letras se repiten entre fichas.
Solo entran las vigentes; no las de planificación, proceso o sustituidas.

## Stack y estructura

- **El generador usa OR-Tools CP-SAT, en Java.** CP-SAT expresa los desdobles y agrupamientos, que aquí son ubicuos, como restricción estructural; Timefold, como penalizaciones. Sede: `docs/plan_trabajo_horarios.md`, fila Solver y «Por qué OR-Tools sobre Timefold».
- **El backend es Spring Boot 4.1.0 con Java 17.** El plan no da el motivo; el comentario de la versión en `pom.xml` dice que la línea 3.5 se descartó por su fin de soporte. Sede: `docs/plan_trabajo_horarios.md`, fila Backend.
- **Los datos van en SQLite, con Hibernate y el dialecto de comunidad.** La Fase 6 pedía una base que funcione en Windows sin instalar nada, y SQLite va embebido. Sede: `docs/plan_trabajo_horarios.md`, fila Base de datos y criterios de la Fase 6.
- **Una base es un centro.** No hay multicentro, y un curso nuevo es una base duplicada. La fuente no da el motivo. Sede: `docs/plan_trabajo_horarios.md`, fila Multi-centro.
- **La interfaz es Angular 21, con pruebas en Vitest y no en Karma.** La fuente no da el motivo. Sede: `docs/plan_trabajo_horarios.md`, fila UI.
- **Maven multimódulo.** solver va sin Spring ni Hibernate; app lleva Spring Boot, persistencia y API REST; el frontend entra con frontend-maven-plugin. Entidades JPA y modelo del solver son clases distintas, unidas por un mapper. La fuente no da el motivo. Sede: `docs/plan_trabajo_horarios.md`, filas Estructura del repositorio y Modelo del solver vs modelo de persistencia.

## El solver

- **El dominio del solver no conoce OR-Tools.** Las variables de CP-SAT viven en el paquete cpsat, y domain no importa nada de cpsat ni de io, para aislar OR-Tools. Sede: `docs/plan_trabajo_horarios.md`, «Decisiones tácticas», POJOs del dominio.
- **Las entidades del solver se identifican por su código.** Sin id sintético; los códigos se resuelven una vez, en el mapper. Enums solo donde el solver compara. No hay entidad Centro. La fuente no da el motivo. Sede: `docs/plan_trabajo_horarios.md`, «Decisiones tácticas», POJOs del dominio.
- **Leer el JSON y validarlo van separados.** Jackson solo está en el cargador, y el mapper es puro; de esto la fuente no da el motivo. I1, I3 e I6 se validan en la configuración, porque el JSON no transporta particiones. Sede: `docs/plan_trabajo_horarios.md`, notas bajo «Decisiones permanentes».
- **Cada instancia tiene su variable de tramo y su intervalo, con no solape por profesor, aula y subgrupo.** La fuente no da el motivo. Sede: `docs/plan_trabajo_horarios.md`, fila Variables del solver.
- **Repartir una actividad en días distintos es regla dura.** Se hizo dura en la Fase 2 para mantener la factibilidad pura, sin función objetivo. Sede: `docs/plan_trabajo_horarios.md`, «Decisiones tácticas», Distribución temporal en Fase 2, y fila Distribución temporal.
- **El problema valida sus tramos al construirse.** De esto la fuente no da el motivo. El verificador no reutiliza el cálculo del modelo, para seguir siendo un oráculo independiente. Sede: `docs/gestion-historico.md`, O-disponibilidad, I y G.

## Datos y dominio

- **Una plaza puede tener varios profesores, sin titular ni apoyo.** Se previó desde el primer commit para no migrar esquema ni modelo al llegar la codocencia. Sede: `docs/plan_trabajo_horarios.md`, «Decisiones tácticas», DTO de plaza multi-profesor, y fila Plaza ↔ Profesor.
- **La unidad del solver es el subgrupo.** Cada subgrupo es de un solo grupo, y un desdoble es una plaza con varios: así se modela entero el caso de la Fase 3. Del resto, la fuente no da el motivo. Sede: `docs/plan_trabajo_horarios.md`, filas Unidad atómica e Identidad de Subgrupo; `docs/gestion_proyecto.md`, §4, «Decisión arquitectónica consciente», Frontera Fase 2→3.
- **El profesor de cada plaza lo pone una persona, no el solver.** La fuente no da el motivo. Sede: `docs/plan_trabajo_horarios.md`, fila Asignación profesor↔plaza.
- **Que una actividad sea desdoble o agrupamiento no se guarda.** Se infiere de sus plazas. Sede: `docs/plan_trabajo_horarios.md`, fila Campo Actividad.tipo.
- **Una actividad es CLASE, REUNION o FUNCION.** Las que no son clase no tienen alumnos y pueden no tener aula, como las reuniones del horario oficial. La hora de una reunión la pone el generador. Del tipo, la fuente no da el motivo. Sede: `docs/gestion-historico.md`, O-datos-centro, D, E y F.
- **El PDC es un grupo virtual con grupo padre y tiene su propia vía de alta.** El alta normal crea solo grupos ordinarios, porque es la frontera entre dos objetivos. Sede: `docs/plan_trabajo_horarios.md`, fila PDC; `docs/gestion_proyecto.md`, §4, «Decisión arquitectónica consciente», D-S104-tipo.
- **La tutoría se marca en la actividad, no por el nombre de la asignatura.** Religión y PTVE van por grupo o transversales, según el centro. La fuente no da el motivo. Sede: `docs/plan_trabajo_horarios.md`, filas Marca de tutoría y Religión/PTVE.
- **La disponibilidad es una tabla con tipo DURA o BLANDA y un peso.** El peso no se expone y no se activará hasta que el solver lo lea. De la tabla, la fuente no da el motivo. Sede: `docs/plan_trabajo_horarios.md`, fila Restricciones del profesorado; `docs/gestion_proyecto.md`, §4, «Limitación conocida», D-F8.5-E-a.
- **Los totales declarados son opcionales y avisan en los dos sentidos, en las listas.** Las horas configuradas cuentan solo clases, pero la sobrecarga cuenta toda actividad, porque una reunión ocupa al profesor. Varios profesores pueden tener el mismo cargo, porque el centro tiene tres jefes de estudio. Sede: `docs/gestion-historico.md`, O-datos-centro, A, C, G y J.
- **Cada base es un curso.** Los pines no pasan al curso nuevo, porque son ajustes de un horario concreto. El solo lectura lo pone la aplicación, porque el del fichero no arranca o da 500 y no vale en Windows. El curso se cambia en caliente, porque el e2e usa una URL explícita. Sede: `docs/gestion-historico.md`, O-curso, A, B, D, E y F.
- **El esquema se migra con scripts propios.** `app/src/main/resources/schema.sql` crea una base vacía; una base existente sube por `app/src/main/resources/esquema/`, sellando su versión, porque un create table if not exists no llega a una base que ya existe. Flyway se descartó por pesar sin retorno. La versión se comprueba antes de todo script, para no escribir en una base posterior. Sede: `docs/plan_trabajo_horarios.md`, fila Base de datos; `docs/gestion-historico.md`, O-base-tecnica, A, G y H.
- **Hibernate no crea las tablas.** El dialecto de comunidad no emite claves ajenas en el DDL. Cada conexión activa además las claves ajenas. Sede: `docs/plan_trabajo_horarios.md`, fila Base de datos.
- **Guardar un horario va en una transacción real.** Una llamada interna no pasaba por el proxy, y su anotación no se aplicaba. Sede: `docs/gestion-historico.md`, O-base-tecnica, D e I.
- **Cada generación deja rastro en el log.** Antes ni un 503 ni un 422 dejaban nada. Sede: `docs/gestion-historico.md`, O-base-tecnica, C y K.
- **El usuario elige cuánto dura la generación: 10, 20, 30 o 60 minutos.** Parar en la primera solución daba horarios de tres a cuatro veces peores. El máximo de 60 lo decidió el usuario, contra la recomendación del arquitecto. El backend acepta cualquier tiempo, porque los tests usan 5 s. Sede: `docs/plan_trabajo_horarios.md`, fila Presupuesto del solver; `docs/gestion-historico.md`, O-pre-demo, M, N y R.
- **Solo hay una generación a la vez.** La segunda se rechaza en un método sincronizado, para no dejar hueco entre comprobar y marcar. Sede: `docs/gestion-historico.md`, O-pre-demo, O.
- **Las reglas del bloqueo se validan dos veces, a propósito.** Unificarlas obligaría a cargar el catálogo entero para dar de alta un pin. Sede: `docs/gestion_proyecto.md`, §4, «Decisión arquitectónica consciente», D-F8.2b-iv-a.
- **El 422 de la prevalidación reutiliza el cuerpo del fallo de generación, con causa propia.** La fuente no da el motivo. Sede: `docs/gestion-historico.md`, O-disponibilidad, H.
- **La aplicación permite una configuración correcta y avisa si no lo es.** La fuente no da el motivo. Sede: `docs/gestion-historico.md`, O-aulas, AA.

## Aulas

- **Las reglas de aula son datos, y el sistema deduce las aulas posibles.** Con candidatas escritas a mano, el sistema no elegiría con reglas. El aula escrita en una clase manda. Sede: `docs/plan_trabajo_horarios.md`, fila Asignación aula↔plaza; `docs/gestion-historico.md`, O-aulas, B y L.
- **Una asignatura nombra sus aulas, exclusivas o preferidas.** No hay compatibilidades por tipo, porque las reglas del centro nombran aulas concretas. Una exclusiva es regla dura. Un aula «para profesoras concretas» es preferida de su materia, porque lo es por la materia. Sede: `docs/gestion-historico.md`, O-aulas, G, H, I y K.
- **El aula del grupo es regla dura en sus clases propias.** No cuesta factibilidad, porque un grupo no compite consigo mismo, y así lo hace el horario oficial. Una clase puede ir al aula de cualquiera de sus grupos. Una clase solo de PDC no hereda el aula del padre, ocupada a esa hora. Sede: `docs/gestion-historico.md`, O-aulas, C, Q y AC.
- **Lo que el centro pide «si es posible» es preferencia blanda, de peso 1.** Valen también en las clases del grupo entero, porque si no el aula del grupo las anulaba. Sin preferidas no se penaliza, porque el centro no ha dado regla. En el diagnóstico es un total propio, porque mover una clase no cambia su aula. Sede: `docs/gestion-historico.md`, O-aulas, D, J, R, S, T y U.
- **«No se usa» gana siempre, y una clase sin aula posible impide generar.** La prevalidación comprueba además la carga de aulas, como condición necesaria. Capacidad vacía es sin límite. Un horario guardado conserva su aula aunque cambien las reglas. Del resto, la fuente no da el motivo. Sede: `docs/gestion-historico.md`, O-aulas, M, N, O, P y AB.

## Guardias

- **Las guardias se reparten después del horario de clases, sin CP-SAT.** Así no encarecen la generación, admiten el recreo y se recalculan solas tras un ajuste. Sede: `docs/gestion-historico.md`, O-guardias, A y R.
- **El mínimo por tramo es un dato del centro, porque la aplicación es general.** Sin valor guardado vale 4. Se edita encima de la lista de profesores. Cuenta solo las ordinarias, y cualquier actividad ocupa al profesor. El número de cada profesor es exacto; de esto, la fuente no da el motivo. Sede: `docs/gestion-historico.md`, O-guardias, B, D, F, J, K y O.
- **Si no hay reparto, no se guarda nada.** Que el mínimo supere la suma es ERROR de la prevalidación, por ser condición necesaria demostrable. El diagnóstico lo comprueba con código propio. Un ajuste a mano recalcula todas las guardias y se rechaza si no hay reparto. Sede: `docs/gestion-historico.md`, O-guardias, G, I, P, Q, S, V y W.
- **Las guardias se ven por profesor y en un horario propio.** En su página van códigos, porque con nombres la celda más llena no cabe. Van al final del CSV, en lista propia, para añadir tipos sin cambiar columnas. Sede: `docs/gestion-historico.md`, O-guardias, E, Y, Z y AA.
- **Biblioteca y convivencia son horas de un profesor, no guardias.** Son pocas, de personas concretas y sin cobertura por tramo. El recreo copia a las ordinarias, y recreo es todo tramo no lectivo. Sus mínimos valen 0 sin valor guardado, y su coherencia la juzga la prevalidación. Sede: `docs/gestion-historico.md`, O-guardias-especiales, A, B, E, F, I, J y L.
- **El reparto se equilibra con un flujo de coste mínimo.** La guardia lleva tipo: ordinaria, recreo o biblioteca. La fuente no da el motivo. Sede: `docs/gestion-historico.md`, O-guardias-especiales, G, M, O, P y U.

## Interfaz

- **La disponibilidad se pinta con pincel.** Con rotación por clic, pulsar un día con estados mezclados no tiene resultado claro. El recreo no se pinta, porque en él no se programa nada. Los símbolos son SVG, porque Unicode puede salir como emoji o depender de la fuente. Las horas van en las filas, como pide su criterio. Sede: `docs/gestion-historico.md`, O-disponibilidad, B, C, E, K, N y O.
- **Mientras se genera, toda la aplicación queda bloqueada en la pestaña.** Las listas de más de 10 opciones llevan filtro. La fuente no da el motivo. Sede: `docs/gestion-historico.md`, O-pre-demo, P y Q.
- **La rejilla expande las celdas densas en vez de achicarlas.** Con expansión, recortar unas pocas celdas es densidad. El aviso de ocupación ve solo la entidad de la vista, porque verlo todo repetiría otra vez las reglas de no solape. El recreo se pinta como banda, y en pantalla solo el primero. Sede: `docs/gestion_proyecto.md`, §4, «Decisión arquitectónica consciente», palancas de altura y D-F8.6-B-b; `docs/gestion-historico.md`, O-guardias-especiales, V.
- **Las pantallas se piensan para 1920 px.** Se retiró un ancho máximo en vez de subirlo, porque con más ancho Actividades seguía sin caber. Desde una entidad se llega a sus actividades, y buscar un grupo no trae su PDC. Del resto, la fuente no da el motivo. Sede: `docs/gestion-historico.md`, O-interfaz, A, C, D, G y H.

## Construcción, versión y CI

- **La versión sale del tag y se ve bajo la marca.** El `pom.xml` declara una revisión variable, y la CI le pasa el tag. Se decidió porque el pom decía la misma versión en dos entregas distintas. Sede: `docs/plan_trabajo_horarios.md`, nota Versión de la aplicación; `docs/gestion-historico.md`, O-base-tecnica, B, J y L.
- **Las pruebas Java se corren desde la raíz.** Desde el módulo app compilarían contra el solver instalado, no contra el del árbol. Sede: `docs/plan_trabajo_horarios.md`, fila Invocación de suites.
- **La CI tiene jobs independientes, en un runner fijado.** Así el rojo de una suite no oculta otro, y la imagen solo cambia con un commit. El repositorio es público y sus runners son gratuitos. El comprobador de rutas corre en cada push; de esto, la fuente no da el motivo. Sede: `docs/gestion-historico.md`, O-ci, A e I; `docs/gestion_proyecto.md`, O-doc-tecnica, DR.
- **El zip se publica en la Release del tag, sin prueba de humo.** Un artefacto de Actions caduca. La huella del jar viaja por un canal distinto de la entrega. El humo no llega al solver sobre una base vacía, y el arranque ya lo prueba la aceptación. Sede: `docs/gestion-historico.md`, O-ci, B, D y G.

## Pruebas

- **El centro de la aceptación es pequeño y versionado.** Su tamaño lo fija el paso 2 del guion, no la escala. Lleva una actividad de dos tramos, porque el producto deja crear bloques. Los oráculos se usan tal cual. Sede: `docs/gestion-historico.md`, O-aceptación, A, B y C.
- **«Windows limpio» es una cuenta estándar nueva en la máquina virtual.** Un script de desinstalación solo borraría lo que se conoce. Sede: `docs/gestion-historico.md`, O-aceptación, G.
- **El guion de aceptación pone a 0 el mínimo de guardias ordinarias.** Con cuatro profesores, ningún número de guardias llega a cuatro por tramo. Sede: `docs/gestion-historico.md`, O-guardias-especiales, D.
- **El e2e no genera horario.** Lo archivado se comprueba en las pruebas de la JVM. Sede: `docs/gestion-historico.md`, O-curso, C.
- **El comprobador de rutas falla si una ruta no existe en el commit citado.** Si existe en él y no en HEAD, avisa. Las rutas de esta carpeta se comprueban en el árbol de trabajo. La fuente no da el motivo. Sede: `docs/gestion_proyecto.md`, O-doc-tecnica, DS y DT.

## Entrega e instalación

- **Se entrega un app-image con `scripts/Instalar.cmd`, sin MSI.** No hay desinstalación ni entrada en «Aplicaciones instaladas». El MSI por usuario se descartó porque instala en la carpeta de datos. Sede: `docs/plan_trabajo_horarios.md`, fila Empaquetado Windows; `docs/gestion_proyecto.md`, O-uso-centro, BP.
- **El zip llega por USB, sin firma de código.** El USB no marca el fichero como descargado; la carpeta compartida sí, y SmartScreen lo para. Sede: `docs/gestion_proyecto.md`, §4, «Decisión arquitectónica consciente», sin firma, y O-uso-centro, BL; `docs/gestion-historico.md`, O-ci, C, y O-demo-bundle, K.
- **Solo Windows 11.** El PC del centro es Windows 11, y solo se ha probado ahí. Sede: `docs/gestion_proyecto.md`, §4, «Limitación conocida», Windows 10, y O-uso-centro, BM.
- **La base de partida es la de 2025/2026, con su nombre de curso.** El nombre lo pone `tools/base-partida/preparar_base_partida.py`, y la base viaja junto al instalador. La fuente no da el motivo. Sede: `docs/gestion_proyecto.md`, O-uso-centro, BS, CJ y CK.
- **Mientras genera, la aplicación pide a Windows que no suspenda el equipo ni apague la pantalla.** Una generación no debe cortarse por una suspensión. Usa la JNA que trae OR-Tools, sin mezclar versiones. Sede: `docs/plan_trabajo_horarios.md`, nota JNA; `docs/gestion_proyecto.md`, O-uso-centro, BX y condición 4.
- **El USB lleva el zip, su huella, la base de partida y el manual en PDF.** El manual incluye la instalación y acaba en la guía de incidencias. Sus capturas se hacen con Playwright, porque Claude in Chrome no captura. Del resto, la fuente no da el motivo. Sede: `docs/gestion-historico.md`, O-manual, CS, CT, CU y CW.

## Herramientas y datos del centro

- **Las herramientas de Python usan solo la biblioteca estándar.** El único entorno virtual se rompió al cambiar el Python del sistema. Sus tests quedan fuera de la CI por la regla R-terminado. Sede: `docs/plan_trabajo_horarios.md`, fila Herramientas de datos del centro.
- **El catálogo del centro se deriva con un script, no a mano.** El centro sacó tres versiones en una semana. Se cruza entre grupos y profesores, porque no hay horario completo por aulas. Las guardias siguen el mismo camino. El Excel de aulas es dato de prueba, no especificación. Sede: `docs/gestion-historico.md`, O-carga-2026, D y E; O-guardias, AD; O-aulas, A.
- **Los horarios del centro se versionan, porque son públicos.** Los documentos internos del centro no, y `docs/horario-referencia/2026-2027/reglas-aulas.json` no lleva nombres de personas. Sede: `docs/gestion_proyecto.md`, §4, «Decisión arquitectónica consciente», D-nombres-reales-en-repo; `docs/gestion-historico.md`, O-carga-2026, B, y O-aulas, Z.

## Limitaciones conocidas

- **El puerto 8080 es fijo.** Con uno libre, la segunda instancia tendría que saber cuál usó la primera. Sede: `docs/gestion_proyecto.md`, §4, «Limitación conocida», Puerto 8080 fijo.
- **Cerrar durante una generación tarda de 35 a 40 s y la pierde.** Revisada en S190: no bloquea. Sede: `docs/gestion_proyecto.md`, §4, «Limitación conocida», D-apagado-espera-generacion.
- **El dialecto de comunidad no clasifica los fallos de clave ajena.** Depende de Hibernate. Sede: `docs/gestion_proyecto.md`, §4, «Limitación conocida», D-F8.5-E-c.
- **El modelo de la Fase 1 lleva simplificaciones pendientes de datos reales o de fases futuras.** Son D2, D3, D8, D12 y parte de D22. Sede: `docs/gestion_proyecto.md`, §4, «Limitación conocida».
- **El perfil de escala compite por la CPU.** No bloquea la suite rápida. Sede: `docs/gestion_proyecto.md`, §4, «Limitación conocida», D25.
