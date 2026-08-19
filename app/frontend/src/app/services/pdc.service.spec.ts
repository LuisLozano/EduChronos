import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { PdcService } from './pdc.service';
import { Grupo, PdcRequest } from '../models/grupo.model';

/**
 * Congela el CONTRATO REST de `PdcService`: verbo, URL y cuerpo de los tres endpoints
 * del sub-recurso `/api/grupos/{idPadre}/pdc`. NO cubre traducción de error (eso es de
 * los componentes) ni la lógica del backend (eso es del backend). Secuencia propia del
 * fichero desde (1), como `grupo.service.spec.ts`.
 *
 * <p>`ID_PADRE = 7`, no 1, y la URL esperada se escribe LITERAL en cada test en vez de
 * componerla con el template del fuente: con 1, una implementación que incrustara el id
 * a mano seguiría verde y la interpolación quedaría sin medir; componiéndola, el aserto
 * sería circular. Mismo criterio que `diagnostico.service.spec.ts`.
 *
 * <p><b>Aquí ese literal mide algo más que la interpolación.</b> Los tres métodos
 * reciben el id DEL PADRE, y el error de uso natural desde un componente es pasarles el
 * id del PDC —los dos son `number`, así que TypeScript no dice nada—. Una URL completa
 * y literal es lo único que caza que el segmento del medio sea el que toca.
 *
 * <p>`verify()` en el `afterEach` es RED, no aserto: caza una petición no contemplada
 * (un método que dispare dos). En VERDE no lanza nunca. Este fichero tiene cuatro tests,
 * así que le aplica la cascada documentada en `bloqueo.service.spec.ts`: si `verify()`
 * lanza, impide el reset del `TestBed` y el `beforeEach` siguiente revienta con "Cannot
 * configure the test module..."; durante una campaña de mutación, el rojo que cuenta es
 * el PRIMERO de la cadena.
 */

/** El id que viaja por el PRIMER segmento de la ruta. Es el del PADRE. Ver arriba: 7 y no 1. */
const ID_PADRE = 7;

describe('PdcService', () => {
  let service: PdcService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PdcService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('(1) obtener → GET /api/grupos/{idPadre}/pdc y devuelve el Grupo', () => {
    const esperado: Grupo = {
      id: 42,
      codigo: '3ADI',
      nivel: '3ESO',
      tipo: 'DIVERSIFICACION_PDC',
    };
    let recibido: Grupo | undefined;
    service.obtener(ID_PADRE).subscribe((r) => (recibido = r));

    const req = http.expectOne('/api/grupos/7/pdc');
    expect(req.request.method).toBe('GET');
    req.flush(esperado);

    expect(recibido).toEqual(esperado);
  });

  it('(2) obtener PROPAGA el 404 sin transformarlo', () => {
    // Congela la decisión del TSDoc de `obtener`: el 404 ("este padre no tiene PDC")
    // es un estado legítimo, pero se deja SUBIR al consumidor. Si alguien mete un
    // `catchError(() => of(null))` en el servicio, el subscriber recibiría un valor
    // en vez de un error y estas dos aserciones se ponen rojas.
    let valor: Grupo | undefined;
    let error: HttpErrorResponse | undefined;
    service.obtener(ID_PADRE).subscribe({
      next: (r) => (valor = r),
      error: (e: HttpErrorResponse) => (error = e),
    });

    const req = http.expectOne('/api/grupos/7/pdc');
    req.flush('sin PDC', { status: 404, statusText: 'Not Found' });

    expect(valor).toBeUndefined();
    expect(error?.status).toBe(404);
  });

  it('(3) crear → POST /api/grupos/{idPadre}/pdc con el body de request', () => {
    const cuerpo: PdcRequest = { codigo: '3ADI' };
    const esperado: Grupo = {
      id: 42,
      codigo: '3ADI',
      nivel: '3ESO',
      tipo: 'DIVERSIFICACION_PDC',
    };
    let recibido: Grupo | undefined;
    service.crear(ID_PADRE, cuerpo).subscribe((r) => (recibido = r));

    const req = http.expectOne('/api/grupos/7/pdc');
    expect(req.request.method).toBe('POST');
    // Sobre el cuerpo COMPLETO (`toEqual`, no una propiedad suelta): así cae también
    // quien "por si acaso" le añada un `nivel`, un `tipo` o un `idPadre` que el
    // backend no lee.
    expect(req.request.body).toEqual(cuerpo);
    req.flush(esperado);

    expect(recibido).toEqual(esperado);
  });

  it('(4) borrar → DELETE /api/grupos/{idPadre}/pdc y completa sin cuerpo', () => {
    let recibido: void | undefined;
    let completado = false;
    service.borrar(ID_PADRE).subscribe({
      next: (r) => (recibido = r),
      complete: () => (completado = true),
    });

    const req = http.expectOne('/api/grupos/7/pdc');
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });

    expect(recibido).toBeNull();
    expect(completado).toBe(true);
  });
});
