# Modelo de datos — Fase 1

Entregable de cierre de Fase 1 del proyecto **Aplicación de Horarios Escolares**.
Este documento es la referencia autoritativa del esquema de datos hasta que sea
modificado explícitamente en fases posteriores con justificación registrada.

---

## 1. Resumen ejecutivo

El modelo se construye sobre **tres conceptos atómicos** que sustituyen a los
ocho tipos de sesión del enunciado original del proyecto:

- **Subgrupo de alumnos** — la unidad atómica del scheduling. Conjunto persistente
  de alumnos que se desplaza junto durante el curso. Es lo que "consume" tramos
  del horario. El solver razona sobre subgrupos, no sobre alumnos individuales.
- **Plaza** — una asignación concreta de profesor + aula + asignatura + subgrupos
  participantes. Es lo que ocupa físicamente un profesor y un aula en un tramo.
- **Actividad** — la unidad de planificación que el solver coloca en el tiempo.
  Agrupa una o más plazas que **comparten tramo obligatoriamente**. El solver
  decide el tramo de la actividad; las plazas heredan ese tramo.

Esta abstracción representa los ocho "tipos de sesión" del enunciado original
como combinaciones de tres ejes ortogonales:

1. Número de plazas simultáneas en la actividad (**K**).
2. Número de subgrupos asistentes a cada plaza.
3. Número de grupos administrativos cubiertos por la suma de subgrupos (**N**).

El alcance es **un único centro por instalación**. La aplicación no gestiona
multi-centro: la base de datos contiene los datos de un único centro educativo.
El cambio de curso académico se aborda como duplicación de base de datos
(ver Fase 10 del plan revisado).

---

## 2. Hallazgos críticos del análisis de datos reales

Los datos del IES de Sevilla aportados como referencia revelaron tres patrones
que afectan al diseño del modelo:

### Hallazgo A — Los grupos PDC son subgrupos transversales

Los subgrupos de Diversificación (3ºADi, 3ºBDi, 3ºCDi) **no son entidades
aisladas de su grupo de origen**. Para todas las sesiones de currículo
alternativo (ÁmbSL, ÁmbCM, IngDi, OyD, RefMt, TPMAR), los tres subgrupos Di
se agrupan en una única sesión en A8 con un único profesor. Solo para
algunas materias compartidas (p.ej. EPVA, EF; lista completa en §6.2) cada Di vuelve a su grupo de origen.

Implicación: el modelo trata 3ºADi como `GrupoAdministrativo` virtual con
identidad propia, pero los subgrupos que define se enlazan en `SubgrupoGrupo`
de forma cruzada para reflejar la agrupación transversal en A8.

### Hallazgo B — Desdoble y agrupamiento no son tipos disjuntos

Los desdobles de CyR en ESO cruzan grupos: las dos plazas (TEC3 + INF1) cubren
tres o cuatro grupos completos a la vez. Este caso no encaja en la definición
de "desdoble" (un grupo partido en dos) ni en la de "agrupamiento" (varios
grupos con N profesores). Estructuralmente, es la misma cosa: una actividad
con K plazas cuyos subgrupos cubren N grupos.

Implicación: el esquema **no usa discriminadores por tipo de sesión**.
Originalmente se contempló un campo `Actividad.tipo` como etiqueta
informativa para la UI; la validación contra 1ºESO A confirmó que es
redundante e introduce riesgo de desincronización con el contenido real
(una sesión etiquetada `ORDINARIA` que en realidad tiene dos profesores,
por ejemplo). El campo se elimina del esquema en favor de inferir la
naturaleza estructural a partir del contenido (ver §4.6 y §7).

### Hallazgo C — La densidad de simultaneidad es alta

Una misma "actividad coordinada" puede contener hasta 5 plazas y bloquear 4
grupos enteros (bloque de optativas de 1ºBach). En ESO, Religión+Atención
Educativa puede combinar 3 plazas, 4 grupos administrativos (incluyendo Di)
y consumir 3 aulas y 3 profesores en un solo tramo.

Implicación: el modelo soporta un número arbitrario de plazas por actividad
y un número arbitrario de subgrupos por plaza, sin techos predefinidos.

### Hallazgo D — Un subgrupo de alumnos puede aparecer en varias particiones

Validación contra 1ºBACH B (Sesión 4): la asignatura optativa DTec aparece
como plaza tanto en el bloque OPT1 (Lun 10, Mar 9) como en el bloque OPT2
(Jue 12:30, Vie 11:30), con el mismo profesor (DIB2) y la misma aula
(TALL1). El alumno que cursa DTec consume **4 horas semanales** repartidas
2+2 entre los dos bloques. La población de DTec es **idéntica** en ambos
bloques: son los mismos alumnos.

Implicación: `Subgrupo` deja de ser propiedad exclusiva de una partición.
Se modela como **entidad de primera clase** con identidad propia, enlazada
a particiones mediante una tabla N:M `SubgrupoParticion`. Plazas de
actividades distintas pueden referenciar el mismo `Subgrupo` cuando se
trata de la misma población de alumnos. La invariante S3 (sin colisión de
subgrupo) sigue funcionando: el solver obliga a que esas plazas vayan en
tramos distintos.

### Hallazgo E — En Bachillerato, la "tutoría" puede no llamarse TUT

Validación contra 1ºBACH B (Sesión 4): el horario no contiene una
asignatura "Tutoría" explícita. El rol tutorial está absorbido en **PTVE**
(Proyecto Transversal Vital y Emprendedor), 1 hora semanal con un profesor
que típicamente es el tutor del grupo (FIL2 en 1ºBach B, GH6 en 1ºBach A,
FIL2 en 1ºBach D). Patrones similares aparecen con Pat (Patrimonio) o
PTVA en otros niveles.

Implicación: la invariante S8 ("la tutoría la imparte el tutor") **no puede
atarse al nombre de la asignatura**. Se modela como un flag
`Actividad.requiere_tutor` (boolean) configurable por centro. La actividad
TUT en ESO y la actividad PTVE en Bach pueden marcarse ambas como
tutoriales si el centro así lo decide. La asignatura sigue siendo solo una
etiqueta de catálogo; la semántica "esto consume cupo de tutoría" reside
en la actividad.

### Hallazgo F — La opción Religión / PTVE puede no ser transversal entre grupos

Validación contra 1ºBACH B (Sesión 4): a diferencia de los bloques OPT1,
OPT2 y OPT3 (transversales entre 4 o 2 grupos del nivel), el par
Religión/PTVE cae en **tramos distintos para cada grupo** del nivel
(Lun 13:30 en B, Mar 10 en A, Jue 13:30 en C, Vie 13:30 en D). El profesor
REL1 reutiliza la misma asignación semanal pero en tramos distintos para
cada grupo. PTVE lo lleva el tutor de cada grupo en aulas distintas.

Implicación: **no requiere cambios en el modelo**. El esquema ya soporta
tanto la configuración transversal (sección 6.4: una actividad multi-grupo
con plazas paralelas) como la configuración per-grupo (sección 6.1: una
actividad por grupo administrativo con sus plazas). La elección entre
ambas formas es **decisión de configuración del centro**, no del modelo.
Se registra como observación operativa: dos centros distintos pueden
representar el mismo concepto pedagógico con configuraciones diferentes.

### Hallazgo G — Bloques obligatorios de duración > 2 tramos

Validación contra 1ºFPB (Sesión 5): la asignatura PS (Preparación de
superficies) con el profesor PAU2 ocupa los martes los tramos 8:00–9:00,
9:00–10:00 y 10:00–11:00 como un bloque continuo. La estructura
pedagógica de FPB incluye, por tanto, no solo los bloques de 2 tramos
previstos en el system prompt, sino **bloques de 3 tramos** (y
potencialmente más en otros centros de FPB).

Implicación: **no requiere cambios en el modelo**. El campo
`Actividad.duracion_tramos` ya es entero sin techo declarado y la
invariante S6 se evalúa para cualquier N ≥ 1 mediante recorrido de
`TramoSemanal.siguiente_inmediato_id`. Confirma la flexibilidad del
diseño. La UI de Fase 8 debe permitir introducir cualquier N sin
limitarlo a 1 o 2.

### Hallazgo H — Aulas implícitas no listadas en el horario de origen

Validación contra 1ºFPB (Sesión 5): el PDF del horario por grupos del
centro de referencia omite el aula en las celdas cuyo profesor es
titular del grupo (PAU2, TEC1, FOL3). La verificación cruzada con el
PDF por aulas confirma que las asignaturas técnicas de FPB (AMO, MECSO,
PS, IPE, CA, Tut) **no aparecen en ningún aula del listado**, aunque
físicamente existen (taller de mecanizado/carrocería).

Implicación: **no afecta al modelo de datos**. El modelo permite
declarar tantas aulas como sean necesarias con tipos arbitrarios. El
hallazgo afecta a la **capa de aplicación** (importador de datos, UI):
no se puede asumir que un horario externo contiene la totalidad de las
aulas del centro. Se registra como deuda **D8**. El aula TALL3, además,
está ocupada exclusivamente por sesiones de Comunicación y Sociedad de
los grupos FPB, no por sesiones técnicas; su tipificación como
`TALLER_FPB` en el system prompt original era engañosa, su uso real es
de aula teórica dedicada a CS para FPB.

### Hallazgo I — Pares de sesiones de la misma asignatura cruzando el recreo

Validación contra 1ºFPB (Sesión 5): la asignatura CS · GH1 aparece
consecutivamente en los tramos 10:00–11:00 y 11:30–12:30 tanto los lunes
como los miércoles. Son dos sesiones independientes desde el punto de
vista del modelo (el recreo rompe `TramoSemanal.siguiente_inmediato_id`,
por lo que no son contiguas a efectos de S6), pero el centro las
programa intencionalmente como pares pedagógicos.

Análisis de las opciones consideradas:

- **(a)** No modelar la preferencia. El solver coloca las sesiones
  independientemente; la sensación de "bloque" se consigue por
  casualidad o no se consigue. Riesgo bajo de pérdida de información si
  la preferencia no es crítica.
- **(b)** Reaprovechar `patron_temporal = AGRUPADA` en la actividad CS.
  No encaja: AGRUPADA penaliza repartir entre días distintos, pero CS
  necesita aparecer en 3 días (Lun, Mié, Vie), no en uno solo.
- **(c)** Modelar un bloque elástico de duración > 1 que tolera un tramo
  no lectivo entre medias. Cambio estructural en S6 o introducción de
  una invariante S6'. Coste alto.
- **(d)** Añadir una restricción blanda nueva al catálogo: penalización
  por falta de simetría alrededor del recreo en una asignatura marcada.
  No requiere cambios estructurales, sí una nueva entrada en el catálogo
  de restricciones.

Implicación: **decisión diferida**. En Fase 1 no se introduce ningún
cambio. Se evaluará en Fase 5 (instituto completo) si el solver
reproduce el patrón naturalmente. Si no lo hace y el centro considera
el patrón importante, se evaluará (d). Si el centro lo considera
estructural, se evaluará (c). Se registra como deuda **D9**.

### Hallazgo J — Co-docencia intra-aula

Validación contra 1ºESO A (Sesión actual): la asignatura LCL aparece en
4 tramos semanales con dos profesores (LEN2 y LEN8) y **una sola aula
listada en la celda del horario (A5)**. La inspección visual del PDF
confirma una única aula por celda en los 4 tramos. No es un desdoble:
el grupo no se parte en dos subgrupos disjuntos que vayan a aulas
distintas. Son dos profesores trabajando simultáneamente con el grupo
completo en la misma aula. El patrón es estructural en LCL de 1ºESO
del centro de referencia (presente en todos los grupos A/B/C/D).

