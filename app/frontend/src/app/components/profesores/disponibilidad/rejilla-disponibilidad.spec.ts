import { TramoJornadaDTO } from '../../../models/jornada.model';
import { RestriccionHoraria } from '../../../models/restriccion-horaria.model';
import {
  EstadosCeldas,
  Rejilla,
  aRestricciones,
  aplicarPincel,
  claveCelda,
  construirRejilla,
  desdeRestricciones,
  diaDeJornada,
} from './rejilla-disponibilidad';

/**
 * Lógica pura de la rejilla de disponibilidad. Sin TestBed: el módulo no conoce Angular.
 *
 * <p>La malla es la REAL del banco s137, medida en la fase 1 de S167: cinco días, seis
 * lectivos y el recreo tras el tercero (11:00–11:30). Con una malla inventada y simétrica,
 * un desplazamiento de filas o un recreo mal colocado seguirían verdes.
 */

const DIAS_API = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES'];

/** El día tipo del banco s137, en su orden. `null` en `ordenEnDia` = recreo. */
const DIA_TIPO: ReadonlyArray<[string, string, number | null]> = [
  ['08:00', '09:00', 1],
  ['09:00', '10:00', 2],
  ['10:00', '11:00', 3],
  ['11:00', '11:30', null],
  ['11:30', '12:30', 4],
  ['12:30', '13:30', 5],
  ['13:30', '14:30', 6],
];

/** La semana como la sirve `GET /api/jornada`: 35 tramos, `orden` continuo 1..35. */
function jornadaReal(): TramoJornadaDTO[] {
  let orden = 1;
  return DIAS_API.flatMap((dia) =>
    DIA_TIPO.map(([horaInicio, horaFin, ordenEnDia]) => ({
      dia,
      horaInicio,
      horaFin,
      esLectivo: ordenEnDia !== null,
      orden: orden++,
      ordenEnDia,
    })));
}

/** Estados con la rejilla real y la lista dada. */
function montar(lista: RestriccionHoraria[] = []): {
  rejilla: Rejilla;
  estados: EstadosCeldas;
  original: readonly RestriccionHoraria[];
} {
  const rejilla = construirRejilla(jornadaReal());
  return { rejilla, ...desdeRestricciones(rejilla, lista) };
}

/** El estado de una celda, o `undefined` si no es una celda editable. */
function estadoDe(estados: EstadosCeldas, dia: number, ordenEnDia: number): string | undefined {
  return estados.get(claveCelda(dia, ordenEnDia))?.estado;
}

/** Foto plana de los estados, para comparar mapas enteros por igualdad. */
function foto(estados: EstadosCeldas): Record<string, string> {
  return Object.fromEntries([...estados].map(([k, c]) => [k, c.estado]));
}

/**
 * Estado MEZCLADO de partida para los casos del pincel: el lunes tiene los tres estados y
 * el tramo 1 también, así que un pincel que pisara de más o de menos se nota.
 */
const MEZCLA: RestriccionHoraria[] = [
  { tipo: 'DURA', dia: 1, ordenEnDia: 1, motivo: 'guardia' },
  { tipo: 'BLANDA', dia: 1, ordenEnDia: 2, motivo: null },
  { tipo: 'BLANDA', dia: 2, ordenEnDia: 1, motivo: null },
  { tipo: 'DURA', dia: 3, ordenEnDia: 4, motivo: 'reducción' },
  { tipo: 'DURA', dia: 5, ordenEnDia: 6, motivo: null },
];

