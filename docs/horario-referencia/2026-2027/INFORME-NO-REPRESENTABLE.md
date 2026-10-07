# Lo que el horario oficial de 2026/2027 contiene y Educhronos no representa

Condición 5 de `O-carga-2026` (S199). Fuentes: los volcados de grupos y de
profesores de este directorio y sus PDF; análisis en
`/home/luis/educhronos-aceptacion/s199/f3/` (fuera del repositorio). Nada de lo
que lista la sección A entra en `catalogo-derivado.json`: sus 5 códigos y
`GrecB` aparecen 0 veces en él. Las leyendas citadas son las del PDF de
profesores, que las imprime truncadas.

## A. Contenido del horario oficial que el modelo no representa

| Qué | Códigos (leyenda) | Celdas | Ficha |
|---|---|---|---|
| Guardias lectivas | `G` «Guardia» (139), `Gbibl` «Gbiblioteca» (9), `conv` «R.Convivencia» (2) | 150 | `D-guardias-sin-modelo` |
| Guardias de recreo | `GR` «Guardia Recreo» (33, sin aula), `GRBib` «Guardia Recreo Bibliotec» (5, en `GrecB`) | 38 | `D-guardias-sin-modelo` |
| Co-tutor de las 5 PDC | segunda línea «Tutor:» de cada PDC (`ORI1`) | 5 tutorías | `D-cargador-tutoria-pisa` |

Las celdas sin grupo son 197 en 12 códigos, ninguna con aula: las 150 de
guardias de arriba y 47 de reuniones y funciones, que ya se representan (nota
al final de la sección); las de recreo, 38 de 13 profesores. `GrecB` es el
lugar de las guardias de recreo de biblioteca, no un aula de clase: no está en
ninguna leyenda ni entre las aulas del catálogo. El co-tutor sí lo representa
el modelo; lo pierde el cargador, que emite un PUT de reemplazo por entrada.

Desde S204 se representan: RED, RT12 y RT34 como actividades REUNION (una por
código, todos sus profesores en un tramo; RT12 y RT34 por hipótesis sin
fuente) y ORYCA, APSTE, FOREI, HUERT, PROAR y REYR como FUNCION (una por código
y profesor), sin aula; tipo de actividad desde S201 y cadena de carga en
9fcc2b3, 0672879 y 8b217c7. D-cargos-sin-modelo, cerrada en S203.

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
  clases con grupo. Ver `D-censo-profesores-80-59`. La 25 (Lobato Montes, su
  celda `RED`) entra como el profesor `PROV1`, código provisional
  (`profesor-lobato-provisional`, sin fuente); la 7 y la 32 se ignoran
  (`paginas-sin-codigo-ignoradas`).
- Nombres de `RT12` y `RT34`: su propio código, porque la leyenda solo lo
  repite (`nombres-rt-sin-fuente`, sin fuente).
- Los otros siete nombres son los literales de la leyenda «Asignaturas:» del
  PDF de profesores (`nombres-leyenda-profesores`; fuente:
  `/home/luis/educhronos-aceptacion/s200/m2/informe.md:485-493`). `RED`
  «Reunión de Equipo Direct» y `ORYCA` «Ordenación y Catalogació» llegan
  truncados: lo prueba el texto, cortado a mitad de palabra (`truncado: true`
  en `nombres-derivados.json`).

## Preguntas para la secretaria (no bloquean)

1. Qué son `RT12` y `RT34`.
2. La confirmación del parche de aulas.
3. Los tutores de los grupos de Bachillerato.
4. Qué espacios son `TAL3a`, `TAL3b`, `TAL41` y `A12`, y de qué tipo.
5. El código real de Lobato Montes (hoy `PROV1`, sin fuente).
6. El nombre completo de `RED` y de `ORYCA`.
