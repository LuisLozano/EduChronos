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
| Qué señal se sacrifica para bajar de 154,2 px de celda, si es que alguna (D11) | Decisión del arquitecto entre las tres palancas medidas: banda del badge (16 px), padding y margen de plaza (~25 px en la celda de seis) o tamaño de `.asig` en modo bloque (15 px). El mecanismo de expansión hay que construirlo igual |
| Resolución del portátil (D0-2) | La resolución real del portátil del jefe de estudios. Nadie la ha medido. Hasta entonces el diseño se deriva de una constante y no de un valor cableado |
| Hora de reloj del recreo (D8) | Verificar la interpretación de zona de `LocalTime` en el dialecto de comunidad, o leer `GET /api/jornada` con el backend en marcha |
| Badges y violaciones reales sobre 1B-A/1ºA | No están en la base: los calcula `GET /api/horarios/1/diagnostico`. La maqueta reservó su sitio y NO inventó valores. Hace falta levantar el backend contra una copia de la base |
| Búsqueda en los selectores de formulario (`D-selectores-sin-busqueda`) | Sigue viva y fuera de este diseño. El filtro de D16 es de la LISTA de un destino, no de los `<select multiple>` de los formularios, que es lo que esa deuda nombra |
| Orden título/controles en la cabecera (D9) | Preferencia del arquitecto. Se eligió título a la izquierda por coherencia con la reserva de curso, que va a la derecha. Es un intercambio de una línea |
| Sede de la decisión 3 de identidad de O-diseño (densidad/espaciado, H-2) | Decisión del arquitecto: o entra en C-identidad, o se tokeniza el espaciado en un Cambio propio |

---

## 6. Lección de método de esta sesión

**El contador de celdas recortadas dio 0 durante dos iteraciones sobre un caso que sí
desbordaba.** Lo cazó el ojo del arquitecto viendo aparecer un scroll vertical, no el
instrumento. La causa: el detector comparaba `scrollHeight` con `clientHeight` sobre el
`<td>`, y un `<td>` crece con su contenido, así que **jamás podía desbordar**. El
medidor sólo sabía devolver «todo bien».

Dos cosas que llevarse, y la segunda importa más que la primera:
1. En una tabla, `height` sobre `tr` o `td` es un mínimo. Para acotar de verdad hace
   falta un envoltorio con `max-height` (D2).
2. **Un medidor que sólo puede devolver un resultado no es un medidor.** Antes de
   apoyarse en un contador hay que forzarle un caso que DEBA hacerlo saltar; es la
   campaña de mutación de M3 aplicada al instrumento de medida, y aquí no se hizo.

**Desenlace, y confirma que la lección valía la pena.** El hueco se cerró POR CÁLCULO y
no por observación: un contador que deriva la altura de las reglas CSS decididas y de
los datos reales de los 28 grupos (`/tmp/maqueta/calcular-recortes.py`), sometido antes
a cinco mutaciones que debía superar —alto de 1 px: todas recortadas; alto de 10 000 px:
ninguna; una celda sintética de doce plazas: recortada; monotonía al bajar el alto;
`alto(6) > alto(5) > alto(1)`—. Las cinco saltaron.

**El resultado fue 22 de 791, no 0** (D11). Es decir: el contador de DOM no se equivocó
sólo en 1ºA por casualidad, sino que habría dado cero sobre un centro con 22 celdas
recortadas en 11 grupos. La observación en navegador habría confirmado un diseño que
pierde clases.

Todas las alturas del cálculo son exactas por construcción: salen de `line-height`
NUMÉRICO por `font-size` —un múltiplo del tamaño, independiente de la fuente— y de
paddings en `rem`, y no hay ajuste de línea porque `.ln` y `.entrada--fila` llevan
`white-space: nowrap`. La única magnitud no calculable es el cromo de la vista, que
depende de la altura que el navegador dé a `<select>` y `<button>` nativos: por eso
entra como parámetro y se barre en un rango, en vez de fingir un número.

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
| app (JUnit) | 282 | **283** | Uno MÁS que la referencia. La cifra del encargo se ha quedado corta en algún momento; conviene actualizarla donde esté escrita |
| solver (JUnit) | 91 | **91** | Cuadra. La referencia era correcta |
| e2e | 2 | 2 (no ejecutado) | No se corre: levanta backend y reescribe `educhronos-e2e.db`. La cifra es trivial de verificar en fuente |

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
- `/tmp/maqueta/calcular-recortes.py` — el contador por cálculo de D11, con su autoprueba
  de cinco mutaciones. Es el único artefacto de la sesión que quizá convenga conservar:
  quien implemente D11 necesitará volver a correrlo cuando cambie la geometría de la
  celda, y rehacerlo cuesta más que guardarlo. Decisión del arquitecto.

El diseño de este documento se validó en navegador contra 1B-A y 1ºA, y por cálculo
contra los 28 grupos del centro. No hay capturas a propósito: lo de arriba debe poder leerse en una sesión futura
sin la maqueta delante.
