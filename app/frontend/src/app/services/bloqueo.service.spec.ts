import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { BloqueoService } from './bloqueo.service';
import { BloqueoRequest } from '../models/bloqueo.model';

/**
 * CONTRATO DE ENDPOINTS, no cobertura de lógica. Los tres métodos son wrappers
 * pelados —`return this.http.<verbo><T>(url)` sin `.pipe`—, así que aquí no hay
 * comportamiento que medir: lo que este fichero congela son TRES RUTAS CONCRETAS
 * con su verbo, no tres dimensiones distintas de un mismo contrato. Con ellas se
 * estrena `HttpTestingController`, que hasta hoy no tenía uso efectivo en el repo.
 *
 * <p>`id = 7`, no 1: con 1, una implementación que incrustara el id a mano
 * seguiría verde y la interpolación de (11) quedaría sin medir. Mismo criterio que
 * el "índice 2 y no 1" de `horario-grid.spec.ts:52-56`. Por la misma razón cada URL
 * esperada se escribe LITERAL y no se compone con el template del fuente:
 * componerla allí volvería circular el aserto.
 *
 * <p>El `subscribe()` NO asevera el valor emitido. Existe solo para calentar el
 * Observable: sin suscripción `HttpClient` no emite petición y no habría nada que
 * interceptar.
 *
 * <p>`verify()` es RED, no aserto. Caza una petición no contemplada —un método que
 * dispare dos—, pero no es independientemente reddable bajo la campaña de
 * mutación. En VERDE no lanza nunca, y la patología que sigue SOLO existe bajo
 * mutación.
 *
 * <p>CASCADA, no doble fallo. Bajo una mutación, `verify()` lanza dentro del
 * `afterEach`, y eso impide el reset del `TestBed`: el `beforeEach` del test
 * SIGUIENTE de este mismo fichero revienta con "Cannot configure the test module
 * when the test module has already been instantiated". Medido en S85: M2 —una
 * mutación en la URL de `listar()`, que no toca `guardar` ni `borrar`— tumbó (9),
 * (10) y (11).
 *
 * <p>REGLA DE LECTURA para quien corra una campaña sobre este fichero: la VÍCTIMA
 * REAL es la que falla por `expectOne`; las COLATERALES fallan por "Cannot
 * configure the test module". Sin esa distinción, "cayeron tres tests" no atribuye
 * nada y la campaña deja de medir qué ruta se rompió.
 *
 * <p>Se decidió NO blindar el `afterEach` con `try/finally` +
 * `resetTestingModule()` ni partir el fichero en un `describe` por test: la cascada
 * solo aparece bajo mutación, y ambas alternativas meten maquinaria permanente o
 * rompen el "un describe por fichero" de `horario-view.spec.ts` y
 * `horario-grid.spec.ts`.
 *
 * <p>El matcher compara contra `urlWithParams` por igualdad ESTRICTA de string.
 * Hoy es indiferente —ningún método pasa `options.params`—, pero si alguien se los
 * añadiera a `listar()`, (9) se pondría rojo y ESE ROJO SERÍA CORRECTO: la URL
 * efectiva habría cambiado.
 *
 * <p>QUEDA FUERA POR ALCANCE, NO POR IMPOSIBILIDAD: la propagación del 400 que
 * documenta el servicio y "devuelve lo que llega" SÍ son reddables —caen bajo
 * `catchError(() => of(null))` y `map((x) => ({ ...x }))`, y ambas compilan—. Se
 * dejan fuera porque iv-A no es cobertura de lógica. El único item de verdad
 * no-reddable es el TIPO GENÉRICO: se borra en runtime.
 *
 * <p>La mitad `method` del matcher está VIVA, no es adorno. Confirmado en S85 con
 * la mutación `delete<void>` → `get<void>` sobre `borrar`: (11) cae con la URL
 * recibida `/api/bloqueos/7` IDÉNTICA a la esperada, siendo el verbo lo único que
 * difiere. Por eso D4 —aseverar por verbo Y URL, nunca solo por URL— tiene
 * contenido: `_match` no restringe por un campo omitido.
 *
 * <p>NOTA DE MANTENIMIENTO sobre la campaña de mutación de (11): la mutación que
 * quita `${id}` de la URL compila HOY porque `noUnusedParameters` no está activado
 * ni se hereda de `strict` —no forma parte de él—, así que el parámetro `id` puede
 * quedar sin usar. VERIFICADO sobre el árbol real en S85, no supuesto: la mutación
 * se aplicó, compiló sin error TS y cayó por `expectOne`. Si algún día se activa
 * `noUnusedParameters`, esa mutación deja de ser expresable y hay que sustituirla
 * por `/api/bloqueos/${id}` → `/api/bloqueo/${id}`, que conserva el uso del
 * parámetro y sigue moviendo la URL.
 */

/** El id que viaja por la ruta del DELETE. Ver arriba: 7 y no 1. */
const ID = 7;

/**
 * Cuerpo del POST, LOCAL a este spec: no se toca
 * `testing/proyeccion-1eso.fixture.ts`, que es de otra capa y está commiteado.
 *
 * <p>Calibrado en tres dimensiones para que (10) no pase por acumulación:
 * `indice` es 2 y no 1 (un cuerpo que fijara el índice a mano seguiría verde),
 * `dia` y `orden` son distintos entre sí (si se permutaran, con `dia === orden` el
 * cuerpo saldría idéntico) y `aulas` NO está vacío (con la lista vacía, un
 * serializado que perdiera las aulas no se distinguiría del correcto).
 */
const PETICION: BloqueoRequest = {
  actividadCodigo: 'Bloque-CyR_OyD_RefMt-1ESO',
  indice: 2,
  tramo: { dia: 3, orden: 4 },
  aulas: [{ plazaCodigo: 'Bloque-CyR-Inf', aulaCodigo: 'A12In' }],
};

describe('cliente de bloqueos', () => {
  let servicio: BloqueoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    servicio = TestBed.inject(BloqueoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('(9) listar pide GET a la colección de bloqueos', () => {
    servicio.listar().subscribe();

    const req = http.expectOne({ method: 'GET', url: '/api/bloqueos' });

    req.flush(null);
  });

  /**
   * `toEqual` y no `toBe`: lo que se congela es el cuerpo que viaja por el cable,
   * y ahí una copia superficial es indistinguible del original. Aseverar la
   * IDENTIDAD del objeto pondría rojo un `{ ...peticion }`, que no cambia nada
   * observable —el JSON serializado es el mismo— y por tanto no es un defecto:
   * sería un falso positivo contra una refactorización legítima.
   */
  it('(10) guardar pide POST a la colección con el cuerpo de la petición', () => {
    servicio.guardar(PETICION).subscribe();

    const req = http.expectOne({ method: 'POST', url: '/api/bloqueos' });
    expect(req.request.body).toEqual(PETICION);

    req.flush(null);
  });

  it('(11) borrar pide DELETE a la ruta del bloqueo con el id interpolado', () => {
    servicio.borrar(ID).subscribe();

    const req = http.expectOne({ method: 'DELETE', url: '/api/bloqueos/7' });

    req.flush(null);
  });
});
