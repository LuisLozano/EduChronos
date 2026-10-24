import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { Dialog } from '@angular/cdk/dialog';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { ActividadLista } from './actividad-lista';

/**
 * Congela el comportamiento de la lista de actividades. Hereda del molde de
 * `subgrupo-lista` el render, el vacío, el error de carga, las dos caras del 409 y el
 * con-qué-se-abre el diálogo; añade lo propio de esta lista: las SEIS columnas con
 * «varias» para la asignatura nula (6). La GUARDA DE MULTIPLAZA se retiró en el trozo B
 * —el formulario ya edita N plazas—, así que (12) congela lo contrario: toda actividad es
 * editable. (9) sigue cubriendo que el borrado avisa de cuántas plazas caen.
 * Secuencia propia desde (1).
 *
 * <p>Los datos discriminan: la primera fila es de UNA plaza y con asignatura propia, la
 * segunda de DOS y con asignatura `null` («varias»). Con una sola fila no se distinguiría
 * un botón habilitado por la retirada de la guarda de uno habilitado siempre.
 *
 * <p><b>Búsqueda por pertenencia (S225).</b> El init pide CINCO cosas: las actividades y las
 * cuatro listas de catálogo. {@link flushLista} las atiende todas por URL, con el catálogo vacío
 * salvo que el caso pase uno; así los casos de antes no ven ninguna coincidencia exacta y siguen
 * midiendo la búsqueda por texto. De (15) a (18), lo nuevo: la línea, la búsqueda por texto con
 * el nombre de la asignatura, `?buscar=` y el aviso si falla un GET de catálogo. La lógica de la
 * pertenencia la mide su propio spec (`catalogo/pertenencia.spec.ts`).
 */
