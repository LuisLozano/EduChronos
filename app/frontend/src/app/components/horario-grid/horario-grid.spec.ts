import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

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

/**
 * DebugElement de la instancia que pinta `asignatura`. Hace falta el DebugElement
 * —no el nativo— para disparar los outputs del `cdkDrag` (`cdkDragStarted` /
 * `cdkDragEnded`) con `triggerEventHandler`, que invoca el handler cableado en la
 * plantilla sin simular el gesto de puntero, irreproducible en jsdom.
 */
function debugDe(fixture: ComponentFixture<HorarioGrid>, asignatura: string): DebugElement {
  const de = fixture.debugElement
    .queryAll(By.css('div.instancia'))
    .find((d) => (d.nativeElement as HTMLElement).querySelector('.asig')?.textContent?.trim() === asignatura);
  if (!de) {
    throw new Error(`No se pintó ninguna instancia de ${asignatura}`);
  }
  return de;
}

/** El `<td>` del slot (dia, tramo): fila `tramo`, columna `dia` (la 1ª celda es `<th>`). */
function tdDe(fixture: ComponentFixture<HorarioGrid>, dia: number, tramo: number): HTMLTableCellElement {
  const raiz = fixture.nativeElement as HTMLElement;
  const fila = raiz.querySelectorAll('tbody tr')[tramo - 1];
  return fila.querySelectorAll('td')[dia - 1] as HTMLTableCellElement;
}

/** Cambia la lista de grupos de una sesión sin tocar el helper base {@link sesion}. */
function conGrupos(s: SesionVista, grupos: readonly string[]): SesionVista {
  return { ...s, grupos: [...grupos] };
}

/** Reubica una sesión en otro (dia, tramo) sin tocar el helper base {@link sesion}. */
function enSlot(s: SesionVista, dia: number, tramo: number): SesionVista {
  return { ...s, dia, tramo };
}

/**
 * Instancia de SEIS plazas: seis sub-entradas de la MISMA instancia
 * (`Bloque-1ºA`, índice 3) en el mismo slot, con asignaturas y plazas distintas.
 * Es la celda peor del centro real —22 de las 791 lo son— y la única forma de
 * que el recorte esconda más de una plaza.
 *
 * <p>Asignaturas distintas a propósito: el `title` de la marca lista TODAS las
 * plazas, y con seis «Mat» ese aserto no distinguiría una implementación que
 * repitiera la primera. El índice 3 sigue el criterio del fichero de no usar 1,
 * que una implementación podría fijar a mano.
 */
const ASIGS_BLOQUE = ['Mat', 'LCL', 'ING', 'FIS', 'QUI', 'BIO'] as const;
const BLOQUE_6: readonly SesionVista[] = ASIGS_BLOQUE.map((a, i) =>
  sesion(100 + i, 'Bloque-1ºA', 3, a, `Bloque-1ºA-P${i + 1}`),
);

/** Geometría de S126, la misma que usan (12)-(14) de `horario/oculto.spec.ts`. */
const PASO_PX = 21.7;
const ALTO_DEL_MODELO: Readonly<Record<number, number>> = { 4: 110.8, 5: 132.5, 6: 154.2 };
/** Hueco por fila medido en S126: 716 útiles entre seis tramos. */
const DISPONIBLE_PX = 109.57;

/** Original guardado UNA vez, para que el `afterEach` pueda restaurarlo siempre. */
const RECT_ORIGINAL = Element.prototype.getBoundingClientRect;

function rectDe(top: number, bottom: number): DOMRect {
  return new DOMRect(0, top, 0, bottom - top);
}

/**
 * Sustituye `getBoundingClientRect` por la geometría del modelo. En jsdom el
 * original devuelve ceros SIEMPRE —medido en el sondeo de S127—, así que sin esto
 * `ocultasEnCelda` devuelve `null` y no hay nada que afirmar (caso 27).
 *
 * <p>Discrimina por `classList`, NUNCA por orden de llamada: el recorrido de
 * {@link HorarioGrid} visita el DOM en un orden que la plantilla puede cambiar, y
 * un stub que contara llamadas convertiría cualquier reordenación en un fallo
 * falso. El índice de cada plaza se saca de su posición ENTRE SUS HERMANAS, que
 * es un hecho del DOM y no del recorrido.
 *
 * <p>Se instala DENTRO del test que lo usa y jamás en un `beforeEach` global: la
 * marca vive dentro de `.rotulo`, así que una instancia marcada haría que el
 * `textContent` del rótulo fuese `'2 simultáneas+2'` y tumbaría al caso (20),
 * que es ajeno a D11.
 */
