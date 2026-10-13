import { Actividad, Plaza } from '../models/actividad.model';
import { Asignatura } from '../models/asignatura.model';
import { Grupo } from '../models/grupo.model';
import { Profesor } from '../models/profesor.model';
import { Subgrupo } from '../models/subgrupo.model';
import { coincide } from './busqueda';
import { Catalogo, casarExacto, lineaPertenencia, nombresDeAsignaturas } from './pertenencia';

/**
 * Lógica PURA, como `busqueda.spec.ts`: sin TestBed ni HTTP.
 *
 * <p>El catálogo es INVENTADO y pequeño, pero sus trampas son las del centro real (T0 de S225):
 * «4ºCDi» empieza por «4ºC», «ECO» es profesor y asignatura, «1ºB» es ESO y «1B» no casa con él,
 * y hay una actividad con asignatura `null` que solo casa por su plaza. El subgrupo MIXTO (de 4ºC
 * y de 4ºCDi a la vez) no existe en la base: está para fijar que la pertenencia a un grupo es
 * «alguno de sus grupos ES ese», no «algún grupo empieza o contiene ese texto».
 */
describe('búsqueda por pertenencia de Actividades', () => {
  const profesor = (codigo: string) => ({ codigo }) as Profesor;
  const asignatura = (codigo: string, nombreCompleto: string) => ({ codigo, nombreCompleto }) as Asignatura;
  const grupo = (codigo: string) => ({ codigo }) as Grupo;
  const subgrupo = (codigo: string, grupos: string[]) => ({ codigo, grupos }) as Subgrupo;

  const CATALOGO: Catalogo = {
    profesores: [profesor('ECO'), profesor('BYG1')],
    asignaturas: [
      asignatura('Mús', 'Música'),
      asignatura('ECO', 'Economía'),
      asignatura('Biol', 'Biología y Geología'),
      asignatura('BIOL', 'Biología'),
      asignatura('Relig', 'Religión'),
      asignatura('VE', 'Valores Éticos'),
    ],
    grupos: [grupo('4ºC'), grupo('4ºCDi'), grupo('1ºB')],
    subgrupos: [
      subgrupo('4ºC-DT', ['4ºC']),
      subgrupo('4ºCDi-DT', ['4ºCDi']),
      subgrupo('MIXTO', ['4ºC', '4ºCDi']),
      subgrupo('1ºB-Completo', ['1ºB']),
    ],
  };

  const plaza = (p: Partial<Plaza>): Plaza => ({
    id: 0,
    codigo: 'X-P1',
    asignatura: 'Mús',
    aulaFija: 'A1',
    aulasCandidatas: [],
    profesores: [],
    subgrupos: [],
    ...p,
  });
  const actividad = (id: number, codigo: string, asig: string | null, plazas: Plaza[]): Actividad => ({
    id,
    codigo,
    tipo: 'CLASE',
    asignatura: asig,
    duracionTramos: 1,
    repeticionesPorSemana: 1,
    patronTemporal: 'NEUTRA',
    requiereTutor: false,
    plazas,
  });

  const ACTIVIDADES: Actividad[] = [
    actividad(1, 'Mús-1ºB', 'Mús', [plaza({ profesores: ['BYG1'], subgrupos: ['1ºB-Completo'] })]),
    actividad(2, 'DT-4ºC', 'Biol', [plaza({ asignatura: 'Biol', subgrupos: ['4ºC-DT'] })]),
    actividad(3, 'DT-4ºCDi', 'Biol', [plaza({ asignatura: 'Biol', subgrupos: ['4ºCDi-DT'] })]),
    actividad(4, 'EFis-mixta', 'BIOL', [plaza({ asignatura: 'BIOL', subgrupos: ['MIXTO'] })]),
    actividad(5, 'Rel-1ºB', null, [
      plaza({ asignatura: 'Relig', subgrupos: ['1ºB-Completo'] }),
      plaza({ asignatura: 'VE', subgrupos: ['1ºB-Completo'] }),
    ]),
    actividad(6, 'Eco-1ºB', 'ECO', [plaza({ asignatura: 'ECO', subgrupos: ['1ºB-Completo'] })]),
    actividad(7, 'Fil-4ºC', 'Mús', [plaza({ profesores: ['ECO'], subgrupos: ['4ºC-DT'] })]),
  ];

  /** Ids de las actividades que pertenecen a lo que casa con `termino`, o `null` si no casa nada. */
  function ids(termino: string): number[] | null {
    const p = casarExacto(termino, CATALOGO);
    return p ? ACTIVIDADES.filter(p.pertenece).map((a) => a.id) : null;
  }

  it('(1) «4ºC» trae las de sus subgrupos y la del mixto, NO la que solo es de 4ºCDi', () => {
    expect(ids('4ºC')).toEqual([2, 4, 7]);
  });

  it('(2) «4ºCDi» trae la suya y la del mixto, que es de los dos grupos', () => {
    expect(ids('4ºCDi')).toEqual([3, 4]);
  });

  it('(3) una actividad con asignatura null casa por la asignatura de su plaza', () => {
    expect(ids('Relig')).toEqual([5]);
    expect(ids('VE')).toEqual([5]);
  });

  it('(4) el nombre completo exacto de una asignatura casa como su código', () => {
    const p = casarExacto('Música', CATALOGO);
    expect(p?.entidades).toEqual([{ tipo: 'asignatura', codigo: 'Mús', nombre: 'Música' }]);
    expect(ids('Música')).toEqual([1, 7]);
  });

  it('(5) mayúsculas, tildes y espacios alrededor no impiden la coincidencia exacta', () => {
    expect(ids('musica')).toEqual([1, 7]);
    expect(ids('  MÚSICA ')).toEqual([1, 7]);
    expect(ids('mus')).toEqual([1, 7]);
    expect(ids('byg1')).toEqual([1]);
  });

  it('(6) los ordinales cuentan: «1B» no casa con el grupo 1ºB, «1ºB» y «1°B» sí', () => {
    expect(casarExacto('1B', CATALOGO)).toBeNull();
    expect(ids('1ºB')).toEqual([1, 5, 6]);
    expect(ids('1°B')).toEqual([1, 5, 6]);
  });

  it('(7) si casan varias entidades se ve la unión, y la línea las nombra todas en orden', () => {
    const p = casarExacto('eco', CATALOGO);
    expect(p?.entidades.map((e) => `${e.tipo} ${e.codigo}`)).toEqual(['profesor ECO', 'asignatura ECO']);
    expect(ids('eco')).toEqual([6, 7]);
    expect(lineaPertenencia(p!.entidades)).toBe(
      'Actividades del profesor ECO · de la asignatura Economía (ECO)',
    );
  });

  it('(8) dentro de un tipo, por código: «Biol» casa con BIOL y con Biol, que la normalización no separa', () => {
    const p = casarExacto('Biol', CATALOGO);
    expect(p?.entidades.map((e) => e.codigo)).toEqual(['BIOL', 'Biol']);
    expect(ids('Biol')).toEqual([2, 3, 4]);
  });

  it('(9) la línea de una sola entidad, para cada tipo', () => {
    const linea = (t: string) => lineaPertenencia(casarExacto(t, CATALOGO)!.entidades);
    expect(linea('BYG1')).toBe('Actividades del profesor BYG1');
    expect(linea('Mús')).toBe('Actividades de la asignatura Música (Mús)');
    expect(linea('4ºC')).toBe('Actividades del grupo 4ºC');
    expect(linea('4ºC-DT')).toBe('Actividades del subgrupo 4ºC-DT');
  });

  it('(10) sin coincidencia exacta no hay pertenencia, y el texto de la fila con el nombre encuentra «Músic»', () => {
    expect(casarExacto('Músic', CATALOGO)).toBeNull();
    const nombres = new Map(CATALOGO.asignaturas.map((a) => [a.codigo, a.nombreCompleto]));
    const mus = ACTIVIDADES[0];
    expect(coincide(mus.codigo, 'Músic')).toBe(false);
    expect(coincide([mus.codigo, ...nombresDeAsignaturas(mus, nombres)].join(' '), 'Músic')).toBe(true);
  });

  it('(11) los nombres de la fila salen de la actividad y de sus plazas, sin repetir y sin códigos desconocidos', () => {
    const nombres = new Map(CATALOGO.asignaturas.map((a) => [a.codigo, a.nombreCompleto]));
    expect(nombresDeAsignaturas(ACTIVIDADES[4], nombres)).toEqual(['Religión', 'Valores Éticos']);
    expect(nombresDeAsignaturas(ACTIVIDADES[0], nombres)).toEqual(['Música']);
    expect(nombresDeAsignaturas(actividad(9, 'X', 'NADA', [plaza({ asignatura: 'NADA' })]), nombres)).toEqual([]);
  });

  it('(12) un término vacío o de solo separadores no casa con nada', () => {
    expect(casarExacto('', CATALOGO)).toBeNull();
    expect(casarExacto('   ', CATALOGO)).toBeNull();
    expect(casarExacto('...', CATALOGO)).toBeNull();
  });
});
