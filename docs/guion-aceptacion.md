# Guion de aceptación de Educhronos

## 1. Qué es

Este guion ejecuta, como UNA sola cadena, los seis pasos de §1 de `docs/gestion_proyecto.md`
(«Estado final del proyecto»). Es la condición 1 de `O-aceptación` y el instrumento de su condición 4:
el proyecto está terminado cuando un humano lo ejecuta entero en un Windows limpio y cada paso pasa su
oráculo.

Los resultados esperados que se citan aquí son los del ensayo por la API sobre Linux (S171), con el
jar de `4c07553`. Los datos que no dependen de dónde coloque el solver cada clase (cuentas, entradas por
página, reglas rechazadas) se exigen tal cual. La colocación puede variar, y por eso las acciones del
paso 4 se describen por la condición que deben cumplir, no por una celda fija.

Todos los rótulos de pantalla entre «» están copiados del código del frontend
(`app/frontend/src/app/`).

## 2. Reglas de ejecución

1. Lo ejecuta un humano, **solo por la pantalla** de la aplicación.
2. **No se toca la base de datos ni se edita ningún JSON.** Sí se puede **leer** en el navegador la URL
   del diagnóstico o de la proyección (`/api/horarios/<id>/diagnostico`, `/api/horarios/<id>/proyeccion`):
   son lecturas.
3. **Una sola pestaña del navegador**, por `D-curso-pestanas-desfasadas`. Para leer una URL de la API se
   usa la misma pestaña y se vuelve con «Atrás».
4. **Una captura por cada resultado esperado** (se indica en cada paso con **Captura**).
5. **Si un paso falla, se para.** El fallo se arregla en un Cambio y la cadena se repite COMPLETA
   desde el paso 1, en un Windows limpio. Una corrida válida es una corrida sin paradas.

## 3. Preparación

### 3.1 Windows (la aceptación)

- Windows 11 **limpio**, **cuenta estándar** (sin permisos de administrador), **sin Java ni Node**.
- El bundle se construye desde el commit que se acepta según `docs/empaquetado.md`: lado Linux (§2),
  lado Windows (§3) y construcción en la máquina virtual (sección «Construir en la máquina virtual
  (S163)»). No se repite aquí el procedimiento.
- Se anotan para el acta: **commit**, **sha256 del jar** (el que se pasa como `-HuellaJar`) y
  **sha256 del zip del bundle** (lo imprime `empaquetar-windows.ps1`).
- Se guarda aparte el **mismo fichero de jar** cuyo sha256 se pasó como `-HuellaJar`. Los oráculos
  de §7 lo necesitan. No vale reconstruirlo: el jar no es reproducible bit a bit.

### 3.2 Ensayo en Linux (opcional; NO valida el paso 1)

Con el jar del commit (`mvn clean package -DskipTests` desde la raíz, `docs/empaquetado.md` §2) y una
carpeta de datos vacía y propia, en modo servidor (sin `-Deduchronos.escritorio=true`):

```bash
D=/tmp/ensayo-aceptacion; mkdir -p $D/datos; ls -A $D/datos   # debe salir vacío
ss -ltnH 'sport = :8080'                                        # debe salir vacío: puerto libre
XDG_DATA_HOME=$D/datos nohup java -jar <raíz>/app/target/app-0.1.0-SNAPSHOT.jar > $D/app.log 2>&1 &
echo $! > $D/app.pid
```

Se abre `http://127.0.0.1:8080` en el navegador. La base queda en `$D/datos/educhronos/educhronos.db`
(el log lo dice en la línea «Base de datos de Educhronos en …»). Para pararlo: `kill $(cat $D/app.pid)`.
Si el ensayo pasa entero, solo demuestra que la cadena es ejecutable. **El paso 1 solo se valida en
Windows.**

## 4. El centro de aceptación

Contiene al menos una vez cada elemento del paso 2 de §1, además de una actividad de 2 tramos. Los
códigos son exactamente estos, porque el paso 4 y los oráculos se refieren a ellos.

**Jornada** (la propuesta de la pantalla, sin tocar): lunes a viernes, 6 tramos lectivos y un recreo.

| # | Inicio | Fin | Recreo |
|---|---|---|---|
| 1 | 08:00 | 09:00 | — |
| 2 | 09:00 | 10:00 | — |
| 3 | 10:00 | 11:00 | — |
| 4 | 11:00 | 11:30 | ✔ |
| 5 | 11:30 | 12:30 | — |
| 6 | 12:30 | 13:30 | — |
| 7 | 13:30 | 14:30 | — |

En la rejilla del horario los lectivos se numeran del 1 al 6, y el recreo cae entre el 3 y el 4.

**Nivel** (la interfaz lo exige para crear grupos): `3ESO`, orden `3`.

**Profesores**

