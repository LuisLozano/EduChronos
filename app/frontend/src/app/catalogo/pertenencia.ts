import { Actividad } from '../models/actividad.model';
import { Asignatura } from '../models/asignatura.model';
import { Grupo } from '../models/grupo.model';
import { Profesor } from '../models/profesor.model';
import { Subgrupo } from '../models/subgrupo.model';
import { normaliza } from './busqueda';

/**
 * Búsqueda POR PERTENENCIA de la lista de Actividades (S225, `C-actividades-por-pertenencia`,
 * condición 3 de O-interfaz). Hermana de `busqueda.ts`, pero esta sí conoce entidades: recibe
 * las cuatro listas de catálogo y decide qué actividades les pertenecen. `coincide()` sigue
 * siendo de las siete listas y de los selectores, y no se toca.
 *
 * <p><b>Devuelve TODAS las entidades que casan; nunca elige una.</b> La coincidencia exacta se
 * resuelve con {@link normaliza}, que es una reducción con pérdida y no una identidad
 * (`busqueda.ts:59-62`): en el centro real «Biol» y «BIOL» son dos asignaturas, «ECO» es a la vez
 * un profesor y una asignatura, e «Inglés» es el nombre de tres. Quedarse con una sería esconder
 * las otras sin decirlo; la unión, con la línea que las nombra, lo dice.
 *
 * <p><b>Con los ordinales conservados</b> (regla de S185, `busqueda.ts:21-24`): «1B» no casa
 * con el grupo «1ºB», que en este centro es ESO; quien escribe «1B» sigue con la búsqueda por
 * texto de siempre, que encuentra las dos etapas.
 */

/** Las cuatro listas contra las que se casa el término. */
export interface Catalogo {
  readonly profesores: readonly Profesor[];
  readonly asignaturas: readonly Asignatura[];
  readonly grupos: readonly Grupo[];
  readonly subgrupos: readonly Subgrupo[];
}

export type TipoEntidad = 'profesor' | 'asignatura' | 'grupo' | 'subgrupo';

/** Una entidad que casa exactamente con el término. `nombre` solo lo lleva la asignatura. */
export interface EntidadCasada {
  readonly tipo: TipoEntidad;
  readonly codigo: string;
  readonly nombre?: string;
}

/** Resultado de casar un término: las entidades, en el orden de la línea, y su predicado. */
export interface Pertenencia {
  readonly entidades: readonly EntidadCasada[];
  /** `true` si la actividad pertenece a ALGUNA de las entidades (unión). */
  readonly pertenece: (actividad: Actividad) => boolean;
}

/** Los ordinales cuentan al casar (S185): «1B» ≠ «1ºB». */
const CON_ORDINALES = true;

/** Orden por código dentro de cada tipo, estable y sin depender del idioma del navegador. */
function porCodigo(a: EntidadCasada, b: EntidadCasada): number {
  return a.codigo < b.codigo ? -1 : a.codigo > b.codigo ? 1 : 0;
}

/**
 * Entidades cuyo código —o, en la asignatura, también su nombre completo— casa EXACTAMENTE con
 * el término una vez normalizados los dos, y el predicado de pertenencia a su unión. `null` si
 * no casa ninguna: entonces manda la búsqueda por texto.
 *
 * <p>Orden: profesores, asignaturas, grupos y subgrupos; dentro de cada tipo, por código.
 */
