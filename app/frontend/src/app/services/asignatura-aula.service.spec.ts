import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AsignaturaAulaService } from './asignatura-aula.service';
import { AsignaturaAula } from '../models/asignatura-aula.model';

/**
 * Congela el CONTRATO REST de `AsignaturaAulaService` (S206): verbo, URL y cuerpo de los dos
 * endpoints de `/api/asignaturas/{id}/aulas`. Molde de `tutoria.service.spec.ts`: id 7 y no 1, y
 * URL LITERAL en cada caso, para que el segmento final (`/aulas` y no `/aulas-compatibles` ni el
 * CRUD de la asignatura) quede medido.
 */
const ID_ASIGNATURA = 7;

describe('AsignaturaAulaService', () => {
  let service: AsignaturaAulaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AsignaturaAulaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('(1) listar → GET /api/asignaturas/{id}/aulas y devuelve la lista tal cual', () => {
    const esperado: AsignaturaAula[] = [
      { aula: 'LAB1', rol: 'EXCLUSIVA' },
      { aula: 'LAB2', rol: 'EXCLUSIVA' },
    ];
    let recibido: AsignaturaAula[] | undefined;
    service.listar(ID_ASIGNATURA).subscribe((r) => (recibido = r));

    const req = http.expectOne('/api/asignaturas/7/aulas');
    expect(req.request.method).toBe('GET');
    req.flush(esperado);
    expect(recibido).toEqual(esperado);
  });

  it('(2) reemplazar → PUT /api/asignaturas/{id}/aulas con el array desnudo como cuerpo', () => {
    const cuerpo: AsignaturaAula[] = [
      { aula: 'A13', rol: 'PREFERIDA' },
      { aula: 'A14', rol: 'PREFERIDA' },
    ];
    let recibido: AsignaturaAula[] | undefined;
    service.reemplazar(ID_ASIGNATURA, cuerpo).subscribe((r) => (recibido = r));

    const req = http.expectOne('/api/asignaturas/7/aulas');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(cuerpo);
    req.flush(cuerpo);
    expect(recibido).toEqual(cuerpo);
  });

  it('(3) reemplazar con [] manda el array vacío: es como se quitan todas', () => {
    service.reemplazar(ID_ASIGNATURA, []).subscribe();

    const req = http.expectOne('/api/asignaturas/7/aulas');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual([]);
    req.flush([]);
  });
});
