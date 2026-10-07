import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { GrupoForm } from './grupo-form';

/**
 * Congela el formulario de grupos. Hereda el foco del molde —(3)(4) traducción del
 * 400, (5) el guardado OK cierra con `true`, (6) precedencia de `mensaje()`— y añade
 * los cinco casos propios de la única dimensión nueva de S104: el desplegable poblado
 * por red (7)(8), su fallo de carga (10), y el `tipo` que viaja en el cuerpo sin estar
 * en la UI, en las DOS ramas de guardado —POST (9) y PUT (11)—.
 * Secuencia propia desde (1).
 *
 * <p><b>EXTENSIÓN DEL MOLDE: `montar()` ahora hace red.</b> El helper de
 * `AsignaturaForm` no dispara ninguna petición; este sí, porque el `ngOnInit` pide
 * `/api/niveles`. Con `http.verify()` en el `afterEach`, CUALQUIER caso que montase el
 * componente sin consumir esa petición fallaría por "open request" —incluidos los seis
 * heredados, que no hablan de niveles—. Por eso el flush de `/api/niveles` vive DENTRO
 * del helper y no en cada caso: es parte del montaje, no del escenario.
 *
 * <p>El segundo `detectChanges()` tras el flush no es ceremonia: pinta los `<option>`
 * que acaban de llegar, sin lo cual (7) y (8) medirían un `<select>` vacío.
 */
