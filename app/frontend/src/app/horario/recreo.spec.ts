import { TramoJornadaDTO } from '../models/jornada.model';
import { recreoTrasTramo } from './recreo';

function tramo(dia: string, orden: number, esLectivo: boolean, ordenEnDia: number | null): TramoJornadaDTO {
  return { dia, orden, esLectivo, ordenEnDia, horaInicio: '08:00', horaFin: '09:00' };
}

/** Malla de referencia del centro real: 6 lectivos con el recreo en cuarta posición. */
const LUNES: TramoJornadaDTO[] = [
  tramo('LUNES', 1, true, 1),
  tramo('LUNES', 2, true, 2),
  tramo('LUNES', 3, true, 3),
  tramo('LUNES', 4, false, null),
  tramo('LUNES', 5, true, 4),
  tramo('LUNES', 6, true, 5),
  tramo('LUNES', 7, true, 6),
];

describe('posición de la fila de recreo', () => {
  it('(1) la malla del centro pone el recreo tras el tercer tramo lectivo', () => {
    expect(recreoTrasTramo(LUNES)).toBe(3);
  });

  /**
   * Cuenta LECTIVOS, no posiciones globales. En la malla del centro el recreo está
   * en `orden` 4 y va tras el lectivo 3, así que `orden - 1` acertaría por
   * casualidad —y también acierta si sólo se desplaza el recreo, porque el `orden`
   * se desplaza con él—. Lo que rompe esa confusión es que el `orden` NO empiece en
   * 1: aquí el día arranca en 8, como el martes de una semana real, y `orden - 1`
   * diría 10 en vez de 3.
   */
  it('(2) cuenta LECTIVOS, no posiciones globales: el orden del día no empieza en 1', () => {
    const martes: TramoJornadaDTO[] = [
      tramo('MARTES', 8, true, 1),
      tramo('MARTES', 9, true, 2),
      tramo('MARTES', 10, true, 3),
      tramo('MARTES', 11, false, null),
      tramo('MARTES', 12, true, 4),
    ];
    expect(recreoTrasTramo(martes)).toBe(3);
  });

  it('(3) sin tramos, o sin ninguno no lectivo, no hay fila que pintar', () => {
    expect(recreoTrasTramo([])).toBeNull();
    expect(recreoTrasTramo(LUNES.filter((t) => t.esLectivo))).toBeNull();
  });

  it('(4) un no lectivo antes de la primera clase no es un recreo', () => {
    const tarde = [tramo('LUNES', 1, false, null), ...LUNES.filter((t) => t.esLectivo)];
    expect(recreoTrasTramo(tarde)).toBeNull();
  });

  it('(5) sólo mira el primer día: la jornada es idéntica los cinco', () => {
    // El martes lleva el recreo en otro sitio, cosa que el backend no produce. Si
    // se colara, el resultado NO debe depender de él: la rejilla pinta una fila.
    const martes = [
      tramo('MARTES', 8, true, 1),
      tramo('MARTES', 9, false, null),
    ];
    expect(recreoTrasTramo([...LUNES, ...martes])).toBe(3);
  });

  /**
   * Con la malla desordenada —el JSON no promete orden— el resultado no cambia:
   * la función ordena por `orden` antes de contar. Sin ese `sort`, este caso
   * devolvería 1, porque el recreo aparecería el segundo de la lista.
   */
  it('(6) la malla llega desordenada y el resultado no cambia', () => {
    const revuelto = [LUNES[0], LUNES[3], LUNES[1], LUNES[2], LUNES[4]];
    expect(recreoTrasTramo(revuelto)).toBe(3);
  });
});
