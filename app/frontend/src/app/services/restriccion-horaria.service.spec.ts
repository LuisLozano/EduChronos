import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { RestriccionHorariaService } from './restriccion-horaria.service';
import { RestriccionHoraria, RestriccionHorariaRequest } from '../models/restriccion-horaria.model';

/**
 * Congela el CONTRATO REST de `RestriccionHorariaService`: verbo, URL y cuerpo de los DOS
 * endpoints del sub-recurso. Molde de `tutoria.service.spec.ts`: la lógica del backend
 * (orden de validación, reemplazo total) la mide la JVM, aquí solo el cable.
 *
 * <p>`ID_PROFESOR = 7`, no 1, y la URL LITERAL en cada caso: con 1 un id incrustado a mano
 * seguiría verde, y componer la URL con el template del fuente haría el aserto circular.
 * La URL entera mide además el SEGMENTO FINAL: `/api/profesores/7` es el CRUD del profesor.
 *
 * <p>`verify()` en el `afterEach` es RED, no aserto: caza una petición de más.
 */

/** El id que viaja por el segmento del medio. Ver arriba: 7 y no 1. */
const ID_PROFESOR = 7;

const URL = '/api/profesores/7/restricciones-horarias';

describe('RestriccionHorariaService', () => {
  let service: RestriccionHorariaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(RestriccionHorariaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('(1) obtener → GET /api/profesores/{idProfesor}/restricciones-horarias', () => {
    service.obtener(ID_PROFESOR).subscribe();

    const req = http.expectOne(URL);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('(2) obtener devuelve la lista TAL CUAL la da el backend, motivo null incluido', () => {
    // Dos elementos de tipos DISTINTOS, uno con motivo y otro con null: con uno solo, un
    // wrapper que devolviera `[lista[0]]` o que "limpiara" los null seguiría verde.
    const esperado: RestriccionHoraria[] = [
      { tipo: 'DURA', dia: 1, ordenEnDia: 1, motivo: 'm2' },
      { tipo: 'BLANDA', dia: 2, ordenEnDia: 2, motivo: null },
    ];
    let recibido: RestriccionHoraria[] | undefined;
    service.obtener(ID_PROFESOR).subscribe((r) => (recibido = r));

    http.expectOne(URL).flush(esperado);

    expect(recibido).toEqual(esperado);
  });

  it('(3) reemplazar → PUT con el ARRAY DESNUDO como cuerpo', () => {
    const cuerpo: RestriccionHorariaRequest[] = [
      { tipo: 'DURA', dia: 1, ordenEnDia: 1, motivo: 'm2' },
      { tipo: 'BLANDA', dia: 2, ordenEnDia: 2, motivo: null },
    ];
    let recibido: RestriccionHoraria[] | undefined;
    service.reemplazar(ID_PROFESOR, cuerpo).subscribe((r) => (recibido = r));

    const req = http.expectOne(URL);
    expect(req.request.method).toBe('PUT');
    // `toEqual` sobre el array entero: un envoltorio `{ restricciones: [...] }` o un
    // `profesor` colado en el cuerpo —viaja por la URL— se ponen rojos aquí.
    expect(req.request.body).toEqual(cuerpo);
    req.flush(cuerpo);

    expect(recibido).toEqual(cuerpo);
  });

  it('(4) reemplazar con [] manda un array VACÍO: es como se quitan todas', () => {
    service.reemplazar(ID_PROFESOR, []).subscribe();

    const req = http.expectOne(URL);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual([]);
    req.flush([]);
  });
});
