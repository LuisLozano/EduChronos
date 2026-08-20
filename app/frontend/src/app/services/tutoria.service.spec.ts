import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { TutoriaService } from './tutoria.service';
import { Tutoria, TutoriaRequest } from '../models/tutoria.model';

/**
 * Congela el CONTRATO REST de `TutoriaService`: verbo, URL y cuerpo de los DOS
 * endpoints del sub-recurso `/api/grupos/{idGrupo}/tutoria`. NO cubre traducción de
 * error (eso es de los componentes) ni la lógica del backend —el orden de validación,
 * I4, la idempotencia— que ya mide `TutoriaEndpointTest` en la JVM. Secuencia propia
 * del fichero desde (1), como `pdc.service.spec.ts` (D-S101-num).
 *
 * <p>`ID_GRUPO = 7`, no 1, y la URL esperada se escribe LITERAL en cada test en vez de
 * componerla con el template del fuente: con 1, una implementación que incrustara el id
 * a mano seguiría verde y la interpolación quedaría sin medir; componiéndola, el aserto
 * sería circular. Mismo criterio que `pdc.service.spec.ts` y `diagnostico.service.spec.ts`.
 *
 * <p>La URL literal mide además el SEGMENTO FINAL. `/api/grupos/7` es el CRUD del grupo
 * y `/api/grupos/7/pdc` es otro sub-recurso vivo sobre la misma base; un `expectOne` con
 * la ruta entera es lo único que caza que la petición no se haya ido a ninguno de los dos.
 *
 * <p>`verify()` en el `afterEach` es RED, no aserto: caza una petición no contemplada (un
 * método que dispare dos). En VERDE no lanza nunca. Le aplica la cascada documentada en
 * `bloqueo.service.spec.ts`: si `verify()` lanza, impide el reset del `TestBed` y el
 * `beforeEach` siguiente revienta con "Cannot configure the test module..."; durante una
 * campaña de mutación, el rojo que cuenta es el PRIMERO de la cadena.
 */

/** El id que viaja por el segmento del medio. Es el del GRUPO. Ver arriba: 7 y no 1. */
const ID_GRUPO = 7;

describe('TutoriaService', () => {
  let service: TutoriaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TutoriaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('(1) obtener → GET /api/grupos/{idGrupo}/tutoria', () => {
    service.obtener(ID_GRUPO).subscribe();

    const req = http.expectOne('/api/grupos/7/tutoria');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('(2) obtener devuelve la lista TAL CUAL la da el backend', () => {
    // DOS elementos y de roles DISTINTOS, en el orden en que el backend los sirve (el
    // principal primero): con uno solo, un servicio que devolviera `[lista[0]]` o que
    // reordenara seguiría verde. El wrapper es pelado y no debe tocar ni el contenido
    // ni el orden.
    const esperado: Tutoria[] = [
      { profesor: 'MAT1', rol: 'TUTOR_PRINCIPAL' },
      { profesor: 'LEN2', rol: 'CO_TUTOR' },
    ];
    let recibido: Tutoria[] | undefined;
    service.obtener(ID_GRUPO).subscribe((r) => (recibido = r));

    const req = http.expectOne('/api/grupos/7/tutoria');
    req.flush(esperado);

    expect(recibido).toEqual(esperado);
  });

  it('(3) reemplazar → PUT /api/grupos/{idGrupo}/tutoria con el array como cuerpo', () => {
    const cuerpo: TutoriaRequest[] = [
      { profesor: 'MAT1', rol: 'TUTOR_PRINCIPAL' },
      { profesor: 'LEN2', rol: 'CO_TUTOR' },
    ];
    let recibido: Tutoria[] | undefined;
    service.reemplazar(ID_GRUPO, cuerpo).subscribe((r) => (recibido = r));

    const req = http.expectOne('/api/grupos/7/tutoria');
    expect(req.request.method).toBe('PUT');
    // El cuerpo es el ARRAY DESNUDO, y el `toEqual` sobre el array entero lo mide: quien
    // lo envuelva en un `{ tutorias: [...] }` —o le cuele un `grupo`, que viaja por la
    // URL y no por el cuerpo— se pone rojo aquí, no con un 400 en producción.
    expect(req.request.body).toEqual(cuerpo);
    req.flush(cuerpo);

    expect(recibido).toEqual(cuerpo);
  });

  it('(4) reemplazar con [] manda un array VACÍO: es el gesto de quitar el tutor', () => {
    // No hay DELETE en el sub-recurso, así que este PUT es la ÚNICA forma de borrar la
    // tutoría. El aserto es sobre el cuerpo, no sobre el verbo: un servicio que
    // "optimizara" el caso vacío mandando `null`, omitiendo el cuerpo o saltándose la
    // petición dejaría la tutoría intacta en el servidor sin que nadie se enterase.
    service.reemplazar(ID_GRUPO, []).subscribe();

    const req = http.expectOne('/api/grupos/7/tutoria');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual([]);
    req.flush([]);
  });
});
