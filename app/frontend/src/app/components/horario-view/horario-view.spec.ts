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
import { AjusteService } from '../../services/ajuste.service';
import { DiagnosticoService } from '../../services/diagnostico.service';
import { PrevalidacionService } from '../../services/prevalidacion.service';
import { JornadaService } from '../../services/jornada.service';
import { Bloqueo } from '../../models/bloqueo.model';
import { HorarioProyeccion, SesionVista } from '../../models/horario.model';
import { IntercambioRealizado } from '../../models/ajuste.model';
import { InstanciaCelda } from '../../horario/proyeccion';
import { Diagnostico } from '../../models/diagnostico.model';
import { AvisoPrevalidacion } from '../../models/prevalidacion.model';
import { JornadaDTO } from '../../models/jornada.model';

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
  let ultimoListar: Subject<Bloqueo[]>;
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
  let sujetoJornada: Subject<JornadaDTO>;
  let jornadas: { obtener: ReturnType<typeof vi.fn> };
  let ultimoCerrado: Subject<boolean | undefined>;
  let ultimoMover: Subject<SesionVista[]>;
  let ultimoIntercambiar: Subject<IntercambioRealizado>;
  let ajustes: { mover: ReturnType<typeof vi.fn>; intercambiar: ReturnType<typeof vi.fn> };
  let dialog: { open: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    sujetoParam = new Subject<ParamMap>();
    sujetoProyeccion = new Subject<HorarioProyeccion>();
    sujetoDiagnostico = new Subject<Diagnostico>();
    sujetoPrevalidacion = new Subject<AvisoPrevalidacion[]>();
    sujetoJornada = new Subject<JornadaDTO>();

    bloqueos = {
      // FRESCO POR INVOCACIÓN guardado en `ultimoListar`, misma forma que
      // `guardar` (ver comentario abajo): un Subject compartido cerrado por
      // `.error()` redispara SÍNCRONAMENTE al re-suscribirse, así que el primer
      // test que encadene fallo→reintento sobre `cargarPines` (dos emisiones de
      // ruta) lo encontraría inescribible. Ningún test lo reintenta aún; se
      // homogeneiza con los otros tres dobles del contenedor (D-F8.6-ivD-b).
      listar: vi.fn(() => (ultimoListar = new Subject<Bloqueo[]>())),
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
    // miembro que `generar()` toca. Emitir a mano da la fase "antes de confirmar".
    //
    // FRESCO POR INVOCACIÓN desde S145, no compartido: ahora el diálogo se abre en
    // TODA generación, así que un test que genere dos veces (el reintento, (35))
    // abre dos veces y deja DOS suscripciones vivas sobre el mismo Subject. Con uno
    // compartido, el segundo `next(true)` dispararía también la primera y
    // `lanzarGeneracion` correría dos veces por una sola confirmación: los conteos
    // `toHaveBeenCalledTimes` medirían el doble sin que la implementación falle.
    dialog = { open: vi.fn(() => ({ closed: (ultimoCerrado = new Subject<boolean | undefined>()) })) };
    // Doble del servicio de ajuste. FRESCO POR INVOCACIÓN, por la misma razón que
    // `guardar`: un Subject cerrado por `.error()` redispara síncronamente al
    // re-suscribirse, y los tests del rechazo encadenan intento fallido → intento
    // siguiente.
    ajustes = {
      mover: vi.fn(() => (ultimoMover = new Subject<SesionVista[]>())),
      intercambiar: vi.fn(() => (ultimoIntercambiar = new Subject<IntercambioRealizado>())),
    };
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
    // Doble por `useValue`, como el resto. SIN doble, el contenedor construiría el
    // JornadaService real, que inyecta HttpClient y no está en este TestBed: caerían
    // los 49 casos del fichero en el beforeEach, no uno.
    jornadas = { obtener: vi.fn(() => sujetoJornada) };

    await TestBed.configureTestingModule({
      imports: [HorarioView],
      providers: [
        { provide: ActivatedRoute, useValue: { paramMap: sujetoParam } },
        { provide: Router, useValue: router },
        { provide: Dialog, useValue: dialog },
        { provide: HorarioService, useValue: horario },
        { provide: BloqueoService, useValue: bloqueos },
        { provide: AjusteService, useValue: ajustes },
        { provide: DiagnosticoService, useValue: diagnosticos },
        { provide: PrevalidacionService, useValue: prevalidaciones },
        { provide: JornadaService, useValue: jornadas },
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
    ultimoListar.next(pines);
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

    ultimoListar.error(new Error('conexión caída'));
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
    ultimoListar.next([]);
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

  // --- Gesto del PIN, en los dos sentidos (S145) ------------------------------
  //
  // El sentido de QUITAR ya estaba cubierto por (2)-(4) y no se toca. Lo que se
  // añade aquí es el de PONER, que S145 devolvió al producto con el candado como
  // interruptor: al pasar el arrastre a ajustar el horario, `guardar` se quedó sin
  // llamador y crear pines dejó de ser posible desde la interfaz.

  /**
   * El cuerpo del POST se arma en el contenedor a partir de la CLAVE que emite la
   * rejilla y del TRAMO que la rejilla NO manda: se resuelve contra la proyección
   * vigente. El objeto esperado va LITERAL, nunca compuesto desde la fila:
   * componerlo volvería circular el aserto.
   *
   * <p>El fixture tiene DOS instancias de la misma actividad en tramos distintos,
   * y se pina la del índice 2: eso es lo que separa "resuelve por la clave entera"
   * de "coge la primera fila de esa actividad", que daría el tramo de la hermana.
   * `dia` (3) y `orden` (4) son distintos entre sí para que una permutación de los
   * dos no salga idéntica.
   */
  it('(72) pinar arma el POST con el tramo que la instancia ocupa en la proyección', async () => {
    const grid = await montarConSesiones([
      fila(1, 'Mat-1ºA', 1, 5, 6),
      fila(2, 'Mat-1ºA', 2, 3, 4),
    ]);

    grid.pinar.emit('Mat-1ºA|2');
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
   * Tras el 200 la instancia entra en el índice y el aviso de pines CRECE. Ese
   * crecimiento es el síntoma de la capacidad recuperada: hasta este cambio el
   * contador solo podía menguar, porque nada llamaba a `guardar`.
   *
   * <p>SIN alta optimista: la mitad "antes" es la única que discrimina —un alta
   * optimista produce el MISMO estado final—, y por eso el sujeto no se emite hasta
   * haber comprobado el 0. El fixture arranca con un pin previo para que el aserto
   * mida además que el índice se PRESERVA: con `pinadas` vacío, "mapa copiado" y
   * "mapa desde cero" darían lo mismo.
   */
  it('(73) el pin entra en el índice al llegar la respuesta, y el aviso de pines crece', async () => {
    sujetoParam.next(convertToParamMap({ id: '1' }));
    ultimoListar.next([pin(7, 'Mat-1ºA', 1, 5, 6)]);
    sujetoProyeccion.next({
      ...PROYECCION_VACIA,
      sesiones: [fila(1, 'Mat-1ºA', 1, 5, 6), fila(2, 'LCL-1ºA', 1, 3, 4)],
    });
    await fixture.whenStable();
    const grid = rejilla();

    const raiz = fixture.nativeElement as HTMLElement;
    expect(raiz.querySelector('.aviso')?.textContent?.trim()).toBe('1 pines sin aplicar — regenerar');

    grid.pinar.emit('LCL-1ºA|1');
    await fixture.whenStable();

    // ANTES de la respuesta: el candado no se cierra y el contador no se mueve.
    expect(grid.pinadas().size).toBe(1);
    expect(grid.pinadas().has('LCL-1ºA|1')).toBe(false);

    ultimoGuardar.next({
      id: 9,
      actividadCodigo: 'LCL-1ºA',
      indice: 1,
      tramo: { dia: 3, orden: 4 },
      aulas: [],
    });
    await fixture.whenStable();

    // DESPUÉS: entra el nuevo, sigue el viejo, y el aviso lo refleja.
    expect(grid.pinadas().size).toBe(2);
    expect(grid.pinadas().get('LCL-1ºA|1')).toBe(9);
    expect(grid.pinadas().get('Mat-1ºA|1')).toBe(7);
    expect(raiz.querySelector('.aviso')?.textContent?.trim()).toBe('2 pines sin aplicar — regenerar');
  });

  /**
   * La clave del índice sale de la RESPUESTA del POST, no de la petición: el backend
   * es la autoridad sobre qué instancia quedó pinada.
   *
   * <p>FIXTURE DEFENSIVO DECLARADO, igual que el (22) original: en producción el
   * backend devuelve lo que recibe, así que esta divergencia es imposible. Es
   * deliberada —sin ella petición y respuesta coincidirían y la mutación quedaría
   * verde— y diverge en las DOS dimensiones de la clave, actividad e índice.
   */
  it('(74) la clave del índice sale de la respuesta del POST, no de la petición', async () => {
    const grid = await montarConSesiones([fila(1, 'Mat-1ºA', 2, 3, 4)]);

    grid.pinar.emit('Mat-1ºA|2');
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
   * Un POST que falla NO pina —el candado se queda abierto— y el mensaje se DEGRADA
   * cuando el body no trae `message` ni `error`. Se asevera el TEXTO EXACTO, no la
   * mera presencia de `.error`: la degradación silenciosa a cadena vacía dejaría el
   * aviso mudo y pasaría con un aserto de presencia.
   *
   * <p>Se lee por el `<p class="error">`: aquí la proyección va OK y no hay fallo de
   * diagnóstico, así que ese párrafo es inequívocamente `errorPin`. El mensaje sigue
   * saliendo de {@link mensaje}, que habla del pin y no se tocó al añadir
   * `mensajeAjuste`.
   */
  it('(75) un POST de pin que falla no pina y el mensaje se degrada al estado', async () => {
    const grid = await montarConSesiones([fila(1, 'Mat-1ºA', 2, 3, 4)]);

    grid.pinar.emit('Mat-1ºA|2');
    await fixture.whenStable();

    ultimoGuardar.error({ status: 400, error: {} });
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;
    expect(raiz.querySelector('.error')?.textContent?.trim()).toBe(
      'El servidor rechazó el pin (400).',
    );
    expect(grid.pinadas().size).toBe(0);
    // El aviso de pines no llegó a existir: nada que regenerar.
    expect(raiz.querySelector('.aviso')).toBeNull();
  });

  /**
   * Una clave que no está en la proyección no tiene tramo que mandar: no se emite
   * POST y no se inventa un aviso, con el mismo criterio que {@link alDespinar} ante
   * un id ausente. La hermana SÍ presente es lo que hace escopado al aserto: sin
   * ella, "no llamó" podría ser una proyección vacía.
   */
  it('(76) pinar una clave ausente de la proyección no emite POST', async () => {
    const grid = await montarConSesiones([fila(1, 'Mat-1ºA', 2, 3, 4)]);

    // Misma actividad, otra repetición: esa instancia no está en la proyección.
    grid.pinar.emit('Mat-1ºA|3');
    await fixture.whenStable();

    expect(bloqueos.guardar).not.toHaveBeenCalled();
    expect(grid.pinadas().size).toBe(0);
    expect((fixture.nativeElement as HTMLElement).querySelector('.error')).toBeNull();
  });

  /**
   * Los DOS invariantes que el (25) original fijaba sobre el alta desde la suelta, y
   * que al mover el alta al candado se habían quedado sin dueño:
   *
   * <p>(a) Tras un alta OK, la proyección NO se recarga: `getProyeccion` sigue en la
   * ÚNICA llamada del montaje. El pin es una restricción para la PRÓXIMA generación,
   * no un movimiento del horario vigente, así que no hay nada nuevo que traer. Mata
   * el `this.cargar(id)` en el `next`, que dispararía un segundo GET.
   *
   * <p>(b) Un segundo intento LIMPIA `errorPin` antes de que su POST responda:
   * `alPinar` hace `errorPin.set(null)` en su primera línea. Se comprueba con el
   * segundo sujeto AÚN sin emitir; sin esa fase, el aviso del fallo anterior se
   * quedaría pintado mientras el usuario espera y parecería que el reintento también
   * falló.
   *
   * <p>Aquí es donde el doble `guardar` FRESCO POR INVOCACIÓN es imprescindible: el
   * sujeto del alta fallida queda CERRADO tras `.error()`, y re-suscribirse a un
   * Subject cerrado redispara el error SÍNCRONAMENTE, lo que repoblaría `errorPin` y
   * haría inobservable la fase "a null". Mismo razonamiento que el (25) original.
   *
   * <p>Las tres instancias del fixture son distintas a propósito: si el segundo
   * intento reusara la clave del primero, "limpia el error" y "el error nunca se
   * pobló" no se distinguirían.
   */
  it('(78) el alta OK no recarga la proyección, y un nuevo intento limpia el error previo antes de responder', async () => {
    const grid = await montarConSesiones([
      fila(1, 'Mat-1ºA', 2, 3, 4),
      fila(2, 'LCL-1ºA', 1, 2, 5),
      fila(3, 'ING-1ºA', 1, 1, 1),
    ]);
    expect(horario.getProyeccion).toHaveBeenCalledTimes(1);

    // (a) alta OK: la proyección no se recarga.
    grid.pinar.emit('Mat-1ºA|2');
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
    grid.pinar.emit('LCL-1ºA|1');
    await fixture.whenStable();
    ultimoGuardar.error({ status: 400, error: {} });
    await fixture.whenStable();
    expect(raiz.querySelector('.error')).not.toBeNull();

    // ...y el siguiente intento lo limpia ANTES de que su POST responda.
    grid.pinar.emit('ING-1ºA|1');
    await fixture.whenStable();
    expect(raiz.querySelector('.error')).toBeNull();
  });

  /**
   * El gesto del pin y el del ajuste son INDEPENDIENTES: pinar no manda ninguna
   * petición de movimiento. Mata la mutación que cableara `pinar` al camino del
   * ajuste —los dos nacen del mismo componente y llevan la misma instancia—.
   */
  it('(77) pinar no dispara ninguna petición de ajuste', async () => {
    const grid = await montarConSesiones([fila(1, 'Mat-1ºA', 2, 3, 4)]);

    grid.pinar.emit('Mat-1ºA|2');
    await fixture.whenStable();

    expect(ajustes.mover).not.toHaveBeenCalled();
    expect(ajustes.intercambiar).not.toHaveBeenCalled();
  });

  // --- Gesto de AJUSTE (S145) ------------------------------------------------
  //
  // Sustituye a los casos (21)-(26), que medían el alta de PIN que este mismo
  // gesto hacía hasta S144. Ese camino ya no existe: arrastrar mueve el horario
  // vigente, no deja una restricción para la próxima generación, así que aquellos
  // asertos no se adaptan —afirmaban un comportamiento retirado—.

  /**
   * Una instancia ocupante del slot destino. Solo se leen `actividadCodigo` e
   * `indice` —la clave de negocio que viaja al backend—; `entradas` va vacío porque
   * el contenedor no lo mira: quien lo usa es la rejilla, para pintar.
   */
  function ocupante(actividadCodigo: string, indice: number): InstanciaCelda {
    return { actividadCodigo, indice, entradas: [] };
  }

  /** Una fila de proyección mínima, para poblar las respuestas del ajuste. */
  function fila(sesionId: number, actividadCodigo: string, indice: number, dia: number, tramo: number): SesionVista {
    return {
      sesionId,
      indice,
      dia,
      tramo,
      asignaturaCodigo: 'X',
      asignaturaNombre: 'X',
      profesores: ['P1'],
      aulaCodigo: 'A1',
      subgrupos: ['1ºA-Completo'],
      grupos: ['1ºA'],
      actividadCodigo,
      plazaCodigo: `${actividadCodigo}-P1`,
    };
  }

  /**
   * Monta con una proyección que SÍ tiene sesiones, a diferencia de
   * {@link montar}: los casos del refresco necesitan filas que sustituir, y con
   * `sesiones: []` un "se refrescó" sería indistinguible de un "no se tocó nada".
   */
  async function montarConSesiones(sesiones: SesionVista[]): Promise<HorarioGrid> {
    sujetoParam.next(convertToParamMap({ id: '1' }));
    ultimoListar.next([]);
    sujetoProyeccion.next({ ...PROYECCION_VACIA, sesiones });
    await fixture.whenStable();
    return rejilla();
  }

  /**
   * Destino VACÍO ⇒ `mover`, nunca `intercambiar`. Las dos mitades discriminan: sin
   * el `not.toHaveBeenCalled` sobre `intercambiar`, una implementación que llamara a
   * los DOS pasaría.
   *
   * <p>El cuerpo va LITERAL, no compuesto desde el evento: componerlo volvería
   * circular el aserto. `dia = 3` y `orden = 4` son distintos entre sí —con
   * `dia === orden` una permutación de ambos daría un cuerpo idéntico— e `indice`
   * es 2 y no 1, que una implementación podría fijar a mano. El id del horario (1)
   * sale de la ruta, y va como primer argumento.
   */
  it('(61) destino vacío: se llama a mover con el tramo destino, y no a intercambiar', async () => {
    const grid = await montar([]);

    grid.soltar.emit({ actividadCodigo: 'Mat-1ºA', indice: 2, dia: 3, orden: 4, ocupantes: [] });
    await fixture.whenStable();

    expect(ajustes.mover).toHaveBeenCalledTimes(1);
    expect(ajustes.mover).toHaveBeenCalledWith(1, {
      actividadCodigo: 'Mat-1ºA',
      indice: 2,
      dia: 3,
      orden: 4,
    });
    expect(ajustes.intercambiar).not.toHaveBeenCalled();
  });

  /**
   * Destino con UNA instancia ⇒ `intercambiar` con AMBAS nombradas, y no `mover`.
   *
   * <p>Las dos referencias DIFIEREN en actividad Y en índice, y ninguna repite los
   * valores de la otra: así el aserto distingue "manda la arrastrada como primera y
   * la ocupante como segunda" de una implementación que las permutara o que mandara
   * dos veces la misma. El tramo destino NO viaja: el intercambio no lo lleva en el
   * cuerpo (cada una va donde está la otra), y que el evento sí lo traiga es lo que
   * mide que no se cuela.
   */
  it('(62) destino con una instancia: se llama a intercambiar con las DOS referencias, y no a mover', async () => {
    const grid = await montar([]);

    grid.soltar.emit({
      actividadCodigo: 'Mat-1ºA',
      indice: 2,
      dia: 3,
      orden: 4,
      ocupantes: [ocupante('LCL-1ºA', 1)],
    });
    await fixture.whenStable();

    expect(ajustes.intercambiar).toHaveBeenCalledTimes(1);
    expect(ajustes.intercambiar).toHaveBeenCalledWith(1, {
      primera: { actividadCodigo: 'Mat-1ºA', indice: 2 },
      segunda: { actividadCodigo: 'LCL-1ºA', indice: 1 },
    });
    expect(ajustes.mover).not.toHaveBeenCalled();
  });

  /**
   * Destino con DOS o más ⇒ NINGUNA petición y un aviso que dice cuántas hay. Es
   * una limitación conocida del gesto, no un rechazo: se pinta con la clase
   * `.aviso-ajuste`, distinta de `.error-ajuste`, y NO se toca el backend.
   *
   * <p>El número va EN el aserto del texto (`Hay 2 clases`): sin él, una
   * implementación que fijara la frase a mano —o que contara `entradas` en vez de
   * instancias— quedaría verde.
   */
  it('(63) destino con dos o más: no se pide nada y el aviso dice cuántas hay', async () => {
    const grid = await montar([]);

    grid.soltar.emit({
      actividadCodigo: 'Mat-1ºA',
      indice: 2,
      dia: 3,
      orden: 4,
      ocupantes: [ocupante('LCL-1ºA', 1), ocupante('ING-1ºA', 1)],
    });
    await fixture.whenStable();

    expect(ajustes.mover).not.toHaveBeenCalled();
    expect(ajustes.intercambiar).not.toHaveBeenCalled();

    const raiz = fixture.nativeElement as HTMLElement;
    const aviso = raiz.querySelector('.aviso-ajuste');
    expect(aviso).not.toBeNull();
    expect(aviso!.textContent?.trim()).toBe(
      'Hay 2 clases en ese tramo: no puedo saber con cuál intercambiar.',
    );
    // No es un rechazo del servidor: el bloque de error sigue ausente.
    expect(raiz.querySelector('.error-ajuste')).toBeNull();
  });

  /**
   * El 200 del INTERCAMBIO refleja LOS DOS lados en la rejilla, cada lista aplicada
   * a SU referencia. El fixture es el caso que rompe una implementación que
   * reagrupara por `actividadCodigo`: las dos instancias son repeticiones de la
   * MISMA actividad (`Mat-1ºA`, índices 1 y 2), así que concatenar y reagrupar las
   * mezclaría sin remedio.
   *
   * <p>Se lee por el input `sesiones` de la rejilla, la frontera pública, y se
   * asevera el par (dia, tramo) de cada instancia: los dos se INTERCAMBIAN, así que
   * una implementación que aplicara `primera` a los dos lados —o que se saltara uno—
   * cae. La mitad "antes" fija que no hubo movimiento optimista.
   */
  it('(64) el 200 del intercambio mueve las DOS instancias, cada lista a su lado', async () => {
    const grid = await montarConSesiones([
      fila(1, 'Mat-1ºA', 1, 1, 1),
      fila(2, 'Mat-1ºA', 2, 5, 6),
    ]);

    grid.soltar.emit({
      actividadCodigo: 'Mat-1ºA',
      indice: 1,
      dia: 5,
      orden: 6,
      ocupantes: [ocupante('Mat-1ºA', 2)],
    });
    await fixture.whenStable();

    // ANTES del 200: la rejilla no se ha movido.
    expect(grid.sesiones().find((s) => s.indice === 1)?.dia).toBe(1);
    expect(grid.sesiones().find((s) => s.indice === 2)?.dia).toBe(5);

    ultimoIntercambiar.next({
      primera: [fila(1, 'Mat-1ºA', 1, 5, 6)],
      segunda: [fila(2, 'Mat-1ºA', 2, 1, 1)],
    });
    await fixture.whenStable();

    // DESPUÉS: permutadas, y sin filas de más ni de menos.
    expect(grid.sesiones().length).toBe(2);
    const primera = grid.sesiones().find((s) => s.indice === 1);
    const segunda = grid.sesiones().find((s) => s.indice === 2);
    expect([primera?.dia, primera?.tramo]).toEqual([5, 6]);
    expect([segunda?.dia, segunda?.tramo]).toEqual([1, 1]);
  });

  /**
   * El 200 de MOVER sustituye las filas de la instancia movida y DEJA EN PAZ a las
   * demás. La sesión ajena (`LCL-1ºA`) es lo que discrimina: sin ella, "sustituye lo
   * suyo" y "reemplaza la proyección entera por la respuesta" darían lo mismo.
   *
   * <p>La instancia movida tiene DOS filas (un desdoble) y la respuesta también:
   * una implementación que sustituyera solo la primera dejaría tres filas de esa
   * instancia y el conteo cae.
   */
  it('(65) el 200 de mover sustituye las filas de esa instancia y no toca las ajenas', async () => {
    const grid = await montarConSesiones([
      fila(1, 'Mat-1ºA', 2, 1, 1),
      fila(2, 'Mat-1ºA', 2, 1, 1),
      fila(3, 'LCL-1ºA', 1, 2, 3),
    ]);

    grid.soltar.emit({ actividadCodigo: 'Mat-1ºA', indice: 2, dia: 4, orden: 5, ocupantes: [] });
    await fixture.whenStable();

    ultimoMover.next([fila(1, 'Mat-1ºA', 2, 4, 5), fila(2, 'Mat-1ºA', 2, 4, 5)]);
    await fixture.whenStable();

    const movidas = grid.sesiones().filter((s) => s.actividadCodigo === 'Mat-1ºA');
    expect(movidas.length).toBe(2);
    expect(movidas.every((s) => s.dia === 4 && s.tramo === 5)).toBe(true);
    // La ajena, intacta.
    const ajena = grid.sesiones().find((s) => s.actividadCodigo === 'LCL-1ºA');
    expect([ajena?.dia, ajena?.tramo]).toEqual([2, 3]);
  });

  /**
   * 409 `VIOLA_REGLA_DURA`: se pinta el texto de la causa Y una línea por violación,
   * cada una con su recurso. Las DOS violaciones del fixture tienen recursos
   * distintos y se aseveran las dos: con una sola, "pinta la primera" y "pinta
   * todas" serían indistinguibles.
   */
  it('(66) un 409 VIOLA_REGLA_DURA pinta las violaciones con su recurso', async () => {
    const grid = await montar([]);

    grid.soltar.emit({ actividadCodigo: 'Mat-1ºA', indice: 2, dia: 3, orden: 4, ocupantes: [] });
    await fixture.whenStable();

    ultimoMover.error({
      status: 409,
      error: {
        causa: 'VIOLA_REGLA_DURA',
        mensaje: 'prosa de log que no se enseña',
        violaciones: [
          {
            regla: 'SOLAPE_PROFESOR',
            recursoCodigo: 'PROF7',
            tramoCodigo: 'L-2',
            celdas: [{ actividadCodigo: 'Mat-1ºA', indice: 2, plazaCodigo: null }],
            descripcion: 'inerte',
          },
          {
            regla: 'SOLAPE_SUBGRUPO',
            recursoCodigo: '1ºA-Completo',
            tramoCodigo: 'L-2',
            celdas: [{ actividadCodigo: 'Mat-1ºA', indice: 2, plazaCodigo: null }],
            descripcion: 'inerte',
          },
        ],
      },
    });
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;
    expect(raiz.querySelector('.error-ajuste')?.textContent?.trim()).toBe(
      'Ese cambio provoca conflictos que antes no existían:',
    );
    const lineas = Array.from(raiz.querySelectorAll('.violaciones-ajuste .violacion')).map((li) =>
      li.textContent?.trim(),
    );
    expect(lineas).toEqual(['SOLAPE_PROFESOR — PROF7 en L-2', 'SOLAPE_SUBGRUPO — 1ºA-Completo en L-2']);
    // La prosa del servidor NO se enseña en esta causa: la vista decide con la causa.
    expect(raiz.textContent).not.toContain('prosa de log que no se enseña');
  });

  /**
   * `DISTRIBUCION_MISMO_DIA` es regla DURA y NO trae `recursoCodigo` —el conflicto
   * es de la actividad consigo misma, medido en S144—: la línea nombra la regla y
   * las CELDAS culpables. Es el caso que rompe la plantilla ingenua, y por eso el
   * aserto exige el texto completo: ni «recurso: undefined» ni la violación oculta.
   *
   * <p>Las dos celdas van en el aserto —son las dos repeticiones del mismo día—, y
   * la ausencia de `null` en el texto se asevera aparte: la mutación que quita el
   * fallback y escribe `recursoCodigo` a secas produce exactamente esa cadena.
   */
  it('(67) un 409 con DISTRIBUCION_MISMO_DIA, sin recurso, pinta la regla y sus celdas', async () => {
    const grid = await montar([]);

    grid.soltar.emit({ actividadCodigo: 'Mat-1ºA', indice: 3, dia: 3, orden: 4, ocupantes: [] });
    await fixture.whenStable();

    ultimoMover.error({
      status: 409,
      error: {
        causa: 'VIOLA_REGLA_DURA',
        mensaje: 'prosa',
        violaciones: [
          {
            regla: 'DISTRIBUCION_MISMO_DIA',
            recursoCodigo: null,
            tramoCodigo: null,
            celdas: [
              { actividadCodigo: 'Mat-1ºA', indice: 1, plazaCodigo: null },
              { actividadCodigo: 'Mat-1ºA', indice: 3, plazaCodigo: null },
            ],
            descripcion: 'inerte',
          },
        ],
      },
    });
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;
    const linea = raiz.querySelector('.violaciones-ajuste .violacion')?.textContent?.trim();
    expect(linea).toBe('DISTRIBUCION_MISMO_DIA — Mat-1ºA #1, Mat-1ºA #3');
    expect(linea).not.toContain('null');
    expect(linea).not.toContain('undefined');
  });

  /**
   * 409 `INSTANCIA_PINADA`: el mensaje dice que manda el PIN, y la rejilla NO se
   * mueve. Las dos mitades importan —el texto exacto y la proyección intacta—:
   * la segunda es la que mata un movimiento optimista que dejara la instancia en el
   * destino pese al rechazo.
   */
  it('(68) un 409 INSTANCIA_PINADA dice que manda el pin y la rejilla no se mueve', async () => {
    const grid = await montarConSesiones([fila(1, 'Mat-1ºA', 2, 1, 1)]);

    grid.soltar.emit({ actividadCodigo: 'Mat-1ºA', indice: 2, dia: 4, orden: 5, ocupantes: [] });
    await fixture.whenStable();

    ultimoMover.error({
      status: 409,
      error: { causa: 'INSTANCIA_PINADA', mensaje: 'prosa', violaciones: [] },
    });
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;
    expect(raiz.querySelector('.error-ajuste')?.textContent?.trim()).toBe(
      'Esa clase está pinada y el pin manda sobre el arrastre: quita el pin antes de moverla.',
    );
    // La rejilla sigue EXACTAMENTE como estaba: mismo tramo de origen.
    expect(grid.sesiones().length).toBe(1);
    expect([grid.sesiones()[0].dia, grid.sesiones()[0].tramo]).toEqual([1, 1]);
    // Sin violaciones que enumerar, la lista ni se pinta.
    expect(raiz.querySelector('.violaciones-ajuste')).toBeNull();
  });

  /**
   * 404 `INSTANCIA_INEXISTENTE`: el cuerpo dice CUÁL de las dos falta —solo viaja en
   * la prosa del servidor, no hay campo estructurado— y el aviso la nombra. Es la
   * ÚNICA causa que arrastra ese texto, y el aserto lo exige literal: sin él, "una
   * de las dos" a secas dejaría al usuario sin saber cuál.
   */
  it('(69) un 404 INSTANCIA_INEXISTENTE nombra cuál de las dos instancias falta', async () => {
    const grid = await montar([]);

    grid.soltar.emit({
      actividadCodigo: 'Mat-1ºA',
      indice: 2,
      dia: 3,
      orden: 4,
      ocupantes: [ocupante('LCL-1ºA', 1)],
    });
    await fixture.whenStable();

    ultimoIntercambiar.error({
      status: 404,
      error: {
        causa: 'INSTANCIA_INEXISTENTE',
        mensaje: "La instancia 'segunda' (LCL-1ºA, 1) no está en el horario 1",
        violaciones: [],
      },
    });
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;
    expect(raiz.querySelector('.error-ajuste')?.textContent?.trim()).toBe(
      "Una de las dos clases ya no está en el horario. La instancia 'segunda' (LCL-1ºA, 1) no está en el horario 1",
    );
  });

  /**
   * Cualquier rechazo deja la proyección EXACTAMENTE como estaba: mismas filas, en
   * los mismos tramos, y ninguna recarga. `getProyeccion` sigue en la única llamada
   * del montaje, que es lo que mata un `this.cargar(id)` en la rama de error —un
   * refresco que "arreglaría" la vista y taparía el hecho de que no se movió nada—.
   *
   * <p>El fixture usa una causa DESCONOCIDA a propósito (la del degradado), que es
   * la rama por la que también pasan `TRAMO_INEXISTENTE` y cualquier causa futura:
   * si el degradado tocara la rejilla, ninguna de las otras lo detectaría.
   */
  it('(70) tras un rechazo, la rejilla queda exactamente como estaba y no se recarga', async () => {
    const grid = await montarConSesiones([
      fila(1, 'Mat-1ºA', 2, 1, 1),
      fila(2, 'LCL-1ºA', 1, 2, 3),
    ]);
    expect(horario.getProyeccion).toHaveBeenCalledTimes(1);
    const antes = grid.sesiones().map((s) => [s.sesionId, s.dia, s.tramo]);

    grid.soltar.emit({ actividadCodigo: 'Mat-1ºA', indice: 2, dia: 4, orden: 5, ocupantes: [] });
    await fixture.whenStable();

    ultimoMover.error({ status: 400, error: { causa: 'TRAMO_INEXISTENTE', mensaje: 'x', violaciones: [] } });
    await fixture.whenStable();

    expect(grid.sesiones().map((s) => [s.sesionId, s.dia, s.tramo])).toEqual(antes);
    expect(horario.getProyeccion).toHaveBeenCalledTimes(1);
    // Degradado honesto: dice el estado, no inventa un motivo.
    const raiz = fixture.nativeElement as HTMLElement;
    expect(raiz.querySelector('.error-ajuste')?.textContent?.trim()).toBe(
      'El servidor rechazó el cambio (400).',
    );
  });

  /**
   * Mientras el ajuste vuela se reutiliza el estado de espera de S118 —la misma
   * señal y el mismo `<p class="generando">`—, pero con SU frase: anunciarle los
   * diez minutos de un solve sería falso. El botón «Generar» queda cerrado, que es
   * la otra mitad de reutilizar el estado y no duplicarlo.
   */
  it('(71) mientras el ajuste vuela se reutiliza el aviso de espera, con su propia frase', async () => {
    const grid = await montar([]);

    grid.soltar.emit({ actividadCodigo: 'Mat-1ºA', indice: 2, dia: 3, orden: 4, ocupantes: [] });
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;
    expect(raiz.querySelector('.generando')?.textContent?.trim()).toBe('Aplicando el cambio…');
    expect((raiz.querySelector('button.generar') as HTMLButtonElement).disabled).toBe(true);

    ultimoMover.next([]);
    await fixture.whenStable();

    expect(raiz.querySelector('.generando')).toBeNull();
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
    ultimoListar.next([]);
    sujetoPrevalidacion.next(avisos);
    sujetoProyeccion.next(PROYECCION_VACIA);
    await fixture.whenStable();
  }

  /**
   * Pulsa el botón «Generar» por el DOM, la frontera real del gesto. Desde S145 eso
   * ya NO lanza la generación: abre el diálogo y ahí se queda.
   */
  function abrirDialogoGenerar(): void {
    const boton = (fixture.nativeElement as HTMLElement).querySelector(
      'button.generar',
    ) as HTMLButtonElement;
    if (!boton) {
      throw new Error('El botón de generar no está en el DOM.');
    }
    boton.click();
  }

  /**
   * El gesto COMPLETO: pulsar y confirmar. Lo usan los tests que miden lo que pasa
   * DESPUÉS de generar (navegación, errores, espera), a los que el diálogo no les
   * interesa. Los que miden el diálogo mismo usan {@link abrirDialogoGenerar} y
   * emiten el cierre a mano.
   *
   * <p>La emisión es síncrona tras el click y eso basta: `generar()` se suscribe a
   * `closed` dentro del propio manejador, así que cuando esta línea corre la
   * suscripción ya existe.
   */
  function pulsarGenerar(): void {
    abrirDialogoGenerar();
    ultimoCerrado.next(true);
  }

  /**
   * S145 INVIERTE el (27) original, que fijaba «sin ERROR ⇒ 0 al diálogo». Ahora el
   * diálogo se abre SIEMPRE: lo que se confirma es el coste de la operación —diez
   * minutos, sustituye el trabajo en curso, irreversible—, que existe también con el
   * catálogo sano. Sobre el centro real la pre-validación devuelve lista vacía, así
   * que con la regla vieja el diálogo no se abría NUNCA y una generación salía de un
   * clic.
   *
   * <p>Las tres mitades discriminan: `open` recibe 1 (no 0: la regla vieja), el
   * `data` va con la lista VACÍA —no con el aviso no-ERROR, que es lo que separa
   * «filtra los ERROR» de «pasa lo que haya»— y `generar` recibe 0 antes del cierre.
   */
  it('(27) sin ERROR en la pre-validación, generar abre el diálogo igualmente, con data vacío', async () => {
    await montarConPrevalidacion([AVISO_NO_ERROR]);

    abrirDialogoGenerar();
    await fixture.whenStable();

    expect(dialog.open).toHaveBeenCalledTimes(1);
    expect(dialog.open).toHaveBeenCalledWith(ConfirmarGeneracion, { data: [] });
    expect(horario.generar).toHaveBeenCalledTimes(0);
  });

  /**
   * Gemelo del (27): confirmado con lista vacía, la generación procede. Sin este, la
   * mutación que abre el diálogo pero no cablea el `closed` quedaría verde en el
   * camino sin avisos, que es el único que recorre este centro.
   */
  it('(27b) confirmado el diálogo sin avisos, la generación procede una vez', async () => {
    await montarConPrevalidacion([AVISO_NO_ERROR]);

    abrirDialogoGenerar();
    await fixture.whenStable();
    ultimoCerrado.next(true);
    await fixture.whenStable();

    expect(horario.generar).toHaveBeenCalledTimes(1);
  });

  /**
   * Cancelar el diálogo NO dispara ninguna generación. `false` y no `undefined`
   * —ese lo cubre el (29) del backdrop—: juntos fijan que la condición del cierre es
   * exactamente `=== true` por los dos lados.
   */
  it('(27c) cancelado el diálogo sin avisos, no se llama al backend', async () => {
    await montarConPrevalidacion([AVISO_NO_ERROR]);

    abrirDialogoGenerar();
    await fixture.whenStable();
    ultimoCerrado.next(false);
    await fixture.whenStable();

    expect(horario.generar).toHaveBeenCalledTimes(0);
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

    abrirDialogoGenerar();
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

    abrirDialogoGenerar();
    await fixture.whenStable();
    expect(horario.generar).toHaveBeenCalledTimes(0);

    ultimoCerrado.next(undefined);
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

    abrirDialogoGenerar();
    await fixture.whenStable();

    ultimoCerrado.next(true);
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
   *
   * <p>S118: este test YA NO fija el TEXTO del aviso. Lo fijaba —el degradado
   * `'El servidor rechazó el pin (422).'`, heredado de compartir `mensaje()` con la
   * vía de pines y equivocado en esta vía, que no habla de ningún pin—, y desde el
   * (42) el texto del 422 de generación se asevera allí, donde su pareja (41) lo
   * hace discriminante frente al 503. Aquí queda lo que este test mide de verdad:
   * QUÉ señal se puebla y qué NO se toca. Que el aviso lleve texto sigue aseverado
   * (un `.error-generacion` vacío no distinguiría "falló" de "no falló").
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
    // (2) su aviso propio, con su clase y con algún texto (cuál, lo fija el (42)).
    const aviso = raiz.querySelector('.error-generacion');
    expect(aviso).not.toBeNull();
    expect(aviso!.textContent?.trim()).not.toBe('');
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
    ultimoListar.next([]);
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

  /**
   * El índice de pines es de TODO el horario, no del filtro: cambiar de VISTA no
   * lo recarga (invariante del TSDoc de `cargarPines`, horario-view.ts:125-132).
   * Hoy se sostiene por AUSENCIA de llamada, no por lógica defensiva; este test lo
   * fija. El ASERTO A (precondición: `listar` en 1) evita medir un no-op —sin la
   * carga del montaje, B pasaría trivial—, y el C confirma que el gesto ocurrió de
   * verdad. `cambiarVista` es `protected` y se invoca por cast, único disparador
   * que toca SOLO la vista sin reemitir la ruta (precedente: it (34), línea 776).
   */
  it('(37) cambiar de vista no recarga el índice de pines', async () => {
    await montar([]);
    const comp = fixture.componentInstance as unknown as {
      cambiarVista(v: string): void;
      vista(): string;
    };

    // ASERTO A (precondición): el montaje ya cargó el índice una vez.
    expect(bloqueos.listar).toHaveBeenCalledTimes(1);

    comp.cambiarVista('profesor');
    await fixture.whenStable();

    // ASERTO B (discriminante): el gesto NO redisparó la carga.
    expect(bloqueos.listar).toHaveBeenCalledTimes(1);
    // ASERTO C: el gesto ocurrió de verdad.
    expect(comp.vista()).toBe('profesor');
  });

  /**
   * Gemelo del (37) para el gesto de ENTIDAD: cambiar de entidad tampoco recarga
   * el índice —es de TODO el horario, no del filtro—. `cambiarEntidad` es un setter
   * puro (`this.entidad.set(e)`, horario-view.ts:309-311) que NO consulta
   * `entidades()`, así que basta una entidad distinta de la actual: tras el montaje
   * con proyección vacía `entidad()` es '', y se pasa un valor no vacío para que el
   * ASERTO C discrimine. Mismos A/B/C que (37).
   */
  it('(38) cambiar de entidad no recarga el índice de pines', async () => {
    await montar([]);
    const comp = fixture.componentInstance as unknown as {
      cambiarEntidad(e: string): void;
      entidad(): string;
    };

    // ASERTO A (precondición): el montaje ya cargó el índice una vez.
    expect(bloqueos.listar).toHaveBeenCalledTimes(1);

    comp.cambiarEntidad('1ºA');
    await fixture.whenStable();

    // ASERTO B (discriminante): el gesto NO redisparó la carga.
    expect(bloqueos.listar).toHaveBeenCalledTimes(1);
    // ASERTO C: el gesto ocurrió de verdad.
    expect(comp.entidad()).toBe('1ºA');
  });
  /**
   * RAMA DE ID DISTINTO: el horario devuelto no es el cargado, así que se navega y
   * la recarga NO se hace aquí —la disparará la emisión de `paramMap` al cambiar de
   * ruta, que en este spec no ocurre porque el `Router` está doblado y no cableado
   * al `paramMap` (ver cabecero)—. Por eso el segundo aserto puede exigir que
   * `getProyeccion` siga en la ÚNICA llamada del montaje: cualquier recarga extra
   * sería del `next`, no de la ruta.
   *
   * <p>Es el gemelo exacto del (40) y juntos guardan una rama cada uno: sin este,
   * la implementación que SIEMPRE recarga (y nunca navega) quedaría verde.
   */
  it('(39) generar con id distinto navega a la ruta nueva y no recarga la proyección', async () => {
    await montarConPrevalidacion([AVISO_NO_ERROR]);
    expect(horario.getProyeccion).toHaveBeenCalledTimes(1);

    pulsarGenerar();
    await fixture.whenStable();

    ultimoGenerar.next({ ...PROYECCION_VACIA, id: 2 });
    await fixture.whenStable();

    // (1) se navega al horario devuelto. Array LITERAL, nunca compuesto desde el
    // dto: componerlo volvería circular el aserto (misma disciplina que el (31)).
    expect(router.navigate).toHaveBeenCalledTimes(1);
    expect(router.navigate).toHaveBeenCalledWith(['/horario', 2]);
    // (2) DISCRIMINANTE: no hubo recarga por esta vía. Mata la implementación que
    // llama a `cargar` pase lo que pase.
    expect(horario.getProyeccion).toHaveBeenCalledTimes(1);
  });

  /**
   * RAMA DEL MISMO ID: el horario devuelto es el que ya está cargado, así que NO se
   * navega —el router ignora la navegación a la URL vigente (`onSameUrlNavigation`
   * en `'ignore'`), `paramMap` no reemitiría y la rejilla se quedaría con el horario
   * viejo— y se recarga a mano. Escenario REAL: primera generación de una
   * instalación nueva, con la BD vacía (proyección inicial en 404) y el `/horario/1`
   * clavado de la landing, así que el id devuelto coincide con el de la ruta.
   *
   * <p>RE-STUB DECLARADO de `getProyeccion`: a diferencia de `listar`/`guardar`/
   * `borrar`/`generar`, su doble devuelve un Subject COMPARTIDO
   * (`sujetoProyeccion`), y el 404 inicial lo deja CERRADO. Re-suscribirse a un
   * Subject cerrado redispara el error SÍNCRONAMENTE, así que la segunda carga
   * nacería fallida y el tercer aserto sería inalcanzable. El `mockImplementation`
   * le da un sujeto nuevo, con vida propia, SIN perder el contador de llamadas —que
   * es justo lo que mide el segundo aserto—. Es local a este caso: el `beforeEach`
   * estrena objeto y `vi.fn()` en cada uno, así que no filtra.
   *
   * <p>El tercer aserto es el que ata el caso al síntoma del usuario: sin él, una
   * recarga que dejase la pantalla en el mensaje de error pasaría igual.
   */
  it('(40) generar con el mismo id recarga la proyección sin navegar', async () => {
    // Montaje del escenario: ruta 1, pre-validación emitida (habilita el botón) y
    // proyección en 404 —BD vacía, aún no hay horario 1—.
    sujetoParam.next(convertToParamMap({ id: '1' }));
    ultimoListar.next([]);
    sujetoPrevalidacion.next([AVISO_NO_ERROR]);
    sujetoProyeccion.error({ status: 404 });
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;
    // PRECONDICIÓN: la pantalla está en el error de carga y sin rejilla. Sin esto,
    // el tercer aserto mediría un estado que ya era limpio de nacimiento.
    expect(raiz.querySelector('.error')?.textContent?.trim()).toBe(
      'No se pudo cargar el horario 1 (404).',
    );
    expect(fixture.debugElement.query(By.directive(HorarioGrid))).toBeNull();
    expect(horario.getProyeccion).toHaveBeenCalledTimes(1);

    // Re-stub (ver javadoc): la recarga necesita un sujeto vivo, no el cerrado.
    const recarga = new Subject<HorarioProyeccion>();
    horario.getProyeccion.mockImplementation(() => recarga);

    pulsarGenerar();
    await fixture.whenStable();

    ultimoGenerar.next({ ...PROYECCION_VACIA, id: 1 });
    await fixture.whenStable();

    // (1) DISCRIMINANTE: no se navega a la ruta vigente.
    expect(router.navigate).not.toHaveBeenCalled();
    // (2) DISCRIMINANTE: hubo una SEGUNDA petición de proyección, con el id 1.
    expect(horario.getProyeccion).toHaveBeenCalledTimes(2);
    expect(horario.getProyeccion).toHaveBeenLastCalledWith(1);

    // La recarga responde: es un GET fresco, la proyección del POST se descartó.
    recarga.next(PROYECCION_VACIA);
    await fixture.whenStable();

    // (3) DISCRIMINANTE: la pantalla salió del error y la rejilla se pinta.
    expect(raiz.querySelector('.error')).toBeNull();
    expect(fixture.debugElement.query(By.directive(HorarioGrid))).not.toBeNull();
  });
  /**
   * S118 · el 503 del presupuesto agotado. Que CP-SAT se quede sin tiempo NO
   * demuestra que el horario sea imposible, solo que no dio tiempo a decidirlo, y
   * el usuario tiene una acción útil: volver a intentarlo. El mensaje debe ofrecer
   * esa acción.
   *
   * <p>Va EMPAREJADO con el (42): son la misma pregunta —¿la vista distingue el
   * status?— con las dos respuestas, y por eso no se puede pasar uno solo con un
   * mensaje fijo. El body va `{}` a propósito: si el texto saliera del cuerpo del
   * error y no del status, ambos degradarían al mismo sitio y el par caería.
   */
  it('(41) un 503 de generación ofrece reintentar, no declara el horario imposible', async () => {
    await montarConPrevalidacion([AVISO_NO_ERROR]);

    pulsarGenerar();
    await fixture.whenStable();

    ultimoGenerar.error({ status: 503, error: {} });
    await fixture.whenStable();

    const aviso = (fixture.nativeElement as HTMLElement).querySelector('.error-generacion');
    expect(aviso).not.toBeNull();
    expect(aviso!.textContent?.trim()).toBe(
      'Se agotó el tiempo de cálculo. Vuelve a intentarlo.',
    );
  });

  /**
   * S118 · el 422 del catálogo imposible. Aquí reintentar NO sirve de nada —el
   * resultado será idéntico— y proponerlo mandaría al usuario a esperar en balde.
   * El mensaje señala la configuración, que es lo único que puede cambiar.
   *
   * <p>Pareja del (41): mismo gesto, mismo body vacío, distinto status, distinto
   * texto. Una implementación con un único mensaje para todo fallo de generación
   * —la de hoy— no puede pasar los dos.
   */
  it('(42) un 422 de generación señala la configuración, no ofrece reintentar', async () => {
    await montarConPrevalidacion([AVISO_NO_ERROR]);

    pulsarGenerar();
    await fixture.whenStable();

    ultimoGenerar.error({ status: 422, error: {} });
    await fixture.whenStable();

    const aviso = (fixture.nativeElement as HTMLElement).querySelector('.error-generacion');
    expect(aviso).not.toBeNull();
    expect(aviso!.textContent?.trim()).toBe(
      'Esta configuración no tiene solución. Revisa el catálogo.',
    );
  });
  /**
   * S118 · el estado de espera, mitad "durante". Los TRES asertos atacan mecanismos
   * distintos y por eso van juntos —borrar uno solo debe poner el test rojo—:
   *
   * <p>(a) el botón se cierra mientras el POST vuela. Sin esto, la generación tarda
   * minutos con el botón pulsable y un segundo clic lanza otro solve encima del
   * primero. El `|| generando()` se AÑADE al `avisosPrevalidacion() === null` que ya
   * estaba: el (34) sigue midiendo esa otra mitad y ninguno de los dos basta solo.
   *
   * <p>(b) el texto de espera aparece; (c) con los minutos dentro. El número es lo
   * que distingue "está trabajando" de "se ha colgado" en una espera de diez minutos,
   * así que un texto sin él no cumple el propósito y el aserto lo exige.
   *
   * <p>El Subject NO se emite: la fase "en vuelo" es justamente la que se mide, y
   * cualquier emisión la cerraría.
   */
  it('(43) mientras el POST vuela: botón cerrado y aviso de espera con los minutos', async () => {
    await montarConPrevalidacion([AVISO_NO_ERROR]);

    pulsarGenerar();
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;
    const boton = raiz.querySelector('button.generar') as HTMLButtonElement;
    expect(boton.disabled).toBe(true);

    const espera = raiz.querySelector('.generando');
    expect(espera).not.toBeNull();
    expect(espera!.textContent).toContain('10 minutos');
  });

  /**
   * S118 · el estado de espera, mitad "después por ÉXITO". Es la pareja imprescindible
   * del (43): con `generando` puesto a `true` y nunca a `false`, el (43) pasa y la
   * aplicación queda con el botón muerto para siempre. Este test es el único que lo
   * caza por la rama de éxito.
   *
   * <p>Se emite un id DISTINTO del cargado para salir por la rama de navegación, que
   * no recarga la proyección (ver (39)): así el aserto mide el cierre del vuelo y no
   * se enreda con el ciclo de recarga.
   */
  it('(44) tras el 200 el botón se rehabilita y el aviso de espera desaparece', async () => {
    await montarConPrevalidacion([AVISO_NO_ERROR]);

    pulsarGenerar();
    await fixture.whenStable();

    ultimoGenerar.next({ ...PROYECCION_VACIA, id: 2 });
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;
    expect((raiz.querySelector('button.generar') as HTMLButtonElement).disabled).toBe(false);
    expect(raiz.querySelector('.generando')).toBeNull();
  });

  /**
   * S118 · el estado de espera, mitad "después por ERROR". La rama que más fácil se
   * olvida: poner `generando` a `false` solo en `next` deja la pantalla bloqueada
   * justo cuando el usuario necesita reintentar, y el (44) no lo caza porque nunca
   * pasa por el error.
   *
   * <p>Se comprueba a la vez que el aviso de error SÍ está: el fin de la espera y la
   * aparición del mensaje son dos efectos del mismo `error`, y separarlos permitiría
   * una implementación que limpiara el estado sin decir qué pasó.
   */
  it('(45) tras un fallo el botón se rehabilita, el aviso de espera se va y el de error llega', async () => {
    await montarConPrevalidacion([AVISO_NO_ERROR]);

    pulsarGenerar();
    await fixture.whenStable();

    ultimoGenerar.error({ status: 503, error: { causa: 'PRESUPUESTO_AGOTADO' } });
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;
    expect((raiz.querySelector('button.generar') as HTMLButtonElement).disabled).toBe(false);
    expect(raiz.querySelector('.generando')).toBeNull();
    expect(raiz.querySelector('.error-generacion')).not.toBeNull();
  });

  /**
   * S118 · la `causa` manda sobre el status. Un 422 con causa CONFIGURACION_INCOMPLETA
   * —catálogo sin jornada, la excepción sin solve— NO dice lo mismo que el 422 del
   * (42), que comparte status y significa otra cosa.
   *
   * <p>Es lo que impide que la vista decida solo por el número: con una implementación
   * que mire únicamente el status, este test y el (42) piden textos distintos para el
   * mismo 422 y uno de los dos cae.
   */
  it('(46) un 422 con causa CONFIGURACION_INCOMPLETA manda a configurar la jornada', async () => {
    await montarConPrevalidacion([AVISO_NO_ERROR]);

    pulsarGenerar();
    await fixture.whenStable();

    ultimoGenerar.error({ status: 422, error: { causa: 'CONFIGURACION_INCOMPLETA' } });
    await fixture.whenStable();

    const aviso = (fixture.nativeElement as HTMLElement).querySelector('.error-generacion');
    expect(aviso!.textContent?.trim()).toBe(
      'Falta configurar la jornada antes de generar un horario.',
    );
  });

  /**
   * D9, la mitad que importa: la cabecera vive FUERA de la cadena `@if`, así que
   * sobrevive al arranque con 404 —proyección que no existe todavía, que es el
   * estado real de una instalación nueva antes de la primera generación—.
   *
   * <p>Discriminante de la DIRECCIÓN de la fusión. Fundir título y controles admite
   * dos: subir el `<h2>` a `.controles`, o bajar `.controles` a la rama
   * `@else if (proyeccion())` donde el `<h2>` vivía. La segunda deja este escenario
   * sin botón «Generar» —y sin él no hay forma de salir del 404, porque generar es
   * justo lo que falta—, y la tira este caso junto al (40).
   */
  it('(47) arranque con 404: el título y el botón siguen en la cabecera, sin rejilla', async () => {
    sujetoParam.next(convertToParamMap({ id: '1' }));
    ultimoListar.next([]);
    sujetoProyeccion.error({ status: 404 });
    await fixture.whenStable();

    const controles = (fixture.nativeElement as HTMLElement).querySelector('.controles');
    expect(controles).not.toBeNull();

    // Los dos DENTRO de la fila: es lo que la fusión promete, y lo que se pierde si
    // la cabecera se baja a la rama de proyección.
    expect(controles!.querySelector('.titulo')?.textContent?.trim()).toBe('Horario');
    expect(controles!.querySelector('button.generar')).not.toBeNull();

    // PRECONDICIÓN del escenario: no hay proyección, así que no hay rejilla. Sin
    // esto el caso podría estar midiendo una pantalla cargada con normalidad.
    expect(fixture.debugElement.query(By.directive(HorarioGrid))).toBeNull();
  });

  /**
   * D10: el título dice CUÁNDO se generó el horario, no qué grupo estás mirando —eso
   * lo dice el selector, que desde D9 está a diez centímetros en la misma fila— ni el
   * instante crudo del backend.
   *
   * <p>El nombre se emite aquí en su forma REAL (`"Horario " + Instant`, la que pone
   * `GeneradorHorarioService` cuando el POST no manda nombre, que es siempre) y no se
   * usa `PROYECCION_VACIA`: su nombre de fixture no lleva ese prefijo y cae en el
   * degradado, que es otro comportamiento y se mide aparte, abajo.
   */
  it('(48) el título acorta el instante del backend y no repite lo que dicen los selectores', async () => {
    sujetoParam.next(convertToParamMap({ id: '1' }));
    ultimoListar.next([]);
    sujetoProyeccion.next({ ...PROYECCION_VACIA, nombre: 'Horario 2026-08-24T15:37:39.317184258Z' });
    await fixture.whenStable();

    const titulo = (fixture.nativeElement as HTMLElement).querySelector('.titulo')!.textContent!.trim();

    expect(titulo.startsWith('Horario ')).toBe(true);
    // (a) el instante crudo NO se pinta: son 38 caracteres que no caben en la fila.
    expect(titulo).not.toContain('2026-08-24T15:37:39.317184258Z');
    expect(titulo.length).toBeLessThan('Horario 2026-08-24T15:37:39.317184258Z'.length);
    // (b) y no repite lo que ya dicen los dos selectores de su misma fila.
    expect(titulo).not.toContain('grupo');
    expect(titulo).not.toContain('Proyección de prueba');
  });

  /**
   * El degradado de D10, con el fixture tal cual: un nombre que NO es del backend se
   * respeta entero. Un título feo se lee; uno vacío no dice qué horario miras.
   */
  it('(48b) un nombre puesto a mano se pinta tal cual', async () => {
    await montar([]);

    expect((fixture.nativeElement as HTMLElement).querySelector('.titulo')?.textContent?.trim())
      .toBe('Proyección de prueba');
  });

  /**
   * D6, la CAUSA. El e2e afirma la consecuencia —que `.grupos` no se pinta cuando el
   * único grupo es el de la vista— pero nadie afirmaba que la vista transmita su
   * entidad a la rejilla, que es de donde sale esa consecuencia.
   *
   * <p>Se observa por el input público de la hija, como el resto del fichero: el DOM
   * de la celda ya lo miden los cuatro casos de `horario-grid.spec.ts`, y duplicarlo
   * aquí ataría este spec al marcado de un componente que no es el suyo.
   *
   * <p>Se compara con `''` y no con un código de grupo: con `sesiones: []` no hay
   * entidades que derivar y `entidad()` se queda en su valor inicial. Lo que se mide
   * es que la vista TRANSMITE su entidad, no cuál es —eso ya lo miden los casos de
   * `entidadesDeVista`—. El discriminante está en la segunda mitad: en vista de
   * profesor NO hay grupo implícito y el input tiene que pasar a `null`, o la rejilla
   * condensaría la lista de grupos en la única vista donde esa lista informa.
   */
  it('(49) grupoActual llega a la rejilla en vista de grupo, y es null en las otras', async () => {
    const grid = await montar([]);

    expect(grid.grupoActual()).toBe('');

    (fixture.componentInstance as unknown as { cambiarVista(v: 'profesor'): void }).cambiarVista(
      'profesor',
    );
    await fixture.whenStable();

    expect(grid.grupoActual()).toBeNull();
  });

  /**
   * D7 · la vista deriva la posición del recreo de la jornada y se la pasa a la
   * rejilla. La mitad "antes" es la que discrimina: hasta que la jornada llega, el
   * input es null y la rejilla no inventa ninguna fila.
   */
  it('(50) la posición del recreo llega a la rejilla cuando llega la jornada', async () => {
    const grid = await montar([]);
    expect(grid.recreoTras()).toBeNull();

    sujetoJornada.next({
      persistida: true,
      tramos: [
        { dia: 'LUNES', orden: 1, esLectivo: true, ordenEnDia: 1, horaInicio: '08:00', horaFin: '09:00' },
        { dia: 'LUNES', orden: 2, esLectivo: true, ordenEnDia: 2, horaInicio: '09:00', horaFin: '10:00' },
        { dia: 'LUNES', orden: 3, esLectivo: true, ordenEnDia: 3, horaInicio: '10:00', horaFin: '11:00' },
        { dia: 'LUNES', orden: 4, esLectivo: false, ordenEnDia: null, horaInicio: '11:00', horaFin: '11:30' },
        { dia: 'LUNES', orden: 5, esLectivo: true, ordenEnDia: 4, horaInicio: '11:30', horaFin: '12:30' },
      ],
    });
    await fixture.whenStable();

    expect(grid.recreoTras()).toBe(3);
  });
});
