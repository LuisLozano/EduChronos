# Informe de conservación del catálogo derivado

Generado por `tools/carga-centro/derivar-catalogo.py`. Volcados: `docs/horario-referencia/2026-2027` (30 grupo-*.json, 0 aula-*.json). Decisiones: `docs/horario-referencia/2026-2027/decisiones-catalogo.json`. Parche: `docs/horario-referencia/2026-2027/parche-aulas.json`.

## Cifras

| familia | n |
|---|---|
| grupos | 30 |
| niveles | 8 |
| asignaturas | 101 |
| profesores | 59 |
| aulas | 32 |
| actividades | 217 |
| plazas | 310 |
| plazas con aulaFija | 287 |
| plazas con aulasCandidatas | 23 |
| subgrupos | 379 |
| sesiones semanales | 629 |
| envíos de la carga | 852 |

Prevalidación de `cargar-centro.py` sobre el catálogo (nombres aparte): 0 violaciones.

## Horas por grupo

| grupo | catálogo | franjas del volcado | celdas del volcado |
|---|---|---|---|
| 1ºA | 30 | 30 | 49 |
| 1ºB | 30 | 30 | 49 |
| 1ºC | 30 | 30 | 49 |
| 1ºD | 30 | 30 | 50 |
| 2ºA | 30 | 30 | 43 |
| 2ºB | 30 | 30 | 43 |
| 2ºC | 30 | 30 | 43 |
| 2ºD | 30 | 30 | 43 |
| 3ºA | 30 | 30 | 41 |
| 3ºADi | 30 | 30 | 33 |
| 3ºB | 30 | 30 | 41 |
| 3ºBDi | 30 | 30 | 33 |
| 3ºC | 30 | 30 | 41 |
| 3ºCDi | 30 | 30 | 33 |
| 4ºA | 30 | 30 | 62 |
| 4ºB | 30 | 30 | 62 |
| 4ºBDi | 30 | 30 | 53 |
| 4ºC | 30 | 30 | 53 |
| 4ºCDi | 30 | 30 | 53 |
| 1B-Ac | 30 | 30 | 51 |
| 1B-Am | 30 | 30 | 55 |
| 1B-Bc | 30 | 30 | 51 |
| 1B-Bm | 30 | 30 | 55 |
| 1B-Ch | 30 | 30 | 55 |
| 1B-Cs | 30 | 30 | 51 |
| 2B-A | 30 | 30 | 51 |
| 2B-B | 30 | 30 | 67 |
| 2B-C | 30 | 30 | 51 |
| 1FPB | 30 | 30 | 30 |
| 2FPB | 30 | 30 | 30 |

## Conservación

Clave `(grupo, día, tramo, asignatura, profesor)`, como conjunto (M2 de S198).

| | n |
|---|---|
| claves del volcado | 1421 |
| claves del catálogo | 1421 |
| comunes | 1421 |
| agregadas (grupo, asignatura, profesor) volcado / catálogo | 583 / 583 |
| divergencias explicadas por decisión | 0 |
| divergencias sin explicar | 0 |


## Parche de aulas

18 sesiones aplicadas. La regla de aulas por plaza se recalcula después del parche; ninguna otra plaza cambia.

