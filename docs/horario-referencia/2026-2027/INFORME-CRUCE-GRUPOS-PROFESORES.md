# Informe de cruce — horario por grupos ↔ horario por profesores (2026/2027)

Cruce determinista de los volcados de `docs/horario-referencia/2026-2027/` (`grupo-*.json` vs `profesor-*.json`), generado por `tools/carga-centro/cruzar-grupos-profesores.py` (S197). **Clave de cruce:** `(grupo, día, tramo, profesor)`.

> Leyenda «Profesores:» leída de: `Horarios de grupos.pdf`, `Horarios de grupos (11).pdf` (el último gana por título, como en el extractor). Los volcados no la guardan.

## Normalización de códigos de grupo

La vista de grupos titula con forma larga y la de profesores imprime la forma corta. Mapeo de `tools/carga-centro/codigos_grupo.py`, aplicado **solo para el cruce**:

| Forma larga (grupos) | Forma corta (profesores) |
|---|---|
| `1º ESO A` | `1ºA` |
| `1º ESO B` | `1ºB` |
| `1º ESO C` | `1ºC` |
| `1º ESO D` | `1ºD` |
| `1º FPB` | `1FPB` |
| `1ºBACH A Ciencias` | `1B-Ac` |
| `1ºBACH A Mixto` | `1B-Am` |
| `1ºBACH B Ciencias` | `1B-Bc` |
| `1ºBACH B Mixto` | `1B-Bm` |
| `1ºBACH C Humanidades` | `1B-Ch` |
| `1ºBACH C Sociales` | `1B-Cs` |
| `2º ESO A` | `2ºA` |
| `2º ESO B` | `2ºB` |
| `2º ESO C` | `2ºC` |
| `2º ESO D` | `2ºD` |
| `2º FPB` | `2FPB` |
| `2ºBACH A` | `2B-A` |
| `2ºBACH B` | `2B-B` |
| `2ºBACH C` | `2B-C` |
| `3º ESO A` | `3ºA` |
| `3º ESO A PDC` | `3ºADi` |
| `3º ESO B` | `3ºB` |
| `3º ESO B PDC` | `3ºBDi` |
| `3º ESO C` | `3ºC` |
| `3º ESO PDC` | `3ºCDi` |
| `4º ESO A` | `4ºA` |
| `4º ESO B` | `4ºB` |
| `4º ESO B PDC` | `4ºBDi` |
| `4º ESO C` | `4ºC` |
| `4º ESO C PDC` | `4ºCDi` |

## Normalización de profesores

Cada página de la vista de profesores se casa con el código de la leyenda «Profesores:» cuyo nombre (truncado en la leyenda) es prefijo del título. Uno → su código; ninguno → «sin código»; varios → error.

