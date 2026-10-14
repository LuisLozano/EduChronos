# Comparación con el horario oficial de 2026/2027 (S228)

Objetivo `O-comparacion` (H5). Compara el horario que genera Educhronos sobre los datos de 2026/2027 con el horario oficial del centro, con métricas fijadas y selladas antes de calcularlas.

## Cómo se ha medido
- Oficial: volcados de `docs/horario-referencia/2026-2027/` (30 grupos y 62 profesores).
- Educhronos: cinco generaciones con la petición de la interfaz (`{"maxSegundos":600}`, semilla por defecto) sobre una copia de la base de S222; horarios 4 a 8 (`s228/f3`).
- Métricas: `METRICAS-COMPARACION.md`, selladas en `1344596` (sha256 `57845934dad6fb9e6259494e65e726bc3662fa12a438725da23de215f9f61fa9`) antes del script `tools/carga-centro/comparar-con-oficial.py` (`10602df`).
- Derivación: `python3 -I -B tools/carga-centro/comparar-con-oficial.py --db <copia de s228/f3> --volcados docs/horario-referencia/2026-2027 --horario 4 5 6 7 8`; salidas en `s228/f4/B4-corridas`.

## Validez y generación
- Las cinco corridas dieron horario en unos 601 s, sin violaciones en el diagnóstico y con la carga conservada (0 divergentes en 583 claves).
- Con las semillas 1 a 5, a 600 s, ninguna dio horario (`s228/f1`): generar con estos datos depende de la semilla fija de la interfaz (`D-generacion-no-reproducible`).

## Resultados
| Métrica | Oficial | Mediana | Mín–máx | ¿Peor en las cinco? |
|---|---|---|---|---|
| M1 huecos del profesorado (total) | 135 | 227 | 219–320 | sí |
| M2 asignatura repetida en el día (total) | 77 | 76 | 64–80 | no |
| M3 días con clase por profesor (suma) | 267 | 280 | 276–281 | sin dirección |
| M4a guardias ordinarias (suma de cuadrados) | 651 | 633 | 633–633 | no |
| M4b guardias de recreo (suma de cuadrados) | 290 | 290 | 290–290 | no |
| M5 aulas por grupo y día | 565 | 560 | 548–571 | no |
| M6 coincidencia de tramo (%) | — | 12,17 | 6,97–13,51 | sin dirección |

## Hallazgo
**Huecos del profesorado (M1): fallo del generador.** El objetivo del solver tiene un término de ventanas del profesorado, y a 600 s deja un 68 % más de huecos que el oficial. Caso: ECO, 0 huecos en el oficial y de 6 a 11 en las corridas. Cifras secundarias en la misma dirección: pares profesor-día con 3 o más huecos, 16 frente a 6; profesores con clase los 5 días, 51 frente a 43. Causa sin medir: presupuesto o pesos blandos sin calibrar. Ficha `D-huecos-profesorado`; no bloquea la prueba (decisión del usuario).

## Observaciones (no clasificadas por la regla sellada)
- Asignaturas DISTRIBUIDA: Educhronos las repite el mismo día más que el oficial en las cinco corridas (15 a 18 frente a 10); es una cifra secundaria de M2.
- Coincidencia de tramo del 6,97 al 13,51 %: dos horarios igual de válidos casi no comparten horas, así que parecerse no significa coincidir.
- El código de aula B12 del oficial no existe en la base: es el parche de aulas de `O-carga-2026` (limitación declarada de M5). El oficial tiene 16 celdas de grupo sin aula y las corridas, ninguna sesión sin aula.
