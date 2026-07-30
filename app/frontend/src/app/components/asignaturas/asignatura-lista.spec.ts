import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { Dialog } from '@angular/cdk/dialog';
import { AsignaturaLista } from './asignatura-lista';

/**
 * Congela el comportamiento de la lista. El foco de M3 (traducción de error) son
 * los casos (4)(5): que el 409 muestra el TEXTO DEL BACKEND cuando viaja, y el
 * DEGRADADO cuando no. El borrado se dispara con el diálogo espiado a `true`,
 * sin overlay real. Secuencia propia desde (1). Fuera de alcance por decisión,
 * no por imposibilidad: estilos, y el orden interno de `mensaje()` (cubierto en
 * el form, misma función).
 */
describe('AsignaturaLista', () => {
  let fixture: ComponentFixture<AsignaturaLista>;
  let http: HttpTestingController;
  let dialog: { open: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    dialog = { open: vi.fn() };
    TestBed.configureTestingModule({
      imports: [AsignaturaLista],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Dialog, useValue: dialog },
      ],
    });
    fixture = TestBed.createComponent(AsignaturaLista);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function flushLista(filas: unknown[] = []): void {
    fixture.detectChanges(); // dispara ngOnInit → cargar()
    http.expectOne('/api/asignaturas').flush(filas);
  }

  it('(1) carga la lista en init y la pinta', async () => {
    flushLista([
      { id: 7, codigo: 'Mat', nombreCompleto: 'Matemáticas' },
      { id: 8, codigo: 'LCL', nombreCompleto: 'Lengua Castellana y Literatura' },
    ]);
    await fixture.whenStable();
    const filas = fixture.nativeElement.querySelectorAll('tbody tr');
    // DOS filas, no una: con una sola, un `@for` roto que pintara solo el primer
    // elemento quedaría verde. El conteo y los dos textos miden que el bucle itera.
    expect(filas.length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('Matemáticas');
    expect(fixture.nativeElement.textContent).toContain('Lengua Castellana y Literatura');
  });

  it('(2) lista vacía muestra la invitación a crear la primera', async () => {
    flushLista([]);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.asignaturas__vacio')).toBeTruthy();
  });

  it('(3) error de carga cae al degradado con status', async () => {
    fixture.detectChanges();
    http.expectOne('/api/asignaturas').flush('', { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.asignaturas__error').textContent;
    expect(err).toContain('No se pudo cargar');
    expect(err).toContain('500');
  });

  it('(4) al borrar, un 409 CON message muestra el texto RICO del backend, no el degradado', async () => {
    flushLista([{ id: 7, codigo: 'Mat', nombreCompleto: 'Matemáticas' }]);
    await fixture.whenStable();
    // el diálogo de confirmación devuelve true
    dialog.open.mockReturnValue({ closed: { subscribe: (fn: (v: boolean) => void) => fn(true) } });

    (fixture.componentInstance as unknown as { borrar: (asig: unknown) => void }).borrar({
      id: 7, codigo: 'Mat', nombreCompleto: 'Matemáticas',
    });

    http.expectOne('/api/asignaturas/7').flush(
      { message: 'No se puede borrar: referenciada por 2 actividad(es), 1 plaza(s)' },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.asignaturas__error').textContent;
    expect(err).toContain('referenciada por 2 actividad(es)');
    // discriminante: NO cae al degradado. El `(409)` no es adorno: sin él, 'Mat' es
    // prefijo de 'Matemáticas' y el aserto dejaría de distinguir codigo de nombre.
    expect(err).not.toContain('No se pudo borrar la asignatura Mat (409)');
  });

  it('(5) al borrar, un 409 SIN message cae al degradado que dice qué pasó', async () => {
    flushLista([{ id: 7, codigo: 'Mat', nombreCompleto: 'Matemáticas' }]);
    await fixture.whenStable();
    dialog.open.mockReturnValue({ closed: { subscribe: (fn: (v: boolean) => void) => fn(true) } });

    (fixture.componentInstance as unknown as { borrar: (asig: unknown) => void }).borrar({
      id: 7, codigo: 'Mat', nombreCompleto: 'Matemáticas',
    });

    http.expectOne('/api/asignaturas/7').flush({}, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.asignaturas__error').textContent;
    // el `(409)` corta el prefijo: con `nombreCompleto` sería '…Matemáticas (409)'
    // y este aserto caería, que es justo lo que debe discriminar.
    expect(err).toContain('No se pudo borrar la asignatura Mat (409)');
  });
});
