# Diseño de navegación — medición fundacional de O-navegación

Sesión S122. Tipo: medición y diseño. **No modifica ningún fichero de `app/`**: la
sesión sólo escribe en `docs/` y en `/tmp`.

Este documento es la MEDICIÓN QUE FUNDA **O-navegación**, objetivo propio decidido
por el arquitecto en esta sesión. No cuelga de O-diseño y no amplía su criterio: la
ficha de O-diseño (`gestion_proyecto.md:797-805`) deja expresamente FUERA de su
criterio, por R-terminado y en el M0 de S121, cuatro de las deudas que este diseño
toca —`D-configuracion-monolitica`, `D-selectores-sin-busqueda`,
`D-actividad-forma-implicita`, `D-vista-horario-sin-horario`— «porque si entran,
esto deja de ser acabado y pasa a ser rehacer la UI».

Razón escrita del arquitecto para el objetivo propio: esto toca enrutado,
paginación y densidad de rejilla, arrastra la decisión ruta-hija-vs-contenedor
aplazada desde S101 y previsiblemente rompe `centro-minimo.spec.ts`. **Un Cambio que
rompe un e2e de criterio no es un Cambio.** O-diseño queda intacto y su orden pasa a
ser: sistema (hecho) → **O-navegación** → C-identidad y C-revisión sobre la UI
definitiva.

Las dos fichas que ya remitían aquí sin sede a la que remitir:
`D-configuracion-monolitica` («se resuelve donde ya espera `D-pdc-lista-rancia`: en
el Cambio que decida ruta-hija-vs-contenedor, aplazado desde S101») y
`D-pdc-lista-rancia` («la decisión ruta-hija-vs-contenedor que S101 aplazó a Cambio
propio»).

---

## 0. Declaraciones NUEVAS de esta sesión

No son lecturas de la documentación. Se declaran aquí por primera vez y por eso van
antes que nada.

**D0-1 — Resolución objetivo: 1920×1080.** La ficha de `D-sin-puntos-de-ruptura` y el
criterio de O-diseño piden desde S121 que las vistas no se rompan «en UNA resolución
declarada, la de la demo» (`gestion_proyecto.md:804`), y **ese valor no estaba
declarado en ningún documento del repo**: se buscó y no aparece. El arquitecto lo fija
aquí en 1920×1080.

**D0-2 — La resolución del portátil queda SIN FIJAR, como parámetro.** No se inventa.
Todo el diseño de altura de esta sesión se deriva de una constante única
(`OBJETIVO.alto`), de modo que fijar la del portátil recalcula el reparto sin tocar
ninguna otra decisión. Lo que hace falta para cerrarlo: la resolución real del
portátil del jefe de estudios, que nadie ha medido.

**D0-3 — El responsive sigue fuera** (`D-sin-puntos-de-ruptura`, decisión escrita de
S121). Este diseño no añade un solo `@media`.

---

## 1. FASE A — Lo que se midió, con su evidencia

Base: `app/educhronos-demo-m4.db`, el único horario del centro real que existe
(declarada horario de referencia en S119). Abierta con `sqlite3 -readonly` y con
`mode=ro`; md5 `ea1a70a0337831dddccdbcd322f48e9b` **antes y después**, idéntico al
declarado en `plan_trabajo_horarios.md:957`. La base no se modificó.

### A1 — Las insignias `1` y `-1` son coste blando con signo, no un contador

Es la suma con signo de los deltas de penalización **de la INSTANCIA**, no de la celda
ni de sus entradas.

| Qué | Dónde |
|---|---|
| Se pinta por instancia, dentro del `@for` de instancias | `horario-grid.html:31-35` |
| «Suma con signo del coste blando de cada instancia, YA agregada por el contenedor: la rejilla PINTA el número, no lo calcula» | `horario-grid.ts:50-57` |
| Clave de cruce: `clavePin(actividadCodigo, indice)` | `horario-grid.ts:140-142` |
| Quien lo calcula: suma los `delta` y **descarta las claves cuya suma da 0** | `horario/diagnostico.ts:76-89` |
| Por eso el predicado es `has`, sin comparar con 0 | `horario-grid.ts:148-151` |
| Significado del signo: «`>0` mover la celda mejora, `<0` la celda tapa un hueco» | `models/diagnostico.model.ts:52-55` |
| Origen del dato: `GET /api/horarios/{id}/diagnostico` | `HorarioController.java`, consumido en `horario-view.ts:115-117` |

**Los tres datos observados por el arquitecto casan exactamente con el grupo 1B-A y
confirman el mecanismo por mutación de un solo factor.** Medido por SQL sobre la base:
Miércoles T1 = 2 sub-entradas, Jueves T1 = 2, Jueves T2 = 1. Dos celdas con el MISMO
número de entradas (Mié T1 y Jue T1), una con insignia y otra sin: la insignia no
puede depender del recuento. `1` significa que mover esa instancia mejoraría el
objetivo en 1; `-1`, que tapa un hueco y moverla lo empeoraría.

Advertencia del contrato, escrita dos veces en el código y que conviene no pisar: el
número **no es `Totales` y no tiene por qué cuadrar con él** —los `Totales` son
conteos SIN signo del coste actual y estos son deltas CONTRAFACTUALES con signo—;
«contrastarlos es la trampa de este contrato» (`models/diagnostico.model.ts:58-65`,
`horario/diagnostico.ts:64-67`).

### A2 — Configuración es un componente único con ocho listas apiladas; no hay rutas hijas

- `app.routes.ts:7-11`: tres rutas planas (`''`, `configuracion`, `horario/:id`).
  Ningún `children`; el único `<router-outlet>` es el raíz (`app.html:10`).
- `configuracion.html:1-11`: las ocho secciones son etiquetas hermanas en un `<section>`.
- El enrutado **no decide nada**. La elección está escrita y razonada en
  `configuracion.ts:12-19`: «el CRUD de profesores va INLINE aquí y no como ruta hija:
  no hay estado que anidar ni URL que enlazar, y evitarlo ahorra el `<router-outlet>` y
  el array `children` que el proyecto no usa en ningún sitio salvo el raíz».
- Marco: `configuracion.css:1-5`, `max-width: 60rem` centrado. Es el único `max-width`
  de una vista en todo el frontend.

### A3 — El selector «Vista» ya tiene las tres opciones

`horario-view.html:4-8`: `grupo`, `profesor` y `aula`, las tres presentes. Lo que no
existe es búsqueda: el segundo selector (`horario-view.html:13-17`) es un `<select>`
nativo que enumera `entidades()`.

