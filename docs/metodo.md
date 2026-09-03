# Método de trabajo — Educhronos

Este documento gobierna **cómo se ejecuta una sesión**. Es referencia estable: NO
se relee entero en cada apertura. El prompt de apertura lleva solo un checklist de
una línea por regla; el texto completo se consulta cuando se va a TENSAR una regla
concreta.

Es uno de tres documentos con responsabilidad separada:
- `gestion_proyecto.md` — planificación (qué se hace y por qué).
- **`metodo.md`** (este) — método (cómo se hace).
- `plan_trabajo_horarios.md` — registro (qué pasó).

Es MECANISMO VIGENTE, no crónica: si un apartado deja de aplicarse se BORRA, no se
narra. Las sesiones citadas lo son como evidencia de que la regla se practica.

**Checklist de apertura (lo único que viaja en el prompt):**
> M0 nombrar Cambio/Objetivo/Hito · M2 medir antes de fijar alcance · M3 campaña
> si hay lógica · M4 contraste antes de teclear (turno inter-módulos si toca tipo
> compartido) · M5 parar al cumplir criterio de terminado · M1 al cerrar.

---

## R4/R5 — Higiene documental (criterio de S63)

Al archivar o condensar cualquier cosa del plan rigen dos reglas, en este orden:

**R4 (guardarraíl de tokens):** ningún identificador (Dxx, D-Bx-y, Cx, §x.y) puede
quedar sin citante vivo NI sin definición viva. Se verifica por grep contra el
fichero, no por inspección visual. Si un recorte dejaría un token huérfano, NO se
recorta: se para y se decide.

**R5 (mecanismo vivo ≠ historia):** el texto que describe CÓMO SE COMPORTA EL
SISTEMA o QUÉ QUEDA PENDIENTE es estado vivo y no se archiva, aunque no tenga
identificador. Nombres de clases, métodos y comportamientos de src/main son estado
vivo. Una deuda se cierra IMPLEMENTANDO algo: su línea condensada conserva esa
implementación.

Origen: S62 perdió la descomposición de Fase 8 por archivarla sin comprobar que
era la única copia viva; S63 estuvo a punto de perder D-B8-1 y el mecanismo de D13.

---

## M-respuesta — Formato de los turnos del modelo principal (S131)

Un turno del modelo principal empieza por la DECISIÓN o la conclusión, no por el
razonamiento que lleva a ella. Inmediatamente después va el REPARTO: qué hace el
arquitecto, qué se delega a Claude Code y qué hace el propio modelo.

Si el reparto tiene más de un paso por lado, se escribe como FLUJO NUMERADO en el
orden real de ejecución —«1. tú levantas la aplicación; 2. Claude Code aplica y
mide; 3. tú juzgas en el navegador; 4. me pegas el traspaso»—. Un reparto sin
orden obliga al arquitecto a reconstruirlo, y es justo donde deja de leer.

El razonamiento y la evidencia van DESPUÉS, y sólo si sostienen una decisión
todavía abierta. Se amplían a petición.

REGLA DE NO REPETICIÓN: lo que el arquitecto ya ha leído en el turno anterior
—salida de Claude Code, tabla, diff— NO se reescribe ni se parafrasea de vuelta.
Se cita por su nombre y se dice qué se concluye de ello.

LO QUE ESTA REGLA NO RELAJA: M2 sigue trayendo la salida literal de una medición
SIN INTERPRETAR antes de proponer estructura. Viajar entera y no ser parafraseada
de vuelta son cosas distintas. Origen: el arquitecto declaró en S131 que lee las
conclusiones y las preguntas, y hace de mensajero con el resto. Acortar el
razonamiento es correcto; acortar lo que hay que JUZGAR rompería el mecanismo que
sostienen M2 y M4.

---

## M0 — Apertura: la sesión nombra su lugar en el mapa

Antes de fijar alcance, la sesión responde OBLIGATORIAMENTE:
1. ¿Qué Cambio hace avanzar?
2. ¿Qué Objetivo hace avanzar? (leído de `gestion_proyecto.md` §3)
3. ¿Qué Hito acerca?
4. ¿Toca trabajo que un objetivo YA PLANIFICADO invalidará? (R-invalidación)

