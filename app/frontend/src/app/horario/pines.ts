import { Bloqueo } from '../models/bloqueo.model';
import { SesionVista } from '../models/horario.model';

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

/**
 * La primera fila de la instancia cuya CLAVE es `clave`, o `undefined` si esa
 * instancia no está en la lista.
 *
 * <p>Existe porque poner un pin necesita el TRAMO —el cuerpo del POST lo lleva—, y
 * la rejilla emite solo la clave: quien tiene la proyección es el contenedor. Se
 * busca comparando la clave RECONSTRUIDA con {@link clavePin}, nunca partiendo la
 * recibida por su separador: `actividadCodigo` es texto del catálogo y nada impide
 * que algún día contenga una barra, y un `split('|')` la trocearía en silencio.
 *
 * <p>Devuelve la PRIMERA fila y no todas: las de una instancia comparten tramo por
 * construcción —se mueven juntas o no se mueven—, así que cualquiera sirve para
 * leerlo. La misma razón por la que `alSoltar` toma `inst.entradas[0]`.
 */
export function filaDeClave(
  sesiones: readonly SesionVista[],
  clave: string,
): SesionVista | undefined {
  return sesiones.find((s) => clavePin(s.actividadCodigo, s.indice) === clave);
}
