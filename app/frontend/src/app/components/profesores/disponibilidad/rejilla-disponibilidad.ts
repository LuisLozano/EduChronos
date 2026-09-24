import { DIAS } from '../../../horario/proyeccion';
import { TramoJornadaDTO } from '../../../models/jornada.model';
import {
  RestriccionHoraria,
  RestriccionHorariaRequest,
  TipoRestriccion,
} from '../../../models/restriccion-horaria.model';

/**
 * Lógica PURA de la rejilla de disponibilidad de un profesor (C-rejilla-disponibilidad,
 * S167): derivar la rejilla de la jornada, pintar con el pincel y convertir entre las
 * celdas y la lista del `PUT`. Sin Angular y sin estado: cada función recibe lo que
 * necesita y devuelve un valor NUEVO.
 *
 * <p><b>El día tiene dos formas, y se traduce AQUÍ y sólo aquí.</b> `GET /api/jornada`
 * sirve `'LUNES'`…`'VIERNES'`; las restricciones viajan con un entero 1..5
 * (`Dia.ordinal() + 1`, `RestriccionHorariaService.java:190, :229`). Toda la rejilla
 * trabaja con el entero; {@link diaDeJornada} es el único punto que conoce la cadena.
 *
 * <p><b>Filas de UN día.</b> La jornada es idéntica los cinco días por construcción: el
 * `PUT` recibe un día tipo y el backend lo expande (`JornadaService.java:164-170`). Por
 * eso las filas salen del primer día y las columnas son los días presentes. Es el mismo
 * criterio que `horario/recreo.ts`.
 *
 * <p><b>Horas sin conversión.</b> Las filas llevan las cadenas `"HH:mm"` de la API tal
 * cual. Esta capa no decide si se pintan: eso es de la plantilla.
 */

/** Estado de una celda. `DISPONIBLE` = sin fila en el `PUT`. */
export type Estado = 'DISPONIBLE' | TipoRestriccion;

/** Clave de una celda, derivada de `(dia 1..5, ordenEnDia)`. */
export type ClaveCelda = string;

export function claveCelda(dia: number, ordenEnDia: number): ClaveCelda {
  return `${dia}-${ordenEnDia}`;
}

/** Una columna: un día, con su número de contrato (1..5) y su nombre de `DIAS`. */
export interface ColumnaDia {
  dia: number;
  nombre: string;
}

/** Fila de tramo LECTIVO: editable. */
export interface FilaLectiva {
  tipo: 'lectivo';
  ordenEnDia: number;
  horaInicio: string;
  horaFin: string;
}

/** Fila de RECREO: separadora, sin número y NO editable. */
export interface FilaRecreo {
  tipo: 'recreo';
  horaInicio: string;
  horaFin: string;
}

export type Fila = FilaLectiva | FilaRecreo;

export interface Rejilla {
  columnas: readonly ColumnaDia[];
  filas: readonly Fila[];
}

/** Una celda editable y su estado. Lleva sus coordenadas para no tener que parsear la clave. */
export interface Celda {
  dia: number;
  ordenEnDia: number;
  estado: Estado;
}

/** Estado de TODAS las celdas editables de una rejilla. Inmutable. */
export type EstadosCeldas = ReadonlyMap<ClaveCelda, Celda>;

/** A qué se aplica el pincel. El recreo no tiene forma de ser objetivo: no tiene número. */
export type Objetivo =
  | { tipo: 'celda'; dia: number; ordenEnDia: number }
  | { tipo: 'dia'; dia: number }
  | { tipo: 'tramo'; ordenEnDia: number };

/** El `name()` del enum `Dia` del backend, en orden, frente a su número de contrato. */
const DIA_DE_JORNADA: Readonly<Record<string, number>> = {
  LUNES: 1,
  MARTES: 2,
  MIERCOLES: 3,
  JUEVES: 4,
  VIERNES: 5,
};

/**
 * `'LUNES'` → 1 … `'VIERNES'` → 5. Un día desconocido LANZA: pintarlo en una columna
 * inventada o descartarlo en silencio sería afirmar una malla que no es la del centro.
 */
export function diaDeJornada(dia: string): number {
  const numero = DIA_DE_JORNADA[dia];
  if (numero === undefined) {
    throw new Error(`Día de jornada desconocido: '${dia}'`);
  }
  return numero;
}

/**
 * (3a) La rejilla de la jornada: columnas = los días presentes, en orden 1..5, con el
 * nombre de `DIAS`; filas = los tramos del PRIMER día en su `orden`, lectivos y recreos.
 */
export function construirRejilla(tramos: readonly TramoJornadaDTO[]): Rejilla {
  const numerados = tramos.map((t) => ({ t, dia: diaDeJornada(t.dia) }));
  const dias = [...new Set(numerados.map((n) => n.dia))].sort((a, b) => a - b);
  const columnas = dias.map((dia) => ({ dia, nombre: DIAS[dia - 1] }));
  if (dias.length === 0) {
    return { columnas, filas: [] };
  }
  const primero = dias[0];
  const filas: Fila[] = numerados
    .filter((n) => n.dia === primero)
    .map((n) => n.t)
    .sort((a, b) => a.orden - b.orden)
    .map((t): Fila =>
      t.esLectivo && t.ordenEnDia !== null
        ? { tipo: 'lectivo', ordenEnDia: t.ordenEnDia, horaInicio: t.horaInicio, horaFin: t.horaFin }
        : { tipo: 'recreo', horaInicio: t.horaInicio, horaFin: t.horaFin });
  return { columnas, filas };
}

