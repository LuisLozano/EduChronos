# Especificación del catálogo de entrada — centro de referencia

> **Qué es esto.** El guion para **teclear a mano**, por la interfaz de Educhronos, el
> catálogo de entrada completo del IES de referencia, derivado de los volcados fieles de
> `docs/horario-referencia/` (28 `grupo-*.json`, 43 `aula-*.json`).
>
> **Qué NO es.** No es un formato de importación. **No existe importador, ni runner, ni
> script de carga**, y no está previsto escribirlos: el catálogo se teclea. El fichero
> hermano `catalogo-derivado.json` es el mismo contenido en forma legible por máquina,
> para ir tachando durante la carga y, más adelante, como **oráculo de regresión** contra
> el que comparar lo que produzca el solver.
>
> **Autoridad.** Manda `modelo_datos_fase1.md` §5 (I1–I7, S1–S9) y §6.1. Sobre los datos,
> manda el volcado (jerarquía del `README.md` de esta carpeta). Todo lo que este documento
> supone y no mide está en la §3, *Ambigüedades y decisiones pendientes*.

---

## 1. Censo en orden de tecleo

El orden no es negociable: cada paso resuelve por clave natural las entidades del
anterior (un grupo necesita su nivel, un subgrupo sus grupos, una actividad sus
asignaturas, profesores, aulas y subgrupos).

| # | Paso | Elementos | Envíos de formulario | Nota |
|---|---|---|---|---|
| 1 | **Jornada** | 7 tramos del día tipo | **1** | Un único formulario de reemplazo; el backend expande el día tipo a los 5 días |
| 2 | **Niveles** | 8 | **8** | |
| 3 | **Asignaturas** | 100 | **100** | `nombreCompleto` es obligatorio y no está en los volcados (→ A8) |
| 4 | **Profesores** | 59 | **59** | ídem |
| 5 | **Aulas** | 43 | **43** | 10 sin ninguna clase en el volcado, pero son espacios reales del centro |
| 6 | **Grupos** | 28 (23 ordinarios + 5 PDC) | **23** | Solo los ordinarios; los PDC van en el paso 7 |
| 7 | **PDC** | 5 | **5** | Sub-recurso del padre. Cada alta crea *además* su subgrupo `{código}-Completo` |
| 8 | **Tutores** | 28 | **28** | Un envío por grupo (la lista de tutorías del grupo se reemplaza entera) |
| 9 | **Subgrupos** | 334 | **329** | 334 − 5 que el paso 7 ya creó |
| 10 | **Actividades** | 219 (316 plazas) | **219** | Las plazas se teclean dentro del formulario de su actividad |

<br>

> # ⚠️ TOTAL: **815 envíos de formulario**
>
> De los cuales **548 (67 %)** son los pasos 9 y 10 — subgrupos y actividades. Los ocho
> primeros pasos suman 267 envíos, casi todos de un solo campo.

El catálogo describe **632 sesiones semanales** repartidas en 219 actividades, y cubre
exactamente los **840 slots** del horario real (28 grupos × 30 tramos) sin huecos ni
solapes.

---

## 2. Detalle por paso

### Paso 1 — Jornada (1 envío)

5 días (L–V) × 7 tramos del día tipo. El recreo **no está en los volcados** y hay que
insertarlo a mano (R10); los tramos 4–6 del volcado se desplazan una posición.

| Orden | Inicio | Fin | Tipo | Corresponde a |
|---|---|---|---|---|
| 1 | 08:00 | 09:00 | lectivo | T1 |
| 2 | 09:00 | 10:00 | lectivo | T2 |
| 3 | 10:00 | 11:00 | lectivo | T3 |
| 4 | 11:00 | 11:30 | **RECREO** | — (no está en el volcado) |
| 5 | 11:30 | 12:30 | lectivo | T4 |
| 6 | 12:30 | 13:30 | lectivo | T5 |
| 7 | 13:30 | 14:30 | lectivo | T6 |

### Paso 2 — Niveles (8 envíos)

| Código | Orden |
|---|---|
| `1ESO` | 1 |
| `2ESO` | 2 |
| `3ESO` | 3 |
| `4ESO` | 4 |
| `1BACH` | 5 |
| `2BACH` | 6 |
| `1FPB` | 7 |
| `2FPB` | 8 |

> Los códigos `1FPB` y `2FPB` se repiten como código de **nivel** y como código de
> **grupo**. Son entidades distintas y la aplicación no los confunde, pero conviene
> saberlo al teclear.

### Paso 3 — Asignaturas (100 envíos)

Los 100 códigos están en `catalogo-derivado.json` → `asignaturas`, con el número de
celdas de cada uno en el volcado. Tres advertencias de tecleo:

- **No normalices acentos ni mayúsculas** (R7). `EFis` y `EFís` son **códigos distintos
  del mismo nivel** (4º ESO, 8 y 4 celdas): `EFis` es la EF de 4ºA + 4ºADi y `EFís` la de
  4ºD + 4ºDDi. Lo mismo con `ÁmbCM`/`AmbCT` y `ÁmbSL`/`AmbSL`, donde la tilde separa 3º de
  4º de ESO.
- **`ECO` es a la vez código de asignatura y código de profesor** (R7). Son espacios de
  nombres separados; hay una celda (`aula-A10.json`, J·T3) donde vale las dos cosas a la
  vez. Al teclear la asignatura `ECO` no estás tecleando al profesor `ECO`, y viceversa.
- `nombreCompleto` es **obligatorio** y no está en los volcados (→ A8).

### Paso 4 — Profesores (59 envíos)

Los 59 códigos están en el JSON → `profesores`. Mismas dos advertencias: `ECO` colisiona
con la asignatura homónima, y `nombreCompleto` es obligatorio y no derivable.

### Paso 5 — Aulas (43 envíos)

El `tipo` se ha derivado del nombre del espacio (R11 prohíbe derivar compatibilidades,
no tipos). `capacidad`, `edificio`, `planta` y `sector` son nullable y **no** derivables
(→ A9).

| Código | Tipo propuesto | ¿La usa alguna actividad? |
|---|---|---|
| `A1` | ORDINARIA | sí |
| `A10` | ORDINARIA | sí |
| `A11` | ORDINARIA | sí |
| `A12 Informática` | INFORMATICA | sí |
| `A13` | ORDINARIA | sí |
| `A14` | ORDINARIA | sí |
| `A15` | ORDINARIA | sí |
| `A16` | ORDINARIA | sí |
| `A17` | ORDINARIA | sí |
| `A18` | ORDINARIA | sí |
| `A19 TUTOR` | ORDINARIA | **no** |
| `A2` | ORDINARIA | sí |
| `A3` | ORDINARIA | sí |
| `A4` | ORDINARIA | sí |
| `A5` | ORDINARIA | sí |
| `A6 Laboratorio` | LAB_CIENCIAS | sí |
| `A7` | ORDINARIA | sí |
| `A8` | ORDINARIA | sí |
| `A9` | ORDINARIA | sí |
| `B01` | ORDINARIA | sí |
| `B02` | ORDINARIA | sí |
| `B03` | ORDINARIA | sí |
| `B04` | ORDINARIA | sí |
| `B05` | ORDINARIA | sí |
| `B06` | ORDINARIA | sí |
| `B07` | ORDINARIA | sí |
| `B08` | ORDINARIA | **no** |
| `B09` | ORDINARIA | **no** |
| `B10` | ORDINARIA | **no** |
| `B11 Taller Tecnología` | TALLER_TEC | **no** |
| `C00` | ORDINARIA | sí |
| `C01 Aula Plástica` | TALLER_PLASTICA | sí |
| `C02` | ORDINARIA | **no** |
| `C03` | ORDINARIA | **no** |
| `C04` | ORDINARIA | **no** |
| `COM1` | COMUN | sí |
| `COM4` | COMUN | sí |
| `Gimnasio` | GIMNASIO | sí |
| `Pista` | PISTA | sí |
| `Taller 1 Aula Plástica` | TALLER_PLASTICA | sí |
| `Taller 2` | TALLER_FPB | **no** |
| `Taller 3` | ORDINARIA | sí |
| `Taller 4` | TALLER_FPB | **no** |

