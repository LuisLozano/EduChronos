import { clavePin, indicePines } from './pines';
import { Bloqueo } from '../models/bloqueo.model';

/**
 * Bloqueo mínimo: solo importan la actividad, el índice y el id (sin pines de
 * aula). `id` es `number | null` como el DTO: estrecharlo aquí impediría
 * construir el fixture del pin sin id, que es un estado que el contrato admite.
 */
function pin(
  id: number | null,
  actividadCodigo: string,
  indice: number,
  dia: number,
  orden: number,
): Bloqueo {
  return { id, actividadCodigo, indice, tramo: { dia, orden }, aulas: [] };
}

/**
 * Dos repeticiones de la MISMA actividad pinadas en tramos distintos: el patrón
 * real de Mat-1ºA (1 plaza, 3 repeticiones), del que el usuario fija dos.
 */
const DOS_REPETICIONES: Bloqueo[] = [
  pin(1, 'Mat-1ºA', 1, 1, 2),
  pin(2, 'Mat-1ºA', 2, 2, 2),
];

describe('índice de pines', () => {
  it('(1) la identidad es la INSTANCIA: dos repeticiones de la misma actividad son dos pines', () => {
    const indice = indicePines(DOS_REPETICIONES);

    // Discriminante doble: agrupar solo por actividadCodigo daría tamaño 1, y
    // el emparejamiento clave→id fija QUÉ id acompaña a QUÉ instancia, no solo
    // que ambos estén presentes.
    expect(indice.size).toBe(2);
    expect(indice).toEqual(
      new Map([
        [clavePin('Mat-1ºA', 1), 1],
        [clavePin('Mat-1ºA', 2), 2],
      ]),
    );
  });

  it('(2) el índice reconoce la instancia pinada, NO sus hermanas sin pinar, y devuelve su id', () => {
    const indice = indicePines(DOS_REPETICIONES);

    expect(indice.has(clavePin('Mat-1ºA', 1))).toBe(true);
    expect(indice.has(clavePin('Mat-1ºA', 3))).toBe(false);
    expect(indice.has(clavePin('Bloque-CyR_OyD_RefMt-1ESO', 1))).toBe(false);

    // La dimensión NUEVA: la consulta devuelve el id que el DELETE necesita.
    expect(indice.get(clavePin('Mat-1ºA', 2))).toBe(2);
    // Ausente es `undefined`, no `null`: la distinción que (4) explota.
    expect(indice.get(clavePin('Mat-1ºA', 3))).toBeUndefined();
  });

  it('(3) sin bloqueos el índice está vacío', () => {
    expect(indicePines([]).size).toBe(0);
  });

  /**
   * Un pin sin id es un pin VIVO: pinta candado igual que los demás. Descartarlo
   * del índice lo volvería invisible en la rejilla, que es peor que mostrarlo sin
   * poder borrarlo. `null` y `undefined` significan cosas distintas aquí.
   */
  it('(4) un bloqueo con id null entra en el índice con valor null, no se descarta', () => {
    const indice = indicePines([pin(null, 'LCL-1ºA', 1, 3, 4)]);

    expect(indice.size).toBe(1);
    expect(indice.has(clavePin('LCL-1ºA', 1))).toBe(true);
    expect(indice.get(clavePin('LCL-1ºA', 1))).toBeNull();
  });
});
