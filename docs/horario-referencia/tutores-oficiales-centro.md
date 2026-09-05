# Tutores oficiales del centro — entrega de S137

FUENTE: jefatura de estudios del IES, entregada al arquitecto durante la Sesión 137
como respuesta a la petición del punto 1 de `docs/salvedades-demo.md` (ambigüedad
**A5** de `ESPECIFICACION-CATALOGO.md:530`, escrita en S115 y sin hacer durante
veinte sesiones — ver `D-preguntas-sin-cola`).

Este fichero existe porque el dato llegó por conversación y no por documento. Sin
sede en el repositorio no se puede auditar qué aplicó S137 ni contra qué. Es la
PROCEDENCIA de `tools/carga-centro/aplicar-tutores-oficiales.py`, que sólo lleva
dentro las 7 entradas que cambiaban.

La lista cubre los **28 grupos**: 23 ordinarios y 5 PDC. Se transcribe tal como se
entregó, con las etiquetas del centro y sin traducir a códigos de catálogo salvo
donde S137 lo verificó.

| Grupo (etiqueta del centro) | Tutor/es | Código |
|---|---|---|
| 1º ESO A | Jiménez Montes, María de los Ángeles | GH6 |
| 1º ESO B | Campanario Canales, Fco. Javier | MAT8 |
| 1º ESO C | García Quintana, Consuelo | LEN9 |
| 1º ESO D | García Granados, José Antonio | EFI2 |
| 2º ESO A | López Ruiz, Paloma | MAT5 |
| 2º ESO B | Ríos Palomo, María del Carmen | MAT1 |
| 2º ESO C | Torres Cubillo, Mariano José | GH2 |
| 3º ESO A | Gutiérrez Sánchez, Carlos Alberto | MAT6 |
| 3º ESO A PDC | Guerrero Serrano, Pilar / Gutiérrez Sánchez, Carlos Alberto | ORI1 / MAT6 |
| 3º ESO B | Afán Herencia, María Trinidad | BYG2 |
| 3º ESO B PDC | Afán Herencia, María Trinidad / Guerrero Serrano, Pilar | BYG2 / ORI1 |
| 3º ESO C | Crespo Saborido, Ana María | BYG3 |
| 3º ESO PDC | Crespo Saborido, Ana María / Guerrero Serrano, Pilar | BYG3 / ORI1 |
| 4º ESO A | Jiménez Morgaz, Elena María | ING6 |
| 4º ESO A PDC | Jiménez Morgaz, Elena María / Guerrero Serrano, Pilar | ING6 / ORI1 |
| 4º ESO B | Fedriani Freixinet, María del Mar | GH4 |
| 4º ESO C | García Rodríguez, Lina María | LEN6 |
| 4º ESO D | Pérez Reyes, Ramón | EFI3 |
| 4º ESO D PDC | Pérez Reyes, Ramón / Guerrero Serrano, Pilar | EFI3 / ORI1 |
| 1ºBACH A | Moreno Mesa, Juan Manuel | LEN7 |
| 1ºBACH B | Carbonell Coronado, Carmen | MAT7 |
| 1ºBACH C | García Gómez, Ana Belén | LEN2 |
| 1ºBACH D | Moreno Araujo, Fco. Javier | LEN1 |
| 2ºBACH A | Mesa Guarnido, Eva María | MAT2 |
| 2ºBACH B | Vela Montero, José Antonio | GH3 |
| 2ºBACH C | López Escudero, Raquel | GH5 |
| 1º FPB | Millán Erencia, Antonio | PAU2 |
| 2º FPB | Martínez Martínez, Enrique | PAU1 |

## Qué verificó S137 con esta lista

- **Cobertura:** 28 grupos exactos, ni sobra ni falta ninguno. La etiqueta
  `3º ESO PDC` (sin la C) coincide con la nomenclatura del catálogo, cuya
  correspondencia con 3ºC quedó cerrada en S115 por tres vías.
- **Contraste, que es lo que le da crédito:** casa **16/16** con el cruce que S136
  hizo contra `Horarios de profesores.pdf` en los grupos ordinarios de ESO y FPB,
  nombre a nombre y código a código. S137 amplía la comprobación al catálogo cargado
  y sube a **21/21**: los 5 PDC, cuyo tutor principal el proyecto heredaba del grupo
  padre por regla propia, casan también, así que la lista CONFIRMA esa regla de
  herencia, que hasta ahora era deducción nuestra sin respaldo del centro. Acierta en
  todo lo verificable por nuestra cuenta, y eso es lo que permite fiarse de las 7
  filas de Bachillerato, que no se pueden contrastar contra ninguna otra fuente.
- **Existencia:** los 7 códigos de Bachillerato (LEN7, MAT7, LEN2, LEN1, MAT2, GH3,
  GH5) figuran entre los 59 profesores del catálogo, así que la corrección se pudo
  aplicar sin dar de alta a nadie (`D-censo-profesores-80-59` no mordió).

## Qué se aplicó y qué NO

**APLICADO en S137:** las 7 tutorías de Bachillerato (1B-A…2B-C), que discrepaban
7 de 7 por contenido: FIL2 figuraba como principal de cinco grupos a la vez, y GH6
y EFI3 estaban duplicados con sus tutorías reales de ESO.

**NO APLICADO:** las **cinco co-tutorías de la orientadora ORI1** sobre los grupos
PDC, que esta lista respalda oficialmente. Quedan fuera por **R-terminado** —un
co-tutor ausente no es un dato falso, y está declarado en `docs/salvedades-demo.md`—
y porque añadirlas exige cambiar la forma del catálogo, el bucle de carga y la
prevalidación (`D-cargador-tutoria-pisa`).

**NO ES EVIDENCIA:** en `3º ESO A PDC` la lista pone a ORI1 delante y en las otras
cuatro detrás. Es formato, no indicación de rol. El tutor principal de los cinco PDC
se mantiene heredado del grupo padre, que es lo que la lista también recoge.
