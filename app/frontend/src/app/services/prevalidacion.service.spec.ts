import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PrevalidacionService } from './prevalidacion.service';

/**
 * CONTRATO DE ENDPOINT, no cobertura de lógica. `getPrevalidacion` es un wrapper
 * pelado —`return this.http.get<T>(url)` sin `.pipe`—, así que lo que este fichero
 * congela es el par (verbo, URL) de UNA ruta. Mismo patrón que
 * `diagnostico.service.spec.ts` y `horario.service.spec.ts` (S85).
 *
 * <p>SIN convención `id = 7`, a diferencia de los otros dos: esta ruta NO lleva
 * id —la pre-validación es del catálogo global—, así que no hay interpolación que
 * medir. La URL se escribe LITERAL, no se compone desde el fuente: componerla
 * volvería circular el aserto.
 *
 * <p>El `subscribe()` NO asevera el valor emitido; solo calienta el Observable
 * —sin suscripción `HttpClient` no emite petición—. `verify()` es RED, no aserto:
 * caza una petición no contemplada, en verde no lanza. Con UN solo test no hay
 * cascada de `TestBed` que atribuir (documentada en `bloqueo.service.spec.ts`).
 */
describe('cliente de pre-validación', () => {
  let servicio: PrevalidacionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    servicio = TestBed.inject(PrevalidacionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('(27) getPrevalidacion pide GET a la colección de pre-validación', () => {
    servicio.getPrevalidacion().subscribe();

    const req = http.expectOne({ method: 'GET', url: '/api/prevalidacion' });

    req.flush(null);
  });
});
