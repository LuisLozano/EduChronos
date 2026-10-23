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
 *
 * <p>Desde S250 el formulario pide `/api/niveles` en el `ngOnInit`: `montar` consume esa
 * petición, como en `GrupoForm`, para que el `http.verify()` del `afterEach` no tumbe los casos
 * que no hablan de niveles. (7) a (10) fijan el código corto, el nivel y el código resultante.
 */
describe('AsignaturaForm', () => {
  let fixture: ComponentFixture<AsignaturaForm>;
  let http: HttpTestingController;
  let ref: { close: ReturnType<typeof vi.fn> };

  const NIVELES = [
    { id: 1, codigo: '1ESO', orden: 1 },
    { id: 2, codigo: '2ESO', orden: 2 },
  ];

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
    fixture.detectChanges(); // dispara ngOnInit → GET /api/niveles
    http.expectOne('/api/niveles').flush(NIVELES);
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  it('(1) en alta el formulario nace vacío', () => {
    montar(null);
    expect(fixture.nativeElement.querySelector('.asignatura-form__titulo').textContent)
      .toContain('Nueva asignatura');
  });

  it('(2) en edición precarga los valores de la asignatura', () => {
    montar({ id: 7, codigo: 'Mat', codigoCorto: 'Mat', nombreCompleto: 'Matemáticas', nivel: null });
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
    inst.form.setValue({ codigoCorto: 'Mat', nivel: '', nombreCompleto: 'Matemáticas' });
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
    inst.form.setValue({ codigoCorto: 'Mat', nivel: '', nombreCompleto: 'Matemáticas' });
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
    inst.form.setValue({ codigoCorto: 'Mat', nivel: '', nombreCompleto: 'Matemáticas' });
    inst.guardar();

    http.expectOne('/api/asignaturas').flush({ id: 7, codigo: 'Mat', codigoCorto: 'Mat', nombreCompleto: 'Matemáticas', nivel: null });
    await fixture.whenStable();
    expect(ref.close).toHaveBeenCalledWith(true);
  });

  it('(6) mensaje(): message tiene precedencia sobre error', async () => {
    montar(null);
    const inst = fixture.componentInstance as unknown as {
      form: { setValue: (v: unknown) => void };
      guardar: () => void;
    };
    inst.form.setValue({ codigoCorto: 'Mat', nivel: '', nombreCompleto: 'Matemáticas' });
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

  it('(7) el desplegable de nivel arranca en «— sin nivel —» y ofrece los niveles del backend', () => {
    montar(null);
    const opciones = [...fixture.nativeElement.querySelectorAll('select option')].map(
      (o: HTMLOptionElement) => o.textContent?.trim(),
    );
    expect(opciones).toEqual(['— sin nivel —', '1ESO', '2ESO']);
    expect(fixture.nativeElement.querySelector('select').value).toBe('');
  });

  it('(8) envía (codigoCorto, nombreCompleto, nivel) y pinta el código que resultará', async () => {
    montar(null);
    const inst = fixture.componentInstance as unknown as {
      form: { setValue: (v: unknown) => void };
      guardar: () => void;
    };
    inst.form.setValue({ codigoCorto: 'Biol', nivel: '2ESO', nombreCompleto: 'Biología' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.asignatura-form__codigo').textContent).toContain('Biol-2ESO');

    inst.guardar();
    const peticion = http.expectOne('/api/asignaturas');
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({ codigoCorto: 'Biol', nombreCompleto: 'Biología', nivel: '2ESO' });
    peticion.flush({ id: 9, codigo: 'Biol-2ESO', codigoCorto: 'Biol', nombreCompleto: 'Biología', nivel: '2ESO' });
    await fixture.whenStable();
    expect(ref.close).toHaveBeenCalledWith(true);
  });

  it('(9) sin nivel envía nivel null y el código resultante es el corto', () => {
    montar(null);
    const inst = fixture.componentInstance as unknown as {
      form: { setValue: (v: unknown) => void };
      guardar: () => void;
    };
    inst.form.setValue({ codigoCorto: 'RED', nivel: '', nombreCompleto: 'Reunión' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.asignatura-form__codigo .dato').textContent.trim()).toBe('RED');

    inst.guardar();
    const peticion = http.expectOne('/api/asignaturas');
    expect(peticion.request.body).toEqual({ codigoCorto: 'RED', nombreCompleto: 'Reunión', nivel: null });
    peticion.flush({ id: 9, codigo: 'RED', codigoCorto: 'RED', nombreCompleto: 'Reunión', nivel: null });
  });

  it('(10) en edición precarga el corto y el nivel, y el PUT los lleva', () => {
    montar({ id: 7, codigo: 'Mat-1ESO', codigoCorto: 'Mat', nombreCompleto: 'Matemáticas', nivel: '1ESO' });
    expect(fixture.nativeElement.querySelector('input').value).toBe('Mat');
    expect(fixture.nativeElement.querySelector('select').value).toBe('1ESO');
    expect(fixture.nativeElement.querySelector('.asignatura-form__codigo').textContent).toContain('Mat-1ESO');

    (fixture.componentInstance as unknown as { guardar: () => void }).guardar();
    const peticion = http.expectOne('/api/asignaturas/7');
    expect(peticion.request.method).toBe('PUT');
    expect(peticion.request.body).toEqual({ codigoCorto: 'Mat', nombreCompleto: 'Matemáticas', nivel: '1ESO' });
    peticion.flush({ id: 7, codigo: 'Mat-1ESO', codigoCorto: 'Mat', nombreCompleto: 'Matemáticas', nivel: '1ESO' });
  });
});
