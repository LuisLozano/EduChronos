import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { Dialog } from '@angular/cdk/dialog';
import { NivelLista } from './nivel-lista';

/**
 * Congela el comportamiento de la lista, molde `SubgrupoLista` (S108). Focos: (4)(5)
 * la traducción del 409 con y sin `message`; (1), que además de medir que el `@for`
 * itera congela que el cliente PINTA EN EL ORDEN RECIBIDO (D-1); y (6)(7), el ciclo
 * del diálogo. Secuencia propia desde (1).
 *
 * <p>Los datos discriminan: el fixture usa un orden pedagógico que CONTRADICE al
 * alfabético ('1BACH' < '1ESO'), así que un sort por `codigo` colado en el cliente
 * invertiría las filas del (1).
 *
 * <p>La precedencia interna de `mensaje()` se cubre AQUÍ, en el (4): hay una función
 * `mensaje()` en la lista y otra en el formulario, copiadas a propósito y NO
 * compartidas (extraerlas tocaría horario-view, H1 cerrado). Son funciones distintas,
 * así que el caso del formulario no cubre a esta. Medido por mutación en S111.
 *
 * <p>NO se cubren dos comportamientos, por equivalencia y no por olvido (S111): el
 * `=== true` estricto de `abrirForm` —`NivelForm` cierra con `true` o sin argumento,
 * nunca con `false`, así que un `if (guardado)` laxo se comporta igual— y el
 * `track nivel.id` —las filas no tienen estado en el DOM y la lista se reemplaza
 * entera en cada carga, así que `$index` no produce síntoma observable—.
 */
describe('NivelLista', () => {
  let fixture: ComponentFixture<NivelLista>;
  let http: HttpTestingController;
  let dialog: { open: ReturnType<typeof vi.fn> };

  interface Interna {
    nuevo: () => void;
    editar: (n: unknown) => void;
    borrar: (n: unknown) => void;
  }

  const FILAS = [
    { id: 3, codigo: '1ESO', orden: 1 },
    { id: 1, codigo: '1BACH', orden: 2 },
  ];

  beforeEach(() => {
    dialog = { open: vi.fn() };
    TestBed.configureTestingModule({
      imports: [NivelLista],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Dialog, useValue: dialog },
      ],
    });
    fixture = TestBed.createComponent(NivelLista);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function flushLista(filas: unknown[] = []): void {
    fixture.detectChanges(); // dispara ngOnInit → cargar()
    http.expectOne('/api/niveles').flush(filas);
  }

  /** Hace que el próximo diálogo abierto cierre con el valor dado. */
  function dialogoDevuelve(valor: boolean | undefined): void {
    dialog.open.mockReturnValue({
      closed: { subscribe: (fn: (v: boolean | undefined) => void) => fn(valor) },
    });
  }

  /** Dispara el borrado de una fila con el diálogo de confirmación aceptado. */
  function borrar(nivel: unknown): void {
    dialogoDevuelve(true);
    (fixture.componentInstance as unknown as Interna).borrar(nivel);
  }

  it('(1) carga la lista en init y la pinta en el orden que llega, no en el alfabético', async () => {
    flushLista(FILAS);
    await fixture.whenStable();
    const filas = fixture.nativeElement.querySelectorAll('tbody tr');
    // DOS filas, no una: con una sola, un `@for` roto que pintara solo el primer
    // elemento quedaría verde.
    expect(filas.length).toBe(2);
    // Discriminante de D-1: '1BACH' < '1ESO' alfabéticamente.
    expect(filas[0].textContent).toContain('1ESO');
    expect(filas[1].textContent).toContain('1BACH');
  });

  it('(2) lista vacía muestra la invitación a crear el primero', async () => {
    flushLista([]);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.niveles__vacio')).toBeTruthy();
  });

  it('(3) error de carga cae al degradado con status', async () => {
    fixture.detectChanges();
    http.expectOne('/api/niveles').flush('', { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.niveles__error').textContent;
    expect(err).toContain('No se pudo cargar');
    expect(err).toContain('500');
  });

  it('(4) al borrar, un 409 CON message muestra el texto RICO del backend, y message gana a error', async () => {
    flushLista(FILAS);
    await fixture.whenStable();
    borrar(FILAS[0]);

    // El cuerpo lleva AMBAS claves, como un error real de Spring: así el caso mide
    // también la precedencia de `mensaje()` (message antes que error), que de otro
    // modo quedaría sin guardián en este fichero.
    http.expectOne('/api/niveles/3').flush(
      { message: 'No se puede borrar: referenciada por 4 grupo(s)', error: 'Conflict' },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.niveles__error').textContent;
    expect(err).toContain('referenciada por 4 grupo(s)');
    expect(err).not.toContain('Conflict');
    // discriminante: NO cae al degradado, que es lo único que lleva el status.
    expect(err).not.toContain('(409)');
  });

  it('(5) al borrar, un 409 SIN message cae al degradado que dice qué pasó', async () => {
    flushLista(FILAS);
    await fixture.whenStable();
    borrar(FILAS[0]);

    http.expectOne('/api/niveles/3').flush({}, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.niveles__error').textContent;
    expect(err).toContain('No se pudo borrar el nivel 1ESO (409)');
  });

  it('(6) un guardado en el diálogo recarga la lista; un cierre sin guardar no', async () => {
    flushLista([]);
    await fixture.whenStable();
    const inst = fixture.componentInstance as unknown as Interna;

    dialogoDevuelve(true);
    inst.nuevo();
    http.expectOne('/api/niveles').flush([{ id: 3, codigo: '1ESO', orden: 1 }]); // recargó
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('1ESO');

    // backdrop/Escape emiten undefined: no debe salir un segundo GET. El http.verify()
    // del afterEach lo pondría rojo si saliera.
    dialogoDevuelve(undefined);
    inst.nuevo();
  });

  it('(7) editar pasa el nivel al diálogo; nuevo pasa null', async () => {
    flushLista([FILAS[1]]);
    await fixture.whenStable();
    const inst = fixture.componentInstance as unknown as Interna;
    dialogoDevuelve(undefined); // se cancela: aquí solo interesa CON QUÉ se abrió

    inst.editar(FILAS[1]);
    expect(dialog.open.mock.calls.at(-1)?.[1]).toEqual({ data: FILAS[1] });

    inst.nuevo();
    expect(dialog.open.mock.calls.at(-1)?.[1]).toEqual({ data: null });
  });
});