Si no hay respuesta a las tres primeras, la sesión NO se abre. Una deuda solo
justifica una sesión propia si BLOQUEA el criterio de terminado del objetivo
activo (R-deuda). El caso de S99 —abrir sesión para cerrar una deuda de andamio
que no bloqueaba nada— es exactamente lo que M0 impide.

---

## M1 — Cierre de sesión. Ocho pasos (aplicables según tipo de sesión, ver §Tipos)

0. REGISTRAR qué Objetivo/Cambio avanzó la sesión, y si cumplió (total o
   parcialmente) su criterio de terminado.
1. REGISTRAR la sesión en el plan: cabecera compacta nueva, actualización del
   estado del mapa (Objetivo activo), y la deuda nueva o cerrada. Una deuda que se
   cierra dice QUÉ quedó implementado y DÓNDE; una que sobrevive con matices dice
   cuáles. La deuda nueva nace CLASIFICADA (§4 de `gestion_proyecto.md`): técnica
   real / mejora futura / decisión consciente / limitación conocida.
2. ARCHIVAR la cabecera más antigua de la ventana a la bitácora, para que el plan
   conserve siempre las 4 últimas. Ver M1-bis: único paso con fallos históricos.
3. ACTUALIZAR el censo de la bitácora. Su cabecera declara el rango DOS veces (la
   línea de descripción y la de orden cronológico). Las dos.
4. ACTUALIZAR en el plan la crónica de archivado y la frase de ventana.
5. EVALUAR LIMPIEZA con el criterio de S63/S80: se condensa un frente CERRADO,
   nunca uno con sub-bloques abiertos. Si no hay acumulación, se DICE y no se
   inventa trabajo.
6. VERIFICAR R4 por grep y R5 por lectura, contra el fichero y no de memoria.
   Además: DIFF DEL CUERPO (qué regiones se tocaron, y ninguna otra) y revisión de
   COSTURA. Este paso es mecánico: lo ejecuta preferentemente un script corrido por
   Claude Code, y el arquitecto lee su reporte (ver §Automatización).
7. PROPONER EL NOMBRE DE SESIÓN: «Educhronos. Sesión NN. <breve resumen>».
8. ENTREGAR EL PROMPT DE LA SESIÓN SIGUIENTE sin que el usuario lo pida. Ver M1-ter.

**M1-bis — El archivado, con verificación propia.** Único paso con fallos
registrados (S59 dejó copia truncada; S68 dejó el censo desfasado; S88 omitió una
rotación). Por eso lleva verificación propia:
- TRES ROTACIONES: (1) NACE la cabecera actual como `### Sesión NN`; (2) SALE la
  más antigua a la bitácora; (3) DEGRADA la H3 anterior al prefijo «Última sesión
  registrada (previa):». Invariante: UNA SOLA cabecera H3 viva en el plan.
  Verificar con `grep -c "^### Sesión" plan_trabajo_horarios.md` → 1.
- PROMOVER, no solo mover: la cabecera archivada pasa de prefijo compacto a
  `### Sesión NN` (todas las de la bitácora son H3).
- INSERTAR al final, en orden cronológico ascendente, la entrada ÍNTEGRA una vez.
- COMPROBAR: una cabecera por sesión, cuerpo idéntico al que salió (diff), y los
  dos censos coherentes entre sí y con la crónica.

**M1-ter — El prompt de la sesión siguiente.** Se entrega al cerrar y NO fija el
alcance: nombra los candidatos vivos con su estado leído del mapa y deja la
elección para la apertura. NO copia lo que ya está en la documentación: remite a
ella. Si supera ~60 líneas, está duplicando documentación y hay que podarlo.

**EXCEPCIÓN (S131) — la sesión de Acabado visual.** Si la siguiente sesión es de
ese tipo (M-visual), el prompt SÍ lleva alcance: un ACTA con la lista cerrada de
lo que puede cambiar, TRANSCRITA del registro y no decidida en el cierre. Si el
registro no la contiene ya enumerada, no hay acta y la sesión no puede arrancar
directa en Claude Code.

---

## M2 — Medición previa, antes de decidir alcance