| Código | Nombre completo |
|---|---|
| P1 | Profesor Uno |
| P2 | Profesora Dos |
| P3 | Profesor Tres |
| P4 | Profesora Cuatro |

**Aulas**

| Código | Tipo |
|---|---|
| A1 | ORDINARIA |
| A2 | ORDINARIA |
| LAB | LAB_CIENCIAS |

**Asignaturas**

| Código | Nombre completo |
|---|---|
| MAT | Matemáticas |
| LEN | Lengua Castellana |
| ING | Inglés |
| REL | Religión |
| VAL | Valores Éticos |
| ACM | Ámbito Científico-Matemático |
| TEC | Tecnología |

**Grupos, tutores y PDC**

| Grupo | Tipo | Nivel | Tutor principal | Cómo se crea |
|---|---|---|---|---|
| 3ºA | ordinario | 3ESO | P1 | «Nuevo grupo» |
| 3ºB | ordinario | 3ESO | P2 | «Nuevo grupo» |
| 3ºA-Di | PDC, padre 3ºA | 3ESO (heredado) | P1 (heredado del padre) | botón «PDC» de 3ºA |

**Subgrupos.** El alta de un grupo ordinario NO crea ningún subgrupo, así que se crean a mano. El PDC
crea el suyo, `3ºA-Di-Completo`.

| Subgrupo | Grupos | Para qué |
|---|---|---|
| 3ºA-Completo | 3ºA | clases de todo 3ºA |
| 3ºB-Completo | 3ºB | clases de todo 3ºB |
| 3ºA-G1 | 3ºA | desdoble de ING, mitad 1 |
| 3ºA-G2 | 3ºA | desdoble de ING, mitad 2 |
| REL | 3ºA, 3ºB | agrupamiento REL/VAL |
| VAL | 3ºA, 3ºB | agrupamiento REL/VAL |
| 3ºA-Di-Completo | 3ºA-Di | (lo crea el PDC) |

**Disponibilidad del profesorado**

| Profesor | Tipo | Dónde |
|---|---|---|
| P3 | DURA («No puede») | lunes, los 6 tramos |
| P2 | BLANDA («Prefiere no») | viernes, tramo 6 (13:30–14:30) |

**Actividades.** Todas con patrón NEUTRA, sin «Requiere tutor» y la asignatura de la actividad en
«— varias (una por plaza) —». Todas las plazas usan aula fija.

| Código | Estructura | Duración | Repeticiones | Plazas (asignatura · aula · profesor · subgrupo) | Sesiones |
|---|---|---|---|---|---|
| MAT-3ºA | ordinaria | 1 | 3 | MAT · A1 · P1 · 3ºA-Completo | 3 |
| MAT-3ºB | ordinaria | 1 | 3 | MAT · A2 · P1 · 3ºB-Completo | 3 |
| LEN-3ºA | ordinaria | 1 | 3 | LEN · A1 · P2 · 3ºA-Completo | 3 |
| LEN-3ºB | ordinaria | 1 | 3 | LEN · A2 · P2 · 3ºB-Completo | 3 |
| ING-3ºA | **desdoble** | 1 | 2 | ING · A1 · P3 · 3ºA-G1 ‖ ING · LAB · P4 · 3ºA-G2 | 2 × 2 plazas |
| RELVAL-3ºAB | **agrupamiento** 3ºA+3ºB | 1 | 2 | REL · A1 · P3 · REL ‖ VAL · A2 · P4 · VAL | 2 × 2 plazas |
| ACM-3ºA-Di | **PDC** | 1 | 3 | ACM · LAB · P4 · 3ºA-Di-Completo | 3 |
| TEC-3ºB | **bloque de 2 tramos** | **2** | 1 | TEC · LAB · P3 · 3ºB-Completo | 1 (ocupa 2 tramos) |

«Currículo» no es una pantalla propia: es el conjunto de actividades de cada grupo (tabla anterior).

Totales: 8 actividades, 10 plazas y 24 filas de sesión por horario. Carga por grupo: 3ºA, 10 tramos;
3ºB, 10; 3ºA-Di, 3. Carga por profesor: P1 6, P2 6, P3 6 y P4 7.

## 5. Pasos

### Paso 1 — Instalar y arrancar

**Acción**
1. Extraer el zip del bundle con el Explorador en una carpeta del usuario (`docs/empaquetado.md`,
   «Prueba final en Windows limpio (S156)»).
2. Doble clic en `Educhronos.exe`, dentro de la carpeta extraída.

**Resultado esperado**
- Se abre solo el navegador en `http://127.0.0.1:8080`, con la portada.
- En la barra superior: «Educhronos», «Configuración», «Horario», el nombre «Curso sin nombre» y el botón
  «Cursos…».
