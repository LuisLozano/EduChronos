import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { ActividadForm } from './actividad-form';

/**
 * Congela el formulario de actividad de UNA plaza. Hereda del molde de `grupo-form` el
 * `montar()` que hace red y la traducción del 400, y del de `subgrupo-form` el
 * multiselect; los casos propios son el XOR de aula (4)(5)(6), el I7 en cliente (7), la
 * ausencia deliberada de regla sobre subgrupos (8), la derivación de `modoAula` en
 * edición (9), el cuerpo del PUT (10), el 409 por camino distinto del 400 (12) y la
 * guarda de multiplaza (13). Secuencia propia desde (1).
 *
 * <p><b>`montar()` hace CUATRO peticiones.</b> El `ngOnInit` pide asignaturas,
 * profesores, aulas y subgrupos. Con `http.verify()` en el `afterEach`, cualquier caso
 * que montase sin consumirlas fallaría por "open request", así que el flush de las cuatro
 * vive DENTRO del helper: es parte del montaje, no del escenario. El `detectChanges()`
 * posterior pinta los `<option>` que acaban de llegar.
 */
describe('ActividadForm', () => {
  let fixture: ComponentFixture<ActividadForm>;
  let http: HttpTestingController;
  let ref: { close: ReturnType<typeof vi.fn> };

  const ASIGNATURAS = [
    { id: 1, codigo: 'Mat', nombreCompleto: 'Matemáticas' },
    { id: 2, codigo: 'CyR', nombreCompleto: 'Cultura y Robótica' },
  ];
  const PROFESORES = [
    { id: 1, codigo: 'MATA', nombreCompleto: 'Ana' },
    { id: 2, codigo: 'MAT6', nombreCompleto: 'Bea' },
  ];
  const AULAS = [
    { id: 1, codigo: 'A1', tipo: 'ORDINARIA', capacidad: null, edificio: null, planta: null, sector: null },
    { id: 2, codigo: 'INF1', tipo: 'INFORMATICA', capacidad: null, edificio: null, planta: null, sector: null },
  ];
  const SUBGRUPOS = [
    { id: 1, codigo: '1ºA-Completo', grupos: ['1ºA'] },
    { id: 2, codigo: '1ºB-Completo', grupos: ['1ºB'] },
  ];

  /** Miembros protegidos que los casos necesitan tocar. */
  interface Interna {
    form: {
      patchValue: (v: unknown) => void;
      invalid: boolean;
    };
    plazas: {
      at: (i: number) => {
        controls: Record<string, { setValue: (v: unknown) => void; value: unknown }>;
      };
      length: number;
    };
    guardar: () => void;
  }

  function instancia(): Interna {
    return fixture.componentInstance as unknown as Interna;
  }

  /**
   * Monta el componente y consume las CUATRO peticiones del `ngOnInit`. Con `fallaAulas`
   * responde 500 a la de aulas, que es el escenario de (14): la variante vive dentro del
   * helper porque lo único que cambia es cómo se cierra esa misma petición.
   */
  function montar(data: unknown = null, fallaAulas = false): void {
    ref = { close: vi.fn() };
    TestBed.configureTestingModule({
      imports: [ActividadForm],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: DialogRef, useValue: ref },
        { provide: DIALOG_DATA, useValue: data },
      ],
    });
    fixture = TestBed.createComponent(ActividadForm);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges(); // dispara ngOnInit → los cuatro GET
    http.expectOne('/api/asignaturas').flush(ASIGNATURAS);
    http.expectOne('/api/profesores').flush(PROFESORES);
    const aulas = http.expectOne('/api/aulas');
    if (fallaAulas) {
      aulas.flush('', { status: 500, statusText: 'Server Error' });
    } else {
      aulas.flush(AULAS);
    }
    http.expectOne('/api/subgrupos').flush(SUBGRUPOS);
    fixture.detectChanges(); // pinta los <option> ya cargados
  }

  /** Monta SIN consumir peticiones: para el caso multiplaza, que no debe pedir nada. */
  function montarSinRed(data: unknown): void {
    ref = { close: vi.fn() };
    TestBed.configureTestingModule({
      imports: [ActividadForm],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: DialogRef, useValue: ref },
        { provide: DIALOG_DATA, useValue: data },
      ],
    });
    fixture = TestBed.createComponent(ActividadForm);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  /** Rellena la plaza única con un contenido válido de la rama indicada. */
  function rellenarPlaza(
    modo: 'FIJA' | 'CANDIDATAS',
    extra: Partial<Record<string, unknown>> = {},
  ): void {
    const fila = instancia().plazas.at(0);
    fila.controls['asignatura'].setValue('Mat');
    fila.controls['modoAula'].setValue(modo);
    if (modo === 'FIJA') {
      fila.controls['aulaFija'].setValue('A1');
    } else {
      fila.controls['aulasCandidatas'].setValue(['A1', 'INF1']);
    }
    fila.controls['profesores'].setValue(['MATA']);
    fila.controls['subgrupos'].setValue(['1ºA-Completo']);
    for (const [k, v] of Object.entries(extra)) {
      fila.controls[k].setValue(v);
    }
  }

  const ACTIVIDAD_FIJA = {
    id: 7,
    codigo: 'Mat-1ºA',
    asignatura: 'Mat',
    duracionTramos: 2,
    repeticionesPorSemana: 3,
    patronTemporal: 'AGRUPADA',
    requiereTutor: true,
    plazas: [
      {
        id: 11,
        codigo: 'Mat-1ºA-P1',
        asignatura: 'Mat',
        aulaFija: 'A1',
        aulasCandidatas: [] as string[],
        profesores: ['MATA'],
        subgrupos: ['1ºA-Completo'],
      },
    ],
  };

  const ACTIVIDAD_CANDIDATAS = {
    ...ACTIVIDAD_FIJA,
    id: 8,
    codigo: 'CyR-Bloque',
    asignatura: null as string | null,
    plazas: [
      {
        ...ACTIVIDAD_FIJA.plazas[0],
        id: 12,
        aulaFija: null as string | null,
        aulasCandidatas: ['A1', 'INF1'],
      },
    ],
  };

  afterEach(() => http.verify());

  it('(1) en alta nace con UNA plaza y los valores por defecto del contrato', () => {
    montar(null);
    const inst = instancia();
    expect(fixture.nativeElement.querySelector('.actividad-form__titulo').textContent)
      .toContain('Nueva actividad');
    // La fila nace DENTRO del FormArray desde el principio (el trozo B será un delta).
    expect(inst.plazas.length).toBe(1);
    const fila = inst.plazas.at(0);
    expect(fila.controls['modoAula'].value).toBe('FIJA');
    // Defaults: duración 1, repeticiones 1, patrón NEUTRA.
    const raiz = fixture.nativeElement as HTMLElement;
    const numeros = raiz.querySelectorAll<HTMLInputElement>('input[type="number"]');
    expect(numeros[0].value).toBe('1');
    expect(numeros[1].value).toBe('1');
    expect(raiz.querySelector<HTMLSelectElement>('select[formControlName="patronTemporal"]')!.value)
      .toBe('NEUTRA');
  });

  it('(2) los cuatro desplegables se pueblan con lo que llega del backend', () => {
    montar(null);
    const raiz = fixture.nativeElement as HTMLElement;

    // Asignatura de ACTIVIDAD: las dos asignaturas MÁS la opción vacía («varias»).
    const asigActividad = raiz.querySelector<HTMLSelectElement>(
      'select[formControlName="asignatura"]',
    )!;
    expect(asigActividad.options.length).toBe(ASIGNATURAS.length + 1);
    expect(asigActividad.options[0].value).toBe('');

    // Aula fija: las dos aulas MÁS el placeholder deshabilitado.
    const aulaFija = raiz.querySelector<HTMLSelectElement>('.actividad-form__aula-fija')!;
    expect(aulaFija.options.length).toBe(AULAS.length + 1);
    expect(aulaFija.options[0].disabled).toBe(true);
    // El texto lleva el TIPO, que es lo que el usuario necesita para elegir sin filtro I3.
    expect(aulaFija.options[2].textContent).toContain('INFORMATICA');

    // Multiselects: sin placeholder, tantas opciones como elementos.
    expect(raiz.querySelectorAll('.actividad-form__profesores option').length).toBe(2);
    expect(raiz.querySelectorAll('.actividad-form__subgrupos option').length).toBe(2);
  });

  it('(3) las aulas NO se filtran por compatibilidad I3: se ofrecen todas', () => {
    montar(null);
    const raiz = fixture.nativeElement as HTMLElement;
    const opciones = [...raiz.querySelectorAll('.actividad-form__aula-fija option')]
      .map((o) => (o as HTMLOptionElement).value)
      .filter((v) => v !== '');
    // Decisión tomada: habla el 400 del backend, no un filtro en cliente que se
    // desincroniza. Si alguien filtrara por tipo, esta igualdad caería.
    expect(opciones).toEqual(['A1', 'INF1']);
  });

  it('(4) XOR-FIJA: el POST lleva aulaFija con valor y aulasCandidatas vacías', () => {
    montar(null);
    const inst = instancia();
    inst.form.patchValue({ codigo: 'Mat-1ºA', duracionTramos: 2, repeticionesPorSemana: 3 });
    rellenarPlaza('FIJA');
    inst.guardar();

    const req = http.expectOne('/api/actividades');
    expect(req.request.method).toBe('POST');
    // Igualdad ESTRICTA: caza tanto colar `modoAula` (que es de UI y no del contrato)
    // como mandar las dos ramas del XOR a la vez.
    expect(req.request.body).toEqual({
      codigo: 'Mat-1ºA',
      asignatura: null,   // el select quedó en '' → null, no cadena vacía
      duracionTramos: 2,
      repeticionesPorSemana: 3,
      patronTemporal: 'NEUTRA',
      requiereTutor: false,
      plazas: [
        {
          asignatura: 'Mat',
          aulaFija: 'A1',
          aulasCandidatas: [],
          profesores: ['MATA'],
          subgrupos: ['1ºA-Completo'],
        },
      ],
    });
    req.flush(ACTIVIDAD_FIJA);
  });

  it('(5) XOR-CANDIDATAS: el POST lleva aulaFija null y ≥1 candidata', () => {
    montar(null);
    const inst = instancia();
    inst.form.patchValue({ codigo: 'CyR-Bloque', asignatura: 'CyR' });
    rellenarPlaza('CANDIDATAS');
    inst.guardar();

    const req = http.expectOne('/api/actividades');
    const plaza = (req.request.body as { plazas: unknown[] }).plazas[0];
    // `null`, NO cadena vacía: '' sería un código no resoluble y un 400 del backend.
    expect(plaza).toEqual({
      asignatura: 'Mat',
      aulaFija: null,
      aulasCandidatas: ['A1', 'INF1'],
      profesores: ['MATA'],
      subgrupos: ['1ºA-Completo'],
    });
    // La asignatura de ACTIVIDAD sí viaja cuando se elige (contraste con (4)).
    expect((req.request.body as { asignatura: string }).asignatura).toBe('CyR');
    req.flush(ACTIVIDAD_CANDIDATAS);
  });

  it('(6) cambiar de rama LIMPIA la anterior: el aula fija elegida no sobrevive', async () => {
    montar(null);
    const inst = instancia();
    const fila = inst.plazas.at(0);
    fila.controls['aulaFija'].setValue('A1');
    expect(fila.controls['aulaFija'].value).toBe('A1');

    fila.controls['modoAula'].setValue('CANDIDATAS');
    // Sin la limpieza, el aula abandonada seguiría en el control y reaparecería al
    // volver a la rama fija; y el formulario tendría las dos ramas rellenas, que es
    // justo lo que el contrato rechaza.
    expect(fila.controls['aulaFija'].value).toBe('');

    // La vuelta también limpia la otra rama.
    fila.controls['aulasCandidatas'].setValue(['A1', 'INF1']);
    fila.controls['modoAula'].setValue('FIJA');
    expect(fila.controls['aulasCandidatas'].value).toEqual([]);

    // Y el DOM sigue al modo: en FIJA se pinta el select único, no el múltiple.
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.actividad-form__aula-fija')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.actividad-form__candidatas')).toBeNull();
  });

  it('(7) I7: sin profesores el formulario NO envía nada', () => {
    montar(null);
    const inst = instancia();
    inst.form.patchValue({ codigo: 'Mat-1ºA' });
    rellenarPlaza('FIJA', { profesores: [] });
    inst.guardar();

    // `Validators.required` daría por bueno el array vacío; `arrayNoVacio` lo corta
    // aquí, replicando I7 sin adivinar el 400. El http.verify() del afterEach confirma
    // que no salió ninguna petición.
    expect(inst.form.invalid).toBe(true);
    http.expectNone('/api/actividades');
  });

  it('(8) D-plaza-sin-subgrupos: CERO subgrupos es válido y se envía como lista vacía', () => {
    montar(null);
    const inst = instancia();
    inst.form.patchValue({ codigo: 'Mat-1ºA' });
    rellenarPlaza('FIJA', { subgrupos: [] });
    inst.guardar();

    // El backend acepta una plaza sin subgrupos; poner aquí una regla que el contrato no
    // tiene haría que el formulario rechazara cuerpos que la API acepta. Si alguien le
    // añade `arrayNoVacio` a subgrupos, este caso se pone rojo: esa es su razón de ser.
    const req = http.expectOne('/api/actividades');
    expect((req.request.body as { plazas: { subgrupos: string[] }[] }).plazas[0].subgrupos)
      .toEqual([]);
    req.flush(ACTIVIDAD_FIJA);
  });

  it('(9) en edición precarga los valores y DERIVA modoAula del dato', () => {
    montar(ACTIVIDAD_CANDIDATAS);
    const inst = instancia();
    const fila = inst.plazas.at(0);

    expect(fixture.nativeElement.querySelector('.actividad-form__titulo').textContent)
      .toContain('Editar actividad');
    // aulaFija null ⇒ la rama derivada es CANDIDATAS, no el 'FIJA' de arranque.
    expect(fila.controls['modoAula'].value).toBe('CANDIDATAS');
    expect(fila.controls['aulasCandidatas'].value).toEqual(['A1', 'INF1']);
    expect(fila.controls['profesores'].value).toEqual(['MATA']);
    // La asignatura null de la actividad se precarga como '' (la opción «varias»).
    expect(fixture.nativeElement.querySelector('input').value).toBe('CyR-Bloque');
  });

  it('(10) en edición el PUT lleva el cuerpo COMPLETO, requiereTutor incluido', () => {
    montar(ACTIVIDAD_FIJA);
    const inst = instancia();
    inst.guardar();

    const req = http.expectOne('/api/actividades/7');
    expect(req.request.method).toBe('PUT');
    // Igualdad estricta de TODO el cuerpo: perder un escalar por el camino (p. ej.
    // requiereTutor, que es el único booleano y el más fácil de olvidar) apagaría una
    // propiedad de la actividad en silencio, porque el PUT reemplaza el estado entero.
    expect(req.request.body).toEqual({
      codigo: 'Mat-1ºA',
      asignatura: 'Mat',
      duracionTramos: 2,
      repeticionesPorSemana: 3,
      patronTemporal: 'AGRUPADA',
      requiereTutor: true,
      plazas: [
        {
          asignatura: 'Mat',
          aulaFija: 'A1',
          aulasCandidatas: [],
          profesores: ['MATA'],
          subgrupos: ['1ºA-Completo'],
        },
      ],
    });
    req.flush(ACTIVIDAD_FIJA);
  });

  it('(11) un 400 CON message muestra el texto accionable del backend, inline', async () => {
    montar(null);
    const inst = instancia();
    inst.form.patchValue({ codigo: 'Mat-1ºA' });
    rellenarPlaza('FIJA');
    inst.guardar();

    http.expectOne('/api/actividades').flush(
      { message: 'la asignatura Mat no admite el aula INF1 de tipo INFORMATICA' },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();
    fixture.detectChanges();
    const err = fixture.nativeElement.querySelector('.actividad-form__error-servidor').textContent;
    expect(err).toContain('no admite el aula INF1');
    // el 400 NO usa el hueco del 409: son caminos distintos
    expect(fixture.nativeElement.querySelector('.actividad-form__conflicto')).toBeNull();
  });

  it('(12) un 409 del PUT va al aviso APARTE y dice qué borrar antes', async () => {
    montar(ACTIVIDAD_FIJA);
    const inst = instancia();
    inst.guardar();

    http.expectOne('/api/actividades/7').flush(
      { message: 'No se puede editar: referenciada por 1 sesion(es), 1 aula(s) bloqueada(s)' },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();
    fixture.detectChanges();

    const aviso = fixture.nativeElement.querySelector('.actividad-form__conflicto');
    expect(aviso).not.toBeNull();
    // El desglose del backend, tal cual, MÁS la salida (qué hay que borrar).
    expect(aviso.textContent).toContain('1 sesion(es)');
    expect(aviso.textContent).toContain('Borra el horario generado');
    // Y NO se ha pintado como error inline: si compartieran hueco, el usuario leería
    // «corrige el formulario» ante algo que el formulario no arregla.
    expect(fixture.nativeElement.querySelector('.actividad-form__error-servidor')).toBeNull();
    // el diálogo sigue abierto: un 409 no es un guardado
    expect(ref.close).not.toHaveBeenCalled();
  });

  it('(13) GUARDA: una actividad multiplaza no se pinta ni se guarda, y no pide catálogos', () => {
    const multi = { ...ACTIVIDAD_FIJA, plazas: [...ACTIVIDAD_FIJA.plazas, ACTIVIDAD_CANDIDATAS.plazas[0]] };
    montarSinRed(multi);

    // Ni un GET: no hay formulario que poblar. El http.verify() del afterEach lo exige.
    const aviso = fixture.nativeElement.querySelector('.actividad-form__conflicto');
    expect(aviso).not.toBeNull();
    expect(aviso.textContent).toContain('varias plazas');
    // No se pinta el formulario a medias: guardar una plaza borraría la otra.
    expect(fixture.nativeElement.querySelector('form')).toBeNull();

    // Y aunque se invoque guardar() a mano, no sale ninguna petición.
    instancia().guardar();
    http.expectNone('/api/actividades/7');
  });

  it('(14) un fallo al cargar las aulas se presenta y deja el desplegable sin aulas', async () => {
    montar(null, true);
    await fixture.whenStable();

    const err = fixture.nativeElement.querySelector('.actividad-form__error-servidor').textContent;
    expect(err).toContain('No se pudieron cargar las aulas');
    expect(err).toContain('500');
    // Solo queda el placeholder: ni un <option> de aula real.
    expect(fixture.nativeElement.querySelectorAll('.actividad-form__aula-fija option').length).toBe(1);
  });

  it('(15) el multiselect vuelca al control lo que el usuario marca en el DOM', () => {
    montar(null);
    const select = fixture.nativeElement.querySelector(
      '.actividad-form__profesores',
    ) as HTMLSelectElement;
    select.options[0].selected = true;
    select.options[1].selected = true;
    select.dispatchEvent(new Event('change'));

    // El <select multiple> no vincula por formControlName: sin el handler, el control
    // seguiría vacío y el I7 cortaría un formulario que el usuario sí rellenó.
    expect(instancia().plazas.at(0).controls['profesores'].value).toEqual(['MATA', 'MAT6']);
  });

  it('(16) un guardado correcto cierra el diálogo con true', async () => {
    montar(null);
    const inst = instancia();
    inst.form.patchValue({ codigo: 'Mat-1ºA' });
    rellenarPlaza('FIJA');
    inst.guardar();

    http.expectOne('/api/actividades').flush(ACTIVIDAD_FIJA);
    await fixture.whenStable();
    expect(ref.close).toHaveBeenCalledWith(true);
  });
});