Antes de fijar el contrato de un cambio se mide el estado real de aquello sobre lo
que se va a trabajar, con el INSTRUMENTO MÁS BARATO que responda a la pregunta
(greps y lectura literal; un test desechable cuando la pregunta es sobre datos y no
sobre el repo). La salida literal se trae SIN INTERPRETAR antes de proponer
estructura. Rendimiento demostrado: en S75–S85 la medición desmintió una suposición
de apertura del arquitecto TODAS las veces. Corolario: una afirmación sobre el
estado del repo que no se ha medido se declara como RAZONAMIENTO, no como medición.

Cuatro precisiones, en el orden en que se aplican:
- CUANDO LA MEDICIÓN DESMIENTE AL PLAN (no a una suposición del arquitecto), se
  declara como tal y se CORRIGE LA AFIRMACIÓN VIVA en TODAS sus sedes (una casilla
  de bloque y una cabecera de ventana suelen ser dos copias). Lo ya ARCHIVADO en la
  bitácora NO se corrige: es histórico de solo lectura.
- UNA CONCLUSIÓN DE MEDICIÓN DECLARA QUÉ SE MIDIÓ, no solo qué se concluye. Una
  conclusión que enumera su evidencia deja el hueco visible sin que nadie lo pise.
  Y EL INSTRUMENTO SE VERIFICA ANTES DE CREERLE: se le fuerza un caso que DEBA
  hacerlo fallar. Un medidor que solo sabe devolver «todo bien» no es un medidor, y
  su número es plausible justo hasta que alguien lo mira por otra vía. Tres
  precedentes: S117, un `EXIT=$?` colocado tras un `echo`, que medía el `echo` y no
  el comando; S121, la vía que comprobaba que un `var(--token-inexistente)` NO rompe
  el build —cierta y ciega, incapaz por construcción de distinguir un token vivo de
  uno muerto—; S122, el contador de desbordes que devolvió 0 sobre un caso que sí
  desbordaba, porque medía el `<td>`, que crece con su contenido. La forma es la
  misma en los tres: el instrumento se apoya en algo que SE PARECE al conjunto
  medido y no lo es. Ejemplar de referencia: `scripts/calcular-recortes.py`, cuyas
  cinco mutaciones viven en `autoprueba()`, corren siempre antes de dar ningún
  número y abortan si alguna no salta.
- UN TIPO COMPARTIDO SE MIDE EN TODOS LOS MÓDULOS QUE LO TOCAN. La pregunta es
  «¿quién más CONSTRUYE o CONSUME este tipo?», no «¿cuántos call sites tiene aquí?».
  Corolario: `referencia-codigo-solver.md` lista FIRMAS, no quién las usa; cuando el
  §A no pueda ver los consumidores desde el Project, la enumeración se PIDE a Claude
  Code en el turno de contraste y el contrato NO se cierra hasta tenerla.
- Nota de acumulación: una quinta precisión a M2 se condensa, no se añade (R5: el
  método no narra sesiones).

---

## M3 — Campaña de mutación: lo que un aserto vale

Un aserto vale lo que vale la mutación que lo pone rojo. Al cerrar un cambio con
tests se declara la campaña (qué mutaciones, cuál cae y por qué vía) y se demuestra
la suite NO VACÍA (romper algo → rojo esperado → restaurar → verde). Cada
restauración se verifica por DIFF contra una copia previa, nunca por el verde de la
suite: ante un mutante SUPERVIVIENTE el conteo sale idéntico con la mutación puesta
—en S110 confirmó una restauración que no había ocurrido—. Cuatro precisiones:
- CAER ante una mutación ≠ DISCRIMINAR la dimensión que ataca: un test puede caer
  por acoplamiento. La tabla de mutaciones NO es matriz de cobertura.
