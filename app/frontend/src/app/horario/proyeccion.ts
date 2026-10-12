import { GuardiaVista, SesionVista } from '../models/horario.model';
import { clavePin } from './pines';

/** Las tres vistas de Fase 7 y, desde S215, la de guardias, que no tiene recurso. */
export type Vista = 'grupo' | 'profesor' | 'aula' | 'guardias';

/** La entrada con la que se pinta una guardia en la celda de su profesor (F2). */
export const ROTULO_GUARDIA = 'Guardia';

/** Une los códigos de una celda del modo guardias (F4), como el PDF de guardias. */
export const SEPARADOR_CODIGOS_GUARDIA = ', ';

/** Etiquetas de los 5 días lectivos (índice 0 = dia 1 = lunes). */
export const DIAS = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes'] as const;
/** Los 6 tramos lectivos (ordenEnDia 1..6). */
export const TRAMOS = [1, 2, 3, 4, 5, 6] as const;

/**
 * Entidades seleccionables de una vista, DERIVADAS de la propia proyección
 * (sin endpoint nuevo): la unión ordenada de grupos / profesores / aulas que
 * aparecen en las sesiones. Una sub-entrada aporta varios grupos o profesores.
 *
 * <p>Desde S215 (F3), en la de profesor cuentan también los profesores de las
 * `guardias`: quien solo tiene guardias aparece, en el mismo orden que los demás. La
 * de guardias no tiene recurso que elegir y devuelve la lista vacía. Desde S221 cuentan
 * también las de recreo: quien solo tiene guardias de recreo aparece igual (D8).
 */
