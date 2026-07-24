import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { HorarioService } from './horario.service';

/**
 * CONTRATO DE ENDPOINT, no cobertura de lógica. `getProyeccion` es un wrapper
 * pelado —un `return this.http.get<T>(url)` sin `.pipe`—, así que aquí no hay
 * comportamiento que medir: lo que este fichero congela es el par (verbo, URL) de
 * UNA ruta concreta. Con él se estrena además `HttpTestingController`, que hasta
 * hoy no tenía ningún uso efectivo en el repo: su única mención era el comentario
 * de `horario-view.spec.ts` que declara por qué allí NO se usa.
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
 * <p>`verify()` es RED, no aserto. Caza una petición no contemplada —un método
 * que dispare dos—, pero no es independientemente reddable bajo la campaña de
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
 * <p>QUEDA FUERA POR ALCANCE, NO POR IMPOSIBILIDAD: la propagación del error y
 * "devuelve lo que llega" SÍ son reddables —caen bajo `catchError(() => of(null))`
 * y `map((x) => ({ ...x }))` respectivamente, y ambas compilan—. Se dejan fuera
 * porque iv-A no es cobertura de lógica. El único item de verdad no-reddable es el
 * TIPO GENÉRICO: se borra en runtime, y `get<HorarioProyeccion>` y `get<any>`
 * producen exactamente la misma petición.
 */

/** El id que viaja por la ruta. Ver arriba: 7 y no 1. */
const ID = 7;

describe('cliente de horarios', () => {
  let servicio: HorarioService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    servicio = TestBed.inject(HorarioService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('(8) getProyeccion pide GET a la ruta de proyección con el id interpolado', () => {
    servicio.getProyeccion(ID).subscribe();

    const req = http.expectOne({ method: 'GET', url: '/api/horarios/7/proyeccion' });

    req.flush(null);
  });

  /**
   * `generar` congela el otro par (verbo, URL): POST a la colección, sin id. El
   * cuerpo se asevera `{}` LITERAL —no cualquier body—: una mutación que colara
   * parámetros (p. ej. `{ maxSegundos: 30 }`) cambiaría el contrato con el backend
   * y ESTE ROJO SERÍA CORRECTO. El body vacío es deliberado: el backend acepta
   * `{}` y cae a sus defaults (S-medido).
   */
  it('(33) generar pide POST a la colección con body vacío', () => {
    servicio.generar().subscribe();

    const req = http.expectOne({ method: 'POST', url: '/api/horarios' });
    expect(req.request.body).toEqual({});

    req.flush(null);
  });
});