> Los seis códigos abreviados del volcado por grupos ya están traducidos con la tabla de
> alias de R6: `A12In`→`A12 Informática`, `A6`→`A6 Laboratorio`, `C01`→`C01 Aula Plástica`,
> `Gim`→`Gimnasio`, `TALL1`→`Taller 1 Aula Plástica`, `TALL3`→`Taller 3`. En este
> documento y en el JSON aparece siempre el nombre largo.

### Paso 6 y 7 — Grupos (23 envíos) y PDC (5 envíos)

| Código | Nivel | Tipo | Grupo padre |
|---|---|---|---|
| `1ºA` | 1ESO | ORDINARIO | — |
| `1ºB` | 1ESO | ORDINARIO | — |
| `1ºC` | 1ESO | ORDINARIO | — |
| `1ºD` | 1ESO | ORDINARIO | — |
| `2ºA` | 2ESO | ORDINARIO | — |
| `2ºB` | 2ESO | ORDINARIO | — |
| `2ºC` | 2ESO | ORDINARIO | — |
| `3ºA` | 3ESO | ORDINARIO | — |
| `3ºADi` | 3ESO | DIVERSIFICACION_PDC | 3ºA |
| `3ºB` | 3ESO | ORDINARIO | — |
| `3ºBDi` | 3ESO | DIVERSIFICACION_PDC | 3ºB |
| `3ºC` | 3ESO | ORDINARIO | — |
| `3ºCDi` | 3ESO | DIVERSIFICACION_PDC | 3ºC |
| `4ºA` | 4ESO | ORDINARIO | — |
| `4ºADi` | 4ESO | DIVERSIFICACION_PDC | 4ºA |
| `4ºB` | 4ESO | ORDINARIO | — |
| `4ºC` | 4ESO | ORDINARIO | — |
| `4ºD` | 4ESO | ORDINARIO | — |
| `4ºDDi` | 4ESO | DIVERSIFICACION_PDC | 4ºD |
| `1B-A` | 1BACH | ORDINARIO | — |
| `1B-B` | 1BACH | ORDINARIO | — |
| `1B-C` | 1BACH | ORDINARIO | — |
| `1B-D` | 1BACH | ORDINARIO | — |
| `2B-A` | 2BACH | ORDINARIO | — |
| `2B-B` | 2BACH | ORDINARIO | — |
| `2B-C` | 2BACH | ORDINARIO | — |
| `1FPB` | 1FPB | ORDINARIO | — |
| `2FPB` | 2FPB | ORDINARIO | — |

Los cinco PDC se dan de alta **por el sub-recurso de su padre**, no por el CRUD de grupos
(que rechaza cualquier tipo distinto de `ORDINARIO`). El alta de cada PDC crea además,
automáticamente, su subgrupo `{código}-Completo` — por eso el paso 9 teclea 329 y no 334.

**Sesiones que cada PDC comparte con su padre** (R9: una sola plaza que lista subgrupos de
los dos grupos; S9 es ciega al `grupo_padre` y trata esa plaza como una única sesión que
toca a ambos):

| PDC | Padre | Sesiones/semana compartidas | Asignaturas |
|---|---|---|---|
| `3ºADi` | `3ºA` | 10 | EF (2), EPVA (2), Tec (2), TUT3 (1), ATED (1), Rel (1) — las dos últimas dentro de `Bloque-ATED_Rel-3ºA+3ºADi+3ºB+3ºBDi` |
| `3ºBDi` | `3ºB` | 10 | idénticas a 3ºADi |
| `3ºCDi` | `3ºC` | 9 | EF (2), EPVA (2), Tec (2), TUT3 (1), ATED (1), Rel (1), en `Bloque-ATED_Rel-3ºC+3ºCDi` |
| `4ºADi` | `4ºA` | 8 | EFis (2), TUT4 (1), ATEDU (1), Rel (1) |
| `4ºDDi` | `4ºD` | 8 | EFís (2), TUT4 (1), ATEDU (1), Rel (1) |

Coincide con el Hallazgo A y §6.2, que citan EPVA y EF como las materias que devuelven al
Di a su grupo de origen. El **tronco** de los tres Di de 3º (ÁmbCM 8 h con MAT4, ÁmbSL 7 h
con LEN2, en A8) sale como dos actividades con **una sola plaza que lista los tres
subgrupos Di**, sin ningún grupo ordinario dentro — exactamente la corrección de S23 al
Hallazgo A.

### Paso 8 — Tutores (28 envíos)

Derivados del profesor que imparte `TUT1`/`TUT2`/`TUT3`/`TUT4`/`Tut` en ESO y FPB, y
`PTVE`/`PTEV` en Bachillerato (Hallazgo E: en Bach la tutoría no se llama TUT). Los 28
grupos tienen un candidato único, pero cinco profesores salen como tutor de más de un
grupo (→ A5).

| Grupo | Tutor principal | Aviso |
|---|---|---|
| `1ºA` | `GH6` | ⚠️ también de 1B-A |
| `1ºB` | `MAT8` | — |
| `1ºC` | `LEN9` | — |
| `1ºD` | `EFI2` | — |
| `2ºA` | `MAT5` | — |
| `2ºB` | `MAT1` | — |
| `2ºC` | `GH2` | — |
| `3ºA` | `MAT6` | ⚠️ también de 3ºADi |
| `3ºADi` | `MAT6` | ⚠️ también de 3ºA |
| `3ºB` | `BYG2` | ⚠️ también de 3ºBDi |
| `3ºBDi` | `BYG2` | ⚠️ también de 3ºB |
| `3ºC` | `BYG3` | ⚠️ también de 3ºCDi |
| `3ºCDi` | `BYG3` | ⚠️ también de 3ºC |
| `4ºA` | `ING6` | ⚠️ también de 4ºADi |
| `4ºADi` | `ING6` | ⚠️ también de 4ºA |
| `4ºB` | `GH4` | — |
| `4ºC` | `LEN6` | — |
| `4ºD` | `EFI3` | ⚠️ también de 1B-C, 4ºDDi |
| `4ºDDi` | `EFI3` | ⚠️ también de 1B-C, 4ºD |
| `1B-A` | `GH6` | ⚠️ también de 1ºA |
| `1B-B` | `FIL2` | ⚠️ también de 1B-D, 2B-A, 2B-B, 2B-C |
| `1B-C` | `EFI3` | ⚠️ también de 4ºD, 4ºDDi |
| `1B-D` | `FIL2` | ⚠️ también de 1B-B, 2B-A, 2B-B, 2B-C |
| `2B-A` | `FIL2` | ⚠️ también de 1B-B, 1B-D, 2B-B, 2B-C |
| `2B-B` | `FIL2` | ⚠️ también de 1B-B, 1B-D, 2B-A, 2B-C |
| `2B-C` | `FIL2` | ⚠️ también de 1B-B, 1B-D, 2B-A, 2B-B |
| `1FPB` | `PAU2` | — |
| `2FPB` | `PAU1` | — |

### Paso 9 — Subgrupos (329 envíos)

