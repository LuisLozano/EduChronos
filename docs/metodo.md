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
la suite NO VACÍA (romper algo → rojo esperado → restaurar → verde). Cuatro
precisiones:
- CAER ante una mutación ≠ DISCRIMINAR la dimensión que ataca: un test puede caer
  por acoplamiento. La tabla de mutaciones NO es matriz de cobertura.
- Reutilizar una función NO hereda su test: es cobertura fantasma.
- Una mutación que no compila NO es una mutación. En TypeScript hay que declarar el
  cast que la hace expresable, o el compilador tapa el hueco.
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

El script solo reporta; el arquitecto lee su salida. Su cobertura se valida una vez
contra un cierre hecho a mano (el de S99 sirve de test de oro).