Detalle que importa y que no es evidente: las opciones **se derivan de la proyección,
no del catálogo** —`entidadesDeVista(p.sesiones, vista())`, `horario-view.ts:131-134`—,
así que un profesor sin clases asignadas no aparece en su propio selector.

### A4 — No hay endpoint por grupo, pero el dato ya viaja y se compone en cliente

No existe `GET /api/grupos/{id}/subgrupos` ni equivalente: `GrupoController` sólo añade
`/{id}/tutoria`, y los subgrupos cuelgan de `/api/subgrupos` y de
`/api/grupos/{idPadre}/pdc`.

Lo que sí existe y basta:
- `SubgrupoDTO.java:10-21` — `grupos: List<String>`, los códigos de los grupos que pueblan el subgrupo.
- `PlazaDTO.java:11,25` — `subgrupos: List<String>`, embebido en `ActividadDTO.plazas`.

Componer «subgrupos de este grupo + actividades que los usan» es un filtro sobre dos
GET existentes, **sin escritura y sin endpoint nuevo**. Coste: traerse las listas
enteras (334 subgrupos, 208 actividades). Si algún día pesa, la alternativa es un
endpoint de composición, que ya sería backend.

### A5 — Lo que impedía que cupiera: densidad de celda × altura de fila libre

Medido, no supuesto:
- **No existe una sola regla de altura ni de `overflow` en la rejilla.** `grep` de
  `height|overflow` en todo el CSS de `src` devuelve: dos `line-height: 1` en adornos,
  el `line-height` global de `styles.css:83`, y `max-height: 80vh` + `overflow-y` sólo
  en `actividad-form.css:4-5` (el diálogo). **La tabla crece sin tope.**
- Cada sub-entrada son **cuatro líneas apiladas** —asignatura, profesores, aula,
  grupos— (`horario-grid.html:49-56`) en `flex-direction: column` (`horario-grid.css:96`).
- El ancho **no** era el problema: `.rejilla { width:100%; table-layout:fixed }` y
  `.app__contenido` sin `max-width`. En 1920 cada columna sale a ~374 px.
- Las cabeceras son baratas: `thead` una línea, `.esquina` 3rem.

Densidad real medida sobre el horario del centro, por vista:

| Vista | Máximo de sub-entradas en una celda | Peor caso |
|---|---|---|
| **Grupo** | **6** | 1ºA, jueves, T5 |
| Profesor | 1 | — |
| Aula | 1 | — |

**La densidad sólo se juega en la vista por grupo.** Las otras dos no esconden ningún
caso peor, y eso acota el riesgo de todo este diseño.

Distribución completa, medida sobre los 28 grupos y sus 791 celdas: 599 instancias de
una plaza, 83 de dos, 37 de tres, 22 de cuatro, 28 de cinco y **22 de seis**. La cola
larga es corta pero no es despreciable, y es la que gobierna D11.

---

## 2. Correcciones de premisa

Tres afirmaciones del encargo que la medición desmintió. Se registran porque M2 lo
exige y porque dos de ellas cambian contra qué hay que validar.

**C-1 — 1B-A tiene celdas de CINCO sub-entradas, no de cuatro.** Cuatro celdas con 5
(Martes T2, Miércoles T4, Miércoles T5, Viernes T2). Todas son la misma forma: el
bloque de destinos alternativos de 1º de Bachillerato
`Bloque-CE_DTec_Lab_Pat_TEst2-1BACH`, **una sola instancia con cinco plazas** (CE,
DTec, Lab, Pat, TEst2). Agravante de ancho no previsto: cada una de esas cinco pinta
en su cuarta línea `1B-A, 1B-B, 1B-C, 1B-D`, porque `grupos` es «la unión sin
duplicados de los grupos de todos los subgrupos de la plaza» (`SesionVistaDTO.java:11-16`).
La celda más densa es también la de texto más largo.

**C-2 — El peor caso del centro NO está en 1B-A, está en 1ºA.** El encargo descartaba
1ºA por fácil («1B-A tiene celdas con CUATRO entradas simultáneas; un diseño validado
contra 1ºA no vale»). Medido: 1ºA tiene una celda de **seis** (Jueves T5) y 1B-A se
queda en cinco. El instinto era correcto —validar contra el grupo fácil no vale— pero
el grupo fácil era el elegido. **El diseño se validó contra los dos.**

**C-3 — `D-generacion-sin-indicador` está CERRADA, no viva.** La pagó S118 y su ficha
lo dice: «PAGADA Y CERRADA EN S118, de paso». Dejó tras de sí
`D-presupuesto-anunciado-espejo`. El marcado actual es deliberadamente mínimo y sin
estilo por R-invalidación (`horario-view.html:30-37`): «O-diseño rehará el aspecto, no
el estado».

---

## 3. Hallazgos de producto

**H-1 — La insignia lleva información de ajuste y se pinta sin una sola palabra de
explicación.** El `<span class="badge">` de `horario-grid.html:34` no tiene `title` ni
`aria-label`, a diferencia del candado, que sí los lleva (`horario-grid.html:40-41`).
Un usuario ve un `-1` en la esquina de una celda y no tiene forma de saber que
significa «esta clase está tapando un hueco». Es el dato más denso de la rejilla y el
único sin rótulo.

Esto **resuelve la pregunta que S121 dejó abierta** sobre qué es esa señal, y la
convierte de dato de diseño en hallazgo de producto. En esta sesión se ha RESERVADO su
sitio y su explicación —tooltip y etiqueta accesible, como el candado— y **no se ha
implementado**: es material de **C-identidad**, dentro de O-diseño.

**H-2 — El espaciado NO está tokenizado, y por eso `padding-top: 16px` sobrevivió a
C-sustitución.** `horario-grid.css:43-45` conserva un literal de 16 px porque
C-sustitución (S121) sustituyó **color y `font-size`**, que es exactamente lo que pide
el criterio 1 de O-diseño («ningún CSS de componente contiene un color literal ni un
`font-size` literal»), y el espaciado quedó fuera de esa verificación binaria. Los
tokens `--e1..--e6` existen en `styles.css:63-68` pero no gobiernan el CSS de
componente. **Esto afecta a la decisión 3 de las seis de identidad de O-diseño
—densidad/espaciado—, que sigue sin sede.** No se paga aquí.