| # | profesor (parche) | código | día·tramo | asignatura | grupos | aula | actividad · plaza | aula de la plaza antes → después | confirmación |
|---|---|---|---|---|---|---|---|---|---|
| 1 | Barba Riquel, María Carmen | INF1 | L1 | DIG | 4ºC | B12 → A12 | `DIG-4ºC` · 1 (DIG/INF1) | fija B12 → fija A12 | pendiente |
| 2 | Barba Riquel, María Carmen | INF1 | L5 | TICO | 1B-Ac 1B-Am 1B-Bc 1B-Bm 1B-Ch 1B-Cs | B12 → A12 | `Bloque-DTec_PATRI_PLAB_TES1_TICO-1BACH` · 5 (TICO/INF1) | fija B12 → fija A12 | pendiente |
| 3 | Barba Riquel, María Carmen | INF1 | M4 | DIG | 4ºA 4ºB 4ºBDi 4ºC 4ºCDi | B12 → A12 | `Bloque-DIG_EXPRE_FOPP_TEC-4ESO` · 1 (DIG/INF1) | fija B12 → fija A12 | pendiente |
| 4 | Barba Riquel, María Carmen | INF1 | X2 | DIG | 4ºC | B12 → A12 | `DIG-4ºC` · 1 (DIG/INF1) | fija B12 → fija A12 | pendiente |
| 5 | Barba Riquel, María Carmen | INF1 | X3 | CyR | 3ºA 3ºB 3ºC | B12 → A12 | `Bloque-B1_BioNu_CyR_RefMt-3ºA+3ºB+3ºC` · 3 (CyR/INF1) | fija B12 → fija A12 | pendiente |
| 6 | Barba Riquel, María Carmen | INF1 | X4 | CyR | 1ºA 1ºB 1ºC 1ºD | B12 → A12 | `Bloque-CyR_OyD_RefMt-1ESO` · 1 (CyR/INF1) | fija B12 → fija A12 | pendiente |
| 7 | Barba Riquel, María Carmen | INF1 | X5 | TICO | 1B-Ac 1B-Am 1B-Bc 1B-Bm 1B-Ch 1B-Cs | B12 → A12 | `Bloque-DTec_PATRI_PLAB_TES1_TICO-1BACH` · 5 (TICO/INF1) | fija B12 → fija A12 | pendiente |
| 8 | Barba Riquel, María Carmen | INF1 | J3 | DIG | 4ºA 4ºB 4ºBDi 4ºC 4ºCDi | B12 → A12 | `Bloque-DIG_EXPRE_FOPP_TEC-4ESO` · 1 (DIG/INF1) | fija B12 → fija A12 | pendiente |
| 9 | Barba Riquel, María Carmen | INF1 | V1 | DIG | 4ºC | B12 → A12 | `DIG-4ºC` · 1 (DIG/INF1) | fija B12 → fija A12 | pendiente |
| 10 | Barba Riquel, María Carmen | INF1 | V2 | DIG | 4ºA 4ºB 4ºBDi 4ºC 4ºCDi | B12 → A12 | `Bloque-DIG_EXPRE_FOPP_TEC-4ESO` · 1 (DIG/INF1) | fija B12 → fija A12 | pendiente |
| 11 | Barba Riquel, María Carmen | INF1 | V3 | CyR | 3ºA 3ºB 3ºC | B12 → A12 | `Bloque-B1_BioNu_CyR_RefLe-3ºA+3ºB+3ºC` · 3 (CyR/INF1) | fija B12 → fija A12 | pendiente |
| 12 | Barba Riquel, María Carmen | INF1 | V4 | CyR | 1ºA 1ºB 1ºC 1ºD | B12 → A12 | `Bloque-CyR_OyD_RefMt-1ESO` · 1 (CyR/INF1) | fija B12 → fija A12 | pendiente |
| 13 | Carbonell Coronado, Carmen | MAT7 | M3 | CyR | 2ºA 2ºB 2ºC 2ºD | B12 → A12 | `Bloque-CyR_PEPA_RefLe-2ESO` · 1 (CyR/MAT7) | fija B12 → fija A12 | pendiente |
| 14 | Carbonell Coronado, Carmen | MAT7 | J5 | CyR | 2ºA 2ºB 2ºC 2ºD | B12 → A12 | `Bloque-CyR_PEPA_RefMt-2ESO` · 1 (CyR/MAT7) | fija B12 → fija A12 | pendiente |
| 15 | Téllez Martínez, Olga | DIB2 | L2 | PLAS | 1ºB | TALL1 → A4 | `PLAS-1ºB` · 1 (PLAS/DIB2) | fija TALL1 → fija A4 | pendiente |
| 16 | Téllez Martínez, Olga | DIB2 | L3 | PLAS | 1ºC | TALL1 → A4 | `PLAS-1ºC` · 1 (PLAS/DIB2) | fija TALL1 → fija A4 | pendiente |
| 17 | Téllez Martínez, Olga | DIB2 | M6 | PLAS | 1ºD | TALL1 → A5 | `PLAS-1ºD` · 1 (PLAS/DIB2) | fija TALL1 → fija A5 | pendiente |
| 18 | Téllez Martínez, Olga | DIB2 | X2 | PLAS | 1ºA | TALL1 → A4 | `PLAS-1ºA` · 1 (PLAS/DIB2) | fija TALL1 → fija A4 | pendiente |

Plazas modificadas: 12.

## Decisiones

| id | fija | aplicaciones | fuente |
|---|---|---|---|
| `jornada-horas` | jornada.horas | 1 | tools/carga-centro/extraer-horario.py:80-88 (TRAMOS, derivado de las etiquetas de P05 en S196) |
| `jornada-recreo` | jornada.recreo | 1 | tools/carga-centro/extraer-horario.py:84 y :29-30; las 38 entradas de la clave recreo de profesor-*.json confirman la franja |
| `niveles-orden` | niveles.orden | 1 | docs/horario-referencia/ESPECIFICACION-CATALOGO.md:68-79 |
| `aulas-nombre-largo` | aulas.alias | 0 | sin fuente |
| `aulas-tipo-neutro` | aulas.tipo | 32 | sin fuente |
| `tutores-bachillerato` | tutorias.tutorPrincipal | 9 | sin fuente |
| `requieretutor-ptve-ptev` | actividades.requiereTutor | 4 | sin fuente |
| `notas-invariantes` | meta.notasInvariantes | 1 | sin fuente |

Entradas «sin fuente»: 5 (`aulas-nombre-largo`, `aulas-tipo-neutro`, `tutores-bachillerato`, `requieretutor-ptve-ptev`, `notas-invariantes`).
Decisiones sin ninguna aplicación: `aulas-nombre-largo`.

## Grupos

| título del volcado | grupo |
|---|---|
| 1ºBACH A Ciencias | 1B-Ac |
| 1ºBACH A Mixto | 1B-Am |
| 1ºBACH B Ciencias | 1B-Bc |
| 1ºBACH B Mixto | 1B-Bm |
| 1ºBACH C Humanidades | 1B-Ch |
| 1ºBACH C Sociales | 1B-Cs |
| 1º FPB | 1FPB |
| 1º ESO A | 1ºA |
| 1º ESO B | 1ºB |
| 1º ESO C | 1ºC |
| 1º ESO D | 1ºD |
| 2ºBACH A | 2B-A |
| 2ºBACH B | 2B-B |
| 2ºBACH C | 2B-C |
| 2º FPB | 2FPB |
| 2º ESO A | 2ºA |
| 2º ESO B | 2ºB |
| 2º ESO C | 2ºC |
| 2º ESO D | 2ºD |
| 3º ESO A | 3ºA |
| 3º ESO A PDC | 3ºADi |
| 3º ESO B | 3ºB |
| 3º ESO B PDC | 3ºBDi |
| 3º ESO C | 3ºC |
| 3º ESO PDC | 3ºCDi |
| 4º ESO A | 4ºA |
| 4º ESO B | 4ºB |
| 4º ESO B PDC | 4ºBDi |
| 4º ESO C | 4ºC |
| 4º ESO C PDC | 4ºCDi |