describe('ActividadLista', () => {
  let fixture: ComponentFixture<ActividadLista>;
  let http: HttpTestingController;
  let dialog: { open: ReturnType<typeof vi.fn> };
  /** Parámetros de consulta de la ruta; los lee el init a través del doble de `ActivatedRoute`. */
  let consulta: Record<string, string>;

  /** Las cuatro listas que el init pide además de las actividades; vacías salvo que el caso diga. */
  interface CatalogoFixture {
    profesores?: unknown[];
    asignaturas?: unknown[];
    grupos?: unknown[];
    subgrupos?: unknown[];
  }
  const URL_CATALOGO = {
    profesores: '/api/profesores',
    asignaturas: '/api/asignaturas',
    grupos: '/api/grupos',
    subgrupos: '/api/subgrupos',
  } as const;

  interface Interna {
    nuevo: () => void;
    editar: (a: unknown) => void;
    borrar: (a: unknown) => void;
  }

  const PLAZA = {
    id: 11,
    codigo: 'Mat-1ºA-P1',
    asignatura: 'Mat',
    aulaFija: 'A1',
    aulasCandidatas: [] as string[],
    profesores: ['MATA'],
    subgrupos: ['1ºA-Completo'],
  };

  const UNA_PLAZA = {
    id: 5,
    codigo: 'Mat-1ºA',
    tipo: 'CLASE',
    asignatura: 'Mat',
    duracionTramos: 1,
    repeticionesPorSemana: 4,
    patronTemporal: 'DISTRIBUIDA',
    requiereTutor: false,
    plazas: [PLAZA],
  };

  const DOS_PLAZAS = {
    id: 6,
    codigo: 'Bloque',
    tipo: 'CLASE',
    asignatura: null,
    duracionTramos: 2,
    repeticionesPorSemana: 1,
    patronTemporal: 'NEUTRA',
    requiereTutor: true,
    plazas: [PLAZA, { ...PLAZA, id: 12, codigo: 'Bloque-P2' }],
  };

  const FILAS = [UNA_PLAZA, DOS_PLAZAS];

  /** Una tercera fila de OTRO profesor, subgrupo y asignatura: la que la pertenencia separa. */
  const DE_LENGUA = {
    ...UNA_PLAZA,
    id: 7,
    codigo: 'Len-1ºB',
    asignatura: 'Len',
    plazas: [{ ...PLAZA, id: 13, codigo: 'Len-1ºB-P1', asignatura: 'Len', profesores: ['LENB'], subgrupos: ['1ºB-Completo'] }],
  };
  const TRES_FILAS = [UNA_PLAZA, DOS_PLAZAS, DE_LENGUA];
  /** Una REUNIÓN (S251) que también casa con «mat» por su código y su asignatura. */
  const REUNION = {
    ...UNA_PLAZA,
    id: 8,
    codigo: 'REU-Mat',
    tipo: 'REUNION',
    plazas: [{ ...PLAZA, id: 14, codigo: 'REU-Mat-P1', aulaFija: null, subgrupos: [] }],
  };
  const CATALOGO: CatalogoFixture = {
    profesores: [{ id: 1, codigo: 'MATA' }, { id: 2, codigo: 'LENB' }],
    asignaturas: [{ id: 1, codigo: 'Mat', nombreCompleto: 'Matemáticas' }, { id: 2, codigo: 'Len', nombreCompleto: 'Lengua' }],
    grupos: [{ id: 1, codigo: '1ºA' }, { id: 2, codigo: '1ºB' }],
    subgrupos: [
      { id: 1, codigo: '1ºA-Completo', grupos: ['1ºA'] },
      { id: 2, codigo: '1ºB-Completo', grupos: ['1ºB'] },
    ],
  };

  beforeEach(() => {
    dialog = { open: vi.fn() };
    consulta = {};
    TestBed.configureTestingModule({
      imports: [ActividadLista],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Dialog, useValue: dialog },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { get queryParamMap() { return convertToParamMap(consulta); } } },
        },
      ],
    });
    fixture = TestBed.createComponent(ActividadLista);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function flushLista(filas: unknown[] = [], catalogo: CatalogoFixture = {}): void {
    fixture.detectChanges(); // dispara ngOnInit → cargar() y cargarCatalogo()
    http.expectOne('/api/actividades').flush(filas);
    flushCatalogo(catalogo);
  }

  function flushCatalogo(catalogo: CatalogoFixture = {}): void {
    for (const clave of Object.keys(URL_CATALOGO) as (keyof CatalogoFixture)[]) {
      http.expectOne(URL_CATALOGO[clave]).flush(catalogo[clave] ?? []);
    }
  }

  function linea(): string | null {
    return fixture.nativeElement.querySelector('.actividades__pertenencia')?.textContent.trim() ?? null;
  }

  function codigosVisibles(): string[] {
    return Array.from(fixture.nativeElement.querySelectorAll('tbody tr td:first-child') as NodeListOf<HTMLElement>)
      .map((td) => td.textContent!.trim());
  }

  /** Hace que el próximo diálogo abierto cierre con el valor dado. */
  /** Cierre simulado del diálogo: `true` (confirmar borrado), `{ id }` (editor que guardó,
   *  S227) o `undefined` (backdrop, Escape, Cancelar). */
  function dialogoDevuelve(valor: boolean | { id: number } | undefined): void {
    dialog.open.mockReturnValue({
      closed: { subscribe: (fn: (v: boolean | { id: number } | undefined) => void) => fn(valor) },
    });
  }

  function borrar(actividad: unknown): void {
    dialogoDevuelve(true);
    (fixture.componentInstance as unknown as Interna).borrar(actividad);
  }

  it('(1) carga la lista en init y la pinta', async () => {
    flushLista(FILAS);
    await fixture.whenStable();
    const filas = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(filas.length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('Mat-1ºA');
    expect(fixture.nativeElement.textContent).toContain('Bloque');
  });

  it('(2) lista vacía muestra la invitación a crear la primera', async () => {
    flushLista([]);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.estado-lista__vacio')).toBeTruthy();
  });

  it('(3) error de carga cae al degradado con status', async () => {
    fixture.detectChanges();
    http.expectOne('/api/actividades').flush('', { status: 500, statusText: 'Server Error' });
    flushCatalogo();
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.estado-lista__error').textContent;
    expect(err).toContain('No se pudo cargar');
    expect(err).toContain('500');
  });

  it('(4) al borrar, un 409 CON message muestra el desglose del backend, no el degradado', async () => {
    flushLista(FILAS);
    await fixture.whenStable();
    borrar(FILAS[0]);

    http.expectOne('/api/actividades/5').flush(
      { message: 'No se puede borrar: referenciada por 3 sesion(es)' },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.estado-lista__error').textContent;
    expect(err).toContain('referenciada por 3 sesion(es)');
    expect(err).not.toContain('No se pudo borrar la actividad Mat-1ºA (409)');
  });

  it('(5) al borrar, un 409 SIN message cae al degradado que dice qué pasó', async () => {
    flushLista(FILAS);
    await fixture.whenStable();
    borrar(FILAS[0]);

    http.expectOne('/api/actividades/5').flush({}, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.estado-lista__error').textContent;
    expect(err).toContain('No se pudo borrar la actividad Mat-1ºA (409)');
  });

  it('(6) la tabla pinta las siete columnas y «varias» cuando la asignatura es null', async () => {
    flushLista(FILAS);
    await fixture.whenStable();
    const raiz = fixture.nativeElement as HTMLElement;

    // «Tipo» va justo después de «Código» desde S201: desplaza una celda todo lo demás.
    const cabeceras = [...raiz.querySelectorAll('thead th')].map((th) => th.textContent!.trim());
    expect(cabeceras).toEqual([
      'Código',
      'Tipo',
      'Asignatura',
      'Patrón',
      'Duración',
      'Repeticiones',
      'N.º de plazas',
      '',
    ]);

    // Fila 1: asignatura propia, se pinta tal cual; y su recuento de plazas es 1.
    const primera = raiz.querySelectorAll('tbody tr:nth-child(1) td');
    expect(primera[2].textContent!.trim()).toBe('Mat');
    expect(primera[3].textContent!.trim()).toBe('DISTRIBUIDA');
    expect(primera[4].textContent!.trim()).toBe('1');
    expect(primera[5].textContent!.trim()).toBe('4');
    expect(primera[6].textContent!.trim()).toBe('1');

    // Fila 2: asignatura null → «varias» (no vacío, no 'null'), y 2 plazas.
    const segunda = raiz.querySelectorAll('tbody tr:nth-child(2) td');
    expect(segunda[2].textContent!.trim()).toBe('varias');
    expect(segunda[6].textContent).toContain('2');
  });

  it('(l1) la columna Tipo muestra la etiqueta, no el código: «Reunión» y «Clase»', async () => {
    const reunion = { ...UNA_PLAZA, id: 9, codigo: 'REU-Dpto', tipo: 'REUNION' };
    flushLista([UNA_PLAZA, reunion]);
    await fixture.whenStable();
    const raiz = fixture.nativeElement as HTMLElement;

    const tipoDe = (codigo: string): string => {
      const fila = [...raiz.querySelectorAll('tbody tr')].find(
        (tr) => tr.querySelector('td')!.textContent!.trim() === codigo,
      );
      return fila!.querySelectorAll('td')[1].textContent!.trim();
    };
    expect(tipoDe('REU-Dpto')).toBe('Reunión');
    expect(tipoDe('Mat-1ºA')).toBe('Clase');
  });

  it('(12) toda actividad es editable: el botón no se deshabilita con varias plazas y editar() abre el diálogo', async () => {
    flushLista(FILAS);
    await fixture.whenStable();
    const raiz = fixture.nativeElement as HTMLElement;
    const inst = fixture.componentInstance as unknown as Interna;
    dialogoDevuelve(undefined);

    // La fila de DOS plazas es la que importa; la de una es el contraste.
    const botones = raiz.querySelectorAll<HTMLButtonElement>('.actividades__editar');
    expect(botones.length).toBe(2);
    expect([...botones].some((b) => b.disabled === true)).toBe(false);

    // El aviso de la guarda retirada no puede sobrevivir en ninguna fila.
    expect(raiz.querySelector('.actividades__aviso-multiplaza')).toBeNull();

    // Y la puerta real al PUT ya no filtra: abre con la actividad que se le pasa.
    inst.editar(DOS_PLAZAS);
    expect(dialog.open.mock.calls.at(-1)?.[1]).toEqual({ data: DOS_PLAZAS });

    inst.editar(UNA_PLAZA);
    expect(dialog.open.mock.calls.at(-1)?.[1]).toEqual({ data: UNA_PLAZA });
  });

  it('(9) el borrado SÍ se permite en multiplaza, y la confirmación avisa de cuántas caen', async () => {
    flushLista(FILAS);
    await fixture.whenStable();
    borrar(DOS_PLAZAS);

    // Borrar es íntegro, no parcial: la guarda de edición no lo alcanza.
    const req = http.expectOne('/api/actividades/6');
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
    // recarga tras el borrado
    http.expectOne('/api/actividades').flush([UNA_PLAZA]);
    await fixture.whenStable();

    const lineas = dialog.open.mock.calls.at(-1)?.[1] as { data: string[] };
    expect(lineas.data[0]).toContain('Bloque');
    expect(lineas.data[1]).toContain('2 plazas');
  });

  it('(10) un guardado en el diálogo recarga la lista; un cierre sin guardar no', async () => {
    flushLista([]);
    await fixture.whenStable();
    const inst = fixture.componentInstance as unknown as Interna;

    dialogoDevuelve({ id: 5 });
    inst.nuevo();
    http.expectOne('/api/actividades').flush([UNA_PLAZA]); // recargó
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Mat-1ºA');

    // backdrop/Escape emiten undefined: no debe salir un segundo GET. El http.verify()
    // del afterEach lo pondría rojo si saliera.
    dialogoDevuelve(undefined);
    inst.nuevo();
  });

  it('(11) nuevo() abre el diálogo con null', async () => {
    flushLista([]);
    await fixture.whenStable();
    const inst = fixture.componentInstance as unknown as Interna;
    dialogoDevuelve(undefined);

    inst.nuevo();
    expect(dialog.open.mock.calls.at(-1)?.[1]).toEqual({ data: null });
  });

  /**
   * La cabecera compartida está MONTADA y con SUS rótulos (S124). Los dos asertos
   * son distintos a propósito: el del `<h2>` caza un `titulo` mal cableado, y el
   * del botón caza el género —«Nueva actividad» no se deriva de «Actividades»— que es lo
   * que el e2e localiza por texto exacto. Un `<app-cabecera-lista>` presente pero
   * sin inputs pasaría un `querySelector` a secas y falla estos dos.
   */
  it('(12) monta la cabecera compartida con su rótulo y su texto de alta', async () => {
    flushLista([]);
    await fixture.whenStable();
    const cabecera = fixture.nativeElement.querySelector('app-cabecera-lista');
    expect(cabecera).toBeTruthy();
    expect(cabecera.querySelector('.cabecera-lista__titulo').textContent).toContain('Actividades');
    expect(cabecera.querySelector('.cabecera-lista__nuevo').textContent.trim()).toBe('Nueva actividad');
  });

  /** Teclea en la caja de la cabecera como una persona, y deja repintar. */
  async function buscar(texto: string): Promise<void> {
    const caja = fixture.nativeElement.querySelector('.cabecera-lista__busqueda');
    caja.value = texto;
    caja.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  /**
   * La búsqueda recorta la tabla y el contador lo dice (la actividad sin asignatura, que la tabla pinta como «varias»).
   * Los tres asertos van juntos a propósito: el conteo de filas mide el `@for`
   * sobre `visibles()`, el texto que SOBREVIVE y el que DESAPARECE miden que
   * recorta la correcta —no «una cualquiera»—, y el contador ata el cableado de
   * la cabecera, que recibe `coincidencias` por separado de `total`.
   */
  it('(13) al buscar se ve solo la fila que casa, y el contador dice «1 de 2»', async () => {
    flushLista(FILAS);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(2);

    await buscar('bloque');

    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('Bloque');
    expect(fixture.nativeElement.textContent).not.toContain('Mat-1ºA');
    expect(
      fixture.nativeElement.querySelector('.cabecera-lista__contador').textContent.trim(),
    ).toBe('1 de 2');
  });

  /**
   * El SEGUNDO estado vacío, que no es el de catálogo sin filas. Se asevera que
   * aparece el mensaje nuevo CON el texto tecleado, que NO aparece el de
   * «no hay … todavía» —son dos mensajes distintos y confundirlos es el error
   * fácil— y que la tabla se va entera.
   */
  it('(14) una búsqueda sin resultados da su propio mensaje, no el de catálogo vacío', async () => {
    flushLista(FILAS);
    await fixture.whenStable();

    await buscar('zzz');

    const sinResultados = fixture.nativeElement.querySelector('.estado-lista__sin-resultados');
    expect(sinResultados).toBeTruthy();
    expect(sinResultados.textContent).toContain('zzz');
    expect(fixture.nativeElement.querySelector('.estado-lista__vacio')).toBeNull();
    expect(fixture.nativeElement.querySelector('tbody tr')).toBeNull();
  });

  /**
   * (15) El código EXACTO de un profesor trae sus actividades y la línea lo dice. Por texto,
   * «lenb» no casa con ninguna fila (la tabla no pinta profesores): la fila que queda solo puede
   * venir de la pertenencia.
   */
  it('(15) un código exacto filtra por pertenencia y la línea nombra la entidad', async () => {
    flushLista(TRES_FILAS, CATALOGO);
    await fixture.whenStable();
    expect(linea()).toBeNull();

    await buscar('lenb');

    expect(codigosVisibles()).toEqual(['Len-1ºB']);
    expect(linea()).toBe('Actividades del profesor LENB');
  });

  /**
   * (16) Sin coincidencia exacta no hay línea, y el texto de la fila lleva el NOMBRE de la
   * asignatura: «Matemát» no está en ninguna celda (la columna pinta «Mat» o «varias») y aun
   * así encuentra las dos de Matemáticas, la segunda solo por la asignatura de sus plazas.
   */
  it('(16) la búsqueda por texto no pinta línea y encuentra por el nombre de la asignatura', async () => {
    flushLista(TRES_FILAS, CATALOGO);
    await fixture.whenStable();

    await buscar('Matemát');

    expect(codigosVisibles()).toEqual(['Mat-1ºA', 'Bloque']);
    expect(linea()).toBeNull();
  });

  it('(17) ?buscar= llega a la caja y filtra desde el primer pintado', async () => {
    consulta = { buscar: '1ºB' };
    flushLista(TRES_FILAS, CATALOGO);
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.cabecera-lista__busqueda').value).toBe('1ºB');
    expect(codigosVisibles()).toEqual(['Len-1ºB']);
    expect(linea()).toBe('Actividades del grupo 1ºB');
  });

  /**
   * (18) Si falla un GET de catálogo, el forkJoin cancela los otros tres: se casan con
   * `expectOne` sin atenderlos (no se puede atender una petición cancelada) para que el
   * `verify()` no los cuente. La lista y la búsqueda por texto siguen; la exacta no.
   */
  it('(18) si falla un GET de catálogo se avisa y queda la búsqueda por texto', async () => {
    fixture.detectChanges();
    http.expectOne('/api/actividades').flush(TRES_FILAS);
    http.expectOne(URL_CATALOGO.profesores).flush('', { status: 500, statusText: 'Server Error' });
    for (const url of [URL_CATALOGO.asignaturas, URL_CATALOGO.grupos, URL_CATALOGO.subgrupos]) {
      expect(http.expectOne(url).cancelled).toBe(true);
    }
    await fixture.whenStable();
    expect(linea()).toBe('No se pudo cargar la búsqueda por pertenencia; se busca solo por texto.');

    await buscar('Len-1ºB');

    expect(codigosVisibles()).toEqual(['Len-1ºB']);
    expect(linea()).toBe('No se pudo cargar la búsqueda por pertenencia; se busca solo por texto.');
  });

  /** Elige un tipo en el desplegable bajo la cabecera, como una persona; '' es «Todos los tipos». */
  async function elegirTipo(valor: string): Promise<void> {
    const desplegable = fixture.nativeElement.querySelector('.actividades__filtro-tipo') as HTMLSelectElement;
    desplegable.value = valor;
    desplegable.dispatchEvent(new Event('change'));
    await fixture.whenStable();
  }

  /** (19) S251: el tipo deja solo las filas de ese tipo, y «Todos los tipos» las devuelve. */
  it('(19) el desplegable de tipo filtra la tabla y «Todos los tipos» quita el filtro', async () => {
    flushLista([UNA_PLAZA, DOS_PLAZAS, REUNION]);
    await fixture.whenStable();

    await elegirTipo('REUNION');
    expect(codigosVisibles()).toEqual(['REU-Mat']);

    await elegirTipo('');
    expect(codigosVisibles()).toEqual(['Mat-1ºA', 'Bloque', 'REU-Mat']);
  });

  /**
   * (20) El tipo se SUMA a la búsqueda: «mat» casa con Mat-1ºA y REU-Mat, y con Clase queda solo
   * Mat-1ºA. Bloque es Clase pero no casa con «mat»: una u otra condición la dejaría ver.
   */
  it('(20) el tipo y la búsqueda filtran a la vez', async () => {
    flushLista([UNA_PLAZA, DOS_PLAZAS, REUNION]);
    await fixture.whenStable();

    await buscar('mat');
    expect(codigosVisibles()).toEqual(['Mat-1ºA', 'REU-Mat']);

    await elegirTipo('CLASE');
    expect(codigosVisibles()).toEqual(['Mat-1ºA']);
  });

  /** (21) Con un tipo elegido y la caja vacía, el contador de la cabecera cuenta lo filtrado. */
  it('(21) con un tipo elegido el contador dice «n de N» sin búsqueda', async () => {
    flushLista([UNA_PLAZA, DOS_PLAZAS, REUNION]);
    await fixture.whenStable();
    const contador = (): string =>
      fixture.nativeElement.querySelector('.cabecera-lista__contador').textContent.trim();
    expect(contador()).toBe('3');

    await elegirTipo('REUNION');
    expect(contador()).toBe('1 de 3');
  });

  /**
   * (22) Un tipo que no deja filas dice cuál es, y con búsqueda los dos unidos por « · »: el
   * mensaje no puede quedarse en «Ningún resultado para «»» con la caja vacía.
   */
  it('(22) sin resultados por tipo, el mensaje nombra el tipo y, si la hay, la búsqueda', async () => {
    flushLista([UNA_PLAZA, DOS_PLAZAS]);
    await fixture.whenStable();
    const mensaje = (): string =>
      fixture.nativeElement.querySelector('.estado-lista__sin-resultados').textContent.trim();

    await elegirTipo('FUNCION');
    expect(mensaje()).toBe('Ningún resultado para «Función».');

    await buscar('zzz');
    expect(mensaje()).toBe('Ningún resultado para «zzz · Función».');
  });

  /**
   * Aviso tras guardar con la lista filtrada (S227, condición 10 de O-interfaz). «BYG1» es un
   * profesor del catálogo, así que la búsqueda va por PERTENENCIA: la actividad deja de casar
   * cuando el guardado le quita a BYG1 de la plaza.
   */
  describe('aviso tras guardar con búsqueda (S227)', () => {
    const CATALOGO_BYG: CatalogoFixture = {
      ...CATALOGO,
      profesores: [...(CATALOGO.profesores ?? []), { id: 3, codigo: 'BYG1' }],
    };
    const CON_BYG = { ...UNA_PLAZA, plazas: [{ ...PLAZA, profesores: ['BYG1'] }] };
    const TEXTO_AVISO =
      'Actividad «Mat-1ºA» guardada. No aparece en la lista porque no corresponde a «BYG1».';

    function aviso(): string | null {
      return fixture.nativeElement.querySelector('.actividades__aviso')?.textContent.trim() ?? null;
    }

    /** Lista con CON_BYG y la de Lengua, búsqueda «BYG1» puesta: solo se ve Mat-1ºA. */
    async function filtradaPorByg(): Promise<Interna> {
      flushLista([CON_BYG, DE_LENGUA], CATALOGO_BYG);
      await fixture.whenStable();
      await buscar('BYG1');
      expect(codigosVisibles()).toEqual(['Mat-1ºA']);
      return fixture.componentInstance as unknown as Interna;
    }

    /** Guarda CON_BYG en el editor y responde a la recarga con `recarga`. */
    async function guardarYRecargar(inst: Interna, recarga: unknown[]): Promise<void> {
      dialogoDevuelve({ id: 5 });
      inst.editar(CON_BYG);
      http.expectOne('/api/actividades').flush(recarga);
      await fixture.whenStable();
    }

    it('T3a con búsqueda, la guardada que deja de casar da el aviso con código y búsqueda', async () => {
      const inst = await filtradaPorByg();
      await guardarYRecargar(inst, [UNA_PLAZA, DE_LENGUA]); // UNA_PLAZA: profesor MATA

      expect(codigosVisibles()).toEqual([]);
      expect(aviso()).toBe(TEXTO_AVISO);
    });

    it('T3b con búsqueda, la guardada que sigue casando no da aviso', async () => {
      const inst = await filtradaPorByg();
      await guardarYRecargar(inst, [CON_BYG, DE_LENGUA]);

      expect(codigosVisibles()).toEqual(['Mat-1ºA']);
      expect(aviso()).toBeNull();
    });

    it('T3c sin búsqueda y con todos los tipos, guardar no da aviso', async () => {
      flushLista([CON_BYG, DE_LENGUA], CATALOGO_BYG);
      await fixture.whenStable();
      const inst = fixture.componentInstance as unknown as Interna;
      await guardarYRecargar(inst, [UNA_PLAZA, DE_LENGUA]);

      expect(codigosVisibles()).toEqual(['Mat-1ºA', 'Len-1ºB']);
      expect(aviso()).toBeNull();
    });

    it('T3d con el aviso a la vista, cambiar la búsqueda lo quita', async () => {
      const inst = await filtradaPorByg();
      await guardarYRecargar(inst, [UNA_PLAZA, DE_LENGUA]);
      expect(aviso()).toBe(TEXTO_AVISO);

      await buscar('MATA');

      expect(codigosVisibles()).toEqual(['Mat-1ºA']);
      expect(aviso()).toBeNull();
    });

    it('T3e cancelar el editor no da aviso, y abrirlo otra vez borra el que hubiera', async () => {
      const inst = await filtradaPorByg();
      dialogoDevuelve(undefined);
      inst.editar(CON_BYG); // sin GET: el verify() del afterEach lo pondría rojo
      await fixture.whenStable();
      expect(aviso()).toBeNull();

      await guardarYRecargar(inst, [UNA_PLAZA, DE_LENGUA]);
      expect(aviso()).toBe(TEXTO_AVISO);
      dialogoDevuelve(undefined);
      inst.editar(UNA_PLAZA);
      await fixture.whenStable();
      expect(aviso()).toBeNull();
    });

    /** La misma Mat-1ºA, guardada como reunión: deja de ser de Clase. */
    const COMO_REUNION = { ...UNA_PLAZA, tipo: 'REUNION' };
    const CON_BYG_COMO_REUNION = { ...CON_BYG, tipo: 'REUNION' };

    it('T3f sin búsqueda y con un tipo elegido, la guardada de otro tipo da el aviso del tipo', async () => {
      flushLista([CON_BYG, DE_LENGUA], CATALOGO_BYG);
      await fixture.whenStable();
      await elegirTipo('CLASE');
      const inst = fixture.componentInstance as unknown as Interna;
      await guardarYRecargar(inst, [COMO_REUNION, DE_LENGUA]);

      expect(codigosVisibles()).toEqual(['Len-1ºB']);
      expect(aviso()).toBe('Actividad «Mat-1ºA» guardada. No aparece en la lista porque no es del tipo «Clase».');
    });

    it('T3g la guardada que deja de casar con la búsqueda y con el tipo nombra los dos', async () => {
      const inst = await filtradaPorByg();
      await elegirTipo('CLASE');
      await guardarYRecargar(inst, [COMO_REUNION, DE_LENGUA]); // sin BYG1 y reunión

      expect(aviso()).toBe(
        'Actividad «Mat-1ºA» guardada. No aparece en la lista porque no corresponde a «BYG1» ni es del tipo «Clase».',
      );
    });

    it('T3h con búsqueda y tipo, la que sigue casando con la búsqueda solo nombra el tipo', async () => {
      const inst = await filtradaPorByg();
      await elegirTipo('CLASE');
      await guardarYRecargar(inst, [CON_BYG_COMO_REUNION, DE_LENGUA]); // con BYG1, pero reunión

      expect(aviso()).toBe('Actividad «Mat-1ºA» guardada. No aparece en la lista porque no es del tipo «Clase».');
    });

    it('T3i con el aviso a la vista, cambiar el tipo lo quita', async () => {
      const inst = await filtradaPorByg();
      await guardarYRecargar(inst, [UNA_PLAZA, DE_LENGUA]);
      expect(aviso()).toBe(TEXTO_AVISO);

      await elegirTipo('CLASE');

      expect(aviso()).toBeNull();
    });
  });
});
