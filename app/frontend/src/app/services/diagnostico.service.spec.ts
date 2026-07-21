import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DiagnosticoService } from './diagnostico.service';

/**
 * CONTRATO DE ENDPOINT, no cobertura de lógica. `getDiagnostico` es un wrapper
 * pelado —un `return this.http.get<T>(url)` sin `.pipe`—, así que aquí no hay
 * comportamiento que medir: lo que este fichero congela es el par (verbo, URL) de
 * UNA ruta concreta. Con él se estrena `HttpTestingController`, que hasta hoy no
 * tenía uso efectivo en el repo.
 *
 * <p>`id = 7`, no 1: con 1, una implementación que incrustara el id a mano
 * seguiría verde y la interpolación quedaría sin medir. Mismo criterio que el
 * "índice 2 y no 1" de `horario-grid.spec.ts:52-56`. Por la misma razón la URL
 * esperada se escribe LITERAL y no se compone con el template del fuente:
 * componerla allí volvería circular el aserto.
 *
 * <p>El `subscribe()` NO asevera el valor emitido. Existe solo para calentar el
 * Observable: sin suscripción `HttpClient` no emite petición y no habría nada que
 * interceptar.
 *
 * <p>`verify()` es RED, no aserto. Caza una petición no contemplada —un método que
 * dispare dos—, pero no es independientemente reddable bajo la campaña de
 * mutación. En VERDE no lanza nunca.
 *
 * <p>Este fichero tiene UN SOLO test, así que la CASCADA que `verify()` provoca en
 * los ficheros de varios —lanza dentro del `afterEach`, eso impide el reset del
 * `TestBed`, y el `beforeEach` del test siguiente revienta con "Cannot configure
 * the test module when the test module has already been instantiated"— aquí NO
 * PUEDE DARSE: no hay test posterior al que contaminar. La patología y la regla de
 * lectura que hace falta para atribuir un rojo durante una campaña están
 * documentadas en `bloqueo.service.spec.ts`, que sí tiene tres tests.
 *
 * <p>El matcher compara contra `urlWithParams` por igualdad ESTRICTA de string.
 * Hoy es indiferente —ningún método de los tres servicios pasa `options.params`—,
 * pero si alguien se los añadiera a este método, este test se pondría rojo y ESE
 * ROJO SERÍA CORRECTO: la URL efectiva habría cambiado.
 *
 * <p>QUEDA FUERA POR ALCANCE, NO POR IMPOSIBILIDAD, y aquí importa señalarlo: el
 * TSDoc de `diagnostico.service.ts` dedica seis líneas a defender que el 404 se
 * PROPAGA sin traducir, porque "no hay horario" y "horario sin violaciones" son
 * estados distintos. Esa afirmación SÍ es testable —cae bajo
 * `catchError(() => of(null))`, que compila—, igual que "devuelve lo que llega"
 * cae bajo `map((x) => ({ ...x }))`. No se aseveran aquí porque iv-A no es
 * cobertura de lógica, NO porque no puedan ponerse rojas. El único item de verdad
 * no-reddable es el TIPO GENÉRICO: se borra en runtime, y `get<Diagnostico>` y
 * `get<any>` producen exactamente la misma petición.
 */

/** El id que viaja por la ruta. Ver arriba: 7 y no 1. */
const ID = 7;

describe('cliente de diagnóstico', () => {
  let servicio: DiagnosticoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    servicio = TestBed.inject(DiagnosticoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('(12) getDiagnostico pide GET a la ruta de diagnóstico con el id interpolado', () => {
    servicio.getDiagnostico(ID).subscribe();

    const req = http.expectOne({ method: 'GET', url: '/api/horarios/7/diagnostico' });

    req.flush(null);
  });
});