**H-3 — El recreo es invisible en la vista de horario, y no por olvido de la vista.**
La proyección numera los tramos 1..6 con recreos **excluidos**: «`tramo` es el
ordenEnDia 1..6 (recreos excluidos)» (`SesionVistaDTO.java:11-12`). El horario real del
centro tiene **7 tramos por día**, de los cuales el 4º es recreo, uniforme en los cinco
días (medido: `es_lectivo = 0` en los órdenes 4, 11, 18, 25 y 32). Un horario de
instituto sin recreo no se parece al que el centro tiene colgado en la pared.

El dato ya viaja y no hace falta tocar el backend: `TramoJornadaDTO.java:13-25` expone
`esLectivo` y `ordenEnDia` **nullable a propósito** («vale null en los tramos no
lectivos. Un recreo no tiene sitio en la numeración 1..6»), servido por
`GET /api/jornada`.

---

## 4. Decisiones de diseño

Una por bloque, con la alternativa descartada y su razón. **Ninguna introduce una
escritura nueva**: toda composición añadida es de solo lectura, conforme a la
invariante del encargo y a la ficha de O-particiones.

### Bloque rejilla

**D1 — La altura de fila es CONSTANTE y se deriva del presupuesto, no se elige.**
Se mide lo que gastan barra, cabecera de vista, `thead` y fila de recreo, y el resto se
reparte entre los seis tramos. Cambiar `OBJETIVO.alto` (D0-2, el portátil) recalcula el
diseño entero sin tocar nada más.
*Descartado:* altura fija en píxeles escrita a mano. Habría que reajustarla a ojo en
cada cambio de cromo y volvería a romperse en la primera resolución distinta.

**D2 — La altura se impone con un envoltorio `max-height` DENTRO de la celda, nunca
desde `tr` o `td`.** En una tabla, `height` sobre fila o celda es un **mínimo**: el
contenido la estira igualmente. Es la trampa que hizo fallar la medición de esta misma
sesión (§6).
*Descartado:* `height` en `tr` con `overflow:hidden` en `td`. No recorta nada, porque
el `td` crece.

**D3 — La sub-entrada baja de cuatro líneas a dos**: `ASIGNATURA … aula` /
`profesores … marca`. Es la palanca que más altura devuelve y se exprimió antes de
tocar ninguna otra.
*Descartado:* mantener cuatro líneas y compensar con colapso. Habría convertido el
colapso en la interacción principal de la vista en vez de un caso excepcional.

**D4 — Un bloque se pinta como UNA unidad: una línea por plaza bajo un rótulo común**
(«5 simultáneas», con el código completo de la actividad en el `title`, porque
`Bloque-CE_DTec_Lab_Pat_TEst2-1BACH` no cabe ni debe). El rótulo va en la banda que el
badge ya reservaba, así que no cuesta un píxel nuevo. Es además lo que dice el modelo:
un bloque es una instancia con N plazas, no N clases sueltas, y es la unidad
arrastrable (`horario-grid.ts:24-27`).
*Descartado:* repetir la estructura de dos líneas N veces. Multiplica por N el caso
peor, que es justo donde el diseño se juega.

**D5 — `.entrada` se CONSERVA como clase base y `--fila` es un modificador.**
Decisión tomada por sus consecuencias en los tests, no por estética: `horario-grid.spec.ts:259`
y `:290` cuentan `.entrada` dentro de una instancia, y ambos casos usan instancias de
DOS sub-entradas, que en este diseño son modo bloque.
*Descartado:* sustituir la clase por `.entrada--fila`. Rompería dos tests que este
cambio no tiene ninguna necesidad de tocar.

**D6 — La cuarta línea se condensa en una marca `+N` con el detalle en el `title`; no
se elimina.** Medido sobre 1B-A: `grupos` vale exactamente `["1B-A"]` en **23 de las 51**
sub-entradas, es decir, en la vista por grupo repite el grupo que ya estás mirando casi
la mitad de las veces. En las otras 28 sí informa (plazas compartidas con 1B-B/C/D) y
ahí se conserva como marca consultable.
*Descartado:* borrar la línea. Habría eliminado una señal existente, que la invariante
del encargo prohíbe.

**D7 — El recreo se pinta como fila propia, estrecha, entre T3 y T4** (H-3), por
composición de solo lectura con `GET /api/jornada`.
*Descartado:* dejarlo fuera. Es la primera cosa que el arquitecto echó en falta al ver
el horario real, y su ausencia no es un detalle estético: cambia el parecido con el
horario que el centro usa.

**D8 — El recreo se pinta SIN hora de reloj.** En `tramo_semanal` las horas son enteros
(`25200000`) cuya zona no se ha verificado. Convertirlos daría «7:00», y con un
desplazamiento de zona plausible daría «8:00» o «9:00». **Hueco declarado, no
rellenado.** Lo que falta para cerrarlo: comprobar cómo interpreta la zona el dialecto
de comunidad al leer `LocalTime`, o simplemente leer la jornada por la API con el
backend en marcha.

**D9 — El título de la vista y los controles van en UNA sola fila.** El `<h2>` gastaba
su línea a 1.5em más dos márgenes de 0.83em, y los controles otra banda con su
`margin-bottom`: dos franjas horizontales para seis palabras y tres controles. Fundidas,
el presupuesto recupera esa banda entera y D1 la reparte sola entre los seis tramos.
Fue lo que eliminó el último resto de scroll en 1ºA.
*Descartado:* dejarlo para más adelante. Se difirió y hubo que traerlo: era el alto que
faltaba.

**D10 — En la propuesta el título NO repite el grupo**, porque lo dice el selector, que
ahora está en la misma fila.
*Descartado:* `Horario 1 — grupo: 1ºA` junto a un selector que pone `1ºA`. Gasta ancho
para decir dos veces lo mismo a diez centímetros.

**D11 — El mecanismo de expansión es OBLIGATORIO.** La decisión quedó condicionada,
por el arquitecto, a que el número de celdas recortadas fuese CERO en todo el centro.
**No lo es: son 22 de 791**, así que D11 se invierte y el mecanismo se construye.

Calculado, no observado (§6). Sobre los 28 grupos del centro y sus 791 celdas:

| Plazas de la instancia | Instancias | Alto que la celda necesita |
|---|---|---|
| 1 | 599 | 62,9 px |
| 2 | 83 | 67,4 px |
| 3 | 37 | 89,1 px |
| 4 | 22 | 110,8 px |
| 5 | 28 | 132,5 px |
| **6** | **22** | **154,2 px** |