Discriminador operativo frente a un desdoble: número de aulas listadas
en la celda del horario. Una sola aula con varios profesores ⇒
co-docencia intra-aula. Varias aulas con profesores distintos y
poblaciones disjuntas ⇒ desdoble o agrupamiento.

Implicación: la relación `Plaza ↔ Profesor` pasa de 1:1 (FK
`Plaza.profesor_id`) a M:N simétrica vía nueva tabla `PlazaProfesor`.
Una plaza tiene 1..N profesores: N=1 para los casos clásicos, N≥2 para
co-docencia. Nueva invariante I7 (toda plaza tiene al menos un
profesor). Reformulación textual (semánticamente equivalente) de S1
y S8 para operar sobre el conjunto `PlazaProfesor` en lugar de sobre
una FK única. El bloque LCL en §6.1 se reescribe: una sola actividad
con `repeticiones=4`, una sola plaza con `profesores={LEN2, LEN8}`,
`aula_fija=A5`, `subgrupos=(1ºA-Completo)`. La antigua
`Particion "LCL-1ESO"` se elimina (no había tal partición; el grupo
no se divide). El total semanal de 1ºA sigue siendo 30 sesiones.
Se registra deuda **D10** para la UX de configuración en Fase 8.

### Hallazgo K — Bloques de asignaturas alternativas intra-grupo no siempre son transversales

Validación contra los horarios de 1ºESO A/B/C/D del PDF (Sesión 13).
La versión inicial de §6.1 presentaba el bloque Fr2/ALCT como una única
actividad transversal con dos plazas cubriendo a los cuatro grupos
mediante subgrupos cruzados, paralela al bloque CyR/OyD/RefMt. La
verificación contra los datos reales muestra que **no es ese el patrón
en el centro de referencia**: Fr2/ALCT se organiza como cuatro
actividades independientes, una por grupo, con dos repeticiones
semanales cada una, en tramos que no coinciden entre grupos. El
profesor de Fr2 (FRA1) es común a las cuatro actividades e imparte
8 sesiones a la semana en total; el profesor de ALCT cambia por grupo
(LEN2, LEN9, LEN9, LEN5); el aula de Fr2 es la del grupo (A5, A11, A3,
A14); el aula de ALCT es A17 mayoritariamente, con una excepción en
1ºD que va a A10 en una de las dos celdas.

Es decir, **el solo hecho de que varios grupos tengan asignaturas
alternativas con la misma etiqueta no implica que las impartan en un
bloque coordinado**. Hay tres patrones distintos conviviendo en 1ºESO
del centro de referencia, y solo se distinguen inspeccionando los
tramos y aulas reales:

- *Transversal sobre N grupos:* CyR/OyD/RefMt — una sola actividad con
  K plazas que se imparte en el mismo tramo a los cuatro grupos.
- *Transversal por parejas:* Religión/ATED — Religión es multi-grupo
  (REL1 reúne A+B en A5 en un tramo y C+D en A3 en otro), y la Atención
  Educativa es per-grupo, cada grupo con su propio profesor y aula, en el
  mismo tramo que la Religión de su pareja. Verificado en 1ºESO cruzando el
  horario por aulas (S19): A+B jue 12:30 (ATED 1ºA→ING6/A17, 1ºB→GH5/A11);
  C+D mar 13:30 (ATED 1ºC→GH2/C00, 1ºD→FRA1/A14). Validado en el solver en
  Fase 5 (Bloque 1).
- *Per-grupo sin coordinación intergrupos:* Fr2/ALCT — cuatro
  actividades independientes con tramos no coincidentes.

Implicación: el modelo de Fase 1 cubre los tres patrones sin cambios
estructurales (la estructura unificada Actividad → Plaza → Subgrupo es
expresiva en todos los casos). El ejemplo de §6.1 ha sido corregido
para reflejar la estructura real de Fr2/ALCT. No es regresión del
modelo: es enseñanza operativa sobre el importador y sobre los
criterios de validación.

---

## 3. Principios del diseño

1. **Unificación**: una sola estructura `Actividad` → `Plaza` → subgrupos
   representa todas las variantes de sesión. No hay tablas separadas para
   "desdobles", "agrupamientos" ni "optativas".

2. **Subgrupos persistentes**: las particiones de un grupo son entidades
   nombradas y persistentes durante el curso, no construcciones ad-hoc por
   sesión. Esto facilita la trazabilidad y los reportes.

3. **El solver decide tramo, no profesor ni aula**: la asignación
   profesor-grupo-asignatura es decisión humana (configuración del centro);
   el solver únicamente coloca actividades en tramos. El aula puede ser
   fija o el solver elige entre un conjunto candidato.

4. **Simultaneidad estructural**: la coordinación temporal de plazas
   (desdobles, agrupamientos, optativas) es una propiedad estructural del
   modelo (todas las plazas de una actividad comparten tramo por construcción),
   no una restricción que el solver tenga que descubrir.

5. **Restricciones duras vs blandas explícitamente separadas**: nunca se
   colapsan en la misma columna. Las duras restringen el espacio de búsqueda;
   las blandas tienen peso numérico y entran en la función objetivo.

---

## 4. Esquema de entidades

Se presenta en pseudo-DDL agnóstico al motor. La implementación final será
SQLite + Hibernate.

### 4.1 Catálogo del centro

```
-- Parámetros globales clave-valor (pesos del solver, jornada, etc.)
Configuracion(
  clave PRIMARY KEY,
  valor
)
-- Claves típicas:
--   w_pasillo, w_planta, w_planta_dif, w_edificio
--   w_transicion_profesor, w_transicion_grupo
--   max_horas_consecutivas_profesor, max_ventanas_dia_profesor
--   penalizacion_ventana_profesor, penalizacion_ventana_grupo

Nivel(
  id PRIMARY KEY,
  codigo,           -- "1ESO", "2ESO", "1BACH", "1FPB"
  orden             -- entero para ordenación en UI
)

GrupoAdministrativo(
  id PRIMARY KEY,
  nivel_id FOREIGN KEY,
  codigo,           -- "1ºA", "3ºADi", "1ºBach C"
  tipo,             -- ORDINARIO | DIVERSIFICACION_PDC | VIRTUAL_OPTATIVA
  grupo_padre_id NULL FOREIGN KEY    -- para PDC: apunta al ordinario padre
)

Profesor(
  id PRIMARY KEY,
  codigo,           -- "MAT8", "LEN2", "BYG1"
  nombre_completo
)

Aula(
  id PRIMARY KEY,
  codigo,           -- "A5", "B07", "A12In", "TALL1", "Gim"
  tipo,             -- ORDINARIA | LAB_CIENCIAS | INFORMATICA | TALLER_TEC |
                    -- TALLER_PLASTICA | GIMNASIO | PISTA | TALLER_FPB | COMUN
  capacidad NULL,
  edificio NULL,    -- "A", "B", "C", "Talleres", "Polideportivo"
  planta NULL,      -- 0=baja, 1=primera, 2=segunda
  sector NULL       -- subdivisión opcional dentro del edificio
)

Asignatura(
  id PRIMARY KEY,
  codigo,           -- "Mat", "LCL", "CyR", "ÁmbSL"
  nombre_completo
)

AsignaturaAulaCompatible(
  asignatura_id FOREIGN KEY,
  tipo_aula,        -- valor del enum tipo de Aula
  PRIMARY KEY (asignatura_id, tipo_aula)
)

TramoSemanal(
  id PRIMARY KEY,
  dia,              -- LUNES | MARTES | MIERCOLES | JUEVES | VIERNES
  hora_inicio,      -- TIME
  hora_fin,         -- TIME
  es_lectivo,       -- BOOLEAN; false para el recreo
  orden,            -- entero global de ordenación
  siguiente_inmediato_id NULL FOREIGN KEY   -- tramo siguiente sin pausa
)
```

### 4.2 Particiones de alumnos y subgrupos

```
Particion(
  id PRIMARY KEY,
  codigo,           -- "CyR-1ESO", "RefMt-1ESO", "Diversif-3ESO"
  descripcion
)

Subgrupo(
  id PRIMARY KEY,
  codigo            -- "1ºA-CyR-Tec", "1Bach-Opt-DTec", "3ºADi"
)
-- Subgrupo es una entidad de primera clase con identidad propia,
-- INDEPENDIENTE de la partición en la que se declara. Un mismo Subgrupo
-- (misma población de alumnos) puede declararse en varias particiones
-- distintas y aparecer en plazas de actividades distintas manteniendo
-- identidad. Ejemplo: el subgrupo "1Bach-Opt-DTec" aparece en la
-- partición "Optativas-1Bach-Bloque1" y en "Optativas-1Bach-Bloque2"
-- con los mismos alumnos. Ver Hallazgo D y la invariante I6.

SubgrupoParticion(
  particion_id FOREIGN KEY,
  subgrupo_id FOREIGN KEY,
  PRIMARY KEY (particion_id, subgrupo_id)
)
-- Relación N:M entre particiones y subgrupos. Reemplaza la antigua
-- FK directa Subgrupo.particion_id.

SubgrupoGrupo(
  subgrupo_id FOREIGN KEY,
  grupo_id FOREIGN KEY,
  PRIMARY KEY (subgrupo_id, grupo_id)
)
-- La población de alumnos del subgrupo (los grupos administrativos que
-- contribuyen al subgrupo) se define UNA sola vez aquí, no por
-- partición. Si el subgrupo está en varias particiones, su población
-- es la misma en todas ellas por construcción.
```

### 4.3 Restricciones y relaciones del profesorado

```
ProfesorTutoria(
  profesor_id FOREIGN KEY,
  grupo_id FOREIGN KEY,
  rol,              -- TUTOR_PRINCIPAL | CO_TUTOR
  PRIMARY KEY (profesor_id, grupo_id)
)

ProfesorRestriccionHoraria(
  id PRIMARY KEY,
  profesor_id FOREIGN KEY,
  tramo_id FOREIGN KEY,
  tipo,             -- DURA | BLANDA
  peso DEFAULT 1,   -- aplica si tipo=BLANDA: penalización en f. objetivo
  motivo NULL       -- texto libre para auditoría
)
```

### 4.4 Distancias entre aulas

La distancia se calcula por defecto con una fórmula sobre los campos
`edificio`, `planta`, `sector` de `Aula`. La siguiente tabla almacena
excepciones puntuales.

```
DistanciaAulasOverride(
  aula_a_id FOREIGN KEY,
  aula_b_id FOREIGN KEY,
  distancia,        -- valor numérico que sobreescribe la fórmula
  PRIMARY KEY (aula_a_id, aula_b_id)
)
```

Fórmula por defecto (no se almacena, se calcula en runtime):

```
distancia(a, b) =
  0                                     si misma aula
  w_pasillo                             si mismo edificio + planta + sector
  w_planta                              si mismo edificio + planta, distinto sector
  w_planta_dif * |Δplanta|              si mismo edificio, distinta planta
  w_edificio + w_planta * |Δplanta|     si distinto edificio
```

### 4.5 Demanda curricular (informativa)

```
DemandaCurricular(
  id PRIMARY KEY,
  grupo_id FOREIGN KEY,
  asignatura_id FOREIGN KEY,
  horas_semanales
)
```

**Esta tabla NO es input del solver.** Es una herramienta de validación de la
capa de configuración: permite responder a la pregunta "¿la suma de plazas
configuradas cubre el currículo declarado del grupo?" antes de lanzar el
solver. La UI (Fase 8) la usará para alertar al usuario de currículos
incompletos o duplicados.

El solver razona exclusivamente sobre `Actividad`, `Plaza`, `Subgrupo` y
`repeticiones_por_semana`. La asignación profesor-aula-asignatura-subgrupos
ya está en `Plaza`, y el número de tramos a colocar ya está en
`Actividad.repeticiones_por_semana` × `Actividad.duracion_tramos`.

