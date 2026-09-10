import { reemplazarInstancia, textoViolacion } from './ajuste';
import { Violacion } from '../models/diagnostico.model';
import { SesionVista } from '../models/horario.model';

/**
 * Lógica PURA del ajuste: sin TestBed, sin DOM y sin dobles, igual que
 * `proyeccion.spec.ts` y `diagnostico.spec.ts`.
 */

/** Sesión mínima: solo importan actividad, índice y la identidad de la fila. */
function sesion(sesionId: number, actividadCodigo: string, indice: number, dia = 1, tramo = 1): SesionVista {
  return {
    sesionId,
    indice,
    dia,
    tramo,
    asignaturaCodigo: 'X',
    asignaturaNombre: 'X',
    profesores: ['P1'],
    aulaCodigo: 'A1',
    subgrupos: ['1ºA-Completo'],
    grupos: ['1ºA'],
    actividadCodigo,
    plazaCodigo: `${actividadCodigo}-P1`,
  };
}

function violacion(campos: Partial<Violacion>): Violacion {
  return {
    regla: 'SOLAPE_PROFESOR',
    recursoCodigo: null,
    tramoCodigo: null,
    celdas: [],
    descripcion: 'inerte',
    ...campos,
  };
}

describe('lógica pura del ajuste manual', () => {
  /**
   * La identidad es la INSTANCIA, no la actividad. El fixture tiene DOS
   * repeticiones de la misma actividad (índices 1 y 2) y una tercera de otra: si
   * el filtro mirara solo `actividadCodigo`, la hermana no movida desaparecería.
   * Ese es el aserto que discrimina; con una sola repetición por actividad, la
   * mutación quedaría verde.
   */
  it('(54) reemplazarInstancia sustituye SOLO la instancia dada, no las hermanas de su actividad', () => {
    const antes = [sesion(1, 'Mat-1ºA', 1), sesion(2, 'Mat-1ºA', 2), sesion(3, 'LCL-1ºA', 1)];

    const despues = reemplazarInstancia(antes, { actividadCodigo: 'Mat-1ºA', indice: 1 }, [
      sesion(9, 'Mat-1ºA', 1, 4, 5),
    ]);

    expect(despues.map((s) => s.sesionId).sort()).toEqual([2, 3, 9]);
    // La hermana (índice 2) y la ajena siguen donde estaban.
    expect(despues.find((s) => s.sesionId === 2)?.indice).toBe(2);
    expect(despues.find((s) => s.sesionId === 3)?.actividadCodigo).toBe('LCL-1ºA');
    // La nueva trae el tramo del servidor, no el viejo.
    expect(despues.find((s) => s.sesionId === 9)?.dia).toBe(4);
    expect(despues.find((s) => s.sesionId === 9)?.tramo).toBe(5);
  });

  /**
   * Un desdoble son VARIAS filas de UNA instancia y se sustituyen TODAS: una
   * implementación que quitara la primera coincidencia dejaría huérfanas las demás.
   */
  it('(55) reemplazarInstancia sustituye todas las filas de la instancia, no solo la primera', () => {
    const antes = [sesion(1, 'Mat-1ºA', 2), sesion(2, 'Mat-1ºA', 2), sesion(3, 'LCL-1ºA', 1)];

    const despues = reemplazarInstancia(antes, { actividadCodigo: 'Mat-1ºA', indice: 2 }, [
      sesion(9, 'Mat-1ºA', 2, 4, 5),
    ]);

    expect(despues.map((s) => s.sesionId).sort()).toEqual([3, 9]);
  });

  /** No muta la entrada: la lista original sigue como estaba. */
  it('(56) reemplazarInstancia no muta la lista de entrada', () => {
    const antes = [sesion(1, 'Mat-1ºA', 1), sesion(2, 'LCL-1ºA', 1)];

    reemplazarInstancia(antes, { actividadCodigo: 'Mat-1ºA', indice: 1 }, []);

    expect(antes.map((s) => s.sesionId)).toEqual([1, 2]);
  });

  /**
   * CON recurso se nombra el recurso y el tramo. El aserto es sobre el TEXTO
   * completo, no un `toContain` del recurso: sin la cadena entera, una mutación que
   * también colara las celdas pasaría.
   */
  it('(57) una violación con recurso lo nombra a él y al tramo', () => {
    const v = violacion({
      regla: 'SOLAPE_PROFESOR',
      recursoCodigo: 'PROF7',
      tramoCodigo: 'L-2',
      celdas: [{ actividadCodigo: 'Mat-1ºA', indice: 1, plazaCodigo: null }],
    });

    expect(textoViolacion(v)).toBe('SOLAPE_PROFESOR — PROF7 en L-2');
  });

  /**
   * SIN recurso —el caso REAL de `DISTRIBUCION_MISMO_DIA`, medido en S144— se
   * nombran la regla y las celdas culpables. Las DOS celdas van en el aserto: con
   * una sola, "pinta la primera" y "pinta todas" serían indistinguibles.
   *
   * <p>Este es el caso que mata la plantilla ingenua: si el fallback desaparece y
   * se interpola `recursoCodigo` siempre, aquí sale «— null» y el aserto cae.
   */
  it('(58) DISTRIBUCION_MISMO_DIA no trae recurso: se nombran la regla y sus celdas', () => {
    const v = violacion({
      regla: 'DISTRIBUCION_MISMO_DIA',
      recursoCodigo: null,
      tramoCodigo: null,
      celdas: [
        { actividadCodigo: 'Mat-1ºA', indice: 1, plazaCodigo: null },
        { actividadCodigo: 'Mat-1ºA', indice: 3, plazaCodigo: null },
      ],
    });

    expect(textoViolacion(v)).toBe('DISTRIBUCION_MISMO_DIA — Mat-1ºA #1, Mat-1ºA #3');
  });

  /**
   * `plazaCodigo` no-null solo en `SOLAPE_AULA` (asimetría D15): cuando lo hay se
   * nombra, y cuando no, no se escribe «(null)». El fixture mezcla las DOS formas en
   * la misma violación, que es lo único que separa "nombra la plaza si la hay" de
   * "la nombra siempre" o "nunca".
   */
  it('(59) las celdas nombran su plaza solo cuando la tienen', () => {
    const v = violacion({
      regla: 'SOLAPE_AULA',
      recursoCodigo: null,
      tramoCodigo: 'M-3',
      celdas: [
        { actividadCodigo: 'Mat-1ºA', indice: 1, plazaCodigo: 'Mat-1ºA-P1' },
        { actividadCodigo: 'LCL-1ºA', indice: 2, plazaCodigo: null },
      ],
    });

    expect(textoViolacion(v)).toBe('SOLAPE_AULA — Mat-1ºA #1 (Mat-1ºA-P1), LCL-1ºA #2 en M-3');
  });

  /** Sin recurso y sin celdas queda la regla sola, nunca un guion colgando. */
  it('(60) sin recurso y sin celdas se pinta la regla sola', () => {
    expect(textoViolacion(violacion({ regla: 'REGLA_X' }))).toBe('REGLA_X');
  });
});
