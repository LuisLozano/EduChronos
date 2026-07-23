import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, ParamMap, convertToParamMap } from '@angular/router';
import { Subject } from 'rxjs';

import { HorarioView } from './horario-view';
import { HorarioGrid } from '../horario-grid/horario-grid';
import { HorarioService } from '../../services/horario.service';
import { BloqueoService } from '../../services/bloqueo.service';
import { DiagnosticoService } from '../../services/diagnostico.service';
import { PrevalidacionService } from '../../services/prevalidacion.service';
import { Bloqueo } from '../../models/bloqueo.model';
import { HorarioProyeccion } from '../../models/horario.model';
import { Diagnostico } from '../../models/diagnostico.model';
import { AvisoPrevalidacion } from '../../models/prevalidacion.model';

/**
 * COORDINACIÓN del contenedor, no transporte: los tres colaboradores son dobles
 * por `useValue` con `vi.fn()`, sin `HttpTestingController`. Lo que se mide es
 * quién llama a quién y qué hace el contenedor con la respuesta; que las URLs y
 * los verbos sean los correctos es asunto de los specs de servicio.
 *
 * <p>Los tres dobles usan `Subject` PELADO, nunca `of()` ni `BehaviorSubject`.
 * Emitir a mano es lo que da la mitad "antes" de cada aserto: con un observable
 * que emite al suscribirse, todo habría ocurrido ya cuando `createComponent`
 * retorna y los asertos no podrían distinguir "el componente reaccionó" de "el
 * componente nació así".
 *
 * <p>CAVEAT del `Subject` pelado: el `ActivatedRoute.paramMap` REAL emite al
 * suscribirse, y el doble no. Por eso el aserto (1) mide "una emisión de la ruta
 * ⇒ una carga del índice", NO "un montaje ⇒ una carga": el estado previo a la
 * primera emisión es un artefacto del doble que en producción no existe, y por
 * eso este fichero no asevera sobre él.
 *
 * <p>SIN `provideRouter([])`, a diferencia de `app.spec.ts`: allí hace falta
 * porque `App` es el shell con `router-outlet`; `HorarioView` solo inyecta
 * `ActivatedRoute` y su plantilla no tiene `routerLink` ni outlet, así que
 * añadir el router real metería un colaborador que el componente no usa.
 *
 * <p>`pinadas` es `protected`, y se observa por el input PÚBLICO de la rejilla
 * hija ({@link rejilla}), que es la frontera real del contrato; nunca por un
 * cast `as any` sobre el padre. El `<p class="aviso">` solo da cardinalidad.
 *
 * <p>Zoneless (sin zone.js, sin `provideZoneChangeDetection`): el render se
 * espera con `await fixture.whenStable()`, NUNCA con `detectChanges()`.
 */

/**
 * Bloqueo mínimo: solo importan la actividad, el índice y el id. Duplicado a
 * propósito del helper homónimo de `pines.spec.ts` en vez de extraerlo allí: no
 * se toca un fichero commiteado y verde por dos specs nuevos.
 */
function pin(
  id: number | null,
  actividadCodigo: string,
  indice: number,
  dia: number,
  orden: number,
): Bloqueo {
  return { id, actividadCodigo, indice, tramo: { dia, orden }, aulas: [] };
}

/**
 * Proyección mínima con `sesiones: []`. La rejilla se monta con que
 * `proyeccion()` sea no-null —la cadena `@if` de la plantilla no mira las
 * sesiones—, así que una lista vacía basta para tenerla en el DOM. Poner
 * sesiones aquí solo añadiría una dimensión que estos asertos no miden y abriría
 * la puerta a que alguno pasara por acumulación.
 */
const PROYECCION_VACIA: HorarioProyeccion = {
  id: 1,
  nombre: 'Proyección de prueba',
  estado: 'BORRADOR',
  estadoSolver: 'OPTIMAL',
  objetivo: null,
  cotaInferior: null,
  fechaGeneracion: '2026-07-21T00:00:00Z',
  sesiones: [],
};

