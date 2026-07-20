/**
 * Modelos TS del contrato de diagnóstico (Fase 8, Bloque 8.3-C). Reflejan campo a
 * campo los records de `app.web.dto`: CeldaRefDTO, ViolacionDTO, PenalizacionDTO,
 * TotalesDTO y DiagnosticoDTO.
 *
 * La clave de cruce con `SesionVista` es la misma que la de los bloqueos, el par
 * (`actividadCodigo`, `indice`) —D-6—: por eso no se toca `horario.model.ts`, que
 * declara reflejar solo el contrato de proyección de Fase 7.
 *
 * `Violacion` y `Penalizacion` NO tienen la misma forma, y la asimetría es
 * deliberada (D15): se copia tal cual, no se normaliza.
 */

/**
 * Espejo de `CeldaRefDTO`. Una celda implicada en una {@link Violacion}.
 */
export interface CeldaRef {
  actividadCodigo: string;
  indice: number;
  /** Nullable de verdad: no-null solo en `SOLAPE_AULA` (el aula se cuenta por plaza). */
  plazaCodigo: string | null;
}

/**
 * Espejo de `ViolacionDTO`. Una restricción DURA incumplida, atribuida a N celdas.
 */
export interface Violacion {
  /** Nombre del enum `ReglaDura`. String pelado: estrechar a unión de literales afirmaría más que el servidor. */
  regla: string;
  /** Null cuando la regla no habla de un recurso concreto. */
  recursoCodigo: string | null;
  /** Null cuando la regla no habla de un tramo concreto. */
  tramoCodigo: string | null;
  celdas: CeldaRef[];
  descripcion: string;
}

/**
 * Espejo de `PenalizacionDTO`. La aportación CONTRAFACTUAL de una celda a un
 * término blando del objetivo.
 *
 * NO lleva `plazaCodigo`, ni siquiera opcional: la atribución blanda es POR
 * INSTANCIA y el DTO aplana la celda a (`actividadCodigo`, `indice`) a propósito
 * —un campo siempre null es un campo que miente—. Por eso no anida un
 * {@link CeldaRef}.
 */
export interface Penalizacion {
  /** Nombre del enum de la regla blanda. String pelado, igual que en {@link Violacion}. */
  regla: string;
  actividadCodigo: string;
  indice: number;
  /** Nullable: no-null solo en `INDISPONIBILIDAD_BLANDA`. */
  tramoCodigo: string | null;
  /** LLEVA SIGNO: `>0` mover la celda mejora, `<0` la celda tapa un hueco. */
  delta: number;
}

/**
 * Espejo de `TotalesDTO`. Totales blandos del horario entero.
 *
 * Son conteos SIN signo del coste ACTUAL, y NO tienen por qué coincidir con la
 * suma de los `delta` contrafactuales de {@link Penalizacion}: contrastarlos es
 * la trampa obvia de este contrato. Los tres son `int` en Java, nunca null:
 * ensanchar la nullabilidad sería tan infiel como estrecharla.
 */
export interface Totales {
  ventanas: number;
  consecutivas: number;
  indispBlanda: number;
}

/** Espejo de `DiagnosticoDTO`. Duras atribuidas + blandas atribuidas + totales. */
export interface Diagnostico {
  violaciones: Violacion[];
  penalizaciones: Penalizacion[];
  totales: Totales;
}
