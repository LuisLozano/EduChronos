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

/**
 * Suma CON SIGNO de los delta blandos por instancia (clave de {@link clavePin}).
 * Hermana de {@link indicePenalizaciones}, pero AGREGA en vez de agrupar: es lo
 * que alimenta el badge, un único número por instancia. Vive aquí, en la capa
 * pura, y no en el contenedor —el componente orquesta señales, no suma—.
 *
 * <p>El número NO es `Totales` y NO tiene por qué cuadrar con él: los `Totales`
 * son conteos SIN signo del coste actual y estos son deltas CONTRAFACTUALES con
 * signo (ver javadoc de `TotalesDTO`). Contrastarlos es la trampa del contrato,
 * no un descuadre que arreglar.
 *
 * <p>C2/S65: una instancia cuyos delta suman 0 NO se emite. Un delta 0 es
 * indiferente y el backend ya no lo manda; una instancia indiferente EN AGREGADO
 * (sus delta se cancelan) tendría un badge que promete un coste que no existe.
 * Por eso la clave se descarta, y el predicado del consumidor es `has(clave)`,
 * sin comparar con 0. El filtrado es una SEGUNDA pasada a propósito: el signo
 * puede cancelarse a mitad de la suma, así que solo el total decide.
 */
export function sumaDeltasPorInstancia(
  penalizaciones: readonly Penalizacion[],
): Map<string, number> {
  const sumas = new Map<string, number>();
  for (const penalizacion of penalizaciones) {
    const clave = clavePin(penalizacion.actividadCodigo, penalizacion.indice);
    sumas.set(clave, (sumas.get(clave) ?? 0) + penalizacion.delta);
  }
  for (const [clave, suma] of sumas) {
    if (suma === 0) {
      sumas.delete(clave);
    }
  }
  return sumas;
}
