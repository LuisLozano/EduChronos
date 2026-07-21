import { Bloqueo } from '../models/bloqueo.model';

/**
 * Clave estable de un pin. La identidad de un bloqueo es la INSTANCIA, el par
 * (`actividadCodigo`, `indice`) —D-6—, no la actividad: una actividad de tres
 * repeticiones puede tener pinada solo una de ellas.
 */
export function clavePin(actividadCodigo: string, indice: number): string {
  return `${actividadCodigo}|${indice}`;
}

/**
 * Índice de consulta O(1) de las instancias pinadas de una lista de bloqueos.
 * El valor es el `id` del bloqueo —lo que el DELETE necesita—, y es `number |
 * null` porque el DTO lo declara nullable de verdad (ver `Bloqueo.id`): una
 * clave presente con valor `null` es un pin VIVO que no se sabe borrar, no la
 * ausencia de pin. Distinguirlo es de quien tenga el servicio, no de aquí.
 */
export function indicePines(bloqueos: readonly Bloqueo[]): Map<string, number | null> {
  return new Map(bloqueos.map((b) => [clavePin(b.actividadCodigo, b.indice), b.id]));
}
