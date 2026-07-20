import { clavePin, indicePines } from './pines';
import { Bloqueo } from '../models/bloqueo.model';

/** Bloqueo mínimo: solo importan la actividad y el índice (sin pines de aula). */
function pin(id: number, actividadCodigo: string, indice: number, dia: number, orden: number): Bloqueo {
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

    // Discriminante: agrupar solo por actividadCodigo daría tamaño 1.
    expect(indice.size).toBe(2);
    expect(indice).toEqual(new Set([clavePin('Mat-1ºA', 1), clavePin('Mat-1ºA', 2)]));
  });

  it('(2) el índice reconoce la instancia pinada y NO sus hermanas sin pinar', () => {
    const indice = indicePines(DOS_REPETICIONES);

    expect(indice.has(clavePin('Mat-1ºA', 1))).toBe(true);
    expect(indice.has(clavePin('Mat-1ºA', 3))).toBe(false);
    expect(indice.has(clavePin('Bloque-CyR_OyD_RefMt-1ESO', 1))).toBe(false);
  });

  it('(3) sin bloqueos el índice está vacío', () => {
    expect(indicePines([]).size).toBe(0);
  });
});