- Reutilizar una función NO hereda su test: es cobertura fantasma.
- Una mutación que no compila —o que no altera el comportamiento— NO es una mutación.
  En TypeScript hay que declarar el cast que la hace expresable, o el compilador tapa
  el hueco. **Ampliado en S124 con dos casos medidos, condensados aquí y no como
  precisión nueva.** (i) Una mutación puede compilar, ser sintácticamente válida y aun
  así no mutar nada: se apuntó a `\p{Sk}` creyendo que el signo de grado era símbolo
  modificador, cuando es `\p{So}`; la clase quedó vacía y el verde parecía cobertura.
  (ii) Si la mutación SÍ muta y nada se pone rojo, la causa no es siempre un test
  decoración: puede ser código INERTE. En la misma sesión, quitar un `replace` de
  marcas diacríticas no rompía nada porque el barrido posterior ya se las llevaba. La
  respuesta correcta fue BORRAR el paso, no blindarlo con un test: un paso muerto es
  peor que uno ausente, porque enseña que ahí vive una lógica que no vive ahí. Antes de
  arreglar el test, comprobar cuál de los dos casos es.
- Leer el spec ANTES de calibrar la campaña: el instrumento tiene sus propias
  trampas.

M3 se aplica DONDE HAY LÓGICA QUE MUTAR. Un renombrado, un binding de UI o un
cambio cosmético no tienen mutación que valga: se declara qué se verifica (¿compila?
¿el formulario valida? ¿el conflicto se ve?) sin exigir campaña de mutación de
lógica inexistente (ver §Tipos).

PRECISIÓN — HELPER DE SPEC CON DEPENDENCIA DE RED: si el componente bajo prueba
dispara una petición en su inicialización (p. ej. poblar un desplegable en
`ngOnInit`), el helper `montar()` DEBE flushear esa petición antes de devolver el
fixture, o el `verify()` de cierre tumba TODOS los casos —incluidos los que no
hablan de esa petición—, no solo el que la prueba. El flush no es decorativo:
sostiene los casos heredados del molde plano. Es la regla para toda entidad con
dependencia de red en construcción, que el molde plano (formularios sin carga
inicial) no necesitaba.

---

## M4 — Contraste antes de teclear, y artefactos derivados

En modo híbrido el contrato se contrasta con Claude Code ANTES de escribir código,
y lo que ese turno destape se REGISTRA, no se tapa: es el mecanismo que más errores
de especificación ha cazado (dos en S79, tres en S81/S82/S83, cinco en S85).
Corolario (S66): se especifica el ASERTO DISCRIMINANTE, no el propósito del test —un
propósito bien enunciado produce el camino feliz, que es justo lo que NO detecta el
fallo que el test existe para detectar.

PRECISIÓN DE ORDEN (S90): si el cambio toca un tipo compartido ENTRE MÓDULOS, el
contraste tiene un primer turno de MEDICIÓN —los consumidores y constructores del
tipo, en `main` y en `test`, de todos los módulos— que va ANTES de que el arquitecto
escriba contrato. El orden por defecto (contrato → contraste) deja que el contraste
descubra que el contrato era INALCANZABLE. NO se aplica a cambios de un solo módulo;
la condición es el número de módulos, no el tamaño.

Artefactos derivados (regla mecánica): si se toca `solver/src/main` se REGENERA
`referencia-codigo-solver.md`; si no, se declara que no se ha tocado. La
documentación va en commit APARTE del código; el manifiesto de dependencias va CON
el código que lo necesita.

Guion para Claude Code: BLOQUE COPIABLE Y AUTOCONTENIDO, sin referencias a «arriba»
o «la tabla» que no viajen con el texto. Rutas ABSOLUTAS. Leer la invocación de la
suite antes de escribirla.

---

## M5 — Criterio de terminado y parada

Un objetivo termina cuando cumple su criterio de terminado (definido en
`gestion_proyecto.md` §3). Las mejoras conocidas que no cambian ese criterio ni
desbloquean el siguiente objetivo se registran como mejora futura y NO se ejecutan
dentro de este objetivo.

Es el freno que convierte "¿sigo puliendo?" en pregunta binaria: ¿cambia el criterio
de terminado del objetivo activo? No → para y registra. Aplicado a S84–S99, habría
detenido la campaña de cobertura del contenedor de horario muchas sesiones antes.

M5 no es permiso para bajar el listón de calidad DENTRO de un cambio: es permiso
para NO añadir cambios que el objetivo no pide. M2/M3/M4 siguen siendo obligatorios
en cada cambio que sí se ejecuta.

---

## M-mockup — Aviso de oportunidad de mockup (antes D-F8.6-a)