Las 22 celdas que se recortan son exactamente las de **seis plazas**, y **no son una
rareza de un grupo**: están repartidas en **11 grupos de 28** —1ºA-D, 3ºA-C, 4ºA-D—, a
dos celdas por grupo, y las produce un patrón sistemático del centro: cinco bloques de
optativas con seis destinos (`Bloque-CyR_OyD_RefMt-1ESO` en 8,
`Bloque-AFAVS_CeH_DT_RefLe/RefMt-4º` en 4 cada uno,
`Bloque-BioNu_CyR_RefLe/RefMt-3º` en 3 cada uno).

**Y no se arregla con más presupuesto.** Para cero recortes el alto de fila debe ser
154,2 px, lo que da una tabla de 979,8 px y deja un **cromo máximo admisible de 96,2 px**
para barra, cabecera de vista y el cromo del propio navegador juntos. Sólo la barra
superior mide ~56 px y el cromo del navegador ronda los 100: **es inalcanzable**. El
número de recortes es 22 en todo el rango de cromo probado (100–220 px), es decir, no
depende del ajuste fino.

**Corrección de una inferencia falsa de la Fase B, que el arquitecto detectó:** que 1ºA
«cupiera sin scroll» no probaba que cupiese. Cabía **porque el prototipo la recortaba**
—`.celda` lleva `overflow:hidden`—, y quedarse con el resultado del mecanismo mientras
se retira el mecanismo no se sostiene. El scroll desapareció por el recorte, no por el
ajuste.

*Alternativa descartada:* recortar sin expansión. Es **pérdida de información, no
densidad**: el horario ocultaría dos clases reales por grupo en 11 grupos, sin decir
cuáles y sin forma de verlas. Es la misma familia que la insignia sin leyenda de §3-H-1,
y en un horario de centro es peor: ahí lo que se pierde no es una explicación, es una
clase.

*Lo que el implementador puede explorar antes de dar por buena la geometría*, y que esta
sesión no decide porque cada opción retira algo: la banda del badge cuesta 16 px fijos
por instancia; el `padding` y el `margin-bottom` de cada plaza de bloque cuestan ~4,2 px
por plaza (25 px en una celda de seis); bajar `.asig` de `--tam-s` a `--tam-xs` en modo
bloque ahorra 15 px más. Cualquiera de las tres, o su suma, baja de 154,2 px. Ninguna se
aplica aquí sin decidir qué señal se sacrifica.

Nota de proporción, porque cambia qué clase de mecanismo hace falta: 22 de 791 es el
**2,8 %** de las celdas del centro, y en los grupos afectados son 2 de 30. El colapso
sigue siendo el **caso excepcional** que el arquitecto quería y no la interacción
principal de la vista; lo que no puede es no existir.

### Correcciones de S126, al implementar el tramo 1

Nada de lo anterior se borra: se fecha. La tabla de alturas por plaza (62,9 … 154,2 px)
sale VALIDADA del contraste con el navegador —cinco tamaños medidos sobre cientos de
celdas, ninguna desviación mayor de 1 px—. Lo que caducó es el presupuesto contra el que
se comparaban, y estas cinco cosas:

1. **El «22 de 791 / 2,8 %» de la nota de proporción se midió a 1080 px de pantalla y 135
   de cromo.** A viewport real (1920×887, Firefox) las 28 celdas de cinco plazas caen
   también: 50 de 791, el 6,3 %. Y con la geometría YA IMPLEMENTADA el hueco real de la
   rejilla resultó ser 716 px, con lo que entran además las 22 de cuatro plazas: **72 de
   791, el 9,1 %**. D11 no cambia de sentido —sigue siendo obligatorio— pero el caso
   excepcional pasa del 2,8 % al 9,1 %, y en los grupos afectados ya no son 2 de 30.
2. **D3 lleva dos cambios y sólo nombra uno.** Además de bajar de cuatro líneas a dos, su
   geometría medida exige `line-height: var(--lh-apretado)` y `--tam-xs` en la línea 2.
   Heredando el 1.5 del `body`, la celda de cuatro plazas nace en 124,8 px en vez de 110,8
   y el acantilado se cruza sin que nadie lo note. Quien lea D3 sin correr el instrumento
   implementa la mitad.
3. **D9 admite dos direcciones de fusión y una rompe el producto.** El texto no dice cuál.
   Medido por mutación en S126: bajar los controles a la rama `@else if (proyeccion())`
   tira el caso (40) de `horario-view.spec.ts` y deja al usuario sin botón «Generar» en el
   arranque real —base sin horario, proyección en 404—, que es exactamente donde hace
   falta. **Se implementa subiendo el `<h2>` a la fila de controles, nunca al revés.**
4. **D8 conserva su decisión y cambia su razón.** El hueco declarado era «la zona de los
   enteros de `tramo_semanal` no se ha verificado». S126 la verificó levantando el backend:
   la API devuelve `08:00`–`14:30` con recreo de 11:00 a 11:30, correcto. Pero esa
   corrección depende de la zona del proceso del servidor, así que en otro despliegue sería
   falsa. Sigue sin pintarse la hora, por una razón distinta y registrada como
   `D-jornada-zona-servidor`.
5. **D2 describe un `overflow: hidden` que no existía en el producto.** Era de la maqueta
   v8. Hoy el `td` crece y la página hace scroll: no había recorte silencioso que retirar,
   había uno que nunca se puso. D2 es trabajo NUEVO, no modificación.

Y una decisión que el bloque no contemplaba, tomada en S126 dentro de D6: la marca `+N`
**sólo se aplica cuando hay grupo implícito**. El argumento de D6 —que `grupos` repite el
grupo que ya estás mirando— sólo vale en la vista por grupo; en las de profesor y aula no
hay grupo implícito y condensar la lista retiraría la única señal que lo nombra. La rejilla
recibe `grupoActual`, con defecto `null`, y con `null` pinta la lista entera.

### Decisiones de S127, al implementar el tramo 2 (D11)

El tramo 1 dejó el repositorio ocultando 72 celdas de 791 sin marca y sin forma de
verlas. Esto lo cierra. **D11 decidía que el mecanismo era OBLIGATORIO y no decía cuál
era**, y §5 tampoco lo listaba como pendiente: era una costura del documento, y se cierra
aquí escribiéndola.

