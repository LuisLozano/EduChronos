import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { AsignaturaForm } from './asignatura-form';

/**
 * Congela el formulario. Foco de M3: (3)(4) la traducción del 400 —muestra el
 * message del backend cuando viaja, degradado cuando no— y (5) que un guardado
 * OK cierra el diálogo con `true`. (6) fija el orden de precedencia de
 * `mensaje()` (message gana a error), que en la lista quedó fuera de alcance.
 * Secuencia propia desde (1).
 */
describe('AsignaturaForm', () => {
  let fixture: ComponentFixture<AsignaturaForm>;
  let http: HttpTestingController;
  let ref: { close: ReturnType<typeof vi.fn> };

  function montar(data: unknown = null): void {
    ref = { close: vi.fn() };
    TestBed.configureTestingModule({
      imports: [AsignaturaForm],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: DialogRef, useValue: ref },
        { provide: DIALOG_DATA, useValue: data },
      ],
    });
    fixture = TestBed.createComponent(AsignaturaForm);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  it('(1) en alta el formulario nace vacío', () => {
    montar(null);
    expect(fixture.nativeElement.querySelector('.asignatura-form__titulo').textContent)
      .toContain('Nueva asignatura');
  });

  it('(2) en edición precarga los valores de la asignatura', () => {
    montar({ id: 7, codigo: 'Mat', nombreCompleto: 'Matemáticas' });
    const inputs = fixture.nativeElement.querySelectorAll('input');
    expect(inputs[0].value).toBe('Mat');
    expect(inputs[1].value).toBe('Matemáticas');
  });

  it('(3) un 400 CON message muestra el texto del backend (código duplicado), no el degradado', async () => {
    montar(null);
    const inst = fixture.componentInstance as unknown as {
      form: { setValue: (v: unknown) => void };
      guardar: () => void;
    };
    inst.form.setValue({ codigo: 'Mat', nombreCompleto: 'Matemáticas' });
    inst.guardar();

    http.expectOne('/api/asignaturas').flush(
      { message: 'Ya existe una asignatura con codigo Mat' },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.asignatura-form__error-servidor').textContent;
    expect(err).toContain('Ya existe una asignatura con codigo Mat');
    expect(err).not.toContain('No se pudo guardar la asignatura (400)');
  });

  it('(4) un 400 SIN message cae al degradado con status', async () => {
    montar(null);
    const inst = fixture.componentInstance as unknown as {
      form: { setValue: (v: unknown) => void };
      guardar: () => void;
    };
    inst.form.setValue({ codigo: 'Mat', nombreCompleto: 'Matemáticas' });
    inst.guardar();

    http.expectOne('/api/asignaturas').flush({}, { status: 400, statusText: 'Bad Request' });
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.asignatura-form__error-servidor').textContent;
    expect(err).toContain('No se pudo guardar la asignatura');
    expect(err).toContain('400');
  });

  it('(5) un guardado correcto cierra el diálogo con true', async () => {
    montar(null);
    const inst = fixture.componentInstance as unknown as {
      form: { setValue: (v: unknown) => void };
      guardar: () => void;
    };
    inst.form.setValue({ codigo: 'Mat', nombreCompleto: 'Matemáticas' });
    inst.guardar();

    http.expectOne('/api/asignaturas').flush({ id: 7, codigo: 'Mat', nombreCompleto: 'Matemáticas' });
    await fixture.whenStable();
    expect(ref.close).toHaveBeenCalledWith(true);
  });

  it('(6) mensaje(): message tiene precedencia sobre error', async () => {
    montar(null);
    const inst = fixture.componentInstance as unknown as {
      form: { setValue: (v: unknown) => void };
      guardar: () => void;
    };
    inst.form.setValue({ codigo: 'Mat', nombreCompleto: 'Matemáticas' });
    inst.guardar();

    // body con AMBOS: message debe ganar
    http.expectOne('/api/asignaturas').flush(
      { message: 'texto-de-message', error: 'texto-de-error' },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.asignatura-form__error-servidor').textContent;
    expect(err).toContain('texto-de-message');
    expect(err).not.toContain('texto-de-error');
  });
});
