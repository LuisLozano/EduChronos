import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HorarioGrid } from './horario-grid';
import { SesionVista } from '../../models/horario.model';
import { Violacion } from '../../models/diagnostico.model';
import { ViolacionEnCelda } from '../../horario/diagnostico';

/**
 * Rejilla AISLADA: sin colaboradores, inputs por `setInput` y asertos por DOM. La
 * rejilla no habla con ningún servicio —emite `soltar` y `despinar` y el
 * contenedor decide—, así que aquí no hay dobles que montar.
 *
 * <p>Bajo zoneless (sin zone.js y sin `provideZoneChangeDetection`, el bootstrap
 * de Angular 21 antepone el modo zoneless), el render se espera con
 * `await fixture.whenStable()`, NUNCA con `detectChanges()`.
 */

/** Slot único: las dos instancias del fixture comparten `(dia, tramo)`. */
const DIA = 1;
const TRAMO = 1;

/**
 * Sesión mínima. Solo importan la actividad, el índice y la asignatura —lo que la
 * rejilla pinta y lo que este spec usa para localizar cada instancia—; los demás
 * campos obligatorios de `SesionVista` se rellenan con constantes inertes:
 * cambiarlas no puede mover ningún aserto de este fichero.
 */
function sesion(
  sesionId: number,
  actividadCodigo: string,
  indice: number,
  asignaturaCodigo: string,
  plazaCodigo: string = `${actividadCodigo}-P1`,
): SesionVista {
  return {
    sesionId,
    indice,
    dia: DIA,
    tramo: TRAMO,
    asignaturaCodigo,
    asignaturaNombre: asignaturaCodigo,
    profesores: ['PROF1'],
    aulaCodigo: 'A1',
    subgrupos: ['1ºA-Completo'],
    grupos: ['1ºA'],
    actividadCodigo,
    plazaCodigo,
  };
}

/**
 * Violación inerte para los resaltes: la rejilla solo mira el `plazaCodigo` de
 * cada {@link ViolacionEnCelda} —nunca el contenido de la `Violacion`— para
 * aplicar una clase, así que basta un objeto mínimo. `enCelda(plaza)` fabrica la
 * entrada indexada: plaza null = violación de instancia (profesor/subgrupo),
 * plaza no-null = violación de aula de ESA plaza (D15).
 */
const VIOL: Violacion = {
  regla: 'SOLAPE_AULA',
  recursoCodigo: null,
  tramoCodigo: null,
  celdas: [],
  descripcion: 'inerte',
};

function enCelda(plazaCodigo: string | null): ViolacionEnCelda {
  return { violacion: VIOL, plazaCodigo };
}

/**
 * Desdoble: UNA instancia (`Mat-1ºA`, índice 2) con DOS sub-entradas de plazas
 * distintas en el MISMO slot. `agruparPorActividad` las reúne en una sola
 * `InstanciaCelda` con dos `entradas` en orden de aparición (P1, P2), que es lo
 * que exigen T3 (casar la SEGUNDA) y T5 (casar las dos).
 */
const DESDOBLE_P1 = sesion(10, 'Mat-1ºA', 2, 'Mat', 'Mat-1ºA-P1');
const DESDOBLE_P2 = sesion(11, 'Mat-1ºA', 2, 'Mat', 'Mat-1ºA-P2');
const DESDOBLE: readonly SesionVista[] = [DESDOBLE_P1, DESDOBLE_P2];

/**
 * Dos instancias en el MISMO slot: una pinada y otra no. Compartir slot es lo que
 * hace escopado al aserto (7): si cada una viviera en su celda, "no hay candado"
 * podría pasar por no haberse pintado nada.
 *
 * <p>El índice de la pinada es 2, no 1, a propósito: con índice 1 una
 * implementación que fijara `|1` a mano seguiría verde y la dimensión del índice
 * de la clave quedaría sin medir.
 */
const MAT_PINADA = sesion(1, 'Mat-1ºA', 2, 'Mat');
const LCL_SIN_PIN = sesion(2, 'LCL-1ºA', 1, 'LCL');
const SESIONES: readonly SesionVista[] = [MAT_PINADA, LCL_SIN_PIN];

/** El valor (7) es el id del bloqueo: la rejilla lo recibe y NO debe emitirlo. */
const PINADAS = new Map<string, number | null>([['Mat-1ºA|2', 7]]);

