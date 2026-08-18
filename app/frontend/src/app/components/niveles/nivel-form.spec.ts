import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { NivelForm } from './nivel-form';

/**
 * Congela el formulario. Focos: (3)(4)(6) la traducción del error, molde
 * `AsignaturaForm`; y (7)(8) la DECISIÓN PROPIA de S111 sobre `orden` —el campo es
 * obligatorio pero su VALOR no se restringe—, que son los dos casos que no existen
 * en ninguna otra entidad del catálogo. Secuencia propia desde (1).
 */
describe('NivelForm', () => {
  let fixture: ComponentFixture<NivelForm>;
  let http: HttpTestingController;
  let ref: { close: ReturnType<typeof vi.fn> };

  function montar(data: unknown = null): void {
    ref = { close: vi.fn() };
    TestBed.configureTestingModule({
      imports: [NivelForm],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: DialogRef, useValue: ref },
        { provide: DIALOG_DATA, useValue: data },
      ],
    });
    fixture = TestBed.createComponent(NivelForm);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  function instancia(): {
    form: { setValue: (v: unknown) => void };
    guardar: () => void;
  } {
    return fixture.componentInstance as unknown as {
      form: { setValue: (v: unknown) => void };
      guardar: () => void;
    };
  }

  afterEach(() => http.verify());

  it('(1) en alta el formulario nace vacío, también el orden', () => {
    montar(null);
    expect(fixture.nativeElement.querySelector('.nivel-form__titulo').textContent)
      .toContain('Nuevo nivel');
    const inputs = fixture.nativeElement.querySelectorAll('input');
    expect(inputs[0].value).toBe('');
    // Discriminante de la decisión de S111: si el control naciera en 0 o en 1,
    // el usuario podría guardar un orden que no ha elegido.
    expect(inputs[1].value).toBe('');
  });

  it('(2) en edición precarga los valores del nivel', () => {
    montar({ id: 7, codigo: '1ESO', orden: 1 });
    const inputs = fixture.nativeElement.querySelectorAll('input');
    expect(inputs[0].value).toBe('1ESO');
    expect(inputs[1].value).toBe('1');
  });

  it('(3) un 400 CON message muestra el texto del backend (código duplicado), no el degradado', async () => {
    montar(null);
    const inst = instancia();
    inst.form.setValue({ codigo: '1ESO', orden: 1 });
    inst.guardar();

    http.expectOne('/api/niveles').flush(
      { message: 'Ya existe un nivel con codigo 1ESO' },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.nivel-form__error-servidor').textContent;
    expect(err).toContain('Ya existe un nivel con codigo 1ESO');
    expect(err).not.toContain('(400)');
  });

  it('(4) un 400 SIN message cae al degradado con status', async () => {
    montar(null);
    const inst = instancia();
    inst.form.setValue({ codigo: '1ESO', orden: 1 });
    inst.guardar();

    http.expectOne('/api/niveles').flush({}, { status: 400, statusText: 'Bad Request' });
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.nivel-form__error-servidor').textContent;
    expect(err).toContain('No se pudo guardar el nivel');
    expect(err).toContain('400');
  });

  it('(5) un guardado correcto cierra el diálogo con true', async () => {
    montar(null);
    const inst = instancia();
    inst.form.setValue({ codigo: '1ESO', orden: 1 });
    inst.guardar();

    http.expectOne('/api/niveles').flush({ id: 7, codigo: '1ESO', orden: 1 });
    await fixture.whenStable();
    expect(ref.close).toHaveBeenCalledWith(true);
  });

  it('(6) mensaje(): message tiene precedencia sobre error', async () => {
    montar(null);
    const inst = instancia();
    inst.form.setValue({ codigo: '1ESO', orden: 1 });
    inst.guardar();

    http.expectOne('/api/niveles').flush(
      { message: 'texto-de-message', error: 'texto-de-error' },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.nivel-form__error-servidor').textContent;
    expect(err).toContain('texto-de-message');
    expect(err).not.toContain('texto-de-error');
  });

  it('(7) con el orden vacío NO se envía nada: el backend recibiría 0 en silencio', () => {
    montar(null);
    const inst = instancia();
    inst.form.setValue({ codigo: '1ESO', orden: null });
    inst.guardar();
    // Sin expectOne: el `http.verify()` del afterEach falla si hubo petición.
    // Es el único guardián del motivo por el que `orden` lleva required: el tipo
    // `int` primitivo del Request convierte una clave ausente en 0 sin error.
    expect(ref.close).not.toHaveBeenCalled();
  });

  it('(8) el orden 0 SÍ se envía: el formulario no es más estricto que la API', () => {
    montar(null);
    const inst = instancia();
    inst.form.setValue({ codigo: '1ESO', orden: 0 });
    inst.guardar();

    const req = http.expectOne('/api/niveles');
    // Congela que NO hay validador de rango ni de positividad. El backend acepta
    // 0 y negativos; rechazarlos aquí mentiría sobre el contrato (precedente S109).
    expect(req.request.body).toEqual({ codigo: '1ESO', orden: 0 });
    req.flush({ id: 7, codigo: '1ESO', orden: 0 });
  });
});