**Caso de cobertura parcial.** Cuando los alumnos de un grupo se reparten
entre asignaturas alternativas en el mismo bloque temporal (CyR/OyD/RefMt en
1ºESO, MCCSS/Latín en Bachillerato, Religión/Atención Educativa en
cualquiera), la tabla registra cada asignatura con sus horas tal como
aparece en el currículo, aunque ningún alumno del grupo curse las tres a la
vez. La validación contra la oferta de plazas se hace contando subgrupos
participantes, no asumiendo que todo el grupo cursa todo.

### 4.6 Actividades, plazas y participantes

```
Actividad(
  id PRIMARY KEY,
  codigo,                           -- identificador legible
  asignatura_id NULL FOREIGN KEY,   -- null si las plazas tienen distintas asignaturas
  duracion_tramos DEFAULT 1,        -- 1 normalmente, 2+ para bloques FPB obligatorios
  repeticiones_por_semana DEFAULT 1,
  patron_temporal DEFAULT 'NEUTRA', -- DISTRIBUIDA | AGRUPADA | NEUTRA
  requiere_tutor BOOLEAN DEFAULT false
)
-- No existe un campo `tipo` (ORDINARIA/DESDOBLE/AGRUPAMIENTO/...). La
-- naturaleza estructural de cada actividad es inferible de su contenido:
--   1 plaza, 1 profesor, subgrupo "Completo"     ⇒ ordinaria
--   1 plaza, ≥2 profesores, mismos subgrupos     ⇒ co-docencia intra-aula
--   ≥2 plazas con subgrupos disjuntos            ⇒ desdoble / agrupamiento / bloque de optativas
--   plazas cuyos subgrupos cubren >1 grupo       ⇒ actividad coordinada del nivel
-- Cualquier etiqueta redundante puede desincronizarse del contenido real
-- (ver decisión registrada en §7 y §10). Si la UI necesita una etiqueta
-- legible para humanos, la calcula a partir del contenido en la capa de
-- presentación, no como columna persistida.
--
-- patron_temporal aplica a las repeticiones de la actividad:
--   DISTRIBUIDA: penaliza dos instancias el mismo día (favorece reparto semanal)
--   AGRUPADA:    penaliza dos instancias en días distintos (favorece bloque)
--   NEUTRA:      sin preferencia temporal
-- requiere_tutor: si true, la invariante S8 obliga a que el profesor de
--   alguna plaza de la actividad sea tutor de un grupo cubierto por los
--   subgrupos de esa plaza. Permite que la "tutoría" se llame TUT, PTVE
--   o cualquier otra asignatura según el centro (ver Hallazgo E).

ActividadInstancia(
  id PRIMARY KEY,
  actividad_id FOREIGN KEY,
  indice,                           -- 1..repeticiones_por_semana
  UNIQUE (actividad_id, indice)
)

Plaza(
  id PRIMARY KEY,
  actividad_id FOREIGN KEY,
  asignatura_id FOREIGN KEY,        -- siempre rellena, aunque Actividad.asignatura_id sea null
  aula_fija_id NULL FOREIGN KEY     -- si null, ver PlazaAulaCandidata
)
-- La relación Plaza ↔ Profesor es M:N (ver PlazaProfesor). Una plaza
-- tiene 1..N profesores. Caso típico N=1: sesión ordinaria, desdoble,
-- agrupamiento. Caso N≥2: co-docencia intra-aula (Hallazgo J;
-- ejemplo: LCL de 1ºESO con LEN2+LEN8 en A5).

PlazaProfesor(
  plaza_id FOREIGN KEY,
  profesor_id FOREIGN KEY,
  PRIMARY KEY (plaza_id, profesor_id)
)
-- No hay rol titular/apoyo: la relación es simétrica. Si el centro
-- necesita distinguir, lo hace por convención en el código del profesor
-- o en una capa adicional fuera del modelo del solver.

PlazaAulaCandidata(
  plaza_id FOREIGN KEY,
  aula_id FOREIGN KEY,
  PRIMARY KEY (plaza_id, aula_id)
)
-- Solo se rellena si Plaza.aula_fija_id es NULL.
-- El solver elige una aula compatible entre estas candidatas.

PlazaSubgrupo(
  plaza_id FOREIGN KEY,
  subgrupo_id FOREIGN KEY,
  PRIMARY KEY (plaza_id, subgrupo_id)
)
```

### 4.7 Bloqueos manuales y salida del solver

```
SesionBloqueada(
  actividad_instancia_id PRIMARY KEY FOREIGN KEY,
  tramo_inicio_id FOREIGN KEY,
  aula_id NULL FOREIGN KEY          -- opcional: forzar también el aula
)
-- Antes de invocar el solver, el usuario puede fijar instancias a tramos concretos.
-- El solver respeta estos bloqueos como constantes.

HorarioGenerado(
  id PRIMARY KEY,
  nombre,                           -- texto descriptivo
  fecha_generacion TIMESTAMP,
  estado                            -- BORRADOR | DEFINITIVO | DESCARTADO
)

Sesion(
  id PRIMARY KEY,
  horario_id FOREIGN KEY,
  actividad_instancia_id FOREIGN KEY,
  tramo_inicio_id FOREIGN KEY,
  aula_id FOREIGN KEY,
  UNIQUE (horario_id, actividad_instancia_id)
)
```

---

## 5. Invariantes del modelo

Estas reglas debe garantizarlas la capa de configuración (validación al
guardar) o el propio solver. No son restricciones SQL convencionales pero son
parte integral del modelo.

### 5.1 Invariantes de configuración (capa de aplicación)

**I1. Las particiones cubren completamente al grupo.**
Para una `Particion P` cuyo alcance incluye al `GrupoAdministrativo G`, el
conjunto de `Subgrupo`s de `P` enlazados a `G` cubre la totalidad de los
alumnos de `G` sin solapamiento. Es una partición real.

**I2. Las plazas de una actividad usan subgrupos disjuntos.**
Dentro de una misma `Actividad`, ningún `Subgrupo` aparece en dos `Plaza`s
distintas. El mismo subgrupo no puede asistir a dos sesiones simultáneas.

**I3. Asignatura ↔ tipo de aula compatible.**
Si `Plaza.aula_fija_id` está rellena, el tipo del aula referenciada debe
estar en `AsignaturaAulaCompatible` para la asignatura de la plaza
(salvo asignaturas que admiten aula ordinaria).

**I4. Tutoría única por grupo (rol principal).**
Cada `GrupoAdministrativo` tiene exactamente un `ProfesorTutoria` con
`rol=TUTOR_PRINCIPAL`. Puede tener varios co-tutores.

**I5. Los PDC tienen grupo padre.**
Si `GrupoAdministrativo.tipo = DIVERSIFICACION_PDC`, entonces
`grupo_padre_id` no es null y apunta a un grupo `tipo = ORDINARIO`.

**I6. Identidad de subgrupo entre particiones.**
Un `Subgrupo` es entidad de primera clase con identidad propia. Si un
mismo `Subgrupo` aparece en varias `Particion`es (vía `SubgrupoParticion`),
representa la **misma población de alumnos** en todas ellas. La población
se define una sola vez en `SubgrupoGrupo`, no por partición. La capa de
configuración debe garantizar que el código del subgrupo y su población
no se duplican accidentalmente cuando se trata de poblaciones diferentes.

**I7. Toda plaza tiene al menos un profesor.**
Para toda `Plaza P`, existe al menos un registro en `PlazaProfesor` con
`plaza_id = P.id`. Es decir, `|P.profesores| ≥ 1`. Una plaza sin profesor
es una configuración inválida. Caso típico: `|P.profesores| = 1` (sesión
ordinaria, desdoble, agrupamiento). Caso N≥2: co-docencia intra-aula.

### 5.2 Invariantes del scheduling (responsabilidad del solver)

**S1. Sin colisión de profesor.**
Para dos `Sesion`s con el mismo `tramo_inicio_id`, los conjuntos
`P.profesores` de toda `Plaza P` de la primera y toda `Plaza Q` de la
segunda son disjuntos. Es decir, ningún profesor aparece simultáneamente
en una plaza de la primera sesión y en una plaza de la segunda. La
verificación se computa sobre `PlazaProfesor` (un mismo profesor puede
estar en varias plazas de la misma actividad si se diera el caso, pero
no en plazas de actividades distintas en el mismo tramo).

**S2. Sin colisión de aula.**
Para dos `Sesion`s con el mismo `tramo_inicio_id`, las aulas usadas no se
solapan. El aula "usada" por una plaza es su `aula_fija` si está fijada, o el
aula que el solver elige entre `aulas_candidatas` si es variable. El no-solape
se impone sobre el aula efectivamente ocupada, no solo sobre la fija
(implementado en Sesión 16, Fase 3 commit 2: intervalos opcionales por
candidata que entran en el `addNoOverlap` del aula cuando su literal de
presencia es verdadero). Corolario por el glosario: como el uso compartido de
un aula por varios grupos se modela como UNA plaza (co-docencia / multi-grupo,
no varias plazas con la misma aula), dos plazas distintas de la misma
actividad que ocupen la misma aula en el mismo tramo son una colisión de S2,
no un uso compartido.

**S3. Sin colisión de subgrupo.**
Para dos `Sesion`s con el mismo `tramo_inicio_id`, ningún `Subgrupo` está
en una `PlazaSubgrupo` de la primera y otra de la segunda.

**S4. Tramo lectivo.**
`Sesion.tramo_inicio_id` apunta a un `TramoSemanal` con `es_lectivo = true`.

**S5. Bloqueos respetados.**
Si existe `SesionBloqueada` para una `ActividadInstancia`, la `Sesion`
generada usa exactamente ese `tramo_inicio_id` (y, si fue especificada,
esa `aula_id`).

**S6. Bloque obligatorio.**
Si `Actividad.duracion_tramos > 1`, la `Sesion` ocupa N tramos consecutivos
lectivos (verificados por `TramoSemanal.siguiente_inmediato_id`).

**S7. Restricciones duras del profesorado.**
Ningún profesor de ninguna plaza está asignado a una `Sesion` cuyo tramo
aparezca en `ProfesorRestriccionHoraria` con `tipo = DURA` para ese profesor.

**S8. Tutoría correcta.**
Para toda `Actividad` con `requiere_tutor = true`, existe al menos una
`Plaza P` de la actividad y un profesor `T ∈ P.profesores` tal que `T`
está en `ProfesorTutoria` con `rol = TUTOR_PRINCIPAL` para un
`GrupoAdministrativo` cubierto por los subgrupos de `P`. El nombre de
la asignatura es irrelevante para esta invariante: la actividad puede
llamarse TUT, PTVE, Pat o cualquier otra según el centro. La semántica
reside en el flag de la actividad, no en el catálogo de asignaturas
(ver Hallazgo E). En el caso de co-docencia, basta con que UNO de los
profesores de la plaza sea tutor del grupo cubierto: la invariante no
exige que todos los profesores lo sean.

**S9. Sin colisión de grupo.**
Para dos `Sesion`s con el mismo `tramo_inicio_id`, ningún `GrupoAdministrativo`
es tocado por ambas. Un grupo es "tocado" por una sesión si algún `Subgrupo`
de alguna `PlazaSubgrupo` de la sesión pertenece a ese grupo (vía
`SubgrupoGrupo`). Es la contraparte en el solver de la invariante de
configuración I1: cuando un grupo se reparte en varios subgrupos (desdobles,
agrupamientos, bloques de optativas), I1 no se transporta en el JSON del solver
(no hay particiones), de modo que el no-solape por subgrupo (S3) deja de
garantizar que el grupo no esté en dos sitios a la vez. S9 lo impone
directamente sobre el grupo.