| Página | Título (profesores) | Código | Nombre en la leyenda |
|---|---|---|---|
| 1 | Afán Herencia, María Trinidad | `BYG2` | Afán Herencia, María Tri |
| 2 | Barba Riquel, María Carmen | `INF1` | Barba Riquel, María Carm |
| 3 | Barroso Rodríguez, José | `EFI2` | Barroso Rodríguez, José |
| 4 | Carbonell Coronado, Carmen | `MAT7` | Carbonell Coronado, Carm |
| 5 | Carrasco López, Héctor | `FIS3` | Carrasco López, Héctor |
| 6 | Castellanos Fernández,Isabel | `LEN8` | Castellanos Fernández,Is |
| 7 | Chacón Muñoz, Isabel María | sin código | — |
| 8 | Doña Alcaide, Jesús | `ING1` | Doña Alcaide, Jesús |
| 9 | Dorador de los Santos, María de los | `LEN3` | Dorador de los Santos, M |
| 10 | Esteban Parra, Ginés MIguel | `FIS4` | Esteban Parra, Ginés MIg |
| 11 | Estepa Guillén, María Ángeles | `BIO3t` | Estepa Guillén, María Án |
| 12 | Fernández García, Felipe Manuel | `MAT4` | Fernández García, Felipe |
| 13 | Fuentes Lucas Torres, Salvador | `ECO` | Fuentes Lucas Torres, Sa |
| 14 | Galdámez Mayoral, Patricia | `PAU1` | Galdámez Mayoral, Patric |
| 15 | García Bernal, María Teresa | `LEN2` | García Bernal, María Ter |
| 16 | García Rodríguez, Lina María | `LEN5` | García Rodríguez, Lina M |
| 17 | García-Baquero Vega, José Antonio | `FOL2` | García-Baquero Vega, Jos |
| 18 | Gutiérrez Sánchez, Carlos Alberto | `MAT6` | Gutiérrez Sánchez, Carlo |
| 19 | Henajeros Luque, Inés | `MAT8` | Henajeros Luque, Inés |
| 20 | Huerta Rodríguez, Joaquín de la | `FRA1T` | Huerta Rodríguez, Joaquí |
| 21 | Jiménez López, Juan | `TEC1` | Jiménez López, Juan |
| 22 | Jiménez Montes, María de los Ángele | `GH4` | Jiménez Montes, María de |
| 23 | Jiménez Morgaz, Elena María | `ING5` | Jiménez Morgaz, Elena Ma |
| 24 | LEN9 | `LEN9` | LEN9 |
| 25 | Lobato Montes, María Carmen | sin código | — |
| 26 | López Codón, Rocío | `FOL4` | López Codón, Rocío |
| 27 | López Ruiz, Paloma | `MAT5` | López Ruiz, Paloma |
| 28 | Macías Magro, Sonia | `BYG1` | Macías Magro, Sonia |
| 29 | Macías Negrillo, Jesús María | `LEN7` | Macías Negrillo, Jesús M |
| 30 | Marín Rodríguez, Rafael | `FIS2` | Marín Rodríguez, Rafael |
| 31 | Martín Ginés, Almudena | `TEC3` | Martín Ginés, Almudena |
| 32 | Mateos-Cañero Maraver, Lourdes | sin código | — |
| 33 | Mejías Márquez, Manuela | `FIL2` | Mejías Márquez, Manuela |
| 34 | Mesa Guarnido, Eva María | `MAT2` | Mesa Guarnido, Eva María |
| 35 | Montes García, Antonio | `FIL1` | Montes García, Antonio |
| 36 | Moreno Araujo, Fco. Javier | `LEN1` | Moreno Araujo, Fco. Javi |
| 37 | Moreno Mesa, Juan Manuel | `LEN6` | Moreno Mesa, Juan Manuel |
| 38 | Ortega Roda, Pedro Juan | `MUS1` | Ortega Roda, Pedro Juan |
| 39 | Painta Lesiak, Bárbara Anna | `ING3` | Painta Lesiak, Bárbara A |
| 40 | Pérez Alvarez, Adolfo Luis | `GH6` | Pérez Alvarez, Adolfo L |
| 41 | Pérez Lobato, José | `PAU2` | Pérez Lobato, José |
| 42 | Pérez Morales, Cristina | `MAT3` | Pérez Morales, Cristina |
| 43 | Piqueras Mateos, Alejandro | `EFI3` | Piqueras Mateos, Alejand |
| 44 | Redondo Ceballos, María Ángeles | `TEC2` | Redondo Ceballos, María |
| 45 | Religión Evangélica | `REV` | Religión Evangélica |
| 46 | Ríos Palomo, María del Carmen | `MAT1` | Ríos Palomo, María del C |
| 47 | Rivas Prieto, Carmen | `REL1` | Rivas Prieto, Carmen |
| 48 | Rodríguez Rodríguez, Esperanza | `GH5` | Rodríguez Rodríguez, Esp |
| 49 | Rodríguez Tirado, Joaquín | `ING6` | Rodríguez Tirado, Joaquí |
| 50 | Romero Quiles, Aguas Santas | `FIS1` | Romero Quiles, Aguas San |
| 51 | Rosales Jiménez, Fco. Javier | `GH1` | Rosales Jiménez, Fco. Ja |
| 52 | Rúiz Arjona, Carmen María | `ORI1` | Rúiz Arjona, Carmen Marí |
| 53 | Ruiz del Castillo, María Azucena | `DIB1` | Ruiz del Castillo, María |
| 54 | Sacramento Trujillo, Daniel | `CLA1` | Sacramento Trujillo, Dan |
| 55 | Sierra Naranjo, Rubén | `EFI1` | Sierra Naranjo, Rubén |
| 56 | Solís Vázquez, José David | `FIL3` | Solís Vázquez, José Davi |
| 57 | Téllez Martínez, Olga | `DIB2` | Téllez Martínez, Olga |
| 58 | Torres Bravo, Adoración | `ING4` | Torres Bravo, Adoración |
| 59 | Torres Cubillo, Mariano José | `GH2` | Torres Cubillo, Mariano |
| 60 | Torres Villar, Myriam | `LEN4` | Torres Villar, Myriam |
| 61 | Vela Montero, José Antonio | `GH3` | Vela Montero, José Anton |
| 62 | Velázquez Barba, Trinidad | `ING2` | Velázquez Barba, Trinida |

