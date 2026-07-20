import { Penalizacion, Violacion } from '../models/diagnostico.model';
import { clavePin } from './pines';

/**
 * Una violación vista DESDE UNA de sus celdas. No es espejo de ningún DTO: es
 * una forma derivada, y por eso vive aquí y no en `diagnostico.model.ts`.
 *
 * Existe porque una `Violacion` con N celdas se indexa N veces, y bajo cada
 * clave hay que saber con qué `plazaCodigo` entró —el de ESA celda, no el de la
 * primera—: es lo que permite distinguir después un resalte de sub-entrada
 * (`SOLAPE_AULA`, plaza no-null) de uno de celda entera (plaza null).
 */
export interface ViolacionEnCelda {
  violacion: Violacion;
  /** El `plazaCodigo` de la celda bajo la que se indexó esta entrada. */
  plazaCodigo: string | null;
}

/**
 * Índice de consulta O(1) de las violaciones duras por instancia. Una violación
 * de N celdas aparece bajo CADA una de ellas: la atribución es por celda, no por
 * violación, y la misma violación puede tocar instancias distintas.
 */
export function indiceViolaciones(
  violaciones: readonly Violacion[],
): Map<string, ViolacionEnCelda[]> {
  const indice = new Map<string, ViolacionEnCelda[]>();
  for (const violacion of violaciones) {
    for (const celda of violacion.celdas) {
      const clave = clavePin(celda.actividadCodigo, celda.indice);
      const entradas = indice.get(clave) ?? [];
      entradas.push({ violacion, plazaCodigo: celda.plazaCodigo });
      indice.set(clave, entradas);
    }
  }
  return indice;
}

/**
 * Índice de consulta O(1) de las penalizaciones blandas por instancia. Agrupa y
 * nada más: SIN sumatorio de `delta` —el sumatorio es presentación, y tenerlo
 * aquí invitaría a contrastarlo con `Totales`, que por contrato no tiene por qué
 * cuadrar—.
 */
export function indicePenalizaciones(
  penalizaciones: readonly Penalizacion[],
): Map<string, Penalizacion[]> {
  const indice = new Map<string, Penalizacion[]>();
  for (const penalizacion of penalizaciones) {
    const clave = clavePin(penalizacion.actividadCodigo, penalizacion.indice);
    const entradas = indice.get(clave) ?? [];
    entradas.push(penalizacion);
    indice.set(clave, entradas);
  }
  return indice;
}
