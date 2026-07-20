import { SesionVista } from '../models/horario.model';
import { clavePin } from './pines';

/** Las tres vistas de Fase 7. */
export type Vista = 'grupo' | 'profesor' | 'aula';

/** Etiquetas de los 5 días lectivos (índice 0 = dia 1 = lunes). */
export const DIAS = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes'] as const;
/** Los 6 tramos lectivos (ordenEnDia 1..6). */
export const TRAMOS = [1, 2, 3, 4, 5, 6] as const;

/**
 * Entidades seleccionables de una vista, DERIVADAS de la propia proyección
 * (sin endpoint nuevo): la unión ordenada de grupos / profesores / aulas que
 * aparecen en las sesiones. Una sub-entrada aporta varios grupos o profesores.
 */
export function entidadesDeVista(sesiones: readonly SesionVista[], vista: Vista): string[] {
  const set = new Set<string>();
  for (const s of sesiones) {
    if (vista === 'aula') {
      set.add(s.aulaCodigo);
    } else {
      for (const v of vista === 'grupo' ? s.grupos : s.profesores) {
        set.add(v);
      }
    }
  }
  return [...set].sort();
}

/**
 * Filtra las sesiones que pertenecen a la entidad seleccionada. La vista de un
 * grupo incluye TODA sesión cuyo `grupos` lo contenga —de ahí que el bloque de
 * agrupamiento se proyecte entero en la vista de nivel (D-F7-1)—; la de un aula
 * usa igualdad exacta (una sesión ocupa un aula).
 */
export function filtrar(sesiones: readonly SesionVista[], vista: Vista, entidad: string): SesionVista[] {
  switch (vista) {
    case 'grupo':
      return sesiones.filter((s) => s.grupos.includes(entidad));
    case 'profesor':
      return sesiones.filter((s) => s.profesores.includes(entidad));
    case 'aula':
      return sesiones.filter((s) => s.aulaCodigo === entidad);
  }
}

/** Clave estable de un slot de la rejilla. */
export function claveSlot(dia: number, tramo: number): string {
  return `${dia}-${tramo}`;
}

/**
 * Agrupa las sesiones por slot `(dia, tramo)`. Un slot con más de una entrada es
 * una celda-como-lista: el desdoble/agrupamiento/co-docencia coloca varias
 * plazas en el mismo tramo, y cada una es una sub-entrada.
 */
export function agruparPorSlot(sesiones: readonly SesionVista[]): Map<string, SesionVista[]> {
  const slots = new Map<string, SesionVista[]>();
  for (const s of sesiones) {
    const k = claveSlot(s.dia, s.tramo);
    const arr = slots.get(k);
    if (arr) {
      arr.push(s);
    } else {
      slots.set(k, [s]);
    }
  }
  return slots;
}

/**
 * Las sub-entradas de un slot que pertenecen a UNA instancia —el par
 * (`actividadCodigo`, `indice`), D-6—. Es la unidad que el usuario manipula:
 * las 6 plazas de un bloque se mueven juntas o no se mueven.
 */
export interface InstanciaCelda {
  actividadCodigo: string;
  indice: number;
  entradas: SesionVista[];
}

/**
 * Segundo nivel de agrupamiento SOBRE {@link agruparPorSlot}: dentro de cada
 * slot, reúne las sub-entradas por instancia, preservando el orden de aparición
 * tanto entre instancias como dentro de cada una. La clave externa del Map
 * sigue siendo {@link claveSlot}, de modo que la rejilla indexa igual que antes.
 */
export function agruparPorActividad(sesiones: readonly SesionVista[]): Map<string, InstanciaCelda[]> {
  const celdas = new Map<string, InstanciaCelda[]>();
  for (const [k, entradas] of agruparPorSlot(sesiones)) {
    const porInstancia = new Map<string, InstanciaCelda>();
    for (const e of entradas) {
      const ki = clavePin(e.actividadCodigo, e.indice);
      const inst = porInstancia.get(ki);
      if (inst) {
        inst.entradas.push(e);
      } else {
        porInstancia.set(ki, { actividadCodigo: e.actividadCodigo, indice: e.indice, entradas: [e] });
      }
    }
    celdas.set(k, [...porInstancia.values()]);
  }
  return celdas;
}
