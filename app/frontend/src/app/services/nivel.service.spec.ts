import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { NivelService } from './nivel.service';
import { Nivel } from '../models/nivel.model';

/**
 * Congela el CONTRATO REST de NivelService: verbo, URL y cuerpo de su ÚNICO endpoint.
 * Un solo caso porque el servicio expone un solo método a propósito (ver su
 * javadoc): el CRUD de `/api/niveles` existe en backend pero no tiene consumidor en
 * la UI. Secuencia propia del fichero desde (1).
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

  it('(1) listar → GET /api/niveles, respetando el orden que llega', () => {
    // DOS niveles, y en orden NO alfabético respecto a su `orden`, para que el
    // aserto de igualdad estricta caiga si alguien añadiera un `.sort()` en el
    // cliente: el backend ya ordena por `orden` (D-1) y reordenar aquí es un bug.
    const esperado: Nivel[] = [
      { id: 2, codigo: '1BACH', orden: 5 },
      { id: 1, codigo: '1ESO', orden: 1 },
    ];
    let recibido: Nivel[] | undefined;
    service.listar().subscribe((r) => (recibido = r));

    const req = http.expectOne('/api/niveles');
    expect(req.request.method).toBe('GET');
    req.flush(esperado);
    expect(recibido).toEqual(esperado);
  });
});