**334 subgrupos** en total: **28** `{grupo}-Completo` (uno por grupo, para las sesiones de
grupo entero) y **306 parciales** (uno por cada par grupo × vía de bloque). De los 28
completos, 5 los crea solo el alta del PDC.

Regla de nombres (R5, patrón de §6.1): `{grupo}-{asignatura}` cuando ese par tiene una
sola combinación de profesores en todo el centro; `{grupo}-{asignatura}-{profesor}` cuando
hay varias. Cada subgrupo se identifica por `(grupo, asignatura, conjunto de profesores)` y
**se reutiliza** en todos los bloques donde reaparece esa misma población (I6).

Población: todos los subgrupos derivados son **mono-grupo**. La transversalidad se expresa
poniendo varios subgrupos en la misma plaza, no fundiendo grupos dentro de un subgrupo —
la corrección de S23 al Hallazgo A. Un subgrupo multi-grupo (la «Lectura B» de Bach) no
aparece en estos datos.

Muestra de 15 (los 334 están en el JSON → `subgrupos`):

| Código | Población (grupos) |
|---|---|
| `1B-A-ANAP` | 1B-A |
| `1B-A-Bio` | 1B-A |
| `1B-A-CE` | 1B-A |
| `1B-A-Completo` | 1B-A |
| `1B-A-DA` | 1B-A |
| `1B-A-DTec` | 1B-A |
| `1ºD-ATED` | 1ºD |
| `1ºD-Completo` | 1ºD |
| `1ºD-CyR-INF1` | 1ºD |
| `1ºD-CyR-TEC3` | 1ºD |
| `3ºADi-ATED-ING3` | 3ºADi |
| `3ºADi-Completo` | 3ºADi |
| `3ºADi-Rel` | 3ºADi |
| `3ºB-ATED-FIL1` | 3ºB |
| `3ºB-ATED-ING3` | 3ºB |

Subgrupos por grupo, de más a menos: 4ºA y 4ºB con 24, 4ºD 21, 1B-D 19, 1B-C y 4ºC 18,
2B-B 16, 1B-A/1B-B/2B-C 14, 3ºA/3ºB 13, 2B-A/3ºC 12, los cuatro 1º ESO y los tres 2º ESO
11 cada uno, 4ºADi/4ºDDi 6, 3ºADi/3ºBDi 4, 3ºCDi 3, y **1FPB y 2FPB con 1 cada uno** (los
dos únicos grupos sin ningún bloque de optatividad: sus 30 slots son todos de grupo
completo).

### Paso 10 — Actividades (219 envíos, 316 plazas)

Estructura derivada según R1–R4:

| Forma | Actividades |
|---|---|
| 1 plaza (sesión ordinaria, co-docencia o clase multi-grupo) | 180 |
| 2 plazas | 14 |
| 3 plazas | 10 |
| 4 plazas | 2 |
| 5 plazas | 8 |
| 6 plazas | 5 |

- **38** actividades con `asignatura = NULL` (plazas de asignaturas distintas, §6.1).
- **59** actividades multi-grupo; **4** plazas con dos profesores (co-docencia intra-aula,
  I7 con `|profesores| = 2`: las LCL de 1º ESO A/B/C/D).
- Aulas: **268** plazas con aula fija, **37** con candidatas, **11 sin aula** (→ A7, y
  **caso inexpresable nº 1** de la §4).
- `patronTemporal`: 160 DISTRIBUIDA, 59 NEUTRA. `duracionTramos` = 1 en las 219 (→ A10).
- `requiereTutor` = true en 22 actividades; las 28 tutorías de grupo quedan cubiertas y S8
  se verifica en las 22.
- Repeticiones por semana: 45 actividades con 1, 44 con 2, 52 con 3, 67 con 4, 3 con 5,
  2 con 6, 3 con 7, 2 con 8 y 1 con 11 (`MEC-2FPB`).

Muestra de 15 actividades, elegidas para cubrir cada forma estructural (las 219 están en
el JSON → `actividades`):

**`ByG-1ºA`**  ·  asignatura = `ByG` · repeticiones = 3 · DISTRIBUIDA · requiereTutor = false · 1 plaza(s) · toca `1ºA`

| Plaza | Asignatura | Profesores | Aula | Subgrupos |
|---|---|---|---|---|
| 1 | `ByG` | `BYG1` | `A5` (fija) | `1ºA-Completo` |

**`LCL-1ºA`**  ·  asignatura = `LCL` · repeticiones = 4 · DISTRIBUIDA · requiereTutor = false · 1 plaza(s) · toca `1ºA`

| Plaza | Asignatura | Profesores | Aula | Subgrupos |
|---|---|---|---|---|
| 1 | `LCL` | `LEN2` + `LEN8` | `A5` (fija) | `1ºA-Completo` |

**`TUT1-1ºA`**  ·  asignatura = `TUT1` · repeticiones = 1 · NEUTRA · requiereTutor = true · 1 plaza(s) · toca `1ºA`

| Plaza | Asignatura | Profesores | Aula | Subgrupos |
|---|---|---|---|---|
| 1 | `TUT1` | `GH6` | `A5` (fija) | `1ºA-Completo` |

**`Bloque-ALCT_Fr2-1ºA`**  ·  asignatura = **NULL** · repeticiones = 2 · DISTRIBUIDA · requiereTutor = false · 2 plaza(s) · toca `1ºA`

| Plaza | Asignatura | Profesores | Aula | Subgrupos |
|---|---|---|---|---|
| 1 | `ALCT` | `LEN2` | `A17` (fija) | `1ºA-ALCT` |
| 2 | `Fr2` | `FRA1` | `A5` (fija) | `1ºA-Fr2` |

**`Bloque-ATED_Relig-1ºA+1ºB`**  ·  asignatura = **NULL** · repeticiones = 1 · NEUTRA · requiereTutor = false · 3 plaza(s) · toca `1ºA`, `1ºB`

| Plaza | Asignatura | Profesores | Aula | Subgrupos |
|---|---|---|---|---|
| 1 | `ATED` | `GH5` | `A11` (fija) | `1ºB-ATED` |
| 2 | `ATED` | `ING6` | `A17` (fija) | `1ºA-ATED` |
| 3 | `Relig` | `REL1` | `A5` (fija) | `1ºA-Relig`, `1ºB-Relig` |

**`Bloque-CyR_OyD_RefMt-1ESO`**  ·  asignatura = **NULL** · repeticiones = 2 · DISTRIBUIDA · requiereTutor = false · 6 plaza(s) · toca `1ºA`, `1ºB`, `1ºC`, `1ºD`

| Plaza | Asignatura | Profesores | Aula | Subgrupos |
|---|---|---|---|---|
| 1 | `CyR` | `INF1` | `A12 Informática` (fija) | `1ºA-CyR-INF1`, `1ºB-CyR-INF1`, `1ºC-CyR-INF1`, `1ºD-CyR-INF1` |
| 2 | `CyR` | `TEC3` | candidatas `A5`, `B07` | `1ºA-CyR-TEC3`, `1ºB-CyR-TEC3`, `1ºC-CyR-TEC3`, `1ºD-CyR-TEC3` |
| 3 | `OyD` | `FIL3` | candidatas `A11`, `A5` | `1ºA-OyD`, `1ºB-OyD`, `1ºC-OyD`, `1ºD-OyD` |
| 4 | `RefMt` | `MAT4` | candidatas `A10`, `A14` | `1ºA-RefMt-MAT4`, `1ºB-RefMt-MAT4`, `1ºC-RefMt-MAT4`, `1ºD-RefMt-MAT4` |
| 5 | `RefMt` | `MAT6` | candidatas `A11`, `A3` | `1ºA-RefMt-MAT6`, `1ºB-RefMt-MAT6`, `1ºC-RefMt-MAT6`, `1ºD-RefMt-MAT6` |
| 6 | `RefMt` | `MAT7` | candidatas `A14`, `A3` | `1ºA-RefMt-MAT7`, `1ºB-RefMt-MAT7`, `1ºC-RefMt-MAT7`, `1ºD-RefMt-MAT7` |

