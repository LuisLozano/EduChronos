# Métricas de la comparación con el oficial de 2026/2027

O-comparacion (H5), S228. Contrato sellado ANTES de calcular ninguna métrica sobre un horario
de Educhronos: se transcribe literalmente del guion F4 de S228, con la Enmienda 1 aplicada a
M2 y M5. Lo calcula `tools/carga-centro/comparar-con-oficial.py`.

Las citas «FORMA:n» son líneas de
`/home/luis/educhronos-aceptacion/s228/f1/FORMA-resumen.txt` (fuera del repositorio; sha256
`f5d4095edb83b866f54fbd16ea19193e28649e3d96e0bd9b232a111e60d184ab`).

## Contrato

C1 Representación común. Tuplas (grupo, día, tramo, asignatura, profesor). Oficial: entradas_del_pdf
   (FORMA:63-64) sobre los grupo-*.json. Educhronos: entradas_de_la_base (FORMA:65-66) sobre sesion.
   El script NO copia esas funciones: importa verificar-conservacion.py con importlib.
   P1. ¿Qué devuelve entradas_de_la_base como día y como tramo? FORMA:66 selecciona t.dia y
   t.orden, y el recreo es el orden 4 del día tipo (FORMA:38). Regla: el script normaliza el día
   LUNES..VIERNES a 1..5 y el tramo a la posición del tramo entre los LECTIVOS de su día, por orden
   (como p8_oficial, FORMA:124); si la función ya devuelve esa forma, no normaliza dos veces. Tras
   normalizar, todo día ∈ 1..5 y todo tramo ∈ 1..6 en los dos lados, o el script aborta.
   P2. ¿Las asignaturas y los profesores de las tuplas casan entre lados sin más transformación?
   Regla: sí, si la conservación de F3 dio 0 divergentes (agrega por (grupo, asig, prof), FORMA:73);
   el script lo comprueba: el multiconjunto (grupo, asig, prof) es igual en los dos lados, o aborta.
C2 Aulas. Las tuplas no llevan aula (FORMA:64, 66). Oficial: campo aula de las celdas de grupo
   (FORMA:3). Educhronos: sesion.aula_id (FORMA:141) → aula.codigo, con la misma unión
   sesion→plaza→subgrupo→grupo que entradas_de_la_base.
   P3. Los códigos de aula del volcado y de la base ¿son el mismo espacio? Regla: el script cuenta los
   códigos de aula del oficial que no existen en la tabla aula de la base y lo escribe; no aborta.
C3 Guardias. Oficial: ordinarias, celdas de profesor-* con asignatura exactamente «G» (FORMA:122);
   recreo, objetos de la lista recreo con GR o GRBib (FORMA:10, 30-32), uno por (profesor, día).
   Educhronos: tabla guardia del horario, tipo ORDINARIA o RECREO/BIBLIOTECA (FORMA:143); el tramo de
   una ordinaria, como en P1.
C4 Patrón. (grupo, asignatura) → actividad.patron_temporal (FORMA:135) por plaza.asignatura y
   plaza_subgrupo/subgrupo_grupo (FORMA:55, 57). Si un par tiene actividades con patrones distintos,
   su clase es «mixto». La clase sale de la base y vale igual para los dos lados.
C5 Universo. Profesores: los que aparecen en alguna tupla de cualquiera de los dos lados. Grupos: los
   30 de los volcados (FORMA:13). «Clase» es una tupla de C1: guardias, reuniones y funciones no son clase.

## Métricas (calculadas igual sobre el oficial y sobre cada horario)

M1 Huecos del profesorado (menos es mejor). Por profesor y día con clase: tramos lectivos sin clase
   entre su primera y su última clase. Se dan el total, el máximo por profesor y semana y el número de
   pares (profesor, día) con 3 o más.
M2 Asignatura repetida en el día (menos es mejor). La unidad es
   la FRANJA: un par (día, tramo) distinto de cada (grupo, asignatura), sin importar cuántas tuplas
   tenga (codocencia y desdoble cuentan una vez). Por (grupo, asignatura): número de franjas menos
   número de días distintos en que se da. Se dan la suma total y la suma por clase de patrón (C4),
   cada una con su mínimo forzado: Σ max(0, franjas − 5).
M3 Días con clase por profesor (sin dirección; contexto). Suma de días con clase y número de
   profesores con clase los 5 días.
M4 Guardias (menos es mejor). a) Suma de los cuadrados de las ordinarias por tramo lectivo (30
   tramos). b) Suma de los cuadrados de las de recreo por día (5 recreos).
M5 Aulas por grupo y día (menos es mejor). Por (grupo, día): número
   de aulas distintas NO NULAS usadas por sus tuplas; se suma max(0, aulas − 1). Las celdas o
   sesiones sin aula no cuentan, y su número por lado se escribe en metricas.tsv como dato.
   Limitación declarada: el volcado de grupos refleja las aulas de la vista de grupos, sin el parche
   de aulas de O-carga-2026.
M6 Coincidencia de tramo (sin dirección; contexto). Porcentaje de tuplas de Educhronos que también
   están en el oficial.

REGLA. Una métrica con dirección es «peor en todas» si su valor en las 5 corridas es peor que el del
oficial. En las de varias cifras, se aplica a la primera de las que da (M1: el total; M2: la suma total).

## Resoluciones de P1–P3 (parte A del guion F4 de S228)

P1 (regla aplicada): la base da dia como texto y tramo como orden con el recreo dentro:
   tramo_semanal LUNES = órdenes 1..7, el 4 con es_lectivo=0; 7 tramos por día, 6 lectivos, en los 5 días
   (consulta mode=ro). El oficial ya viene en 1..5 / 1..6 (FORMA:14-15). => normaliza solo el lado base:
   LUNES..VIERNES -> 1..5 y orden -> posición entre lectivos (orden 5 -> 4), como p8_oficial (FORMA:124).
P2 (regla aplicada): la conservación de F3 dio 0 divergentes sobre 583 claves en los horarios 4..8
   (s228/f3/conservacion-<id>.txt) => sí casan sin transformación; el script lo comprueba y aborta si no.
P3 (regla aplicada): no se resuelve leyendo; lo cuenta el script y lo escribe, sin abortar.

Enmienda 1 (S228): M2 cuenta franjas (día, tramo) y M5 excluye aulas nulas, por el contraste de la parte A