- Se crea la carpeta `%LOCALAPPDATA%\Educhronos\` con `educhronos.db` y `educhronos.log`.

**Oráculo.** En pantalla, la portada. En el Explorador, la carpeta y sus dos ficheros.

**Captura.** 1a: la portada con la barra. 1b: la carpeta `%LOCALAPPDATA%\Educhronos\`.

### Paso 2 — Crear el centro desde cero por la interfaz

Todas las altas siguen el mismo molde: «Configuración», el destino del índice izquierdo, el botón de
alta, rellenar, y «Guardar». El diálogo se cierra y la fila aparece en la lista. Si el diálogo sigue
abierto con un mensaje, el alta ha fallado: el paso falla.

**Acción**

1. **Jornada.** «Configuración» (abre en «Jornada»). Se ve la insignia «Propuesta · sin guardar» y la
   tabla de la §4. No se toca nada: «Guardar jornada».
2. **Nivel.** «Niveles» › «Nuevo nivel» › «Código» `3ESO`, «Orden» `3` › «Guardar».
3. **Profesores.** «Profesores» › «Nuevo profesor» › «Código» y «Nombre completo» de la §4 › «Guardar».
   Cuatro veces.
4. **Aulas.** «Aulas» › «Nueva aula» › «Código», «Tipo» (desplegable «— elige un tipo —») › «Guardar».
   Tres veces.
5. **Asignaturas.** «Asignaturas» › «Nueva asignatura» › «Código», «Nombre completo» › «Guardar». Siete
   veces.
6. **Grupos.** «Grupos» › «Nuevo grupo» › «Código» `3ºA`, «Nivel» `3ESO` (desplegable «— elige un
   nivel —») › «Guardar». Igual con `3ºB`.
7. **Tutores (ANTES del PDC).** En la fila de 3ºA, «Tutoría» › «Tutor principal» `P1 — Profesor Uno` ›
   «Guardar». En la fila de 3ºB, lo mismo con `P2 — Profesora Dos`.
8. **PDC.** En la fila de 3ºA, «PDC». El diálogo dice «3ºA no tiene grupo de diversificación. Dale un
   código para crearlo.» › «Código del PDC» `3ºA-Di` › «Crear PDC» › «Cerrar».
9. **Comprobar la herencia.** En la fila nueva de 3ºA-Di, «Tutoría»: el «Tutor principal» ya es
   `P1 — Profesor Uno`. «Cancelar».
10. **Subgrupos.** «Subgrupos» › «Nuevo subgrupo» › «Código» y «Grupos» (selección múltiple con Ctrl+clic)
    de la §4 › «Guardar». Seis veces. `3ºA-Di-Completo` ya está en la lista y no se crea.
11. **Disponibilidad DURA.** «Profesores» › en la fila de P3, «Disponibilidad». En «Pincel» dejar
    «No puede», pulsar la cabecera de columna «Lunes» (pinta los 6 tramos del lunes) › «Guardar».
12. **Disponibilidad BLANDA.** En la fila de P2, «Disponibilidad». En «Pincel» elegir «Prefiere no» y
    pulsar la celda Viernes · `13:30–14:30` › «Guardar».
13. **Actividades.** «Actividades» › «Nueva actividad», una vez por fila de la tabla de actividades de
    la §4:
    - «Código»; «Asignatura (opcional)» se deja en «— varias (una por plaza) —».
    - «Duración (tramos)»: `1`, salvo **TEC-3ºB, que lleva `2`**. «Repeticiones por semana» según la tabla.
    - «Patrón temporal» `NEUTRA`; «Requiere tutor» sin marcar.
    - En «Plaza 1»: «Asignatura de la plaza»; «Aula fija» (ya marcada) con su aula; «Profesores» y
      «Subgrupos» (selección múltiple).
    - En ING-3ºA y RELVAL-3ºAB: «Añadir plaza» y rellenar «Plaza 2».
    - «Guardar».
14. **Comprobar la prevalidación.** «Horario» en la barra superior.

**Resultado esperado**
- Tras la acción 1, la insignia «Propuesta · sin guardar» desaparece.
- Cada lista muestra lo creado. La tabla de «Grupos» muestra tres grupos: 3ºA y 3ºB con «Tipo»
  «Ordinario», y 3ºA-Di con «PDC».
  «Subgrupos» muestra siete.
- Tras la acción 9, 3ºA-Di tiene tutor principal P1.
- Tras la acción 14, la vista del horario muestra «Sin hallazgos de pre-validación. Esto no garantiza
  que el solver encuentre horario en el tiempo previsto.» y «Este curso todavía no tiene horario. Créalo
  con «Generar horario».».

**Oráculo**
- En pantalla: las listas y los dos textos de la acción 14. El primero equivale a
  `GET /api/prevalidacion` → `[]` (en el ensayo, `[]`).
- A posteriori en Linux (§7.2): las cuentas de la configuración y el detalle (grupos, tutores,
  restricciones, jornada, subgrupos y estructura de actividades) sobre la base traída.

**Captura.** 2a: la jornada guardada, sin insignia. 2b: la lista de grupos. 2c: la tutoría de 3ºA-Di.
2d: la lista de subgrupos. 2e: la disponibilidad de P3. 2f: la disponibilidad de P2. 2g: la lista de
actividades. 2h: la vista del horario con los dos textos.

### Paso 3 — Generar un horario

**Acción**
1. En «Horario», «Generar horario».
2. En el diálogo «Confirmar la generación» (se ve «Tarda unos diez minutos.», «Sustituye el horario en
   el que estás trabajando.» y «No se puede deshacer.»), pulsar «Generar horario».
3. Anotar el id de la barra de direcciones: `/horario/<id>`. En una instalación nueva es `1`.
4. En la misma pestaña, abrir `http://127.0.0.1:8080/api/horarios/<id>/proyeccion` y leer
   `estadoSolver` y `objetivo`. Después, «Atrás».