describe('rejilla-disponibilidad', () => {
  describe('construirRejilla', () => {
    it('(1) con la malla real: seis lectivas, el recreo tras la tercera y las horas tal cual', () => {
      const rejilla = construirRejilla(jornadaReal());

      expect(rejilla.filas).toEqual([
        { tipo: 'lectivo', ordenEnDia: 1, horaInicio: '08:00', horaFin: '09:00' },
        { tipo: 'lectivo', ordenEnDia: 2, horaInicio: '09:00', horaFin: '10:00' },
        { tipo: 'lectivo', ordenEnDia: 3, horaInicio: '10:00', horaFin: '11:00' },
        { tipo: 'recreo', horaInicio: '11:00', horaFin: '11:30' },
        { tipo: 'lectivo', ordenEnDia: 4, horaInicio: '11:30', horaFin: '12:30' },
        { tipo: 'lectivo', ordenEnDia: 5, horaInicio: '12:30', horaFin: '13:30' },
        { tipo: 'lectivo', ordenEnDia: 6, horaInicio: '13:30', horaFin: '14:30' },
      ]);
    });

    it('(2) traduce los cinco días a 1..5 con su nombre de DIAS, aunque lleguen desordenados', () => {
      // Al revés: VIERNES primero. Las columnas salen por NÚMERO, no por orden de llegada.
      const rejilla = construirRejilla(jornadaReal().reverse());

      expect(rejilla.columnas).toEqual([
        { dia: 1, nombre: 'Lunes' },
        { dia: 2, nombre: 'Martes' },
        { dia: 3, nombre: 'Miércoles' },
        { dia: 4, nombre: 'Jueves' },
        { dia: 5, nombre: 'Viernes' },
      ]);
      // Y las filas siguen en el orden de la jornada, no en el de llegada.
      expect(rejilla.filas.map((f) => f.horaInicio)).toEqual(
        ['08:00', '09:00', '10:00', '11:00', '11:30', '12:30', '13:30']);
    });

    it('(3) diaDeJornada: LUNES → 1 … VIERNES → 5, y un día desconocido lanza', () => {
      expect(DIAS_API.map(diaDeJornada)).toEqual([1, 2, 3, 4, 5]);
      expect(() => diaDeJornada('SABADO')).toThrowError(/SABADO/);
    });

    it('(4) las filas salen del PRIMER día presente, el lunes, y de ningún otro', () => {
      // La malla es uniforme por construcción; este caso fija DE QUÉ día se leen las filas
      // si no lo fuera. Se marca el LUNES y se exige verlo: marcar otro día sólo probaría
      // que no se lee ESE, y leer el viernes seguiría verde (medido: mutante (l) de S167).
      const jornada = jornadaReal().map((t) =>
        t.dia === 'LUNES' && t.orden === 1 ? { ...t, horaInicio: '07:55' } : t);

      expect(construirRejilla(jornada).filas[0].horaInicio).toBe('07:55');
    });
  });

  describe('desdeRestricciones', () => {
    it('(5) treinta celdas editables, ninguna en el recreo, y ausente = DISPONIBLE', () => {
      const { estados } = montar();

      expect(estados.size).toBe(30);
      expect([...estados.keys()].every((k) => /^[1-5]-[1-6]$/.test(k))).toBe(true);
      expect([...estados.values()].every((c) => c.estado === 'DISPONIBLE')).toBe(true);
    });

    it('(6) cada fila pinta SU celda y el resto queda DISPONIBLE', () => {
      const { estados } = montar(MEZCLA);

      expect(estadoDe(estados, 1, 1)).toBe('DURA');
      expect(estadoDe(estados, 1, 2)).toBe('BLANDA');
      expect(estadoDe(estados, 2, 1)).toBe('BLANDA');
      expect(estadoDe(estados, 3, 4)).toBe('DURA');
      expect(estadoDe(estados, 5, 6)).toBe('DURA');
      expect([...estados.values()].filter((c) => c.estado === 'DISPONIBLE')).toHaveLength(25);
    });

    it('(7) conserva la lista original entera, huérfanas incluidas', () => {
      const huerfana: RestriccionHoraria = { tipo: 'DURA', dia: 2, ordenEnDia: 9, motivo: 'x' };
      const { original, estados } = montar([...MEZCLA, huerfana]);

      expect(original).toEqual([...MEZCLA, huerfana]);
      expect(estados.has(claveCelda(2, 9))).toBe(false);
    });
  });

  describe('aplicarPincel', () => {
    it('(8) en una CELDA cambia esa y sólo esa', () => {
      const { estados } = montar(MEZCLA);
      const antes = foto(estados);

      const despues = aplicarPincel(estados, 'BLANDA', { tipo: 'celda', dia: 4, ordenEnDia: 3 });

      expect(foto(despues)).toEqual({ ...antes, '4-3': 'BLANDA' });
    });

    it('(9) en un DÍA pinta sus seis tramos lectivos y ningún otro día', () => {
      const { estados } = montar(MEZCLA);
      const antes = foto(estados);

      const despues = aplicarPincel(estados, 'DURA', { tipo: 'dia', dia: 1 });

      const esperado = { ...antes };
      for (let o = 1; o <= 6; o++) {
        esperado[`1-${o}`] = 'DURA';
      }
      expect(foto(despues)).toEqual(esperado);
    });

    it('(10) en un TRAMO pinta ese ordenEnDia en los cinco días y nada más', () => {
      const { estados } = montar(MEZCLA);
      const antes = foto(estados);

      const despues = aplicarPincel(estados, 'BLANDA', { tipo: 'tramo', ordenEnDia: 1 });

      const esperado = { ...antes };
      for (let d = 1; d <= 5; d++) {
        esperado[`${d}-1`] = 'BLANDA';
      }
      expect(foto(despues)).toEqual(esperado);
    });

    it('(11) DISPONIBLE borra: la celda queda sin fila en el PUT', () => {
      const { estados, original } = montar(MEZCLA);

      const despues = aplicarPincel(estados, 'DISPONIBLE', { tipo: 'celda', dia: 1, ordenEnDia: 1 });

      expect(estadoDe(despues, 1, 1)).toBe('DISPONIBLE');
      const put = aRestricciones(despues, original);
      expect(put.some((r) => r.dia === 1 && r.ordenEnDia === 1)).toBe(false);
      expect(put).toHaveLength(MEZCLA.length - 1);
    });

    it('(12) es INMUTABLE: la entrada no cambia y la salida es otro mapa', () => {
      const { estados } = montar(MEZCLA);
      const antes = foto(estados);

      const despues = aplicarPincel(estados, 'DURA', { tipo: 'dia', dia: 2 });

      expect(despues).not.toBe(estados);
      expect(foto(estados)).toEqual(antes);
    });

    it('(13) un objetivo sin celda no crea celdas: el pincel sólo pinta lo que existe', () => {
      const { estados } = montar(MEZCLA);

      const despues = aplicarPincel(estados, 'DURA', { tipo: 'celda', dia: 1, ordenEnDia: 7 });

      expect(despues.size).toBe(30);
      expect(foto(despues)).toEqual(foto(estados));
    });
  });

  describe('aRestricciones', () => {
    it('(14) conserva el motivo si la celda sigue con el MISMO tipo', () => {
      const { estados, original } = montar(MEZCLA);

      // Repintar DURA sobre DURA no es un cambio: el motivo sigue siendo verdad.
      const despues = aplicarPincel(estados, 'DURA', { tipo: 'celda', dia: 1, ordenEnDia: 1 });

      expect(aRestricciones(despues, original)).toContainEqual(
        { tipo: 'DURA', dia: 1, ordenEnDia: 1, motivo: 'guardia' });
    });

    it('(15) pierde el motivo si la celda CAMBIA de tipo', () => {
      const { estados, original } = montar(MEZCLA);

      const despues = aplicarPincel(estados, 'BLANDA', { tipo: 'celda', dia: 1, ordenEnDia: 1 });

      expect(aRestricciones(despues, original)).toContainEqual(
        { tipo: 'BLANDA', dia: 1, ordenEnDia: 1, motivo: null });
    });

    it('(16) pierde el motivo si la celda pasa a DISPONIBLE: no queda fila que lo lleve', () => {
      const { estados, original } = montar(MEZCLA);

      const despues = aplicarPincel(estados, 'DISPONIBLE', { tipo: 'celda', dia: 3, ordenEnDia: 4 });

      const put = aRestricciones(despues, original);
      expect(put.some((r) => r.motivo === 'reducción')).toBe(false);
      expect(put.some((r) => r.dia === 3 && r.ordenEnDia === 4)).toBe(false);
    });

    it('(17) una fila huérfana —tramo que no está en la rejilla— se reenvía TAL CUAL', () => {
      const huerfana: RestriccionHoraria = { tipo: 'BLANDA', dia: 2, ordenEnDia: 9, motivo: 'antigua' };
      const { estados, original } = montar([...MEZCLA, huerfana]);

      expect(aRestricciones(estados, original)).toContainEqual(huerfana);
    });

    it('(18) el PUT sale ordenado por (dia, ordenEnDia), con la huérfana en su sitio', () => {
      const huerfana: RestriccionHoraria = { tipo: 'BLANDA', dia: 2, ordenEnDia: 9, motivo: null };
      const { estados, original } = montar([huerfana]);

      // Una celda del día 3 y otra del día 1: la huérfana del día 2 tiene que caer EN
      // MEDIO. Sin ordenar, iría al final.
      let despues = aplicarPincel(estados, 'DURA', { tipo: 'celda', dia: 3, ordenEnDia: 2 });
      despues = aplicarPincel(despues, 'DURA', { tipo: 'celda', dia: 1, ordenEnDia: 5 });

      expect(aRestricciones(despues, original).map((r) => [r.dia, r.ordenEnDia])).toEqual([
        [1, 5],
        [2, 9],
        [3, 2],
      ]);
    });

    it('(19) ida y vuelta: sin tocar nada devuelve la lista original, motivos incluidos', () => {
      // Ordenada como la sirve el backend y con una huérfana en medio.
      const lista: RestriccionHoraria[] = [
        { tipo: 'DURA', dia: 1, ordenEnDia: 1, motivo: 'guardia' },
        { tipo: 'BLANDA', dia: 1, ordenEnDia: 2, motivo: null },
        { tipo: 'BLANDA', dia: 2, ordenEnDia: 1, motivo: null },
        { tipo: 'DURA', dia: 2, ordenEnDia: 9, motivo: 'huérfana' },
        { tipo: 'DURA', dia: 3, ordenEnDia: 4, motivo: 'reducción' },
        { tipo: 'DURA', dia: 5, ordenEnDia: 6, motivo: null },
      ];
      const { estados, original } = montar(lista);

      expect(aRestricciones(estados, original)).toEqual(lista);
    });
  });
});
