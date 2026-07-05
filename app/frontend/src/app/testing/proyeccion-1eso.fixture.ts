import { HorarioProyeccion, SesionVista } from '../models/horario.model';

/**
 * Proyección de fixture (mock del JSON de 7A) del bloque 1ºESO de
 * problema-3-cierre-cyr-refmt: bloque CyR/OyD/RefMt (6 plazas, rep 2) + 4 Mat
 * (1 plaza, rep 3) = 24 sesiones. `Bloque-CyR-Tec` lleva co-docencia (2
 * profesores en UNA entrada) para ejercitar D-F7-2.
 */
const BLOQUE = 'Bloque-CyR_OyD_RefMt-1ESO';
const GRUPOS_NIVEL = ['1ºA', '1ºB', '1ºC', '1ºD'];

let seq = 0;
function s(e: Omit<SesionVista, 'sesionId'>): SesionVista {
  return { sesionId: ++seq, ...e };
}

function sufijo(suf: string): string[] {
  return GRUPOS_NIVEL.map((g) => `${g}-${suf}`);
}

/** Las 6 sub-entradas del bloque en un mismo slot (misma repetición). */
function bloque(dia: number, tramo: number, indice: number): SesionVista[] {
  const base = { indice, dia, tramo, grupos: GRUPOS_NIVEL, actividadCodigo: BLOQUE };
  return [
    s({ ...base, asignaturaCodigo: 'CyR', asignaturaNombre: 'Computacion y Robotica',
      profesores: ['TEC3', 'TEC4'], aulaCodigo: 'A5', subgrupos: sufijo('CyR-Tec'), plazaCodigo: 'Bloque-CyR-Tec' }),
    s({ ...base, asignaturaCodigo: 'CyR', asignaturaNombre: 'Computacion y Robotica',
      profesores: ['INF1'], aulaCodigo: 'A12In', subgrupos: sufijo('CyR-Inf'), plazaCodigo: 'Bloque-CyR-Inf' }),
    s({ ...base, asignaturaCodigo: 'OyD', asignaturaNombre: 'Oratoria y Debate',
      profesores: ['FIL3'], aulaCodigo: 'A11', subgrupos: sufijo('OyD'), plazaCodigo: 'Bloque-OyD' }),
    s({ ...base, asignaturaCodigo: 'RefMt', asignaturaNombre: 'Refuerzo de Matematicas',
      profesores: ['MAT6'], aulaCodigo: 'A3', subgrupos: sufijo('RefMt-MAT6'), plazaCodigo: 'Bloque-RefMt-MAT6' }),
    s({ ...base, asignaturaCodigo: 'RefMt', asignaturaNombre: 'Refuerzo de Matematicas',
      profesores: ['MAT7'], aulaCodigo: 'A14', subgrupos: sufijo('RefMt-MAT7'), plazaCodigo: 'Bloque-RefMt-MAT7' }),
    s({ ...base, asignaturaCodigo: 'RefMt', asignaturaNombre: 'Refuerzo de Matematicas',
      profesores: ['MAT4'], aulaCodigo: 'A10', subgrupos: sufijo('RefMt-MAT4'), plazaCodigo: 'Bloque-RefMt-MAT4' }),
  ];
}

/** Mat de un grupo: 1 plaza, 3 repeticiones, grupos == ["1ºX"]. */
function mat(letra: string, aula: string, prof: string): SesionVista[] {
  const g = `1º${letra}`;
  return [1, 2, 3].map((indice) =>
    s({ indice, dia: indice, tramo: 2, asignaturaCodigo: 'Mat', asignaturaNombre: 'Matematicas',
      profesores: [prof], aulaCodigo: aula, subgrupos: [`${g}-Completo`], grupos: [g],
      actividadCodigo: `Mat-${g}`, plazaCodigo: `Mat-${g}-P1` }),
  );
}

export const PROYECCION_1ESO: HorarioProyeccion = {
  id: 1,
  nombre: 'Proyeccion Fase 7',
  estado: 'BORRADOR',
  estadoSolver: 'OPTIMAL',
  objetivo: 0,
  cotaInferior: 0,
  fechaGeneracion: '2026-07-05T00:00:00Z',
  sesiones: [
    ...bloque(1, 1, 1), // repetición 1 del bloque en (dia 1, tramo 1)
    ...bloque(2, 1, 2), // repetición 2 en (dia 2, tramo 1)
    ...mat('A', 'A1', 'MATA'),
    ...mat('B', 'A2', 'MATB'),
    ...mat('C', 'A4', 'MATC'),
    ...mat('D', 'A7', 'MATD'),
  ],
};
