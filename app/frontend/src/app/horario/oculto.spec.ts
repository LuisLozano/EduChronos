import { plazasOcultas, type LineaDePlaza } from './oculto';

/**
 * Geometría de apoyo. El escalón de ~21,7 px por plaza sale de la tabla de
 * alturas de `diseno-navegacion.md` §4 (62,9 / 67,4 / 89,1 / 110,8 / 132,5 /
 * 154,2), VALIDADA contra navegador en S126 con desviación menor de 1 px.
 * Aquí es ilustrativa: lo que esta suite prueba es la REGLA. Los fondos reales
 * los mide el DOM en el commit 2.
 */
const PASO = 21.7;
const RÓTULO = 16;

function lineas(n: number, paso = PASO, desde = RÓTULO): LineaDePlaza[] {
  return Array.from({ length: n }, (_, i) => ({
    arriba: desde + i * paso,
    abajo: desde + (i + 1) * paso,
  }));
}

describe('plazasOcultas', () => {
  it('(1) sin recorte: todas las líneas caben y no oculta ninguna', () => {
    expect(plazasOcultas(lineas(3), 200)).toBe(0);
  });

  it('(2) el caso de S126: la celda de cuatro plazas se pasa 0,8 px y NO cuenta como oculta', () => {
    // 16 + 4×21,7 = 102,8 de contenido contra 102 de hueco: se ve el 96 % de la última.
    expect(plazasOcultas(lineas(4), 102)).toBe(0);
  });

  it('(3) la celda de cinco plazas esconde una entera', () => {
    expect(plazasOcultas(lineas(5), 102)).toBe(1);
  });

  it('(4) la celda de seis plazas esconde dos', () => {
    expect(plazasOcultas(lineas(6), 102)).toBe(2);
  });

  // (5) y (6) usan LITERALES a propósito. Derivar la entrada de
  // FRACCION_VISIBLE_MINIMA hacía que los dos casos se movieran con la
  // constante que pretenden proteger: con el umbral a 1 seguían pasando.
  // Medido en el barrido de mutación de S127.
  it('(5) la frontera: exactamente la mitad visible NO se cuenta como oculta', () => {
    const media = [{ arriba: 0, abajo: 20 }];
    expect(plazasOcultas(media, 10)).toBe(0);
  });

  it('(6) un pelo por debajo de la mitad SÍ se cuenta como oculta', () => {
    const media = [{ arriba: 0, abajo: 20 }];
    expect(plazasOcultas(media, 9.99)).toBe(1);
  });

  it('(7) sin alto medido devuelve null, no cero', () => {
    expect(plazasOcultas(lineas(6), null)).toBeNull();
    expect(plazasOcultas(lineas(6), 0)).toBeNull();
    expect(plazasOcultas(lineas(6), Number.NaN)).toBeNull();
  });

  it('(8) sin líneas no oculta nada', () => {
    expect(plazasOcultas([], 102)).toBe(0);
  });

  it('(9) una línea de alto cero no esconde nada aunque quede fuera del hueco', () => {
    expect(plazasOcultas([{ arriba: 300, abajo: 300 }], 102)).toBe(0);
  });

  it('(10) una línea que empieza fuera del hueco cuenta como oculta', () => {
    expect(plazasOcultas([{ arriba: 300, abajo: 321.7 }], 102)).toBe(1);
  });
});
