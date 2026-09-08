import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { ReplicacionDialogo } from './replicacion-dialogo';

/**
 * Congela el diálogo de replicación: los tres filtros del desplegable de hermanos
 * (1)-(4), el plan que no haría nada (5), la habilitación de «Aceptar» por decisiones
 * completas (6)-(8), la composición del cuerpo (9)-(11), lo que la pantalla NO pinta
 * (12)-(13), la inferencia sobre el 400 (14) y el ciclo escribir/deshacer/cerrar
 * (15)-(19). Secuencia propia desde (1).
 *
 * <p><b>`HttpTestingController` y no un doble del servicio.</b> Molde de
 * `grupo-form.spec.ts`, no el de `pdc-dialogo.spec.ts`: aquel mockea `PdcService` porque
 * necesita entregar un observable PENDIENTE, escenario que aquí no hace falta, y a cambio
 * pierde la capacidad de afirmar sobre la RUTA y sobre la AUSENCIA de peticiones, que es
 * justo lo que miden (5), (16) y (17).
 *
 * <p><b>EXTENSIÓN DEL MOLDE: `montar()` hace red.</b> El `ngOnInit` pide `/api/grupos`
 * para poblar el desplegable de hermanos. Con `http.verify()` en el `afterEach`, CUALQUIER
 * caso que montase el componente sin consumir esa petición fallaría por «open request»
 * —incluidos los quince que no hablan del catálogo—, así que el flush vive DENTRO del
 * helper: es parte del montaje, no del escenario. Misma decisión y mismo motivo que el
 * `/api/niveles` de `GrupoForm`.
 *
 * <p>La app es ZONELESS: el DOM repinta en el frame siguiente. Todo caso que lea el DOM
 * tras una respuesta o tras cambiar un `<select>` hace `await fixture.whenStable()` antes;
 * leerlo en el mismo tick mediría el DOM de antes.
 *
 * <p><b>El grupo tiene id 7, no 1</b> (molde de `PdcDialogo`): con 1, un componente que
 * pasara una constante o el id equivocado seguiría verde. Y el catálogo trae UN candidato
 * excluido por CADA filtro y por ninguna otra razón: `2ESOA` es ordinario y de otro id,
 * `1ESOADI` es del mismo nivel y de otro id, y `1ESOC` es ordinario y de su nivel. Un
 * fixture donde un candidato cayera por dos motivos a la vez dejaría verde el caso sin
 * discriminar cuál de los dos filtros lo tumbó.
 */

const GRUPO = { id: 7, codigo: '1ESOC', nivel: '1ESO', tipo: 'ORDINARIO' };

/** Ordinario, mismo nivel, otro id: el ÚNICO que debe salir en el desplegable. */
const HERMANO = { id: 3, codigo: '1ESOB', nivel: '1ESO', tipo: 'ORDINARIO' };
/** Excluido SOLO por el nivel: es ordinario y su id no es el del grupo. */
const OTRO_NIVEL = { id: 4, codigo: '2ESOA', nivel: '2ESO', tipo: 'ORDINARIO' };
/** Excluido SOLO por el tipo: mismo nivel y su id no es el del grupo. */
const PDC = { id: 5, codigo: '1ESOADI', nivel: '1ESO', tipo: 'DIVERSIFICACION_PDC' };
/** Excluido SOLO por ser él mismo: ordinario y de su propio nivel. */
const EL_PROPIO = { id: 7, codigo: '1ESOC', nivel: '1ESO', tipo: 'ORDINARIO' };

const CATALOGO = [HERMANO, OTRO_NIVEL, PDC, EL_PROPIO];

/**
 * Un plan con UN bloque replicado y UN bloque de reparto de DOS vías, cuyo primer vía
 * lleva los originales de DOS espejos.
 *
 * <p>`subgruposACrear` trae TRES códigos y en otro orden que los de reparto, y el primero
 * —el `-Completo`— no participa en ningún bloque: es la lista que una implementación
 * perezosa recorrería para componer el cuerpo, y (9) y (10) caen si lo hace.
 *
 * <p>`plazaCodigo` es `'P-1'`/`'P-2'`, cadenas que no aparecen en ningún otro campo del
 * fixture: (12) mide su ausencia en el DOM por `textContent`, y con un código que fuera
 * subcadena de un rótulo el aserto no distinguiría nada.
 */
