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
 * edición (9), el cuerpo del PUT (10) y el 409 por camino distinto del 400 (12).
 * Secuencia propia desde (1).
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
    anadirPlaza: () => void;
    quitarPlaza: (indice: number) => void;
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

  /** Rellena la plaza única con un contenido válido de la rama indicada. */
  function rellenarPlaza(
    indice: number,
    modo: 'FIJA' | 'CANDIDATAS',
    extra: Partial<Record<string, unknown>> = {},
  ): void {
    const fila = instancia().plazas.at(indice);
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

  /**
   * Añade una fila POR CLICK, no llamando al método. En zoneless, mutar un `FormArray` no
   * marca la vista sucia —no es escritura de señal—, así que solo el listener del botón
   * programa detección de cambios. Llamar al método y forzar con `detectChanges()` lanza
   * NG0100 (el `[formGroupName]="$index"` cambia después de comprobarse) y pondría rojo un
   * código correcto; llamar al método y esperar `whenStable()` a secas no repinta y no
   * discrimina nada. El click es el camino real y el único harness limpio.
   */
  async function anadirFila(): Promise<void> {
    const boton = fixture.nativeElement.querySelector(
      '.actividad-form__anadir-plaza',
    ) as HTMLButtonElement;
    boton.click();
    await fixture.whenStable();
  }

  /** Quita la fila indicada por CLICK en SU botón. Misma razón que `anadirFila`. */
  async function quitarFila(indice: number): Promise<void> {
    const botones = fixture.nativeElement.querySelectorAll(
      '.actividad-form__quitar-plaza',
    ) as NodeListOf<HTMLButtonElement>;
    botones[indice].click();
    await fixture.whenStable();
  }

  /** Una plaza del bloque de seis. Las pares van por aula fija y las impares por
   *  candidatas, y cada una tiene profesor y subgrupo propios: así ninguna se confunde
   *  con otra si el orden o la posición se pierden. */
  function plazaSeis(n: number) {
    const fija = n % 2 === 0;
    return {
      id: 100 + n,
      codigo: `Bloque-P${n + 1}`,
      asignatura: n < 3 ? 'Mat' : 'CyR',
      aulaFija: fija ? 'A1' : (null as string | null),
      aulasCandidatas: fija ? ([] as string[]) : ['A1', 'INF1'],
      profesores: [`PROF${n}`],
      subgrupos: [`SG${n}`],
    };
  }

  /** Actividad de SEIS plazas, molde del `roundTrip_bloqueSeisPlazas` del backend. */
  const ACTIVIDAD_SEIS = {
    id: 9,
    codigo: 'Bloque',
    asignatura: null as string | null,
    duracionTramos: 1,
    repeticionesPorSemana: 1,
    patronTemporal: 'NEUTRA',
    requiereTutor: false,
    plazas: [0, 1, 2, 3, 4, 5].map(plazaSeis),
  };

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

  it('(17) en edición reconstruye UNA fila por plaza: seis filas con SU contenido', () => {
    montar(ACTIVIDAD_SEIS);
    const inst = instancia();
    expect(inst.plazas.length).toBe(6);

    // La fila 3 debe llevar la CUARTA plaza, no la primera repetida seis veces: un
    // `precargar` que cree seis filas y vuelque siempre `plazas[0]` pasaría un aserto de
    // longitud y caería aquí.
    const cuarta = inst.plazas.at(3);
    expect(cuarta.controls['asignatura'].value).toBe('CyR');
    expect(cuarta.controls['modoAula'].value).toBe('CANDIDATAS');
    expect(cuarta.controls['profesores'].value).toEqual(['PROF3']);
    expect(cuarta.controls['subgrupos'].value).toEqual(['SG3']);
    // Y la 0 conserva la suya, con la otra rama del XOR.
    expect(inst.plazas.at(0).controls['modoAula'].value).toBe('FIJA');
    expect(inst.plazas.at(0).controls['aulaFija'].value).toBe('A1');
  });

  it('(18) editar seis plazas sin tocar nada devuelve las MISMAS seis, en el mismo orden', () => {
    montar(ACTIVIDAD_SEIS);
    instancia().guardar();

    const req = http.expectOne('/api/actividades/9');
    expect(req.request.method).toBe('PUT');
    // Igualdad ESTRICTA del array entero, no un conteo: la reconciliación del backend es
    // POSICIONAL, así que reordenar las plazas no es un detalle cosmético — sobrescribiría
    // el contenido de unas plazas vivas con el de otras.
    expect((req.request.body as { plazas: unknown[] }).plazas).toEqual([
      { asignatura: 'Mat', aulaFija: 'A1', aulasCandidatas: [], profesores: ['PROF0'], subgrupos: ['SG0'] },
      { asignatura: 'Mat', aulaFija: null, aulasCandidatas: ['A1', 'INF1'], profesores: ['PROF1'], subgrupos: ['SG1'] },
      { asignatura: 'Mat', aulaFija: 'A1', aulasCandidatas: [], profesores: ['PROF2'], subgrupos: ['SG2'] },
      { asignatura: 'CyR', aulaFija: null, aulasCandidatas: ['A1', 'INF1'], profesores: ['PROF3'], subgrupos: ['SG3'] },
      { asignatura: 'CyR', aulaFija: 'A1', aulasCandidatas: [], profesores: ['PROF4'], subgrupos: ['SG4'] },
      { asignatura: 'CyR', aulaFija: null, aulasCandidatas: ['A1', 'INF1'], profesores: ['PROF5'], subgrupos: ['SG5'] },
    ]);
    req.flush(ACTIVIDAD_SEIS);
  });

  it('(19) la fila añadida nace VACÍA de la fábrica, no clonada de la anterior', async () => {
    montar(null);
    const inst = instancia();
    rellenarPlaza(0, 'CANDIDATAS');
    await anadirFila();

    // Clonar la fila anterior sería lo cómodo y estaría mal: heredaría la rama, el aula y
    // los subgrupos, y esos subgrupos heredados violarían I2 nada más nacer.
    expect(inst.plazas.length).toBe(2);
    const nueva = inst.plazas.at(1);
    expect(nueva.controls['modoAula'].value).toBe('FIJA');
    expect(nueva.controls['aulaFija'].value).toBe('');
    expect(nueva.controls['aulasCandidatas'].value).toEqual([]);
    expect(nueva.controls['profesores'].value).toEqual([]);
    expect(nueva.controls['subgrupos'].value).toEqual([]);
  });

  it('(20) TRACK: quitar la fila del medio deja el DOM alineado con el modelo', async () => {
      // ESTE es el caso que protege el `track` por control del `@for`. Con `track $index`,
      // quitar una fila intermedia deja nodos enlazados al FormGroup ya extraído del array:
      // lo que el usuario teclee en ellos no llega al modelo y el cuerpo enviado sería el
      // viejo, sin ningún error visible.
      //
      // <p>DÓNDE MIRAR, que es lo que hace o deshace este caso (medido en la campaña de
      // mutación de S110): los nodos dentro de un `@if` —el aula fija, las candidatas, los
      // <span> de error— NO sirven de testigo. Al cambiar el contexto de la fila, la rama
      // del `@if` se destruye y se recrea, y el `FormControlName` recién creado re-resuelve
      // contra la posición actual: el desalineamiento se cura solo ahí. Los únicos testigos
      // válidos son los nodos FUERA de todo `@if` enlazados por `formControlName`, cuya
      // directiva persiste con su `[formGroupName]` de siempre. Aquí, el select de
      // asignatura de la plaza. Si alguien reescribe este caso mirando solo el aula, el
      // `track $index` vuelve a pasar en verde.
      montar(null);
      const inst = instancia();
      inst.form.patchValue({ codigo: 'Bloque' });
      rellenarPlaza(0, 'FIJA', { subgrupos: ['1ºA-Completo'] });
      await anadirFila();
      rellenarPlaza(1, 'CANDIDATAS', { subgrupos: ['1ºB-Completo'] });
      await anadirFila();
      // La tercera lleva asignatura DISTINTA (el testigo del track) y va sin profesores a
      // propósito, para que su mensaje de error tenga que seguir a su fila.
      rellenarPlaza(2, 'FIJA', {
        asignatura: 'CyR',
        aulaFija: 'INF1',
        subgrupos: [],
        profesores: [],
      });
      inst.guardar(); // form inválido ⇒ markAllAsTouched, ninguna petición
      fixture.detectChanges();

      await quitarFila(1);

      const raiz = fixture.nativeElement as HTMLElement;
      const fieldsets = raiz.querySelectorAll('.actividad-form__plaza');
      expect(fieldsets.length).toBe(2);

      // TESTIGO DEL TRACK (lectura): la asignatura de cada fila superviviente. Con `track
      // $index` el segundo select seguiría enlazado a la fila BORRADA y mostraría su 'Mat'.
      const asignaturas = [...fieldsets].map(
        (fs) => fs.querySelector<HTMLSelectElement>('select[formControlName="asignatura"]')!.value,
      );
      expect(asignaturas).toEqual(['Mat', 'CyR']);

      // Lo que sí cazan los nodos del @if: que la fila borrada era la de candidatas y que
      // el aula de cada superviviente es la suya (esto es lo que mata `removeAt(0)`).
      const fijas = [...raiz.querySelectorAll<HTMLSelectElement>('.actividad-form__aula-fija')];
      expect(fijas.map((s) => s.value)).toEqual(['A1', 'INF1']);
      expect(raiz.querySelector('.actividad-form__candidatas')).toBeNull();

      // El mensaje de error viaja con SU fila: ahora es la segunda, no la tercera.
      expect(fieldsets[1].textContent).toContain('Elige al menos un profesor');
      expect(fieldsets[0].textContent).not.toContain('Elige al menos un profesor');

      // Las plazas se renumeran: sin esto, el número del legend no lo asevera nadie.
      const legends = [...fieldsets].map((fs) => fs.querySelector('legend')!.textContent!.trim());
      expect(legends).toEqual(['Plaza 1', 'Plaza 2']);

      // TESTIGO DEL TRACK (escritura), que es el síntoma caro: teclear en la fila visible
      // tiene que llegar al modelo VIVO. Con el binding rancio, esto entra en el FormGroup
      // extraído y el modelo no se entera, así que `aRequest()` enviaría el valor viejo.
      const segunda = fieldsets[1].querySelector<HTMLSelectElement>(
        'select[formControlName="asignatura"]',
      )!;
      segunda.value = 'Mat';
      segunda.dispatchEvent(new Event('change'));
      expect(inst.plazas.at(1).controls['asignatura'].value).toBe('Mat');
    });

  it('(21) MÍNIMO: con una sola fila no se puede quitar, y con dos sí', async () => {
    montar(null);
    const inst = instancia();
    const raiz = fixture.nativeElement as HTMLElement;

    const solo = raiz.querySelector<HTMLButtonElement>('.actividad-form__quitar-plaza')!;
    expect(solo.disabled).toBe(true);
    // Dos capas: el atributo es presentación, el método es la puerta real a un cuerpo sin
    // plazas (400 del backend). Invocarlo a mano tampoco debe quitar la última.
    inst.quitarPlaza(0);
    expect(inst.plazas.length).toBe(1);

    // SEGUNDA CARA, sin la cual un `[disabled]="true"` fijo pasaría igual que el código
    // correcto: al haber dos filas, ninguno de los botones sigue apagado.
    await anadirFila();
    const botones = raiz.querySelectorAll<HTMLButtonElement>('.actividad-form__quitar-plaza');
    expect(botones.length).toBe(2);
    expect([...botones].some((b) => b.disabled)).toBe(false);
  });

  it('(22) I2: el mismo subgrupo en dos filas corta el envío y NOMBRA el subgrupo', async () => {
    montar(null);
    const inst = instancia();
    inst.form.patchValue({ codigo: 'Bloque' });
    rellenarPlaza(0, 'FIJA');
    await anadirFila();
    rellenarPlaza(1, 'FIJA'); // el helper repite '1ºA-Completo' a propósito
    inst.guardar();
    fixture.detectChanges();

    // El validador del array pone `form.invalid`, así que la guarda de `guardar()` corta
    // sin comprobación aparte. El `http.verify()` del afterEach confirma que no salió nada.
    http.expectNone('/api/actividades');
    const aviso = fixture.nativeElement.querySelector('.actividad-form__error-i2');
    expect(aviso).not.toBeNull();
    // Nombrar el código es el punto: el 400 equivalente del backend lo nombra, pero hoy
    // llega mudo al navegador (D-F8.6-ii-a), así que este es el único que lo dice.
    expect(aviso.textContent).toContain('1ºA-Completo');
  });

  it('(23) DISCRIMINANTE de (22): con subgrupos distintos SÍ envía las dos plazas', async () => {
    montar(null);
    const inst = instancia();
    inst.form.patchValue({ codigo: 'Bloque' });
    rellenarPlaza(0, 'FIJA', { subgrupos: ['1ºA-Completo'] });
    await anadirFila();
    rellenarPlaza(1, 'FIJA', { subgrupos: ['1ºB-Completo'] });
    inst.guardar();

    // Sin este caso, un validador que rechazara SIEMPRE pasaría el (22) tan campante.
    const req = http.expectOne('/api/actividades');
    expect((req.request.body as { plazas: unknown[] }).plazas.length).toBe(2);
    expect(fixture.nativeElement.querySelector('.actividad-form__error-i2')).toBeNull();
    req.flush(ACTIVIDAD_FIJA);
  });

  it('(24) el validador NO normaliza: dos códigos que difieren en mayúsculas son distintos', async () => {
    montar(null);
    const inst = instancia();
    inst.form.patchValue({ codigo: 'Bloque' });
    rellenarPlaza(0, 'FIJA', { subgrupos: ['1ºA-Completo'] });
    await anadirFila();
    rellenarPlaza(1, 'FIJA', { subgrupos: ['1ºa-completo'] });
    inst.guardar();

    // Congela la equivalencia con el backend, que cruza con `HashSet<String>` sin
    // normalizar: para él son dos subgrupos distintos y acepta el cuerpo. Añadir aquí un
    // `.toLowerCase()` «por robustez» haría que el formulario rechazara algo que la API
    // acepta —la familia de D-plaza-sin-subgrupos—, y ESTE es el único caso que cae.
    const req = http.expectOne('/api/actividades');
    expect((req.request.body as { plazas: unknown[] }).plazas.length).toBe(2);
    req.flush(ACTIVIDAD_FIJA);
  });

  it('(25) el aviso de I2 NO se pinta cuando lo que falta es otra cosa', () => {
    montar(null);
    const inst = instancia();
    inst.form.patchValue({ codigo: 'Mat-1ºA' });
    rellenarPlaza(0, 'FIJA', { profesores: [] });
    inst.guardar();
    fixture.detectChanges();

    // Los errores de las filas PROPAGAN al array, así que condicionar el aviso a
    // `plazas.invalid` en vez de al error concreto haría leer «subgrupo repetido» a quien
    // solo ha olvidado el profesor.
    expect(fixture.nativeElement.querySelector('.actividad-form__error-i2')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Elige al menos un profesor');
  });

  it('(26) el XOR es POR FILA: cambiar de rama en una no toca el aula de la otra', async () => {
    montar(null);
    const inst = instancia();
    rellenarPlaza(0, 'FIJA', { aulaFija: 'A1', subgrupos: ['1ºA-Completo'] });
    await anadirFila();
    rellenarPlaza(1, 'FIJA', { aulaFija: 'INF1', subgrupos: ['1ºB-Completo'] });

    inst.plazas.at(1).controls['modoAula'].setValue('CANDIDATAS');

    // Con UNA sola fila —como en el (6) heredado— un `aplicarModo` que operase sobre
    // `this.plazas.at(0)`, o una suscripción creada fuera de la fábrica y compartida,
    // pasaría todos los demás casos. Aquí se ve: la limpieza va a su fila y solo a la suya.
    expect(inst.plazas.at(1).controls['aulaFija'].value).toBe('');
    expect(inst.plazas.at(0).controls['aulaFija'].value).toBe('A1');
    expect(inst.plazas.at(0).controls['modoAula'].value).toBe('FIJA');
  });

  it('(27) tras quitar una fila, el cuerpo lleva las SUPERVIVIENTES y en su orden', async () => {
    montar(null);
    const inst = instancia();
    inst.form.patchValue({ codigo: 'Bloque' });
    rellenarPlaza(0, 'FIJA', { aulaFija: 'A1', profesores: ['MATA'], subgrupos: ['1ºA-Completo'] });
    await anadirFila();
    rellenarPlaza(1, 'FIJA', { aulaFija: 'INF1', profesores: ['MAT6'], subgrupos: ['1ºB-Completo'] });
    await anadirFila();
    rellenarPlaza(2, 'CANDIDATAS', { profesores: ['MAT6'], subgrupos: [] });

    await quitarFila(1);
    inst.guardar();

    const req = http.expectOne('/api/actividades');
    // Igualdad estricta, no un conteo: con reconciliación posicional, ESTE cuerpo decide
    // qué plaza viva se sobrescribe con qué contenido. Un conteo de 2 pasaría aunque
    // hubieran sobrevivido las dos equivocadas o en el orden cambiado.
    expect((req.request.body as { plazas: unknown[] }).plazas).toEqual([
      { asignatura: 'Mat', aulaFija: 'A1', aulasCandidatas: [], profesores: ['MATA'], subgrupos: ['1ºA-Completo'] },
      { asignatura: 'Mat', aulaFija: null, aulasCandidatas: ['A1', 'INF1'], profesores: ['MAT6'], subgrupos: [] },
    ]);
    req.flush(ACTIVIDAD_FIJA);
  });

  it('(28) quitar la fila culpable LIMPIA el error de I2', async () => {
    montar(null);
    const inst = instancia();
    inst.form.patchValue({ codigo: 'Bloque' });
    rellenarPlaza(0, 'FIJA');
    await anadirFila();
    rellenarPlaza(1, 'FIJA'); // repite subgrupo ⇒ I2 en rojo
    expect(inst.form.invalid).toBe(true);

    await quitarFila(1);
    fixture.detectChanges();

    // Un validador reimplementado como suscripción a `valueChanges` con `setErrors` a mano
    // —el «arreglo» típico si alguien quiere ahorrar recorridos— pasaría (22) y (23) y
    // fallaría aquí: dejaría el formulario bloqueado con un error fantasma de una fila que
    // ya no existe. Como validador de array, `removeAt` revalida solo.
    expect(fixture.nativeElement.querySelector('.actividad-form__error-i2')).toBeNull();
    inst.guardar();
    http.expectOne('/api/actividades').flush(ACTIVIDAD_FIJA);
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
    rellenarPlaza(0, 'FIJA');
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
    rellenarPlaza(0, 'CANDIDATAS');
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
    rellenarPlaza(0, 'FIJA', { profesores: [] });
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
    rellenarPlaza(0, 'FIJA', { subgrupos: [] });
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
    rellenarPlaza(0, 'FIJA');
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
    rellenarPlaza(0, 'FIJA');
    inst.guardar();

    http.expectOne('/api/actividades').flush(ACTIVIDAD_FIJA);
    await fixture.whenStable();
    expect(ref.close).toHaveBeenCalledWith(true);
  });
});
