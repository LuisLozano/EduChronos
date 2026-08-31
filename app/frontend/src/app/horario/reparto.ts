/**
 * Lo que gasta una FILA de la rejilla por encima de su contenido: 8 px de padding
 * del `td` (0.25rem × 2, el `PAD_CELDA` del instrumento) más 1 px de borde
 * colapsado. Es la traducción entre «alto de fila», que es lo que el presupuesto
 * reparte, y `--alto-celda`, que topa el envoltorio de dentro de la celda.
 */
export const CROMO_FILA = 9;

/**
 * Tope de altura de una celda (D1), o `null` si no hay medida útil.
 *
 * <p>El alto NO se elige: se DERIVA del hueco que el flex deja a la rejilla, que
 * ya lleva descontado todo lo que hay encima —barra, padding del contenido,
 * cabecera fundida, panel de prevalidación y cualquier aviso que aparezca—. Por eso
 * esta función no enumera el cromo de la vista: no lo necesita.
 *
 * <p>Devuelve `null` sin medida —jsdom no hace layout y todas las alturas son 0—,
 * y ése es el motivo de que la suite de unidad no vea recorte alguno: sin tope, la
 * rejilla se comporta como antes de D1.
 */
export function altoDeCelda(
  hueco: number,
  altoThead: number,
  altoRecreo: number,
  filas: number,
): number | null {
  if (filas <= 0) {
    return null;
  }
  const paraFilas = hueco - altoThead - altoRecreo;
  const alto = Math.floor(paraFilas / filas) - CROMO_FILA;
  return alto > 0 ? alto : null;
}
