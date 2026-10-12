/**
 * Modelos TS del contrato de proyección de Fase 7 (Bloque 7A). Reflejan campo a
 * campo los records de `app.web.dto`: SesionVistaDTO, GuardiaVistaDTO (S215) y
 * HorarioProyeccionDTO.
 */

/** Espejo de `SesionVistaDTO`. Proyección plana de una sesión colocada. */
export interface SesionVista {
  sesionId: number;
  indice: number;
  /** 1..5 (lunes..viernes). */
  dia: number;
  /** ordenEnDia 1..6 (recreos excluidos). Es el tramo de INICIO. */
  tramo: number;
  /** Tramos que ocupa la sesión, >= 1. Espejo del campo `duracion` de `SesionVistaDTO`. */
  duracion: number;
  asignaturaCodigo: string;
  asignaturaNombre: string;
  /** Co-docencia: varios profesores en UNA entrada (D-F7-2). */
  profesores: string[];
  /** null cuando la plaza no tiene aula (reuniones y funciones, S201). */
  aulaCodigo: string | null;
  subgrupos: string[];
  /** Unión sin duplicados de los grupos de los subgrupos de la plaza (D-F7-1). */
  grupos: string[];
  actividadCodigo: string;
  plazaCodigo: string;
}

/**
 * Espejo de `GuardiaVistaDTO` (S215, 120089e; las de recreo, S221, 9990939). Una guardia
 * repartida: el profesor que está de guardia y dónde. Una ORDINARIA lleva su tramo, con la
 * MISMA numeración que {@link SesionVista}, y `recreo` null; una de RECREO o BIBLIOTECA lleva
 * el número de su recreo en el día, desde 1, y `tramo` null. Forma copiada del JSON real de
 * `GET /api/horarios/2/proyeccion` (s221/t2/exp/proyeccion-2.json):
 * `{"profesorCodigo": "BIO3t", "dia": 1, "tramo": 1, "recreo": null, "tipo": "ORDINARIA"}` y
 * `{"profesorCodigo": "BYG1", "dia": 1, "tramo": null, "recreo": 1, "tipo": "RECREO"}`.
 */
export interface GuardiaVista {
  profesorCodigo: string;
  /** 1..5 (lunes..viernes). */
  dia: number;
  /** ordenEnDia 1..6 (recreos excluidos) de una ORDINARIA; null en una de recreo. */
  tramo: number | null;
  /** Número del recreo en el día, desde 1, de una de RECREO o BIBLIOTECA; null en una ORDINARIA. */
  recreo: number | null;
  /** `ORDINARIA`, `RECREO` o `BIBLIOTECA` (el `name()` de `TipoGuardia`). */
  tipo: string;
}

/** Espejo de `HorarioProyeccionDTO`. Cabecera + sesiones + guardias. */
export interface HorarioProyeccion {
  id: number;
  nombre: string;
  estado: string;
  estadoSolver: string;
  /** Double nullable real: 0.0 es válido, null es "no medido". */
  objetivo: number | null;
  cotaInferior: number | null;
  /** Instant ISO-8601 serializado como texto. */
  fechaGeneracion: string;
  sesiones: SesionVista[];
  /**
   * Las ORDINARIA por (dia, tramo, profesorCodigo) y, detrás, las de recreo por (dia, recreo,
   * tipo, profesorCodigo); `[]` si el horario no tiene. Nunca null.
   */
  guardias: GuardiaVista[];
}
