import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { ConfiguracionGuardias, ConfiguracionGuardiasService } from './configuracion-guardias.service';

/**
 * Congela el CONTRATO REST de ConfiguracionGuardiasService (S212): verbo, URL y cuerpo de los
 * dos endpoints de `/api/configuracion-guardias`. El cuerpo del PUT se compara ENTERO: la clave
 * es `minimoPorTramo` y no otra. Secuencia propia del fichero desde (1).
 */
describe('ConfiguracionGuardiasService', () => {
  let service: ConfiguracionGuardiasService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ConfiguracionGuardiasService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('(1) obtener → GET /api/configuracion-guardias', () => {
    let recibido: ConfiguracionGuardias | undefined;
    service.obtener().subscribe((r) => (recibido = r));

    const req = http.expectOne('/api/configuracion-guardias');
    expect(req.request.method).toBe('GET');
    req.flush({ minimoPorTramo: 4 });
    expect(recibido).toEqual({ minimoPorTramo: 4 });
  });

  it('(2) guardar → PUT /api/configuracion-guardias con el cuerpo exacto { minimoPorTramo }', () => {
    let recibido: ConfiguracionGuardias | undefined;
    service.guardar(6).subscribe((r) => (recibido = r));

    const req = http.expectOne('/api/configuracion-guardias');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ minimoPorTramo: 6 });
    req.flush({ minimoPorTramo: 6 });
    expect(recibido).toEqual({ minimoPorTramo: 6 });
  });
});