**D11-a — El mecanismo es una MARCA `+N` con el detalle en el `title`, no una expansión.**
Lo que el criterio 4 exige es «colapsada Y con forma visible de expandirlas», y la palabra
que manda es *visible*: lo que no puede pasar es que el recorte sea mudo. La marca es la
parte visible; el `title` es la forma de verlas.
Y no se estrena patrón: **D4 y D6 ya establecieron esta convención**. D4 condensa el código
de la actividad y lo recupera por `title`; D6 condensa la lista de grupos en `+N` y la
recupera por `title`. D11 condensa unas plazas y hace lo mismo. Medido al implementar: en
el frontend no hay un solo `<details>`, ni popover propio, ni tooltip propio; lo único que
existe es `title` nativo, en esos dos sitios y los dos como binding.
*Descartado — expansión en sitio:* en una tabla, crecer una celda estira su fila y reflota
la rejilla; el desbordamiento devuelve el scroll vertical que el tramo 1 acaba de quitar.
Gastar el tramo 2 en reintroducir el síntoma del tramo 1 no se sostiene.
*Descartado — overlay del CDK:* técnicamente barato (el CDK ya está) pero es un patrón
nuevo en el repo para lo que el propio diseño llama el caso excepcional. Queda como salida
si algún día se mide que el `title` no basta.
*Descartado — `MatDialog`:* existe y se usa, pero es modal. Para asomarse a dos clases
taparía el horario que se está mirando.

**D11-b — La marca vive DENTRO de la banda del rótulo de D4, y eso no es coherencia
estética: es lo que impide el bucle.** `.instancia.bloque` reserva `padding-top: 16px` y
`.rotulo` es `position: absolute` dentro de esa banda, así que escribir ahí **no ocupa un
píxel de alto**. Importa porque la medición que produce la marca depende del alto
disponible: si la marca ocupara alto, aparecería → empujaría el contenido → la última plaza
dejaría de estar oculta → la marca desaparecería → volvería a estar oculta. Es una
OSCILACIÓN entre valores distintos, y la guarda de igualdad que corta el bucle de
`repartirAltura` no la detendría, porque esa guarda sólo impide reescribir el MISMO valor.
La marca lleva `title` PROPIO y no reutiliza el de `.rotulo`: ese `title` es el único sitio
donde se lee `Bloque-CE_DTec_Lab_Pat_TEst2-1BACH` entero, y ocuparlo sería retirar una
señal existente, que es lo que la invariante del encargo prohíbe.

**D11-c — La marca NO se dispara por número de plazas, sino por desbordamiento medido con
una FRACCIÓN: una plaza cuenta como oculta si se ve menos de la mitad.** Es la corrección
que la corrección 1 de S126 hacía inevitable y que nadie había sacado. Con la fila en 109,57
px el déficit no es homogéneo: la celda de cuatro plazas pide 110,8 y **se pasa 1,23 px**
—se ve el 94 % de su última línea—, la de cinco esconde una línea entera (21,7 px) y la de
seis, dos. Marcar las 72 pondría una señal que miente en 22 celdas, justo al lado de la
marca `+N` verdadera de D6; es la familia de `D-vacio-miente-con-error`.
**Marcadas: 50 de 791.** Las 28 de cinco plazas y las 22 de seis. Que el número coincida con
la predicción de S125 es casualidad aritmética, no confirmación: aquel 50 salía de otro
reparto de altura.
La regla es una fracción y no un umbral en píxeles por la misma razón por la que D1 deriva
la altura en vez de escribirla: una constante literal hay que reajustarla a mano en cuanto
cambien tipografía o padding, y por ahí ya se cruzó el acantilado una vez.

**D11-d — La medición corre en la fase `read` de `afterRenderEffect`, NO colgada del
`ResizeObserver` que ya existía.** El observador sólo despierta con cambios de TAMAÑO, y su
javadoc declara —correctamente— que no hay bucle porque el reparto no depende de ninguna
medida de la tabla. La marca sí depende del contenido: cambiar de grupo repinta la rejilla
sin mover un píxel (`table-layout: fixed`, seis filas), el observador no se dispara y las
marcas del grupo anterior sobrevivirían al cambio. Estrena patrón en el repo —no había
ningún `afterRenderEffect` ni `effect()` en producción— y se asume: reutilizar el observador
habría sido reutilizar el disparador equivocado.
Efecto lateral que obligó a una línea más: `repartirAltura` publica `--alto-celda` con
`setProperty` desde fuera del ciclo de render, así que fijarlo no agenda ninguna pasada. Sin
un espejo en señal del tope, en el primer pintado se mide la celda ANTES de que tenga tope y
no se marca nada. El espejo es DISPARADOR, no fuente de verdad: el estilo lo sigue
escribiendo `setProperty`.

**LIMITACIÓN DECLARADA, no descubierta después: sólo se marca lo que tiene banda** —modo
bloque o badge—. Una instancia de una plaza no la tiene, y dársela cambiaría su altura, que
es exactamente la realimentación que D11-b evita. Hoy no muerde: una celda de una plaza mide
62,9 px contra ~110 disponibles y no se recorta nunca. Es propiedad de estos datos, no del
modelo. Lo mismo vale para el supuesto «celda ≡ instancia»: medido sobre el volcado, **las
791 celdas del centro tienen exactamente una instancia cada una**, pero `agruparPorActividad`
devuelve una lista y `slotsOcupados` cuenta instancias precisamente porque puede haber
varias. La implementación mide por instancia contra su celda, que funciona igual si algún día
hay dos apiladas; lo que se rompería entonces es que la banda de la segunda caiga dentro de
la zona recortada, y ahí la marca estaría tan oculta como lo que anuncia.

**VERIFICADO EN NAVEGADOR (M4 de S127), en la superficie del criterio 4:** Firefox del
arquitecto maximizado, viewport 1920×887, `devicePixelRatio` 1, hueco 716 px, `--alto-celda`
101 px. Recuentos contra el cálculo del volcado, y cuadran: **1B-A 4, 4ºA 3 —dos `+2` de seis
plazas y un `+1` de cinco—, 2B-B 0 sobre seis celdas de cuatro plazas.** 4ºA es el caso que
decide, porque sus tres celdas marcadas y sus tres sin marcar conviven en la misma pantalla.
Al cambiar de grupo y volver, las marcas siguen al grupo pintado. **Consola sin un solo aviso
de `ResizeObserver loop`**, que es exactamente la forma en que se habría manifestado la
realimentación que D11-b descarta por construcción.

**Y UNA CONFIRMACIÓN QUE VALE POR SÍ SOLA: con el aviso «1 pines sin aplicar» en pantalla, la
celda de seis pasa a `+3` y el scroll NO reaparece** (medido en 1ºA). Esos son los 62 px que
S126 midió, absorbidos en caliente. Es la prueba de que el reparto en runtime de D1 compró
algo real: con la fórmula de constantes que S126 retiró dos veces, aquí habría vuelto la barra
de scroll.