/**
 * Localiza una instancia por la asignatura que pinta. NO se escopa por la clase
 * `.pinada`: esa clase la decide `estaPinada`, que es justo el predicado bajo
 * prueba, y usarla para seleccionar volvería circular al aserto (7).
 */
function instanciaDe(fixture: ComponentFixture<HorarioGrid>, asignatura: string): HTMLElement {
  const raiz = fixture.nativeElement as HTMLElement;
  const encontrada = Array.from(raiz.querySelectorAll<HTMLElement>('div.instancia')).find(
    (d) => d.querySelector('.asig')?.textContent?.trim() === asignatura,
  );
  if (!encontrada) {
    throw new Error(`No se pintó ninguna instancia de ${asignatura}`);
  }
  return encontrada;
}

describe('rejilla de horario', () => {
  let fixture: ComponentFixture<HorarioGrid>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HorarioGrid],
    }).compileComponents();

    fixture = TestBed.createComponent(HorarioGrid);
    fixture.componentRef.setInput('sesiones', SESIONES);
    fixture.componentRef.setInput('pinadas', PINADAS);
    await fixture.whenStable();
  });

  /**
   * El click va directo sobre el botón: verificado por lectura de
   * `@angular/cdk` 21.2.14 que `drag-drop.mjs` no registra NINGÚN listener de
   * `click` —solo `mousedown`, `touchstart` y `dragstart`—, así que el `cdkDrag`
   * que envuelve al candado no puede interceptarlo. Sintetizarlo con
   * `mousedown` + `mouseup` sí lo vería el CDK, y jsdom además no deriva `click`
   * de ese par.
   *
   * <p>Vigila dos regresiones concretas: emitir el `id` del bloqueo (7), que la
   * rejilla recibe en el input `pinadas` pero cuyo vocabulario no le pertenece, y
   * emitir el `actividadCodigo` a secas, que pierde la dimensión del índice y
   * confundiría dos repeticiones de la misma actividad. Ninguna de las dos lleva
   * aserto propio: `toHaveBeenCalledWith('Mat-1ºA|2')` junto a
   * `toHaveBeenCalledTimes(1)` fija el argumento de la ÚNICA emisión, de modo que
   * el id, la actividad sola o cualquier tercera forma ponen rojo el test.
   * Añadir `not.toHaveBeenCalledWith(...)` sobre esos dos valores no podría
   * fallar por separado nunca: sería cobertura fingida.
   */
  it('(6) la instancia pinada tiene candado, y el click emite la CLAVE, no el id ni la actividad a secas', async () => {
    const espia = vi.fn();
    fixture.componentInstance.despinar.subscribe(espia);

    const candado = instanciaDe(fixture, 'Mat').querySelector<HTMLButtonElement>('button.candado');
    expect(candado).not.toBeNull();

    candado!.click();
    await fixture.whenStable();

    expect(espia).toHaveBeenCalledTimes(1);
    // Literal, y no `clavePin('Mat-1ºA', 2)`: esta línea es el ÚNICO punto del
    // repo que fija el FORMATO de la clave. `pines.spec.ts` no lo fija —asevera
    // con la propia `clavePin`, así que un cambio de separador lo dejaría
    // verde—, y la rejilla la consume en vez de declararla. Si el formato se
    // mueve, cae aquí y en ningún otro sitio.
    expect(espia).toHaveBeenCalledWith('Mat-1ºA|2');
  });

  it('(7) la instancia SIN pin no tiene candado, aunque comparta slot con una pinada', () => {
    const raiz = fixture.nativeElement as HTMLElement;
    // Las dos se pintaron: sin esto, "no hay candado" podría estar mirando un
    // slot vacío en vez de una instancia sin pin.
    expect(raiz.querySelectorAll('div.instancia').length).toBe(2);

    expect(instanciaDe(fixture, 'LCL').querySelector('button.candado')).toBeNull();
    // Contrapunto en el MISMO render: sin él, un `[pinadas]` roto del todo daría
    // cero candados y el aserto pasaría por la razón equivocada.
    expect(instanciaDe(fixture, 'Mat').querySelector('button.candado')).not.toBeNull();
  });

  /**
   * El badge pinta el número que RECIBE, sin derivarlo de nada de la sesión (el
   * -3 no es id, ni índice, ni sesionId de ningún fixture). Se pone sobre la
   * instancia SIN pin (LCL) a propósito: así la pinada (Mat) queda pinada y SIN
   * badge, que es lo único que discrimina "reservar hueco por candado" —con la
   * mutación `con-badge = tieneBadge || estaPinada`, Mat ganaría la clase; sin
   * ella, no—.
   */
  it('(15) el badge pinta lo que recibe; sin clave en el Map no hay badge ni clase con-badge', async () => {
    fixture.componentRef.setInput('badges', new Map<string, number>([['LCL-1ºA|1', -3]]));
    await fixture.whenStable();

    const lcl = instanciaDe(fixture, 'LCL');
    const badge = lcl.querySelector('.badge');
    expect(badge).not.toBeNull();
    expect(badge!.textContent?.trim()).toBe('-3');
    expect(lcl.classList).toContain('con-badge');

    // Mat está pinada pero NO tiene clave en badges: ni badge ni hueco reservado.
    const mat = instanciaDe(fixture, 'Mat');
    expect(mat.querySelector('.badge')).toBeNull();
    expect(mat.classList).not.toContain('con-badge');
  });

  /**
   * Resaltes de violación DURA (8.6-iii-B2-b), las dos granularidades de D15. Los
   * asertos van por `classList`/estructura DOM, NUNCA por `getComputedStyle`: no
   * hay precedente en la suite y jsdom + la encapsulación `_ngcontent` de Angular
   * lo hacen poco fiable.
   *
   * <p>NO se asevera que el resalte conserve el `border-left` estructural de
   * `.entrada` (3px #4a7): `outline` y `border` son cajas ORTOGONALES y `outline`
   * no puede desalojar `border-left` POR CONSTRUCCIÓN, así que no hay nada que un
   * test de unidad pueda romper ahí. Comprobarlo exigiría leer estilos computados
   * —un test visual de navegador real—, y eso queda fuera de vitest. Es prosa,
   * no aserto.
   */
  it('(16) plaza null: se resalta la INSTANCIA entera y ninguna sub-entrada', async () => {
    fixture.componentRef.setInput('violaciones', new Map([['LCL-1ºA|1', [enCelda(null)]]]));
    await fixture.whenStable();

    const lcl = instanciaDe(fixture, 'LCL');
    expect(lcl.classList).toContain('en-violacion');
    // La violación de instancia NO baja a las sub-entradas: la plaza null no casa
    // con ninguna plaza concreta.
    expect(lcl.querySelectorAll('.entrada.en-violacion').length).toBe(0);
  });

  it('(17) plaza no-null: se resalta SOLO la sub-entrada que casa; ni la hermana ni la instancia', async () => {
    fixture.componentRef.setInput('sesiones', DESDOBLE);
    fixture.componentRef.setInput('violaciones', new Map([['Mat-1ºA|2', [enCelda('Mat-1ºA-P2')]]]));
    await fixture.whenStable();

    const inst = instanciaDe(fixture, 'Mat');
    const entradas = inst.querySelectorAll('.entrada');
    expect(entradas.length).toBe(2);

    // Orden de aparición: [0] es P1, [1] es P2. La violación casa con P2.
    expect(entradas[0].classList).not.toContain('en-violacion');
    expect(entradas[1].classList).toContain('en-violacion');
    // Aula ≠ instancia: la celda entera NO se tiñe.
    expect(inst.classList).not.toContain('en-violacion');
  });

  it('(18) clave ausente del mapa: cero resaltes en toda la celda', async () => {
    // Clave que no corresponde a ninguna instancia pintada: prueba que el resalte
    // es por lookup de clave, no "el mapa está vacío".
    fixture.componentRef.setInput('violaciones', new Map([['NOEXISTE|9', [enCelda(null)]]]));
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;
    expect(raiz.querySelectorAll('.en-violacion').length).toBe(0);
  });

  it('(19) desdoble: una violación con dos celdas, una por plaza, resalta LAS DOS sub-entradas', async () => {
    // Espejo de diagnostico.spec (9): una única Violacion indexada bajo la clave
    // con dos entradas, una por plaza.
    fixture.componentRef.setInput('sesiones', DESDOBLE);
    fixture.componentRef.setInput(
      'violaciones',
      new Map([['Mat-1ºA|2', [enCelda('Mat-1ºA-P1'), enCelda('Mat-1ºA-P2')]]]),
    );
    await fixture.whenStable();

    const inst = instanciaDe(fixture, 'Mat');
    const entradas = inst.querySelectorAll('.entrada');
    expect(entradas.length).toBe(2);

    // Cada sub-entrada casa con SU plaza: las dos marcadas, evaluadas por separado.
    expect(entradas[0].classList).toContain('en-violacion');
    expect(entradas[1].classList).toContain('en-violacion');
  });
});