function instalarStubDeRects(): void {
  Element.prototype.getBoundingClientRect = function (this: Element): DOMRect {
    if (this.classList.contains('celda')) {
      return rectDe(0, DISPONIBLE_PX);
    }
    if (this.classList.contains('entrada')) {
      const hermanas = Array.from(this.parentElement?.querySelectorAll('.entrada') ?? []);
      const i = hermanas.indexOf(this);
      const n = hermanas.length;
      const fin = ALTO_DEL_MODELO[n];
      if (i < 0 || fin === undefined) {
        return rectDe(0, 0);
      }
      const bottom = fin - (n - 1 - i) * PASO_PX;
      return rectDe(bottom - PASO_PX, bottom);
    }
    return rectDe(0, 0);
  };
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
   * Restaura el original aunque el test haya fallado: `afterEach` corre igual, y
   * dejar el stub puesto contaminaría a TODO el fichero (empezando por el caso
   * 20). Es idempotente, así que corre también tras los tests que no lo instalan.
   */
  afterEach(() => {
    Element.prototype.getBoundingClientRect = RECT_ORIGINAL;
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

  it('(T1) al iniciar arrastre, el <td> de un slot con instancias lleva .ocupado', async () => {
    // beforeEach monta SESIONES: Mat y LCL comparten el slot (DIA, TRAMO).
    const td = tdDe(fixture, DIA, TRAMO);
    // Precondición: en reposo nada está marcado, para no medir un no-op.
    expect(td.classList).not.toContain('ocupado');

    debugDe(fixture, 'Mat').triggerEventHandler('cdkDragStarted', {});
    await fixture.whenStable();

    // Arrastrando Mat, el slot conserva a LCL: sigue teniendo clase, luego ocupado.
    expect(td.classList).toContain('ocupado');
  });

  it('(T2) el <td> del slot de ORIGEN de la instancia arrastrada NO lleva .ocupado', async () => {
    // Mat SOLA en (DIA, TRAMO); LCL en OTRO slot (2,3) como testigo de que el gesto corrió.
    fixture.componentRef.setInput('sesiones', [MAT_PINADA, enSlot(LCL_SIN_PIN, 2, 3)]);
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;
    // Precondición: en reposo, cero slots marcados.
    expect(raiz.querySelectorAll('td.ocupado').length).toBe(0);

    debugDe(fixture, 'Mat').triggerEventHandler('cdkDragStarted', {});
    await fixture.whenStable();

    // El origen de Mat, donde está SOLA, no se tiñe: la marca excluye a la arrastrada.
    expect(tdDe(fixture, DIA, TRAMO).classList).not.toContain('ocupado');
    // Pero el otro slot, con LCL, SÍ: prueba de que el arrastre tomó efecto (no un no-op).
    expect(tdDe(fixture, 2, 3).classList).toContain('ocupado');
  });

  it('(T3) un slot con desdoble marca su <td> UNA vez, no una por entrada', async () => {
    // El desdoble (una instancia, dos entradas) queda SOLO en (DIA, TRAMO); LCL vive en
    // otro slot (2,3) para arrastrarlo desde allí. Así el slot examinado contiene una
    // única InstanciaCelda —el desdoble—, que es lo que este test afirma medir.
    fixture.componentRef.setInput('sesiones', [...DESDOBLE, enSlot(LCL_SIN_PIN, 2, 3)]);
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;
    const td = tdDe(fixture, DIA, TRAMO);
    expect(td.classList).not.toContain('ocupado');

    debugDe(fixture, 'LCL').triggerEventHandler('cdkDragStarted', {});
    await fixture.whenStable();

    // Arrastrando LCL (en 2,3, su origen, excluido), el slot del desdoble queda ocupado:
    // UNA marca (una clase en el <td>) pese a sus dos entradas. El aserto va sobre el <td>,
    // no sobre conteo de entradas; el .toBe(1) cuadra porque (2,3) es origen y no se marca.
    expect(td.classList).toContain('ocupado');
    expect(raiz.querySelectorAll('td.ocupado').length).toBe(1);
  });

  it('(T4) cdkDragEnded limpia la marca: ningún <td> queda .ocupado', async () => {
    // beforeEach monta SESIONES (Mat y LCL en (DIA, TRAMO)).
    const raiz = fixture.nativeElement as HTMLElement;

    debugDe(fixture, 'Mat').triggerEventHandler('cdkDragStarted', {});
    await fixture.whenStable();
    // Precondición: el arrastre dejó algo marcado, para que "cero marcas" no sea un no-op.
    expect(raiz.querySelectorAll('td.ocupado').length).toBeGreaterThan(0);

    debugDe(fixture, 'Mat').triggerEventHandler('cdkDragEnded', {});
    await fixture.whenStable();

    expect(raiz.querySelectorAll('td.ocupado').length).toBe(0);
  });

  /**
   * Modo bloque (D4). El desdoble es UNA instancia con DOS plazas, que es
   * exactamente el umbral: `2 simultáneas` distingue este render de una
   * implementación que contara `entradas.length - 1` (diría `1`) o que fijara el
   * rótulo a mano.
   */
  it('(20) instancia de varias plazas: rótulo común, banda reservada y una fila por plaza', async () => {
    fixture.componentRef.setInput('sesiones', DESDOBLE);
    await fixture.whenStable();

    const inst = instanciaDe(fixture, 'Mat');
    expect(inst.classList).toContain('bloque');

    const rotulo = inst.querySelector('.rotulo');
    expect(rotulo?.textContent?.trim()).toBe('2 simultáneas');
    // El código completo no cabe en la celda y vive en el title, no en el texto.
    expect(rotulo?.getAttribute('title')).toBe('Mat-1ºA');

    const entradas = inst.querySelectorAll('.entrada');
    expect(entradas.length).toBe(2);
    expect(entradas[0].classList).toContain('entrada--fila');
    expect(entradas[1].classList).toContain('entrada--fila');
  });

  /**
   * Contrapunto del (20) en el MISMO fixture del beforeEach: dos instancias de
   * una plaza compartiendo slot. Sin este caso, "es bloque" podría ser cierto
   * siempre y el (20) seguiría verde.
   */
  it('(21) instancia de una plaza: ni bloque ni rótulo, y su entrada no es fila', () => {
    for (const asignatura of ['Mat', 'LCL']) {
      const inst = instanciaDe(fixture, asignatura);
      expect(inst.classList).not.toContain('bloque');
      expect(inst.querySelector('.rotulo')).toBeNull();
      expect(inst.querySelector('.entrada')!.classList).not.toContain('entrada--fila');
    }
  });

  /**
   * La marca de grupos con grupo implícito (D6). Tres grupos y no dos: `+2` sólo
   * cuadra contando los OTROS, así que una implementación que pintara
   * `grupos.length` diría `+3` y caería aquí. La segunda mitad —LCL, cuyo único
   * grupo ES el de la vista— es la que mide la condensación: sin ella, "pintar
   * siempre la marca" pasaría el test.
   */
  it('(22) con grupo actual: la marca cuenta los OTROS grupos y el detalle va en el title', async () => {
    fixture.componentRef.setInput('sesiones', [conGrupos(MAT_PINADA, ['1ºA', '1ºB', '1ºC']), LCL_SIN_PIN]);
    fixture.componentRef.setInput('grupoActual', '1ºA');
    await fixture.whenStable();

    const marca = instanciaDe(fixture, 'Mat').querySelector('.grupos');
    expect(marca?.textContent?.trim()).toBe('+2');
    expect(marca?.getAttribute('title')).toBe('1ºA, 1ºB, 1ºC');

    // El grupo que ya estás mirando no se repite: sin otros grupos, no hay marca.
    expect(instanciaDe(fixture, 'LCL').querySelector('.grupos')).toBeNull();
  });

  /**
   * Vistas de profesor y de aula: no hay grupo implícito, así que la lista NO se
   * condensa. `grupoActual` se queda en su defecto (null) a propósito: es el
   * estado en que la rejilla se monta si nadie le dice de qué grupo habla, y
   * condensar ahí borraría el único sitio donde el grupo aparece.
   */
  it('(23) sin grupo actual: se pinta la lista de grupos, no la marca', async () => {
    fixture.componentRef.setInput('sesiones', [conGrupos(MAT_PINADA, ['1ºA', '1ºB']), LCL_SIN_PIN]);
    await fixture.whenStable();

    expect(instanciaDe(fixture, 'Mat').querySelector('.grupos')?.textContent?.trim()).toBe('1ºA, 1ºB');
    expect(instanciaDe(fixture, 'LCL').querySelector('.grupos')?.textContent?.trim()).toBe('1ºA');
  });

  /**
   * D7 · la fila de recreo va DONDE dice el input, no en un sitio fijo. Se afirma
   * la posición contando filas: la de recreo es la cuarta del tbody cuando va tras
   * el tramo 3, y eso distingue "se pintó" de "se pintó en su sitio".
   */
  it('(24) con recreoTras, se pinta UNA fila de recreo tras ese tramo, sin hora', async () => {
    fixture.componentRef.setInput('recreoTras', 3);
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;
    const recreos = raiz.querySelectorAll('tr.recreo');
    expect(recreos.length).toBe(1);

    const filas = Array.from(raiz.querySelectorAll('tbody tr'));
    expect(filas.indexOf(recreos[0] as HTMLElement)).toBe(3);

    const celda = recreos[0].querySelector('td')!;
    expect(celda.textContent?.trim()).toBe('Recreo');
    // D8: el hueco de la hora está declarado y no se rellena.
    expect(celda.textContent).not.toMatch(/\d/);
    expect(celda.getAttribute('colspan')).toBe('5');
  });

  it('(25) sin recreoTras no se pinta ninguna fila de recreo', () => {
    // El defecto del input: sin jornada cargada la rejilla no inventa un recreo.
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('tr.recreo').length).toBe(0);
  });

  /**
   * D11 · el atributo por el que la medición indexa cada instancia. Vive en el
   * DOM porque `medirOcultas` recorre elementos, no el modelo: sin él, el mapa de
   * ocultas no puede casarse con la instancia que lo pintó.
   *
   * <p>Se afirman los DOS valores literales, como en (6): el índice 2 de `Mat` es
   * lo único que distingue una implementación que fijara `|1` a mano, y el `|1` de
   * `LCL` es lo único que distingue una que fijara `|2`.
   */
  it('(26) cada instancia lleva data-clave con la CLAVE de clavePin', () => {
    expect(instanciaDe(fixture, 'Mat').getAttribute('data-clave')).toBe('Mat-1ºA|2');
    expect(instanciaDe(fixture, 'LCL').getAttribute('data-clave')).toBe('LCL-1ºA|1');
  });

  /**
   * D11 · POR QUÉ el resto de la suite no ve el cableado. Este caso no mata
   * ninguna mutación y no pretende hacerlo: fija en un aserto lo que hasta ahora
   * sólo decía un comentario. En jsdom `getBoundingClientRect` devuelve ceros
   * —medido en el sondeo de S127—, luego el alto de la celda es 0,
   * `ocultasEnCelda` devuelve `null` y no se marca nada. Sin los stubs de (28) y
   * (29) el cableado es INVISIBLE para los tests, y eso es un hecho de la
   * herramienta, no un descuido.
   */
  it('(27) sin stub, en jsdom los rects son ceros y no se marca ninguna instancia', async () => {
    fixture.componentRef.setInput('sesiones', DESDOBLE);
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;
    // La instancia SÍ se pintó: sin esto, "no hay marca" podría ser "no hay nada".
    expect(raiz.querySelectorAll('div.instancia').length).toBe(1);
    expect(raiz.querySelectorAll('.oculta').length).toBe(0);
  });

  /**
   * D11 · la cadena entera, de rectángulo a píxel pintado: el effect mide, el mapa
   * se puebla y la plantilla marca. Es el único caso que recorre todo el camino.
   *
   * <p>El `+2` NO es una elección: con el hueco de S126 (109,57 px) y el escalón de
   * 21,7, la cuarta plaza se ve al 94 % y la regla de la mitad NO la cuenta;
   * la quinta y la sexta quedan enteras fuera. Cambiar el umbral a «cualquier
   * parte oculta cuenta» daría +3 y este aserto caería, que es justo su trabajo.
   *
   * <p>El `title` lista LAS SEIS plazas, no sólo las ocultas: mismo criterio que
   * D6 con los grupos —la marca condensa, el title conserva—.
   */
  it('(28) con rectángulos stubeados, la instancia de seis plazas marca +2', async () => {
    instalarStubDeRects();
    fixture.componentRef.setInput('sesiones', BLOQUE_6);
    await fixture.whenStable();

    const marca = instanciaDe(fixture, 'Mat').querySelector('.oculta');
    expect(marca).not.toBeNull();
    expect(marca!.textContent?.trim()).toBe('+2');

    const title = marca!.getAttribute('title') ?? '';
    for (const asignatura of ASIGS_BLOQUE) {
      expect(title).toContain(asignatura);
    }
  });

  /**
   * D11 · la marca vive DENTRO de la banda del rótulo, que `.instancia.bloque` ya
   * reserva y es `absolute`. De ahí sale la garantía de que no realimenta la
   * medición: si colgara de la instancia ocuparía alto, cambiaría los rectángulos
   * y la siguiente pasada mediría otra cosa.
   *
   * <p>El segundo aserto protege a D4: el `title` del rótulo es el ÚNICO sitio
   * donde se lee el código completo de la actividad, y la marca trae el suyo
   * propio en vez de robárselo.
   */
  it('(29) la marca está dentro del rótulo, y el rótulo conserva su propio title', async () => {
    instalarStubDeRects();
    fixture.componentRef.setInput('sesiones', BLOQUE_6);
    await fixture.whenStable();

    const rotulo = instanciaDe(fixture, 'Mat').querySelector('.rotulo');
    expect(rotulo).not.toBeNull();
    expect(rotulo!.querySelector('.oculta')).not.toBeNull();
    expect(rotulo!.getAttribute('title')).toBe('Bloque-1ºA');
  });

  /**
   * D-desbordamiento-sin-etiqueta (S129) · las dos marcas de la rejilla dejan de
   * ser mudas para un lector de pantalla. Las dos llevaban `title` y nada más, a
   * quince líneas de la insignia que S128 dotó del par completo: de la de
   * ocultas se oía «más 2» sin decir de qué, y de la de grupos la marca
   * condensada sin los grupos que condensa.
   *
   * <p>Se asevera la IGUALDAD con el `title` y no un texto literal, porque la
   * decisión escrita es que el `aria-label` REPITA la cadena original sin
   * mejorarla: un aserto con el texto a mano permitiría que las dos cadenas
   * divergieran sin que nadie se enterase, que es la mitad que importa.
   *
   * <p>La comprobación de no-nulo va ANTES y no es defensiva: sin ella, borrar
   * los DOS atributos deja `null === null` y el caso pasa en verde, que es el
   * caso que este test existe para cazar.
   *
   * <p>Las dos marcas van en UN caso y no en dos: es el mismo defecto con el
   * mismo arreglo, y un caso gemelo no discriminaría nada. El precio, anotado
   * por si algún día falla: su nombre no dirá cuál de las dos se rompió.
   */
  it('(30) las marcas de ocultas y de grupos llevan aria-label igual a su title', async () => {
    instalarStubDeRects();
    fixture.componentRef.setInput('sesiones', BLOQUE_6);
    await fixture.whenStable();

    const instancia = instanciaDe(fixture, 'Mat');

    const ocultas = instancia.querySelector('.oculta');
    expect(ocultas).not.toBeNull();
    expect(ocultas!.getAttribute('aria-label')).not.toBeNull();
    expect(ocultas!.getAttribute('aria-label')).toBe(ocultas!.getAttribute('title'));

    const grupos = instancia.querySelector('.grupos');
    expect(grupos).not.toBeNull();
    expect(grupos!.getAttribute('aria-label')).not.toBeNull();
    expect(grupos!.getAttribute('aria-label')).toBe(grupos!.getAttribute('title'));
  });
});
