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

  // ─────────────────────────── S203 T3a: horas de clase declaradas y cargo

  type ConCampos = {
    form: { setValue: (v: unknown) => void };
    guardar: () => void;
  };

  /** Escribe en un input como lo haría el usuario: valor y evento `input`. */
  function teclear(selector: string, valor: string): void {
    const input = fixture.nativeElement.querySelector(selector) as HTMLInputElement;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
  }

  /** Elige una opción del select de cargo por su valor, con el evento `change`. */
  function elegirCargo(valor: string): void {
    const select = fixture.nativeElement.querySelector('.profesor-form__cargo') as HTMLSelectElement;
    select.value = valor;
    select.dispatchEvent(new Event('change'));
  }

  it('(7) envía el total y el cargo elegidos', () => {
    montar(null);
    const inst = fixture.componentInstance as unknown as ConCampos;
    inst.form.setValue({ codigo: 'MAT8', nombreCompleto: 'Ana Ruiz' });
    teclear('.profesor-form__total', '18');
    elegirCargo('DIRECTOR');
    inst.guardar();

    const req = http.expectOne('/api/profesores');
    expect(req.request.body).toEqual({
      codigo: 'MAT8',
      nombreCompleto: 'Ana Ruiz',
      totalDeclarado: 18,
      cargo: 'DIRECTOR',
      guardiasOrdinarias: 0,
    });
    req.flush({ id: 7 });
  });

  it('(8) sin tocarlos, el total viaja como null y el cargo como PROFESOR', () => {
    montar(null);
    const inst = fixture.componentInstance as unknown as ConCampos;
    inst.form.setValue({ codigo: 'MAT8', nombreCompleto: 'Ana Ruiz' });
    inst.guardar();

    const req = http.expectOne('/api/profesores');
    expect(req.request.body).toEqual({
      codigo: 'MAT8',
      nombreCompleto: 'Ana Ruiz',
      totalDeclarado: null,
      cargo: 'PROFESOR',
      guardiasOrdinarias: 0,
    });
    req.flush({ id: 7 });
  });

  it('(9) un total escrito y luego borrado vuelve a viajar como null, no como 0', () => {
    montar(null);
    const inst = fixture.componentInstance as unknown as ConCampos;
    inst.form.setValue({ codigo: 'MAT8', nombreCompleto: 'Ana Ruiz' });
    teclear('.profesor-form__total', '5');
    teclear('.profesor-form__total', '');
    inst.guardar();

    const req = http.expectOne('/api/profesores');
    expect(req.request.body.totalDeclarado).toBeNull();
    req.flush({ id: 7 });
  });

  it('(10) el total 0 es válido y viaja como 0', () => {
    montar(null);
    const inst = fixture.componentInstance as unknown as ConCampos;
    inst.form.setValue({ codigo: 'MAT8', nombreCompleto: 'Ana Ruiz' });
    teclear('.profesor-form__total', '0');
    inst.guardar();

    const req = http.expectOne('/api/profesores');
    expect(req.request.body.totalDeclarado).toBe(0);
    req.flush({ id: 7 });
  });

  it('(11) con total negativo no se puede guardar ni se envía petición', async () => {
    montar(null);
    const inst = fixture.componentInstance as unknown as ConCampos;
    inst.form.setValue({ codigo: 'MAT8', nombreCompleto: 'Ana Ruiz' });
    teclear('.profesor-form__total', '-1');
    inst.guardar();
    await fixture.whenStable();

    http.expectNone('/api/profesores');
    expect(fixture.nativeElement.textContent).toContain('Las horas no pueden ser negativas.');
  });

  it('(12) el select de cargo ofrece los cinco y nace en Profesor/a', () => {
    montar(null);
    const select = fixture.nativeElement.querySelector('.profesor-form__cargo') as HTMLSelectElement;

    expect([...select.options].map((o) => o.textContent!.trim())).toEqual([
      'Profesor/a',
      'Jefe/a de Estudios',
      'Director/a',
      'Vicedirector/a',
      'Secretario/a',
    ]);
    expect(select.options[select.selectedIndex].textContent!.trim()).toBe('Profesor/a');
  });

  it('(13) al editar un profesor con 18 y DIRECTOR, los controles los muestran y guardar sin tocarlos los manda', async () => {
    montar({ id: 7, codigo: 'MAT8', nombreCompleto: 'Ana Ruiz', totalDeclarado: 18, cargo: 'DIRECTOR' });
    await fixture.whenStable();
    const total = fixture.nativeElement.querySelector('.profesor-form__total') as HTMLInputElement;
    const cargo = fixture.nativeElement.querySelector('.profesor-form__cargo') as HTMLSelectElement;
    expect(total.value).toBe('18');
    expect(cargo.options[cargo.selectedIndex].textContent!.trim()).toBe('Director/a');

    (fixture.componentInstance as unknown as ConCampos).guardar();

    const req = http.expectOne('/api/profesores/7');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({
      codigo: 'MAT8',
      nombreCompleto: 'Ana Ruiz',
      totalDeclarado: 18,
      cargo: 'DIRECTOR',
      guardiasOrdinarias: 0,
    });
    req.flush({ id: 7 });
  });

  // ─────────────────────────── S212: guardias ordinarias (C-dato-guardias, F1)

  it('(14) al editar un profesor con 18 horas y 0 guardias, poner 3 guardias viaja en el PUT sin tocar el total', async () => {
    montar({ id: 7, codigo: 'MAT8', nombreCompleto: 'Ana Ruiz', totalDeclarado: 18, guardiasOrdinarias: 0 });
    await fixture.whenStable();
    const guardias = fixture.nativeElement.querySelector('.profesor-form__guardias') as HTMLInputElement;
    expect(guardias.value).toBe('0');
    teclear('.profesor-form__guardias', '3');
    (fixture.componentInstance as unknown as ConCampos).guardar();

    const req = http.expectOne('/api/profesores/7');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.guardiasOrdinarias).toBe(3);
    expect(req.request.body.totalDeclarado).toBe(18);
    req.flush({ id: 7 });
  });

  it('(15) en edición carga las guardias que tiene el profesor', async () => {
    montar({ id: 7, codigo: 'MAT8', nombreCompleto: 'Ana Ruiz', totalDeclarado: 18, guardiasOrdinarias: 2 });
    await fixture.whenStable();

    expect((fixture.nativeElement.querySelector('.profesor-form__guardias') as HTMLInputElement).value).toBe('2');
  });

  it('(16) el alta lleva las guardias escritas', () => {
    montar(null);
    const inst = fixture.componentInstance as unknown as ConCampos;
    inst.form.setValue({ codigo: 'MAT8', nombreCompleto: 'Ana Ruiz' });
    teclear('.profesor-form__guardias', '4');
    inst.guardar();

    const req = http.expectOne('/api/profesores');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.guardiasOrdinarias).toBe(4);
    req.flush({ id: 7 });
  });

  it('(17) con −1 guardias el formulario es inválido, avisa y no se envía petición', async () => {
    montar(null);
    const inst = fixture.componentInstance as unknown as ConCampos;
    inst.form.setValue({ codigo: 'MAT8', nombreCompleto: 'Ana Ruiz' });
    teclear('.profesor-form__guardias', '-1');
    inst.guardar();
    await fixture.whenStable();

    http.expectNone('/api/profesores');
    expect(fixture.nativeElement.textContent).toContain('Las guardias tienen que ser un número entero, 0 o más.');
  });
});