5. En la misma pestaña, abrir `http://127.0.0.1:8080/api/horarios/<id>/diagnostico`. Después, «Atrás».
6. En «Vista», recorrer «Grupo», «Profesor» y «Aula», y en el segundo desplegable cada entidad.

**Resultado esperado**
- Mientras calcula se ve «Generando horario… puede tardar hasta 10 minutos.». En el ensayo tardó menos
  de un segundo.
- La rejilla aparece.
- Entidades del desplegable: Grupo → `3ºA`, `3ºA-Di`, `3ºB`; Profesor → `P1`–`P4`; Aula → `A1`, `A2`,
  `LAB`.
- **Ninguna clase lleva el contorno rojo de conflicto.**
- TEC-3ºB ocupa dos tramos seguidos del mismo día, sin cruzar el recreo; el segundo se pinta con borde
  discontinuo y sin candado.
- Proyección: `"estadoSolver":"OPTIMAL"` y `"objetivo":0.0`.
- Diagnóstico: `"violaciones":[]` y `"totales":{"ventanas":0,"consecutivas":0,"indispBlanda":0}`.
- En el ensayo: horario 1, OPTIMAL, objetivo 0.0, 24 sesiones y 0 violaciones.

**Oráculo**
- En pantalla y en el navegador: el diagnóstico, con `violaciones` vacía.
- A posteriori en Linux (§7.3): el diagnóstico recalculado con el jar del commit sobre la copia de la
  base traída.

**Captura.** 3a: el diálogo de confirmación. 3b: la rejilla de 3ºB con TEC en sus dos tramos. 3c: la
proyección (estado y objetivo). 3d: el diagnóstico.

### Paso 4 — Ajustar a mano

Antes de cada arrastre se mira que el destino esté libre en las tres vistas que afectan a esa clase:
«Grupo» (su grupo), «Profesor» (sus profesores) y «Aula» (sus aulas). Se arrastra agarrando la clase
por su cuerpo. Si al soltar hay UNA clase en la celda de destino, la aplicación intercambia las dos en
vez de mover, y esa no es la acción pedida.

**Acción**

- **4a — Mover a una celda con restricción BLANDA.** Elegir una sesión de LEN (profesora P2) cuya celda
  de viernes, tramo 6, esté libre para su grupo, para su aula y para P2. En la vista de su grupo,
  arrastrarla a Viernes · 6.
- **4b — Intento que rompe una DURA.** Elegir una clase de P3 que NO sea TEC-3ºB (una sesión de
  ING-3ºA o de RELVAL-3ºAB) y un tramo del lunes libre para su grupo, sus subgrupos, sus aulas y el
  otro profesor de la clase, de modo que lo único que se rompa sea la indisponibilidad DURA de P3.
  Arrastrarla allí.
- **4c — Intento de bloque que cruza el recreo.** Arrastrar TEC-3ºB a la celda del tramo 3 de un día
  que no sea lunes, con esa celda libre para 3ºB, LAB y P3. Un bloque de 2 que empieza en el 3
  cruzaría el recreo.
- **4d — Fijar y relanzar.**
  1. Pasar el ratón sobre la sesión de LEN de 4a: aparece el candado 🔓 («Poner pin»). Pulsarlo.
  2. «Generar horario» › «Generar horario».
  3. Anotar el id nuevo de la barra de direcciones.
  4. Abrir `/api/horarios/<id nuevo>/proyeccion` y `/api/horarios/<id nuevo>/diagnostico`, volviendo con
     «Atrás» después de cada una.