**`Bloque-Mate2-2B-A`**  ·  asignatura = `Mate2` · repeticiones = 4 · DISTRIBUIDA · requiereTutor = false · 2 plaza(s) · toca `2B-A`

| Plaza | Asignatura | Profesores | Aula | Subgrupos |
|---|---|---|---|---|
| 1 | `Mate2` | `MAT2` | `A16` (fija) | `2B-A-Mate2-MAT2` |
| 2 | `Mate2` | `MAT3` | candidatas `A17`, `A5`, `A7` | `2B-A-Mate2-MAT3` |

**`Bloque-ECO_GRI-1B-C+1B-D`**  ·  asignatura = **NULL** · repeticiones = 4 · DISTRIBUIDA · requiereTutor = false · 3 plaza(s) · toca `1B-C`, `1B-D`

| Plaza | Asignatura | Profesores | Aula | Subgrupos |
|---|---|---|---|---|
| 1 | `ECO` | `ECO` | candidatas `COM1`, `COM4` | `1B-C-ECO-ECO`, `1B-D-ECO-ECO` |
| 2 | `ECO` | `FOL3` | `B02` (fija) | `1B-C-ECO-FOL3`, `1B-D-ECO-FOL3` |
| 3 | `GRI` | `CLA1` | candidatas `A17`, `COM4` | `1B-D-GRI` |

**`Bloque-ANAP_DA_DTec_TEstI_TICO-1BACH`**  ·  asignatura = **NULL** · repeticiones = 2 · DISTRIBUIDA · requiereTutor = false · 5 plaza(s) · toca `1B-A`, `1B-B`, `1B-C`, `1B-D`

| Plaza | Asignatura | Profesores | Aula | Subgrupos |
|---|---|---|---|---|
| 1 | `ANAP` | `BYG3` | `A6 Laboratorio` (fija) | `1B-A-ANAP`, `1B-B-ANAP`, `1B-C-ANAP`, `1B-D-ANAP` |
| 2 | `DA` | `DIB1` | `C01 Aula Plástica` (fija) | `1B-A-DA`, `1B-B-DA`, `1B-C-DA`, `1B-D-DA` |
| 3 | `DTec` | `DIB2` | `Taller 1 Aula Plástica` (fija) | `1B-A-DTec`, `1B-B-DTec`, `1B-C-DTec`, `1B-D-DTec` |
| 4 | `TEstI` | `FOL2` | `COM1` (fija) | `1B-A-TEstI`, `1B-B-TEstI`, `1B-C-TEstI`, `1B-D-TEstI` |
| 5 | `TICO` | `INF1` | `A12 Informática` (fija) | `1B-A-TICO`, `1B-B-TICO`, `1B-C-TICO`, `1B-D-TICO` |

**`Bloque-DIG_EXPRE_FOPP_TEC-4ºA+4ºB+4ºC+4ºD`**  ·  asignatura = **NULL** · repeticiones = 3 · DISTRIBUIDA · requiereTutor = false · 5 plaza(s) · toca `4ºA`, `4ºB`, `4ºC`, `4ºD`

| Plaza | Asignatura | Profesores | Aula | Subgrupos |
|---|---|---|---|---|
| 1 | `DIG` | `INF1` | `A12 Informática` (fija) | `4ºA-DIG`, `4ºB-DIG`, `4ºC-DIG`, `4ºD-DIG` |
| 2 | `EXPRE` | `DIB1` | `Taller 1 Aula Plástica` (fija) | `4ºA-EXPRE`, `4ºB-EXPRE` |
| 3 | `EXPRE` | `DIB2` | `C01 Aula Plástica` (fija) | `4ºC-EXPRE`, `4ºD-EXPRE` |
| 4 | `FOPP` | `ECO` | candidatas `A15`, `A2` | `4ºA-FOPP`, `4ºB-FOPP`, `4ºC-FOPP`, `4ºD-FOPP` |
| 5 | `TEC` | `TEC2` | candidatas `A15`, `B07` | `4ºA-TEC`, `4ºB-TEC`, `4ºC-TEC`, `4ºD-TEC` |

**`Bloque-ATEDU_Rel-4ESO`**  ·  asignatura = **NULL** · repeticiones = 1 · NEUTRA · requiereTutor = false · 5 plaza(s) · toca `4ºA`, `4ºADi`, `4ºB`, `4ºC`, `4ºD`, `4ºDDi`

| Plaza | Asignatura | Profesores | Aula | Subgrupos |
|---|---|---|---|---|
| 1 | `ATEDU` | `EFI1` | `A2` (fija) | `4ºA-ATEDU-EFI1`, `4ºADi-ATEDU-EFI1`, `4ºB-ATEDU-EFI1`, `4ºC-ATEDU-EFI1`, `4ºD-ATEDU-EFI1`, `4ºDDi-ATEDU-EFI1` |
| 2 | `ATEDU` | `EFI3` | `A10` (fija) | `4ºA-ATEDU-EFI3`, `4ºADi-ATEDU-EFI3`, `4ºB-ATEDU-EFI3`, `4ºC-ATEDU-EFI3`, `4ºD-ATEDU-EFI3`, `4ºDDi-ATEDU-EFI3` |
| 3 | `ATEDU` | `GH4` | `A14` (fija) | `4ºA-ATEDU-GH4`, `4ºADi-ATEDU-GH4`, `4ºB-ATEDU-GH4`, `4ºC-ATEDU-GH4`, `4ºD-ATEDU-GH4`, `4ºDDi-ATEDU-GH4` |
| 4 | `ATEDU` | `ING3` | `B04` (fija) | `4ºA-ATEDU-ING3`, `4ºADi-ATEDU-ING3`, `4ºB-ATEDU-ING3`, `4ºC-ATEDU-ING3`, `4ºD-ATEDU-ING3`, `4ºDDi-ATEDU-ING3` |
| 5 | `Rel` | `REL1` | `A15` (fija) | `4ºA-Rel`, `4ºADi-Rel`, `4ºB-Rel`, `4ºC-Rel`, `4ºD-Rel`, `4ºDDi-Rel` |

**`ÁmbCM-3ºADi+3ºBDi+3ºCDi`**  ·  asignatura = `ÁmbCM` · repeticiones = 8 · NEUTRA · requiereTutor = false · 1 plaza(s) · toca `3ºADi`, `3ºBDi`, `3ºCDi`

| Plaza | Asignatura | Profesores | Aula | Subgrupos |
|---|---|---|---|---|
| 1 | `ÁmbCM` | `MAT4` | `A8` (fija) | `3ºADi-Completo`, `3ºBDi-Completo`, `3ºCDi-Completo` |

**`EF-3ºA+3ºADi`**  ·  asignatura = `EF` · repeticiones = 2 · DISTRIBUIDA · requiereTutor = false · 1 plaza(s) · toca `3ºA`, `3ºADi`

| Plaza | Asignatura | Profesores | Aula | Subgrupos |
|---|---|---|---|---|
| 1 | `EF` | `EFI3` | `Pista` (fija) | `3ºA-Completo`, `3ºADi-Completo` |