/** Las celdas editables: cada columna por cada fila LECTIVA. El recreo no aporta ninguna. */
function celdasEditables(rejilla: Rejilla): Celda[] {
  const lectivas = rejilla.filas.filter((f): f is FilaLectiva => f.tipo === 'lectivo');
  const celdas: Celda[] = [];
  for (const c of rejilla.columnas) {
    for (const f of lectivas) {
      celdas.push({ dia: c.dia, ordenEnDia: f.ordenEnDia, estado: 'DISPONIBLE' });
    }
  }
  return celdas;
}

/**
 * (3b) El estado de cada celda a partir de la lista del `GET`: ausente = `DISPONIBLE`.
 * Devuelve también la lista ORIGINAL, que {@link aRestricciones} necesita para conservar
 * los motivos y reenviar las filas que no caen en ninguna celda.
 */
export function desdeRestricciones(
  rejilla: Rejilla,
  lista: readonly RestriccionHoraria[],
): { estados: EstadosCeldas; original: readonly RestriccionHoraria[] } {
  const estados = new Map<ClaveCelda, Celda>();
  for (const celda of celdasEditables(rejilla)) {
    estados.set(claveCelda(celda.dia, celda.ordenEnDia), celda);
  }
  for (const r of lista) {
    const clave = claveCelda(r.dia, r.ordenEnDia);
    const celda = estados.get(clave);
    if (celda) {
      estados.set(clave, { ...celda, estado: r.tipo });
    }
  }
  return { estados, original: [...lista] };
}

/** ¿Cae esta celda bajo el objetivo? */
function alcanza(objetivo: Objetivo, celda: Celda): boolean {
  switch (objetivo.tipo) {
    case 'celda':
      return celda.dia === objetivo.dia && celda.ordenEnDia === objetivo.ordenEnDia;
    case 'dia':
      return celda.dia === objetivo.dia;
    case 'tramo':
      return celda.ordenEnDia === objetivo.ordenEnDia;
  }
}

/**
 * (3c) Aplica el pincel y devuelve un mapa NUEVO; la entrada no se toca. Día = todos los
 * tramos lectivos de ese día; tramo = ese `ordenEnDia` en todos los días. Un objetivo que
 * no casa con ninguna celda editable no cambia nada: el pincel no CREA celdas.
 */
export function aplicarPincel(
  estados: EstadosCeldas,
  pincel: Estado,
  objetivo: Objetivo,
): EstadosCeldas {
  const nuevo = new Map<ClaveCelda, Celda>();
  for (const [clave, celda] of estados) {
    nuevo.set(clave, alcanza(objetivo, celda) ? { ...celda, estado: pincel } : celda);
  }
  return nuevo;
}

function porDiaYTramo(a: RestriccionHoraria, b: RestriccionHoraria): number {
  return a.dia - b.dia || a.ordenEnDia - b.ordenEnDia;
}

/**
 * (3d) La lista para el `PUT`, ordenada por (dia, ordenEnDia).
 *
 * <ul>
 *   <li>`DISPONIBLE` no produce fila.
 *   <li>El `motivo` se conserva SÓLO si la celda tenía fila en el original con el MISMO
 *       tipo. Si cambia de tipo, `null`: el motivo explicaba la restricción anterior, no
 *       ésta. El `PUT` es reemplazo total, así que no reenviarlo es borrarlo.
 *   <li>Una fila del original cuyo tramo NO es una celda de la rejilla se reenvía TAL CUAL.
 *       Omitirla la borraría en silencio: el usuario no la ve y no puede haberla quitado.
 * </ul>
 */
export function aRestricciones(
  estados: EstadosCeldas,
  original: readonly RestriccionHoraria[],
): RestriccionHorariaRequest[] {
  const previas = new Map<ClaveCelda, RestriccionHoraria>();
  for (const r of original) {
    const clave = claveCelda(r.dia, r.ordenEnDia);
    if (!previas.has(clave)) {
      previas.set(clave, r);
    }
  }

  const filas: RestriccionHorariaRequest[] = [];
  for (const [clave, celda] of estados) {
    if (celda.estado === 'DISPONIBLE') {
      continue;
    }
    const previa = previas.get(clave);
    filas.push({
      tipo: celda.estado,
      dia: celda.dia,
      ordenEnDia: celda.ordenEnDia,
      motivo: previa && previa.tipo === celda.estado ? previa.motivo : null,
    });
  }
  for (const r of original) {
    if (!estados.has(claveCelda(r.dia, r.ordenEnDia))) {
      filas.push({ ...r });
    }
  }
  return filas.sort(porDiaYTramo);
}
