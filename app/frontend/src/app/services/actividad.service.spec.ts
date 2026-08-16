import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { ActividadService } from './actividad.service';
import { Actividad, ActividadRequest } from '../models/actividad.model';

/**
 * Congela el CONTRATO REST de ActividadService: verbo, URL y cuerpo de cada uno de los 5
 * endpoints de `/api/actividades`. NO cubre traducción de error (eso es de los
 * componentes) ni la lógica del backend (eso es del backend). Secuencia propia del
 * fichero desde (1).
 *
 * <p>El cuerpo de ejemplo lleva la plaza EMBEBIDA y con la rama de aula fija del XOR:
 * así el aserto de igualdad estricta mide que el agregado viaja entero, y que ni `id` ni
 * `codigo` de plaza se cuelan en el request (no están en `PlazaRequest`).
 */
describe('ActividadService', () => {
  let service: ActividadService;
  let http: HttpTestingController;

  const PETICION: ActividadRequest = {
    codigo: 'Mat-1ºA',
    asignatura: 'Mat',
    duracionTramos: 1,
    repeticionesPorSemana: 4,
    patronTemporal: 'DISTRIBUIDA',
    requiereTutor: false,
    plazas: [
      {
        asignatura: 'Mat',
        aulaFija: 'A1',
        aulasCandidatas: [],
        profesores: ['MATA'],
        subgrupos: ['1ºA-Completo'],
      },
    ],
  };

  const RESPUESTA: Actividad = {
    id: 7,
    codigo: 'Mat-1ºA',
    asignatura: 'Mat',
    duracionTramos: 1,
    repeticionesPorSemana: 4,
    patronTemporal: 'DISTRIBUIDA',
    requiereTutor: false,
    plazas: [
      {
        id: 11,
        codigo: 'Mat-1ºA-P1',
        asignatura: 'Mat',
        aulaFija: 'A1',
        aulasCandidatas: [],
        profesores: ['MATA'],
        subgrupos: ['1ºA-Completo'],
      },
    ],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ActividadService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('(1) listar → GET /api/actividades', () => {
    let recibido: Actividad[] | undefined;
    service.listar().subscribe((r) => (recibido = r));

    const req = http.expectOne('/api/actividades');
    expect(req.request.method).toBe('GET');
    req.flush([RESPUESTA]);
    // La igualdad estricta mide que el agregado llega ENTERO, plazas incluidas.
    expect(recibido).toEqual([RESPUESTA]);
  });

  it('(2) obtener → GET /api/actividades/{id}', () => {
    service.obtener(7).subscribe();
    const req = http.expectOne('/api/actividades/7');
    expect(req.request.method).toBe('GET');
    req.flush(RESPUESTA);
  });

  it('(3) crear → POST /api/actividades con el body de request', () => {
    service.crear(PETICION).subscribe();
    const req = http.expectOne('/api/actividades');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(PETICION);
    req.flush(RESPUESTA);
  });

  it('(4) editar → PUT /api/actividades/{id} con el body de request', () => {
    service.editar(7, PETICION).subscribe();
    const req = http.expectOne('/api/actividades/7');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(PETICION);
    req.flush(RESPUESTA);
  });

  it('(5) borrar → DELETE /api/actividades/{id}', () => {
    service.borrar(7).subscribe();
    const req = http.expectOne('/api/actividades/7');
    expect(req.request.method).toBe('DELETE');
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});
