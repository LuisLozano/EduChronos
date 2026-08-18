import { TestBed } from '@angular/core/testing';
import {
  provideHttpClient,
} from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { NivelService } from './nivel.service';
import { Nivel, NivelRequest } from '../models/nivel.model';

/**
 * Congela el CONTRATO REST de NivelService: verbo, URL y cuerpo de cada uno de los
 * 5 endpoints de `/api/niveles`. NO cubre traducción de error (eso es de los
 * componentes) ni la lógica del backend (eso es del backend). Secuencia propia del
 * fichero desde (1): no continúa la secuencia global de componentes, que hoy tiene
 * colisiones (D-S101-num).
 */
describe('NivelService', () => {
  let service: NivelService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(NivelService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('(1) listar → GET /api/niveles, y el cliente NO reordena lo que llega', () => {
    // Orden pedagógico (D-1) CRUZADO con el alfabético: si algún día alguien
    // colase un sort por `codigo` en el cliente, este caso se pondría rojo.
    const esperado: Nivel[] = [
      { id: 3, codigo: '1ESO', orden: 1 },
      { id: 1, codigo: '2ESO', orden: 2 },
      { id: 2, codigo: '1BACH', orden: 3 },
    ];
    let recibido: Nivel[] | undefined;
    service.listar().subscribe((r) => (recibido = r));

    const req = http.expectOne('/api/niveles');
    expect(req.request.method).toBe('GET');
    req.flush(esperado);
    expect(recibido).toEqual(esperado);
    expect(recibido?.map((n) => n.codigo)).toEqual(['1ESO', '2ESO', '1BACH']);
  });

  it('(2) obtener → GET /api/niveles/{id}', () => {
    service.obtener(7).subscribe();
    const req = http.expectOne('/api/niveles/7');
    expect(req.request.method).toBe('GET');
    req.flush({ id: 7, codigo: '1ESO', orden: 1 });
  });

  it('(3) crear → POST /api/niveles con el body de request', () => {
    const cuerpo: NivelRequest = { codigo: '1ESO', orden: 1 };
    service.crear(cuerpo).subscribe();
    const req = http.expectOne('/api/niveles');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(cuerpo);
    req.flush({ id: 7, ...cuerpo });
  });

  it('(4) editar → PUT /api/niveles/{id} con el body de request', () => {
    const cuerpo: NivelRequest = { codigo: '1ESO', orden: 1 };
    service.editar(7, cuerpo).subscribe();
    const req = http.expectOne('/api/niveles/7');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(cuerpo);
    req.flush({ id: 7, ...cuerpo });
  });

  it('(5) borrar → DELETE /api/niveles/{id}', () => {
    service.borrar(7).subscribe();
    const req = http.expectOne('/api/niveles/7');
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});