export function casarExacto(termino: string, catalogo: Catalogo): Pertenencia | null {
  const clave = normaliza(termino.trim(), CON_ORDINALES);
  if (clave === '') {
    return null;
  }
  const casa = (texto: string) => normaliza(texto, CON_ORDINALES) === clave;
  const entidades: EntidadCasada[] = [
    ...catalogo.profesores
      .filter((p) => casa(p.codigo))
      .map((p): EntidadCasada => ({ tipo: 'profesor', codigo: p.codigo }))
      .sort(porCodigo),
    ...catalogo.asignaturas
      .filter((a) => casa(a.codigo) || casa(a.nombreCompleto))
      .map((a): EntidadCasada => ({ tipo: 'asignatura', codigo: a.codigo, nombre: a.nombreCompleto }))
      .sort(porCodigo),
    ...catalogo.grupos
      .filter((g) => casa(g.codigo))
      .map((g): EntidadCasada => ({ tipo: 'grupo', codigo: g.codigo }))
      .sort(porCodigo),
    ...catalogo.subgrupos
      .filter((s) => casa(s.codigo))
      .map((s): EntidadCasada => ({ tipo: 'subgrupo', codigo: s.codigo }))
      .sort(porCodigo),
  ];
  if (entidades.length === 0) {
    return null;
  }
  const gruposDe = new Map(catalogo.subgrupos.map((s) => [s.codigo, s.grupos]));
  return {
    entidades,
    pertenece: (actividad) => entidades.some((e) => perteneceA(e, actividad, gruposDe)),
  };
}

/**
 * Pertenencia de una actividad a UNA entidad, siempre por igualdad de código:
 * <ul>
 *   <li>profesor y subgrupo: alguna plaza los lleva;
 *   <li>grupo: alguna plaza tiene un subgrupo cuyos grupos INCLUYEN ese código como elemento,
 *       no como texto. Así «4ºCDi», el PDC de 4ºC, no entra al buscar «4ºC» (decisión D de S224);
 *   <li>asignatura: la de la actividad, que es `null` cuando sus plazas tienen varias, o la de
 *       alguna plaza.
 * </ul>
 */
function perteneceA(
  entidad: EntidadCasada,
  actividad: Actividad,
  gruposDe: ReadonlyMap<string, readonly string[]>,
): boolean {
  const codigo = entidad.codigo;
  switch (entidad.tipo) {
    case 'profesor':
      return actividad.plazas.some((p) => p.profesores.includes(codigo));
    case 'subgrupo':
      return actividad.plazas.some((p) => p.subgrupos.includes(codigo));
    case 'grupo':
      return actividad.plazas.some((p) =>
        p.subgrupos.some((s) => (gruposDe.get(s) ?? []).includes(codigo)),
      );
    case 'asignatura':
      return actividad.asignatura === codigo || actividad.plazas.some((p) => p.asignatura === codigo);
  }
}

/** Una entidad en la línea, sin la palabra «Actividades»: «del grupo 4ºC». */
function rotulo(entidad: EntidadCasada): string {
  switch (entidad.tipo) {
    case 'profesor':
      return `del profesor ${entidad.codigo}`;
    case 'asignatura':
      return `de la asignatura ${entidad.nombre} (${entidad.codigo})`;
    case 'grupo':
      return `del grupo ${entidad.codigo}`;
    case 'subgrupo':
      return `del subgrupo ${entidad.codigo}`;
  }
}

/**
 * La línea que va bajo la caja: «Actividades del profesor BYG1», y con varias, unidas por
 * « · » sin repetir la palabra: «Actividades del profesor ECO · de la asignatura Economía (ECO)».
 */
export function lineaPertenencia(entidades: readonly EntidadCasada[]): string {
  return `Actividades ${entidades.map(rotulo).join(' · ')}`;
}

/**
 * Nombres completos de la asignatura de la actividad y de las de sus plazas, sin repetir, para
 * añadirlos al texto de la fila en la búsqueda por texto: así «Músic» encuentra las de «Mús».
 * Un código que no está en el mapa no aporta nada.
 */
export function nombresDeAsignaturas(
  actividad: Actividad,
  nombrePorCodigo: ReadonlyMap<string, string>,
): string[] {
  const codigos = new Set<string>(actividad.plazas.map((p) => p.asignatura));
  if (actividad.asignatura !== null) {
    codigos.add(actividad.asignatura);
  }
  const nombres = new Set<string>();
  for (const codigo of codigos) {
    const nombre = nombrePorCodigo.get(codigo);
    if (nombre !== undefined) {
      nombres.add(nombre);
    }
  }
  return [...nombres];
}