S9 es **ciega al `grupo_padre`**: agrupa por la identidad del grupo del
subgrupo, nunca por su padre. Un grupo PDC (p.ej. `3ºADi`) y su grupo padre
(`3ºA`) son grupos independientes para S9. Sus sesiones compartidas (p.ej. EF, EPVA; lista completa en §6.2)
se modelan con una plaza que lista subgrupos de ambos grupos —y entonces S9 las
trata como una sola sesión que toca a los dos, sin pedir que se no-solape
consigo misma—; sus sesiones propias quedan libres. Fundir PDC con su padre en
S9 sería incorrecto: forzaría a que `3ºA` y `3ºADi` nunca pudieran estar en
tramos distintos, lo cual contradice los datos reales (ver §6.2).

Como S1, S2 y S3, S9 cuenta el intervalo de cada `ActividadInstancia` una sola
vez por grupo: una actividad cuyas plazas cubren los cuatro grupos de 1ºESO
(bloque CyR/OyD/RefMt) aporta un único intervalo a cada uno de los cuatro
grupos. Lo que queda prohibido es que **otra** actividad que toque ese grupo
caiga en el mismo tramo. Es el mecanismo por el que las seis plazas
simultáneas del bloque bloquean los cuatro grupos completos en ese tramo
(ver §6.1).

En el dominio reducido del solver (Fases 2–5, sin particiones) todo `Subgrupo`
pertenece a un único grupo, así que "el grupo del subgrupo" es directo. Cuando
en Fase 5 aparezca un subgrupo cuya población son alumnos de varios grupos
(optativas de Bachillerato sobre `SubgrupoGrupo` N:M), S9 lo tratará como
tocando a todos esos grupos sin cambio estructural.

---

## 6. Validación contra los criterios de Fase 1

### 6.1 Criterio: horario completo de 1ºESO A

Este caso es el principal banco de pruebas del modelo. El horario real
contiene seis tramos no triviales: dos bloques con seis plazas simultáneas
(CyR + OyD + RefMt), dos bloques con dos plazas (Fr2 / ALCT), un bloque con
dos plazas (Religión / Atención Educativa), cuatro tramos de desdoble de
LCL, una tutoría y veintiún tramos ordinarios.

#### Principio aplicable al modelado

**Cuando varios alumnos del mismo grupo cursan asignaturas alternativas en
un mismo bloque temporal, hay UNA partición unificada de ese bloque, no una
partición por asignatura.** El razonamiento es estructural:

- La invariante I1 exige que los subgrupos de una partición cubran el grupo
  por completo. Si se definiera una partición "CyR-1ESO" con solo dos
  subgrupos (los del desdoble de CyR), faltarían los alumnos que cursan
  OyD o RefMt y se violaría I1.
- En la práctica del centro, todos los alumnos del grupo están haciendo
  algo en ese tramo (CyR, OyD o RefMt). La partición correcta es la del
  bloque temporal completo: tantos subgrupos por grupo como destinos
  alternativos haya.

Este principio aplica a todos los bloques de "optativas alternativas dentro
del grupo": CyR/OyD/RefMt, Fr2/ALCT, Religión/Atención Educativa,
MCCSS/Latín en Bachillerato, etc.

#### Configuración: grupos del nivel

```
Niveles:        1ESO
Grupos:         1ºA, 1ºB, 1ºC, 1ºD (tipo=ORDINARIO, nivel=1ESO)
```

#### Configuración: particiones y subgrupos

```
Particion "Grupo-completo-1ESO":
  Subgrupo "1ºA-Completo" → {1ºA}
  Subgrupo "1ºB-Completo" → {1ºB}
  Subgrupo "1ºC-Completo" → {1ºC}
  Subgrupo "1ºD-Completo" → {1ºD}

Particion "Bloque-CyR_OyD_RefMt-1ESO" (UNIFICADA; cubre los 4 grupos):
  Subgrupo "1ºA-CyR-Tec"    → {1ºA}
  Subgrupo "1ºA-CyR-Inf"    → {1ºA}
  Subgrupo "1ºA-OyD"        → {1ºA}
  Subgrupo "1ºA-RefMt-MAT6" → {1ºA}
  Subgrupo "1ºA-RefMt-MAT7" → {1ºA}
  Subgrupo "1ºA-RefMt-MAT4" → {1ºA}
  (idem 6 subgrupos por cada uno de 1ºB, 1ºC, 1ºD → 24 subgrupos en total)

Particion "Fr2_ALCT-1ºA":
  Subgrupo "1ºA-Fr2"  → {1ºA}
  Subgrupo "1ºA-ALCT" → {1ºA}

Particion "Fr2_ALCT-1ºB":
  Subgrupo "1ºB-Fr2"  → {1ºB}
  Subgrupo "1ºB-ALCT" → {1ºB}

Particion "Fr2_ALCT-1ºC":
  Subgrupo "1ºC-Fr2"  → {1ºC}
  Subgrupo "1ºC-ALCT" → {1ºC}

Particion "Fr2_ALCT-1ºD":
  Subgrupo "1ºD-Fr2"  → {1ºD}
  Subgrupo "1ºD-ALCT" → {1ºD}

Particion "Bloque-Relig_ATED-1ºA":
  Subgrupo "1ºA-Relig" → {1ºA}
  Subgrupo "1ºA-ATED"  → {1ºA}
  -- (1ºB-Relig pertenece a la partición de 1ºB; aparece en la plaza de
  --  Religión de esta actividad porque el bloque es por parejas A+B, pero
  --  su población es de 1ºB. El bloque completo se modela en §6.4.)
```

Verificación de I1: en cada partición, la suma de subgrupos enlazados a un
grupo cubre el grupo completo. ✅

#### Actividades de 1ºA — sesiones ordinarias

Cada asignatura no compartida con otros grupos se modela como actividad de
una sola plaza con el subgrupo completo del grupo:

```
Actividad "Mat-1ºA" (asignatura=Mat, repeticiones=4, patron_temporal=DISTRIBUIDA):
  Plaza Mat, MAT8, A5, subgrupos=(1ºA-Completo)

Actividad "ByG-1ºA" (asignatura=ByG, repeticiones=3, patron_temporal=DISTRIBUIDA):
  Plaza ByG, BYG1, A5, subgrupos=(1ºA-Completo)

Actividad "Ing-1ºA" (asignatura=Ing, repeticiones=4, DISTRIBUIDA):
  Plaza Ing, ING4, A5, subgrupos=(1ºA-Completo)

Actividad "Geo-1ºA" (asignatura=Geo, repeticiones=3, DISTRIBUIDA):
  Plaza Geo, GH6, A5, subgrupos=(1ºA-Completo)

Actividad "EF-1ºA" (asignatura=EF, repeticiones=3, DISTRIBUIDA):
  Plaza EF, EFI2, Gim, subgrupos=(1ºA-Completo)
-- Las tres celdas de EF del horario de 1ºA aparecen con el grupo completo
-- en Gim. Nota operativa: el PDF del horario por aulas muestra EFI3 (en
-- lugar de EFI2) en una de las tres celdas; es una inconsistencia de los
-- datos de origen, no del modelo (ver deuda D8). El modelo soporta
-- cualquier asignación profesor↔plaza; la conciliación es responsabilidad
-- del importador.

Actividad "Mús-1ºA" (asignatura=Mús, repeticiones=2, DISTRIBUIDA):
  Plaza Mús, MUS1, A5, subgrupos=(1ºA-Completo)

Actividad "PLAS-1ºA" (asignatura=PLAS, repeticiones=1, NEUTRA):
  Plaza PLAS, DIB1, A5, subgrupos=(1ºA-Completo)

Actividad "TUT-1ºA" (asignatura=TUT, repeticiones=1, NEUTRA):
  Plaza TUT, GH6, A5, subgrupos=(1ºA-Completo)
```

El profesor GH6 es el tutor del grupo (ProfesorTutoria registra el vínculo).
La invariante S8 (tutoría correcta) se cumple al asignar el profesor del
tutor en la única plaza de la actividad TUT.

#### Actividades de 1ºA — co-docencia de LCL

```
Actividad "LCL-1ºA" (asignatura=LCL, repeticiones=4, DISTRIBUIDA):
  Plaza LCL, profesores={LEN2, LEN8}, A5, subgrupos=(1ºA-Completo)
```

Una sola plaza con dos profesores en la misma aula (A5) sobre el grupo
completo. No hay desdoble: el PDF del horario lista una única aula en
cada celda de LCL, y los dos profesores aparecen juntos. La invariante
I7 (toda plaza tiene al menos un profesor) se satisface con `|profesores|=2`.
La invariante S1 (sin colisión de profesor) opera sobre el conjunto
`Plaza.profesores`: ni LEN2 ni LEN8 pueden estar simultáneamente en
otra sesión en ese tramo.

Discriminador operativo frente a un desdoble: número de aulas listadas
en la celda del horario del grupo. Una sola aula con varios profesores
⇒ co-docencia intra-aula (esta sección). Varias aulas con profesores
distintos y subgrupos disjuntos ⇒ desdoble o agrupamiento (siguiente
sección, bloque CyR/OyD/RefMt). ✅

#### Actividad coordinada del nivel — bloque CyR / OyD / RefMt

Esta es la actividad central del nivel y de la validación. **Una sola
actividad** con seis plazas; cada plaza cubre alumnos de los cuatro grupos
de 1ºESO mediante subgrupos transversales:

```
Actividad "Bloque-CyR_OyD_RefMt-1ESO" (asignatura=NULL, repeticiones=2, NEUTRA):
  Plaza CyR,   TEC3, candidatas={A5, B07},   subgrupos=(1ºA-CyR-Tec, 1ºB-CyR-Tec, 1ºC-CyR-Tec, 1ºD-CyR-Tec)
  Plaza CyR,   INF1, A12In (fija),           subgrupos=(1ºA-CyR-Inf, 1ºB-CyR-Inf, 1ºC-CyR-Inf, 1ºD-CyR-Inf)
  Plaza OyD,   FIL3, candidatas={A11, A5},   subgrupos=(1ºA-OyD,     1ºB-OyD,     1ºC-OyD,     1ºD-OyD)
  Plaza RefMt, MAT6, candidatas={A3, A11},   subgrupos=(1ºA-RefMt-MAT6, ..., 1ºD-RefMt-MAT6)
  Plaza RefMt, MAT7, candidatas={A14, A3},   subgrupos=(1ºA-RefMt-MAT7, ..., 1ºD-RefMt-MAT7)
  Plaza RefMt, MAT4, candidatas={A10, A14},  subgrupos=(1ºA-RefMt-MAT4, ..., 1ºD-RefMt-MAT4)
```

**Verificación.**

- *Mismo tramo para las seis plazas:* garantizado por construcción al
  pertenecer a la misma `Actividad`.
- *Bloqueo simultáneo de los cuatro grupos:* los seis subgrupos de un mismo
  grupo (p.ej. 1ºA-CyR-Tec ∪ 1ºA-CyR-Inf ∪ 1ºA-OyD ∪ 1ºA-RefMt-MAT6 ∪
  1ºA-RefMt-MAT7 ∪ 1ºA-RefMt-MAT4) cubren 1ºA completo, según I1. Por S3
  (sin colisión de subgrupo), 1ºA no puede tener ninguna otra sesión en
  ese tramo. Lo mismo para B, C, D.
- *No colisión de profesores y aulas entre las seis plazas:* I2 garantiza
  que los seis subgrupos son disjuntos dentro de la actividad; los seis
  profesores son distintos por configuración; las aulas se resuelven por
  candidatas distintas en cada instancia (aula variable mié vs vie).
- *Las dos instancias caen en tramos distintos:* `repeticiones_por_semana=2`
  produce dos `ActividadInstancia` (índice 1 e índice 2), y el solver las
  coloca en tramos distintos para no violar S1/S2/S3 al cruzarse consigo
  misma.

✅ Criterio cubierto.

#### Actividad por grupo — bloque Fr2 / ALCT

