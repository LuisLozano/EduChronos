import { indicePenalizaciones, indiceViolaciones } from './diagnostico';
import { clavePin } from './pines';
import { CeldaRef, Penalizacion, Violacion } from '../models/diagnostico.model';

/** Celda mínima: `plazaCodigo` null es el caso normal (atribución por instancia). */
function celda(actividadCodigo: string, indice: number, plazaCodigo: string | null = null): CeldaRef {
  return { actividadCodigo, indice, plazaCodigo };
}

/** Violación mínima: solo importan la regla y las celdas atribuidas. */
function violacion(regla: string, celdas: CeldaRef[]): Violacion {
  return { regla, recursoCodigo: null, tramoCodigo: null, celdas, descripcion: regla };
}

/** Penalización mínima: la celda va APLANADA, sin plaza (asimetría D15). */
function penalizacion(regla: string, actividadCodigo: string, indice: number, delta: number): Penalizacion {
  return { regla, actividadCodigo, indice, tramoCodigo: null, delta };
}

/**
 * Fixture DISCRIMINANTE del índice: dos violaciones de la MISMA actividad en
 * repeticiones distintas. Existe porque el índice es la mitad de la clave de
 * instancia (D-6) y reutilizar `clavePin` NO hereda la cobertura de su spec:
 * sin este fixture, agrupar solo por `actividadCodigo` pasaría este spec entero.
 */
const DOS_INSTANCIAS_DURAS: Violacion[] = [
  violacion('SOLAPE_PROFESOR', [celda('Mat-1ºA', 1)]),
  violacion('SOLAPE_PROFESOR', [celda('Mat-1ºA', 2)]),
];

/** El mismo fixture discriminante para la mitad blanda. */
const DOS_INSTANCIAS_BLANDAS: Penalizacion[] = [
  penalizacion('VENTANA_PROFESOR', 'Mat-1ºA', 1, 2),
  penalizacion('VENTANA_PROFESOR', 'Mat-1ºA', 2, -1),
];

/**
 * Fixture de `SOLAPE_AULA`: UNA violación que toca DOS instancias distintas, y
 * cada celda lleva su propia plaza no-null (la asimetría D15, el aula se cuenta
 * por plaza). Discrimina dos cosas a la vez: que la violación se indexe bajo
 * todas sus celdas y no solo la primera, y que cada clave conserve SU plaza.
 */
const SOLAPE_AULA = violacion('SOLAPE_AULA', [
  celda('Mat-1ºA', 1, 'Mat-1ºA-P1'),
  celda('LCL-1ºA', 2, 'LCL-1ºA-P1'),
]);

describe('índice de violaciones duras', () => {
  it('(1) la identidad es la INSTANCIA: dos repeticiones de la misma actividad son dos claves', () => {
    const indice = indiceViolaciones(DOS_INSTANCIAS_DURAS);

    // Discriminante: agrupar solo por actividadCodigo daría tamaño 1.
    expect(indice.size).toBe(2);
    expect([...indice.keys()].sort()).toEqual([clavePin('Mat-1ºA', 1), clavePin('Mat-1ºA', 2)].sort());
  });

  it('(2) una violación de N celdas aparece bajo CADA una de sus celdas', () => {
    const indice = indiceViolaciones([SOLAPE_AULA]);

    // Discriminante: indexar solo celdas[0] daría una sola clave.
    expect(indice.size).toBe(2);
    expect(indice.get(clavePin('Mat-1ºA', 1))?.[0].violacion).toBe(SOLAPE_AULA);
    expect(indice.get(clavePin('LCL-1ºA', 2))?.[0].violacion).toBe(SOLAPE_AULA);
  });

  it('(3) cada clave conserva el plazaCodigo DE SU celda, no el de la primera', () => {
    const indice = indiceViolaciones([SOLAPE_AULA]);

    // Discriminante: propagar celdas[0].plazaCodigo daría 'Mat-1ºA-P1' en ambas.
    expect(indice.get(clavePin('Mat-1ºA', 1))?.[0].plazaCodigo).toBe('Mat-1ºA-P1');
    expect(indice.get(clavePin('LCL-1ºA', 2))?.[0].plazaCodigo).toBe('LCL-1ºA-P1');
  });

  it('(4) la celda sin plaza conserva su null: resalte de celda entera, no de sub-entrada', () => {
    const indice = indiceViolaciones(DOS_INSTANCIAS_DURAS);

    expect(indice.get(clavePin('Mat-1ºA', 1))?.[0].plazaCodigo).toBeNull();
  });

  it('(5) sin violaciones el índice está vacío', () => {
    expect(indiceViolaciones([]).size).toBe(0);
  });
});

describe('índice de penalizaciones blandas', () => {
  it('(6) la identidad es la INSTANCIA: dos repeticiones de la misma actividad son dos claves', () => {
    const indice = indicePenalizaciones(DOS_INSTANCIAS_BLANDAS);

    // Discriminante: agrupar solo por actividadCodigo daría tamaño 1.
    expect(indice.size).toBe(2);
    expect(indice.get(clavePin('Mat-1ºA', 1))).toHaveLength(1);
    expect(indice.get(clavePin('Mat-1ºA', 2))?.[0].delta).toBe(-1);
  });

  it('(7) agrupa SIN sumar: dos penalizaciones de una instancia quedan como dos entradas con su signo', () => {
    const mismaInstancia = [
      penalizacion('VENTANA_PROFESOR', 'Mat-1ºA', 1, 2),
      penalizacion('INDISPONIBILIDAD_BLANDA', 'Mat-1ºA', 1, -3),
    ];
    const entradas = indicePenalizaciones(mismaInstancia).get(clavePin('Mat-1ºA', 1)) ?? [];

    // Discriminante: sumar los delta daría una sola entrada de -1.
    expect(entradas).toHaveLength(2);
    expect(entradas.map((p) => p.delta)).toEqual([2, -3]);
  });

  it('(8) sin penalizaciones el índice está vacío', () => {
    expect(indicePenalizaciones([]).size).toBe(0);
  });
});