Cuando un cambio de UI tiene una decisión de presentación no trivial (dónde vive un
dato, con qué advertencia se pinta), conviene un mockup previo aunque invierta el
orden de M2 (diseñar antes de medir). Es defendible solo si la sesión se dedica a
diseño; se declara como inversión consciente de M2.

La maqueta se escribe en disco y se abre en el navegador (M-doc-2);
no se vuelca en la conversación.

---

## M-doc — Cómo se entrega la documentación (S122)

Los documentos de gestión los EDITA Claude Code sobre el repo. El modelo principal
entrega el TEXTO nuevo y su punto de inserción; NO devuelve el fichero completo.

Razón, medida en S122: `gestion_proyecto.md` (144 kB) y `plan_trabajo_horarios.md`
(264 kB) devueltos enteros son del orden de cien mil tokens de salida por cierre,
en sesiones cuyo trabajo real cabe en una fracción de eso. La norma anterior
—«pide los ficheros, modifícalos y devuélvelos enteros»— nació para evitar
instrucciones de edición que se aplican mal al copiar y pegar; que las aplique
quien tiene los ficheros delante cumple esa garantía mejor, no peor.

1. Toda edición de `.md` va en un guion autocontenido para Claude Code, con rutas
   ABSOLUTAS y con GUARDA: cada inserción se ancla a una cadena que debe aparecer
   EXACTAMENTE UNA VEZ en el fichero; si no aparece una sola vez, el guion aborta
   sin tocar nada.
2. El guion hace copia de seguridad antes de escribir y VERIFICA después, por grep
   y contra el fichero, que el texto quedó donde debía. Los códigos de salida se
   capturan inmediatamente (regla de guion de S117). **El respaldo vive FUERA DEL
   REPO, en `/tmp` (S132):** dejarlo junto al documento vivo crea una copia caducada
   dentro del árbol, que es la forma exacta de `D-plan-duplicado` y que un `grep -r`
   lee igual que a la viva. Es el mismo criterio que M-doc-2 aplica a las maquetas.
5. **Un guion que añade o mueve líneas en `gestion_proyecto.md` o
   `plan_trabajo_horarios.md` TERMINA regenerando el índice (S132).** Los números de
   línea caducan en cuanto el cuerpo se desplaza, y `verificar-cierre.py` NO avisa de
   forma útil: imprime las entradas descuadradas y sale con 0 igualmente
   (`D-guion-exit-enmascarado`). En S132 hubo que suplirlo a mano tres veces.
3. El modelo principal no pide los ficheros para devolverlos. Si necesita leer,
   lee del Project o encarga a Claude Code una lectura acotada.
4. Los mensajes de commit los sigue entregando el modelo principal, de una línea.

---

## M-doc-2 — Maquetas y artefactos desechables (S122)

Una maqueta de M-mockup la ESCRIBE EN DISCO Claude Code y se abre en el navegador.
Nunca se vuelca en la conversación: es un fichero grande de un solo uso. Vive fuera
del repo (`/tmp`) salvo decisión expresa de conservarla.

---

## M-doc-3 — Índice generado (S122)

`gestion_proyecto.md` y `plan_trabajo_horarios.md` llevan al principio un índice
GENERADO, delimitado por `<!-- INDICE:INICIO -->` y `<!-- INDICE:FIN -->`, con cada
encabezado y su línea. Existe para poder leer por secciones en la apertura en vez
de leer el documento entero.

Los números de línea son INDICATIVOS: se regeneran en el cierre (ver §Automatización
del cierre). Si no cuadran, manda el TEXTO del encabezado, que es lo que se busca
por grep. Un índice que se cree exacto y no lo sea es la familia de
D-tokens-inexistentes: estado vivo equivocado.

---

## M-visual — Sesión de acabado visual: el bucle vive en Claude Code (S131)

Un cambio de aspecto no se puede juzgar hasta verlo. El ciclo aplicar → refrescar
→ juzgar tiene que ocurrir donde está el servidor, no atravesando al arquitecto
como mensajero entre dos modelos. Por eso este tipo de sesión ARRANCA EN CLAUDE
CODE y el modelo principal sólo la cierra.

