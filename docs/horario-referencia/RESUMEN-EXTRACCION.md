# Resumen de extracción

Extracción **determinista** (sin OCR) de los horarios de referencia del instituto a ficheros
estructurados y auditables en `docs/horario-referencia/`.

## Librería utilizada

- **pdfplumber 0.11.9** (Python), instalada en un entorno virtual aislado
  (`docs_extra/Ejemplos_SJ/.venv_pdf/`, no versionado).
- Comprobación previa de la capa de texto con `pdfinfo` / `pdftotext` (poppler).
- **No se usó OCR de imagen**: no fue necesario (ver abajo).

## ¿Tenían los PDF capa de texto?

**Sí, los tres.** Todos están generados por *Virtual Print Engine PDF* (texto vectorial
seleccionable, no escaneo). Caracteres extraídos en la página 1 de cada uno:

| Documento | Capa de texto | Caracteres pág. 1 |
|---|---|---|
| `Horarios de grupos.pdf` | Sí | 1792 |
| `Horarios de aulas.pdf` | Sí | 1294 |
| `Horario pequeño Aulas.pdf` | Sí | 3319 |

Al haber capa de texto fiable, **toda la extracción es determinista**: se leen las palabras con
sus coordenadas y se reconstruye la rejilla por geometría. No hay adivinación.

## Páginas procesadas por documento

| Documento | Páginas | Procesadas | Ficheros generados |
|---|---|---|---|
| `Horarios de grupos.pdf` | 28 | 28 | 28 × `grupo-*.json` |
| `Horarios de aulas.pdf` | 43 | 43 | 43 × `aula-*.json` |
| `Horario pequeño Aulas.pdf` | 6 | 0 (ver nota) | — |

**Total: 71 páginas procesadas, 0 fallos.**

### Páginas de aula sin clases (rejilla vacía)

10 de las 43 páginas de aula corresponden a espacios sin clases programadas en estos datos;
generan un `aula-*.json` con `"celdas": []` (no es un fallo de lectura, la rejilla está vacía
en el PDF — verificado):

`A19 TUTOR`, `B08`, `B09`, `B10`, `B11 Taller Tecnología`, `C02`, `C03`, `C04`,
`Taller 2`, `Taller 4`.

### Nota sobre `Horario pequeño Aulas.pdf`

No se usó para el volcado estructurado: es una **versión compacta y redundante** del mismo
horario por aulas (varias aulas por página, maquetación densa). El horario por aulas completo
(`Horarios de aulas.pdf`, una página por aula) es la fuente autorizada y ya cubre el
entregable 1. El PDF compacto tiene capa de texto y queda disponible por si se quiere un
tercer cotejo.

## Método de extracción (determinista)

1. **Rejilla por geometría.** Columnas de día (Lun–Vie) a partir de las líneas verticales de la
   tabla (x ≈ 100/172/244/316/388/460). Filas (tramos) a partir de las etiquetas de hora del
   margen izquierdo: se emparejan (inicio, fin) y los inicios en orden son
   8:00→T1, 9:00→T2, 10:00→T3, **11:00→recreo (se salta)**, 11:30→T4, 12:30→T5, 13:30→T6.
2. **Clasificación por leyenda.** Cada página trae su propia leyenda `Profesores:` y
   `Asignaturas:`; esos conjuntos de códigos son el **vocabulario autorizado** para clasificar
   cada token de celda. La columna interna desambigua asignatura (izq.) de profesor (der.) —
   necesario porque hay códigos compartidos (p.ej. `ECO` es a la vez asignatura *Economía* y
   código de profesor; ídem `FOPP`).
3. **Transcripción fiel.** Los `*.json` recogen el dato **tal cual** lo imprime el PDF, sin
   normalizar ni completar aulas implícitas. Si una celda omite el aula/grupo, se deja `null`.

## Control de calidad

- **Contabilidad de tokens:** en las 71 páginas, cada token de la región de rejilla queda
  asignado a exactamente una celda — **0 tokens perdidos, 0 duplicados**.
- **Cotejo semántico:** reconstrucción de páginas conocidas (grupo `1º ESO A`, aula `A1`)
  idéntica al volcado de texto del PDF, incluidas celdas multi-entrada (optativas) y multi-grupo.
- **Celdas dudosas:** **0**. Dada la capa de texto fiable + validación por leyenda + cuadre
  total de tokens, no hubo ninguna lectura ambigua. (El campo `confianza` queda en el esquema
  para marcar dudas si se reprocesaran otros PDF de peor calidad.)

## Formato de los ficheros de volcado

Cada `grupo-<código>.json` / `aula-<código>.json` tiene:

```json
{
  "_meta": { "fuente": "...", "pagina": N, "codigo_crudo": "1º ESO A", "modo": "grupos" },
  "celdas": [
    { "dia": 1, "tramo": 1, "asignatura": "PLAS", "profesor": "DIB1",
      "aula": "A5", "confianza": "alta", "nota": "" }
  ]
}
```

- En `grupo-*.json` el tercer campo es `"aula"`; en `aula-*.json` es `"grupos"`.
- Una celda con optativas/co-docencia genera **varios objetos** con el mismo `dia`/`tramo`
  (en estos datos la co-docencia se imprime como entradas separadas, una por profesor).
- Convención de reserva: si dos códigos de profesor compartieran una única entrada de
  asignatura, `profesor` los uniría con `/` (no ha ocurrido en este conjunto: 0 casos).