**Resultado esperado**
- **4a:** la clase queda en Viernes · 6 sin ningún aviso de rechazo y lleva una insignia de coste
  blando positiva. En `/api/horarios/<id>/diagnostico`:
  - `violaciones` sigue vacía;
  - `penalizaciones` contiene `{"regla":"INDISPONIBILIDAD_BLANDA","actividadCodigo":"LEN-…","indice":…,"tramoCodigo":"V6","delta":1}`;
  - `totales.indispBlanda` vale 1.

  En el ensayo se movió LEN-3ºB #1 de J5 a V6. Además apareció `EXCESO_CONSECUTIVAS`, porque P2 quedó
  con cuatro clases seguidas; eso depende de la colocación y no se exige.
- **4b:** la clase NO se mueve. Aparece «Ese cambio provoca conflictos que antes no existían:» con UNA
  sola línea: `INDISPONIBILIDAD_PROFESOR — P3 en L<n>`. En el ensayo, ING-3ºA #1 a L1 dio 409
  `VIOLA_REGLA_DURA` con esa única violación, y la base quedó sin cambios.
- **4c:** TEC no se mueve. Aparece el mismo texto con UNA sola línea:
  `BLOQUE_IMPOSIBLE — TEC-3ºB #1 en <día>3`. En el ensayo, a J3: 409 con esa única violación y la
  base sin cambios.
- **4d:**
  - Tras pulsar el candado, la clase se pinta como fijada, con 🔒 («Quitar pin»), y aparece
    «1 pines sin aplicar — regenerar». Ese aviso se sigue viendo mientras exista el pin, también después
    de regenerar.
  - Tras regenerar, el id de la barra es nuevo (en una instalación nueva, `2`) y la sesión fijada sigue
    en Viernes · 6 con 🔒.
  - Ninguna clase lleva contorno rojo.
  - Proyección: `"estadoSolver":"OPTIMAL"` y `"objetivo":1.0`.
  - Diagnóstico: `"violaciones":[]`, `"totales":{"ventanas":0,"consecutivas":0,"indispBlanda":1}`, y la
    ÚNICA penalización con `delta` positivo es la `INDISPONIBILIDAD_BLANDA` de esa sesión en `V6`. Las
    `VENTANA_PROFESOR` con `delta` −1 son contrafactuales: miden lo que empeoraría mover la clase, no
    un coste que exista.
  - En el ensayo: horario 2, OPTIMAL, objetivo 1.0 y esa única blanda.

**Oráculo**
- En pantalla: la posición de las clases, los textos de rechazo, el candado y la ausencia de contorno
  rojo.
- En el navegador: el diagnóstico y la proyección.
- A posteriori en Linux (§7.3): el diagnóstico del horario final recalculado.

**Captura.** 4a: la clase en V6 con su insignia, y el diagnóstico con la penalización. 4b: el rechazo
con su línea. 4c: el rechazo con su línea. 4d: la rejilla regenerada con la clase fijada en V6, la
proyección y el diagnóstico.

### Paso 5 — Exportar

**Acción.** En la vista del horario final (el de 4d), pulsar los cuatro enlaces: «Exportar CSV»,
«PDF por grupo», «PDF por profesor» y «PDF por aula». Abrir cada fichero.

**Resultado esperado**
- Se descargan `horario-<id>.csv`, `horario-<id>-grupo.pdf`, `horario-<id>-profesor.pdf` y
  `horario-<id>-aula.pdf`.
- El CSV se abre en Excel con 25 filas de datos: 24 sesiones más el segundo tramo de TEC.
- PDF de grupo: 3 páginas (3ºA, 3ºA-Di, 3ºB). PDF de profesor: 4 páginas (P1–P4). PDF de aula: 3 páginas
  (A1, A2, LAB).
- TEC aparece en sus dos tramos en el CSV y en los PDF de 3ºB, P3 y LAB.

**Oráculo.** A posteriori en Linux (§7.4). Resultado exigido, igual que en el ensayo:

| Oráculo | Resultado |
|---|---|
| `oraculo-exportacion.py csv` | «OK: las tres vistas coinciden» (grupo 29/29, profesor 25/25, aula 25/25), rc 0 |
| `oraculo-exportacion.py pdf --vista grupo` | 3ºA exige 14, 3ºA-Di 3, 3ºB 12; «TOTAL: halladas 29, FALTAN 0, SOBRAN 0», rc 0 |
| `oraculo-exportacion.py pdf --vista profesor` | P1 6, P2 6, P3 6, P4 7; «TOTAL: halladas 25, FALTAN 0, SOBRAN 0», rc 0 |
| `oraculo-exportacion.py pdf --vista aula` | A1 10, A2 8, LAB 7; «TOTAL: halladas 25, FALTAN 0, SOBRAN 0», «páginas vacías con leyenda: 0», rc 0 |
| `verificar-leyenda-pdf.py` × 3 | «páginas OK: 3 / 4 / 3   con fallo: 0», rc 0 |

