import { CROMO_FILA, altoDeCelda } from './reparto';

describe('reparto de altura de la rejilla', () => {
  /**
   * Las cifras son las MEDIDAS en el viewport de verificación (1920×887, Firefox):
   * hueco 748, thead 30, recreo 24. El instrumento da 115,73 px de alto de fila
   * para ese reparto, con 50 celdas de 791 recortadas — las de 5 y 6 plazas—, que es
   * la cifra del criterio 4.
   */
  it('(1) el reparto medido da 115 px de fila, por encima del umbral de 4 plazas', () => {
    const alto = altoDeCelda(748, 30, 24, 6)!;

    expect(alto + CROMO_FILA).toBe(115);
    // Una celda de 4 plazas pide 110,8 px de fila y debe SEGUIR entrando: si este
    // aserto cae, el recorte se ha disparado de 50 celdas a 72 y el acantilado se
    // ha cruzado.
    expect(alto + CROMO_FILA).toBeGreaterThan(110.8);
    // Y una de 5 pide 132,5: ésa sí se recorta, que es lo que D11 tendrá que explicar.
    expect(alto + CROMO_FILA).toBeLessThan(132.5);
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