**El acta la escribe el cierre anterior, no el bucle.** La sesión no abre M0:
entra con una lista CERRADA de lo que puede cambiar, TRANSCRITA del registro por
el M1-ter de la sesión previa. Lo que no está en el acta no se toca. Es
R-terminado convertido en mecanismo: el recorrido de S130 generó trabajo que su
previsión no contemplaba y partió el tramo por segunda vez.

**Vehículo: `ng serve` en `localhost:4200` con su proxy** (usado en S100, S120 y
S123), no el empaquetado. `spring-boot:run` NO reconstruye el frontend —corolario
de S130—, así que sobre el bundle cada iteración cuesta un `package` entero.
Trampa heredada de S124: `ng serve` puede servir un bundle RANCIO; ante cualquier
resultado visual sorprendente se reinicia ANTES de creerle.

**Variantes: 2–3, y sólo donde la decisión sea de gusto.** Si la regla ya está
escrita en `styles.css`, no hay variante que ofrecer: se aplica. Se ven en la
aplicación real, una detrás de otra; sólo si comparar lado a lado es
imprescindible se escribe una maqueta en `/tmp` (M-doc-2). El juez es el
arquitecto, que es lo que la ficha de O-diseño ya declara para su criterio 4.

**Registro: `styles.css`, en el mismo commit que el CSS.** Lo que cambie una regla
de aspecto se escribe junto a la decisión de identidad que la gobierna, donde el
criterio 4 la verifica por grep desde S129. NO se abre sede paralela:
`diseno-navegacion.md` es medición de geometría de O-navegación, y una segunda
sede para la misma afirmación es la familia de `D-declarado-sin-artefacto`.

**Hallazgos: se anotan, no se tocan.** Lo que el recorrido destape fuera del acta
va al traspaso como candidato a deuda, con su clasificación propuesta (§4 de
`gestion_proyecto.md`).

**El acta fija QUÉ se toca, no CÓMO (S132).** Si la premisa técnica del acta es falsa, se corrige y se hace
el trabajo: el punto 3 de S132 pedía redibujar el adorno de radios y casillas, y la solución correcta fue
`accent-color`, que no dibuja nada. Lo que el acta cierra es la LISTA, no la implementación.

**Una decisión no escrita y que no sea de gusto NO se toma en el bucle (S132).** La regla de variantes cubre
dos casos —regla escrita, se aplica; decisión de gusto, 2-3 variantes— y falta el tercero: una decisión de
MÉTODO que la documentación deja abierta. En S132 fue si conservar o retirar un token sin uso, con su propia
ficha declarando el binario «o se usa donde toca o se retira». Se anota en el traspaso y la resuelve el modelo
principal en el M1.

**Un punto que mueve geometría se mide en las DOS dimensiones (S132).** El punto 2 de S132 se cerró con el
recuento de marcas `+N`, que sólo mide alto, y el mismo commit movió el umbral de corte lateral 80 px sin que
nadie lo viera. Es M2 aplicado al bucle: el instrumento se elige por la dimensión que el cambio toca, no por
la que se suele mirar.

**LÍMITE: el recorrido de JUICIO no es una sesión de Acabado visual (S132).** M-visual CONSUME un acta; el
juicio del arquitecto la PRODUCE. Una sesión que empiece por recorrer la aplicación no puede arrancar en
Claude Code con lista cerrada, porque la lista es su salida. El recorrido lo hace el arquitecto sobre la
aplicación levantada y su resultado es el acta de la siguiente sesión de acabado. Escrito al quedar el
criterio 3 de O-diseño aplicado y sin juzgar.

**Puertas de salida del bucle.** Si el trabajo toca un `.ts`, lógica de negocio o
un tipo compartido, SALE del bucle y vuelve al procedimiento normal con M3 y M4.
El bucle cubre CSS y plantilla sin lógica.

**Qué se conserva y qué se pierde.** M2 no se omite: lo ejecuta Claude Code dentro
del bucle y su salida literal viaja al traspaso CON el comando que la produjo. M3
no aplica mientras no se toque un `.ts`. De M4 se conserva el juicio en navegador
y se pierde el turno de contraste entre dos modelos sobre el contrato: se acepta
AQUÍ Y SÓLO AQUÍ, porque en aspecto el oráculo fuerte es el ojo del arquitecto
sobre la aplicación corriendo. En Desarrollo M4 sigue entero.

