import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { SubgrupoForm } from './subgrupo-form';

/**
 * Congela el formulario de subgrupos. Hereda del molde de GrupoForm —(3)(4)
 * traducción del 400, (5) guardado OK cierra con `true`, (6) precedencia de
 * `mensaje()`— y añade los casos propios de la selección MÚLTIPLE poblada por red:
 * poblado (7), preselección en edición con DOS grupos (8), las dos ramas de guardado
 * con array —POST (9), PUT (11)—, el fallo de carga (10) y el validator de array
 * vacío (12). Secuencia propia desde (1).
 *
 * <p>Como GrupoForm, `montar()` hace red (ngOnInit pide /api/grupos): el flush vive
 * dentro del helper para que los casos heredados no fallen por "open request". El
 * segundo detectChanges pinta los <option> recién llegados.
 *
 * <p>La selección se prueba con la MEZCLA del molde: A1 (manipular el <select multiple>
 * real) para poblado y preselección —el riesgo del multiselect—, A2 (setValue directo)
 * para las ramas de guardado.
 */
describe('SubgrupoForm', () => {
  let fixture: ComponentFixture<SubgrupoForm>;
  let http: HttpTestingController;
  let ref: { close: ReturnType<typeof vi.fn> };

  const GRUPOS = [
    { id: 1, codigo: '1ºA', nivel: '1ESO', tipo: 'ORDINARIO' },
    { id: 2, codigo: '1ºB', nivel: '1ESO', tipo: 'ORDINARIO' },
    { id: 3, codigo: '1ºC', nivel: '1ESO', tipo: 'ORDINARIO' },
  ];

  function montar(
    data: unknown = null,
    grupos: unknown[] = GRUPOS,
    fallaGrupos = false,
  ): void {
    ref = { close: vi.fn() };
    TestBed.configureTestingModule({
      imports: [SubgrupoForm],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: DialogRef, useValue: ref },
        { provide: DIALOG_DATA, useValue: data },
      ],
    });
    fixture = TestBed.createComponent(SubgrupoForm);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges(); // dispara ngOnInit → GET /api/grupos
    const peticion = http.expectOne('/api/grupos');
    if (fallaGrupos) {
      peticion.flush('', { status: 500, statusText: 'Server Error' });
    } else {
      peticion.flush(grupos);
    }
    fixture.detectChanges(); // pinta los <option> ya cargados
  }

  function instancia(): { form: { setValue: (v: unknown) => void }; guardar: () => void } {
    return fixture.componentInstance as unknown as {
      form: { setValue: (v: unknown) => void };
      guardar: () => void;
    };
  }

  /** Marca opciones del <select multiple> real por su value y dispara `change`, como
   *  haría el usuario. Es el camino A1 del poblado. */
  function seleccionarEnDom(...codigos: string[]): void {
    const select: HTMLSelectElement = fixture.nativeElement.querySelector('select');
    for (const opt of Array.from(select.options)) {
      opt.selected = codigos.includes(opt.value);
    }
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  it('(1) en alta el formulario nace vacío', () => {
    montar(null);
    expect(fixture.nativeElement.querySelector('.subgrupo-form__titulo').textContent)
      .toContain('Nuevo subgrupo');
    // ninguna opción seleccionada de arranque
    const select: HTMLSelectElement = fixture.nativeElement.querySelector('select');
    expect(Array.from(select.selectedOptions).length).toBe(0);
  });

  it('(2) en edición precarga código y grupos', () => {
    montar({ id: 5, codigo: '1ºA-CyR-Tec', grupos: ['1ºA'] });
    expect(fixture.nativeElement.querySelector('input').value).toBe('1ºA-CyR-Tec');
    expect(fixture.nativeElement.querySelector('.subgrupo-form__titulo').textContent)
      .toContain('Editar subgrupo');
  });

  it('(3) un 400 CON message muestra el texto del backend, no el degradado', async () => {
    montar(null);
    const inst = instancia();
    inst.form.setValue({ codigo: '1ºA-CyR-Tec', grupos: ['1ºA'] });
    inst.guardar();
    http.expectOne('/api/subgrupos').flush(
      { message: 'Ya existe un subgrupo con codigo 1ºA-CyR-Tec' },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.subgrupo-form__error-servidor').textContent;
    expect(err).toContain('Ya existe un subgrupo con codigo 1ºA-CyR-Tec');
    expect(err).not.toContain('No se pudo guardar el subgrupo (400)');
  });

  it('(4) un 400 SIN message cae al degradado con status', async () => {
    montar(null);
    const inst = instancia();
    inst.form.setValue({ codigo: '1ºA-CyR-Tec', grupos: ['1ºA'] });
    inst.guardar();
    http.expectOne('/api/subgrupos').flush({}, { status: 400, statusText: 'Bad Request' });
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.subgrupo-form__error-servidor').textContent;
    expect(err).toContain('No se pudo guardar el subgrupo');
    expect(err).toContain('400');
  });

  it('(5) un guardado correcto cierra el diálogo con true', async () => {
    montar(null);
    const inst = instancia();
    inst.form.setValue({ codigo: '1ºA-CyR-Tec', grupos: ['1ºA'] });
    inst.guardar();
    http.expectOne('/api/subgrupos').flush({ id: 5, codigo: '1ºA-CyR-Tec', grupos: ['1ºA'] });
    await fixture.whenStable();
    expect(ref.close).toHaveBeenCalledWith(true);
  });

  it('(6) mensaje(): message tiene precedencia sobre error', async () => {
    montar(null);
    const inst = instancia();
    inst.form.setValue({ codigo: '1ºA-CyR-Tec', grupos: ['1ºA'] });
    inst.guardar();
    http.expectOne('/api/subgrupos').flush(
      { message: 'texto-de-message', error: 'texto-de-error' },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.subgrupo-form__error-servidor').textContent;
    expect(err).toContain('texto-de-message');
    expect(err).not.toContain('texto-de-error');
  });

  it('(7) el multiselect se puebla con los grupos que llegan del backend', () => {
    montar(null);
    const opciones = fixture.nativeElement.querySelectorAll('select option');
    // No hay placeholder en un <select multiple>: tantas opciones como grupos.
    expect(opciones.length).toBe(GRUPOS.length);
    expect(opciones[0].value).toBe('1ºA');
    expect(opciones[0].textContent.trim()).toBe('1ºA');
    expect(opciones[1].value).toBe('1ºB');
    expect(opciones[2].value).toBe('1ºC');
  });

  it('(8) en edición los grupos llegan PRESELECCIONADOS en el multiselect (dos grupos)', () => {
    // Dos grupos a propósito, y NO los dos primeros: {1ºA, 1ºC}. Un [selected] roto que
    // marcara todo, nada, o solo el primero daría verde falso con un único grupo o con
    // los dos consecutivos. Se mide el conjunto exacto de selectedOptions.
    montar({ id: 6, codigo: '1ºAC-Agrup', grupos: ['1ºA', '1ºC'] });
    const select: HTMLSelectElement = fixture.nativeElement.querySelector('select');
    const marcados = Array.from(select.selectedOptions).map((o) => o.value).sort();
    expect(marcados).toEqual(['1ºA', '1ºC']);
  });

  it('(9) el body del POST lleva el array de grupos seleccionado en el DOM', () => {
    // A1 para poblar (el usuario marca en el <select>), A2 no: aquí se mide que lo
    // marcado en el DOM llega al body, que es la cadena completa handler→control→request.
    montar(null);
    const inst = instancia();
    inst.form.setValue({ codigo: '1ºAC-Agrup', grupos: [] }); // código por control...
    seleccionarEnDom('1ºA', '1ºC');                            // ...grupos por el DOM
    inst.guardar();
    const req = http.expectOne('/api/subgrupos');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ codigo: '1ºAC-Agrup', grupos: ['1ºA', '1ºC'] });
    req.flush({ id: 7, codigo: '1ºAC-Agrup', grupos: ['1ºA', '1ºC'] });
  });

  it('(10) un fallo al cargar los grupos se presenta y deja el multiselect sin opciones', async () => {
    montar(null, GRUPOS, true);
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.subgrupo-form__error-servidor').textContent;
    expect(err).toContain('No se pudieron cargar los grupos');
    expect(err).toContain('500');
    const opciones = fixture.nativeElement.querySelectorAll('select option');
    expect(opciones.length).toBe(0);
  });

  it('(11) en edición el body del PUT lleva el array reemplazado', () => {
    montar({ id: 6, codigo: '1ºAC-Agrup', grupos: ['1ºA', '1ºC'] });
    const inst = instancia();
    inst.form.setValue({ codigo: '1ºAB-Agrup', grupos: [] });
    seleccionarEnDom('1ºA', '1ºB'); // reemplazo total, no delta
    inst.guardar();
    const req = http.expectOne('/api/subgrupos/6');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ codigo: '1ºAB-Agrup', grupos: ['1ºA', '1ºB'] });
    req.flush({ id: 6, codigo: '1ºAB-Agrup', grupos: ['1ºA', '1ºB'] });
  });

  it('(12) con grupos vacío el form es inválido y no se envía nada', () => {
    // arrayNoVacio: el validator propio, no Validators.required (que pasaría []).
    montar(null);
    const inst = instancia();
    inst.form.setValue({ codigo: '1ºA-CyR-Tec', grupos: [] });
    inst.guardar();
    http.expectNone('/api/subgrupos'); // no hubo POST: el guard de form.invalid frenó
  });
});
