import { Bloqueo } from '../models/bloqueo.model';

/**
 * Clave estable de un pin. La identidad de un bloqueo es la INSTANCIA, el par
 * (`actividadCodigo`, `indice`) —D-6—, no la actividad: una actividad de tres
 * repeticiones puede tener pinada solo una de ellas.
 */
export function clavePin(actividadCodigo: string, indice: number): string {
  return `${actividadCodigo}|${indice}`;
}

/** Índice de consulta O(1) de las instancias pinadas de una lista de bloqueos. */
export function indicePines(bloqueos: readonly Bloqueo[]): Set<string> {
  return new Set(bloqueos.map((b) => clavePin(b.actividadCodigo, b.indice)));
}
