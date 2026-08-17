import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { Dialog } from '@angular/cdk/dialog';
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
 */
describe('ActividadLista', () => {
  let fixture: ComponentFixture<ActividadLista>;
  let http: HttpTestingController;
  let dialog: { open: ReturnType<typeof vi.fn> };

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
    asignatura: null,
    duracionTramos: 2,
    repeticionesPorSemana: 1,
    patronTemporal: 'NEUTRA',
    requiereTutor: true,
    plazas: [PLAZA, { ...PLAZA, id: 12, codigo: 'Bloque-P2' }],
  };

  const FILAS = [UNA_PLAZA, DOS_PLAZAS];

  beforeEach(() => {
    dialog = { open: vi.fn() };
    TestBed.configureTestingModule({
      imports: [ActividadLista],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Dialog, useValue: dialog },
      ],
    });
    fixture = TestBed.createComponent(ActividadLista);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function flushLista(filas: unknown[] = []): void {
    fixture.detectChanges(); // dispara ngOnInit → cargar()
    http.expectOne('/api/actividades').flush(filas);
  }

  /** Hace que el próximo diálogo abierto cierre con el valor dado. */
  function dialogoDevuelve(valor: boolean | undefined): void {
    dialog.open.mockReturnValue({
      closed: { subscribe: (fn: (v: boolean | undefined) => void) => fn(valor) },
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
    expect(fixture.nativeElement.querySelector('.actividades__vacio')).toBeTruthy();
  });

  it('(3) error de carga cae al degradado con status', async () => {
    fixture.detectChanges();
    http.expectOne('/api/actividades').flush('', { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.actividades__error').textContent;
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
    const err = fixture.nativeElement.querySelector('.actividades__error').textContent;
    expect(err).toContain('referenciada por 3 sesion(es)');
    expect(err).not.toContain('No se pudo borrar la actividad Mat-1ºA (409)');
  });

  it('(5) al borrar, un 409 SIN message cae al degradado que dice qué pasó', async () => {
    flushLista(FILAS);
    await fixture.whenStable();
    borrar(FILAS[0]);

    http.expectOne('/api/actividades/5').flush({}, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.actividades__error').textContent;
    expect(err).toContain('No se pudo borrar la actividad Mat-1ºA (409)');
  });

  it('(6) la tabla pinta las seis columnas y «varias» cuando la asignatura es null', async () => {
    flushLista(FILAS);
    await fixture.whenStable();
    const raiz = fixture.nativeElement as HTMLElement;

    const cabeceras = [...raiz.querySelectorAll('thead th')].map((th) => th.textContent!.trim());
    expect(cabeceras).toEqual([
      'Código',
      'Asignatura',
      'Patrón',
      'Duración',
      'Repeticiones',
      'N.º de plazas',
      '',
    ]);

    // Fila 1: asignatura propia, se pinta tal cual; y su recuento de plazas es 1.
    const primera = raiz.querySelectorAll('tbody tr:nth-child(1) td');
    expect(primera[1].textContent!.trim()).toBe('Mat');
    expect(primera[2].textContent!.trim()).toBe('DISTRIBUIDA');
    expect(primera[3].textContent!.trim()).toBe('1');
    expect(primera[4].textContent!.trim()).toBe('4');
    expect(primera[5].textContent!.trim()).toBe('1');

    // Fila 2: asignatura null → «varias» (no vacío, no 'null'), y 2 plazas.
    const segunda = raiz.querySelectorAll('tbody tr:nth-child(2) td');
    expect(segunda[1].textContent!.trim()).toBe('varias');
    expect(segunda[5].textContent).toContain('2');
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

    dialogoDevuelve(true);
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
});