**`MEC-2FPB`**  ·  asignatura = `MEC` · repeticiones = 11 · NEUTRA · requiereTutor = false · 1 plaza(s) · toca `2FPB`

| Plaza | Asignatura | Profesores | Aula | Subgrupos |
|---|---|---|---|---|
| 1 | `MEC` | `PAU1` | **SIN AULA** ⚠️ | `2FPB-Completo` |

**`Bloque-PTVE_Relig-1B-A`**  ·  asignatura = **NULL** · repeticiones = 1 · NEUTRA · requiereTutor = true · 2 plaza(s) · toca `1B-A`

| Plaza | Asignatura | Profesores | Aula | Subgrupos |
|---|---|---|---|---|
| 1 | `PTVE` | `GH6` | `A2` (fija) | `1B-A-PTVE` |
| 2 | `Relig` | `REL1` | `COM1` (fija) | `1B-A-Relig` |


---

## 3. Ambigüedades y decisiones pendientes

Todo lo que este documento **supone** y no mide. Cada entrada dice qué se ha supuesto, en
qué se ha basado y qué hay que preguntarle al jefe de estudios. Esta sección es tan
importante como el censo: sin resolverla, el catálogo se teclea pero no describe al centro.

---

### A1. La población real de los 306 subgrupos parciales es desconocida (I1)

**Qué he supuesto.** Que cada bloque de optatividad reparte a su grupo entre las vías que
se observan en el horario, y que ese reparto cubre el grupo entero sin solapamiento (I1).

**En qué me he basado.** En el principio de §6.1: *«en la práctica del centro, todos los
alumnos del grupo están haciendo algo en ese tramo»*. La partición es del bloque temporal
completo, no por asignatura.

**Lo que no sé.** Cuántos alumnos hay en cada subgrupo, ni quiénes. El volcado dice que
existen tres vías de `RefMt` en 1º ESO, no cuántos alumnos van a cada una. **Sin esa
información el solver puede colocar el horario, pero nadie puede comprobar que las aulas
tienen capacidad ni que los grupos son viables.**

**Preguntar.** El listado de matrícula por optativa, o al menos el tamaño de cada
subgrupo. Es el dato que falta con más consecuencias.

---

### A2. Optativas de 4º ESO: 24 subgrupos por grupo, sin saber cómo se cruzan (D31-b)

**Qué he supuesto.** Que los bloques `DIG/EXPRE/FOPP/TEC` (3 rep), `Biol/FOPP/TEC` (3 rep),
`DIG/FQ` (3 rep), `MatAc/MatAp` (4 rep) y `AFAVS/CeH/DT/RefLe|RefMt` (1 rep cada uno) son
particiones independientes del mismo grupo, y que un alumno que cursa `DIG` en un bloque es
el mismo que cursa `DIG` en otro (por eso `4ºA-DIG` se reutiliza en
`Bloque-DIG_EXPRE_FOPP_TEC-…` y en `Bloque-DIG_FQ-…`).

**En qué me he basado.** En el Hallazgo D, que valida exactamente este patrón para `DTec` de
1º Bach: la misma optativa aparece en dos bloques con el mismo profesor y la misma aula, y
la población es idéntica.

**Lo que no sé.** Si en 4º ESO se cumple lo mismo. `DIG` sí encaja (mismo profesor `INF1`,
misma aula `A12 Informática` en los dos bloques). `FOPP` y `TEC` aparecen en dos bloques con
el mismo profesor pero **con aulas candidatas distintas**, lo que es compatible con la misma
población pero no lo demuestra.

**Preguntar.** Si el itinerario de 4º ESO es una elección única que arrastra a los cinco
bloques, o si el alumno elige por bloque. Si es lo segundo, la reutilización de subgrupo es
incorrecta y hacen falta subgrupos distintos por bloque.

---

### A3. Subgrupos de 1º Bachillerato: dos bloques de cinco vías, y `DTec` en los dos (D31-c)

**Qué he supuesto.** Que `Bloque-ANAP_DA_DTec_TEstI_TICO-1BACH` y
`Bloque-CE_DTec_Lab_Pat_TEst2-1BACH` (5 plazas y 2 repeticiones cada uno, transversales a
los cuatro grupos) comparten la población de `DTec`: `1B-A-DTec` … `1B-D-DTec` aparecen en
ambos.

**En qué me he basado.** Es **literalmente** el caso del Hallazgo D (`DTec`, `DIB2`,
`TALL1`, 2+2 h). Aquí no estoy infiriendo: el modelo ya lo declara resuelto.

**Lo que no sé.** Si las **otras** cuatro vías de cada bloque se cruzan igual (¿el que hace
`ANAP` en el primero hace `Lab` en el segundo, o `CE`?). El volcado no lo dice: los dos
bloques caen en tramos distintos y el horario por aulas no distingue alumnos.

**Preguntar.** La matriz de itinerarios de 1º Bach: qué combinaciones de optativas existen
realmente.

---

### A4. Modalidades de 2º Bachillerato: los bloques cruzan grupos, no niveles (D31-d)

**Qué he supuesto.** Que `Bloque-BIOL_Físic_Geogr_HART-2BACH` (5 plazas, 4 rep, los tres
grupos), `Bloque-CT_DT-2BACH`, `Bloque-DT_EST_MIT-2BACH`,
`Bloque-Econ_Gri2_QUI-2B-B+2B-C` y `Bloque-Lat2_MaCSa_Mate2-2B-B+2B-C` son bloques de
modalidad que reparten a los alumnos de varios grupos a la vez, y que `2B-x-DT` es la misma
población en los dos bloques donde aparece.

**En qué me he basado.** En que las plazas comparten profesor y aula entre grupos en el
mismo tramo (criterio de R4), y en el Hallazgo C, que describe justo esto para el bloque de
optativas de 1º Bach.

**Lo que no sé.** Si los grupos `2B-A/B/C` son *administrativos* (asignación arbitraria de
alumnos) o ya son *modalidades* (Ciencias / Humanidades / Sociales). Si fueran modalidades,
la mitad de los subgrupos parciales de 2º Bach serían en realidad el grupo completo y el
catálogo se simplificaría mucho.

**Preguntar.** Qué criterio usa el centro para asignar alumnos a `2B-A`, `2B-B` y `2B-C`.

---

### A5. Cinco profesores salen como tutor principal de más de un grupo (I4)

**Qué he supuesto.** Que el profesor que imparte la sesión tutorial de un grupo es su tutor.

**En qué me he basado.** En S8 y en el Hallazgo E, que nombra explícitamente a `FIL2` como
tutor de 1º Bach B y de 1º Bach D, y a `GH6` como tutor de 1º Bach A.

**Lo que no cuadra.**

- **`FIL2` sale como tutor de cinco grupos**: `1B-B`, `1B-D`, `2B-A`, `2B-B`, `2B-C`. Es él
  quien imparte `PTVE` en 1º Bach B/D y `PTEV` en los tres 2º Bach. I4 no lo prohíbe
  (limita a un principal *por grupo*, no a un grupo *por profesor*), pero cinco tutorías es
  implausible: lo normal es que `PTEV` sea una asignatura que él imparte y que el tutor sea
  otro.
- **`EFI3`** sale como tutor de `1B-C`, `4ºD` y `4ºDDi`; **`GH6`**, de `1B-A` y `1ºA`.
- **Contradicción dentro del propio modelo:** §6.1 afirma que *«el profesor GH6 es el tutor
  del grupo [1ºA]»* y el Hallazgo E afirma que GH6 es el tutor de 1º Bach A. Ambas
  derivaciones son correctas contra el volcado; las dos a la vez son improbables en el
  centro. **No lo he resuelto: lo dejo escrito porque no es un error mío, es una tensión
  entre dos secciones de la norma.**
