import { TestBed } from '@angular/core/testing';
import {
  provideHttpClient,
} from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { ProfesorService } from './profesor.service';
import { Profesor, ProfesorRequest } from '../models/profesor.model';

/**
 * Congela el CONTRATO REST de ProfesorService: verbo, URL y cuerpo de cada uno
 * de los 5 endpoints de `/api/profesores`. NO cubre traducción de error (eso es
 * de los componentes) ni la lógica del backend (eso es del backend). Secuencia
 * propia del fichero desde (1): no continúa la secuencia global de componentes,
 * que hoy tiene colisiones (ver nota de sesión).
 */
describe('ProfesorService', () => {
  let service: ProfesorService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProfesorService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('(1) listar → GET /api/profesores', () => {
    const esperado: Profesor[] = [
      { id: 7, codigo: 'MAT8', nombreCompleto: 'Ana Ruiz' },
    ];
    let recibido: Profesor[] | undefined;
    service.listar().subscribe((r) => (recibido = r));

    const req = http.expectOne('/api/profesores');
    expect(req.request.method).toBe('GET');
    req.flush(esperado);
    expect(recibido).toEqual(esperado);
  });

  it('(2) obtener → GET /api/profesores/{id}', () => {
    service.obtener(7).subscribe();
    const req = http.expectOne('/api/profesores/7');
    expect(req.request.method).toBe('GET');
    req.flush({ id: 7, codigo: 'MAT8', nombreCompleto: 'Ana Ruiz' });
  });

  it('(3) crear → POST /api/profesores con el body de request', () => {
    const cuerpo: ProfesorRequest = { codigo: 'MAT8', nombreCompleto: 'Ana Ruiz' };
    service.crear(cuerpo).subscribe();
    const req = http.expectOne('/api/profesores');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(cuerpo);
    req.flush({ id: 7, ...cuerpo });
  });

  it('(4) editar → PUT /api/profesores/{id} con el body de request', () => {
    const cuerpo: ProfesorRequest = { codigo: 'MAT8', nombreCompleto: 'Ana Ruiz' };
    service.editar(7, cuerpo).subscribe();
    const req = http.expectOne('/api/profesores/7');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(cuerpo);
    req.flush({ id: 7, ...cuerpo });
  });

  it('(5) borrar → DELETE /api/profesores/{id}', () => {
    service.borrar(7).subscribe();
    const req = http.expectOne('/api/profesores/7');
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});