Códigos de la leyenda sin página: **0**.

## 1. Solo en el horario por grupos

Clave presente en *grupos* y ausente en *profesores*.

**Total: 0.**

| Grupo | Día · Tramo | Profesor | Asignatura | Aula | Título (grupos) |
|---|---|---|---|---|---|

## 2. Solo en el horario por profesores

Clave presente en *profesores* y ausente en *grupos*.

**Total: 0.**

| Grupo | Día · Tramo | Profesor | Asignatura | Aula | Página (profesores) |
|---|---|---|---|---|---|

## 3. Asignatura distinta

Misma clave en las dos vistas con distinto código de asignatura.

**Total: 0.**

| Grupo | Día · Tramo | Profesor | Asignatura (grupos) | Asignatura (profesores) |
|---|---|---|---|---|

## 4. Aula distinta

Misma clave, aula no nula en las dos vistas y distinta.

**Total: 0.**

| Grupo | Día · Tramo | Profesor | Aula (grupos) | Aula (profesores) |
|---|---|---|---|---|

## 5. Aula en una sola vista

Misma clave, aula impresa en una vista y no en la otra.

**Total: 0.**

| Grupo | Día · Tramo | Profesor | Asignatura | Aula (grupos) | Aula (profesores) |
|---|---|---|---|---|---|

## Aparte (no son discrepancias)

### Actividades sin grupo
Celdas de la vista de profesores sin ningún grupo (guardias, reuniones…): no tienen equivalente en la vista de grupos.

| Código | Celdas | Profesores |
|---|---|---|
| `G` | 137 | 45 |
| `RT12` | 11 | 11 |
| `ORYCA` | 10 | 5 |
| `Gbibl` | 9 | 7 |
| `RT34` | 9 | 9 |
| `RED` | 6 | 6 |
| `APSTE` | 2 | 1 |
| `FOREI` | 2 | 1 |
| `HUERT` | 2 | 1 |
| `PROAR` | 2 | 1 |
| `REYR` | 2 | 1 |
| `conv` | 2 | 2 |

### Recreo
Celdas de la fila del recreo de la vista de profesores (la de grupos no la tiene).

| Código | Celdas |
|---|---|
| `GR` | 33 |
| `GRBib` | 5 |

### Profesores sin código
Páginas cuyo título no casa con ningún nombre de la leyenda; no entran en el cruce.

| Página | Celdas | Celdas con grupo | Códigos de asignatura |
|---|---|---|---|
| 7 | 2 | 0 | `G` |
| 25 | 1 | 0 | `RED` |
| 32 | 0 | 0 | — |

## Resumen

- Entradas de **grupos** (una por celda): **1421**
- Entradas de **profesores** (una por grupo de cada celda con grupos): **1421**
- Claves comunes: **1421**
- Grupos normalizados: **30** · profesores con código: **59** · sin código: **3**
- §1 Solo en grupos: **0**
- §2 Solo en profesores: **0**
- §3 Asignatura distinta: **0**
- §4 Aula distinta: **0**
- §5 Aula en una sola vista: **0**
- Aparte: actividades sin grupo **194** celdas (12 códigos) · recreo **38** celdas · profesores sin código **3** páginas
- **Discrepancias totales (§1–§5): 0**