A diferencia del bloque CyR/OyD/RefMt, que es genuinamente transversal
a los cuatro grupos (las seis plazas se imparten en el mismo tramo a
todos), el bloque Fr2/ALCT del centro de referencia se organiza como
**cuatro actividades independientes**, una por grupo. Cada una tiene
dos plazas con poblaciones disjuntas (Fr2 y ALCT cubren al grupo
completo entre los dos) y dos repeticiones por semana. Los tramos no
coinciden entre grupos. Ver Hallazgo K en §2.

Actividad "Bloque-Fr2_ALCT-1ºA" (asignatura=NULL, repeticiones=2, DISTRIBUIDA):
  Plaza Fr2,  FRA1, A5,  subgrupos=(1ºA-Fr2)
  Plaza ALCT, LEN2, A17, subgrupos=(1ºA-ALCT)

Actividad "Bloque-Fr2_ALCT-1ºB" (asignatura=NULL, repeticiones=2, DISTRIBUIDA):
  Plaza Fr2,  FRA1, A11, subgrupos=(1ºB-Fr2)
  Plaza ALCT, LEN9, A17, subgrupos=(1ºB-ALCT)

Actividad "Bloque-Fr2_ALCT-1ºC" (asignatura=NULL, repeticiones=2, DISTRIBUIDA):
  Plaza Fr2,  FRA1, A3,  subgrupos=(1ºC-Fr2)
  Plaza ALCT, LEN9, A17, subgrupos=(1ºC-ALCT)

Actividad "Bloque-Fr2_ALCT-1ºD" (asignatura=NULL, repeticiones=2, DISTRIBUIDA):
  Plaza Fr2,  FRA1, A14,                  subgrupos=(1ºD-Fr2)
  Plaza ALCT, LEN5, candidatas={A17, A10}, subgrupos=(1ºD-ALCT)

Verificación:

- *Subgrupos cubren el grupo entero (I1):* en cada partición Fr2_ALCT-1ºX,
  los dos subgrupos cubren entre ambos el grupo 1ºX. ✅
- *Subgrupos disjuntos dentro de una actividad (I2):* cada actividad
  tiene dos plazas con subgrupos disjuntos del mismo grupo. ✅
- *Mismo tramo para las dos plazas de cada actividad:* garantizado por
  construcción al pertenecer a la misma `Actividad`. ✅
- *FRA1 sin colisión consigo mismo (S1):* las cuatro actividades
  Fr2_ALCT-1ºX tienen plazas que incluyen a FRA1. El solver no puede
  colocarlas dos en el mismo tramo. ✅
- *LEN9 sin colisión consigo mismo (S1):* las actividades Fr2_ALCT-1ºB
  y Fr2_ALCT-1ºC incluyen a LEN9 en su plaza de ALCT. Distintos tramos
  garantizados por S1. ✅
- *Aula A17 sin colisión (S2):* las cuatro plazas de ALCT comparten
  A17 mayoritariamente (excepto la que toque en 1ºD a A10). El solver
  las separa en tramos distintos. ✅

#### Actividad coordinada — Religión / Atención Educativa de 1ºA

Esta vista es PARCIAL a propósito: §6.1 valida 1ºA como grupo individual,
así que solo se enumeran sus subgrupos en la plaza de ATED. Pero la plaza de
Religión NO es per-grupo: en el horario real REL1 reúne a 1ºA y 1ºB juntos en
A5 el jueves 12:30 (verificado cruzando el horario por aulas, S19), por eso su
plaza ya lista `1ºB-Relig` además de `1ºA-Relig`. La Religión de 1ºESO se
organiza por PAREJAS (A+B en un tramo, C+D en otro), no por grupo; ATED es
per-grupo en el mismo tramo que la Religión de su pareja. El bloque por parejas
completo (las dos actividades A+B y C+D con sus cuatro ATED reales) se modela
en §6.4 y se validó en el solver en Fase 5 (Bloque 1, fixture
`problema-5-religion-parejas-1eso.json`). Ver Hallazgo K.

✅ Criterio cubierto (vista parcial de 1ºA; modelado multi-grupo en §6.4).

#### Resumen de la validación de 1ºESO A

| Concepto del system prompt | Caso real en 1ºA | Mecanismo del modelo |
|---|---|---|
| Sesión ordinaria | 21 sesiones (PLAS, ByG, Ing, Mat, EF, Mús, Geo, TUT) | Actividad 1 plaza, subgrupo "Completo" |
| Co-docencia intra-aula | LCL 4× (LEN2+LEN8 en A5) | Actividad 1 plaza con 2 profesores, sin partición |
| Agrupamiento inter-grupo transversal | RefMt | Una plaza de la actividad del bloque, subgrupos de 4 grupos |
| Bloque de optativas alternativas | CyR + OyD + RefMt; Fr2 + ALCT; Relig + ATED | Partición unificada del bloque temporal + actividad multi-plaza |
| Multi-grupo aula única | (a confirmar con 1ºB/C/D) | Misma plaza con subgrupos de varios grupos |
| ATED paralela | Relig + ATED jue 12:30 | Dos plazas en la misma actividad |
| Tutoría | Jue 13:30, TUT1 GH6 A5 | Plaza con profesor = tutor; invariante S8 |
| Recreo | Tramo 11:00-11:30 | `TramoSemanal.es_lectivo = false` |

Total de horas semanales en 1ºA: 21 (ordinarias: PLAS=1, ByG=3, Ing=4,
Mat=4, EF=3, Mús=2, Geo=3, TUT=1) + 4 (LCL co-docencia) + 2 (bloque
CyR/OyD/RefMt) + 2 (bloque Fr2/ALCT) + 1 (bloque Relig/ATED) =
**30 sesiones/semana**, coincide con el horario real.

### 6.2 Criterio: 3ºADi con sesiones propias y compartidas

**Configuración necesaria.**

```
Grupos:
  - 3ºA, 3ºB, 3ºC (tipo=ORDINARIO)
  - 3ºADi (tipo=DIVERSIFICACION_PDC, grupo_padre_id=3ºA)
  - 3ºBDi (tipo=DIVERSIFICACION_PDC, grupo_padre_id=3ºB)
  - 3ºCDi (tipo=DIVERSIFICACION_PDC, grupo_padre_id=3ºC)

Particiones:
  - "Completo-3ESO":      Subgrupo "3ºA-Completo" → {3ºA, 3ºADi}
                          Subgrupo "3ºB-Completo" → {3ºB, 3ºBDi}
                          ...
  - "Diversificacion-3ESO":
        Subgrupo "3ºA-NoDi" → {3ºA}        -- mitad ordinaria de 3ºA
        Subgrupo "3ºADi"    → {3ºA, 3ºADi} -- mitad Di (también vista como grupo virtual)
        Subgrupo "3ºB-NoDi" → {3ºB}
        Subgrupo "3ºBDi"    → {3ºB, 3ºBDi}
        Subgrupo "3ºC-NoDi" → {3ºC}
        Subgrupo "3ºCDi"    → {3ºC, 3ºCDi}
```

Nota sobre la composición de subgrupos: el subgrupo `3ºA-Completo` enlaza tanto a `3ºA` como a `3ºADi`
porque cuando la actividad es compartida, todos los alumnos del grupo asisten juntos.
Las sesiones compartidas reales de `3ºA/3ºADi`, verificadas cruzando el horario de `3ºA` ordinario (PDF pág. 8)
con el de `3ºA PDC` (pág. 9), son: TUT3 (tutoría), EF, EPVA, Tec y el bloque Rel/ATED. Cuando es específica de la mitad Di, se usa `3ºADi`; cuando es de la mitad ordinaria, `3ºA-NoDi`.

**Sesiones.**

- *ÁmbSL en A8 con los tres Di juntos* (sesión propia transversal):
  ```
  Actividad: asignatura=ÁmbSL
    Plaza: profesor=LEN2, aula=A8, subgrupos=(3ºADi, 3ºBDi, 3ºCDi)
  ```

- *Tec con 3ºA y 3ºADi juntos en B07* (compartida con grupo de origen):
  ```
  Actividad: asignatura=Tec
    Plaza: profesor=TEC3, aula=B07, subgrupos=(3ºA-Completo)
  ```
  (Como `3ºA-Completo` enlaza a 3ºA y 3ºADi, ambos quedan en uso.)

- *Tec específico de 3ºBDi en B07 mientras 3ºB hace Biología en B05*:
  ```
  Actividad: asignatura=Tec
    Plaza: profesor=TEC3, aula=B07, subgrupos=(3ºBDi)

  Actividad: asignatura=Biol
    Plaza: profesor=BYG2, aula=B05, subgrupos=(3ºB-NoDi)
  ```
  Ambas actividades coexisten en el mismo tramo sin colisión: los subgrupos
  son disjuntos (Di vs No-Di de 3ºB).

✅ Criterio cubierto.

Nota (Sesión 18) — Rel/ATED es un único bloque para 3ºA y 3ºADi. El cruce de PDFs muestra que el viernes 10-11 el bloque Rel/ATED de 3ºA (REL1 B01 + FIL1 B05 + ING3 A7) es idéntico al de 3ºADi: es la misma actividad, que toca a ambos grupos, no dos sesiones distintas. Se modela como una actividad multi-plaza cuyos subgrupos cubren ambos grupos (mecanismo de §6.4). La transversalidad ampliada por parejas (3ºA+3ºB+3ºADi+3ºBDi, §6.4) se validará con dataset real en Fase 5; el fixture de cierre de Fase 4 recortó Rel/ATED a 3ºA+3ºADi por no aportar nada que EF/Tec no validen ya sobre la ceguera de S9.

### 6.3 Criterio: TICO de 1ºBach (4 grupos comparten aula y profesor)

TICO es una optativa dentro del bloque de optativas de 1ºBach. Modelo:

```
Particion: "Optativas-1Bach-Bloque1"
  Subgrupo "1Bach-Opt-TICO"  → {1ºBachA, 1ºBachB, 1ºBachC, 1ºBachD}
  Subgrupo "1Bach-Opt-DTec"  → {1ºBachA, 1ºBachB, 1ºBachC, 1ºBachD}
  Subgrupo "1Bach-Opt-ANAP"  → {1ºBachA, 1ºBachB, 1ºBachC, 1ºBachD}
  Subgrupo "1Bach-Opt-TEstI" → {1ºBachA, 1ºBachB, 1ºBachC, 1ºBachD}
  Subgrupo "1Bach-Opt-DA"    → {1ºBachA, 1ºBachB, 1ºBachC, 1ºBachD}

Actividad: asignatura=NULL, duracion_tramos=2
  Plaza: TICO,  INF1,  A12In, subgrupos=(1Bach-Opt-TICO)
  Plaza: DTec,  DIB2,  TALL1, subgrupos=(1Bach-Opt-DTec)
  Plaza: ANAP,  BYG3,  A6,    subgrupos=(1Bach-Opt-ANAP)
  Plaza: TEstI, FOL2,  COM1,  subgrupos=(1Bach-Opt-TEstI)
  Plaza: DA,    DIB1,  C01,   subgrupos=(1Bach-Opt-DA)
```

La actividad ocupa dos tramos consecutivos por `duracion_tramos=2`.
La suma de subgrupos cubre completamente los cuatro grupos de 1ºBach: en
ese tramo ningún grupo de 1ºBach tiene actividad común.

✅ Criterio cubierto, y demuestra que el modelo soporta plazas con
asignaturas distintas dentro de la misma actividad.

### 6.4 Criterio: "3 sesiones simultáneas que bloquean 4 grupos"

Caso real: viernes 10-11, Religión + dos plazas de Atención Educativa para
{3ºA, 3ºB, 3ºADi, 3ºBDi}.

