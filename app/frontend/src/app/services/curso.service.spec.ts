import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { CursoService } from './curso.service';
import { CursoDTO, CursoListadoDTO } from '../models/curso.model';

/**
 * Congela el CONTRATO REST de CursoService: verbo, URL y cuerpo de los cuatro endpoints
 * de curso (O-curso, S160). Secuencia propia del fichero desde (1).
 *
 * <p>Frente a los clientes de catálogo añade la dimensión que hace especial a este
 * contrato: son DOS recursos con cardinalidades distintas —`/api/curso` el abierto y
 * `/api/cursos` la colección— y la diferencia de una sola letra entre ellos es justo el
 * tipo de cosa que un refactor rompe en silencio. Por eso las URLs se aseveran literales,
 * no compuestas.
 *
 * <p>NO cubre traducción de error (eso es de los componentes) ni qué hace la aplicación
 * después de abrir o duplicar (eso es del diálogo, que recarga).
 */
describe('CursoService', () => {
  let service: CursoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CursoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('(1) obtener → GET /api/curso, singular', () => {
    const esperado: CursoDTO = {
      nombre: '2025/2026',
      archivado: false,
      propuestaSiguiente: '2026/2027',
    };
    let recibido: CursoDTO | undefined;
    service.obtener().subscribe((r) => (recibido = r));

    const req = http.expectOne('/api/curso');
    expect(req.request.method).toBe('GET');
    req.flush(esperado);
    expect(recibido).toEqual(esperado);
  });

  it('(2) listar → GET /api/cursos, plural', () => {
    const esperado: CursoListadoDTO[] = [
      { fichero: 'curso-2026-2027.db', nombre: '2026/2027', archivado: false, abierto: true },
      { fichero: 'educhronos.db', nombre: '2025/2026', archivado: true, abierto: false },
    ];
    let recibido: CursoListadoDTO[] | undefined;
    service.listar().subscribe((r) => (recibido = r));

    const req = http.expectOne('/api/cursos');
    expect(req.request.method).toBe('GET');
    req.flush(esperado);
    expect(recibido).toEqual(esperado);
  });

  it('(3) abrir → POST /api/cursos/abrir con el fichero en el cuerpo', () => {
    const resultado: CursoDTO = {
      nombre: '2025/2026',
      archivado: true,
      propuestaSiguiente: '2026/2027',
    };
    let recibido: CursoDTO | undefined;
    service.abrir('educhronos.db').subscribe((r) => (recibido = r));

    const req = http.expectOne('/api/cursos/abrir');
    expect(req.request.method).toBe('POST');
    // toEqual sobre el objeto entero: el contrato es un solo campo y no se le añaden más.
    expect(req.request.body).toEqual({ fichero: 'educhronos.db' });
    req.flush(resultado);
    expect(recibido).toEqual(resultado);
  });

  it('(4) duplicar → POST /api/cursos con el cuerpo tal cual se le da', () => {
    service.duplicar({ nombreNuevo: '2026/2027', nombreActual: '2025/2026' }).subscribe();

    const req = http.expectOne('/api/cursos');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      nombreNuevo: '2026/2027',
      nombreActual: '2025/2026',
    });
    req.flush({ nombre: '2026/2027', fichero: 'curso-2026-2027.db' });
  });

  it('(5) duplicar sin nombreActual NO inventa el campo', () => {
    // Importa: el backend rechaza con 400 un nombreActual que no coincida con el suyo, así
    // que mandar un '' donde no se pide sería provocar el rechazo desde el cliente.
    service.duplicar({ nombreNuevo: '2026/2027' }).subscribe();

    const req = http.expectOne('/api/cursos');
    expect(req.request.body).toEqual({ nombreNuevo: '2026/2027' });
    expect('nombreActual' in (req.request.body as object)).toBe(false);
    req.flush({ nombre: '2026/2027', fichero: 'curso-2026-2027.db' });
  });
});
