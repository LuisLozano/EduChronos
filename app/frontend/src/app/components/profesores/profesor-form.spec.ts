import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { ProfesorForm } from './profesor-form';

/**
 * Congela el formulario. Foco de M3: (3)(4) la traducción del 400 —muestra el
 * message del backend cuando viaja, degradado cuando no— y (5) que un guardado
 * OK cierra el diálogo con `true`. (6) fija el orden de precedencia de
 * `mensaje()` (message gana a error), que en la lista quedó fuera de alcance.
 * Secuencia propia desde (1).
 */
describe('ProfesorForm', () => {
  let fixture: ComponentFixture<ProfesorForm>;
  let http: HttpTestingController;
  let ref: { close: ReturnType<typeof vi.fn> };

  function montar(data: unknown = null): void {
    ref = { close: vi.fn() };
    TestBed.configureTestingModule({
      imports: [ProfesorForm],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: DialogRef, useValue: ref },
        { provide: DIALOG_DATA, useValue: data },
      ],
    });
    fixture = TestBed.createComponent(ProfesorForm);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  it('(1) en alta el formulario nace vacío', () => {
    montar(null);
    expect(fixture.nativeElement.querySelector('.profesor-form__titulo').textContent)
      .toContain('Nuevo profesor');
  });

  it('(2) en edición precarga los valores del profesor', () => {
    montar({ id: 7, codigo: 'MAT8', nombreCompleto: 'Ana Ruiz' });
    const inputs = fixture.nativeElement.querySelectorAll('input');
    expect(inputs[0].value).toBe('MAT8');
    expect(inputs[1].value).toBe('Ana Ruiz');
  });

  it('(3) un 400 CON message muestra el texto del backend (código duplicado), no el degradado', async () => {
    montar(null);
    const inst = fixture.componentInstance as unknown as {
      form: { setValue: (v: unknown) => void };
      guardar: () => void;
    };
    inst.form.setValue({ codigo: 'MAT8', nombreCompleto: 'Ana Ruiz' });
    inst.guardar();

    http.expectOne('/api/profesores').flush(
      { message: 'Ya existe un profesor con codigo MAT8' },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.profesor-form__error-servidor').textContent;
    expect(err).toContain('Ya existe un profesor con codigo MAT8');
    expect(err).not.toContain('No se pudo guardar el profesor (400)');
  });

  it('(4) un 400 SIN message cae al degradado con status', async () => {
    montar(null);
    const inst = fixture.componentInstance as unknown as {
      form: { setValue: (v: unknown) => void };
      guardar: () => void;
    };
    inst.form.setValue({ codigo: 'MAT8', nombreCompleto: 'Ana Ruiz' });
    inst.guardar();

    http.expectOne('/api/profesores').flush({}, { status: 400, statusText: 'Bad Request' });
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.profesor-form__error-servidor').textContent;
    expect(err).toContain('No se pudo guardar el profesor');
    expect(err).toContain('400');
  });

  it('(5) un guardado correcto cierra el diálogo con true', async () => {
    montar(null);
    const inst = fixture.componentInstance as unknown as {
      form: { setValue: (v: unknown) => void };
      guardar: () => void;
    };
    inst.form.setValue({ codigo: 'MAT8', nombreCompleto: 'Ana Ruiz' });
    inst.guardar();

    http.expectOne('/api/profesores').flush({ id: 7, codigo: 'MAT8', nombreCompleto: 'Ana Ruiz' });
    await fixture.whenStable();
    expect(ref.close).toHaveBeenCalledWith(true);
  });

  it('(6) mensaje(): message tiene precedencia sobre error', async () => {
    montar(null);
    const inst = fixture.componentInstance as unknown as {
      form: { setValue: (v: unknown) => void };
      guardar: () => void;
    };
    inst.form.setValue({ codigo: 'MAT8', nombreCompleto: 'Ana Ruiz' });
    inst.guardar();

    // body con AMBOS: message debe ganar
    http.expectOne('/api/profesores').flush(
      { message: 'texto-de-message', error: 'texto-de-error' },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.profesor-form__error-servidor').textContent;
    expect(err).toContain('texto-de-message');
    expect(err).not.toContain('texto-de-error');
  });
});
