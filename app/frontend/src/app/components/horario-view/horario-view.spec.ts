import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, ParamMap, Router, convertToParamMap } from '@angular/router';
import { Dialog } from '@angular/cdk/dialog';
import { Subject } from 'rxjs';

import { HorarioView } from './horario-view';
import { HorarioGrid } from '../horario-grid/horario-grid';
import { ConfirmarGeneracion } from '../confirmar-generacion/confirmar-generacion';
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
 * <p>`Router` y `Dialog` son DOBLES por `useValue`, no el router/overlay reales,
 * a diferencia de `app.spec.ts` que usa `provideRouter([])`. `HorarioView` sí usa
 * ambos —`generar()` navega al horario nuevo y abre el diálogo de confirmación—,
 * pero aquí se miden como colaboradores (`navigate`/`open` espiados), no se ejerce
 * la navegación real ni el overlay del CDK: eso metería infraestructura que estos
 * asertos de coordinación no necesitan. El `Router` real, además, no está
 * cableado al `paramMap` doble, así que `navigate` no redispara `cargar`.
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
  let sujetoDiagnostico: Subject<Diagnostico>;
  let ultimoGuardar: Subject<Bloqueo>;
  let ultimoBorrar: Subject<void>;
  let ultimoGenerar: Subject<HorarioProyeccion>;
  let bloqueos: {
    listar: ReturnType<typeof vi.fn>;
    guardar: ReturnType<typeof vi.fn>;
    borrar: ReturnType<typeof vi.fn>;
  };
  let horario: { getProyeccion: ReturnType<typeof vi.fn>; generar: ReturnType<typeof vi.fn> };
  let diagnosticos: { getDiagnostico: ReturnType<typeof vi.fn> };
  let sujetoPrevalidacion: Subject<AvisoPrevalidacion[]>;
  let prevalidaciones: { getPrevalidacion: ReturnType<typeof vi.fn> };
  let sujetoCerrado: Subject<boolean | undefined>;
  let dialog: { open: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    sujetoParam = new Subject<ParamMap>();
    sujetoListar = new Subject<Bloqueo[]>();
    sujetoProyeccion = new Subject<HorarioProyeccion>();
    sujetoDiagnostico = new Subject<Diagnostico>();
    sujetoPrevalidacion = new Subject<AvisoPrevalidacion[]>();
    sujetoCerrado = new Subject<boolean | undefined>();

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
      // FRESCO POR INVOCACIÓN, por el mismo motivo que `guardar` (ver comentario
      // arriba): el reintento de despinado (36) re-suscribe tras un `.error()`, y
      // un Subject compartido cerrado redispararía el error síncronamente.
      borrar: vi.fn(() => (ultimoBorrar = new Subject<void>())),
    };
    horario = {
      getProyeccion: vi.fn(() => sujetoProyeccion),
      // FRESCO POR INVOCACIÓN, por el mismo motivo que `guardar`: el reintento de
      // generación (35) re-suscribe tras un `.error()` sobre el Subject anterior.
      generar: vi.fn(() => (ultimoGenerar = new Subject<HorarioProyeccion>())),
    };
    // Doble del Dialog del CDK: `open` devuelve un objeto con `closed`, el único
    // miembro que `generar()` toca. El Subject de cierre es COMPARTIDO —cada test
    // abre a lo sumo una vez—, y emitir a mano da la fase "antes de confirmar".
    dialog = { open: vi.fn(() => ({ closed: sujetoCerrado })) };
    // Doble del Router: solo se espía `navigate`. No está cableado al `paramMap`
    // doble, así que navegar NO redispara `cargar` (ver cabecero).
    router = { navigate: vi.fn() };
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
        { provide: Router, useValue: router },
        { provide: Dialog, useValue: dialog },
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

    ultimoBorrar.next();
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

    // No hay emisión del DELETE: con el doble FRESCO POR INVOCACIÓN, `borrar` no se
    // invocó (la guarda retorna con id null), así que no existe Subject sobre el
    // que emitir. La mutación que dejase pasar el null la mata directamente el
    // `not.toHaveBeenCalled` de arriba —borrar(null) habría llamado al doble—.
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

    // Sin emisión del DELETE: `borrar` no se invocó (clave ausente del índice), así
    // que no hay Subject fresco sobre el que emitir. El `not.toHaveBeenCalled` de
    // arriba es el discriminador.
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

  // --- Gesto de generar (Fase 8) ---------------------------------------------

  /** Un aviso ERROR: condena la generación, exige confirmación. */
  const AVISO_ERROR: AvisoPrevalidacion = {
    severidad: 'ERROR',
    regla: 'DEMANDA_INSATISFACIBLE',
    entidadCodigo: 'MAT1',
    demanda: 31,
    disponible: 30,
    descripcion: 'MAT1 necesita 31 tramos y dispone de 30',
  };

  /** Un aviso NO-ERROR: no condena nada, la generación procede sin diálogo. */
  const AVISO_NO_ERROR: AvisoPrevalidacion = {
    severidad: 'AVISO',
    regla: 'HOLGURA_JUSTA',
    entidadCodigo: 'LCL1',
    demanda: 20,
    disponible: 20,
    descripcion: 'LCL1 ajusta demanda y disponibilidad',
  };

  /**
   * Monta la vista Y emite la pre-validación con los avisos dados, que es lo que
   * habilita el botón «Generar». Sin esta emisión `avisosPrevalidacion()` sigue en
   * `null` y el botón está deshabilitado —el caso de la guarda, no del gesto—.
   */
  async function montarConPrevalidacion(avisos: AvisoPrevalidacion[]): Promise<void> {
    sujetoParam.next(convertToParamMap({ id: '1' }));
    sujetoListar.next([]);
    sujetoPrevalidacion.next(avisos);
    sujetoProyeccion.next(PROYECCION_VACIA);
    await fixture.whenStable();
  }

  /** Pulsa el botón «Generar» por el DOM, la frontera real del gesto. */
  function pulsarGenerar(): void {
    const boton = (fixture.nativeElement as HTMLElement).querySelector(
      'button.generar',
    ) as HTMLButtonElement;
    if (!boton) {
      throw new Error('El botón de generar no está en el DOM.');
    }
    boton.click();
  }

  /**
   * Pre-validación SIN ningún ERROR: la generación procede directa, sin diálogo.
   * Las DOS mitades discriminan: `generar` recibe EXACTAMENTE 1 (no 0: la mutación
   * que exige confirmación siempre) y `Dialog.open` recibe 0 (no ≥1: la mutación
   * que abre el diálogo pase lo que pase). El fixture tiene un AVISO no vacío para
   * que «sin ERROR» no sea «sin avisos»: separa `filter(sev==='ERROR')` de
   * `avisos.length > 0`.
   */
  it('(27) sin ERROR en la pre-validación, generar procede directo: 1 al servicio, 0 al diálogo', async () => {
    await montarConPrevalidacion([AVISO_NO_ERROR]);

    pulsarGenerar();
    await fixture.whenStable();

    expect(horario.generar).toHaveBeenCalledTimes(1);
    expect(dialog.open).toHaveBeenCalledTimes(0);
  });

  /**
   * Con al menos un ERROR se abre el diálogo y NO se llama al backend hasta que el
   * cierre lo confirme. La mitad "antes de cerrar" es la única discriminante para
   * ese punto: sin ella, una implementación que generara Y abriera el diálogo
   * pasaría. El sujeto de cierre NO se emite en este test a propósito.
   *
   * <p>Además fija el `data` del diálogo (hueco 2): el fixture lleva DOS avisos con
   * textos distintos, uno ERROR y uno AVISO, y al diálogo llega SOLO el ERROR
   * (`{ data: [AVISO_ERROR] }`). El AVISO no-ERROR en el fixture es lo que separa
   * "pasa la lista entera" de "filtra": sin él, `[AVISO_ERROR]` y "todo" coinciden.
   * Con `data: []` (no pasar nada) también cae.
   */
  it('(28) con un ERROR, abre el diálogo con SOLO los errores y no llama al backend hasta el cierre', async () => {
    await montarConPrevalidacion([AVISO_ERROR, AVISO_NO_ERROR]);

    pulsarGenerar();
    await fixture.whenStable();

    expect(dialog.open).toHaveBeenCalledTimes(1);
    expect(dialog.open).toHaveBeenCalledWith(ConfirmarGeneracion, { data: [AVISO_ERROR] });
    expect(horario.generar).toHaveBeenCalledTimes(0);
  });

  /**
   * Cierre por backdrop/Escape (emite `undefined`): la generación NO procede. Se
   * usa `undefined` y no `false` porque es el valor que mata la mutación
   * `confirmado !== false` (que dejaría pasar el `undefined` del backdrop); con
   * `false` esa mutación quedaría verde.
   */
  it('(29) diálogo cerrado por backdrop (undefined) no llama al backend', async () => {
    await montarConPrevalidacion([AVISO_ERROR]);

    pulsarGenerar();
    await fixture.whenStable();
    expect(horario.generar).toHaveBeenCalledTimes(0);

    sujetoCerrado.next(undefined);
    await fixture.whenStable();

    expect(horario.generar).toHaveBeenCalledTimes(0);
  });

  /**
   * Confirmado con `true`: ahí sí procede la generación, UNA vez. Es el gemelo de
   * (29): mismo montaje, cierre opuesto, resultado opuesto. Juntos fijan que la
   * condición del cierre es exactamente `=== true`.
   */
  it('(30) diálogo confirmado (true) llama al backend una vez', async () => {
    await montarConPrevalidacion([AVISO_ERROR]);

    pulsarGenerar();
    await fixture.whenStable();

    sujetoCerrado.next(true);
    await fixture.whenStable();

    expect(horario.generar).toHaveBeenCalledTimes(1);
  });

  /**
   * Tras un 200, se navega a la ruta del horario DEVUELTO. El array esperado va
   * LITERAL (`['/horario', 99]`), nunca compuesto desde `dto`: componerlo volvería
   * circular el aserto. El id 99 es DISTINTO del id 1 de la ruta del fixture —con
   * el mismo id, navegar a la ruta vigente sería indistinguible de no navegar, y
   * la mutación que compone la ruta con el id de `route` quedaría verde—. El path
   * es `/horario` singular (S-medido), no `/horarios`.
   */
  it('(31) tras el 200, navega a ["/horario", id] con el id de la respuesta', async () => {
    await montarConPrevalidacion([AVISO_NO_ERROR]);

    pulsarGenerar();
    await fixture.whenStable();

    ultimoGenerar.next({ ...PROYECCION_VACIA, id: 99 });
    await fixture.whenStable();

    expect(router.navigate).toHaveBeenCalledTimes(1);
    expect(router.navigate).toHaveBeenCalledWith(['/horario', 99]);
  });

  /**
   * Un POST de generación que falla puebla `errorGeneracion` (su clase propia
   * `.error-generacion`) y NO vacía la rejilla: la proyección vigente sigue
   * montada. Misma disciplina que el (14) del diagnóstico y que `errorPrevalidacion`
   * (S92). Se comprueba a la vez que `.error` (error/errorPin) sigue ausente: el
   * fallo de generación no se confunde con los otros. El body va `{}` para forzar
   * el degradado de `mensaje()`.
   */
  it('(32) un POST de generación fallido puebla su aviso propio y no vacía la rejilla', async () => {
    await montarConPrevalidacion([AVISO_NO_ERROR]);

    pulsarGenerar();
    await fixture.whenStable();

    ultimoGenerar.error({ status: 422, error: {} });
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;
    // (1) la rejilla sigue en pie: la proyección no se tocó.
    expect(fixture.debugElement.query(By.directive(HorarioGrid))).not.toBeNull();
    // (2) su aviso propio, con su clase y su texto degradado.
    const aviso = raiz.querySelector('.error-generacion');
    expect(aviso).not.toBeNull();
    expect(aviso!.textContent?.trim()).toBe('El servidor rechazó el pin (422).');
    // (3) NO se confunde con error/errorPin (comparten `.error`).
    expect(raiz.querySelector('.error')).toBeNull();
  });

  /**
   * La rama "no ejecutado" (hueco 1): sin pre-validación emitida,
   * `avisosPrevalidacion()` sigue en `null`. Los tres asertos atacan DOS
   * mecanismos independientes y por eso van juntos —borrar uno solo debe poner el
   * test rojo—:
   *
   * <p>(a) el binding `[disabled]="avisosPrevalidacion() === null"` del `<button>`;
   * quitarlo o invertirlo deja `disabled` en `false`.
   *
   * <p>(b)+(c) la guarda `if (avisos === null) return` del método. Se invoca el
   * gesto A MANO, no por el DOM: el botón está deshabilitado —que es justo lo que
   * asevera (a)—, así que la vía del click no puede llegar a `generar()`, y solo la
   * llamada directa ejercita la guarda. Es la ÚNICA excepción de este fichero al
   * "observar por la frontera pública": aquí no se observa estado protegido con un
   * cast, se DISPARA el gesto, que no tiene otra frontera cuando el botón está
   * cerrado. Sin guarda, `avisos.filter` sobre `null` reventaría; con una guarda
   * mutada a `!== null` se colaría y subiría `open` o `generar`.
   */
  it('(34) sin pre-validar: el botón está deshabilitado y el gesto no dispara backend ni diálogo', async () => {
    // Montaje SIN emitir pre-validación: avisosPrevalidacion() queda en null.
    sujetoParam.next(convertToParamMap({ id: '1' }));
    sujetoListar.next([]);
    sujetoProyeccion.next(PROYECCION_VACIA);
    await fixture.whenStable();

    const boton = (fixture.nativeElement as HTMLElement).querySelector(
      'button.generar',
    ) as HTMLButtonElement;
    // (a) el binding.
    expect(boton.disabled).toBe(true);

    // (b)+(c) la guarda del método, invocada directamente.
    (fixture.componentInstance as unknown as { generar(): void }).generar();
    await fixture.whenStable();

    expect(horario.generar).toHaveBeenCalledTimes(0);
    expect(dialog.open).toHaveBeenCalledTimes(0);
  });

  /**
   * REINTENTO de generación: un segundo gesto limpia `errorGeneracion` ANTES de
   * que su POST responda (`lanzarGeneracion` hace `errorGeneracion.set(null)` en su
   * primera línea, horario-view.ts:295). La fase "a null antes de responder" es la
   * discriminante —el estado final tras un segundo fallo sería idéntico con o sin
   * ese `set(null)`—, y por eso el segundo Subject NO se emite hasta comprobar el
   * null. La pre-validación va SIN ERROR para entrar por la vía directa
   * (horario-view.ts:285) sin abrir diálogo. Aquí es imprescindible el `generar`
   * FRESCO POR INVOCACIÓN: con un Subject compartido, el segundo `generar()`
   * devolvería el cerrado tras `.error()` y re-suscribirse repoblaría
   * `errorGeneracion` síncronamente, haciendo INOBSERVABLE la fase "a null".
   */
  it('(35) el reintento de generación limpia el error previo antes de responder', async () => {
    await montarConPrevalidacion([AVISO_NO_ERROR]);
    const raiz = fixture.nativeElement as HTMLElement;

    // Primer intento: falla y puebla el aviso propio de generación.
    pulsarGenerar();
    await fixture.whenStable();
    ultimoGenerar.error({ status: 500 });
    await fixture.whenStable();

    // ASERTO A: el aviso de generación está poblado.
    expect(raiz.querySelector('.error-generacion')).not.toBeNull();

    // Segundo intento: limpia el error ANTES de que su POST responda (sin emitir).
    pulsarGenerar();
    await fixture.whenStable();

    // ASERTO B (discriminante): el aviso ya no está.
    expect(raiz.querySelector('.error-generacion')).toBeNull();

    // Cierre: el segundo Subject también falla; el test ya midió lo que importaba.
    ultimoGenerar.error({ status: 500 });
    await fixture.whenStable();
  });

  /**
   * REINTENTO de despinado: gemelo del (35) para `alDespinar`. El segundo gesto
   * limpia `errorPin` ANTES de que su DELETE responda (`alDespinar` hace
   * `errorPin.set(null)` en su primera línea, horario-view.ts:236). Depende de que
   * el pin SIGA en el índice tras el primer fallo: `alDespinar` en su rama de error
   * solo puebla `errorPin`, no toca `pinadas` (horario-view.ts:247), así que el
   * candado permanece y el segundo gesto vuelve a resolver el id en vez de salir por
   * el `return` de la guarda (horario-view.ts:239). Imprescindible el `borrar`
   * FRESCO POR INVOCACIÓN, por el mismo motivo que (35).
   */
  it('(36) el reintento de despinado limpia el error previo antes de responder', async () => {
    const grid = await montar([pin(7, 'Mat-1ºA', 1, 1, 2)]);
    const raiz = fixture.nativeElement as HTMLElement;

    // Primer intento: falla y puebla errorPin (`.error`, inequívoco aquí como en el
    // (24): proyección OK y sin fallo de diagnóstico).
    grid.despinar.emit('Mat-1ºA|1');
    await fixture.whenStable();
    ultimoBorrar.error({ status: 500 });
    await fixture.whenStable();

    // ASERTO A: el aviso de pin está poblado.
    expect(raiz.querySelector('.error')).not.toBeNull();

    // ASERTO A-bis: el fallo NO sacó el pin del índice —si lo hubiera hecho, el
    // segundo gesto saldría por el `return` de la guarda y mediríamos un no-op—.
    expect(grid.pinadas().has('Mat-1ºA|1')).toBe(true);
    expect(grid.pinadas().get('Mat-1ºA|1')).toBe(7);

    // Segundo intento: limpia el error ANTES de que su DELETE responda (sin emitir).
    grid.despinar.emit('Mat-1ºA|1');
    await fixture.whenStable();

    // ASERTO B (discriminante): el aviso ya no está.
    expect(raiz.querySelector('.error')).toBeNull();

    // Cierre: el segundo Subject también falla; el test ya midió lo que importaba.
    ultimoBorrar.error({ status: 500 });
    await fixture.whenStable();
  });
});