```
Particiones (una por grupo de origen; cada subgrupo pertenece a UN solo grupo):

"Relig-3ESO-A":  Subgrupo "3ºA-Rel"  → {3ºA}    Subgrupo "3ºA-ATED"  → {3ºA}

"Relig-3ESO-B":  Subgrupo "3ºB-Rel"  → {3ºB}    Subgrupo "3ºB-ATED"  → {3ºB}

"Relig-3ESO-ADi": Subgrupo "3ºADi-Rel" → {3ºADi}  Subgrupo "3ºADi-ATED" → {3ºADi}

"Relig-3ESO-BDi": Subgrupo "3ºBDi-Rel" → {3ºBDi}  Subgrupo "3ºBDi-ATED" → {3ºBDi}
Actividad: asignatura=NULL

Plaza: Rel,  REL1, B01, subgrupos=(3ºA-Rel, 3ºB-Rel, 3ºADi-Rel, 3ºBDi-Rel)

Plaza: ATED, FIL1, B05, subgrupos=(3ºA-ATED, 3ºB-ATED)

Plaza: ATED, ING3, A7,  subgrupos=(3ºADi-ATED, 3ºBDi-ATED)
```

Tres plazas → mismo tramo por construcción. Los subgrupos son MONO-GRUPO
(cada uno pertenece a un único grupo): es la "Lectura A" (una plaza lista
subgrupos de varios grupos). NO se usa un subgrupo cuya población sean alumnos
de varios grupos ("Lectura B", `SubgrupoGrupo` N:M): el `Subgrupo` del dominio
tiene un único `grupo` (1:1), y el modelo CP-SAT bloquea un grupo vía
`tocaGrupo`, que recorre los subgrupos de cada plaza y mira `sg.grupo()`. Por
eso, para que el bloque bloquee los cuatro grupos (S9), se necesita un subgrupo
por grupo en las plazas, no un subgrupo multi-grupo. El reparto concreto de
ATED entre las dos plazas (Letras/Inglés) es una de las particiones posibles
que cubren la población de los cuatro grupos.

✅ Criterio cubierto (Lectura A; validado en el solver en Fase 5, Bloque 1,
con el patrón análogo de 1ºESO en `problema-5-religion-parejas-1eso.json`).

### 6.5 Criterio adicional: horario completo de 1ºBACH B (Sesión 4)

Validación extendida con un grupo de Bachillerato. El horario real de
1ºBACH B contiene **30 sesiones semanales** distribuidas en:
6 troncales (LCL, Matem, Ing1, FQ, Filos, EdFís), 1 bloque Religión/PTVE
intra-grupo, 2 bloques de optativas transversales sobre 4 grupos
(OPT1, OPT2), 1 bloque de modalidad sobre 2 grupos (Bio/TecIn).

#### Particiones

```
Particion "Completo-1BachB":
  Subgrupo "1BachB-Completo"   → {1ºBach B}

Particion "Religion-1BachB" (intra-grupo, dos alternativas):
  Subgrupo "1BachB-Rel"        → {1ºBach B}     (alumnos de Religión)
  Subgrupo "1BachB-PTVE"       → {1ºBach B}     (alumnos de PTVE)

Particion "Optativas-1Bach-Bloque1":
  Subgrupo "1Bach-Opt-DTec"    → {1ºBach A, B, C, D}
  Subgrupo "1Bach-Opt-ANAP"    → {1ºBach A, B, C, D}
  Subgrupo "1Bach-Opt-TEstI"   → {1ºBach A, B, C, D}
  Subgrupo "1Bach-Opt-TICO"    → {1ºBach A, B, C, D}
  Subgrupo "1Bach-Opt-DA"      → {1ºBach A, B, C, D}

Particion "Optativas-1Bach-Bloque2":
  Subgrupo "1Bach-Opt-DTec"    → (MISMO subgrupo que en Bloque1; I6)
  Subgrupo "1Bach-Opt-TEst2"   → {1ºBach A, B, C, D}
  Subgrupo "1Bach-Opt-Lab"     → {1ºBach A, B, C, D}
  Subgrupo "1Bach-Opt-CE"      → {1ºBach A, B, C, D}
  Subgrupo "1Bach-Opt-Pat"     → {1ºBach A, B, C, D}

Particion "Itinerario-Ciencias-1Bach-AB":
  Subgrupo "1BachAB-Bio"       → {1ºBach A, 1ºBach B}
  Subgrupo "1BachAB-TecIn"     → {1ºBach A, 1ºBach B}
```

El subgrupo `1Bach-Opt-DTec` aparece en `SubgrupoParticion` enlazado a
las dos particiones de optativas. Su población se define UNA sola vez en
`SubgrupoGrupo`. Esta es la aplicación canónica del Hallazgo D y la
invariante I6.

#### Actividades

```
Actividad "LCL-1BachB"   (LCL, rep=4, DISTRIBUIDA):
  Plaza LCL, LEN7, B06, subgrupos=(1BachB-Completo)

Actividad "Matem-1BachB" (Matem, rep=4, DISTRIBUIDA):
  Plaza Matem, MAT7, B06, subgrupos=(1BachB-Completo)

Actividad "Ing1-1BachB"  (Ing1, rep=4, DISTRIBUIDA):
  Plaza Ing1, ING4, B06, subgrupos=(1BachB-Completo)

Actividad "FQ-1BachB"    (FQ, rep=4, DISTRIBUIDA):
  Plaza FQ, FIS4, A6, subgrupos=(1BachB-Completo)

Actividad "Filos-1BachB" (Filos, rep=3, DISTRIBUIDA):
  Plaza Filos, FIL1, B06, subgrupos=(1BachB-Completo)

Actividad "EdFís-1BachB" (EdFís, rep=2, DISTRIBUIDA):
  Plaza EdFís, EFI1, candidatas={Pista, Gim}, subgrupos=(1BachB-Completo)

Actividad "RelPTVE-1BachB" (asignatura=NULL, rep=1, NEUTRA, requiere_tutor=true):
  Plaza Relig, REL1, B06, subgrupos=(1BachB-Rel)
  Plaza PTVE,  FIL2, A2,  subgrupos=(1BachB-PTVE)

Actividad "OPT1-1Bach"   (rep=2, NEUTRA):
  Plaza DTec,  DIB2, TALL1, subgrupos=(1Bach-Opt-DTec)   [partición Bloque1]
  Plaza ANAP,  BYG3, A6,    subgrupos=(1Bach-Opt-ANAP)
  Plaza TEstI, FOL2, COM1,  subgrupos=(1Bach-Opt-TEstI)
  Plaza TICO,  INF1, A12In, subgrupos=(1Bach-Opt-TICO)
  Plaza DA,    DIB1, C01,   subgrupos=(1Bach-Opt-DA)

Actividad "OPT2-1Bach"   (rep=2, NEUTRA):
  Plaza DTec,  DIB2, TALL1, subgrupos=(1Bach-Opt-DTec)   [partición Bloque2; MISMO subgrupo]
  Plaza TEst2, FOL2, COM1,  subgrupos=(1Bach-Opt-TEst2)
  Plaza Lab,   BYG2, A6,    subgrupos=(1Bach-Opt-Lab)
  Plaza CE,    ECO,  COM4,  subgrupos=(1Bach-Opt-CE)
  Plaza Pat,   GH6,  B06,   subgrupos=(1Bach-Opt-Pat)

Actividad "MOD-Ciencias-1Bach-AB" (rep=4, DISTRIBUIDA):
  Plaza Bio,   BYG3, A6,  subgrupos=(1BachAB-Bio)
  Plaza TecIn, TEC2, B07, subgrupos=(1BachAB-TecIn)
```

**Recuento de repeticiones:** 4+4+4+4+3+2+1+2+2+4 = **30 sesiones/semana**,
coincide con el horario real.

#### Verificación de invariantes

- **I1:** En cada partición, los subgrupos cubren los grupos asociados.
  Las 5 plazas de OPT1/OPT2 cubren la población de 1ºBach A+B+C+D; las 2
  plazas de Bio/TecIn cubren 1ºBach A+B; las 2 plazas de Rel/PTVE cubren
  1ºBach B. ✅
- **I2:** Cada actividad usa subgrupos disjuntos dentro de sí misma. ✅
- **I6:** El subgrupo `1Bach-Opt-DTec` aparece en dos particiones con la
  misma población definida una sola vez en `SubgrupoGrupo`. ✅
- **S1, S2, S3:** Sin colisión en 1ºBach B aislado. BYG3 imparte ANAP
  (OPT1) y Bio (MOD-Ciencias) en tramos distintos; DIB2 imparte DTec en
  OPT1 y OPT2 en tramos distintos; A6 aloja FQ + ANAP + Bio + Lab en
  tramos distintos. La verificación contra el resto del centro queda
  para Fase 5.
- **S4:** Aulas especializadas coherentes (A6=ciencias, A12In=informática,
  TALL1=plástica, B07=tecnología, C01=plástica, Pista=EdFís). ✅
- **S5:** Las plazas de cada actividad comparten tramo por construcción. ✅
- **S6:** Recreo 11:00–11:30 no contiene actividades. ✅
- **S8:** La actividad `RelPTVE-1BachB` está marcada con
  `requiere_tutor=true`. La plaza PTVE tiene profesor FIL2, que está en
  `ProfesorTutoria(1ºBach B) = FIL2` con rol TUTOR_PRINCIPAL. ✅

#### Casos del modelo validados

| Caso | Mecanismo |
|---|---|
| Optativa compartida entre dos bloques (DTec en OPT1 y OPT2) | Subgrupo único en dos particiones (I6) |
| Tutoría implícita en PTVE en lugar de TUT | Flag `requiere_tutor` en actividad, no nombre de asignatura |
| Religión/PTVE per-grupo (no transversal) | Configuración del centro: una actividad por grupo, no compartida |
| Modalidad transversal sobre subconjunto de grupos del nivel | Misma estructura que las optativas, alcance = 2 grupos en lugar de 4 |
| Profesor en plazas de dos actividades distintas | Plazas independientes con mismo profesor; S1 garantiza tramos distintos |

✅ Criterio cubierto.

### 6.6 Criterio adicional: horario completo de 1ºFPB (Sesión 5)

Validación extendida con un grupo de Formación Profesional Básica.
1ºFPB tiene una estructura distinta a ESO y Bachillerato: bloques
prolongados de talleres técnicos (2 o más tramos consecutivos),
profesorado mayoritariamente compartido con el tutor, y una asignatura
"externa" (CS · GH1) en aula separada del taller. El horario real
contiene **30 sesiones semanales** con la siguiente estructura:

- 9 bloques de 2 tramos consecutivos (CA×2, IPE×1, CS×1, MECSO×2, AMO×3)
- 1 bloque de **3 tramos consecutivos** (PS, martes 8:00–11:00)
- 9 sesiones sueltas de 1 tramo (CS×4, PS×2, IPE×1, MECSO×1, Tut×1)

Total: 18 + 3 + 9 = 30 sesiones. ✅

#### Tutoría y tutor

El tutor del grupo es PAU2 (Millán Erencia, Antonio). Es también el
profesor de la asignatura Tut, así como de las asignaturas técnicas AMO,
MECSO y PS. `ProfesorTutoria(PAU2, 1ºFPB) = TUTOR_PRINCIPAL`. La
actividad TUT-1FPB se marca con `requiere_tutor = true`; la única plaza
tiene profesor PAU2, satisfaciendo S8.

#### Particiones y subgrupos

```
Particion "Completo-1FPB":
  Subgrupo "1FPB-Completo" → {1ºFPB}
```

No hay desdobles ni agrupamientos para 1ºFPB en los datos observados.
Una única partición trivial con un único subgrupo cubre el grupo.

#### Actividades

Cada asignatura se modela según su patrón temporal observado. Las
actividades de bloque y las de sesión suelta se separan porque
`Actividad.duracion_tramos` aplica a todas las instancias de la
actividad; mezclar tramos sueltos con bloques en la misma actividad
violaría S6.

