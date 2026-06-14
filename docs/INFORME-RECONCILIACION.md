# Informe de reconciliación — horario por grupos ↔ horario por aulas

Cruce determinista de los volcados fieles en `docs/horario-referencia/` (`grupo-*.json` vs `aula-*.json`). **Clave de cruce:** `(grupo, día, tramo, profesor)`.

> Se cruza por **código de profesor** porque es estable entre las dos fuentes (misma persona → mismo código en ambas leyendas). No se cruza por asignatura por prudencia: cada nivel/grupo usa su propia abreviatura (`Ing`, `Ingl`, `IngDi`…) y quería una clave robusta. §5 verifica *a posteriori* que, allí donde el cruce casa, el código de asignatura coincide entre fuentes.

## Normalización de códigos de grupo

El horario por grupos titula las páginas con forma larga (`1º ESO A`) y el horario por aulas referencia los grupos con forma corta (`1ºA`). Mapeo aplicado **solo para el cruce** (los ficheros de volcado conservan el código crudo):

| Forma larga (grupos) | Forma corta (aulas) |
|---|---|
| `Nº ESO L` | `NºL` (p.ej. `1º ESO A`→`1ºA`) |
| `Nº ESO L PDC` | `NºLDi` (diversificación) |
| `Nº ESO PDC` | `NºPDC` (sin letra; correspondencia incierta) |
| `NºBACH L` | `NB-L` (p.ej. `1ºBACH A`→`1B-A`) |
| `Nº FPB` | `NFPB` |

**Grupos sin correspondencia 1:1 entre fuentes** (diversificación de 3º ESO). El horario por grupos tiene una página genérica `3º ESO PDC` mientras que el horario por aulas referencia `3ºCDi`; no hay forma determinista de igualarlos, así que **estas celdas se EXCLUYEN de los cruces §2 y §3** y se contabilizan aparte (revisión manual):

- Solo en horario por **aulas**: `3ºCDi` → **31** celdas
- Solo en horario por **grupos**: `3ºPDC` → **31** celdas

> Nota: los demás grupos PDC (`3º ESO A/B PDC`, `4º ESO A/D PDC`) **sí** mapean a `3ºADi`, `3ºBDi`, `4ºADi`, `4ºDDi` y entran en los cruces con normalidad.

## 1. Celdas con aula OMITIDA en el horario por grupos

Celdas del horario por grupos sin código de aula (lectura segura de que el PDF **no** lo imprime; típico de co-docencia o del profesor titular). **No se rellenan.** Se muestra, como referencia, el aula que indica el horario por aulas para el mismo grupo/día/tramo/profesor.

**Total: 65 celdas** — 0 con aula localizable en el horario por aulas, 65 sin dato en aulas.

Se observan **dos patrones** en las omisiones:

- **Co-docencia de Lengua (LCL)** en 1º ESO: el segundo docente (`LEN6`/`LEN8`/`LEN9`) comparte aula con el titular `LEN2`, y ni *grupos* imprime su aula ni *aulas* lo lista por separado (ver §2).
- **Ciclos de FPB** (`1º FPB`, `2º FPB`): el horario por aulas **no cubre** los talleres/aulas de FPB (las páginas `Taller 2`, `Taller 4`, etc. están vacías), por lo que ninguna de sus clases tiene aula localizable.

