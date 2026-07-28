import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { Dialog } from '@angular/cdk/dialog';
import { ProfesorLista } from './profesor-lista';

/**
 * Congela el comportamiento de la lista. El foco de M3 (traducción de error) son
 * los casos (4)(5): que el 409 muestra el TEXTO DEL BACKEND cuando viaja, y el
 * DEGRADADO cuando no. El borrado se dispara con el diálogo espiado a `true`,
 * sin overlay real. Secuencia propia desde (1). Fuera de alcance por decisión,
 * no por imposibilidad: estilos, y el orden interno de `mensaje()` (cubierto en
 * el form, misma función).
 */
describe('ProfesorLista', () => {
  let fixture: ComponentFixture<ProfesorLista>;
  let http: HttpTestingController;
  let dialog: { open: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    dialog = { open: vi.fn() };
    TestBed.configureTestingModule({
      imports: [ProfesorLista],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Dialog, useValue: dialog },
      ],
    });
    fixture = TestBed.createComponent(ProfesorLista);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function flushLista(filas: unknown[] = []): void {
    fixture.detectChanges(); // dispara ngOnInit → cargar()
    http.expectOne('/api/profesores').flush(filas);
  }

  it('(1) carga la lista en init y la pinta', async () => {
    flushLista([{ id: 7, codigo: 'MAT8', nombreCompleto: 'Ana Ruiz' }]);
    await fixture.whenStable();
    const filas = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(filas.length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('Ana Ruiz');
  });

  it('(2) lista vacía muestra la invitación a crear el primero', async () => {
    flushLista([]);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.profesores__vacio')).toBeTruthy();
  });

  it('(3) error de carga cae al degradado con status', async () => {
    fixture.detectChanges();
    http.expectOne('/api/profesores').flush('', { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.profesores__error').textContent;
    expect(err).toContain('No se pudo cargar');
    expect(err).toContain('500');
  });

  it('(4) al borrar, un 409 CON message muestra el texto RICO del backend, no el degradado', async () => {
    flushLista([{ id: 7, codigo: 'MAT8', nombreCompleto: 'Ana Ruiz' }]);
    await fixture.whenStable();
    // el diálogo de confirmación devuelve true
    dialog.open.mockReturnValue({ closed: { subscribe: (fn: (v: boolean) => void) => fn(true) } });

    (fixture.componentInstance as unknown as { borrar: (p: unknown) => void }).borrar({
      id: 7, codigo: 'MAT8', nombreCompleto: 'Ana Ruiz',
    });

    http.expectOne('/api/profesores/7').flush(
      { message: 'No se puede borrar: referenciada por 2 plaza(s), 1 tutoria(s)' },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.profesores__error').textContent;
    expect(err).toContain('referenciada por 2 plaza(s)');
    // discriminante: NO cae al degradado
    expect(err).not.toContain('No se pudo borrar a Ana Ruiz (409)');
  });

  it('(5) al borrar, un 409 SIN message cae al degradado que dice qué pasó', async () => {
    flushLista([{ id: 7, codigo: 'MAT8', nombreCompleto: 'Ana Ruiz' }]);
    await fixture.whenStable();
    dialog.open.mockReturnValue({ closed: { subscribe: (fn: (v: boolean) => void) => fn(true) } });

    (fixture.componentInstance as unknown as { borrar: (p: unknown) => void }).borrar({
      id: 7, codigo: 'MAT8', nombreCompleto: 'Ana Ruiz',
    });

    http.expectOne('/api/profesores/7').flush({}, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.profesores__error').textContent;
    expect(err).toContain('No se pudo borrar a Ana Ruiz');
    expect(err).toContain('409');
  });
});