**Captura.** 5a: el CSV abierto. 5b: la página de 3ºB del PDF de grupo, con TEC en dos celdas.

### Paso 6 — Duplicar el curso

El campo exige la forma `2025/2026`, con barra: «2025-2026» da el error «El nombre del curso debe tener
la forma 2026/2027, con dos años consecutivos.». Los ficheros sí se nombran con guion
(`curso-2026-2027.db`).

**Acción**
1. «Cursos…» › «Duplicar curso…».
2. «Curso nuevo» `2026/2027`. «Nombre del curso actual» `2025/2026`: aparece porque la base no tiene
   nombre. «Duplicar».
3. «Cursos…». En la fila del fichero `educhronos.db`, «Abrir».
4. «Configuración» › «Niveles» › «Nuevo nivel» › «Código» `4ESO`, «Orden» `4` › «Guardar». Después,
   «Cancelar».
5. «Cursos…». En la fila de `curso-2026-2027.db`, «Abrir».
6. «Horario».

**Resultado esperado**
- **Tras 2:** la barra muestra `2026/2027` sin marca de solo lectura.
- **Tras 3:**
  - La barra muestra `2025/2026` con la marca «Solo lectura».
  - En la lista de cursos, `educhronos.db` aparece como «Archivado» y `curso-2026-2027.db` como «Activo».
- **Tras 4:**
  - El diálogo sigue abierto con «El curso 2025/2026 está archivado y es de solo lectura. Los cambios se
    hacen en el curso activo.».
  - El nivel NO se crea: la lista sigue con `3ESO` solo.
  - En el ensayo, HTTP 403 `CURSO_SOLO_LECTURA`.
- **Tras 5 y 6:**
  - `2026/2027` abierto y sin marca.
  - El horario dice «Este curso todavía no tiene horario. Créalo con «Generar horario».».
  - «Configuración» conserva todo el centro.
- **En disco**, en `%LOCALAPPDATA%\Educhronos\`: `educhronos.db`, `curso-2026-2027.db` y el puntero
  `curso-abierto`, cuyo contenido es `curso-2026-2027.db`.

**Oráculo**
- En pantalla: la marca, el mensaje y las listas.
- A posteriori en Linux (§7.5): las 17 tablas de configuración idénticas en las dos bases, el curso
  nuevo sin horario y la fila `curso` de cada una.

**Captura.** 6a: el formulario de duplicar relleno. 6b: la lista de cursos. 6c: la barra con «Solo
lectura» y el mensaje del alta rechazada. 6d: el curso nuevo con su configuración y sin horario. 6e: la
carpeta de datos.

## 6. Qué se trae de Windows

- **La carpeta de datos entera**, `%LOCALAPPDATA%\Educhronos\` (`educhronos.db`, `curso-2026-2027.db`,
  `curso-abierto`, `educhronos.log`), copiada **con la aplicación cerrada**: icono «Educhronos» de la
  bandeja › «Salir».
- **Las cuatro descargas** del paso 5.
- **Todas las capturas**.

## 7. Oráculos a posteriori (Linux, sobre copias)

Son las órdenes del ensayo de S171. Todas trabajan sobre **copias**: la carpeta traída no se modifica, y
sqlite se abre en `mode=ro`. `<id>` es el id del horario final (paso 4d).

### 7.1 Preparación

```bash
R=<raíz del repo, en el commit aceptado>
T=<carpeta traída de Windows>        # contiene educhronos.db, curso-2026-2027.db, curso-abierto
X=<carpeta con las cuatro descargas>
JAR=<el jar guardado en 3.1, cuyo sha256 es el de -HuellaJar>
W=/tmp/aceptacion-oraculos; mkdir -p $W
sha256sum $JAR                        # debe coincidir con el del acta
cp $T/educhronos.db $W/copia.db && cp $T/curso-2026-2027.db $W/copia-nuevo.db; echo "rc=$?"
```

### 7.2 Paso 2 — cuentas y detalle del centro

`educhronos.db` es el curso archivado y guarda el centro tal como se creó (más los horarios).

```bash
for t in actividad asignatura asignatura_aula_compatible aula configuracion grupo_administrativo nivel plaza plaza_aula_candidata plaza_profesor plaza_subgrupo profesor profesor_restriccion_horaria profesor_tutoria subgrupo subgrupo_grupo tramo_semanal; do
  printf '%-30s %s\n' $t "$(sqlite3 "file:$W/copia.db?mode=ro" "select count(*) from $t")"; done