export function entidadesDeVista(
  sesiones: readonly SesionVista[],
  vista: Vista,
  guardias: readonly GuardiaVista[] = [],
): string[] {
  if (vista === 'guardias') {
    return [];
  }
  const set = new Set<string>();
  if (vista === 'profesor') {
    for (const g of guardias) {
      set.add(g.profesorCodigo);
    }
  }
  for (const s of sesiones) {
    if (vista === 'aula') {
      // Una reunión o una función sin aula (S201) no aporta aula a la vista.
      if (s.aulaCodigo !== null) {
        set.add(s.aulaCodigo);
      }
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
    case 'guardias':
      // El modo guardias no pinta clases (F4): su rejilla son los códigos de guardia.
      return [];
  }
}

/** Una guardia ORDINARIA, que está en un tramo de clase (S221): su `tramo` no es null. */
export type GuardiaDeTramo = GuardiaVista & { tramo: number };

/** Una guardia de RECREO o BIBLIOTECA, que está en un recreo (S221): su `recreo` no es null. */
export type GuardiaDeRecreo = GuardiaVista & { recreo: number };

/** Las guardias de una vista, ya separadas por dónde van en la rejilla (S221, D8). */
export interface GuardiasDeVista {
  /** Las de tramo: van en las celdas lectivas, por {@link claveSlot}. */
  deTramo: GuardiaDeTramo[];
  /** Las de recreo: van en la fila del recreo, por (día, recreo). */
  deRecreo: GuardiaDeRecreo[];
}

/**
 * Las guardias que se pintan en la rejilla de la vista (S215): en la de profesor, las
 * del profesor elegido (F2); en la de guardias, todas (F4); en las demás, ninguna.
 *
 * <p>Desde S221 (D8) salen SEPARADAS: las de tramo, que siguen como hasta ahora hacia
 * {@link claveSlot}, y las de recreo, que no tienen tramo y van a la fila del recreo. La
 * separación va aquí, antes de cualquier clave de slot: una de recreo que llegara a
 * {@link codigosDeGuardiaPorSlot} daría la clave `dia-null` y no se vería en ninguna parte.
 */
export function guardiasDeVista(
  guardias: readonly GuardiaVista[],
  vista: Vista,
  entidad: string,
): GuardiasDeVista {
  const elegidas = elegirGuardias(guardias, vista, entidad);
  return {
    deTramo: elegidas.filter((g): g is GuardiaDeTramo => g.tramo !== null),
    deRecreo: elegidas.filter((g): g is GuardiaDeRecreo => g.recreo !== null),
  };
}

function elegirGuardias(
  guardias: readonly GuardiaVista[],
  vista: Vista,
  entidad: string,
): GuardiaVista[] {
  switch (vista) {
    case 'profesor':
      return guardias.filter((g) => g.profesorCodigo === entidad);
    case 'guardias':
      return [...guardias];
    case 'grupo':
    case 'aula':
      return [];
  }
}

/** El `tipo` de una guardia de biblioteca, el `name()` de `TipoGuardia.BIBLIOTECA`. */
export const TIPO_BIBLIOTECA = 'BIBLIOTECA';

/** La entrada con la que se pinta una guardia de biblioteca en la celda de su profesor (S221, X2). */
export const ROTULO_BIBLIOTECA = 'Biblioteca';

/** Delante de los códigos de biblioteca en la celda de un recreo del modo guardias (S221, X3). */
export const PREFIJO_BIBLIOTECA = 'Biblioteca: ';

/**
 * Las entradas de la celda de un día en la fila del recreo, en la vista de profesor (S221,
 * D6): una por guardia de ese día, «Guardia» la de RECREO y «Biblioteca» la de BIBLIOTECA,
 * como en el PDF. Recibe las guardias del recreo que pinta la fila.
 */
export function rotulosDeRecreo(guardias: readonly GuardiaDeRecreo[], dia: number): string[] {
  return guardias
    .filter((g) => g.dia === dia)
    .map((g) => (g.tipo === TIPO_BIBLIOTECA ? ROTULO_BIBLIOTECA : ROTULO_GUARDIA));
}

/**
 * Las líneas de la celda de un día en la fila del recreo, en el modo guardias (S221, D6 y
 * X3): los códigos de RECREO ordenados y unidos por «, » y, en línea aparte, «Biblioteca:»
 * con los de BIBLIOTECA. Sin guardias, ninguna línea.
 */
export function lineasDeRecreoEnGuardias(guardias: readonly GuardiaDeRecreo[], dia: number): string[] {
  const delDia = guardias.filter((g) => g.dia === dia);
  const deRecreo = delDia.filter((g) => g.tipo !== TIPO_BIBLIOTECA).map((g) => g.profesorCodigo).sort();
  const deBiblioteca = delDia.filter((g) => g.tipo === TIPO_BIBLIOTECA).map((g) => g.profesorCodigo).sort();
  const lineas: string[] = [];
  if (deRecreo.length > 0) {
    lineas.push(deRecreo.join(SEPARADOR_CODIGOS_GUARDIA));
  }
  if (deBiblioteca.length > 0) {
    lineas.push(PREFIJO_BIBLIOTECA + deBiblioteca.join(SEPARADOR_CODIGOS_GUARDIA));
  }
  return lineas;
}

/**
 * Los códigos de guardia de cada slot `(dia, tramo)`, ORDENADOS (F4, como D3 del PDF).
 * La clave es {@link claveSlot}. Solo recibe guardias de tramo ({@link guardiasDeVista}).
 */
export function codigosDeGuardiaPorSlot(guardias: readonly GuardiaDeTramo[]): Map<string, string[]> {
  const slots = new Map<string, string[]>();
  for (const g of guardias) {
    const k = claveSlot(g.dia, g.tramo);
    const arr = slots.get(k);
    if (arr) {
      arr.push(g.profesorCodigo);
    } else {
      slots.set(k, [g.profesorCodigo]);
    }
  }
  for (const codigos of slots.values()) {
    codigos.sort();
  }
  return slots;
}

/** Clave estable de un slot de la rejilla. */
export function claveSlot(dia: number, tramo: number): string {
  return `${dia}-${tramo}`;
}

/**
 * Los tramos que ocupa una sesión: `[tramo, …, tramo + duracion - 1]`. Espejo de
 * `SesionVistaDTO.tramosCubiertos()` en Java. La numeración es la lectiva del día,
 * que excluye el recreo; que un bloque no cruce el recreo ni desborde el día lo
 * garantiza el backend (solver y verificador), no esta función, que sólo cuenta.
 */
export function tramosCubiertos(s: SesionVista): number[] {
  return Array.from({ length: s.duracion }, (_, i) => s.tramo + i);
}

/**
 * Agrupa las sesiones por slot `(dia, tramo)`, colocando cada una en TODOS los
 * slots que ocupa ({@link tramosCubiertos}): un bloque de dos tramos aparece en dos
 * slots. Un slot con más de una entrada es una celda-como-lista: el
 * desdoble/agrupamiento/co-docencia coloca varias plazas en el mismo tramo, y cada
 * una es una sub-entrada.
 */
export function agruparPorSlot(sesiones: readonly SesionVista[]): Map<string, SesionVista[]> {
  const slots = new Map<string, SesionVista[]>();
  for (const s of sesiones) {
    for (const tramo of tramosCubiertos(s)) {
      const k = claveSlot(s.dia, tramo);
      const arr = slots.get(k);
      if (arr) {
        arr.push(s);
      } else {
        slots.set(k, [s]);
      }
    }
  }
  return slots;
}

/**
 * Las sub-entradas de un slot que pertenecen a UNA instancia —el par
 * (`actividadCodigo`, `indice`), D-6—. Es la unidad que el usuario manipula:
 * las 6 plazas de un bloque se mueven juntas o no se mueven.
 *
 * <p>`continuacion` es false en el slot del tramo de INICIO y true en los demás que
 * la instancia cubre. Una continuación se pinta, pero no es otra instancia: sus
 * `entradas` son las mismas sesiones que las del slot de inicio, y sólo desde ése
 * se arrastra y se pina.
 */
export interface InstanciaCelda {
  actividadCodigo: string;
  indice: number;
  entradas: SesionVista[];
  continuacion: boolean;
}

/**
 * Segundo nivel de agrupamiento SOBRE {@link agruparPorSlot}: dentro de cada
 * slot, reúne las sub-entradas por instancia, preservando el orden de aparición
 * tanto entre instancias como dentro de cada una. La clave externa del Map
 * sigue siendo {@link claveSlot}, de modo que la rejilla indexa igual que antes.
 * Una instancia de varios tramos aparece en cada slot que cubre, marcada como
 * continuación en todos salvo el de su tramo de inicio.
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
        porInstancia.set(ki, {
          actividadCodigo: e.actividadCodigo,
          indice: e.indice,
          entradas: [e],
          continuacion: claveSlot(e.dia, e.tramo) !== k,
        });
      }
    }
    celdas.set(k, [...porInstancia.values()]);
  }
  return celdas;
}
