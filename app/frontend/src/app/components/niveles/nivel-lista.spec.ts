import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { Dialog } from '@angular/cdk/dialog';
import { NivelLista } from './nivel-lista';

/**
 * Congela el comportamiento de la lista, molde `AsignaturaLista`. Focos: (4)(5) la
 * traducción del 409 con y sin `message`; y (1), que además de medir que el `@for`
 * itera, congela que el cliente PINTA EN EL ORDEN RECIBIDO (D-1) usando un fixture
 * cuyo orden pedagógico contradice al alfabético. Secuencia propia desde (1).
 */
describe('NivelLista', () => {
  let fixture: ComponentFixture<NivelLista>;
  let http: HttpTestingController;
  let dialog: { open: ReturnType<typeof vi.fn> };

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

  it('(1) carga la lista en init y la pinta en el orden que llega, no en el alfabético', async () => {
    flushLista([
      { id: 3, codigo: '1ESO', orden: 1 },
      { id: 1, codigo: '1BACH', orden: 2 },
    ]);
    await fixture.whenStable();
    const filas = fixture.nativeElement.querySelectorAll('tbody tr');
    // DOS filas, no una: con una sola, un `@for` roto que pintara solo el primer
    // elemento quedaría verde.
    expect(filas.length).toBe(2);
    // Discriminante de D-1: '1BACH' < '1ESO' alfabéticamente, así que un sort por
    // `codigo` colado en el cliente invertiría estas dos filas.
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

  it('(4) al borrar, un 409 CON message muestra el texto RICO del backend, no el degradado', async () => {
    flushLista([{ id: 7, codigo: '1ESO', orden: 1 }]);
    await fixture.whenStable();
    // el diálogo de confirmación devuelve true
    dialog.open.mockReturnValue({ closed: { subscribe: (fn: (v: boolean) => void) => fn(true) } });

    (fixture.componentInstance as unknown as { borrar: (n: unknown) => void }).borrar({
      id: 7, codigo: '1ESO', orden: 1,
    });

    http.expectOne('/api/niveles/7').flush(
      { message: 'No se puede borrar: referenciada por 4 grupo(s)' },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.niveles__error').textContent;
    expect(err).toContain('referenciada por 4 grupo(s)');
    // discriminante: NO cae al degradado, que es lo único que lleva el status.
    expect(err).not.toContain('(409)');
  });

  it('(5) al borrar, un 409 SIN message cae al degradado que dice qué pasó', async () => {
    flushLista([{ id: 7, codigo: '1ESO', orden: 1 }]);
    await fixture.whenStable();
    dialog.open.mockReturnValue({ closed: { subscribe: (fn: (v: boolean) => void) => fn(true) } });

    (fixture.componentInstance as unknown as { borrar: (n: unknown) => void }).borrar({
      id: 7, codigo: '1ESO', orden: 1,
    });

    http.expectOne('/api/niveles/7').flush({}, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.niveles__error').textContent;
    expect(err).toContain('No se pudo borrar el nivel 1ESO (409)');
  });
});