```

Esperado, igual que en el ensayo: actividad 8, asignatura 7, asignatura_aula_compatible 0, aula 3,
configuracion 0, grupo_administrativo 3, nivel 1, plaza 10, plaza_aula_candidata 0, plaza_profesor 10,
plaza_subgrupo 10, profesor 4, profesor_restriccion_horaria 7, profesor_tutoria 3, subgrupo 7,
subgrupo_grupo 9 y tramo_semanal 35.

```bash
sqlite3 "file:$W/copia.db?mode=ro" <<'EOF'
.headers on
.mode column
select g.codigo grupo, g.tipo, p.codigo padre from grupo_administrativo g left join grupo_administrativo p on p.id=g.grupo_padre_id order by g.id;
select g.codigo grupo, pr.codigo profesor, t.rol from profesor_tutoria t join grupo_administrativo g on g.id=t.grupo_id join profesor pr on pr.id=t.profesor_id order by g.id;
select pr.codigo profesor, r.tipo, ts.dia, count(*) n from profesor_restriccion_horaria r join profesor pr on pr.id=r.profesor_id join tramo_semanal ts on ts.id=r.tramo_id group by 1,2,3;
select dia, sum(es_lectivo) lectivos, sum(1-es_lectivo) recreos from tramo_semanal group by dia order by min(orden);
select s.codigo subgrupo, group_concat(g.codigo, '+') grupos from subgrupo s join subgrupo_grupo sg on sg.subgrupo_id=s.id join grupo_administrativo g on g.id=sg.grupo_id group by s.id order by s.id;
select a.codigo actividad, a.duracion_tramos dur, a.repeticiones_por_semana rep, pl.codigo plaza, asg.codigo asig, au.codigo aula, pr.codigo prof, sb.codigo subgrupo
from actividad a join plaza pl on pl.actividad_id=a.id join asignatura asg on asg.id=pl.asignatura_id
left join aula au on au.id=pl.aula_fija_id join plaza_profesor pp on pp.plaza_id=pl.id join profesor pr on pr.id=pp.profesor_id
join plaza_subgrupo ps on ps.plaza_id=pl.id join subgrupo sb on sb.id=ps.subgrupo_id order by a.id, pl.id;
EOF
```

Esperado:
- 3ºA-Di es `DIVERSIFICACION_PDC` con padre 3ºA.
- Tutores principales: 3ºA → P1, 3ºB → P2 y 3ºA-Di → P1.
- Restricciones: P2 BLANDA el VIERNES (1) y P3 DURA el LUNES (6).
- La jornada tiene 6 lectivos y 1 recreo cada uno de los cinco días.
- Los subgrupos y las diez plazas coinciden con la §4.

### 7.3 Pasos 3 y 4 — el diagnóstico recalculado

Se arranca el jar del commit sobre una copia de `educhronos.db` (el curso archivado, que es el que
guarda los horarios; en solo lectura los GET funcionan) y se piden la proyección y el diagnóstico de
cada horario. No hay que regenerar nada.

```bash
cp $T/educhronos.db $W/recalculo.db
ss -ltnH 'sport = :8080'                                   # debe salir vacío
nohup java -jar $JAR --spring.datasource.url=jdbc:sqlite:$W/recalculo.db > $W/recalculo.log 2>&1 &
echo $! > $W/recalculo.pid
for i in $(seq 1 60); do c=$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/api/curso); [ "$c" = 200 ] && break; sleep 1; done; echo "http=$c"
for h in 1 <id>; do
  curl -s -o $W/proy-$h.json http://127.0.0.1:8080/api/horarios/$h/proyeccion
  curl -s -o $W/diag-$h.json http://127.0.0.1:8080/api/horarios/$h/diagnostico
  python3 -c "
import json; p=json.load(open('$W/proy-$h.json')); d=json.load(open('$W/diag-$h.json'))
print('horario', p['id'], p['estadoSolver'], 'objetivo', p['objetivo'], 'sesiones', len(p['sesiones']))
print('  violaciones', len(d['violaciones']), 'totales', d['totales'])
print('  blandas con delta>0:', [(x['regla'], x['actividadCodigo'], x['indice'], x['tramoCodigo']) for x in d['penalizaciones'] if x['delta'] > 0])"
done
kill $(cat $W/recalculo.pid)
```

Esperado:
- Horario 1: `OPTIMAL`, objetivo `0.0`, 24 sesiones y 0 violaciones.
- Horario `<id>`: `OPTIMAL`, objetivo `1.0`, 24 sesiones, 0 violaciones, totales `ventanas 0,
  consecutivas 0, indispBlanda 1`, y como única blanda con delta positivo
  `('INDISPONIBILIDAD_BLANDA', 'LEN-…', …, 'V6')`.
- La sesión fijada: `sqlite3 "file:$W/copia.db?mode=ro" "select count(*) from sesion_bloqueada"` → 1.

El orden de `penalizaciones` puede cambiar de una JVM a otra: en el ensayo, el recálculo dio la misma
lista en otro orden. Se compara como conjunto, nunca como texto.

### 7.4 Paso 5 — exportación

```bash
S=$R/scripts
python3 $S/oraculo-exportacion.py csv $W/copia.db <id> $X/horario-<id>.csv; echo "rc=$?"
for v in grupo profesor aula; do
  python3 $S/oraculo-exportacion.py pdf $W/copia.db <id> $X/horario-<id>-$v.pdf --vista $v; echo "oraculo $v rc=$?"
  python3 $S/verificar-leyenda-pdf.py $W/copia.db <id> $X/horario-<id>-$v.pdf $v; echo "leyenda $v rc=$?"
