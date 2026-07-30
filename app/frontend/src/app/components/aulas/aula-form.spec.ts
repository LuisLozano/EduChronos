import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { AulaForm } from './aula-form';
import { Aula, TIPOS_AULA } from '../../models/aula.model';

/**
 * Congela el formulario de aulas. Hereda del molde de `profesor-form.spec.ts` los
 * casos de traducción de error (3)(4)(6) y de cierre con `true` (5), y añade los tres
 * que profesores no podía tener porque no tenía enumerado ni opcionales:
 *
 * <ul>
 *   <li>(7) el selector OFRECE los ocho con semántica y NO ofrece COMUN (D-F8.5-C3-a);
 *   <li>(8) pero editar un aula que YA es COMUN conserva ese valor entre las opciones,
 *       para no borrar en silencio un dato existente;
 *   <li>(9) los cuatro opcionales en blanco viajan null, no `''` ni `0`.
 * </ul>
 *
 * Secuencia propia desde (1).
 */
describe('AulaForm', () => {
  let fixture: ComponentFixture<AulaForm>;
  let http: HttpTestingController;
  let ref: { close: ReturnType<typeof vi.fn> };

  /** Instancia con lo que los casos necesitan tocar (todo `protected` en el componente). */
  type Interna = {
    form: { setValue: (v: unknown) => void };
    guardar: () => void;
  };

  const aulaCompleta: Aula = {
    id: 7,
    codigo: 'A12',
    tipo: 'INFORMATICA',
    capacidad: 30,
    edificio: 'Norte',
    planta: 1,
    sector: 'B',
  };

  function montar(data: Aula | null = null): void {
    ref = { close: vi.fn() };
    TestBed.configureTestingModule({
      imports: [AulaForm],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: DialogRef, useValue: ref },
        { provide: DIALOG_DATA, useValue: data },
      ],
    });
    fixture = TestBed.createComponent(AulaForm);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  /** Rellena SOLO los obligatorios y envía: el camino del caso (9). */
  function guardarMinimo(): void {
    const inst = fixture.componentInstance as unknown as Interna;
    inst.form.setValue({
      codigo: 'A12',
      tipo: 'ORDINARIA',
      capacidad: null,
      edificio: '',
      planta: null,
      sector: '',
    });
    inst.guardar();
  }

  /** Rellena los seis campos y envía: el camino de los casos de error. */
  function guardarCompleto(): void {
    const inst = fixture.componentInstance as unknown as Interna;
    inst.form.setValue({
      codigo: 'A12',
      tipo: 'INFORMATICA',
      capacidad: 30,
      edificio: 'Norte',
      planta: 1,
      sector: 'B',
    });
    inst.guardar();
  }

  function textoError(): string {
    return fixture.nativeElement.querySelector('.aula-form__error-servidor').textContent;
  }

  afterEach(() => http.verify());

  it('(1) en alta el formulario nace vacío', () => {
    montar(null);
    expect(fixture.nativeElement.querySelector('.aula-form__titulo').textContent)
      .toContain('Nueva aula');
    const inputs = fixture.nativeElement.querySelectorAll('input');
    expect([...inputs].every((i: HTMLInputElement) => i.value === '')).toBe(true);
  });

  it('(2) en edición precarga los seis campos, incluidos los opcionales', () => {
    montar(aulaCompleta);
    expect(fixture.nativeElement.querySelector('.aula-form__titulo').textContent)
      .toContain('Editar aula');
    // Orden en el DOM: codigo, capacidad, edificio, planta, sector (el tipo es <select>).
    const inputs = fixture.nativeElement.querySelectorAll('input');
    expect(inputs[0].value).toBe('A12');
    expect(inputs[1].value).toBe('30');
    expect(inputs[2].value).toBe('Norte');
    expect(inputs[3].value).toBe('1');
    expect(inputs[4].value).toBe('B');
    expect(fixture.nativeElement.querySelector('select').value).toBe('INFORMATICA');
  });

  it('(3) un 400 CON message muestra el texto del backend (código duplicado), no el degradado', async () => {
    montar(null);
    guardarCompleto();

    http.expectOne('/api/aulas').flush(
      { message: 'Ya existe un aula con codigo A12' },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();
    expect(textoError()).toContain('Ya existe un aula con codigo A12');
    expect(textoError()).not.toContain('No se pudo guardar el aula (400)');
  });

  it('(4) un 400 SIN message cae al degradado con status', async () => {
    montar(null);
    guardarCompleto();

    http.expectOne('/api/aulas').flush({}, { status: 400, statusText: 'Bad Request' });
    await fixture.whenStable();
    expect(textoError()).toContain('No se pudo guardar el aula');
    expect(textoError()).toContain('400');
  });

  it('(5) un guardado correcto cierra el diálogo con true', async () => {
    montar(null);
    guardarCompleto();

    http.expectOne('/api/aulas').flush(aulaCompleta);
    await fixture.whenStable();
    expect(ref.close).toHaveBeenCalledWith(true);
  });

  it('(6) mensaje(): message tiene precedencia sobre error', async () => {
    montar(null);
    guardarCompleto();

    // body con AMBOS: message debe ganar
    http.expectOne('/api/aulas').flush(
      { message: 'texto-de-message', error: 'texto-de-error' },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();
    expect(textoError()).toContain('texto-de-message');
    expect(textoError()).not.toContain('texto-de-error');
  });

  it('(7) el selector ofrece los ocho tipos con semántica y NO ofrece COMUN', () => {
    montar(null);
    // Se leen del DOM, no de la constante: lo que se congela es lo que el usuario
    // puede ELEGIR. Se descarta el placeholder (value vacío).
    const valores = [...fixture.nativeElement.querySelectorAll('option')]
      .map((o: HTMLOptionElement) => o.value)
      .filter((v: string) => v !== '');

    expect(valores).toEqual([...TIPOS_AULA]);
    // Discriminante frente a un TIPOS_AULA vaciado, que dejaría verde el `not`:
    expect(valores.length).toBe(8);
    expect(valores).not.toContain('COMUN');
  });

  it('(8) al editar un aula que ya es COMUN, el selector conserva ese tipo', () => {
    montar({ ...aulaCompleta, tipo: 'COMUN' });
    const valores = [...fixture.nativeElement.querySelectorAll('option')]
      .map((o: HTMLOptionElement) => o.value)
      .filter((v: string) => v !== '');

    // Se añade el suyo SIN perder los ocho ofrecidos: nueve opciones, no una.
    expect(valores).toContain('COMUN');
    expect(valores.length).toBe(9);
    // Y queda SELECCIONADO: sin la opción, el select se mostraría vacío y el
    // `required` obligaría a reasignar tipo para poder tocar cualquier otro campo.
    expect(fixture.nativeElement.querySelector('select').value).toBe('COMUN');
  });

  it('(9) los opcionales en blanco viajan null, no cadena vacía', () => {
    montar(null);
    guardarMinimo();

    const req = http.expectOne('/api/aulas');
    expect(req.request.body).toEqual({
      codigo: 'A12',
      tipo: 'ORDINARIA',
      capacidad: null,
      edificio: null,
      planta: null,
      sector: null,
    });
    req.flush({ id: 7, codigo: 'A12', tipo: 'ORDINARIA' });
  });

  it('(10) en edición guarda con PUT sobre el id, no con POST', () => {
    montar(aulaCompleta);
    guardarCompleto();

    // Discriminante de la rama alta/edición: `esEdicion` decide el verbo y la URL, y
    // un POST aquí crearía un duplicado en vez de editar.
    const req = http.expectOne('/api/aulas/7');
    expect(req.request.method).toBe('PUT');
    req.flush(aulaCompleta);
  });

  it('(11) sin código ni tipo no se dispara petición alguna', () => {
    montar(null);
    (fixture.componentInstance as unknown as Interna).guardar();
    // http.verify() del afterEach es el aserto: cualquier petición lo pondría rojo.
    expect(ref.close).not.toHaveBeenCalled();
  });
});
