import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { JornadaService } from './jornada.service';
import { JornadaDTO, JornadaRequest } from '../models/jornada.model';

/**
 * Congela el CONTRATO REST de JornadaService: verbo, URL y cuerpo de los DOS endpoints
 * de `/api/jornada`. Son dos y no cinco porque el recurso es un singleton: no hay alta,
 * ni borrado, ni consulta por id. Secuencia propia del fichero desde (1).
 *
 * Frente a los clientes de catálogo añade la dimensión que hace especial a este
 * contrato: (2) verifica que el cuerpo del PUT es el DÍA TIPO recortado —tres campos por
 * tramo— y no lo que devuelve el GET. NO cubre traducción de error (eso es del
 * componente) ni la expansión a cinco días (eso es del backend).
 */
describe('JornadaService', () => {
  let service: JornadaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(JornadaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('(1) obtener → GET /api/jornada', () => {
    const esperado: JornadaDTO = {
      persistida: true,
      tramos: [
        {
          dia: 'LUNES',
          horaInicio: '08:00',
          horaFin: '09:00',
          esLectivo: true,
          orden: 1,
          ordenEnDia: 1,
        },
      ],
    };
    let recibido: JornadaDTO | undefined;
    service.obtener().subscribe((r) => (recibido = r));

    const req = http.expectOne('/api/jornada');
    expect(req.request.method).toBe('GET');
    req.flush(esperado);
    expect(recibido).toEqual(esperado);
  });

  it('(2) reemplazar → PUT /api/jornada con el día tipo, tres campos por tramo', () => {
    const cuerpo: JornadaRequest = {
      tramos: [
        { horaInicio: '08:00', horaFin: '09:00', esLectivo: true },
        { horaInicio: '09:00', horaFin: '09:30', esLectivo: false },
      ],
    };
    service.reemplazar(cuerpo).subscribe();

    const req = http.expectOne('/api/jornada');
    expect(req.request.method).toBe('PUT');
    // toEqual sobre el objeto entero: ni se añaden campos ni se omiten.
    expect(req.request.body).toEqual(cuerpo);
    req.flush({ persistida: true, tramos: [] });
  });
});