describe('GrupoForm', () => {
  let fixture: ComponentFixture<GrupoForm>;
  let http: HttpTestingController;
  let ref: { close: ReturnType<typeof vi.fn> };

  const NIVELES = [
    { id: 1, codigo: '1ESO', orden: 1 },
    { id: 2, codigo: '2ESO', orden: 2 },
  ];

  /** S206. Las aulas del selector «Aula del grupo»; la guardada en (18) es la SEGUNDA. */
  const AULAS = [
    { id: 31, codigo: 'A13', tipo: 'ORDINARIA', capacidad: null, edificio: null, planta: null, sector: null },
    { id: 32, codigo: 'A14', tipo: 'ORDINARIA', capacidad: null, edificio: null, planta: null, sector: null },
  ];

  /**
   * Monta el componente y consume la petición de niveles del `ngOnInit`. Con
   * `fallaNiveles` responde 500 en vez de datos, que es el escenario de (10): la
   * variante vive DENTRO del helper —tres líneas— en vez de en un segundo helper
   * duplicado, porque lo único que cambia es cómo se cierra esa misma petición.
   */
  function montar(
    data: unknown = null,
    niveles: unknown[] = NIVELES,
    fallaNiveles = false,
    fallaAulas = false,
  ): void {
    ref = { close: vi.fn() };
    TestBed.configureTestingModule({
      imports: [GrupoForm],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: DialogRef, useValue: ref },
        { provide: DIALOG_DATA, useValue: data },
      ],
    });
    fixture = TestBed.createComponent(GrupoForm);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges(); // dispara ngOnInit → GET /api/niveles
    const peticion = http.expectOne('/api/niveles');
    if (fallaNiveles) {
      peticion.flush('', { status: 500, statusText: 'Server Error' });
    } else {
      peticion.flush(niveles);
    }
    // S206: el ngOnInit pide también las aulas del selector «Aula del grupo».
    const aulas = http.expectOne('/api/aulas');
    if (fallaAulas) {
      aulas.flush('', { status: 500, statusText: 'Server Error' });
    } else {
      aulas.flush(AULAS);
    }
    fixture.detectChanges(); // pinta los <option> ya cargados
  }

  /** Acceso a los dos miembros protegidos que los casos necesitan tocar. */
  function instancia(): { form: { setValue: (v: unknown) => void }; guardar: () => void } {
    return fixture.componentInstance as unknown as {
      form: { setValue: (v: unknown) => void };
      guardar: () => void;
    };
  }

  afterEach(() => http.verify());

  it('(1) en alta el formulario nace vacío', () => {
    montar(null);
    expect(fixture.nativeElement.querySelector('.grupo-form__titulo').textContent)
      .toContain('Nuevo grupo');
    // sin preselección: el control arranca en '' aunque ya haya opciones cargadas.
    expect(fixture.nativeElement.querySelector('select').value).toBe('');
  });

  it('(2) en edición precarga los valores del grupo', () => {
    montar({ id: 7, codigo: '1ESOA', nivel: '1ESO', tipo: 'ORDINARIO' });
    expect(fixture.nativeElement.querySelector('input').value).toBe('1ESOA');
    expect(fixture.nativeElement.querySelector('.grupo-form__titulo').textContent)
      .toContain('Editar grupo');
  });

  it('(3) un 400 CON message muestra el texto del backend (código duplicado), no el degradado', async () => {
    montar(null);
    const inst = instancia();
    inst.form.setValue({ codigo: '1ESOA', nivel: '1ESO' });
    inst.guardar();

    http.expectOne('/api/grupos').flush(
      { message: 'Ya existe un grupo con codigo 1ESOA' },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.grupo-form__error-servidor').textContent;
    expect(err).toContain('Ya existe un grupo con codigo 1ESOA');
    expect(err).not.toContain('No se pudo guardar el grupo (400)');
  });

  it('(4) un 400 SIN message cae al degradado con status', async () => {
    montar(null);
    const inst = instancia();
    inst.form.setValue({ codigo: '1ESOA', nivel: '1ESO' });
    inst.guardar();

    http.expectOne('/api/grupos').flush({}, { status: 400, statusText: 'Bad Request' });
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.grupo-form__error-servidor').textContent;
    expect(err).toContain('No se pudo guardar el grupo');
    expect(err).toContain('400');
  });

  it('(5) un guardado correcto cierra el diálogo con true', async () => {
    montar(null);
    const inst = instancia();
    inst.form.setValue({ codigo: '1ESOA', nivel: '1ESO' });
    inst.guardar();

    http.expectOne('/api/grupos').flush(
      { id: 7, codigo: '1ESOA', nivel: '1ESO', tipo: 'ORDINARIO' },
    );
    await fixture.whenStable();
    expect(ref.close).toHaveBeenCalledWith(true);
  });

  it('(6) mensaje(): message tiene precedencia sobre error', async () => {
    montar(null);
    const inst = instancia();
    inst.form.setValue({ codigo: '1ESOA', nivel: '1ESO' });
    inst.guardar();

    // body con AMBOS: message debe ganar
    http.expectOne('/api/grupos').flush(
      { message: 'texto-de-message', error: 'texto-de-error' },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.grupo-form__error-servidor').textContent;
    expect(err).toContain('texto-de-message');
    expect(err).not.toContain('texto-de-error');
  });

  it('(7) el desplegable se puebla con los niveles que llegan del backend', () => {
    montar(null);
    // El de niveles es el PRIMER select; desde S206 hay otro, el del aula.
    const opciones = fixture.nativeElement.querySelector('select').querySelectorAll('option');
    // Los niveles MÁS el placeholder de arranque, que no es un nivel: DOS niveles y no
    // uno, porque con uno solo un `@for` roto que pintara únicamente el primer
    // elemento quedaría verde. El conteo mide que el bucle itera; los textos, que
    // pinta el `codigo` y no otro campo.
    expect(opciones.length).toBe(NIVELES.length + 1);
    // índice 0 = placeholder: no seleccionable y con value vacío, para que case el ''
    // inicial del control sin ser un nivel elegible.
    expect(opciones[0].value).toBe('');
    expect(opciones[0].disabled).toBe(true);
    // los niveles reales van DESPLAZADOS una posición por el placeholder
    expect(opciones[1].textContent.trim()).toBe('1ESO');
    expect(opciones[2].textContent.trim()).toBe('2ESO');
    // el `value` es el código, que es lo que el contrato espera en `nivel`
    expect(opciones[1].value).toBe('1ESO');
  });

  it('(8) en edición el nivel llega PRESELECCIONADO en el select', () => {
    // El `setValue` corre en el constructor, ANTES de que existan los <option>: este
    // caso mide la reconciliación posterior, que es el riesgo real del desplegable
    // asíncrono. El nivel elegido es el SEGUNDO de NIVELES a propósito, y sigue
    // siéndolo tras meter el placeholder: con el primero, un `<select>` que se quedara
    // en su opción de arranque daría verde falso. Por eso `selectedIndex` es 2 —el
    // placeholder ocupa el 0 y '1ESO' el 1—, no 1.
    montar({ id: 8, codigo: '2ESOB', nivel: '2ESO', tipo: 'ORDINARIO' });
    const select = fixture.nativeElement.querySelector('select');
    expect(select.value).toBe('2ESO');
    expect(select.selectedIndex).toBe(2);
    // y no es el placeholder ni el primer nivel: doble red contra el verde falso
    expect(select.options[select.selectedIndex].textContent.trim()).toBe('2ESO');
  });

  it("(9) el body lleva tipo:'ORDINARIO' aunque la UI no tenga ese campo", () => {
    montar(null);
    const inst = instancia();
    inst.form.setValue({ codigo: '1ESOA', nivel: '1ESO' });
    inst.guardar();

    const req = http.expectOne('/api/grupos');
    // Igualdad ESTRICTA de los tres campos, no `toContain` del tipo: así cae tanto
    // olvidar `tipo` (el backend respondería 400 y esto quedaría verde sin el aserto)
    // como colar campos que el contrato no declara.
    expect(req.request.body).toEqual({
      codigo: '1ESOA',
      nivel: '1ESO',
      tipo: 'ORDINARIO',
      totalDeclarado: null,
      aulaReferencia: null,
    });
    req.flush({ id: 7, codigo: '1ESOA', nivel: '1ESO', tipo: 'ORDINARIO' });
    // La rama de edición tiene su propio caso en (11): aunque hoy ambas ramas
    // construyan el cuerpo con la misma expresión, eso es una propiedad del código
    // actual y no del contrato, y el spec no debe depender de que siga siéndolo.
  });

  it('(10) un fallo al cargar los niveles se presenta y deja el desplegable sin niveles', async () => {
    montar(null, NIVELES, true);
    await fixture.whenStable();

    const err = fixture.nativeElement.querySelector('.grupo-form__error-servidor').textContent;
    expect(err).toContain('No se pudieron cargar los niveles');
    expect(err).toContain('500');

    // Solo queda el placeholder: ni un `<option>` de nivel real. El aserto es de
    // igualdad a 1 y no `not.toBe(3)`, para que también caiga si el `@for` pintara
    // basura a partir de una lista de error.
    const opciones = fixture.nativeElement.querySelector('select').querySelectorAll('option');
    expect(opciones.length).toBe(1);
    expect(opciones[0].value).toBe('');
    expect(opciones[0].disabled).toBe(true);
  });

  it("(11) en edición el body del PUT también lleva tipo:'ORDINARIO'", () => {
    montar({ id: 8, codigo: '2ESOB', nivel: '2ESO', tipo: 'ORDINARIO' });
    const inst = instancia();
    // se cambian AMBOS campos respecto al grupo editado: así el aserto mide lo que el
    // usuario escribió y no lo que traía el DIALOG_DATA.
    inst.form.setValue({ codigo: '2ESOC', nivel: '1ESO' });
    inst.guardar();

    const req = http.expectOne('/api/grupos/8');
    expect(req.request.method).toBe('PUT');
    // Simétrico a (9): romper la inyección de `tipo` SOLO en la rama de edición
    // quedaría verde sin este caso, porque (9) únicamente recorre el POST.
    expect(req.request.body).toEqual({
      codigo: '2ESOC',
      nivel: '1ESO',
      tipo: 'ORDINARIO',
      totalDeclarado: null,
      aulaReferencia: null,
    });
    req.flush({ id: 8, codigo: '2ESOC', nivel: '1ESO', tipo: 'ORDINARIO' });
  });

  // ─────────────────────────── S203 T3a: horas semanales declaradas

  function teclearTotal(valor: string): void {
    const input = fixture.nativeElement.querySelector('.grupo-form__total') as HTMLInputElement;
    input.value = valor;
    input.dispatchEvent(new Event('input'));
  }

  it('(12) envía el total escrito', () => {
    montar(null);
    instancia().form.setValue({ codigo: '1ESOA', nivel: '1ESO' });
    teclearTotal('30');
    instancia().guardar();

    const req = http.expectOne('/api/grupos');
    expect(req.request.body).toEqual({
      codigo: '1ESOA',
      nivel: '1ESO',
      tipo: 'ORDINARIO',
      totalDeclarado: 30,
      aulaReferencia: null,
    });
    req.flush({ id: 7 });
  });

  it('(13) al editar un grupo con total 30, guardar sin tocarlo envía 30', async () => {
    montar({ id: 8, codigo: '2ESOB', nivel: '2ESO', tipo: 'ORDINARIO', totalDeclarado: 30 });
    await fixture.whenStable();
    const total = fixture.nativeElement.querySelector('.grupo-form__total') as HTMLInputElement;
    expect(total.value).toBe('30');

    instancia().guardar();

    const req = http.expectOne('/api/grupos/8');
    expect(req.request.body).toEqual({
      codigo: '2ESOB',
      nivel: '2ESO',
      tipo: 'ORDINARIO',
      totalDeclarado: 30,
      aulaReferencia: null,
    });
    req.flush({ id: 8 });
  });

  it('(14) con total negativo no se puede guardar', async () => {
    montar(null);
    instancia().form.setValue({ codigo: '1ESOA', nivel: '1ESO' });
    teclearTotal('-1');
    instancia().guardar();
    await fixture.whenStable();

    http.expectNone('/api/grupos');
    expect(fixture.nativeElement.textContent).toContain('Las horas no pueden ser negativas.');
  });

  // ─────────────────────────── S206 T2: aula del grupo

  const selectAula = (): HTMLSelectElement =>
    fixture.nativeElement.querySelector('.grupo-form__aula') as HTMLSelectElement;

  it('(15) el selector de aula ofrece «— sin aula —» y todas las aulas, con su código como value', () => {
    montar(null);
    const opciones = [...selectAula().options];
    expect(opciones.map((o) => o.value)).toEqual(['', 'A13', 'A14']);
    expect(opciones[0].textContent!.trim()).toBe('— sin aula —');
    // Seleccionable, al revés que el placeholder de niveles: elegirlo es como se quita el aula.
    expect(opciones[0].disabled).toBe(false);
    expect(selectAula().value).toBe('');
  });

  it('(16) en alta, sin elegir aula, el cuerpo lleva aulaReferencia:null', () => {
    montar(null);
    instancia().form.setValue({ codigo: '1ESOA', nivel: '1ESO' });
    instancia().guardar();

    const req = http.expectOne('/api/grupos');
    expect(req.request.body.aulaReferencia).toBeNull();
    req.flush({ id: 7 });
  });

  it('(17) elegir un aula manda su código', async () => {
    montar(null);
    instancia().form.setValue({ codigo: '1ESOA', nivel: '1ESO' });
    selectAula().value = 'A14';
    selectAula().dispatchEvent(new Event('change'));
    await fixture.whenStable();
    instancia().guardar();

    const req = http.expectOne('/api/grupos');
    expect(req.request.body.aulaReferencia).toBe('A14');
    req.flush({ id: 7 });
  });

  it('(18) al editar un grupo con aula, llega PRESELECCIONADA y guardar sin tocarla la reenvía', async () => {
    // Es el defecto que dejó abierto el T1: en el backend un aula ausente en el PUT deja el
    // grupo sin aula, así que editar otro campo y no reenviarla la borraría.
    montar({ id: 8, codigo: '2ESOB', nivel: '2ESO', tipo: 'ORDINARIO', aulaReferencia: 'A14' });
    await fixture.whenStable();
    expect(selectAula().value).toBe('A14');
    expect(selectAula().selectedIndex).toBe(2);

    instancia().form.setValue({ codigo: '2ESOC', nivel: '2ESO' });
    instancia().guardar();

    const req = http.expectOne('/api/grupos/8');
    expect(req.request.body).toEqual({
      codigo: '2ESOC',
      nivel: '2ESO',
      tipo: 'ORDINARIO',
      totalDeclarado: null,
      aulaReferencia: 'A14',
    });
    req.flush({ id: 8 });
  });

  it('(19) al editar, elegir «— sin aula —» manda null', async () => {
    montar({ id: 8, codigo: '2ESOB', nivel: '2ESO', tipo: 'ORDINARIO', aulaReferencia: 'A13' });
    await fixture.whenStable();
    selectAula().value = '';
    selectAula().dispatchEvent(new Event('change'));
    await fixture.whenStable();
    instancia().guardar();

    const req = http.expectOne('/api/grupos/8');
    expect(req.request.body.aulaReferencia).toBeNull();
    req.flush({ id: 8 });
  });

  it('(20) un fallo al cargar las aulas se presenta', async () => {
    montar(null, NIVELES, false, true);
    await fixture.whenStable();

    const err = fixture.nativeElement.querySelector('.grupo-form__error-servidor').textContent;
    expect(err).toContain('No se pudieron cargar las aulas');
    expect([...selectAula().options].map((o) => o.value)).toEqual(['']);
  });
});