**El traspaso: cómo vuelve la sesión al modelo principal.** Claude Code avisa de
que ha terminado escribiendo un traspaso corto —del orden de 30 líneas— que el
arquitecto pega en la conversación. Contiene: commits con su hash, ficheros
tocados, decisiones tomadas y variante elegida en cada una, mediciones con el
comando que las produjo, hallazgos no ejecutados y estado de las suites. Sin él,
el M1 del modelo principal sería narrativa inventada sobre una sesión que no ha
visto: exactamente la afirmación no medida y propagada que registra S130.

---

## Tipos de sesión

El ritual es proporcional al tipo. M2 y M4 se conservan en casi todos (son los que
más errores cazan); lo que se relaja es M3 donde no hay lógica que mutar.

| Tipo | Objetivo | Cuándo | Obligatorios | Omitibles (por qué) |
|---|---|---|---|---|
| **Desarrollo** | Avanzar un Cambio con lógica | Caso normal bajo objetivo activo | M0, M2, M3, M4 (si >1 módulo), M1 completo | Ninguno |
| **Saneamiento** | Cerrar varias deudas homogéneas de un objetivo, agrupadas | Cuando la deuda técnica real de un objetivo se acumula y bloquea su criterio | M0, M2 conjunto, M1 con entrada única | M3 si la deuda no tiene lógica (renombrados, cosmética); M4 si es un solo módulo. Admisión: una deuda con camino feliz sale del grupo y va a Desarrollo |
| **Configuración/UI** | Avanzar un Cambio de formulario o vista | Bajo O-shell, O-catálogo, O-estructura | M0, M4 (contraste de contrato de UI), M1 | M3 de lógica donde solo hay binding; la lógica real (validación, cálculo) SÍ lleva M3 |
| **Higiene/Método** | Condensar, archivar acumulado, cambiar el método | Cuando el plan lo pide o el método cambia | M1 + R4/R5 | M2/M3/M4 (no hay código) |
| **Acabado visual** | Aplicar el acabado de una lista CERRADA, con juicio en navegador | Bajo O-diseño, cuando el trabajo es CSS y plantilla sin lógica | Acta heredada, M2 dentro del bucle, M4 en navegador, M1 en el modelo principal con traspaso (M-visual) | M0 (el acta viene del cierre anterior); M3 mientras no se toque un `.ts`; el turno de contraste de M4 |

Regla de admisión a Saneamiento: una deuda entra en el grupo SOLO si no tiene
camino feliz que ocultar. En cuanto tiene lógica de negocio, sale y va a Desarrollo
con M3 completo.

---

## Automatización del cierre (trabajo de Claude Code, sin el modelo principal)

El paso M1.6 es mecánico y verificable; no requiere el modelo principal razonando.
Un script corrido por Claude Code lo ejecuta y REPORTA (no corrige):
- `grep -c "^### Sesión" plan_trabajo_horarios.md` → debe dar 1 (invariante H3).
- Censo de tokens R4: extraer todos los `D-*`, `C*`, `§*`; comprobar que cada uno
  tiene definición viva Y citante vivo.
- Coherencia de los dos censos de la bitácora entre sí y con la crónica.
- Diff de costura: que las regiones tocadas sean solo las previstas.
- Regenerar el índice de `gestion_proyecto.md` y `plan_trabajo_horarios.md`
  (M-doc-3): los números de línea caducan en cada cierre.

El script solo reporta; el arquitecto lee su salida. Su cobertura se valida una vez
contra un cierre hecho a mano (el de S99 sirve de test de oro).

**Regla de guion (S117):** un guion que comprueba el resultado de un comando captura
su código de salida INMEDIATAMENTE y sin nada en medio. Un `EXIT=$?` colocado tras un
`echo` mide el `echo` —siempre 0— y convierte un fallo en éxito anunciado
(D-guion-exit-enmascarado). Es la misma familia que el hallazgo de S109 sobre los
tests de endpoint: un instrumento que mide otra cosa distinta de la que se cree.
