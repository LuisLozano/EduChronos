import { agruparPorSlot, claveSlot, filtrar } from './proyeccion';
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

    // El conjunto exacto de plazas es el bloque + Mat-1ºA, nada más.
    expect(plazasUnicas(vista1A)).toEqual([...PLAZAS_BLOQUE, 'Mat-1ºA-P1'].sort());
  });

  it('(4) co-docencia: una plaza multi-profesor es UNA sub-entrada con lista de profesores', () => {
    const vista1A = filtrar(SESIONES, 'grupo', '1ºA');
    const coDocencia = vista1A.filter((s) => s.plazaCodigo === 'Bloque-CyR-Tec');

    expect(coDocencia.length).toBeGreaterThan(0);
    for (const e of coDocencia) {
      expect(e.profesores).toEqual(['TEC3', 'TEC4']);
    }
    // En el slot del bloque, Bloque-CyR-Tec aporta una sola sub-entrada (no una por profesor).
    const slot = agruparPorSlot(vista1A).get(claveSlot(1, 1)) ?? [];
    expect(slot.filter((s) => s.plazaCodigo === 'Bloque-CyR-Tec')).toHaveLength(1);
  });
});
