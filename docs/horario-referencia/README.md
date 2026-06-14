# Horario de referencia — documentación

Esta carpeta contiene la **transcripción estructurada y auditable** del horario
real del instituto de referencia (IES de Sevilla), usada para validar el modelo
de datos y el solver de Educhronos. **No es el diseño de la aplicación**: son
datos reales de un centro concreto que sirven como banco de pruebas.

---

## 1. Jerarquía de autoridad (regla de oro)

Cuando dos fuentes discrepen, **gana la de más arriba**:

```
PDF (origen inmutable)
   │   los tres PDF generados por el centro. No se editan nunca.
   ▼
Volcado fiel JSON  (grupo-*.json / aula-*.json)
   │   transcripción LITERAL del PDF, sin interpretar, auditable.
   │   Si discrepa del PDF → gana el PDF y se corrige el volcado.
   ▼
modelo_datos_fase1.md §6.x  (interpretación con criterio)
       lectura semántica: agrupamientos por parejas, aulas implícitas
       resueltas, Lectura A vs B, etc. Nunca contradice al volcado sin
       una nota que explique la interpretación.
```

Consecuencia práctica: **para construir un fixture del solver, la fuente es el
volcado fiel JSON, NO el OCR de los PDF ni la memoria.** Los volcados son
deterministas (ver §4); el OCR fragmentado no es fiable (episodio documentado en
Sesión 19).

---

## 2. Inventario

### Fuente primaria (en la raíz del Project / repo)
- `Horarios_de_grupos.pdf` — una página por grupo (28 grupos).
- `Horarios_de_aulas.pdf` — una página por aula (43 espacios).
- `Horario_pequeño_Aulas.pdf` — versión compacta del horario por aulas; no se
  usó para el volcado (redundante), disponible para un tercer cotejo.

### Volcado fiel (esta carpeta)
- `grupo-<código>.json` — 28 ficheros, uno por grupo. Cada celda:
  `{ dia, tramo, asignatura, profesor, aula, confianza, nota }`.
- `aula-<código>.json` — 43 ficheros, uno por aula. Igual, pero el campo de
  contexto es `grupos` en vez de `aula`. 10 corresponden a espacios sin clases
  (`"celdas": []`); no es un fallo de lectura, la rejilla está vacía en el PDF.

### Reconciliación y procedencia (esta carpeta)
- `INFORME-RECONCILIACION.md` — cruce determinista grupos↔aulas por
  `(grupo, día, tramo, profesor)`. Reporta aulas omitidas, co-docencia
  divergente, discrepancias de ubicación, celdas dudosas y grupos no mapeables.
- `RESUMEN-EXTRACCION.md` — cómo se generó el volcado: librería, comprobación de
  capa de texto, páginas procesadas, control de calidad. Contiene también la
  especificación del formato de los JSON (no se duplica aquí).

---

## 3. Cómo usar estos ficheros

- **Construir un fixture de un nivel** → leer los `grupo-*.json` de ese nivel y,
  para confirmar agrupamientos multi-grupo (Religión por parejas, RefMt
  transversal, optativas), cruzar con el `aula-*.json` correspondiente (que
  lista qué grupos comparten una celda). Cada dato del fixture debe quedar
  verificado contra el volcado fiel antes de afirmarse como "real".
- **Antes de confiar en una celda** → mirar el `INFORME-RECONCILIACION.md`:
  dice qué celdas tienen aula omitida (se resuelve por la convención del centro,
  no se inventa), qué co-docencia no se imprime en una de las fuentes, y qué
  grupos no se pueden mapear automáticamente entre fuentes.
- **Convención de rejilla temporal** (ver RESUMEN §método): tramos T1..T6 por
  día saltando el recreo. T1=8-9, T2=9-10, T3=10-11, [recreo 11-11:30],
  T4=11:30-12:30, T5=12:30-13:30, T6=13:30-14:30.

---

## 4. Garantías de la extracción

- **Sin OCR.** Los tres PDF tienen capa de texto vectorial (Virtual Print
  Engine). La extracción es por geometría con pdfplumber; se leen las palabras
  con sus coordenadas. No hay adivinación.
- **Cuadre total de tokens:** cada token de la región de rejilla queda asignado
  a exactamente una celda en las 71 páginas — 0 perdidos, 0 duplicados.
- **0 celdas dudosas** y **0 discrepancias de ubicación** (informe §3, §4).
- **Coherencia de asignaturas:** 1205 cruces grupos↔aulas, 0 divergencias.

---

## 5. Limitaciones conocidas (a tener en cuenta al escalar)

- **FPB sin cobertura de aulas.** El horario por aulas no cubre los talleres de
  FPB; 65 celdas (mayoría FPB, más la co-docencia de LCL de 1ºESO) tienen aula
  omitida en el horario por grupos y sin localizar en el de aulas
  (informe §1). Al modelar FPB habrá que decidir las aulas por otra vía.
- **`3ºPDC` ↔ `3ºCDi` no mapeable.** El horario por grupos tiene una página
  genérica `3º ESO PDC` (31 celdas) que no se puede igualar de forma
  determinista a `3ºCDi` del horario por aulas (31 celdas). Reconciliación
  manual pendiente al abordar 3ºESO. Los demás PDC (3ºA/B, 4ºA/D) sí mapean.
- **Optativas de Bachillerato = Lectura B.** En `aula-A3.json` aparece una celda
  `LU` (LEN1) con grupos `1B-C 1B-D`: subgrupos de 1ºBachillerato mezclados en
  una optativa. Es el primer indicio en datos fieles de un subgrupo cuya
  población son alumnos de varios grupos ("Lectura B", `SubgrupoGrupo` N:M), que
  el modelo de dominio actual NO soporta (un `Subgrupo` pertenece a un único
  grupo). A resolver al abordar el bloque de Bachillerato en Fase 5.

---

## 6. Procedencia

Volcado e informe generados en la Sesión 19 con Claude Code (extracción
determinista, sin OCR). Verificación de uso: el bloque Religión/ATED de 1ºESO
fue verificado plaza a plaza (6/6) contra este volcado para el fixture
`problema-5-religion-parejas-1eso.json` del solver (Fase 5, Bloque 1).
