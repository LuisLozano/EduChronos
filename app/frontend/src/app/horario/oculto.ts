/**
 * Cuántas plazas de una instancia quedan ocultas por el recorte de la celda.
 *
 * Es la mitad que faltaba de D11: el tramo 1 (S126) impuso la altura con
 * `max-height` + `overflow: hidden` en `.celda` y el recorte quedó MUDO. Esta
 * función dice cuántas líneas de plaza no se ven, para que la rejilla pueda
 * marcarlo con el mismo molde `+N` que D6 ya usa.
 *
 * La regla NO es un umbral en píxeles: una plaza cuenta como oculta cuando se
 * ve MENOS DE LA MITAD de su alto. Un umbral literal habría que reajustarlo a
 * mano en cuanto cambien tipografía o padding, que es la trampa que D1 evita
 * derivando la altura en vez de escribirla.
 *
 * Consecuencia medida en S126, y la razón de que la regla sea una fracción: la
 * celda de cuatro plazas se pasa 0,8 px del hueco (110,8 pide, 110 hay). Con
 * «cualquier parte oculta cuenta» se marcarían 22 celdas que no esconden nada
 * legible, que es poner una señal falsa al lado de la marca `+N` verdadera.
 */

/** Fracción mínima de una plaza que debe verse para NO contarla como oculta. */
export const FRACCION_VISIBLE_MINIMA = 0.5;

/** Una línea de plaza, medida sobre el DOM y relativa al borde superior del recorte. */
export interface LineaDePlaza {
  /** Borde superior de la línea, en px. */
  arriba: number;
  /** Borde inferior de la línea, en px. */
  abajo: number;
}

/**
 * @param lineas líneas de plaza en orden de pintado, medidas contra el borde
 *        superior de la zona recortada.
 * @param altoDisponible alto visible de la celda en px (el `--alto-celda` que
 *        publica `repartirAltura`).
 * @returns número de plazas ocultas, o `null` si todavía no hay alto medido.
 */
export function plazasOcultas(
  lineas: readonly LineaDePlaza[],
  altoDisponible: number | null,
): number | null {
  if (altoDisponible === null || !Number.isFinite(altoDisponible) || altoDisponible <= 0) {
    return null;
  }

  let ocultas = 0;
  for (const linea of lineas) {
    const alto = linea.abajo - linea.arriba;
    if (alto <= 0) {
      // Guarda de INTENCIÓN, no rama necesaria: ninguna mutación la discrimina.
      // Sin ella, una línea de alto 0 da 0/0 = NaN y `NaN < 0.5` es false, con
      // lo que tampoco se contaría. Se conserva a propósito para que la
      // corrección no dependa de cómo compara NaN. Medido en el barrido de
      // mutación de S127: sobrevive a la mutación y NO se le escribe un test
      // que aparente sujetarla.
      continue;
    }
    const visible = Math.min(altoDisponible, linea.abajo) - linea.arriba;
    // Sin clamp a cero: con `visible` negativo la fracción sale negativa y
    // queda por debajo del umbral igual. El `Math.max(0, …)` que hubo aquí era
    // INERTE —no cambiaba ningún veredicto— y se borró en vez de blindarlo,
    // mismo criterio que S124.
    const fraccion = visible / alto;
    if (fraccion < FRACCION_VISIBLE_MINIMA) {
      ocultas++;
    }
  }
  return ocultas;
}
