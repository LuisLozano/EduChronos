import { clavePin, filaDeClave, indicePines } from './pines';
import { Bloqueo } from '../models/bloqueo.model';
import { SesionVista } from '../models/horario.model';

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

  /** Fila mínima: solo importan la clave de negocio y el tramo que se va a leer. */
  function fila(actividadCodigo: string, indice: number, dia: number, tramo: number): SesionVista {
    return {
      sesionId: 1,
      indice,
      dia,
      tramo,
      asignaturaCodigo: 'X',
      asignaturaNombre: 'X',
      profesores: [],
      aulaCodigo: 'A1',
      subgrupos: [],
      grupos: [],
      actividadCodigo,
      plazaCodigo: 'P1',
    };
  }

  /**
   * La resolución es por la CLAVE ENTERA, no por la actividad: el fixture tiene dos
   * repeticiones de la misma en tramos distintos, y devolver la primera daría el
   * tramo de la hermana. Es la dimensión que decide dónde se clava el pin.
   */
  it('(5) filaDeClave distingue las repeticiones de una misma actividad', () => {
    const sesiones = [fila('Mat-1ºA', 1, 5, 6), fila('Mat-1ºA', 2, 3, 4)];

    expect(filaDeClave(sesiones, 'Mat-1ºA|2')?.tramo).toBe(4);
    expect(filaDeClave(sesiones, 'Mat-1ºA|1')?.tramo).toBe(6);
  });

  /** Una clave que no está no se inventa: `undefined`, y quien llama decide. */
  it('(6) filaDeClave devuelve undefined para una clave ausente', () => {
    expect(filaDeClave([fila('Mat-1ºA', 1, 5, 6)], 'Mat-1ºA|3')).toBeUndefined();
    expect(filaDeClave([], 'Mat-1ºA|1')).toBeUndefined();
  });

  /**
   * Un `actividadCodigo` que CONTIENE el separador se resuelve bien. Es la razón
   * documentada de comparar la clave reconstruida en vez de partir la recibida por
   * `|`: un `split('|')` daría actividad `'A'` e índice `NaN` y devolvería
   * `undefined` en silencio, clavando el pin en ningún sitio o en otro.
   *
   * <p>El fixture añade una segunda fila cuya clave es la que produciría ese
   * troceado mal hecho, para que la mutación no pueda acertar por casualidad.
   */
  it('(7) filaDeClave resuelve un código que contiene el separador de la clave', () => {
    const sesiones = [fila('A|B-1ºA', 2, 1, 1), fila('A', 2, 5, 6)];

    expect(filaDeClave(sesiones, 'A|B-1ºA|2')?.actividadCodigo).toBe('A|B-1ºA');
    expect(filaDeClave(sesiones, 'A|B-1ºA|2')?.tramo).toBe(1);
  });
});