| Grupo (crudo) | Día · Tramo | Asignatura (grupos) | Profesor | Aula según *aulas* (ref.) |
|---|---|---|---|---|
| 1º ESO A | Martes · T5 | LCL | LEN8 | — (sin dato en aulas) |
| 1º ESO A | Miércoles · T5 | LCL | LEN8 | — (sin dato en aulas) |
| 1º ESO A | Jueves · T3 | LCL | LEN8 | — (sin dato en aulas) |
| 1º ESO A | Viernes · T4 | LCL | LEN8 | — (sin dato en aulas) |
| 1º ESO B | Lunes · T6 | LCL | LEN9 | — (sin dato en aulas) |
| 1º ESO B | Martes · T6 | LCL | LEN9 | — (sin dato en aulas) |
| 1º ESO B | Miércoles · T4 | LCL | LEN9 | — (sin dato en aulas) |
| 1º ESO B | Jueves · T4 | LCL | LEN9 | — (sin dato en aulas) |
| 1º ESO C | Lunes · T5 | LCL | LEN9 | — (sin dato en aulas) |
| 1º ESO C | Miércoles · T2 | LCL | LEN9 | — (sin dato en aulas) |
| 1º ESO C | Jueves · T3 | LCL | LEN9 | — (sin dato en aulas) |
| 1º ESO C | Viernes · T4 | LCL | LEN9 | — (sin dato en aulas) |
| 1º ESO D | Lunes · T4 | LCL | LEN6 | — (sin dato en aulas) |
| 1º ESO D | Martes · T2 | LCL | LEN6 | — (sin dato en aulas) |
| 1º ESO D | Miércoles · T5 | LCL | LEN6 | — (sin dato en aulas) |
| 1º ESO D | Jueves · T2 | LCL | LEN6 | — (sin dato en aulas) |
| 1º FPB | Lunes · T1 | CA | TEC1 | — (sin dato en aulas) |
| 1º FPB | Lunes · T2 | CA | TEC1 | — (sin dato en aulas) |
| 1º FPB | Lunes · T5 | AMO | PAU2 | — (sin dato en aulas) |
| 1º FPB | Lunes · T6 | AMO | PAU2 | — (sin dato en aulas) |
| 1º FPB | Martes · T1 | PS | PAU2 | — (sin dato en aulas) |
| 1º FPB | Martes · T2 | PS | PAU2 | — (sin dato en aulas) |
| 1º FPB | Martes · T3 | PS | PAU2 | — (sin dato en aulas) |
| 1º FPB | Martes · T4 | IPE | FOL3 | — (sin dato en aulas) |
| 1º FPB | Martes · T5 | MECSO | PAU2 | — (sin dato en aulas) |
| 1º FPB | Martes · T6 | MECSO | PAU2 | — (sin dato en aulas) |
| 1º FPB | Miércoles · T1 | CA | TEC1 | — (sin dato en aulas) |
| 1º FPB | Miércoles · T2 | CA | TEC1 | — (sin dato en aulas) |
| 1º FPB | Miércoles · T5 | AMO | PAU2 | — (sin dato en aulas) |
| 1º FPB | Miércoles · T6 | AMO | PAU2 | — (sin dato en aulas) |
| 1º FPB | Jueves · T1 | IPE | FOL3 | — (sin dato en aulas) |
| 1º FPB | Jueves · T2 | IPE | FOL3 | — (sin dato en aulas) |
| 1º FPB | Jueves · T3 | Tut | PAU2 | — (sin dato en aulas) |
| 1º FPB | Jueves · T4 | MECSO | PAU2 | — (sin dato en aulas) |
| 1º FPB | Jueves · T5 | MECSO | PAU2 | — (sin dato en aulas) |
| 1º FPB | Jueves · T6 | MECSO | PAU2 | — (sin dato en aulas) |
| 1º FPB | Viernes · T3 | PS | PAU2 | — (sin dato en aulas) |
| 1º FPB | Viernes · T4 | PS | PAU2 | — (sin dato en aulas) |
| 1º FPB | Viernes · T5 | AMO | PAU2 | — (sin dato en aulas) |
| 1º FPB | Viernes · T6 | AMO | PAU2 | — (sin dato en aulas) |
| 2º FPB | Lunes · T2 | CA | FIS3 | — (sin dato en aulas) |
| 2º FPB | Lunes · T3 | CA | FIS3 | — (sin dato en aulas) |
| 2º FPB | Lunes · T4 | MEC | PAU1 | — (sin dato en aulas) |
| 2º FPB | Lunes · T5 | MEC | PAU1 | — (sin dato en aulas) |
| 2º FPB | Lunes · T6 | MEC | PAU1 | — (sin dato en aulas) |
| 2º FPB | Martes · T1 | ELE | PAU1 | — (sin dato en aulas) |
| 2º FPB | Martes · T2 | ELE | PAU1 | — (sin dato en aulas) |
| 2º FPB | Martes · T3 | MEC | PAU1 | — (sin dato en aulas) |
| 2º FPB | Martes · T4 | MEC | PAU1 | — (sin dato en aulas) |
| 2º FPB | Martes · T5 | MEC | PAU1 | — (sin dato en aulas) |
| 2º FPB | Martes · T6 | PI | FOL3 | — (sin dato en aulas) |
| 2º FPB | Miércoles · T3 | PI | FOL3 | — (sin dato en aulas) |
| 2º FPB | Miércoles · T4 | ELE | PAU1 | — (sin dato en aulas) |
| 2º FPB | Miércoles · T5 | ELE | PAU1 | — (sin dato en aulas) |
| 2º FPB | Miércoles · T6 | ELE | PAU1 | — (sin dato en aulas) |
| 2º FPB | Jueves · T3 | MEC | PAU1 | — (sin dato en aulas) |
| 2º FPB | Jueves · T4 | MEC | PAU1 | — (sin dato en aulas) |
| 2º FPB | Jueves · T5 | ELE | PAU1 | — (sin dato en aulas) |
| 2º FPB | Jueves · T6 | ELE | PAU1 | — (sin dato en aulas) |
| 2º FPB | Viernes · T1 | CA | FIS3 | — (sin dato en aulas) |
| 2º FPB | Viernes · T2 | CA | FIS3 | — (sin dato en aulas) |
| 2º FPB | Viernes · T3 | Tut | PAU1 | — (sin dato en aulas) |
| 2º FPB | Viernes · T4 | MEC | PAU1 | — (sin dato en aulas) |
| 2º FPB | Viernes · T5 | MEC | PAU1 | — (sin dato en aulas) |
| 2º FPB | Viernes · T6 | MEC | PAU1 | — (sin dato en aulas) |