```
# Bloques obligatorios de 2 tramos
Actividad "CA-1FPB"           (asig=CA,    rep=2, duracion_tramos=2, AGRUPADA):
  Plaza CA, TEC1, TALL_FPB, subgrupos=(1FPB-Completo)

Actividad "IPE-1FPB-blk"      (asig=IPE,   rep=1, duracion_tramos=2, NEUTRA):
  Plaza IPE, FOL3, TALL_FPB, subgrupos=(1FPB-Completo)

Actividad "CS-1FPB-blk"       (asig=CS,    rep=1, duracion_tramos=2, NEUTRA):
  Plaza CS, GH1, TALL3, subgrupos=(1FPB-Completo)

Actividad "MECSO-1FPB-blk"    (asig=MECSO, rep=2, duracion_tramos=2, AGRUPADA):
  Plaza MECSO, PAU2, TALL_FPB, subgrupos=(1FPB-Completo)

Actividad "AMO-1FPB"          (asig=AMO,   rep=3, duracion_tramos=2, AGRUPADA):
  Plaza AMO, PAU2, TALL_FPB, subgrupos=(1FPB-Completo)

# Bloque obligatorio de 3 tramos (Hallazgo G)
Actividad "PS-1FPB-blk3"      (asig=PS,    rep=1, duracion_tramos=3, NEUTRA):
  Plaza PS, PAU2, TALL_FPB, subgrupos=(1FPB-Completo)

# Sesiones sueltas
Actividad "PS-1FPB-suelta"    (asig=PS,    rep=2, duracion_tramos=1, NEUTRA):
  Plaza PS, PAU2, TALL_FPB, subgrupos=(1FPB-Completo)

Actividad "IPE-1FPB-suelta"   (asig=IPE,   rep=1, duracion_tramos=1, NEUTRA):
  Plaza IPE, FOL3, TALL_FPB, subgrupos=(1FPB-Completo)

Actividad "CS-1FPB-suelta"    (asig=CS,    rep=4, duracion_tramos=1, NEUTRA):
  Plaza CS, GH1, TALL3, subgrupos=(1FPB-Completo)

Actividad "MECSO-1FPB-suelta" (asig=MECSO, rep=1, duracion_tramos=1, NEUTRA):
  Plaza MECSO, PAU2, TALL_FPB, subgrupos=(1FPB-Completo)

Actividad "TUT-1FPB" (asig=Tut, rep=1, duracion_tramos=1, NEUTRA, requiere_tutor=true):
  Plaza Tut, PAU2, TALL_FPB, subgrupos=(1FPB-Completo)
```

**Recuento de horas semanales:**
2×2 + 1×2 + 1×2 + 2×2 + 3×2 + 1×3 + 2×1 + 1×1 + 4×1 + 1×1 + 1×1 =
4+2+2+4+6+3+2+1+4+1+1 = **30 sesiones/semana**. Coincide con el horario
real. ✅

#### Verificación de invariantes

- **I1:** El subgrupo `1FPB-Completo` cubre la totalidad del grupo, sin
  particiones adicionales. ✅
- **I2:** Cada actividad tiene una sola plaza, no hay riesgo de
  solapamiento intra-actividad. ✅
- **S4:** Todas las actividades se asignan a tramos lectivos. El recreo
  (11:00–11:30) tiene `es_lectivo = false` y queda excluido por
  construcción. ✅
- **S6 con `duracion_tramos = 2`:** los bloques inician en T1 (8:00) o
  T5 (12:30), ocupando T1–T2 o T5–T6. Ambas posiciones tienen cadena
  de `siguiente_inmediato_id` no interrumpida. ✅
- **S6 con `duracion_tramos = 3`:** el bloque PS de martes inicia en
  T1 y ocupa T1–T2–T3. T3 (10:00–11:00) tiene
  `siguiente_inmediato_id = NULL` porque el siguiente tramo cronológico
  es el recreo. Por tanto, T3 no puede ser el primer tramo de un bloque
  de duración ≥ 2 (ni el tramo intermedio de un bloque que continúa
  más allá), pero **sí puede ser el último tramo de un bloque que
  empezó en T1** (T1→T2→T3 es cadena válida). ✅
- **S6 — ningún bloque cruza el recreo:** los bloques de duración 2
  válidos inician en T1, T2, T4 o T5. Los de duración 3 inician solo en
  T1. Posiciones T3 y T6 como inicio de bloque ≥ 2 quedan prohibidas
  por construcción. Los datos reales respetan esta restricción. ✅
- **S8:** la actividad `TUT-1FPB` tiene `requiere_tutor = true`; la
  plaza está asignada a PAU2, que es tutor principal de 1ºFPB. ✅

#### Casos del modelo validados con FPB

| Caso | Mecanismo |
|---|---|
| Bloque obligatorio de 2 tramos consecutivos | `duracion_tramos = 2` + S6 |
| Bloque obligatorio de 3 tramos consecutivos | `duracion_tramos = 3` + S6 (Hallazgo G) |
| Misma asignatura repartida entre bloques y sesiones sueltas | Dos actividades distintas con la misma asignatura y profesor |
| Profesor que es a la vez tutor y titular de varias asignaturas técnicas | Plazas independientes con mismo profesor; S1 garantiza tramos distintos |
| Aula específica para una sola asignatura del grupo (CS en TALL3) | `Plaza.aula_fija_id` distinta en cada actividad según la asignatura |

✅ Criterio cubierto. El modelo soporta la estructura de FPB sin
modificaciones estructurales. Los hallazgos G, H, I se documentan en
la sección 2 y las deudas D8, D9 en la sección 8.

---

## 7. Decisiones tomadas y registradas

Las siguientes decisiones se registran como permanentes y no se reabren sin
justificación de peso (cambio de requisito, hallazgo nuevo, etc.).

| Decisión | Valor | Justificación |
|---|---|---|
| Unidad atómica del solver | Subgrupo, no alumno ni grupo | Los datos muestran que los subgrupos persistentes son la granularidad operativa real |
| Asignación profesor↔plaza | Configuración humana, no del solver | La asignación profesor-grupo-asignatura es decisión del equipo directivo, no algorítmica |
| Asignación aula↔plaza | Fija por defecto + candidatas opcionales | Reduce drásticamente el espacio de búsqueda; refleja la práctica real. Implementada en el solver en Sesión 16 (Fase 3, commit 2): cada plaza con `aulas_candidatas` aporta un intervalo opcional por candidata, con `addExactlyOne` sobre sus literales de presencia (el solver elige exactamente una); el no-solape de aula (S2) se impone sobre el aula efectivamente elegida. Las plazas con `aula_fija` no cambian |
| Multi-centro | Fuera de scope | Una base de datos = un centro. Cambio de curso = duplicación de BD |
| PDC | GrupoAdministrativo virtual con grupo padre | Permite imprimir horario de PDC como entidad propia y asignar tutor distinto |
| Restricciones horarias del profesorado | Tabla única con campo tipo (DURA/BLANDA) y peso numérico | Separa lo violable de lo no violable sin proliferar tablas |
| Preferencias positivas del profesorado | No modeladas en Fase 1 | Raras y de baja prioridad; añadibles después con peso negativo |
| Distancia entre aulas | Fórmula sobre (edificio, planta, sector) + tabla de excepciones | Equilibra precisión y coste de configuración |
| Jerarquía espacial de aulas | Columnas directas en Aula | Tablas separadas (Edificio, Planta) se considerarán en Fase 8 si la UI lo necesita |
| Penalización por cambio de aula | Aplicada a profesores y grupos con pesos distintos | El profesor pesa más que el grupo |
| Distribución temporal de asignaturas | Campo `patron_temporal` en `Actividad`: DISTRIBUIDA/AGRUPADA/NEUTRA | El solver consume la preferencia desde la actividad, que es la unidad que coloca en el tiempo |
| Rol de `DemandaCurricular` | Tabla informativa de validación, NO input del solver | El solver razona sobre actividades configuradas, no sobre demanda agregada; la demanda sirve para validar cobertura en la capa de configuración |
| Asignaturas alternativas dentro del grupo | Una sola partición unificada por bloque temporal | Mantener I1 cuando los alumnos del grupo se reparten entre varias asignaturas alternativas en el mismo tramo |
| Ventanas y horas consecutivas máximas | Parámetros globales en tabla Configuracion | No requieren modelado dedicado; son constantes del centro |
| Identidad de `Subgrupo` | Entidad de primera clase con identidad propia, independiente de la partición. Relación N:M con `Particion` vía `SubgrupoParticion` | Una misma población de alumnos puede aparecer en bloques de optativas distintos (Hallazgo D, validado contra 1ºBACH B) |
| Marca de "tutoría" | Flag `Actividad.requiere_tutor` (boolean) | La asignatura tutorial varía por centro y nivel (TUT en ESO, PTVE en Bach); la semántica reside en la actividad, no en el catálogo de asignaturas (Hallazgo E) |
| Modelo de Religión/PTVE | Configurable: transversal (una actividad multi-grupo) o per-grupo (una actividad por grupo) | Los datos muestran ambas formas en el mismo centro según el nivel (Hallazgo F). El modelo soporta ambas sin cambios |
| Plaza ↔ Profesor | M:N simétrica vía `PlazaProfesor` (sin rol titular/apoyo). Una plaza tiene 1..N profesores. Co-docencia intra-aula = `\|PlazaProfesor\| ≥ 2` | Validación contra 1ºESO A (Hallazgo J): LCL es co-docencia intra-aula con LEN2+LEN8 en A5. No es desdoble. Pluralizar Plaza↔Profesor evita una migración futura del schema |
| Campo `Actividad.tipo` | **Eliminado.** La naturaleza estructural (ordinaria, co-docencia, desdoble, agrupamiento, bloque de optativas, PDC transversal) es inferible del contenido (número de plazas, profesores por plaza, subgrupos cubiertos) | Validación contra 1ºESO A (Sesión actual): mantener una etiqueta redundante introduce riesgo de desincronización con el contenido real, y la UI puede calcular la etiqueta legible a partir del contenido en la capa de presentación |
| No-solape por grupo en el solver (S9) | Restricción dura propia, gemela del no-solape por subgrupo, ciega al `grupo_padre` | Al partir un grupo en subgrupos (Fase 3), S3 deja de garantizar I1; el JSON del solver no transporta particiones, así que la cobertura del grupo no es deducible y debe imponerse como restricción explícita sobre el grupo. Decidido y validado en Sesión 14 (Fase 3, commit 1) |

---

## 8. Deuda consciente para fases posteriores

Lista de simplificaciones aceptadas en Fase 1 que pueden requerir cambios
en fases futuras. Se documentan aquí para que su aparición no sea sorpresa.

**D1. Generación automática de subgrupos por plantilla.**
La automatización de creación de grupos y subgrupos para nuevos cursos se
delega a la **capa de UI en Fase 8** (asistentes, formularios con patrones,
duplicación de configuración). En Fase 8 puede ser necesario añadir un
campo `patron_generacion NULL` (JSON) a `Particion` para describir cómo se
auto-generan los subgrupos cuando cambia el alcance. No se añade ahora
porque el formato del JSON depende del diseño de la UI.

**D2. Gestión de cursos académicos.**
El concepto "curso 2024-25 vs 2025-26" se aborda en la Fase 10 revisada
(antes "Multi-centro") como duplicación de base de datos, no como entidad
del modelo. Si en algún momento se necesita versionado dentro de la misma
BD (auditoría, comparación de cursos), se añadiría una tabla
`CursoAcademico` con FK desde las entidades relevantes.

**D3. Capacidad de aulas no validada en Fase 1.**
El campo `Aula.capacidad` está presente pero el solver no impone
restricción dura sobre número de alumnos por aula. Esto se evaluará en
Fase 5 cuando se vea si los datos del centro lo justifican.