const PLAN = {
  grupo: '1ESOC',
  hermano: '1ESOB',
  subgruposACrear: ['1ESOC-Completo', '1ESOC-Mat1', '1ESOC-Mat2'],
  replicados: [
    {
      actividad: 'LEN-1ESO',
      vias: [
        {
          plazaId: 11,
          plazaCodigo: 'P-9',
          asignatura: 'LEN',
          gruposActuales: ['1ESOA', '1ESOB'],
          espejos: ['1ESOC-Completo'],
          hermanoPresente: true,
        },
      ],
    },
  ],
  reparto: [
    {
      actividad: 'MAT-1ESO',
      vias: [
        {
          plazaId: 7,
          plazaCodigo: 'P-1',
          asignatura: 'MAT',
          gruposActuales: ['1ESOA', '1ESOB'],
          espejos: ['1ESOC-Mat1', '1ESOC-Mat2'],
          hermanoPresente: true,
        },
        {
          plazaId: 9,
          plazaCodigo: 'P-2',
          asignatura: 'MAT',
          gruposActuales: ['1ESOA'],
          espejos: [],
          hermanoPresente: false,
        },
      ],
    },
  ],
};

/**
 * Un plan con DOS bloques de reparto que comparten el MISMO espejo: `1ESOC-Rep` tiene su
 * original en una vía de `MAT-1ESO` y en una de `FIS-1ESO`.
 *
 * <p>Es la forma que el servidor admite y que el catálogo real ya produce en parte —50 de
 * los 334 subgrupos aparecen en dos actividades—, y sobre la que `Destino.plazas()` ACUMULA
 * las vías de los dos bloques para aceptar UNA sola plaza de la unión. El `PLAN` corriente
 * tiene un único bloque de reparto, y sobre él «las vías de su bloque» y «la unión de las
 * vías de sus bloques» son indistinguibles; (20) necesita este.
 *
 * <p>Y lleva un SEGUNDO espejo, `1ESOC-Fis`, que vive SOLO en el bloque de FIS. Sin él, todo
 * espejo del fixture estaría en todos sus bloques y quitar el filtro «los bloques de ESTE
 * espejo» no cambiaría nada: es lo que dejó vivo a un mutante en la campaña, y lo que mide
 * (21).
 */
const PLAN_DOS_BLOQUES = {
  grupo: '1ESOC',
  hermano: '1ESOB',
  subgruposACrear: ['1ESOC-Rep', '1ESOC-Fis'],
  replicados: [],
  reparto: [
    {
      actividad: 'MAT-1ESO',
      vias: [
        {
          plazaId: 7,
          plazaCodigo: 'P-1',
          asignatura: 'MAT',
          gruposActuales: ['1ESOB'],
          espejos: ['1ESOC-Rep'],
          hermanoPresente: true,
        },
        {
          plazaId: 9,
          plazaCodigo: 'P-2',
          asignatura: 'MAT',
          gruposActuales: ['1ESOA'],
          espejos: [],
          hermanoPresente: false,
        },
      ],
    },
    {
      actividad: 'FIS-1ESO',
      vias: [
        {
          plazaId: 21,
          plazaCodigo: 'P-3',
          asignatura: 'FIS',
          gruposActuales: ['1ESOB'],
          espejos: ['1ESOC-Rep', '1ESOC-Fis'],
          hermanoPresente: true,
        },
        {
          plazaId: 23,
          plazaCodigo: 'P-4',
          asignatura: 'FIS',
          gruposActuales: ['1ESOA'],
          espejos: [],
          hermanoPresente: false,
        },
      ],
    },
  ],
};

/** El hermano no tiene NADA que copiar: ni espejos ni bloques. */
const PLAN_VACIO = {
  grupo: '1ESOC',
  hermano: '1ESOB',
  subgruposACrear: [],
  replicados: [],
  reparto: [],
};

/** Espeja la constante `SIN_PLAZA` del componente: el value de «No matricular aquí». */
const SIN_PLAZA = 'sin-plaza';

