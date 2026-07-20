import { agruparPorActividad, agruparPorSlot, claveSlot, filtrar } from './proyeccion';
import { SesionVista } from '../models/horario.model';
import { PROYECCION_1ESO } from '../testing/proyeccion-1eso.fixture';

const SESIONES = PROYECCION_1ESO.sesiones;
const PLAZAS_BLOQUE = [
  'Bloque-CyR-Tec', 'Bloque-CyR-Inf', 'Bloque-OyD',
  'Bloque-RefMt-MAT6', 'Bloque-RefMt-MAT7', 'Bloque-RefMt-MAT4',
];

/** Códigos de plaza únicos, ordenados, de un conjunto de sesiones. */
function plazasUnicas(sesiones: { plazaCodigo: string }[]): string[] {
  return [...new Set(sesiones.map((s) => s.plazaCodigo))].sort();
}

describe('mecanismo de las tres vistas', () => {
  it('(1) colapso: el bloque da 6 sub-entradas en el mismo (dia,tramo) en la vista de 1ºA', () => {
    const vista1A = filtrar(SESIONES, 'grupo', '1ºA');
    const slot = agruparPorSlot(vista1A).get(claveSlot(1, 1)) ?? [];

    expect(slot).toHaveLength(6);
    expect(slot.map((s) => s.plazaCodigo).sort()).toEqual([...PLAZAS_BLOQUE].sort());
  });

  it('(2) filtro por grupos: la vista de 1ºA incluye las plazas del bloque y Mat-1ºA-P1', () => {
    const vista1A = filtrar(SESIONES, 'grupo', '1ºA');
    const plazas = plazasUnicas(vista1A);

    for (const p of PLAZAS_BLOQUE) {
      expect(plazas).toContain(p);
    }
    expect(plazas).toContain('Mat-1ºA-P1');
  });

  it('(3) exclusión (igualdad exacta de conjunto): la vista de 1ºA NO incluye Mat-1ºB/C/D ni sesiones sin "1ºA"', () => {
    const vista1A = filtrar(SESIONES, 'grupo', '1ºA');

    // Toda sesión de la vista lleva "1ºA" en grupos.
    expect(vista1A.every((s) => s.grupos.includes('1ºA'))).toBe(true);

    // El conjunto exacto de plazas es el bloque + Mat-1ºA + la LCL de co-docencia, nada más.
    expect(plazasUnicas(vista1A)).toEqual([...PLAZAS_BLOQUE, 'Mat-1ºA-P1', 'LCL-1ºA-P1'].sort());
  });

  it('(4) co-docencia: la LCL multi-profesor es UNA sub-entrada con lista de profesores', () => {
    const vista1A = filtrar(SESIONES, 'grupo', '1ºA');
    const coDocencia = vista1A.filter((s) => s.plazaCodigo === 'LCL-1ºA-P1');

    expect(coDocencia.length).toBeGreaterThan(0);
    for (const e of coDocencia) {
      expect(e.profesores).toEqual(['LEN2', 'LEN8']);
    }
    // En su slot (dia 3, tramo 1), LCL-1ºA-P1 aporta una sola sub-entrada (no una por profesor).
    const slot = agruparPorSlot(vista1A).get(claveSlot(3, 1)) ?? [];
    expect(slot.filter((s) => s.plazaCodigo === 'LCL-1ºA-P1')).toHaveLength(1);
  });
});

/**
 * Fixture LOCAL: `PROYECCION_1ESO` no tiene ningún slot con dos actividades
 * distintas —sus 6 plazas de CyR/OyD/RefMt son UNA sola actividad, el bloque—,
 * así que el segundo nivel de agrupamiento se ejercita con un slot mínimo que
 * mezcla las dos formas: una instancia de varias plazas y otra de una sola.
 */
function entrada(
  actividadCodigo: string, indice: number, plazaCodigo: string, dia = 1, tramo = 1,
): SesionVista {
  return {
    sesionId: 0, indice, dia, tramo,
    asignaturaCodigo: 'X', asignaturaNombre: 'X', profesores: [], aulaCodigo: 'A1',
    subgrupos: [], grupos: ['1ºA'], actividadCodigo, plazaCodigo,
  };
}

const SLOT_MIXTO: SesionVista[] = [
  entrada('Bloque-CyR_OyD_RefMt-1ESO', 1, 'Bloque-CyR-Tec'),
  entrada('Bloque-CyR_OyD_RefMt-1ESO', 1, 'Bloque-OyD'),
  entrada('Mat-1ºA', 1, 'Mat-1ºA-P1'),
];

/**
 * Slot DEFENSIVO (no un patrón del horario real: dos repeticiones de la misma
 * actividad en un mismo tramo solaparían al grupo). Existe porque el índice es
 * la mitad de la clave de instancia y SLOT_MIXTO, con todos sus `indice` a 1,
 * no lo ejercita: sin este slot, agrupar solo por `actividadCodigo` pasaría
 * este spec entero.
 */
const SLOT_DOS_INDICES: SesionVista[] = [
  entrada('Mat-1ºA', 1, 'Mat-1ºA-P1', 2, 1),
  entrada('Mat-1ºA', 2, 'Mat-1ºA-P1', 2, 1),
];

describe('envoltorio de instancia dentro del slot', () => {
  it('(5) un slot con dos actividades da DOS instancias, y la de varias plazas conserva sus 2 entradas', () => {
    const celdas = agruparPorActividad(SLOT_MIXTO).get(claveSlot(1, 1)) ?? [];

    // Discriminante: agrupar plano (solo por slot) daría 3 celdas de 1 entrada.
    expect(celdas).toHaveLength(2);
    expect(celdas[0].entradas).toHaveLength(2);
    expect(celdas[0].actividadCodigo).toBe('Bloque-CyR_OyD_RefMt-1ESO');
    expect(celdas[1].entradas).toHaveLength(1);
  });

  it('(6) el ÍNDICE parte la celda: misma actividad, dos índices en un slot dan DOS instancias', () => {
    const celdas = agruparPorActividad(SLOT_DOS_INDICES).get(claveSlot(2, 1)) ?? [];

    // Discriminante: agrupar por actividadCodigo a secas daría 1 celda de 2 entradas.
    expect(celdas).toHaveLength(2);
    expect(celdas.map((c) => c.indice)).toEqual([1, 2]);
    expect(celdas[0].entradas).toHaveLength(1);
  });

  it('(7) patrón real: el bloque de 1ºA es UNA instancia con las 6 sub-entradas', () => {
    const vista1A = filtrar(SESIONES, 'grupo', '1ºA');
    const celdas = agruparPorActividad(vista1A).get(claveSlot(1, 1)) ?? [];

    expect(celdas).toHaveLength(1);
    expect(celdas[0].entradas).toHaveLength(6);
    expect(celdas[0].indice).toBe(1);
  });

  it('(8) dos repeticiones del bloque caen en slots distintos, con su índice propio', () => {
    const vista1A = filtrar(SESIONES, 'grupo', '1ºA');
    const celdas = agruparPorActividad(vista1A);

    expect(celdas.get(claveSlot(1, 1))?.[0].indice).toBe(1);
    expect(celdas.get(claveSlot(2, 1))?.[0].indice).toBe(2);
  });
});
