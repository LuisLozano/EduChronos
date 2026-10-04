import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { VersionService } from './version.service';
import { VersionDTO } from '../models/version.model';

/**
 * Congela el CONTRATO REST de VersionService: verbo y URL del único endpoint de
 * `/api/version`. Secuencia propia del fichero desde (1). NO cubre traducción de error (eso
 * es del componente).
 */
describe('VersionService', () => {
  let service: VersionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(VersionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('(1) obtener → GET /api/version, y devuelve el cuerpo', () => {
    const esperado: VersionDTO = { version: '0.3.0' };
    let recibido: VersionDTO | undefined;
    service.obtener().subscribe((r) => (recibido = r));

    const req = http.expectOne('/api/version');
    expect(req.request.method).toBe('GET');
    req.flush(esperado);
    expect(recibido).toEqual(esperado);
  });
});
