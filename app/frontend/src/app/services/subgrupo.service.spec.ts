import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { SubgrupoService } from './subgrupo.service';
import { Subgrupo, SubgrupoRequest } from '../models/subgrupo.model';

/**
 * Congela el CONTRATO REST de SubgrupoService: verbo, URL y cuerpo de cada uno de los
 * 5 endpoints de `/api/subgrupos`. NO cubre traducción de error (eso es de los
 * componentes) ni la lógica del backend (eso es del backend). Secuencia propia del
 * fichero desde (1): no continúa la secuencia global de componentes.
 */
describe('SubgrupoService', () => {
  let service: SubgrupoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SubgrupoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('(1) listar → GET /api/subgrupos', () => {
    const esperado: Subgrupo[] = [
      { id: 3, codigo: '1ºA-CyR-Tec', grupos: ['1ºA'] },
    ];
    let recibido: Subgrupo[] | undefined;
    service.listar().subscribe((r) => (recibido = r));

    const req = http.expectOne('/api/subgrupos');
    expect(req.request.method).toBe('GET');
    req.flush(esperado);
    expect(recibido).toEqual(esperado);
  });

  it('(2) obtener → GET /api/subgrupos/{id}', () => {
    service.obtener(3).subscribe();
    const req = http.expectOne('/api/subgrupos/3');
    expect(req.request.method).toBe('GET');
    req.flush({ id: 3, codigo: '1ºA-CyR-Tec', grupos: ['1ºA'] });
  });

  it('(3) crear → POST /api/subgrupos con el body de request', () => {
    const cuerpo: SubgrupoRequest = { codigo: '1ºA-CyR-Tec', grupos: ['1ºA'] };
    service.crear(cuerpo).subscribe();
    const req = http.expectOne('/api/subgrupos');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(cuerpo);
    req.flush({ id: 3, ...cuerpo });
  });

  it('(4) editar → PUT /api/subgrupos/{id} con el body de request', () => {
    const cuerpo: SubgrupoRequest = { codigo: '1ºA-CyR-Tec', grupos: ['1ºA', '1ºB'] };
    service.editar(3, cuerpo).subscribe();
    const req = http.expectOne('/api/subgrupos/3');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(cuerpo);
    req.flush({ id: 3, ...cuerpo });
  });

  it('(5) borrar → DELETE /api/subgrupos/{id}', () => {
    service.borrar(3).subscribe();
    const req = http.expectOne('/api/subgrupos/3');
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});
