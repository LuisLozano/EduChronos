import { CuadreEntidad } from '../models/prevalidacion.model';

/**
 * Texto de la columna «Horas» de las listas de profesores y grupos (S203, C-totales-y-cargo).
 * Lógica pura, hermana de `busqueda.ts`: recibe la entrada del cuadre que el backend ya
 * calculó y solo la pinta. El frontend no recalcula nada.
 *
 * <p>Con total declarado, «{configuradas} de {declaradas}»; sin él, solo «{configuradas}». El
 * descuadre se dice con TEXTO, {@link TEXTO_NO_CUADRA} tras la cifra, y además con color
 * (clase `horas--no-cuadra` en la celda): la regla de severidad de `styles.css` pide que el
 * dato no viaje solo por color.
 */
export const TEXTO_NO_CUADRA = ' (no cuadra)';

/** El texto de la celda; vacío si no hay entrada (el GET de cuadre falló o no la trae). */
export function textoHoras(entrada: CuadreEntidad | undefined): string {
  if (entrada === undefined) {
    return '';
  }
  const cifra =
    entrada.declaradas === null
      ? `${entrada.configuradas}`
      : `${entrada.configuradas} de ${entrada.declaradas}`;
  return entrada.descuadre ? cifra + TEXTO_NO_CUADRA : cifra;
}
