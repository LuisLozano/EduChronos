/**
 * Modelos TS del contrato de proyección de Fase 7 (Bloque 7A). Reflejan campo a
 * campo los records de `app.web.dto`: SesionVistaDTO y HorarioProyeccionDTO.
 */

/** Espejo de `SesionVistaDTO`. Proyección plana de una sesión colocada. */
export interface SesionVista {
  sesionId: number;
  indice: number;
  /** 1..5 (lunes..viernes). */
  dia: number;
  /** ordenEnDia 1..6 (recreos excluidos). */
  tramo: number;
  asignaturaCodigo: string;
  asignaturaNombre: string;
  /** Co-docencia: varios profesores en UNA entrada (D-F7-2). */
  profesores: string[];
  /** Nunca null: `Sesion.aula` es optional=false (D-F7B-6). */
  aulaCodigo: string;
  subgrupos: string[];
  /** Unión sin duplicados de los grupos de los subgrupos de la plaza (D-F7-1). */
  grupos: string[];
  actividadCodigo: string;
  plazaCodigo: string;
}

/** Espejo de `HorarioProyeccionDTO`. Cabecera + sesiones. */
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
}