**Dos pendientes de S126 cerrados de paso, porque el M4 los tenía delante:** el hueco de 716
px queda confirmado en el navegador del arquitecto y no sólo en el de Playwright —los 32 px
de diferencia contra los 748 calculados siguen SIN EXPLICAR, pero ya no cabe atribuirlos al
entorno de prueba—; **CAUSA CANDIDATA HALLADA EN S128, y NO se corrige: `.app__contenido`
declara `padding: 1rem 0` en `app.css`, es decir 16 + 16 = 32 px exactos, en el CONTENEDOR DE LA
VISTA —el elemento que ni S126 ni S127 miraron, por estar los dos midiendo dentro de
`horario-grid`—. Coincide al píxel con la diferencia, pero se declara CANDIDATA y no probada por
una razón honesta: la derivación de los 748 no está escrita en ningún documento del repo (el plan
dice «los 748 que el arquitecto había calculado»), así que la hipótesis no se puede confirmar ni
refutar contra documentación, y reconstruirla desde el viewport no cuadra. Nace
`D-748-sin-derivacion`. Los 32 px NO se reclaman, por R-terminado: devolverlos subiría
`--alto-celda` ~4,5 px y cambiaría el recorte de un criterio ya cumplido sin que ningún criterio
lo pida. Lo que S128 aporta es que dejan de ser «32 px sin explicar» y pasan a ser «32 px con
causa candidata, escrita en el fichero donde viven»**; y el **arrastre sobre celda VACÍA funciona**, verificado en 1FPB, que es
uno de los dos únicos grupos del centro con huecos: las 49 celdas libres de las 840 posibles
están todas en 1FPB (24) y 2FPB (25), y ninguno de los dos tiene una sola celda de cinco o
seis plazas, así que no existe un grupo donde verificar marcas y hueco de una sola pasada.

### Bloque configuración

**D12 — Configuración se navega por destinos, con RUTAS HIJAS y `<router-outlet>`.**
Decisión del arquitecto, y es la que S101 aplazó. Se compra: una URL enlazable por
destino (`/configuracion/subgrupos`), el botón Atrás del navegador, y que cada lista se
monte y cargue sola al entrar.
*Descartado:* contenedor con `signal` y sin URL. Más barato y menos invasivo con el
e2e, pero no hay enlace que mandar ni Atrás que pulsar, y el estado se pierde al
recargar.
*Descartado también:* rutas hijas con `loadComponent` diferido. Encaja con
`D-bundle-presupuesto`, pero el beneficio hay que medirlo antes de prometerlo y no se
ha medido.

**D13 — `D-pdc-lista-rancia` se resuelve por construcción, no con un parche.** Entrar en
un destino es el momento natural de cargar su lista, así que la lista de subgrupos ya
no puede ignorar el alta de un PDC hecha desde grupos. Su ficha desaconseja
expresamente la alternativa: «no se parchea con un `EventEmitter` ad hoc, que fijaría
el molde por la puerta de atrás».

**D14 — El índice de destinos es VERTICAL y se genera de una lista de destinos.**
El noveno destino es empujar un elemento más, sin tocar el contenedor ni los ocho
anteriores, que es lo que la decisión del arquitecto exigía poder hacer.
*Descartado:* pestañas horizontales. Ocho ya van justas y nueve obligan a repartir en
dos filas o a hacer scroll lateral: exactamente la reforma que la decisión prohíbe.

**D15 — `/configuracion` a secas redirige al primer destino, Jornada.** Se apoya en un
argumento que ya estaba escrito para el orden de la página (`configuracion.ts:41-53`):
la jornada va primera porque la malla horaria es el marco de todo lo demás y su badge
de «propuesta · sin guardar» avisa de que el centro no la ha configurado antes de que
el usuario baje a las entidades.
*Descartado:* una portada de configuración sin destino elegido. Es una ruta más y una
pantalla más que mantener, para ahorrar una redirección.

**D16 — La lista se desplaza DENTRO de su destino**, con la cabecera —contador, filtro y
botón de alta— fija. La página no hace scroll. Con 334 subgrupos es la diferencia entre
una lista usable y un scroll de página de varios miles de píxeles
(`D-configuracion-monolitica`).

**D17 — Jornada no se disfraza de lista.** Es un singleton (`configuracion.ts:33-39`) y
su destino es el editor de la malla, no una tabla con alta y borrado. Que uno de los
ocho no tenga la forma de los otros siete y el índice no se resienta era parte de lo
que había que comprobar, y se comprobó.

### Bloque barra superior

**D18 — La barra reserva el sitio del selector de curso activo de la Fase 10**, rotulado
como reserva y sin funcionar, para no rehacerla entonces.
*Descartado:* añadirlo cuando llegue la Fase 10. Es la razón por la que la reserva se
pidió: el ancho de la barra se reparte una vez.

---

## 5. Lo que queda SIN DECIDIR

| Sin decidir | Qué haría falta para decidirlo |
|---|---|
| ~~Qué señal se sacrifica para bajar de 154,2 px de celda (D11)~~ **RESUELTO: ninguna** | Decidido en `gestion_proyecto.md` §4 y RATIFICADO en S125. Las tres palancas eran dos (la del badge choca con D4, S123). A 835 px de contenido faltan 18,6 px para salvar las celdas de cinco y las dos palancas disponibles (~25,2 + 15 px) SÍ llegarían —a diferencia del portátil de S123, donde faltaban 10,27 px incluso con las tres—, y aun así NO se aplican: retiran señales existentes (separación entre plazas simultáneas, jerarquía tipográfica en la celda más densa), que es lo que la invariante del encargo prohíbe y por lo que D6 rechazó borrar la cuarta línea. Gastarlas para bajar de 6,3 % a 2,8 % persigue un número que el criterio no pide (R-terminado). Quedan como holgura medida |
| ~~Resolución del portátil (D0-2)~~ **CERRADO** | Medido en S123: 1280×585 con escala de Windows al 150 %, `devicePixelRatio` 1.5. EXCLUIDO del criterio 4: la demo no se enseña ahí. D0-2 deja de ser «parámetro sin fijar». S125 añade que la superficie de verificación tampoco es el sobremesa del centro sino el equipo de desarrollo (1920×887, Firefox), por ser el peor caso disponible y por tanto un suelo |
| ~~Qué FORMA tiene el mecanismo de expansión (D11)~~ **RESUELTO en S127: marca `+N` con el detalle en el `title`** | Costura del documento, registrada al cerrarla: D11 decidía que el mecanismo era obligatorio y NO decía cuál era, y esta tabla no lo listaba como pendiente, así que la única decisión de bulto del tramo 2 no tenía sede escrita. Se decide en «Decisiones de S127» de §4, por coherencia con la convención que D4 y D6 ya habían establecido y porque cualquier mecanismo que ocupe alto realimenta la medición que lo produce |
| Hora de reloj del recreo (D8) | Verificar la interpretación de zona de `LocalTime` en el dialecto de comunidad, o leer `GET /api/jornada` con el backend en marcha |
| Badges y violaciones reales sobre 1B-A/1ºA | No están en la base: los calcula `GET /api/horarios/1/diagnostico`. La maqueta reservó su sitio y NO inventó valores. Hace falta levantar el backend contra una copia de la base |
| Búsqueda en los selectores de formulario (`D-selectores-sin-busqueda`) | Sigue viva y fuera de este diseño. El filtro de D16 es de la LISTA de un destino, no de los `<select multiple>` de los formularios, que es lo que esa deuda nombra |
| Orden título/controles en la cabecera (D9) | Preferencia del arquitecto. Se eligió título a la izquierda por coherencia con la reserva de curso, que va a la derecha. Es un intercambio de una línea |
| Sede de la decisión 3 de identidad de O-diseño (densidad/espaciado, H-2) | Decisión del arquitecto: o entra en C-identidad, o se tokeniza el espaciado en un Cambio propio |

