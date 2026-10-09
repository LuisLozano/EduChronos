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
 * Espejo de `GuardiaVistaDTO` (S215, 120089e). Una guardia ordinaria repartida: el
 * profesor que está de guardia y su tramo, con la MISMA numeración que {@link SesionVista}.
 * Forma copiada del JSON real de `GET /api/horarios/2/proyeccion` (s215/t2/val/real/h2):
 * `{"profesorCodigo": "BIO3t", "dia": 1, "tramo": 1}`.
 */
export interface GuardiaVista {
  profesorCodigo: string;
  /** 1..5 (lunes..viernes). */
  dia: number;
  /** ordenEnDia 1..6 (recreos excluidos). */
  tramo: number;
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
  /** Ordenadas por (dia, tramo, profesorCodigo); `[]` si el horario no tiene. Nunca null. */
  guardias: GuardiaVista[];
}