- Los cinco PDC comparten tutor con su padre, porque comparten la sesión de tutoría. Es
  coherente con R9, pero puede que el Di tenga su propio tutor y el volcado no lo muestre.

**Preguntar.** La lista oficial de tutores. Es de las cosas más baratas de confirmar y
afecta a S8 en 22 actividades.

---

### A6. 50 subgrupos parciales reutilizados en más de una actividad (I6)

**Qué he supuesto.** Que la misma tripleta `(grupo, asignatura, profesores)` es la misma
población de alumnos aparezca donde aparezca, y por tanto el mismo `Subgrupo`.

**En qué me he basado.** En I6 (un subgrupo es entidad de primera clase y representa la
misma población en todas sus particiones) y en el Hallazgo D.

**Reutilizaciones cómodas** (mismo profesor y misma aula en los dos bloques): las cuatro de
`DTec` en 1º Bach, las tres de `DT` en 2º Bach, las nueve de `CyR`/`Fr2`/`PEPA` entre los dos
bloques de 2º ESO, las nueve de `BioNu`/`CyR` entre los dos de 3º ESO.

**Reutilizaciones que no forzaría sin confirmar:** `4ºA-DIG`, `4ºB-DIG`, `4ºD-DIG` entre
`Bloque-DIG_EXPRE_FOPP_TEC` y `Bloque-DIG_FQ`, y sus gemelas de `FOPP` y `TEC`. Son el
mismo caso que A2. **Si el centro dice que no son los mismos alumnos, hay que desdoblar
esos subgrupos y el total del paso 9 sube.**

**Preguntar.** Lo mismo que A2.

---

### A7. Once plazas de FPB sin aula (Hallazgo H)

**Qué he supuesto.** Nada. R1 obliga a marcarlas como aula **DESCONOCIDA** y a no inventar,
y así están en el JSON (`_aulaDesconocida: true`).

**Cuáles son.** `AMO`, `CA`, `IPE`, `MECSO`, `PS` y `Tut` de 1º FPB; `CA`, `ELE`, `MEC`,
`PI` y `Tut` de 2º FPB. Son 49 de las 65 celdas sin aula del volcado.

**En qué me he basado.** En el Hallazgo H: el PDF omite el aula cuando el profesor es
titular del grupo, y las asignaturas técnicas de FPB no aparecen en ningún aula del listado
aunque el taller existe físicamente.

**Consecuencia inmediata.** **Estas once plazas no se pueden teclear** (§4, caso 1).

**Preguntar.** En qué espacio se imparten `MEC`, `ELE`, `AMO`, `MECSO`, `PS`, `CA`, `IPE`,
`PI` y las dos tutorías de FPB. Candidatos naturales: `Taller 2` y `Taller 4`, que existen
como ficheros `aula-*.json` y están vacíos.

---

### A8. `nombreCompleto` de las 100 asignaturas y los 59 profesores

**Qué he supuesto.** Nada: está a `null` en el JSON.

**Por qué importa.** `AsignaturaService` y `ProfesorService` exigen `nombreCompleto` no
nulo ni en blanco. Sin ese dato **no se puede completar el paso 3 ni el paso 4**, que son
159 de los 815 envíos.

**Salida de emergencia.** Repetir el código como nombre (`ECO` → «ECO»). Funciona, pero
deja la UI ilegible para quien no conozca los códigos del centro y hace inútil la única
columna que distingue `EFis` de `EFís`.

**Preguntar.** Las dos listas del centro: códigos ↔ nombres. Es un copia y pega de sus
documentos internos.

---

### A9. `capacidad`, `edificio`, `planta` y `sector` de las 43 aulas

**Qué he supuesto.** Nada: `null`. Son nullable en `AulaService`, así que el paso 5 se
puede completar sin ellos.

**Por qué importa igualmente.** Sin `capacidad` no hay forma de comprobar que un subgrupo
cabe en su aula (ver A1), y sin `edificio`/`planta` no se puede poblar §4.4 (distancias
entre aulas) si alguna vez se activa.

**Además, el `tipo` que propongo es mío, no del centro.** Lo he derivado del nombre del
espacio. Dos me chirrían: `Taller 3` lo tipifico `ORDINARIA` porque el Hallazgo H dice que
su uso real es aula teórica de Comunicación y Sociedad para FPB, pese al nombre; y
`A19 TUTOR` lo tipifico `ORDINARIA` a falta de nada mejor (0 celdas en el volcado).

**Preguntar.** Plano del centro con capacidades, y confirmación de los tipos de los diez
espacios sin uso.

---

### A10. `duracionTramos = 1` en las 219 actividades, pero hay 32 bloques contiguos

**Qué he supuesto.** Duración 1 en todo el catálogo, fiel al volcado celda a celda.

**Lo que he medido y no he aplicado.** Hay **32 rachas de tramos consecutivos** del mismo
día y la misma actividad, sin cruzar el recreo. Son sistemáticas y se concentran en dos
sitios:

- **FPB**: `PS-1FPB` martes T1-T2-T3 (tres tramos), `MECSO-1FPB` jueves T4-T5-T6,
  `MEC-2FPB` lunes y viernes T4-T5-T6, `ELE-2FPB` miércoles T4-T5-T6, y once rachas de dos
  tramos más.
- **Ámbitos de PDC**: `ÁmbCM` y `ÁmbSL` de 3º, `AmbCT` y `AmbSL` de 4º, con cuatro, dos,
  tres y tres rachas de dos tramos respectivamente.

`PS-1FPB` martes T1–T3 es **exactamente** el ejemplo del Hallazgo G, que §6.6 modela como
un bloque obligatorio de 3 tramos.

**Por qué no lo he aplicado.** Convertir una racha en `duracionTramos = N` obliga a
recalcular `repeticionesPorSemana` y a decidir **qué instancias forman bloque y cuáles no**
dentro de la misma actividad — el modelo tiene una sola `duracion_tramos` por actividad, así
que `PS-1FPB` (5 sesiones: una racha de 3 y dos sueltas) tendría que partirse en dos
actividades. Eso no es derivable del volcado: es una decisión pedagógica.

**Preguntar.** Qué asignaturas de FPB y qué ámbitos de PDC son bloques obligatorios y de
qué tamaño. Afecta a unas 10-14 actividades y baja algo el total de sesiones.

---

### A11. `patronTemporal` derivado por una regla mía, y §6.1 usa otra

**Qué he supuesto.** `NEUTRA` si `repeticiones = 1`; `DISTRIBUIDA` si las repeticiones caen
todas en días distintos; `NEUTRA` si dos caen el mismo día.

**Dónde discrepa de la norma.** §6.1 marca `Bloque-CyR_OyD_RefMt-1ESO` como `NEUTRA` y mi
regla lo marca `DISTRIBUIDA` (sus dos instancias son miércoles y viernes). En `Fr2/ALCT`
coincidimos: las cuatro salen `DISTRIBUIDA`, igual que §6.1.

**Por qué no me parece grave.** `patronTemporal` es una **preferencia blanda** del solver,
no una restricción. No cambia la factibilidad ni las invariantes; cambia qué solución
prefiere. Pero es una divergencia con la norma y por eso está aquí.

**Preguntar.** Nada al jefe de estudios; es una decisión del arquitecto.

---

### A12. Nombres de subgrupo: `1ºA-CyR-TEC3` donde §6.1 escribe `1ºA-CyR-Tec`

**Qué he supuesto.** Discriminador = código íntegro del profesor.