---

## 6. Lección de método de esta sesión

**El mismo error apareció TRES veces**, y dos de ellas fueron dentro del acto de
corregir la anterior. Por eso esta sección no es una anécdota de sesión.

**Episodio 1 — el contador que no podía saltar.** El contador de celdas recortadas dio
0 durante dos iteraciones sobre un caso que sí desbordaba. Lo cazó el ojo del arquitecto
viendo aparecer un scroll vertical, no el instrumento. Causa: comparaba `scrollHeight`
con `clientHeight` sobre el `<td>`, y un `<td>` crece con su contenido, así que **jamás
podía desbordar**. El medidor sólo sabía devolver «todo bien».

**Episodio 2 — la anotación no es el test.** Para dar la cifra de `solver` se contaron
las anotaciones `@Test`/`@ParameterizedTest` del fuente: 97, frente a las 91 de la
referencia, y se registró como «discrepancia a contrastar». No había discrepancia: un
`@ParameterizedTest` es una anotación y varios tests, y un `@Disabled` es una anotación
y ninguno. Ejecutada la suite, son **91**, y la referencia era correcta.

**Episodio 3 — el directorio no es la corrida.** Para dar la cifra «ejecutada» de `app`
se sumaron los `TEST-*.xml` de `app/target/surefire-reports`. Total: 283, publicado como
«una más que la referencia». Falso: 41 informes eran de la corrida de ese día y **el
42.º era un huérfano del 24 de agosto**, `BarridoPresupuestoS117` —un arnés desechable de
S117, nunca trackeado en git, cuyo fuente ya no existe—, que seguía en `target/` con su
caso de 4306 s. `app` son **282**, y la referencia también era correcta. Se comprobó la
fecha de dos ficheros con un `tail -2` y se dio por buena la del resto.

**La forma común, que es lo que hay que llevarse.** En los tres, el instrumento se apoyó
en algo que **se parece al conjunto medido y no lo es**: el `<td>` se parece a la caja
acotada, la anotación se parece al test, el directorio se parece a la corrida. Y en los
tres el error era invisible desde dentro, porque el instrumento devolvía un número
plausible. De ahí las dos reglas:

1. En una tabla, `height` sobre `tr` o `td` es un mínimo. Para acotar de verdad hace
   falta un envoltorio con `max-height` (D2). Y `target/` no se limpia entre sesiones:
   filtrar por fecha, o `mvn clean test`, no es una precaución sino un requisito.
2. **Un medidor que sólo puede devolver un resultado no es un medidor.** Antes de
   apoyarse en un contador hay que forzarle un caso que DEBA hacerlo saltar: es la
   campaña de mutación de M3 aplicada al instrumento de medida.

**Cómo se cerró el hueco de D11, ya con la regla aplicada.** Por CÁLCULO y no por
observación: un contador que deriva la altura de las reglas CSS decididas y de los datos
reales de los 28 grupos (`scripts/calcular-recortes.py`), sometido ANTES a cinco
mutaciones que debía superar —alto de 1 px: todas recortadas; alto de 10 000 px:
ninguna; una celda sintética de doce plazas: recortada; monotonía al bajar el alto;
`alto(6) > alto(5) > alto(1)`—. Las cinco saltaron, y sólo entonces se leyó el número.

**El resultado fue 22 de 791, no 0** (D11). El contador de DOM no se equivocó sólo en
1ºA por casualidad: habría dado cero sobre un centro con 22 celdas recortadas en 11
grupos. La observación en navegador habría confirmado un diseño que pierde clases.

Nota sobre el determinismo de ese cálculo: todas las alturas son exactas por
construcción —salen de `line-height` NUMÉRICO por `font-size`, un múltiplo del tamaño
independiente de la fuente, y de paddings en `rem`— y no hay ajuste de línea, porque
`.ln` y `.entrada--fila` llevan `white-space: nowrap`. La única magnitud no calculable
es el cromo de la vista, que depende de la altura que el navegador dé a `<select>` y
`<button>` nativos: por eso entra como parámetro y se barre en un rango, en vez de
fingir un número.

---

## 7. Qué hay que medir o rehacer al aplicarlo

### Componentes afectados

| Fichero | Qué le pasa |
|---|---|
| `app.routes.ts` | Estrena `children` bajo `configuracion` + `redirectTo` a `jornada` (D12, D15) |
| `configuracion.html` / `.ts` | Deja de componer ocho hermanas y pasa a índice + `<router-outlet>`. Su javadoc (`:12-19`) argumenta lo contrario de lo que se va a hacer y hay que reescribirlo, no borrarlo: la razón vieja era válida con ocho listas y una pantalla |
| Las ocho listas (`profesor-lista`, `aula-lista`, `asignatura-lista`, `nivel-lista`, `grupo-lista`, `subgrupo-lista`, `actividad-lista`, `jornada`) | Pasan de hijas embebidas a componentes de ruta. Cada una carga al entrar (D13) |
| `horario-grid.html` / `.css` | Sub-entrada a dos líneas, modo bloque, envoltorio de altura, fila de recreo (D1–D7) |
| `horario-grid.ts` | Entrada nueva para la jornada (los tramos no lectivos), que hoy no recibe |
| `horario-view.html` / `.css` | Cabecera de vista fundida (D9, D10) y consumo de `GET /api/jornada` |
| `styles.css` | Sólo si se decide tokenizar el espaciado (H-2). Este diseño no lo exige |

