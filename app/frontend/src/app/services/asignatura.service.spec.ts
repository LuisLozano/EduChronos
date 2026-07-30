import { TestBed } from '@angular/core/testing';
import {
  provideHttpClient,
} from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { AsignaturaService } from './asignatura.service';
import { Asignatura, AsignaturaRequest } from '../models/asignatura.model';

/**
 * Congela el CONTRATO REST de AsignaturaService: verbo, URL y cuerpo de cada uno
 * de los 5 endpoints de `/api/asignaturas`. NO cubre traducción de error (eso es
 * de los componentes) ni la lógica del backend (eso es del backend). Secuencia
 * propia del fichero desde (1): no continúa la secuencia global de componentes,
 * que hoy tiene colisiones (ver nota de sesión).
 */
describe('AsignaturaService', () => {
  let service: AsignaturaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AsignaturaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('(1) listar → GET /api/asignaturas', () => {
    const esperado: Asignatura[] = [
      { id: 7, codigo: 'Mat', nombreCompleto: 'Matemáticas' },
    ];
    let recibido: Asignatura[] | undefined;
    service.listar().subscribe((r) => (recibido = r));

    const req = http.expectOne('/api/asignaturas');
    expect(req.request.method).toBe('GET');
    req.flush(esperado);
    expect(recibido).toEqual(esperado);
  });

  it('(2) obtener → GET /api/asignaturas/{id}', () => {
    service.obtener(7).subscribe();
    const req = http.expectOne('/api/asignaturas/7');
    expect(req.request.method).toBe('GET');
    req.flush({ id: 7, codigo: 'Mat', nombreCompleto: 'Matemáticas' });
  });

  it('(3) crear → POST /api/asignaturas con el body de request', () => {
    const cuerpo: AsignaturaRequest = { codigo: 'Mat', nombreCompleto: 'Matemáticas' };
    service.crear(cuerpo).subscribe();
    const req = http.expectOne('/api/asignaturas');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(cuerpo);
    req.flush({ id: 7, ...cuerpo });
  });

  it('(4) editar → PUT /api/asignaturas/{id} con el body de request', () => {
    const cuerpo: AsignaturaRequest = { codigo: 'Mat', nombreCompleto: 'Matemáticas' };
    service.editar(7, cuerpo).subscribe();
    const req = http.expectOne('/api/asignaturas/7');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(cuerpo);
    req.flush({ id: 7, ...cuerpo });
  });

  it('(5) borrar → DELETE /api/asignaturas/{id}', () => {
    service.borrar(7).subscribe();
    const req = http.expectOne('/api/asignaturas/7');
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});