describe('ReplicacionDialogo', () => {
  let fixture: ComponentFixture<ReplicacionDialogo>;
  let http: HttpTestingController;
  let ref: { close: ReturnType<typeof vi.fn> };

  /**
   * Monta el componente y CONSUME el `GET /api/grupos` del `ngOnInit`. Ver el javadoc del
   * fichero: sin ese flush aquí, el `http.verify()` del `afterEach` tumba todos los casos.
   */
  function montar(catalogo: object[] = CATALOGO, fallaCatalogo = false): void {
    ref = { close: vi.fn() };
    TestBed.configureTestingModule({
      imports: [ReplicacionDialogo],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: DialogRef, useValue: ref },
        { provide: DIALOG_DATA, useValue: GRUPO },
      ],
    });
    fixture = TestBed.createComponent(ReplicacionDialogo);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges(); // dispara ngOnInit → GET /api/grupos
    const peticion = http.expectOne('/api/grupos');
    if (fallaCatalogo) {
      peticion.flush('', { status: 500, statusText: 'Server Error' });
    } else {
      peticion.flush(catalogo);
    }
    fixture.detectChanges(); // pinta los <option> ya cargados
  }

  /** Acceso a los miembros protegidos que los casos necesitan tocar. */
  function instancia(): { estado: () => string; aceptar: () => void } {
    return fixture.componentInstance as unknown as {
      estado: () => string;
      aceptar: () => void;
    };
  }

  const raiz = (): HTMLElement => fixture.nativeElement as HTMLElement;

  /** Un botón por su clase. Falla ruidosamente si no está, en vez de clicar `undefined`. */
  function boton(clase: string): HTMLButtonElement {
    const b = raiz().querySelector<HTMLButtonElement>(clase);
    expect(b, `no existe el botón ${clase}`).toBeTruthy();
    return b!;
  }

  /** Los `value` ofrecidos por un `<select>`, sin el placeholder vacío. */
  function valoresDe(select: HTMLSelectElement): string[] {
    return [...select.querySelectorAll('option')].map((o) => o.value).filter((v) => v !== '');
  }

  /** Los códigos ofrecidos en el desplegable de hermanos. */
  function hermanosOfrecidos(): string[] {
    return valoresDe(raiz().querySelector<HTMLSelectElement>('#replicacion-hermano')!);
  }

  /** Los desplegables de reparto, en el orden en que la plantilla los pinta. */
  function decisiones(): HTMLSelectElement[] {
    return [...raiz().querySelectorAll<HTMLSelectElement>('.replicacion-dialogo__decision')];
  }

  /**
   * Elige un valor en un `<select>` y notifica el cambio como lo haría el navegador. El
   * `value` solo se queda pegado si existe una `<option>` con él, así que un desplegable
   * al que le falte la opción deja la decisión SIN contestar y (6)-(8) lo notan.
   */
  function elegir(select: HTMLSelectElement, valor: string): void {
    select.value = valor;
    select.dispatchEvent(new Event('change'));
  }

  /** Lleva la pantalla hasta `'decidiendo'` con el plan dado. */
  async function verPlan(plan: object, codigoHermano = '1ESOB'): Promise<void> {
    elegir(raiz().querySelector<HTMLSelectElement>('#replicacion-hermano')!, codigoHermano);
    await fixture.whenStable();
    boton('.replicacion-dialogo__ver').click();
    http.expectOne(`/api/grupos/7/replicacion?hermano=${codigoHermano}`).flush(plan);
    await fixture.whenStable();
  }

  /** Contesta las dos decisiones del `PLAN` y pulsa «Aceptar». */
  async function aceptarCon(primera: string, segunda: string): Promise<void> {
    const selects = decisiones();
    expect(selects).toHaveLength(2);
    elegir(selects[0], primera);
    elegir(selects[1], segunda);
    await fixture.whenStable();
    boton('.replicacion-dialogo__aceptar').click();
  }

  afterEach(() => http.verify());

  it('(1) FILTRO NIVEL: un ordinario de otro nivel no se ofrece como hermano', async () => {
    montar();
    await fixture.whenStable();

    // `2ESOA` cumple los otros dos filtros —es ORDINARIO y su id (4) no es el del grupo—,
    // así que lo único que puede haberlo excluido es el nivel.
    expect(hermanosOfrecidos()).not.toContain('2ESOA');
  });

  it('(2) FILTRO TIPO: un PDC del mismo nivel no se ofrece como hermano', async () => {
    montar();
    await fixture.whenStable();

    // `1ESOADI` es de '1ESO' y su id (5) no es el del grupo: solo el tipo lo excluye. Es
    // el filtro que NO tiene espejo en el servidor —`analizar` valida el tipo del grupo,
    // no el del hermano—, así que si desaparece, el backend acepta el par y revienta
    // después en `derivarEspejos` con un 400 sobre prefijos que no explica nada.
    expect(hermanosOfrecidos()).not.toContain('1ESOADI');
  });

  it('(3) FILTRO SÍ MISMO: el propio grupo no se ofrece como hermano de sí mismo', async () => {
    montar();
    await fixture.whenStable();

    // El aserto va sobre los `value` del DESPLEGABLE y no sobre el `textContent` de la
    // pantalla: '1ESOC' está en el <h2> del título, así que un `not.toContain` sobre el
    // DOM entero sería falso siempre y el caso no podría estar verde nunca.
    expect(hermanosOfrecidos()).not.toContain('1ESOC');
  });

  it('(4) un ordinario del mismo nivel SÍ se ofrece', async () => {
    montar();
    await fixture.whenStable();

    // Sin este caso, borrar el desplegable entero —o filtrarlo todo— dejaría verdes
    // (1), (2) y (3): los tres afirman AUSENCIAS.
    expect(hermanosOfrecidos()).toEqual(['1ESOB']);
  });

  it('(5) un plan que no haría nada NO deja continuar, ni por el botón ni por el método', async () => {
    montar();
    await fixture.whenStable();
    await verPlan(PLAN_VACIO);

    expect(boton('.replicacion-dialogo__aceptar').disabled).toBe(true);
    // Y la guarda del método, no solo el atributo: el servidor responde 201 a este POST
    // —un éxito que no ha creado nada—, así que si la única defensa fuera el `disabled`,
    // cualquier camino que llame a `aceptar()` lo colaría. El `expectNone` lo mide.
    instancia().aceptar();
    http.expectNone('/api/grupos/7/replicacion');
  });

  it('(6) DISCRIMINANTE: con dos decisiones y UNA contestada, «Aceptar» sigue apagado', async () => {
    montar();
    await fixture.whenStable();
    await verPlan(PLAN);

    const selects = decisiones();
    expect(selects).toHaveLength(2);
    elegir(selects[0], '7');
    await fixture.whenStable();

    // El caso intermedio, y el único que discrimina: con solo 0 y 2 contestadas
    // sobrevive un `some()` en lugar del `every()`, y el POST saldría sin la segunda
    // asignación —que el servidor rechaza con un 400 de «falta la asignación de 1»—.
    expect(boton('.replicacion-dialogo__aceptar').disabled).toBe(true);
  });

  it('(7) con las DOS contestadas, «Aceptar» se enciende', async () => {
    montar();
    await fixture.whenStable();
    await verPlan(PLAN);

    const selects = decisiones();
    elegir(selects[0], '7');
    elegir(selects[1], '9');
    await fixture.whenStable();

    expect(boton('.replicacion-dialogo__aceptar').disabled).toBe(false);
  });

  it('(8) «No matricular aquí» CUENTA como contestada y viaja como plaza null', async () => {
    montar();
    await fixture.whenStable();
    await verPlan(PLAN);

    // La opción existe de verdad: si no, el `elegir` no pegaría el valor y el caso
    // quedaría verde por el camino equivocado.
    expect(valoresDe(decisiones()[0])).toContain(SIN_PLAZA);

    await aceptarCon(SIN_PLAZA, '9');

    const peticion = http.expectOne('/api/grupos/7/replicacion');
    const cuerpo = peticion.request.body as { asignaciones: { subgrupo: string; plaza: number | null }[] };
    // `null` ESTRICTO, no `undefined` ni ausente: es respuesta legítima del contrato
    // —«el espejo se crea y se queda sin plazas»— y omitir la entrada sería el 400 de
    // «falta la asignación».
    expect(cuerpo.asignaciones[0]).toEqual({ subgrupo: '1ESOC-Mat1', plaza: null });
    peticion.flush(PLAN);
    await fixture.whenStable();
  });

  it('(9) el cuerpo lleva EXACTAMENTE una asignación por espejo de reparto', async () => {
    montar();
    await fixture.whenStable();
    await verPlan(PLAN);
    await aceptarCon('7', '9');

    const peticion = http.expectOne('/api/grupos/7/replicacion');
    const cuerpo = peticion.request.body as { asignaciones: { subgrupo: string }[] };
    // DOS, no tres: `subgruposACrear` trae también '1ESOC-Completo', que no participa en
    // ningún bloque de reparto y por el que el servidor devolvería 400 («no corresponde a
    // ningún bloque de reparto»). La longitud caza el de más y el conjunto caza el
    // duplicado y el omitido.
    expect(cuerpo.asignaciones).toHaveLength(2);
    expect(cuerpo.asignaciones.map((a) => a.subgrupo).sort())
      .toEqual(['1ESOC-Mat1', '1ESOC-Mat2']);
    peticion.flush(PLAN);
    await fixture.whenStable();
  });

  it('(10) el campo subgrupo lleva el código del ESPEJO', async () => {
    montar();
    await fixture.whenStable();
    await verPlan(PLAN);
    await aceptarCon('7', '9');

    const peticion = http.expectOne('/api/grupos/7/replicacion');
    const cuerpo = peticion.request.body as { hermano: string; asignaciones: { subgrupo: string }[] };
    // Valores LITERALES: los espejos salen de `Via.espejos`, y la confusión posible es
    // recorrer `subgruposACrear`, cuyo primer elemento es '1ESOC-Completo' y cuyo orden
    // es otro. El código del ORIGINAL ('1ESOB-Mat1') no está en el plan y por tanto no
    // es una confusión que este fixture pueda provocar: ver el informe del encargo.
    expect(cuerpo.asignaciones[0].subgrupo).toBe('1ESOC-Mat1');
    expect(cuerpo.asignaciones[1].subgrupo).toBe('1ESOC-Mat2');
    // Y el hermano viaja por CÓDIGO, que es como lo nombra el contrato. ESTA línea es lo
    // que hace que (10) no sea redundante: la mitad de los espejos la comparte con (9),
    // que cae ante las mismas mutaciones, pero el `hermano` no lo mira ningún otro caso.
    // Medido: mandar aquí `this.grupo.codigo` en vez del elegido pone rojo (10) y SOLO
    // (10), y es un fallo que el servidor devolvería como un 404 de hermano inexistente.
    expect(cuerpo.hermano).toBe('1ESOB');
    peticion.flush(PLAN);
    await fixture.whenStable();
  });

  it('(11) la plaza viaja por plazaId y como NÚMERO, no como el código ni como texto', async () => {
    montar();
    await fixture.whenStable();
    await verPlan(PLAN);
    await aceptarCon('7', '9');

    const peticion = http.expectOne('/api/grupos/7/replicacion');
    const cuerpo = peticion.request.body as { asignaciones: { plaza: number | null }[] };
    // `toBe(7)` y no `toEqual('7')`: el value de un <select> vuelve como TEXTO, y un '7'
    // colado en el cuerpo pasa el compilador —el campo es `number | null`, pero nadie
    // valida el JSON— y muere en el servidor. La vía elegida tiene plazaCodigo 'P-1', que
    // es lo que NO debe viajar.
    expect(cuerpo.asignaciones[0].plaza).toBe(7);
    expect(cuerpo.asignaciones[1].plaza).toBe(9);
    peticion.flush(PLAN);
    await fixture.whenStable();
  });

  it('(12) el plazaCodigo NO se pinta, y sí el rótulo legible de la vía', async () => {
    montar();
    await fixture.whenStable();
    await verPlan(PLAN);

    // Ordinal técnico documentado como INESTABLE entre ediciones: enseñarlo daría al
    // usuario un identificador que mañana nombra otra cosa.
    expect(raiz().textContent).not.toContain('P-1');
    expect(raiz().textContent).not.toContain('P-2');
    // Y el aserto de presencia, para que el de ausencia no quede verde por no haber
    // pintado ninguna vía: el rótulo es asignatura + grupos + ordinal DEL BLOQUE.
    expect(raiz().textContent).toContain('MAT · 1ESOA, 1ESOB · vía 1');
    expect(raiz().textContent).toContain('MAT · 1ESOA · vía 2');
  });

  it('(13) los bloques replicados llegan PLEGADOS y su detalle no está en el DOM', async () => {
    montar();
    await fixture.whenStable();
    await verPlan(PLAN);

    // Con `replicados` NO vacío —lo dice el resumen— y cero elementos de detalle: son
    // decenas en el centro real y el usuario no decide nada sobre ellos.
    expect(raiz().textContent).toContain('1 bloque(s) se incorporan solos');
    expect(raiz().querySelectorAll('.replicacion-dialogo__replicado')).toHaveLength(0);

    // Y al desplegar aparecen: sin esto, borrar la rama entera dejaría verde el aserto
    // de ausencia.
    boton('.replicacion-dialogo__desplegar').click();
    await fixture.whenStable();
    expect(raiz().querySelectorAll('.replicacion-dialogo__replicado')).toHaveLength(1);
    expect(raiz().textContent).toContain('LEN-1ESO');
  });

  /**
   * Falla el `GET` del plan con el status dado y devuelve el texto presentado. El cuerpo va
   * VACÍO a propósito: el mensaje del servidor no llega al cliente desde la migración a
   * Boot 4 (D-F8.6-ii-a), y es esa ausencia la que obliga a la inferencia por eliminación.
   */
  async function planQueFalla(status: number, statusText: string): Promise<string> {
    elegir(raiz().querySelector<HTMLSelectElement>('#replicacion-hermano')!, '1ESOB');
    await fixture.whenStable();
    boton('.replicacion-dialogo__ver').click();
    http.expectOne('/api/grupos/7/replicacion?hermano=1ESOB').flush({}, { status, statusText });
    await fixture.whenStable();
    return raiz().querySelector('.replicacion-dialogo__error-servidor')!.textContent!;
  }

  it('(14a) un 400 del plan se presenta como «ya tiene subgrupos»', async () => {
    montar();
    await fixture.whenStable();

    expect(await planQueFalla(400, 'Bad Request')).toContain('ya tiene subgrupos');
  });

  it('(14b) DISCRIMINANTE: un 404 NO se presenta como «ya tiene subgrupos»', async () => {
    montar();
    await fixture.whenStable();

    // Va en un `it` propio y no como segunda mitad de (14a) porque el `TestBed` no se
    // deja reconfigurar dos veces en el mismo caso, y montar una sola vez con dos
    // respuestas no es posible: la petición del plan es una. Partirlo, además, deja a
    // cada mitad bajo su propio `http.verify()`.
    //
    // Sin este caso, un mensaje único para todo error dejaría (14a) verde sin medir la
    // discriminación por status, que es lo único que hay aquí: los siete 400 de la ruta
    // llegan indistinguibles y el 404 es lo único que sí se puede separar.
    const err = await planQueFalla(404, 'Not Found');
    expect(err).not.toContain('ya tiene subgrupos');
    expect(err).toContain('404');
  });

  it('(15) tras el 201 la pantalla ofrece «Deshacer» y ya no ofrece «Aceptar»', async () => {
    montar();
    await fixture.whenStable();
    await verPlan(PLAN);
    await aceptarCon('7', '9');
    http.expectOne('/api/grupos/7/replicacion').flush(PLAN);
    await fixture.whenStable();

    expect(instancia().estado()).toBe('hecho');
    expect(raiz().querySelector('.replicacion-dialogo__deshacer')).toBeTruthy();
    // Y no queda forma de reenviar: un segundo POST daría el 400 de «ya tiene subgrupos».
    expect(raiz().querySelector('.replicacion-dialogo__aceptar')).toBeNull();
  });

  it('(16) «Deshacer» emite el DELETE de la ruta del sub-recurso y cierra con true', async () => {
    montar();
    await fixture.whenStable();
    await verPlan(PLAN);
    await aceptarCon('7', '9');
    http.expectOne('/api/grupos/7/replicacion').flush(PLAN);
    await fixture.whenStable();

    boton('.replicacion-dialogo__deshacer').click();
    // Ruta Y verbo: el sub-recurso, nunca `DELETE /api/grupos/7`, que borraría el grupo.
    const peticion = http.expectOne('/api/grupos/7/replicacion');
    expect(peticion.request.method).toBe('DELETE');
    peticion.flush({ grupo: '1ESOC', subgruposBorrados: ['1ESOC-Mat1'], plazasDescableadas: [] });
    await fixture.whenStable();

    // `true` y no `false`: deshacer no es cancelar, es una segunda escritura, y la lista
    // que abrió este diálogo tiene que recargar igual que tras el alta.
    expect(ref.close).toHaveBeenCalledWith(true);
  });

  it('(17) cancelar en seco cierra con false y no escribe nada', async () => {
    montar();
    await fixture.whenStable();

    boton('.replicacion-dialogo__cerrar').click();

    expect(ref.close).toHaveBeenCalledWith(false);
    // Ninguna petición que no sea el GET del catálogo ya consumido. El `http.verify()`
    // del `afterEach` caza también las que salieran después, igual que en `aula-form`.
    http.expectNone((peticion) => peticion.method !== 'GET');
  });

  it('(18) un error del POST se PRESENTA y el diálogo NO se cierra', async () => {
    montar();
    await fixture.whenStable();
    await verPlan(PLAN);
    await aceptarCon('7', '9');
    http.expectOne('/api/grupos/7/replicacion').flush(
      { message: 'No se puede replicar: 2 actividad(es) con horario' },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();

    // El 409 de dependientes no se arregla cerrando: el usuario tiene que leerlo con sus
    // decisiones todavía en pantalla, y volver a intentarlo tras quitar el horario.
    expect(ref.close).not.toHaveBeenCalled();
    const err = raiz().querySelector('.replicacion-dialogo__error-servidor')!.textContent!;
    expect(err).toContain('2 actividad(es) con horario');
    expect(err).not.toContain('No se pudo replicar 1ESOC desde 1ESOB (409)');
    // Y sigue en la rama de decisión, con «Aceptar» disponible para reintentar.
    expect(instancia().estado()).toBe('decidiendo');
    expect(raiz().querySelector('.replicacion-dialogo__aceptar')).toBeTruthy();
  });

  it('(19) un error sin message y sin error cae al degradado propio con el status', async () => {
    montar();
    await fixture.whenStable();
    await verPlan(PLAN);
    await aceptarCon('7', '9');
    http.expectOne('/api/grupos/7/replicacion').flush({}, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    // La precedencia de `mensaje()`: `message`, luego `error`, y solo entonces el texto
    // propio con el status. Con cuerpo vacío se ve el tercero, y nombra al grupo y al
    // hermano —que es lo que lo distingue del degradado de cualquier otra pantalla—.
    const err = raiz().querySelector('.replicacion-dialogo__error-servidor')!.textContent!;
    expect(err).toContain('No se pudo replicar 1ESOC desde 1ESOB (500).');
  });

  it('(20) un espejo en DOS bloques de reparto ofrece la UNIÓN de las vías de ambos', async () => {
    montar();
    await fixture.whenStable();
    await verPlan(PLAN_DOS_BLOQUES);

    // DOS decisiones y no tres: `1ESOC-Rep` aparece en los dos bloques y aun así pregunta
    // UNA vez, porque el servidor acepta una sola plaza por espejo —`Destino.plazas()`
    // acumula las vías de todos sus bloques en un único destino—.
    const selects = decisiones();
    expect(selects).toHaveLength(2);

    // Las CUATRO vías para el espejo que está en los dos, no las dos del primero. Este es
    // el aserto discriminante: con «las vías de su bloque» —la lectura literal, correcta
    // mientras el espejo esté en uno solo— la pantalla escondería 21 y 23, que son
    // destinos que el servidor SÍ acepta, y el usuario no tendría forma de elegirlos.
    expect(valoresDe(selects[0])).toEqual(['7', '9', '21', '23', SIN_PLAZA]);

    // Y agrupadas por actividad: sin el <optgroup>, las vías 7 y 21 serían las dos «vía 1»
    // y el rótulo no las distinguiría.
    const grupos = [...selects[0].querySelectorAll('optgroup')].map((g) => g.label);
    expect(grupos).toEqual(['MAT-1ESO', 'FIS-1ESO']);
  });

  it('(21) DISCRIMINANTE: un espejo NO recibe las vías de bloques donde no participa', async () => {
    montar();
    await fixture.whenStable();
    await verPlan(PLAN_DOS_BLOQUES);

    // `1ESOC-Fis` está solo en FIS-1ESO: sus opciones son 21 y 23, nunca 7 ni 9. Ofrecerle
    // las de MAT sería ofrecer un destino que `validarAsignaciones` rechaza con un 400
    // —«una asignacion solo puede elegir entre las vias de SU propio bloque»—, o sea
    // empujar al usuario a un error que no ha cometido él.
    //
    // Va en un caso aparte de (20) porque es la dimensión CONTRARIA: (20) mide que la
    // unión no se quede corta y este que no se pase. El fixture del (20) por sí solo no lo
    // media —su único espejo estaba en los dos bloques—, y quitar el filtro entero
    // sobrevivía a la suite. Lo destapó la campaña, no la lectura.
    expect(valoresDe(decisiones()[1])).toEqual(['21', '23', SIN_PLAZA]);
  });
});
