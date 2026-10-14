import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { ActividadForm, plazasConSubgrupoRepetido } from './actividad-form';

/**
 * Congela el formulario de actividad de UNA plaza. Hereda del molde de `grupo-form` el
 * `montar()` que hace red y la traducción del 400, y del de `subgrupo-form` el
 * multiselect; los casos propios son el XOR de aula (4)(5)(6), el I7 en cliente (7), la
 * regla de subgrupos de una CLASE (8), la derivación de `modoAula` en edición (9), el
 * cuerpo del PUT (10), el 409 por camino distinto del 400 (12) y el tipo de actividad
 * (f1)-(f7, S201).
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
      controls: Record<string, { setValue: (v: unknown) => void; value: unknown }>;
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
    modo: 'FIJA' | 'CANDIDATAS' | 'NINGUNA',
    extra: Partial<Record<string, unknown>> = {},
  ): void {
    const fila = instancia().plazas.at(indice);
    fila.controls['asignatura'].setValue('Mat');
    fila.controls['modoAula'].setValue(modo);
    if (modo === 'FIJA') {
      fila.controls['aulaFija'].setValue('A1');
    } else if (modo === 'CANDIDATAS') {
      fila.controls['aulasCandidatas'].setValue(['A1', 'INF1']);
    }
    fila.controls['profesores'].setValue(['MATA']);
    fila.controls['subgrupos'].setValue(['1ºA-Completo']);
    for (const [k, v] of Object.entries(extra)) {
      fila.controls[k].setValue(v);
    }
  }

  /**
   * Añade una fila POR CLICK en la pestaña «+», no llamando al método. En zoneless, mutar un
   * `FormArray` no marca la vista sucia —no es escritura de señal—, así que solo el listener
   * del botón programa detección de cambios. Llamar al método y forzar con `detectChanges()`
   * lanza NG0100 (el `[formGroupName]="$index"` cambia después de comprobarse) y pondría rojo
   * un código correcto; llamar al método y esperar `whenStable()` a secas no repinta y no
   * discrimina nada. El click es el camino real y el único harness limpio.
   */
  async function anadirFila(): Promise<void> {
    const boton = fixture.nativeElement.querySelector('.plazas__pestana--nueva') as HTMLButtonElement;
    boton.click();
    await fixture.whenStable();
  }

  /** Abre la pestaña de la plaza indicada (S230: solo se pinta la plaza activa). */
  async function abrirPestana(indice: number): Promise<void> {
    pestana(indice).click();
    await fixture.whenStable();
  }

  /** Quita la fila indicada: abre SU pestaña y pulsa «Quitar esta plaza», que vive dentro. */
  async function quitarFila(indice: number): Promise<void> {
    await abrirPestana(indice);
    (fixture.nativeElement.querySelector('.actividad-form__quitar-plaza') as HTMLButtonElement).click();
    await fixture.whenStable();
  }

  function pestana(indice: number): HTMLButtonElement {
    return fixture.nativeElement.querySelector(`.plazas__pestana[data-plaza="${indice}"]`) as HTMLButtonElement;
  }

  /** Índice de la pestaña abierta, o null si no hay ninguna. */
  function pestanaActiva(): number | null {
    const activa = fixture.nativeElement.querySelector('.plazas__pestana[aria-selected="true"]') as HTMLElement | null;
    return activa === null ? null : Number(activa.dataset['plaza']);
  }

  /** Índices de las pestañas que llevan la marca indicada (`i2` o `invalida`). */
  function pestanasCon(marca: 'i2' | 'invalida'): number[] {
    return Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLElement>('.plazas__pestana[data-plaza]'),
    )
      .filter((b) => b.querySelector(`[data-${marca}]`) !== null)
      .map((b) => Number(b.dataset['plaza']));
  }

  /** Códigos de un lado de la lista doble `nombre` de la plaza abierta, en el orden en que se ven. */
  function enLista(nombre: string, lado: 'elegidos' | 'disponibles'): string[] {
    return Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLElement>(
        `[data-lista="${nombre}"] [data-lado="${lado}"] li[data-codigo]`,
      ),
    ).map((li) => li.dataset['codigo']!);
  }

  /** Pasa a Elegidos esas filas de la lista doble `nombre`: marcarlas y «‹ Añadir». */
  async function elegirEnLista(nombre: string, ...codigos: string[]): Promise<void> {
    const raiz = fixture.nativeElement as HTMLElement;
    for (const c of codigos) {
      raiz.querySelector<HTMLElement>(`[data-lista="${nombre}"] [data-lado="disponibles"] li[data-codigo="${c}"]`)!.click();
      await fixture.whenStable();
    }
    raiz.querySelector<HTMLButtonElement>(`[data-lista="${nombre}"] .lista-doble__anadir`)!.click();
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
    tipo: 'CLASE',
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
    tipo: 'CLASE',
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

  /** Una REUNIÓN sin aula (S201): ni aula fija ni candidatas, sin subgrupos y sin tutor. */
  const ACTIVIDAD_REUNION = {
    ...ACTIVIDAD_FIJA,
    id: 20,
    codigo: 'REU-Dpto',
    tipo: 'REUNION',
    requiereTutor: false,
    plazas: [
      {
        id: 21,
        codigo: 'REU-Dpto-P1',
        asignatura: 'Mat',
        aulaFija: null as string | null,
        aulasCandidatas: [] as string[],
        profesores: ['MATA', 'MAT6'],
        subgrupos: [] as string[],
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
      // <p>Desde S230 solo se pinta la plaza de la pestaña activa, y quitar la del medio deja
      // activa la vecina EN EL MISMO ÍNDICE: la vista de ese índice no se destruye, y con
      // `track $index` su `[formGroupName]` seguiría resuelto contra la fila borrada. El
      // testigo es el select de asignatura de la plaza, fuera de todo `@if` del panel (los
      // nodos de un `@if` —el aula, los errores— se recrean y se curan solos, medido en S110).
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
      // Las pestañas se renumeran y nombran su asignatura.
      const rotulos = [...raiz.querySelectorAll('.plazas__pestana[data-plaza]')].map((b) => b.textContent!.trim());
      expect(rotulos).toEqual(['1 · Mat', '2 · CyR']);
      expect(pestanaActiva()).toBe(1);
      expect(raiz.querySelectorAll('.actividad-form__plaza').length).toBe(1);

      // TESTIGO DEL TRACK (lectura): la plaza visible es la que era la tercera.
      const asignatura = (): HTMLSelectElement =>
        raiz.querySelector<HTMLSelectElement>('.actividad-form__plaza select[formControlName="asignatura"]')!;
      expect(asignatura().value).toBe('CyR');
      expect(raiz.querySelector<HTMLSelectElement>('.actividad-form__aula-fija')!.value).toBe('INF1');
      expect(raiz.querySelector('.actividad-form__candidatas')).toBeNull();
      // El mensaje de error viaja con SU fila: ahora es la segunda.
      expect(raiz.querySelector('.actividad-form__plaza')!.textContent).toContain('Elige al menos un profesor');

      await abrirPestana(0);
      expect(asignatura().value).toBe('Mat');
      expect(raiz.querySelector<HTMLSelectElement>('.actividad-form__aula-fija')!.value).toBe('A1');
      expect(raiz.querySelector('.actividad-form__plaza')!.textContent).not.toContain('Elige al menos un profesor');

      // TESTIGO DEL TRACK (escritura), que es el síntoma caro: teclear en la fila visible
      // tiene que llegar al modelo VIVO. Con el binding rancio, esto entra en el FormGroup
      // extraído y el modelo no se entera, así que `aRequest()` enviaría el valor viejo.
      await abrirPestana(1);
      asignatura().value = 'Mat';
      asignatura().dispatchEvent(new Event('change'));
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
    // correcto: al haber dos filas, el botón de cada pestaña está encendido.
    await anadirFila();
    expect(raiz.querySelectorAll('.actividad-form__quitar-plaza').length).toBe(1);
    expect(raiz.querySelector<HTMLButtonElement>('.actividad-form__quitar-plaza')!.disabled).toBe(false);
    await abrirPestana(0);
    expect(raiz.querySelector<HTMLButtonElement>('.actividad-form__quitar-plaza')!.disabled).toBe(false);
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
    // Con subgrupo: desde S201 una CLASE sin subgrupos no se envía.
    rellenarPlaza(2, 'CANDIDATAS', { profesores: ['MAT6'], subgrupos: ['1ºC-Completo'] });

    await quitarFila(1);
    inst.guardar();

    const req = http.expectOne('/api/actividades');
    // Igualdad estricta, no un conteo: con reconciliación posicional, ESTE cuerpo decide
    // qué plaza viva se sobrescribe con qué contenido. Un conteo de 2 pasaría aunque
    // hubieran sobrevivido las dos equivocadas o en el orden cambiado.
    expect((req.request.body as { plazas: unknown[] }).plazas).toEqual([
      { asignatura: 'Mat', aulaFija: 'A1', aulasCandidatas: [], profesores: ['MATA'], subgrupos: ['1ºA-Completo'] },
      { asignatura: 'Mat', aulaFija: null, aulasCandidatas: ['A1', 'INF1'], profesores: ['MAT6'], subgrupos: ['1ºC-Completo'] },
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

    // Listas dobles: sin placeholder, todos disponibles de arranque.
    expect(enLista('profesores', 'disponibles')).toEqual(['MAT6', 'MATA']);
    expect(enLista('subgrupos', 'disponibles')).toEqual(['1ºA-Completo', '1ºB-Completo']);
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
      tipo: 'CLASE',
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

    // Y el DOM sigue al modo: en FIJA se pinta el select único, no la lista doble.
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

  it('(8) f2: una CLASE con CERO subgrupos no se envía', () => {
    montar(null);
    const inst = instancia();
    inst.form.patchValue({ codigo: 'Mat-1ºA' });
    rellenarPlaza(0, 'FIJA', { subgrupos: [] });
    inst.guardar();

    // El formulario sigue reflejando el contrato, y el contrato ha cambiado (S201): una
    // CLASE exige al menos un subgrupo por plaza (salda D-plaza-sin-subgrupos). Si alguien
    // le quita `arrayNoVacio` a los subgrupos de una clase, este caso se pone rojo.
    expect(inst.form.invalid).toBe(true);
    http.expectNone('/api/actividades');
  });

  // ─────────────────────────────────────── tipo de actividad (S201)

  it('(f1) un alta sin tocar el tipo viaja como CLASE', () => {
    montar(null);
    const inst = instancia();
    inst.form.patchValue({ codigo: 'Mat-1ºA' });
    rellenarPlaza(0, 'FIJA');
    inst.guardar();

    const req = http.expectOne('/api/actividades');
    expect((req.request.body as { tipo: string }).tipo).toBe('CLASE');
    req.flush(ACTIVIDAD_FIJA);
  });

  it('(f3) una REUNION sin aula ni subgrupos viaja con aulaFija null y listas vacías', () => {
    montar(null);
    const inst = instancia();
    inst.form.patchValue({ codigo: 'REU-Dpto', tipo: 'REUNION' });
    rellenarPlaza(0, 'NINGUNA', { subgrupos: [] });
    inst.guardar();

    const req = http.expectOne('/api/actividades');
    const cuerpo = req.request.body as { tipo: string; plazas: unknown[] };
    expect(cuerpo.tipo).toBe('REUNION');
    expect(cuerpo.plazas).toEqual([
      { asignatura: 'Mat', aulaFija: null, aulasCandidatas: [], profesores: ['MATA'], subgrupos: [] },
    ]);
    req.flush(ACTIVIDAD_REUNION);
  });

  /** Texto de la etiqueta del radio NINGUNA de la primera plaza, o null si no se pinta. */
  function etiquetaNinguna(): string | null {
    const radio = (fixture.nativeElement as HTMLElement).querySelector(
      'input[type="radio"][value="NINGUNA"]',
    );
    return radio === null ? null : (radio.closest('label')?.textContent ?? '').trim();
  }

  // (f4) y (f5) reescritos en S207 (C-deduccion-aulas, E): bajas declaradas. Antes «Sin aula»
  // solo existía fuera de una CLASE y pasar a CLASE devolvía la plaza a FIJA.
  it('(f4) «sin aula» se ofrece en todos los tipos: «La elige el generador» en una CLASE y «Sin aula» fuera', () => {
    montar(null);
    const inst = instancia();
    expect(etiquetaNinguna()).toBe('La elige el generador');

    inst.form.patchValue({ tipo: 'REUNION' });
    fixture.detectChanges();
    expect(etiquetaNinguna()).toBe('Sin aula');

    inst.form.patchValue({ tipo: 'FUNCION' });
    fixture.detectChanges();
    expect(etiquetaNinguna()).toBe('Sin aula');
  });

  it('(f5) cambiar el tipo conserva el modo «sin aula» y solo cambia la etiqueta', () => {
    montar(null);
    const inst = instancia();
    inst.form.patchValue({ tipo: 'REUNION' });
    inst.plazas.at(0).controls['modoAula'].setValue('NINGUNA');

    inst.form.patchValue({ tipo: 'CLASE' });
    fixture.detectChanges();
    expect(inst.plazas.at(0).controls['modoAula'].value).toBe('NINGUNA');
    expect(etiquetaNinguna()).toBe('La elige el generador');

    inst.form.patchValue({ tipo: 'REUNION' });
    fixture.detectChanges();
    expect(inst.plazas.at(0).controls['modoAula'].value).toBe('NINGUNA');
    expect(etiquetaNinguna()).toBe('Sin aula');
  });

  it('(f8) una CLASE que deja el aula al generador viaja sin aula fija ni candidatas y con sus subgrupos', () => {
    montar(null);
    const inst = instancia();
    inst.form.patchValue({ codigo: 'Mat-1ºA' });
    rellenarPlaza(0, 'NINGUNA');
    inst.guardar();

    const req = http.expectOne('/api/actividades');
    const cuerpo = req.request.body as { tipo: string; plazas: unknown[] };
    expect(cuerpo.tipo).toBe('CLASE');
    expect(cuerpo.plazas).toEqual([
      { asignatura: 'Mat', aulaFija: null, aulasCandidatas: [], profesores: ['MATA'], subgrupos: ['1ºA-Completo'] },
    ]);
    req.flush(ACTIVIDAD_FIJA);
  });

  it('(f9) editar una CLASE guardada sin aula precarga «La elige el generador»', () => {
    const claseSinAula = {
      ...ACTIVIDAD_FIJA,
      plazas: [{ ...ACTIVIDAD_FIJA.plazas[0], aulaFija: null as string | null, aulasCandidatas: [] as string[] }],
    };
    montar(claseSinAula);
    const inst = instancia();
    fixture.detectChanges();

    expect(inst.form.controls['tipo'].value).toBe('CLASE');
    expect(inst.plazas.at(0).controls['modoAula'].value).toBe('NINGUNA');
    expect(etiquetaNinguna()).toBe('La elige el generador');
    const radio = (fixture.nativeElement as HTMLElement).querySelector(
      'input[type="radio"][value="NINGUNA"]',
    ) as HTMLInputElement;
    expect(radio.checked).toBe(true);
  });

  it('(f11) pasar por «La elige el generador» LIMPIA el aula fija: al volver a «aula fija» no reaparece', () => {
    montar(null);
    const inst = instancia();
    rellenarPlaza(0, 'FIJA'); // aula fija A1
    const fila = inst.plazas.at(0);

    fila.controls['modoAula'].setValue('NINGUNA');
    fila.controls['modoAula'].setValue('FIJA');

    expect(fila.controls['aulaFija'].value).toBe('');
  });

  it('(f10) una plaza en «aula fija» con el selector vacío deja el formulario inválido y no se envía', () => {
    montar(null);
    const inst = instancia();
    inst.form.patchValue({ codigo: 'Mat-1ºA' });
    rellenarPlaza(0, 'FIJA', { aulaFija: '' });
    inst.guardar();

    // Todo lo demás de la plaza es válido: lo único que falta es el aula fija de su rama.
    expect(inst.form.invalid).toBe(true);
    http.expectNone('/api/actividades');
  });

  it('(f6) de CLASE a REUNION: el envío suelta el tutor y los subgrupos', () => {
    montar(null);
    const inst = instancia();
    inst.form.patchValue({ codigo: 'Mat-1ºA', requiereTutor: true });
    rellenarPlaza(0, 'FIJA'); // con subgrupo 1ºA-Completo
    inst.form.patchValue({ tipo: 'REUNION' });
    inst.plazas.at(0).controls['modoAula'].setValue('NINGUNA');
    inst.guardar();

    const req = http.expectOne('/api/actividades');
    const cuerpo = req.request.body as { requiereTutor: boolean; plazas: { subgrupos: string[] }[] };
    expect(cuerpo.requiereTutor).toBe(false);
    expect(cuerpo.plazas[0].subgrupos).toEqual([]);
    req.flush(ACTIVIDAD_REUNION);
  });

  it('(f7) editar una REUNION sin aula precarga el tipo y NINGUNA, y el PUT va sin aula', () => {
    montar(ACTIVIDAD_REUNION);
    const inst = instancia();

    expect(inst.form.controls['tipo'].value).toBe('REUNION');
    expect(inst.plazas.at(0).controls['modoAula'].value).toBe('NINGUNA');
    inst.guardar();

    const req = http.expectOne('/api/actividades/20');
    expect(req.request.method).toBe('PUT');
    const plaza = (req.request.body as { plazas: { aulaFija: unknown; aulasCandidatas: unknown }[] })
      .plazas[0];
    expect(plaza.aulaFija).toBeNull();
    expect(plaza.aulasCandidatas).toEqual([]);
    req.flush(ACTIVIDAD_REUNION);
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
      tipo: 'CLASE',
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

  /**
   * (S226, condición 8 de O-interfaz) Escribe en el campo Código como lo haría el usuario: por
   * el DOM, con su evento `input`, para que el cambio pase por `valueChanges` y no por el TS.
   */
  async function escribirCodigo(texto: string): Promise<void> {
    const input = fixture.nativeElement.querySelector('input[formcontrolname="codigo"]') as HTMLInputElement;
    input.value = texto;
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('(29) tras un 400 al guardar, editar un campo borra el mensaje del servidor', async () => {
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
    expect(fixture.nativeElement.querySelector('.actividad-form__error-servidor')).not.toBeNull();

    await escribirCodigo('Mat-1ºB');

    // El mensaje hablaba de lo que había antes de corregir: ya no se pinta.
    expect(fixture.nativeElement.querySelector('.actividad-form__error-servidor')).toBeNull();
  });

  it('(30) un fallo de CARGA no se borra al editar un campo: editar no lo arregla', async () => {
    montar(null, true);
    await fixture.whenStable();
    fixture.detectChanges();

    await escribirCodigo('Mat-1ºA');

    const err = fixture.nativeElement.querySelector('.actividad-form__error-servidor');
    expect(err).not.toBeNull();
    expect(err.textContent).toContain('No se pudieron cargar las aulas');
  });

  it('(31) tras un 409, editar un campo borra el aviso del conflicto', async () => {
    montar(ACTIVIDAD_FIJA);
    instancia().guardar();
    http.expectOne('/api/actividades/7').flush(
      { message: 'No se puede editar: referenciada por 1 sesion(es), 1 aula(s) bloqueada(s)' },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.actividad-form__conflicto')).not.toBeNull();

    await escribirCodigo('ACT-OTRA');

    expect(fixture.nativeElement.querySelector('.actividad-form__conflicto')).toBeNull();
  });

  it('(15) la lista doble vuelca al control de SU plaza lo que el usuario elige en el DOM', async () => {
    montar(null);
    await anadirFila();
    await elegirEnLista('profesores', 'MAT6', 'MATA');

    // Sin el enlace por formControlName el control seguiría vacío y el I7 cortaría un
    // formulario que el usuario sí rellenó. Va a la plaza ABIERTA (la 2), no a la primera,
    // y en el orden del catálogo, como lo daba el select (enmienda 2).
    expect(instancia().plazas.at(1).controls['profesores'].value).toEqual(['MATA', 'MAT6']);
    expect(instancia().plazas.at(0).controls['profesores'].value).toEqual([]);
  });

  it('(16) un guardado correcto cierra el diálogo con el id que devuelve el servidor', async () => {
    montar(null);
    const inst = instancia();
    inst.form.patchValue({ codigo: 'Mat-1ºA' });
    rellenarPlaza(0, 'FIJA');
    inst.guardar();

    http.expectOne('/api/actividades').flush(ACTIVIDAD_FIJA);
    await fixture.whenStable();
    expect(ref.close).toHaveBeenCalledWith({ id: 7 });
  });

  it('(16b) en la edición también cierra con el id de la actividad guardada', async () => {
    montar(ACTIVIDAD_FIJA);
    instancia().guardar();

    http.expectOne('/api/actividades/7').flush(ACTIVIDAD_FIJA);
    await fixture.whenStable();
    expect(ref.close).toHaveBeenCalledWith({ id: 7 });
  });

  /**
   * Filtro de los seis selectores (S185, condición 5 de O-pre-demo). Listas de DOCE: con 10 o
   * menos el filtro no se pinta. «x1» casa con x10, x11 y x12 y NO con x05, que es la elegida:
   * la opción elegida tiene que seguir en el select aunque la consulta no la alcance. En las
   * tres listas dobles (S230) el filtro recorta Disponibles y la elegida está en Elegidos.
   */
  describe('filtro de selectores (S185)', () => {
    const doce = (prefijo: string): string[] =>
      Array.from({ length: 12 }, (_, i) => `${prefijo}${String(i + 1).padStart(2, '0')}`);
    const ASIGNATURAS_12 = doce('M').map((codigo, i) => ({ id: i + 1, codigo, nombreCompleto: codigo }));
    const PROFESORES_12 = doce('P').map((codigo, i) => ({ id: i + 1, codigo, nombreCompleto: codigo }));
    const AULAS_12 = doce('A').map((codigo, i) => ({
      id: i + 1, codigo, tipo: 'ORDINARIA', capacidad: null, edificio: null, planta: null, sector: null,
    }));
    const SUBGRUPOS_12 = doce('S').map((codigo, i) => ({ id: i + 1, codigo, grupos: ['1ºA'] }));

    /** Como `montar(null)`, con las cuatro listas de doce. */
    function montarLargo(): void {
      ref = { close: vi.fn() };
      TestBed.configureTestingModule({
        imports: [ActividadForm],
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: DialogRef, useValue: ref },
          { provide: DIALOG_DATA, useValue: null },
        ],
      });
      fixture = TestBed.createComponent(ActividadForm);
      http = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
      http.expectOne('/api/asignaturas').flush(ASIGNATURAS_12);
      http.expectOne('/api/profesores').flush(PROFESORES_12);
      http.expectOne('/api/aulas').flush(AULAS_12);
      http.expectOne('/api/subgrupos').flush(SUBGRUPOS_12);
      fixture.detectChanges();
    }

    const raiz = (): HTMLElement => fixture.nativeElement as HTMLElement;
    const sel = (css: string): HTMLSelectElement => raiz().querySelector<HTMLSelectElement>(css)!;
    const campo = (css: string): HTMLInputElement | null => raiz().querySelector<HTMLInputElement>(css);
    /** Valores de las opciones del select, sin las fijas (value ''). */
    const valores = (s: HTMLSelectElement): string[] =>
      Array.from(s.options).map((o) => o.value).filter((v) => v !== '');
    const valorRaiz = (): unknown =>
      (fixture.componentInstance as unknown as { form: { controls: Record<string, { value: unknown }> } })
        .form.controls['asignatura'].value;
    const valorPlaza = (control: string): unknown => instancia().plazas.at(0).controls[control].value;

    async function escribir(el: HTMLInputElement | null, consulta: string): Promise<void> {
      expect(el).not.toBeNull();
      el!.value = consulta;
      el!.dispatchEvent(new Event('input'));
      await fixture.whenStable();
    }

    async function elegirSimple(s: HTMLSelectElement, codigo: string): Promise<void> {
      s.value = codigo;
      s.dispatchEvent(new Event('change'));
      await fixture.whenStable();
    }

    const filtroLista = (nombre: string): HTMLInputElement | null =>
      campo(`[data-lista="${nombre}"] .filtro-opciones__campo`);

    const ASIG_RAIZ = '.actividad-form__fila--1 select[formControlName="asignatura"]';
    const ASIG_PLAZA = '.actividad-form__plaza select[formControlName="asignatura"]';

    it('asignatura: filtrar conserva visible la elegida y no cambia el valor', async () => {
      montarLargo();
      await elegirSimple(sel(ASIG_RAIZ), 'M05');
      await escribir(campo('.actividad-form__fila--1 input[aria-label="Filtrar asignaturas"]'), 'm1');

      expect(valores(sel(ASIG_RAIZ))).toEqual(['M05', 'M10', 'M11', 'M12']);
      expect(valorRaiz()).toBe('M05');
      expect(sel(ASIG_RAIZ).value).toBe('M05');
    });

    it('asignatura de plaza: filtrar conserva visible la elegida y no cambia el valor', async () => {
      montarLargo();
      await elegirSimple(sel(ASIG_PLAZA), 'M05');
      await escribir(campo('.actividad-form__plaza input[aria-label="Filtrar asignaturas"]'), 'm1');

      expect(valores(sel(ASIG_PLAZA))).toEqual(['M05', 'M10', 'M11', 'M12']);
      expect(valorPlaza('asignatura')).toBe('M05');
      expect(sel(ASIG_PLAZA).value).toBe('M05');
    });

    it('aula fija: filtrar conserva visible la elegida y no cambia el valor', async () => {
      montarLargo();
      const fija = (): HTMLSelectElement => sel('select[formControlName="aulaFija"]');
      await elegirSimple(fija(), 'A05');
      await escribir(campo('input[aria-label="Filtrar aulas"]'), 'a1');

      expect(valores(fija())).toEqual(['A05', 'A10', 'A11', 'A12']);
      expect(valorPlaza('aulaFija')).toBe('A05');
      expect(fija().value).toBe('A05');
    });

    it('aulas candidatas: filtrar conserva visible la elegida y no cambia el valor', async () => {
      montarLargo();
      raiz().querySelector<HTMLInputElement>('input[type="radio"][value="CANDIDATAS"]')!.click();
      await fixture.whenStable();
      await elegirEnLista('aulas', 'A05');
      await escribir(filtroLista('aulas'), 'a1');

      expect(enLista('aulas', 'disponibles')).toEqual(['A10', 'A11', 'A12']);
      expect(enLista('aulas', 'elegidos')).toEqual(['A05']);
      expect(valorPlaza('aulasCandidatas')).toEqual(['A05']);
    });

    it('profesores: filtrar conserva visible la elegida y no cambia el valor', async () => {
      montarLargo();
      await elegirEnLista('profesores', 'P05');
      await escribir(filtroLista('profesores'), 'p1');

      expect(enLista('profesores', 'disponibles')).toEqual(['P10', 'P11', 'P12']);
      expect(enLista('profesores', 'elegidos')).toEqual(['P05']);
      expect(valorPlaza('profesores')).toEqual(['P05']);
    });

    it('subgrupos: filtrar conserva visible la elegida y no cambia el valor', async () => {
      montarLargo();
      await elegirEnLista('subgrupos', 'S05');
      await escribir(filtroLista('subgrupos'), 's1');

      expect(enLista('subgrupos', 'disponibles')).toEqual(['S10', 'S11', 'S12']);
      expect(enLista('subgrupos', 'elegidos')).toEqual(['S05']);
      expect(valorPlaza('subgrupos')).toEqual(['S05']);
    });

    it('subgrupos: elegir otra con el filtro puesto conserva la primera', async () => {
      montarLargo();
      await elegirEnLista('subgrupos', 'S05');
      await escribir(filtroLista('subgrupos'), 's1');

      // Como el usuario: con el filtro puesto, pasa S10 sin tocar lo ya elegido.
      await elegirEnLista('subgrupos', 'S10');

      expect(new Set(valorPlaza('subgrupos') as string[])).toEqual(new Set(['S05', 'S10']));
    });
  });

  /**
   * Profesores de la plaza con código Y nombre (S227, condición 10 de O-interfaz). ONCE
   * profesores para que el filtro se pinte (más de UMBRAL_FILTRO); los nueve de relleno se
   * llaman «Ruiz», que no contiene «Pérez», igual que LEN2.
   */
  describe('profesores con código y nombre (S227)', () => {
    const PROFESORES_NOMBRE = [
      { id: 1, codigo: 'BYG1', nombreCompleto: 'Pérez, Ana' },
      { id: 2, codigo: 'LEN2', nombreCompleto: 'Gómez, Luis' },
      { id: 3, codigo: 'SIN1', nombreCompleto: '' },
      ...Array.from({ length: 8 }, (_, i) => ({ id: i + 4, codigo: `R0${i + 1}`, nombreCompleto: 'Ruiz' })),
    ];

    function montarConNombres(): void {
      ref = { close: vi.fn() };
      TestBed.configureTestingModule({
        imports: [ActividadForm],
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: DialogRef, useValue: ref },
          { provide: DIALOG_DATA, useValue: null },
        ],
      });
      fixture = TestBed.createComponent(ActividadForm);
      http = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
      http.expectOne('/api/asignaturas').flush(ASIGNATURAS);
      http.expectOne('/api/profesores').flush(PROFESORES_NOMBRE);
      http.expectOne('/api/aulas').flush(AULAS);
      http.expectOne('/api/subgrupos').flush(SUBGRUPOS);
      fixture.detectChanges();
    }

    /** La fila de un profesor en la lista doble de la plaza (Disponibles, de arranque). */
    const opcion = (codigo: string): HTMLElement | undefined =>
      (fixture.nativeElement as HTMLElement).querySelector<HTMLElement>(
        `[data-lista="profesores"] li[data-codigo="${codigo}"]`,
      ) ?? undefined;

    it('T2a la opción de un profesor dice «código — nombre»', () => {
      montarConNombres();
      expect(opcion('BYG1')!.textContent!.trim()).toBe('BYG1 — Pérez, Ana');
    });

    it('T2b el filtro de profesores encuentra por el nombre', async () => {
      montarConNombres();
      const caja = (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>(
        '[data-lista="profesores"] .filtro-opciones__campo',
      )!;
      caja.value = 'Pérez';
      caja.dispatchEvent(new Event('input'));
      await fixture.whenStable();

      expect(opcion('BYG1')).toBeDefined();
      expect(opcion('LEN2')).toBeUndefined();
    });

    it('T2c un profesor sin nombre se muestra solo con su código', () => {
      montarConNombres();
      expect(opcion('SIN1')!.textContent!.trim()).toBe('SIN1');
    });
  });

  // ─────────────────────── S230: tres filas y una pestaña por plaza (C-editor-plazas)

  /** Una plaza válida con un subgrupo dado, para las plazas de I2 y de C6. */
  function plazaCon(subgrupo: string, extra: Partial<Record<string, unknown>> = {}) {
    return {
      id: 300,
      codigo: 'X',
      asignatura: 'Mat',
      aulaFija: 'A1',
      aulasCandidatas: [] as string[],
      profesores: ['MATA'],
      subgrupos: [subgrupo],
      ...extra,
    };
  }
  const TRES_PLAZAS = {
    ...ACTIVIDAD_FIJA,
    id: 30,
    codigo: 'Tres',
    asignatura: null as string | null,
    plazas: [
      plazaCon('1ºA-Completo'),
      plazaCon('1ºB-Completo', { asignatura: 'CyR' }),
      // Aula distinta: es la que distingue la tercera de la primera cuando queda abierta.
      plazaCon('1ºC-Completo', { aulaFija: 'INF1' }),
    ],
  };

  it('P1 tres filas antes de las plazas, con los campos de la maqueta en cada una', () => {
    montar(null);
    const form = fixture.nativeElement.querySelector('form') as HTMLElement;
    const filas = [...form.querySelectorAll<HTMLElement>(':scope > [data-fila]')].map((f) => f.dataset['fila']);
    expect(filas).toEqual(['1', '2', '3']);

    const fila = (n: string): HTMLElement => form.querySelector<HTMLElement>(`:scope > [data-fila="${n}"]`)!;
    const controles = (n: string): string[] =>
      [...fila(n).querySelectorAll('[formcontrolname]')].map((c) => c.getAttribute('formcontrolname')!);
    expect(controles('1')).toEqual(['codigo', 'tipo', 'asignatura']);
    expect(controles('2')).toEqual(['duracionTramos', 'repeticionesPorSemana', 'patronTemporal', 'requiereTutor']);
    expect(fila('3').querySelector('.plazas__pestanas')).not.toBeNull();
  });

  it('P2 una pestaña por plaza, «n · asignatura», y solo se pinta la abierta', async () => {
    montar(ACTIVIDAD_SEIS);
    const raiz = fixture.nativeElement as HTMLElement;
    const rotulos = [...raiz.querySelectorAll('.plazas__pestana[data-plaza]')].map((b) => b.textContent!.trim());
    expect(rotulos).toEqual(['1 · Mat', '2 · Mat', '3 · Mat', '4 · CyR', '5 · CyR', '6 · CyR']);
    expect(pestanaActiva()).toBe(0);
    const asignatura = (): string =>
      raiz.querySelector<HTMLSelectElement>('.actividad-form__plaza select[formcontrolname="asignatura"]')!.value;
    expect(raiz.querySelectorAll('.actividad-form__plaza').length).toBe(1);
    expect(asignatura()).toBe('Mat');
    expect(raiz.querySelector('.actividad-form__aula-fija')).not.toBeNull();

    await abrirPestana(3);
    expect(pestanaActiva()).toBe(3);
    expect(raiz.querySelectorAll('.actividad-form__plaza').length).toBe(1);
    expect(asignatura()).toBe('CyR');
    expect(raiz.querySelector('.actividad-form__candidatas')).not.toBeNull();
    expect(enLista('aulas', 'elegidos')).toEqual(['A1', 'INF1']);
  });

  it('P3 «+» añade una plaza vacía y la deja abierta', async () => {
    montar(TRES_PLAZAS);
    await anadirFila();
    expect(instancia().plazas.length).toBe(4);
    expect(pestanaActiva()).toBe(3);
    expect(pestana(3).textContent!.trim()).toBe('4 · —');
    expect(enLista('profesores', 'elegidos')).toEqual([]);
  });

  it('P4 «Quitar esta plaza» deja abierta la vecina: la siguiente, o la anterior si era la última', async () => {
    montar(TRES_PLAZAS);
    await quitarFila(1);
    // La del medio: queda abierta la que ocupa su sitio, que era la tercera.
    expect(instancia().plazas.length).toBe(2);
    expect(pestanaActiva()).toBe(1);
    expect(fixture.nativeElement.querySelector('.actividad-form__aula-fija').value).toBe('INF1');

    await quitarFila(1);
    // La última: queda abierta la anterior, no un índice que ya no existe.
    expect(instancia().plazas.length).toBe(1);
    expect(pestanaActiva()).toBe(0);
    expect(fixture.nativeElement.querySelectorAll('.actividad-form__plaza').length).toBe(1);
  });

  it('P5 I2 marca la pestaña de CADA plaza implicada, y solo esas', async () => {
    montar(TRES_PLAZAS);
    await abrirPestana(2);
    // La tercera repite el subgrupo de la primera, por la lista doble.
    await elegirEnLista('subgrupos', '1ºA-Completo');
    fixture.detectChanges();

    expect(pestanasCon('i2')).toEqual([0, 2]);
    expect(fixture.nativeElement.querySelector('.actividad-form__error-i2').textContent).toContain('1ºA-Completo');
    instancia().guardar();
    http.expectNone('/api/actividades/30');
  });

  it('P5b DISCRIMINANTE de P5: sin repetidos, ninguna pestaña lleva la marca de I2', () => {
    montar(TRES_PLAZAS);
    expect(pestanasCon('i2')).toEqual([]);
    expect(fixture.nativeElement.querySelector('.actividad-form__error-i2')).toBeNull();
  });

  it('P6 una plaza con un campo inválido marca su pestaña tras «Guardar»; la recién añadida no nace marcada', async () => {
    montar(TRES_PLAZAS);
    await anadirFila();
    // Nueva y vacía, pero sin tocar: sin marca (enmienda 4).
    expect(pestanasCon('invalida')).toEqual([]);

    await abrirPestana(0);
    instancia().guardar();
    fixture.detectChanges();
    // El error está en la plaza 4, que no es la abierta: su pestaña lo dice.
    expect(pestanasCon('invalida')).toEqual([3]);
    http.expectNone('/api/actividades/30');
  });

  it('P6b DISCRIMINANTE de P6: con todo válido «Guardar» envía y no marca nada', () => {
    montar(TRES_PLAZAS);
    instancia().guardar();
    fixture.detectChanges();
    expect(pestanasCon('invalida')).toEqual([]);
    http.expectOne('/api/actividades/30').flush(TRES_PLAZAS);
  });

  it('P7 modos de aula «Fija», «Candidatas» y «La elige el generador», con lo suyo debajo', async () => {
    montar(null);
    const raiz = fixture.nativeElement as HTMLElement;
    const rotulos = [...raiz.querySelectorAll('input[type="radio"][formcontrolname="modoAula"]')].map((r) =>
      r.closest('label')!.textContent!.trim(),
    );
    expect(rotulos).toEqual(['Fija', 'Candidatas', 'La elige el generador']);
    const radio = (valor: string): HTMLInputElement =>
      raiz.querySelector<HTMLInputElement>(`input[type="radio"][value="${valor}"]`)!;
    const estado = (): [number, number, number] => [
      raiz.querySelectorAll('.actividad-form__aula-fija').length,
      raiz.querySelectorAll('[data-lista="aulas"]').length,
      raiz.querySelectorAll('[data-nota="generador"]').length,
    ];

    expect(estado()).toEqual([1, 0, 0]);
    radio('CANDIDATAS').click();
    await fixture.whenStable();
    expect(estado()).toEqual([0, 1, 0]);
    radio('NINGUNA').click();
    await fixture.whenStable();
    expect(estado()).toEqual([0, 0, 1]);
    expect(raiz.querySelector('[data-nota="generador"]')!.textContent!.trim())
      .toBe('El generador elegirá el aula con las reglas del centro.');

    // Fuera de una CLASE el modo se llama «Sin aula» y no hay nota: no ocupa aula.
    instancia().form.patchValue({ tipo: 'REUNION' });
    fixture.detectChanges();
    expect(estado()).toEqual([0, 0, 0]);
  });

  it('P8 las tres listas dobles de la plaza muestran 5 filas', async () => {
    montar(null);
    const raiz = fixture.nativeElement as HTMLElement;
    raiz.querySelector<HTMLInputElement>('input[type="radio"][value="CANDIDATAS"]')!.click();
    await fixture.whenStable();
    const filas = [...raiz.querySelectorAll<HTMLElement>('[data-lista]')].map((l) => [
      l.dataset['lista'],
      l.style.getPropertyValue('--lista-doble-filas'),
    ]);
    expect(filas).toEqual([
      ['profesores', '5'],
      ['subgrupos', '5'],
      ['aulas', '5'],
    ]);
  });

  it('P9 ids únicos y cada <label for> apunta a un control que existe (decisión 11)', async () => {
    montar(TRES_PLAZAS);
    const raiz = fixture.nativeElement as HTMLElement;
    const comprobar = (): void => {
      const ids = [...raiz.querySelectorAll('[id]')].map((e) => e.id);
      expect(new Set(ids).size).toBe(ids.length);
      for (const l of raiz.querySelectorAll<HTMLLabelElement>('label[for]')) {
        expect(raiz.querySelector(`#${l.htmlFor}`)).not.toBeNull();
      }
    };
    comprobar();
    // Los dos selectores de asignatura y el de aula fija tienen el suyo.
    for (const s of raiz.querySelectorAll<HTMLSelectElement>(
      'select[formcontrolname="asignatura"], select[formcontrolname="aulaFija"]',
    )) {
      expect(raiz.querySelector(`label[for="${s.id}"]`)).not.toBeNull();
    }
    const antes = raiz.querySelector<HTMLSelectElement>('.actividad-form__plaza select[formcontrolname="asignatura"]')!.id;
    await abrirPestana(1);
    comprobar();
    expect(raiz.querySelector<HTMLSelectElement>('.actividad-form__plaza select[formcontrolname="asignatura"]')!.id)
      .not.toBe(antes);
  });

  describe('plazasConSubgrupoRepetido (S230, misma semántica que I2)', () => {
    it('marca las plazas que comparten un subgrupo, y solo esas', () => {
      expect(plazasConSubgrupoRepetido([['A'], ['B'], ['A', 'C'], ['C']])).toEqual([0, 2, 3]);
    });

    it('un subgrupo repetido DENTRO de una plaza no es I2, y la caja cuenta', () => {
      expect(plazasConSubgrupoRepetido([['A', 'A'], ['a']])).toEqual([]);
    });
  });

  /**
   * Asignatura con su nombre (S230, condición 4 de O-acabado-pre-prueba). ONCE asignaturas para
   * que el filtro se pinte; CyR es el oráculo medido en la base (s230/f1f2/40-oraculo-c4.txt):
   * «Robótica» no está en su código.
   */
  describe('asignatura con código y nombre (S230)', () => {
    const ASIGNATURAS_NOMBRE = [
      { id: 1, codigo: 'CyR', nombreCompleto: 'Computación y Robótica' },
      { id: 2, codigo: 'SIN', nombreCompleto: '  ' },
      ...Array.from({ length: 9 }, (_, i) => ({ id: i + 3, codigo: `M0${i + 1}`, nombreCompleto: 'Matemáticas' })),
    ];

    function montarConAsignaturas(): void {
      ref = { close: vi.fn() };
      TestBed.configureTestingModule({
        imports: [ActividadForm],
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
          { provide: DialogRef, useValue: ref },
          { provide: DIALOG_DATA, useValue: null },
        ],
      });
      fixture = TestBed.createComponent(ActividadForm);
      http = TestBed.inject(HttpTestingController);
      fixture.detectChanges();
      http.expectOne('/api/asignaturas').flush(ASIGNATURAS_NOMBRE);
      http.expectOne('/api/profesores').flush(PROFESORES);
      http.expectOne('/api/aulas').flush(AULAS);
      http.expectOne('/api/subgrupos').flush(SUBGRUPOS);
      fixture.detectChanges();
    }

    const SELECTORES = [
      ['actividad', '.actividad-form__fila--1'],
      ['plaza', '.actividad-form__plaza'],
    ] as const;
    const opciones = (zona: string): HTMLOptionElement[] =>
      Array.from(
        (fixture.nativeElement as HTMLElement).querySelector<HTMLSelectElement>(
          `${zona} select[formcontrolname="asignatura"]`,
        )!.options,
      ).filter((o) => o.value !== '');

    for (const [nombre, zona] of SELECTORES) {
      it(`D1 ${nombre}: cada opción dice «código — nombre», y solo el código si no hay nombre`, () => {
        montarConAsignaturas();
        const textos = new Map(opciones(zona).map((o) => [o.value, o.textContent!.trim()]));
        expect(textos.get('CyR')).toBe('CyR — Computación y Robótica');
        expect(textos.get('SIN')).toBe('SIN');
      });

      it(`D2 ${nombre}: buscar «Robótica», que no está en el código, encuentra CyR`, async () => {
        montarConAsignaturas();
        const caja = (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>(
          `${zona} input[aria-label="Filtrar asignaturas"]`,
        )!;
        caja.value = 'Robótica';
        caja.dispatchEvent(new Event('input'));
        await fixture.whenStable();
        expect(opciones(zona).map((o) => o.value)).toEqual(['CyR']);
      });
    }
  });
});
