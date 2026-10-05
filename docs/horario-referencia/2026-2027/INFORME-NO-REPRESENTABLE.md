# Lo que el horario oficial de 2026/2027 contiene y Educhronos no representa

Condición 5 de `O-carga-2026` (S199). Fuentes: los volcados de grupos y de
profesores de este directorio y sus PDF; análisis en
`/home/luis/educhronos-aceptacion/s199/f3/` (fuera del repositorio). Nada de lo
que sigue entra en `catalogo-derivado.json`: los 14 códigos de la sección A y
`GrecB` aparecen 0 veces en él. Las leyendas citadas son las del PDF de
profesores, que las imprime truncadas.

## A. Contenido del horario oficial que el modelo no representa

| Qué | Códigos (leyenda) | Celdas | Ficha |
|---|---|---|---|
| Guardias lectivas | `G` «Guardia» (139), `Gbibl` «Gbiblioteca» (9), `conv` «R.Convivencia» (2) | 150 | `D-guardias-sin-modelo` |
| Guardias de recreo | `GR` «Guardia Recreo» (33, sin aula), `GRBib` «Guardia Recreo Bibliotec» (5, en `GrecB`) | 38 | `D-guardias-sin-modelo` |
| Reunión de Equipo Directivo | `RED` «Reunión de Equipo Direct» | 7 | `D-cargos-sin-modelo` |
| Reuniones de tutores (hipótesis sin medir) | `RT12` (11), `RT34` (9); la leyenda repite el código | 20 | `D-cargos-sin-modelo` |
| Funciones y proyectos | `ORYCA` «Ordenación y Catalogació» (10); `APSTE` «Apoyo Steam», `FOREI` «Formación e innovación», `HUERT` «Huerto», `PROAR` «Proyecto Artístico», `REYR` «Redes y Radio» (2 cada uno) | 20 | `D-cargos-sin-modelo` |
| Co-tutor de las 5 PDC | segunda línea «Tutor:» de cada PDC (`ORI1`) | 5 tutorías | `D-cargador-tutoria-pisa` |

Las celdas sin grupo son 197 en 12 códigos, ninguna con aula; las de recreo,
38 de 13 profesores. `GrecB` es el lugar de las guardias de recreo de
biblioteca, no un aula de clase: no está en ninguna leyenda ni entre las aulas
del catálogo. El co-tutor sí lo representa el modelo; lo pierde el cargador,
que emite un PUT de reemplazo por entrada.

## B. Lo que el PDF no trae y el modelo podría guardar

No son huecos del modelo, sino datos sin fuente. Están escritos como decisiones
en `decisiones-catalogo.json`:

- Tutores de Bachillerato: los 9 grupos no imprimen «Tutor:». En los otros 21,
  el tutor del catálogo coincide con uno de los impresos.
- Las entradas `requiereTutor` sin fuente.
- Tipo y nombre largo de las 32 aulas, todas ORDINARIA. `TAL3a` (9 plazas) y
  `TAL3b` (11) son de 1º de Bachillerato. `TAL41` (30 celdas) aloja las
  asignaturas de 1º FPB que en 2025/2026 iban en «Taller 4», de tipo
  TALLER_FPB. `O-aulas` fija los tipos antes de declarar compatibilidades.
- `A12` y `B12`: el parche de aulas (`parche-aulas.json`) pasa 14 sesiones de
  `B12` a `A12` (8 plazas), con la confirmación pendiente. En 2025/2026 `A12`
  era de tipo INFORMATICA.
- Tres páginas de profesor sin código en el PDF de profesores (páginas 7, 25
  y 32): una con 2 celdas `G`, otra con 1 `RED` y una vacía. Ninguna tiene
  clases con grupo. Ver `D-censo-profesores-80-59`.

## Preguntas para la secretaria (no bloquean)

1. Qué son `RT12` y `RT34`.
2. La confirmación del parche de aulas.
3. Los tutores de los grupos de Bachillerato.
4. Qué espacios son `TAL3a`, `TAL3b`, `TAL41` y `A12`, y de qué tipo.