describe('contenedor del horario', () => {
  let fixture: ComponentFixture<HorarioView>;
  let sujetoParam: Subject<ParamMap>;
  let sujetoListar: Subject<Bloqueo[]>;
  let sujetoProyeccion: Subject<HorarioProyeccion>;
  let sujetoBorrar: Subject<void>;
  let sujetoDiagnostico: Subject<Diagnostico>;
  let ultimoGuardar: Subject<Bloqueo>;
  let bloqueos: {
    listar: ReturnType<typeof vi.fn>;
    guardar: ReturnType<typeof vi.fn>;
    borrar: ReturnType<typeof vi.fn>;
  };
  let horario: { getProyeccion: ReturnType<typeof vi.fn> };
  let diagnosticos: { getDiagnostico: ReturnType<typeof vi.fn> };
  let sujetoPrevalidacion: Subject<AvisoPrevalidacion[]>;
  let prevalidaciones: { getPrevalidacion: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    sujetoParam = new Subject<ParamMap>();
    sujetoListar = new Subject<Bloqueo[]>();
    sujetoProyeccion = new Subject<HorarioProyeccion>();
    sujetoBorrar = new Subject<void>();
    sujetoDiagnostico = new Subject<Diagnostico>();
    sujetoPrevalidacion = new Subject<AvisoPrevalidacion[]>();

    bloqueos = {
      listar: vi.fn(() => sujetoListar),
      // FRESCO POR INVOCACIÓN, no un Subject compartido como los otros dobles: un
      // Subject que ya emitió `.error()` queda CERRADO, y re-suscribirse a él
      // redispara el error de forma SÍNCRONA. El (25) encadena un alta fallida y
      // otra a continuación; con un sujeto compartido, el segundo `guardar()`
      // devolvería el cerrado y `alSoltar` repoblaría `errorPin` nada más
      // suscribirse, haciendo INOBSERVABLE la fase "errorPin a null antes de
      // responder". Cada llamada estrena Subject y guarda el último en
      // `ultimoGuardar` para poder emitir sobre él.
      guardar: vi.fn(() => (ultimoGuardar = new Subject<Bloqueo>())),
      borrar: vi.fn(() => sujetoBorrar),
    };
    horario = { getProyeccion: vi.fn(() => sujetoProyeccion) };
    // Doble por `useValue`, como el resto: `cargar(id)` lo llama pero estos
    // asertos de pines/proyección no lo hacen emitir; su sujeto queda pendiente
    // sin efecto (badges vacío, la rejilla se monta igual).
    diagnosticos = { getDiagnostico: vi.fn(() => sujetoDiagnostico) };
    // Doble por `useValue`, como el resto: `cargar(id)` lo llama pero estos
    // asertos no lo hacen emitir; su sujeto queda pendiente (el panel muestra la
    // rama pendiente, que no colisiona por DOM con .error/.error-diagnostico/.aviso).
    prevalidaciones = { getPrevalidacion: vi.fn(() => sujetoPrevalidacion) };

    await TestBed.configureTestingModule({
      imports: [HorarioView],
      providers: [
        { provide: ActivatedRoute, useValue: { paramMap: sujetoParam } },
        { provide: HorarioService, useValue: horario },
        { provide: BloqueoService, useValue: bloqueos },
        { provide: DiagnosticoService, useValue: diagnosticos },
        { provide: PrevalidacionService, useValue: prevalidaciones },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HorarioView);
    await fixture.whenStable();
  });

  /** La rejilla hija, cuyo input `pinadas` es la ventana al índice del padre. */
  function rejilla(): HorarioGrid {
    const encontrada = fixture.debugElement.query(By.directive(HorarioGrid));
    if (!encontrada) {
      throw new Error('La rejilla no está montada: ¿se emitió la proyección?');
    }
    return encontrada.componentInstance as HorarioGrid;
  }

  /** Ruta + índice de pines + proyección, en el orden en que llegan de verdad. */
  async function montar(pines: Bloqueo[]): Promise<HorarioGrid> {
    sujetoParam.next(convertToParamMap({ id: '1' }));
    sujetoListar.next(pines);
    sujetoProyeccion.next(PROYECCION_VACIA);
    await fixture.whenStable();
    return rejilla();
  }

  /**
   * El índice es de TODO el horario y se refresca por ruta: una carga por
   * emisión, ni más ni menos. Las dos fases fijan juntas el modelo
   * `llamadas = a + b·emisiones` en `a = 0, b = 1`; una sola fase dejaría vivas
   * las degeneraciones con `a = 1`.
   */
  it('(1) el índice de pines se carga una vez por emisión de la ruta', async () => {
    sujetoParam.next(convertToParamMap({ id: '1' }));
    await fixture.whenStable();
    expect(bloqueos.listar).toHaveBeenCalledTimes(1);

    sujetoParam.next(convertToParamMap({ id: '2' }));
    await fixture.whenStable();
    expect(bloqueos.listar).toHaveBeenCalledTimes(2);
  });

  /**
   * SIN movimiento optimista (D-F8.6-ii-5): el candado sigue pintado hasta el
   * 204. La mitad "antes" es la única que discrimina — un borrado optimista
   * produce EXACTAMENTE el mismo estado final, así que aseverar solo después
   * dejaría la mutación verde.
   */
  it('(2) el pin sale del índice al llegar el 204, no al pedir el DELETE', async () => {
    const grid = await montar([pin(7, 'Mat-1ºA', 1, 1, 2)]);
    expect(grid.pinadas().get('Mat-1ºA|1')).toBe(7);

    grid.despinar.emit('Mat-1ºA|1');
    await fixture.whenStable();

    // ANTES del 204. El DELETE viaja con el ID, no con la clave: resolver
    // clave→id es trabajo del contenedor, la rejilla ignora que los pines lo
    // tengan.
    expect(bloqueos.borrar).toHaveBeenCalledTimes(1);
    expect(bloqueos.borrar).toHaveBeenCalledWith(7);
    expect(grid.pinadas().has('Mat-1ºA|1')).toBe(true);

    sujetoBorrar.next();
    await fixture.whenStable();

    // DESPUÉS del 204.
    expect(grid.pinadas().has('Mat-1ºA|1')).toBe(false);
    expect(grid.pinadas().size).toBe(0);
  });

  /**
   * Un pin con id null es un pin VIVO que no se sabe borrar: se queda pintado y
   * no se emite DELETE. `null` y `undefined` significan cosas distintas, y (3) y
   * (4) separan las dos causas que la guarda del componente colapsa.
   */
  it('(3) un pin presente con id null no emite DELETE y no sale del índice', async () => {
    const grid = await montar([pin(null, 'LCL-1ºA', 1, 3, 4)]);
    expect(grid.pinadas().get('LCL-1ºA|1')).toBeNull();

    grid.despinar.emit('LCL-1ºA|1');
    await fixture.whenStable();

    expect(bloqueos.borrar).not.toHaveBeenCalled();

    // Se emite igualmente en el sujeto del DELETE: si la guarda dejase pasar el
    // null, la suscripción estaría viva y este `next` borraría la clave. Sin esta
    // emisión, "el índice no cambia" quedaría verde bajo la mutación y sería
    // cobertura fingida.
    sujetoBorrar.next();
    await fixture.whenStable();

    expect(grid.pinadas().has('LCL-1ºA|1')).toBe(true);
    expect(grid.pinadas().get('LCL-1ºA|1')).toBeNull();
    expect(grid.pinadas().size).toBe(1);
  });

  it('(4) despinar una clave ausente del índice no emite DELETE y no lo toca', async () => {
    const grid = await montar([pin(7, 'Mat-1ºA', 1, 1, 2)]);

    // Hermana sin pinar: misma actividad, otra repetición. La identidad es la
    // INSTANCIA, así que esta clave NO está en el índice.
    grid.despinar.emit('Mat-1ºA|3');
    await fixture.whenStable();

    expect(bloqueos.borrar).not.toHaveBeenCalled();

    sujetoBorrar.next();
    await fixture.whenStable();

    expect(grid.pinadas().size).toBe(1);
    expect(grid.pinadas().get('Mat-1ºA|1')).toBe(7);
  });

  /**
   * El fallo al listar pines no tumba la vista: se avisa y ya. Se asevera el
   * TEXTO exacto, no la mera presencia de `.error`, porque `errorPin` también lo
   * pinta con el mensaje del servidor cuando falla un alta o una baja: sin fijar
   * el texto, el aserto pasaría por la razón equivocada.
   */
  it('(5) si falla la carga del índice, el aviso lo dice con su texto', async () => {
    sujetoParam.next(convertToParamMap({ id: '1' }));
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;
    expect(raiz.querySelector('.error')).toBeNull();

    sujetoListar.error(new Error('conexión caída'));
    await fixture.whenStable();

    const aviso = raiz.querySelector('.error');
    expect(aviso).not.toBeNull();
    expect(aviso!.textContent?.trim()).toBe('No se pudieron cargar los pines existentes.');
  });

  /**
   * El diagnóstico es POR horario y se pide dentro de `cargar(id)`: una petición
   * por emisión de ruta, con el id de ESA emisión. Las dos fases fijan el modelo
   * `llamadas = a + b·emisiones` en `a = 0, b = 1`. La forma de UNA sola emisión
   * dejaría viva la degeneración del constructor (`a = 1, b = 0`), que llama una
   * vez al nacer y ninguna al cambiar de ruta: por eso hacen falta las dos.
   */
  it('(13) getDiagnostico se pide una vez por emisión de la ruta, con el id de la emisión', async () => {
    sujetoParam.next(convertToParamMap({ id: '1' }));
    await fixture.whenStable();
    expect(diagnosticos.getDiagnostico).toHaveBeenCalledTimes(1);
    expect(diagnosticos.getDiagnostico).toHaveBeenCalledWith(1);

    sujetoParam.next(convertToParamMap({ id: '2' }));
    await fixture.whenStable();
    expect(diagnosticos.getDiagnostico).toHaveBeenCalledTimes(2);
    expect(diagnosticos.getDiagnostico).toHaveBeenCalledWith(2);
  });

  /**
   * Un fallo del diagnóstico NO tumba la vista: la proyección vigente no depende
   * de él. Los tres asertos van juntos porque cada uno mata una fuga distinta:
   * vaciar la rejilla (reusar `error`), silenciar el fallo (no poblar
   * `errorDiagnostico`) y confundir las señales (pintar el aviso bajo `.error`).
   * `.error` cubre a la vez `error` y `errorPin` —comparten clase—: aquí ambos
   * están vacíos, así que su ausencia es inequívoca.
   */
  it('(14) si getDiagnostico falla: la proyección sigue en pie, .error ausente y el aviso propio presente', async () => {
    sujetoParam.next(convertToParamMap({ id: '1' }));
    sujetoListar.next([]);
    sujetoProyeccion.next(PROYECCION_VACIA);
    await fixture.whenStable();

    sujetoDiagnostico.error(new Error('diagnóstico caído'));
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;
    // (1) proyección poblada ⇒ la rejilla sigue montada (la plantilla la gatea
    // con `proyeccion()` en el `@else if`).
    expect(fixture.debugElement.query(By.directive(HorarioGrid))).not.toBeNull();
    // (2) el fallo del diagnóstico NO es el fallo de la proyección ni del pin.
    expect(raiz.querySelector('.error')).toBeNull();
    // (3) su aviso propio, con su texto y su clase.
    const aviso = raiz.querySelector('.error-diagnostico');
    expect(aviso).not.toBeNull();
    expect(aviso!.textContent?.trim()).toBe('No se pudo cargar el diagnóstico.');
  });

  /**
   * WIRING, no lógica: que el `computed` liga `indiceViolaciones(d.violaciones)`
   * al input de la rejilla. Una violación con DOS celdas de instancias distintas
   * ⇒ el índice tiene 2 claves, y eso es lo que debe ver la hija. Se lee por el
   * input público de la rejilla ({@link rejilla} vía `By.directive`), no por cast
   * sobre el padre.
   *
   * <p>La mutación que mata este test es de CABLEADO: el `computed` que no liga y
   * devuelve `new Map()` ⇒ 0 claves. NO "indexar por violación en vez de por
   * celda": esa es lógica pura y ya la mata `diagnostico.spec (2)`; ponerla aquí
   * sería cobertura fingida.
   */
  it('(20) el input violaciones de la rejilla se cabla desde el diagnóstico (dos instancias ⇒ dos claves)', async () => {
    const grid = await montar([]);

    const diag: Diagnostico = {
      violaciones: [
        {
          regla: 'SOLAPE_AULA',
          recursoCodigo: null,
          tramoCodigo: null,
          celdas: [
            { actividadCodigo: 'Mat-1ºA', indice: 1, plazaCodigo: 'Mat-1ºA-P1' },
            { actividadCodigo: 'LCL-1ºA', indice: 2, plazaCodigo: 'LCL-1ºA-P1' },
          ],
          descripcion: 'una violación, dos instancias',
        },
      ],
      penalizaciones: [],
      totales: { ventanas: 0, consecutivas: 0, indispBlanda: 0 },
    };
    sujetoDiagnostico.next(diag);
    await fixture.whenStable();

    // Indexado por CELDA: la única violación aparece bajo sus dos instancias.
    expect(grid.violaciones().size).toBe(2);
  });

  /**
   * El cuerpo del POST se CONSTRUYE en el contenedor a partir de la suelta: la
   * rejilla emite la INSTANCIA y el tramo destino, y aquí se arma el
   * `BloqueoRequest` con `aulas: []` —la suelta fija solo el TRAMO (D-5)—. El
   * objeto esperado va LITERAL, nunca compuesto desde la suelta: componerlo desde
   * `s` volvería circular el aserto. `dia = 3` y `orden = 4` son DISTINTOS a
   * propósito —con `dia === orden` la permutación `{dia:s.orden, orden:s.dia}`
   * (M21) saldría idéntica y quedaría sin medir—. No se emite respuesta: lo que
   * mide este test es el argumento de la llamada, no la reacción al next.
   */
  it('(21) el cuerpo del POST se arma desde la suelta con aulas vacías y el tramo sin permutar', async () => {
    const grid = await montar([]);

    grid.soltar.emit({ actividadCodigo: 'Mat-1ºA', indice: 2, dia: 3, orden: 4 });
    await fixture.whenStable();

    expect(bloqueos.guardar).toHaveBeenCalledTimes(1);
    expect(bloqueos.guardar).toHaveBeenCalledWith({
      actividadCodigo: 'Mat-1ºA',
      indice: 2,
      tramo: { dia: 3, orden: 4 },
      aulas: [],
    });
  });

  /**
   * La clave del índice se toma de la RESPUESTA del POST (`b`), no de la suelta
   * (`s`): el backend es la autoridad sobre qué instancia quedó pinada. Para que
   * la mutación `clavePin(b...)` → `clavePin(s...)` (M22) tenga víctima, la
   * respuesta DIVERGE de la suelta en las dos dimensiones de la clave.
   *
   * <p>FIXTURE DEFENSIVO DECLARADO: en producción el backend devuelve lo que
   * recibe, así que esta divergencia (suelta `Mat-1ºA|2`, respuesta `LCL-1ºA|1`)
   * es imposible. Es deliberada: sin ella, `s` y `b` coincidirían y la mutación
   * quedaría verde. Mismo recurso que el it (9) de `diagnostico.spec` (S82). Se
   * aseveran las DOS mitades: la clave de la respuesta presente con su valor, y
   * la de la suelta ausente.
   */
  it('(22) la clave del índice sale de la respuesta del POST, no de la suelta', async () => {
    const grid = await montar([]);

    grid.soltar.emit({ actividadCodigo: 'Mat-1ºA', indice: 2, dia: 3, orden: 4 });
    await fixture.whenStable();

    ultimoGuardar.next({
      id: 9,
      actividadCodigo: 'LCL-1ºA',
      indice: 1,
      tramo: { dia: 3, orden: 4 },
      aulas: [],
    });
    await fixture.whenStable();

    expect(grid.pinadas().get('LCL-1ºA|1')).toBe(9);
    expect(grid.pinadas().has('Mat-1ºA|2')).toBe(false);
  });

  /**
   * SIN alta optimista: el candado aparece al llegar la respuesta del POST, no al
   * emitir la suelta. La mitad "antes" es la única que discrimina —un alta
   * optimista produce el MISMO estado final—, y por eso el sujeto del `guardar`
   * NO se emite hasta haber comprobado el 0. Mata M23 (poblar `pinadas` antes del
   * subscribe), que dejaría el tamaño en 1 ya en la fase "antes".
   */
  it('(23) el pin entra en el índice al llegar la respuesta, no al emitir la suelta', async () => {
    const grid = await montar([]);

    grid.soltar.emit({ actividadCodigo: 'Mat-1ºA', indice: 2, dia: 3, orden: 4 });
    await fixture.whenStable();
    // ANTES de la respuesta: nada pinado.
    expect(grid.pinadas().size).toBe(0);

    ultimoGuardar.next({
      id: 9,
      actividadCodigo: 'Mat-1ºA',
      indice: 2,
      tramo: { dia: 3, orden: 4 },
      aulas: [],
    });
    await fixture.whenStable();
    // DESPUÉS: el único pin.
    expect(grid.pinadas().size).toBe(1);
  });

  /**
   * Un POST que falla NO pina y su mensaje se DEGRADA cuando el body no trae
   * `message` ni `error` (hoy `server.error.include-message` está desactivado).
   * Se asevera el TEXTO EXACTO, no la mera presencia de `.error`: mata M24
   * (`return cuerpo?.message ?? ''`), que dejaría el aviso vacío.
   *
   * <p>Se lee por el `<p class="error">`: aquí la proyección va OK (`error()` es
   * null) y no hay fallo de diagnóstico, así que ese párrafo es inequívocamente
   * `errorPin` —mismo razonamiento que el (5)—. El body es `{}` (ni `message` ni
   * `error`) para forzar la rama del degradado, y `pinadas` sigue en 0.
   */
  it('(24) un POST que falla no pina y el mensaje se degrada a estado cuando el body va vacío', async () => {
    const grid = await montar([]);

    grid.soltar.emit({ actividadCodigo: 'Mat-1ºA', indice: 2, dia: 3, orden: 4 });
    await fixture.whenStable();

    ultimoGuardar.error({ status: 400, error: {} });
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;
    const aviso = raiz.querySelector('.error');
    expect(aviso).not.toBeNull();
    expect(aviso!.textContent?.trim()).toBe('El servidor rechazó el pin (400).');
    expect(grid.pinadas().size).toBe(0);
  });

  /**
   * Dos invariantes del ciclo de pinado en un test:
   *
   * <p>(a) Tras un alta OK, la proyección NO se recarga: `getProyeccion` sigue en
   * la ÚNICA llamada del montaje. El pin es una restricción para la PRÓXIMA
   * generación, no un movimiento del horario vigente (TSDoc de la clase). Mata
   * M25 (`this.cargar(1)` en el next), que dispararía un segundo `getProyeccion`.
   *
   * <p>(b) Un segundo `soltar` LIMPIA `errorPin` antes de que su POST responda:
   * `alSoltar` hace `errorPin.set(null)` en su primera línea. Se comprueba con el
   * segundo sujeto AÚN sin emitir. Mata M25b (quitar ese `set(null)`), que
   * dejaría el aviso del fallo anterior pintado.
   *
   * <p>Aquí es donde el `guardar` FRESCO POR INVOCACIÓN es imprescindible: el
   * sujeto del alta fallida queda cerrado tras `.error()`, y solo un sujeto nuevo
   * en el segundo intento evita que re-suscribirse redispare el error y repueble
   * `errorPin`, lo que haría inobservable la fase "a null".
   */
  it('(25) el alta OK no recarga la proyección, y un nuevo intento limpia el error previo antes de responder', async () => {
    const grid = await montar([]);
    expect(horario.getProyeccion).toHaveBeenCalledTimes(1);

    // (a) alta OK: la proyección no se recarga.
    grid.soltar.emit({ actividadCodigo: 'Mat-1ºA', indice: 2, dia: 3, orden: 4 });
    await fixture.whenStable();
    ultimoGuardar.next({
      id: 9,
      actividadCodigo: 'Mat-1ºA',
      indice: 2,
      tramo: { dia: 3, orden: 4 },
      aulas: [],
    });
    await fixture.whenStable();
    expect(horario.getProyeccion).toHaveBeenCalledTimes(1);

    // (b) un alta que falla puebla el aviso...
    const raiz = fixture.nativeElement as HTMLElement;
    grid.soltar.emit({ actividadCodigo: 'LCL-1ºA', indice: 1, dia: 2, orden: 5 });
    await fixture.whenStable();
    ultimoGuardar.error({ status: 400, error: {} });
    await fixture.whenStable();
    expect(raiz.querySelector('.error')).not.toBeNull();

    // ...y el siguiente intento lo limpia ANTES de que su POST responda.
    grid.soltar.emit({ actividadCodigo: 'Mat-1ºA', indice: 3, dia: 1, orden: 1 });
    await fixture.whenStable();
    expect(raiz.querySelector('.error')).toBeNull();
  });

  /**
   * El alta PRESERVA el índice previo: se añade la clave nueva sin borrar las que
   * ya estaban. Es el único test del camino de alta que arranca con `pinadas` NO
   * vacío —(22) y (23) parten de vacío, donde "mapa copiado" y "mapa desde cero"
   * dan idéntico resultado—, así que es el único que mata M26 (`new Map()` en vez
   * de `new Map(this.pinadas())`), que descartaría el pin preexistente al añadir
   * el nuevo.
   */
  it('(26) el alta preserva los pines previos del índice', async () => {
    const grid = await montar([pin(7, 'Mat-1ºA', 1, 1, 2)]);
    expect(grid.pinadas().get('Mat-1ºA|1')).toBe(7);

    grid.soltar.emit({ actividadCodigo: 'LCL-1ºA', indice: 1, dia: 3, orden: 4 });
    await fixture.whenStable();
    ultimoGuardar.next({
      id: 9,
      actividadCodigo: 'LCL-1ºA',
      indice: 1,
      tramo: { dia: 3, orden: 4 },
      aulas: [],
    });
    await fixture.whenStable();

    expect(grid.pinadas().size).toBe(2);
    expect(grid.pinadas().get('Mat-1ºA|1')).toBe(7);
    expect(grid.pinadas().get('LCL-1ºA|1')).toBe(9);
  });
});
