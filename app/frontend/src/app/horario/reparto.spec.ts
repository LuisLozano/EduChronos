import { CROMO_FILA, altoDeCelda } from './reparto';

describe('reparto de altura de la rejilla', () => {
  /**
   * Cifras MEDIDAS en navegador el 31/08/2026 (Firefox, viewport 1920×887, DPR 1)
   * sobre la demo del centro real: hueco 716, thead 30, recreo 24, 6 tramos.
   *
   * <p>`thead` (30) y recreo (24) salieron EXACTOS. El hueco NO: los 748 eran un
   * cálculo previo y el navegador da 716 —32 px menos—, así que la fila baja de los
   * 115 px predichos a 110 y el escalón de las cuatro plazas queda cruzado por 0,8 px.
   *
   * <p>A este viewport se recortan 72 celdas de 791 (9,1 %), no 50. El 50 era una
   * PREDICCIÓN, no un requisito: el criterio 4 exige AUSENCIA DE SCROLL —verificada
   * en 1ºA, 4ºA y 1B-A—, no un porcentaje concreto, y el 9,1 % queda por debajo del
   * umbral del ~10 % a partir del cual habría que rediscutirlo.
   */
  it('(1) el reparto medido da 110 px de fila: bajo el escalón de 4 plazas, sobre el de 3', () => {
    const alto = altoDeCelda(716, 30, 24, 6)!;

    expect(alto + CROMO_FILA).toBe(110);
    // Por DEBAJO de 110,8: la celda de cuatro plazas ya no entra. Es el escalón que
    // se cruzó, y la razón de que sean 72 celdas y no 50.
    expect(alto + CROMO_FILA).toBeLessThan(110.8);
    // Y por ENCIMA de 89,1, el alto de la celda de tres plazas. ÉSTE es el que hay
    // que vigilar: cruzarlo suma las 37 celdas de 3 plazas del centro y lleva el
    // recorte a 109 de 791 (13,8 %), ya por encima del umbral del criterio 4.
    expect(alto + CROMO_FILA).toBeGreaterThan(89.1);
  });

  it('(2) el recreo cuesta altura: sin su fila, las seis reciben más', () => {
    // Sin esto, ignorar el parámetro del recreo pasaría el (1).
    expect(altoDeCelda(748, 30, 0, 6)!).toBeGreaterThan(altoDeCelda(748, 30, 24, 6)!);
  });

  it('(3) sin medida útil no hay tope: null, nunca un número inventado', () => {
    // jsdom: todas las alturas son 0. Es el caso de la suite de unidad entera.
    expect(altoDeCelda(0, 0, 0, 6)).toBeNull();
    // Hueco menor que el cromo: un negativo topando la celda la dejaría invisible.
    expect(altoDeCelda(60, 30, 24, 6)).toBeNull();
    expect(altoDeCelda(748, 30, 24, 0)).toBeNull();
  });
});