## 2. Co-profesores (co-docencia) presentes en una fuente y no en la otra

Para cada `(grupo, día, tramo)` **presente en ambas fuentes** se compara el conjunto de profesores. Aquí solo aparece la co-docencia real (un profesor adicional que una fuente registra y la otra no, típicamente el desdoble de Lengua cuyo segundo docente comparte aula). Los casos en que el tramo directamente no existe en una fuente se tratan en §3, y los grupos no mapeables, en la nota de normalización.

**Total: 16 ranuras con co-docencia divergente** (se descartaron 49 ranuras con tramo ausente en una fuente).

| Grupo | Día · Tramo | Solo en *grupos* | Solo en *aulas* |
|---|---|---|---|
| 1ºA | Martes · T5 | LEN8 | — |
| 1ºA | Miércoles · T5 | LEN8 | — |
| 1ºA | Jueves · T3 | LEN8 | — |
| 1ºA | Viernes · T4 | LEN8 | — |
| 1ºB | Lunes · T6 | LEN9 | — |
| 1ºB | Martes · T6 | LEN9 | — |
| 1ºB | Miércoles · T4 | LEN9 | — |
| 1ºB | Jueves · T4 | LEN9 | — |
| 1ºC | Lunes · T5 | LEN9 | — |
| 1ºC | Miércoles · T2 | LEN9 | — |
| 1ºC | Jueves · T3 | LEN9 | — |
| 1ºC | Viernes · T4 | LEN9 | — |
| 1ºD | Lunes · T4 | LEN6 | — |
| 1ºD | Martes · T2 | LEN6 | — |
| 1ºD | Miércoles · T5 | LEN6 | — |
| 1ºD | Jueves · T2 | LEN6 | — |

## 3. Discrepancias grupo ↔ aula ↔ tramo

### 3.1 Aula distinta entre fuentes
Mismo `(grupo, día, tramo, profesor)` con aula explícita en *grupos* que no coincide con la ubicación en *aulas*.

**Total: 0.**

| Grupo | Día · Tramo | Profesor | Aula (grupos) | Aula(s) (aulas) |
|---|---|---|---|---|

### 3.2 En *grupos* pero sin confirmación en *aulas*
`(grupo, día, tramo, profesor)` con aula explícita en *grupos*, pero el horario por aulas no sitúa a ese grupo en esa aula en ese tramo.

**Total: 0.**

| Grupo | Día · Tramo | Profesor | Asignatura (grupos) | Aula (grupos) |
|---|---|---|---|---|

### 3.3 En *aulas* pero sin equivalente en *grupos*
`(grupo, día, tramo, profesor)` presente en el horario por aulas y ausente en el horario por grupos.

**Total: 0.**

| Grupo | Día · Tramo | Profesor | Asignatura (aulas) | Aula |
|---|---|---|---|---|

## 4. Celdas marcadas «dudosa»

**No hay ninguna celda marcada «dudosa».** Todos los PDF tienen capa de texto seleccionable; cada token de la rejilla se clasificó contra la leyenda de su propia página y el recuento de tokens cuadró al 100 % en las 71 páginas (ver `RESUMEN-EXTRACCION.md`).

## 5. Coherencia del código de asignatura entre fuentes

Verificación: para cada `(grupo, día, tramo, profesor)` que casa en ambas fuentes, ¿coincide el código de asignatura?

**Sin diferencias.** Los **1205** cruces con asignatura en ambos lados usan el mismo código. Es decir, las dos fuentes son coherentes en la nomenclatura de asignaturas para toda clase cruzable; la variedad de abreviaturas del conjunto de datos (`Ing`/`Ingl`/`IngDi`, `Bio`/`BIOL`…) corresponde a **niveles/grupos distintos**, no a la misma clase codificada de dos maneras.

## Resumen

- Celdas (entradas) en horario por **grupos**: **1301**
- Celdas (entradas) en horario por **aulas**: **770**
- Celdas totales: **2071**
- Celdas **dudosas**: **0** (grupos 0 / aulas 0)
- §1 Aulas omitidas en grupos: **65**
- §2 Ranuras con co-docencia divergente: **16**
- §3.1 Aula distinta entre fuentes: **0**
- §3.2 En grupos sin confirmar en aulas: **0**
- §3.3 En aulas sin equivalente en grupos: **0**
- §5 Pares de código de asignatura divergentes: **0** (de 1205 cruces)
- Celdas de grupos PDC/diversificación no mapeables (excluidas de cruces): **31** (grupos) + **31** (aulas)
- **Discrepancias de ubicación totales (§3): 0**