**D4. Coordinación entre niveles (Gimnasio, Pista).**
La restricción "solo hay un Gim y una Pista" queda implícita por la
restricción S2 (sin colisión de aula). No se modela explícitamente como
una restricción de capacidad de recurso compartido. Si se demuestra
insuficiente en Fase 5, se añadirá modelado dedicado.

**D5. Asignaturas con varias modalidades en el mismo grupo.** ✅ CERRADA en
la validación de Fase 1. El patrón aparece de forma estructural ya en
1ºESO (CyR/OyD/RefMt, Fr2/ALCT, Religión/ATED), no solo en Bachillerato.
Se modela con una **partición unificada por bloque temporal** y una
**actividad con N plazas**, sin tablas adicionales. La regla está
documentada en la sección 6.1 como principio aplicable. No requiere
mecanismo nuevo.

**D6. Sesiones que comparten tramo entre actividades distintas.**
El modelo actual solo permite simultaneidad **dentro** de una actividad
(plazas compartiendo tramo). Si aparece un caso donde dos actividades
distintas deben ir obligatoriamente en el mismo tramo (no es lo mismo
que poder ir en paralelo), se añadirá un mecanismo de "vínculo entre
actividades". No se ha encontrado este caso en los datos.

**D7. UX de subgrupos compartidos entre particiones.**
La invariante I6 garantiza que un `Subgrupo` referenciado en varias
particiones tiene la misma población. La validación se hace en la capa
de configuración. En Fase 6 (persistencia) y Fase 8 (UI) habrá que
definir la experiencia: si el usuario edita la población de un subgrupo
compartido, debe quedar claro que el cambio aplica a todas las
particiones donde aparece, y la UI debe ofrecer descubrir todas sus
apariciones antes de modificar. No afecta al modelo de datos: es deuda
de capa de aplicación.

**D8. Aulas implícitas e inconsistencias entre horarios de origen.**
Origen: Hallazgo H (Sesión 5), ampliada en la validación de 1ºESO A
contra el PDF (Sesión 8) y reverificada con los cuatro grupos de 1ºESO
en Sesión 13. Cuatro problemas operativos del importador que el
modelo de datos absorbe sin cambios estructurales:

1. **Omisión de aula en celdas del horario por grupo.** Los PDFs del
   centro de referencia omiten el aula en celdas cuyo profesor es titular
   del grupo, asumiendo el aula de referencia del grupo.

2. **Omisión de aulas físicas en el horario por aula.** El listado por
   aulas no contiene el taller físico donde se imparten las asignaturas
   técnicas de FPB (AMO, MECSO, PS, IPE, CA, Tut), aunque físicamente
   existen.

3. **Inconsistencias profesor↔plaza entre horario por grupo y horario
   por aula.** El importador debe (a) cruzar los dos listados por grupo
   y por aula, (b) detectar y reportar las inconsistencias al usuario,
   (c) ofrecer un mecanismo de conciliación manual antes de persistir.

   *Nota de Sesión 8:* se registró que la sesión de EF del miércoles
   9:00–10:00 aparecía como EFI2 en el horario del grupo 1ºA y como
   EFI3 en el horario del aula Gim.

   *Nota de Sesión 13:* la verificación sobre los PDFs actuales del
   proyecto NO reproduce esa inconsistencia concreta: las 12 sesiones
   de EF de 1ºESO (3 por grupo × 4 grupos) salen EFI2 en ambos
   listados, y las celdas EFI3 del horario de Gim corresponden a
   3ºB, 4ºC y 1ºBach. La política de cruce sigue siendo necesaria:
   este tipo de error es posible y no detectable sin información del
   centro.

4. **Omisión de co-profesores en horario por aula en celdas de
   co-docencia intra-aula.** Detectado en Sesión 13: las 16 celdas
   de LCL de 1ºESO (4 grupos × 4 repeticiones semanales) listan los
   dos profesores de la co-docencia en el horario por grupos
   (LEN2+LEN8 para 1ºA, LEN3+LEN9 para 1ºB, LEN6+LEN9 para 1ºC,
   LEN3+LEN6 para 1ºD) pero solo el primero en el horario por aulas
   (A5, A11, A3, A14 respectivamente). Es patrón de presentación del
   PDF por aulas, no dato erróneo. Implicación para el importador: la
   información completa de profesores por celda solo está en el
   horario por grupos; el horario por aulas sirve para confirmar
   ubicación, no para inventariar profesorado.

Adicionalmente, la tipificación de aulas en los datos de referencia puede
ser engañosa: TALL3 figura como "Taller FPB" en el system prompt original,
pero su uso real es de aula teórica para CS de FPB. La carga inicial de
datos debe revisar manualmente la tipificación y la lista de aulas.

El modelo de datos no se ve afectado (permite tantas aulas como sea
necesario con tipificación arbitraria, y admite cualquier asignación
profesor↔plaza). Toda la complejidad recae en el **importador de datos**
(Fase 6 o utilidad de importación previa): no puede asumir que un horario
externo contenga la totalidad de las aulas del centro, ni confiar en la
tipificación declarada, ni asumir consistencia perfecta entre los
listados por grupo y por aula. Deuda de capa de aplicación, no de modelo.

**D9. Pares de sesiones de la misma asignatura cruzando el recreo.**
Origen: Hallazgo I (Sesión 5). En 1ºFPB, la asignatura CS aparece en
pares 10:00–11:00 + 11:30–12:30 (lunes y miércoles) por preferencia
pedagógica del centro. El modelo trata estas como dos sesiones
independientes; el solver puede colocarlas en cualquier par de tramos
sin garantizar la simetría alrededor del recreo. Se difiere la decisión
a Fase 5 (instituto completo): si el solver no reproduce el patrón
naturalmente y el centro lo considera importante, se evaluará añadir
una restricción blanda nueva ("simetría alrededor del recreo") en el
catálogo de `ProfesorRestriccionHoraria` o equivalente. Si el centro lo
considera estructural, se introducirá un bloque elástico que tolere un
tramo no lectivo intermedio (cambio en S6). En Fase 1 no se introduce
ningún mecanismo.

**D10. UX de configuración de plazas multi-profesor.**
Origen: Hallazgo J (Sesión actual). La UI de Fase 8 debe ofrecer un
flujo claro y diferenciable para tres casos que el modelo unifica bajo
la estructura `Actividad → Plaza`:

- Sesión ordinaria: una plaza, un profesor, un aula, subgrupo
  "Completo" del grupo.
- Co-docencia intra-aula: una plaza, varios profesores, un aula,
  mismos subgrupos. Discriminador para el usuario: "varios profesores
  en la misma aula con el grupo completo".
- Desdoble o agrupamiento: varias plazas, profesores y aulas
  distintos, subgrupos disjuntos. Discriminador: "el grupo se parte
  o se mezcla con otros grupos".

No afecta al modelo de datos: el esquema y las invariantes cubren los
tres casos sin distinción. La distinción es puramente de presentación.

---

## 9. Glosario

**Actividad.** Unidad de planificación que el solver coloca en el tiempo.
Agrupa una o más plazas que comparten tramo obligatoriamente.

**ActividadInstancia.** Una de las N repeticiones semanales de una
actividad. Es la unidad concreta a la que el solver asigna un tramo.

**Agrupamiento (transversal).** Patrón en el que varios grupos del mismo
nivel se redistribuyen en subgrupos transversales. Estructuralmente
idéntico a un desdoble: una actividad con K plazas cuyos subgrupos cruzan
N grupos.

**Co-docencia intra-aula.** Patrón en el que dos o más profesores
trabajan simultáneamente con el grupo completo en una sola aula. En el
modelo se representa como una sola `Plaza` con `|PlazaProfesor| ≥ 2`,
con el subgrupo "Completo" del grupo. Discriminador frente al
desdoble: número de aulas listadas en la celda del horario (una sola
⇒ co-docencia; varias ⇒ desdoble).

**Desdoble.** Patrón en el que un grupo se parte en dos o más mitades que
trabajan simultáneamente con profesores y aulas distintos. En los datos
del centro de referencia, todos los desdobles cruzan más de un grupo, por
lo que no hay diferencia estructural con el agrupamiento.

**Grupo administrativo.** La etiqueta del horario: "1ºA", "3ºADi",
"1ºBach C". Puede ser ordinario, PDC virtual o virtual de optativa.

**Partición.** Definición de cómo se subdivide una población de alumnos
(de uno o varios grupos) en subgrupos disjuntos. Por ejemplo, la partición
"CyR-1ESO" define dos subgrupos por cada grupo (los que van con TEC3 y
los que van con INF1).

**Plaza.** Asignación concreta de profesor + aula + asignatura + subgrupos.
Es la unidad que ocupa físicamente un profesor y un aula en un tramo.

**Restricción dura.** Restricción que el solver no puede violar bajo
ninguna circunstancia. Reduce el espacio de búsqueda.

**Restricción blanda.** Preferencia con peso numérico que entra en la
función objetivo. El solver intentará minimizarla pero puede violarla.

**Sesión.** Salida del solver: la asignación de una ActividadInstancia a
un tramo concreto y un aula concreta dentro de un HorarioGenerado.

**Subgrupo.** Conjunto persistente de alumnos definido por una partición.
Es la unidad atómica del solver. Un alumno pertenece a varios subgrupos
(uno por cada partición que afecte a su grupo).

**Tramo semanal.** Una franja horaria concreta de una semana lectiva:
"lunes 8:00-9:00". El centro de referencia tiene 30 tramos lectivos + 5
recreos (uno por día).

---

## 10. Cambios respecto al system prompt original

| Aspecto original del prompt | Cambio en el modelo final |
|---|---|
| 8 "tipos de sesión" disjuntos | Sustituidos por la combinación K plazas × N grupos cubiertos. No existe un campo discriminador `Actividad.tipo`: la naturaleza estructural se infiere del contenido (número de plazas, profesores por plaza, subgrupos cubiertos). La UI calcula etiquetas legibles a partir del contenido, no como columna persistida (ver §4.6) |
| Tipo 2 (Desdoble) definido como intra-grupo | Sin distinción estructural con el agrupamiento. Los datos del centro no contienen ningún desdoble intra-grupo |
| Tipo 5 (PDC) como satélite individual de cada grupo | Los subgrupos PDC del mismo nivel se agrupan transversalmente en sus sesiones de currículo alternativo |
| Restricciones blandas del profesorado mencionadas sin estructura | Tabla dedicada `ProfesorRestriccionHoraria` con campo `tipo` y `peso` |
| Distancia entre aulas no mencionada | Modelada con jerarquía espacial en `Aula` + fórmula + tabla de excepciones |
| Multi-centro previsto | Descartado. Una BD = un centro. Cambio de curso = duplicación de BD |
| Demanda curricular como input del scheduling | `DemandaCurricular` pasa a ser informativa/de validación. El solver razona sobre actividades configuradas |
| Asignaturas alternativas dentro del grupo (Religión/Atención, optativas) | Modeladas como UNA partición unificada por bloque temporal + UNA actividad con N plazas, no como particiones separadas por asignatura |
| `Subgrupo` ligado a una sola partición vía FK directa | `Subgrupo` es entidad de primera clase; relación N:M con `Particion` vía `SubgrupoParticion` (Hallazgo D) |
| Tutoría identificada por el nombre de la asignatura "TUT" | Tutoría identificada por flag `Actividad.requiere_tutor`; el nombre de la asignatura es libre por centro (Hallazgo E) |
| Plaza ligada a un único profesor (FK `Plaza.profesor_id`) | Relación M:N vía tabla `PlazaProfesor`. Una plaza tiene 1..N profesores. Co-docencia intra-aula = `\|PlazaProfesor\| ≥ 2`. Nueva invariante I7 (Hallazgo J) |