**Por qué.** §6.1 no es consistente consigo mismo: abrevia el profesor en `CyR`
(`TEC3`→`Tec`, `INF1`→`Inf`) y lo escribe entero en `RefMt` (`MAT6`, `MAT7`, `MAT4`). No hay
regla que reproduzca las dos formas. He elegido la reproducible.

**Correspondencia exacta** (los 24 subgrupos del bloque son los mismos, solo cambia cómo se
escriben cuatro de ellos por grupo): `1ºA-CyR-Tec` ≡ `1ºA-CyR-TEC3`, `1ºA-CyR-Inf` ≡
`1ºA-CyR-INF1`; `1ºA-OyD`, `1ºA-RefMt-MAT6`, `1ºA-RefMt-MAT7`, `1ºA-RefMt-MAT4` y
`1ºA-Completo` son idénticos.

**Preguntar.** Nada. Es cosmético, pero conviene fijarlo antes de teclear 334 códigos.

---

### A13. Códigos de aula: uso el nombre largo, §6.1 usa el corto

**Qué he supuesto.** El alias de R6, que traduce a los nombres largos de los ficheros
`aula-*.json`.

**Dónde discrepa.** §6.1 escribe `A12In (fija)` para la plaza de `CyR`/`INF1`; yo escribo
`A12 Informática`. Es la misma aula. La tabla de alias no es derivable con una regla
(`A6`→`A6 Laboratorio` es prefijo, `Gim`→`Gimnasio` es truncamiento,
`TALL1`→`Taller 1 Aula Plástica` no es ni una cosa ni la otra), así que hay que fijar de
qué lado se teclea.

**Preguntar.** Nada al centro; decisión del arquitecto. Recomiendo el nombre largo: es el
que tiene ficheros propios y el que un humano reconoce en un desplegable de 43 opciones.

---

### A14. Los cinco PDC como cinco grupos, y el tronco de 3º como una plaza de tres subgrupos

**Qué he supuesto.** Lo que manda R9: cinco grupos `DIVERSIFICACION_PDC` con su padre.

**La tensión.** El Hallazgo A, corregido en S23, dice que *«el tronco A8 es un ÚNICO grupo
de Diversificación (3PDC), no tres grupos virtuales»*. R9 me dice tres. **No es una
contradicción real**: con tres grupos Di y una plaza que lista los tres subgrupos
`-Completo`, el tronco sigue siendo una sola sesión en A8 con un solo profesor, que es lo
que S23 exigía. Y los subgrupos Di listan **solo su propio grupo**, que es la otra mitad de
la corrección de S23 (enlazar los grupos de origen daba INFEASIBLE).

**Lo que sí cambia.** Con tres grupos, `3ºADi`, `3ºBDi` y `3ºCDi` pueden en principio
separarse en tramos distintos; con uno, no. En estos datos siempre van juntos, así que la
diferencia es de expresividad, no de contenido.

**Preguntar.** Al arquitecto: si prefiere el `3PDC` único de S23, el paso 6 baja de 5 PDC a
3 y hay que reescribir las actividades del tronco.

---

## 4. Casos sospechosos de ser inexpresables por formulario

Contrastado contra los `*Request` del backend y los formularios de
`app/frontend/src/app/components/`. **Hay uno, y es duro.**

### Caso 1 (real) — Las once plazas de FPB sin aula no se pueden teclear

`ActividadService.validarXor` rechaza con 400 toda plaza que no traiga aula fija ni al menos
un aula candidata:

> `"una plaza necesita aula fija o al menos un aula candidata"`

El formulario de actividad refuerza lo mismo antes de enviar: el radio `modoAula` obliga a
elegir entre `FIJA` y `CANDIDATAS`, y ambas ramas tienen validador de obligatoriedad.

No hay puerta de atrás: es una regla de dominio en el servicio, no solo una validación de
UI. **Las once plazas de A7 no entran tal como están.** Las tres salidas posibles, ninguna
gratuita:

1. Preguntar el aula real al jefe de estudios (lo correcto, y probablemente `Taller 2` /
   `Taller 4`).
2. Crear un aula ficticia `TALLER-FPB` y fijarla. Teclea, pero mete en el catálogo un
   espacio que no existe y contamina el oráculo de regresión.
3. Poner como candidatas todas las aulas plausibles. Le da al solver una libertad que el
   centro no tiene y probablemente produzca un horario que nadie reconoce.

Esto bloquea 11 de las 316 plazas y 2 de los 28 grupos. **Es lo primero que hay que
resolver antes de empezar a teclear.**

### Lo que sí cabe, y conviene decir que cabe

Comprobado uno a uno, porque varias de estas cosas parecían candidatas a no caber:

- **Actividad con `asignatura = NULL`** (38 casos): el formulario tiene la opción explícita
  `— varias (una por plaza) —`. ✅
- **Plaza con dos profesores** (co-docencia de LCL, I7 con N=2): el selector de profesores
  es múltiple. ✅
- **Actividad de 6 plazas** (bloque CyR/OyD/RefMt): «Añadir plaza» no tiene techo. ✅
- **Plaza con subgrupos de varios grupos** (`EF-3ºA+3ºADi`, `Bloque-ATEDU_Rel-4ESO` con seis
  grupos): el selector de subgrupos es múltiple y no filtra por grupo. ✅
- **Aulas candidatas** (37 plazas): rama `CANDIDATAS` del radio. ✅
- **`duracionTramos > 1`** si algún día se aplica A10: `<input type="number" min="1">`, sin
  techo. ✅ (Hallazgo G pedía justo esto.)
- **PDC con grupo padre**: el CRUD de grupos rechaza cualquier tipo que no sea `ORDINARIO`,
  pero el sub-recurso `POST /api/grupos/{idPadre}/pdc` existe y tiene su diálogo. ✅
- **Recreo como tramo no lectivo**: la tabla de jornada tiene su casilla «Recreo». ✅
- **Mismo profesor como tutor principal de varios grupos** (A5): `TutoriaService` solo
  impide dos principales *en el mismo grupo*. Se puede teclear. ✅ *(que se pueda no
  significa que sea correcto — ver A5.)*

Y dos fricciones que no son bloqueos pero que van a doler en 548 envíos:

- Los selectores múltiples son `<select multiple>` nativos: **un clic sin Ctrl deselecciona
  todo lo demás**, y el PUT reemplaza la lista entera. En el formulario de subgrupo eso
  vacía la población sin avisar.
- El selector de subgrupos del formulario de actividad **listará los 334 sin filtro ni
  búsqueda**, y el de aulas los 43. Teclear
  `Bloque-ATEDU_Rel-4ESO` significa localizar 30 subgrupos concretos en una lista de 334.

---

## 5. Verificación contra §6.1

La derivación se ha hecho **sin mirar §6.1 mientras corría** y luego se ha cruzado. El
resultado es coincidencia estructural completa.

### 5.1 Bloque CyR / OyD / RefMt de 1º ESO

§6.1 lo describe como **una sola actividad**, `asignatura = NULL`, `repeticiones = 2`, con
**seis plazas** transversales a los cuatro grupos y 24 subgrupos. Lo derivado:

| Concepto | §6.1 | Derivado | ¿Coincide? |
|---|---|---|---|
| Nº de actividades | 1 | 1 (`Bloque-CyR_OyD_RefMt-1ESO`) | ✅ |
| `asignatura` | NULL | NULL | ✅ |
| `repeticiones` | 2 | 2 (X·T3 y V·T3) | ✅ |
| Nº de plazas | 6 | 6 | ✅ |
| Grupos cubiertos | 1ºA, 1ºB, 1ºC, 1ºD | 1ºA, 1ºB, 1ºC, 1ºD | ✅ |
| Subgrupos | 24 (6 por grupo) | 24 (6 por grupo) | ✅ |
| Plaza CyR · TEC3 | candidatas {A5, B07} | candidatas {A5, B07} | ✅ |
| Plaza CyR · INF1 | A12In **fija** | `A12 Informática` **fija** | ✅ (alias, A13) |
| Plaza OyD · FIL3 | candidatas {A11, A5} | candidatas {A11, A5} | ✅ |
| Plaza RefMt · MAT6 | candidatas {A3, A11} | candidatas {A3, A11} | ✅ |
| Plaza RefMt · MAT7 | candidatas {A14, A3} | candidatas {A14, A3} | ✅ |
| Plaza RefMt · MAT4 | candidatas {A10, A14} | candidatas {A10, A14} | ✅ |
| `patronTemporal` | NEUTRA | DISTRIBUIDA | ⚠️ ver A11 |
| Nombres de subgrupo | `1ºA-CyR-Tec` | `1ºA-CyR-TEC3` | ⚠️ cosmético, ver A12 |

Las seis plazas coinciden **una a una**, incluidos los conjuntos de aulas candidatas, que
son el detalle más fácil de fallar. Incluida la corrección de la Nota de S68: `OyD` es el
cuarto destino del bloque, y ahí está.

### 5.2 Bloque Fr2 / ALCT

§6.1 y el Hallazgo K son enfáticos: **no** es transversal, son **cuatro actividades
independientes** en tramos que no coinciden. R4 me obliga a *aplicar el criterio, no
suponerlo*. Aplicado, sale esto:

| | §6.1 | Derivado | ¿Coincide? |
|---|---|---|---|
| Nº de actividades | 4 (una por grupo) | 4 (`Bloque-ALCT_Fr2-1ºA/B/C/D`) | ✅ |
| `repeticiones` | 2 cada una | 2 cada una | ✅ |
| Plazas por actividad | 2 | 2 | ✅ |
| 1ºA | Fr2·FRA1·A5 / ALCT·LEN2·A17 | idéntico | ✅ |
| 1ºB | Fr2·FRA1·A11 / ALCT·LEN9·A17 | idéntico | ✅ |
| 1ºC | Fr2·FRA1·A3 / ALCT·LEN9·A17 | idéntico | ✅ |
| 1ºD | Fr2·FRA1·A14 / ALCT·LEN5·cand{A17,A10} | idéntico | ✅ |
| Tramos coincidentes entre grupos | no | no (L·T6+X·T4, L·T3+M·T4, L·T4+J·T2, L·T5+X·T6) | ✅ |
| `patronTemporal` | DISTRIBUIDA | DISTRIBUIDA | ✅ |

Coincidencia total, incluida la excepción de `A10` en una de las dos celdas de `1ºD`.
El criterio de R4 reproduce el Hallazgo K sin ayuda: `FRA1` es común a las cuatro
actividades, pero como sus tramos no coinciden, no se funden.

### 5.3 El resto de 1º ESO A

§6.1 cierra con un recuento: **30 sesiones/semana**. Lo derivado da **30**, con el mismo
desglose:

| Concepto de §6.1 | §6.1 | Derivado |
|---|---|---|
| Ordinarias: PLAS 1, ByG 3, Ing 4, Mat 4, EF 3, Mús 2, Geo 3, TUT 1 | 21 | 21, con los mismos profesores y aulas (`MAT8`/A5, `BYG1`/A5, `ING4`/A5, `GH6`/A5, `EFI2`/Gimnasio, `MUS1`/A5, `DIB1`/A5, `GH6`/A5) |
| LCL co-docencia: 1 plaza, {LEN2, LEN8}, A5, grupo completo | 4 | 4 (`LCL-1ºA`, una plaza, dos profesores, `1ºA-Completo`) |
| Bloque CyR/OyD/RefMt | 2 | 2 |
| Bloque Fr2/ALCT | 2 | 2 |
| Bloque Relig/ATED | 1 | 1 |
| **Total** | **30** | **30** ✅ |

El bloque `Relig`/`ATED` merece mención aparte porque es el que §6.1 presenta como vista
*parcial*. Derivado sale entero y sale **por parejas**, como manda el Hallazgo K: tres
plazas, dos grupos, `Relig`·`REL1`·A5 con `1ºA-Relig` **y** `1ºB-Relig` en la misma plaza,
más `ATED`·`ING6`·A17 para 1ºA y `ATED`·`GH5`·A11 para 1ºB. Es exactamente lo que §6.1
anticipa que se modelaría en §6.4, y el gemelo `Bloque-ATED_Relig-1ºC+1ºD` también aparece.

**No he encontrado ninguna divergencia estructural con §6.1.** Las dos diferencias (A11,
`patronTemporal`; A12/A13, ortografía de subgrupos y aulas) no cambian ni una plaza, ni un
subgrupo, ni una invariante.

### 5.4 Invariantes verificadas sobre el catálogo completo

| | Resultado |
|---|---|
| **I1** | Por construcción: cada grupo aparece en todas sus vías de cada bloque. La población real no es derivable (A1). |
| **I2** | ✅ 0 subgrupos repetidos entre plazas de una misma actividad. |
| **I4** | ✅ Un `TUTOR_PRINCIPAL` por grupo, 28/28. Cinco profesores repiten grupo (A5). |
| **I5** | ✅ Los 5 PDC tienen `grupoPadre` de tipo `ORDINARIO`. |
| **I6** | 50 subgrupos parciales reutilizados en más de una actividad; 4 de ellos avalados por el Hallazgo D, el resto en A6. |
| **I7** | ✅ 0 plazas sin profesor. 4 plazas con dos. |
| **S8** | ✅ Las 22 actividades con `requiereTutor` tienen, en alguna plaza, un profesor que es tutor de un grupo cubierto por esa plaza. |
| **S9** | ✅ Los 840 slots (28 grupos × 30 tramos) quedan cubiertos por **exactamente una** actividad cada uno. Ningún grupo aparece en dos actividades del mismo tramo. |

S9 es la comprobación más fuerte del conjunto: significa que las 219 actividades reproducen
el horario real completo, sin huecos y sin dobles ocupaciones.

---

## 6. Nota sobre aulas compatibles (R11)

**No se derivan.** Semántica de S75: cero filas en `AsignaturaAulaCompatible` significa
*irrestricta*, y el centro real corre sin poblarlas. Poblarlas a ojo solo podría hacer
INFEASIBLE un horario que hoy es factible.

Como nota informativa para el jefe de estudios, estas asignaturas usan **sistemáticamente**
un espacio especializado en los volcados, y serían las candidatas si algún día se quiere
poblar la tabla:

| Asignatura(s) | Espacio | Observación |
|---|---|---|
| `EF`, `EFis`, `EFís`, `EdFís`, `AFAVS` | `Gimnasio`, `Pista` | Alternan entre los dos |
| `TICO`, `DIG`, y `CyR`·`INF1` | `A12 Informática` | La otra vía de `CyR` (`TEC3`) va a aula ordinaria |
| `DA`, `EXPRE`, `PEPA`, `DTec`, `PLAS` | `C01 Aula Plástica`, `Taller 1 Aula Plástica` | |
| `ANAP`, `Lab` | `A6 Laboratorio` | |
| Ámbitos de PDC de 3º (`ÁmbCM`, `ÁmbSL`) | `A8` | Aula propia del tronco Di, no es un tipo especializado |
| Técnicas de FPB | **desconocido** | Ver A7 |
