import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { AulaService } from './aula.service';
import { Aula, AulaRequest } from '../models/aula.model';

/**
 * Congela el CONTRATO REST de AulaService: verbo, URL y cuerpo de cada uno de los 5
 * endpoints de `/api/aulas`. NO cubre traducción de error (eso es de los componentes)
 * ni la lógica del backend (eso es del backend). Secuencia propia del fichero desde
 * (1): no continúa la secuencia global de componentes, que hoy tiene colisiones
 * (D-S101-num).
 *
 * Frente al de profesores añade una dimensión: el cuerpo lleva los CUATRO opcionales
 * y (3) los envía en null, que es el caso que el backend persiste sin validar (D-4).
 */
describe('AulaService', () => {
  let service: AulaService;
  let http: HttpTestingController;

  /** DTO de referencia: opcionales POBLADOS, para distinguirlos de los null de (3). */
  const aulaCompleta: Aula = {
    id: 7,
    codigo: 'A12',
    tipo: 'INFORMATICA',
    capacidad: 30,
    edificio: 'Norte',
    planta: 1,
    sector: 'B',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AulaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('(1) listar → GET /api/aulas', () => {
    const esperado: Aula[] = [aulaCompleta];
    let recibido: Aula[] | undefined;
    service.listar().subscribe((r) => (recibido = r));

    const req = http.expectOne('/api/aulas');
    expect(req.request.method).toBe('GET');
    req.flush(esperado);
    expect(recibido).toEqual(esperado);
  });

  it('(2) obtener → GET /api/aulas/{id}', () => {
    service.obtener(7).subscribe();
    const req = http.expectOne('/api/aulas/7');
    expect(req.request.method).toBe('GET');
    req.flush(aulaCompleta);
  });

  it('(3) crear → POST /api/aulas con el body de request, opcionales en null', () => {
    const cuerpo: AulaRequest = {
      codigo: 'A12',
      tipo: 'ORDINARIA',
      capacidad: null,
      edificio: null,
      planta: null,
      sector: null,
    };
    service.crear(cuerpo).subscribe();
    const req = http.expectOne('/api/aulas');
    expect(req.request.method).toBe('POST');
    // toEqual sobre el objeto entero: los null viajan, no se omiten del JSON.
    expect(req.request.body).toEqual(cuerpo);
    req.flush({ id: 7, ...cuerpo });
  });

  it('(4) editar → PUT /api/aulas/{id} con el body de request', () => {
    const cuerpo: AulaRequest = {
      codigo: 'A12',
      tipo: 'INFORMATICA',
      capacidad: 30,
      edificio: 'Norte',
      planta: 1,
      sector: 'B',
    };
    service.editar(7, cuerpo).subscribe();
    const req = http.expectOne('/api/aulas/7');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(cuerpo);
    req.flush({ id: 7, ...cuerpo });
  });

  it('(5) borrar → DELETE /api/aulas/{id}', () => {
    service.borrar(7).subscribe();
    const req = http.expectOne('/api/aulas/7');
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});