done
```

Necesitan `python3`, `pdftotext` y `pdfinfo` (poppler). Esperado: la tabla del paso 5, con los siete
`rc=0`.

### 7.5 Paso 6 — configuración conservada y curso nuevo sin horario

Configuración son las 17 tablas del esquema que el duplicado copia tal cual. Quedan fuera las cuatro que
vacía, `TABLAS_DEL_HORARIO` (`sesion_bloqueada`, `aula_bloqueada`, `sesion`, `horario_generado`,
`app/src/main/java/es/yaroki/educhronos/app/curso/DuplicadorCurso.java:104-105`), y `curso`, que es la
identidad del fichero y se reescribe.

```bash
A=$W/copia.db; N=$W/copia-nuevo.db; fallos=0
for t in actividad asignatura asignatura_aula_compatible aula configuracion grupo_administrativo nivel plaza plaza_aula_candidata plaza_profesor plaza_subgrupo profesor profesor_restriccion_horaria profesor_tutoria subgrupo subgrupo_grupo tramo_semanal; do
  r=$(sqlite3 "file:$N?mode=ro" "attach 'file:$A?mode=ro' as a; select (select count(*) from main.$t), (select count(*) from a.$t), (select count(*) from (select * from main.$t except select * from a.$t)), (select count(*) from (select * from a.$t except select * from main.$t));") || break
  IFS='|' read nn na n_a a_n <<< "$r"
  printf '%-30s nuevo %3s  archivado %3s  nuevo-archivado %s  archivado-nuevo %s\n' $t $nn $na $n_a $a_n
  [ "$nn" = "$na" ] && [ "$n_a" = 0 ] && [ "$a_n" = 0 ] || fallos=$((fallos+1))
done
for t in horario_generado sesion sesion_bloqueada aula_bloqueada; do
  printf '%-30s nuevo %s  archivado %s\n' $t "$(sqlite3 "file:$N?mode=ro" "select count(*) from $t")" "$(sqlite3 "file:$A?mode=ro" "select count(*) from $t")"; done
echo "curso nuevo:     $(sqlite3 "file:$N?mode=ro" "select nombre, archivado from curso")"
echo "curso archivado: $(sqlite3 "file:$A?mode=ro" "select nombre, archivado from curso")"
echo "tablas de configuración con diferencias: $fallos"
cat $T/curso-abierto; echo
```

Esperado:
- Las 17 tablas, con `nuevo-archivado 0` y `archivado-nuevo 0` y las mismas cuentas que en §7.2;
  «tablas de configuración con diferencias: 0».
- En el curso nuevo, 0 en `horario_generado`, `sesion`, `sesion_bloqueada` y `aula_bloqueada`. En el
  archivado, 2, 48, 1 y 0.
- `curso nuevo: 2026/2027|0` y `curso archivado: 2025/2026|1`.
- El puntero contiene `curso-2026-2027.db`.

## 8. Acta

```
ACTA DE ACEPTACIÓN — Educhronos

Fecha:
Ejecutor:
Máquina (modelo / VM) y edición de Windows (versión y compilación):
Cuenta estándar sin administrador: sí / no
Java y Node ausentes antes de instalar: sí / no
Commit aceptado:
sha256 del jar (-HuellaJar):
sha256 del zip del bundle:
Ids de horario: generado en el paso 3 =        regenerado en 4d =

| Paso | Resultado (PASA/FALLA) | Oráculo aplicado | Capturas | Observaciones |
|------|------------------------|------------------|----------|---------------|
| 1 Instalar           | | pantalla + carpeta de datos                          | 1a 1b | |
| 2 Crear el centro    | | listas + prevalidación + §7.2                        | 2a–2h | |
| 3 Generar            | | diagnóstico en navegador + §7.3                      | 3a–3d | |
| 4 Ajustar (a, b, c, d) | | pantalla + diagnóstico + §7.3                      | 4a–4d | |
| 5 Exportar           | | §7.4 (siete rc=0)                                    | 5a 5b | |
| 6 Duplicar           | | pantalla + §7.5                                      | 6a–6e | |

Veredicto de la cadena: PASA / FALLA (si FALLA: paso, Cambio que lo arregla, y nueva corrida desde el paso 1)
```