**Corrección de S126.** Esta tabla mezcla los tres Cambios del objetivo. Las tres primeras
filas son C-rutas-hijas y quedaron HECHAS en S123, así que la predicción sobre
`configuracion.spec.ts` ya se consumó allí. El tramo 1 de C-rejilla-densidad toca sólo las
cuatro últimas, más dos ficheros que la tabla no previó: `horario-view.ts` (que estrena el
título compuesto y el consumo de la jornada) y tres módulos puros nuevos en `horario/`
—`titulo.ts`, `recreo.ts` y `reparto.ts`, cada uno con su spec—. `styles.css` no se toca.

**Corrección de S126 a las referencias.** La cifra de vitest de esta sección (316) es de
S122; en S126 la suite parte de 356 y cierra en 381. Y el aserto del e2e que D6 tira no
está en `centro-minimo.spec.ts:213` sino en `:278`; sus tres vecinas (`.asig`, `.prof`,
`.aula`, en `:275-277`) NO caen, por decisión: D3 reordena la celda sin renombrar esos tres
`<span>`, que es el mismo razonamiento de D5 aplicado a los otros tres selectores.

### Tests que previsiblemente caen

**Medido leyendo los specs, no supuesto. Ninguna suite se ha ejecutado en esta sesión:
no toca `app/`.**

| Test | Por qué cae | Confianza |
|---|---|---|
| `e2e/centro-minimo.spec.ts:213` — `expect(primera.locator('.grupos')).toHaveText('1ESOA')` | D6 elimina el elemento `.grupos` cuando la plaza tiene un solo grupo, que es el caso del e2e | **Cae seguro** |
| `e2e/centro-minimo.spec.ts:107-180` — navegación entera | Hace `goto('/configuracion')` y luego pulsa «Nuevo nivel», «Nueva aula»… sobre UNA sola página. Con destinos cada botón sólo existe dentro del suyo: hay que intercalar navegación antes de cada alta | **Cae seguro.** Es reescritura de la mitad del spec |
| `configuracion.spec.ts` — sus 8 casos | Cada uno afirma `querySelector('app-X-lista')` sobre la plantilla de `Configuracion` (`:73`, `:83`, `:102`, `:115`, `:130`, `:143`, `:159`, `:175`). Con `<router-outlet>` ninguna lista está ahí | **Caen los 8.** Se reescriben como tests de enrutado |
| `horario-grid.spec.ts:259`, `:290` | Cuentan `.entrada` dentro de una instancia de dos sub-entradas = modo bloque | **Protegidos por D5.** Si alguien sustituye la clase en vez de modificarla, caen |
| `horario-grid.spec.ts:213-226` (caso 15, el badge) | No toca `.grupos` ni la estructura de líneas; el badge y `con-badge` siguen | No debería caer |

Nota de valor: la cabecera de `centro-minimo.spec.ts:38-42` afirma que los campos van
por `formControlName` porque «es un atributo FUNCIONAL, así que **sobrevive a
O-diseño**; una clase BEM es cosmética y puede no hacerlo». Sobrevive al aspecto, sí.
**No sobrevive a un cambio de navegación**, y es un argumento más para que esto sea
O-navegación y no un Cambio de O-diseño.

### Suites a correr

Cifras **EJECUTADAS en esta sesión**, no contadas por grep. `mvn test` desde la raíz
(informes de Surefire de esa misma corrida) y `ng test --watch=false` en
`app/frontend`. Todo en verde: 0 fallos, 0 errores, 0 saltados.

| Suite | Referencia | **Ejecutado hoy** | Comentario |
|---|---|---|---|
| vitest | 316 | **316** en 42 ficheros | Cuadra |
| app (JUnit) | 282 | **282** en 41 clases | Cuadra |
| solver (JUnit) | 91 | **91** en 34 clases | Cuadra |
| e2e | 2 | 2 (no ejecutado) | No se corre: levanta backend y reescribe `educhronos-e2e.db`. La cifra es trivial de verificar en fuente |

**Las cuatro cifras de referencia del proyecto son correctas.** Una versión anterior de
este documento afirmó que `app` daba 283 y que la referencia se había quedado corta:
era falso, y la causa está en §6.

Queda anulada la «discrepancia del solver» que este documento registró en su primera
versión: **97 anotaciones y 91 tests no son la misma métrica.** Un
`@ParameterizedTest` es una anotación y varios tests; un `@Disabled` es una anotación y
ninguno. Contar anotaciones para estimar tests es el mismo error de método que medir el
`<td>` para detectar desbordes (§6): un proxy que se parece al dato y no lo es. Se
resolvió ejecutando la suite, no afinando el `grep`.

Orden recomendado al implementar: **vitest primero** (es donde caen los 8 de
`configuracion.spec.ts`, y son los que dicen si el enrutado quedó bien), **e2e al
final** (es el caro y el que hay que reescribir). `app` y `solver` no deberían moverse:
este diseño no toca backend. Si se mueven, algo se ha tocado que no tocaba.

---

## 8. Artefactos de la sesión

- `/tmp/datos-maqueta.json` — datos REALES exportados de `app/educhronos-demo-m4.db` en
  solo lectura: el horario completo de **los 28 grupos del centro** con todas las
  sub-entradas de cada celda (791 celdas), y las ocho listas de Configuración con sus
  filas.
- `/tmp/maqueta/index.html` — maqueta en vivo, con los tokens de `styles.css` copiados
  literalmente y las piezas reales de producto. **Desechable** (M-doc-2): vive fuera del
  repo y no se conserva.
- `scripts/calcular-recortes.py` — el contador por cálculo de D11, con su autoprueba de
  cinco mutaciones dentro del propio fichero. **Es el único artefacto de la sesión que se
  CONSERVA en el repo**, por decisión del arquitecto en el cierre: quien implemente D11
  tendrá que volver a correrlo en cuanto cambie la geometría de la celda, y rehacerlo
  cuesta más que guardarlo. Es además el ejemplar de referencia de la precisión de M2
  que esta sesión añade a `metodo.md`.

El diseño de este documento se validó en navegador contra 1B-A y 1ºA, y por cálculo
contra los 28 grupos del centro. No hay capturas a propósito: lo de arriba debe poder leerse en una sesión futura
sin la maqueta delante.
