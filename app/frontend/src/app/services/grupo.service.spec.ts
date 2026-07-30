import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { GrupoService } from './grupo.service';
import { Grupo, GrupoRequest } from '../models/grupo.model';

/**
 * Congela el CONTRATO REST de GrupoService: verbo, URL y cuerpo de cada uno de los
 * 5 endpoints de `/api/grupos`. NO cubre traducción de error (eso es de los
 * componentes) ni la lógica del backend (eso es del backend). Secuencia propia del
 * fichero desde (1): no continúa la secuencia global de componentes, que hoy tiene
 * colisiones (ver nota de sesión).
 */
describe('GrupoService', () => {
  let service: GrupoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(GrupoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('(1) listar → GET /api/grupos', () => {
    const esperado: Grupo[] = [
      { id: 7, codigo: '1ESOA', nivel: '1ESO', tipo: 'ORDINARIO' },
    ];
    let recibido: Grupo[] | undefined;
    service.listar().subscribe((r) => (recibido = r));

    const req = http.expectOne('/api/grupos');
    expect(req.request.method).toBe('GET');
    req.flush(esperado);
    expect(recibido).toEqual(esperado);
  });

  it('(2) obtener → GET /api/grupos/{id}', () => {
    service.obtener(7).subscribe();
    const req = http.expectOne('/api/grupos/7');
    expect(req.request.method).toBe('GET');
    req.flush({ id: 7, codigo: '1ESOA', nivel: '1ESO', tipo: 'ORDINARIO' });
  });

  it('(3) crear → POST /api/grupos con el body de request', () => {
    const cuerpo: GrupoRequest = { codigo: '1ESOA', nivel: '1ESO', tipo: 'ORDINARIO' };
    service.crear(cuerpo).subscribe();
    const req = http.expectOne('/api/grupos');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(cuerpo);
    req.flush({ id: 7, ...cuerpo });
  });

  it('(4) editar → PUT /api/grupos/{id} con el body de request', () => {
    const cuerpo: GrupoRequest = { codigo: '1ESOA', nivel: '1ESO', tipo: 'ORDINARIO' };
    service.editar(7, cuerpo).subscribe();
    const req = http.expectOne('/api/grupos/7');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(cuerpo);
    req.flush({ id: 7, ...cuerpo });
  });

  it('(5) borrar → DELETE /api/grupos/{id}', () => {
    service.borrar(7).subscribe();
    const req = http.expectOne('/api/grupos/7');
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});
