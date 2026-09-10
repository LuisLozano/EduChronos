import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AjusteService } from './ajuste.service';
import {
  IntercambiarInstanciasRequest,
  IntercambioRealizado,
  MoverInstanciaRequest,
} from '../models/ajuste.model';

/**
 * CONTRATO DE ENDPOINTS, no cobertura de lógica, exactamente como
 * `bloqueo.service.spec.ts`: los dos métodos son wrappers pelados
 * —`return this.http.put<T>(url, body)` sin `.pipe`—, así que aquí se congelan DOS
 * RUTAS con su verbo y su cuerpo, no dos comportamientos.
 *
 * <p>`HORARIO = 7` y no 1: con 1, una implementación que incrustara el id a mano
 * seguiría verde y la interpolación quedaría sin medir. Mismo criterio que el
 * `ID = 7` de `bloqueo.service.spec.ts`. Cada URL esperada se escribe LITERAL y no
 * se compone con el template del fuente: componerla volvería circular el aserto.
 *
 * <p>Las dos rutas comparten prefijo y una es PREFIJO DE LA OTRA
 * (`…/instancias` y `…/instancias/intercambio`), lo que las hace confundibles: por
 * eso (52) asevera la URL COMPLETA con el sufijo, y no un `toContain`. Una mutación
 * que mandara el intercambio a `…/instancias` cae ahí.
 *
 * <p>`verify()` es RED, no aserto, con la misma cascada bajo mutación que documenta
 * `bloqueo.service.spec.ts`: la VÍCTIMA REAL es la que falla por `expectOne`; las
 * COLATERALES fallan por "Cannot configure the test module".
 */

/** Id del horario en la ruta. Ver arriba: 7 y no 1. */
const HORARIO = 7;

/**
 * Cuerpo del movimiento, calibrado en tres dimensiones para que (51) no pase por
 * acumulación: `indice` es 2 y no 1, y `dia` (3) y `orden` (4) son DISTINTOS entre
 * sí —con `dia === orden` una permutación de los dos daría un cuerpo idéntico—.
 */
const MOVIMIENTO: MoverInstanciaRequest = {
  actividadCodigo: 'Bloque-CyR_OyD_RefMt-1ESO',
  indice: 2,
  dia: 3,
  orden: 4,
};

/**
 * Cuerpo del intercambio. Los dos lados DIFIEREN en actividad Y en índice: con
 * `primera` y `segunda` iguales, una mutación que mandara la misma referencia dos
 * veces —o que las permutara— quedaría verde.
 */
const INTERCAMBIO: IntercambiarInstanciasRequest = {
  primera: { actividadCodigo: 'Mat-1ºA', indice: 2 },
  segunda: { actividadCodigo: 'LCL-1ºA', indice: 1 },
};

describe('cliente de ajuste de instancias', () => {
  let servicio: AjusteService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    servicio = TestBed.inject(AjusteService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('(51) mover pide PUT a la colección de instancias del horario, con el cuerpo del movimiento', () => {
    servicio.mover(HORARIO, MOVIMIENTO).subscribe();

    const req = http.expectOne({ method: 'PUT', url: '/api/horarios/7/instancias' });
    expect(req.request.body).toEqual(MOVIMIENTO);

    req.flush([]);
  });

  it('(52) intercambiar pide PUT a la sub-ruta /intercambio, con las DOS referencias', () => {
    servicio.intercambiar(HORARIO, INTERCAMBIO).subscribe();

    const req = http.expectOne({ method: 'PUT', url: '/api/horarios/7/instancias/intercambio' });
    expect(req.request.body).toEqual(INTERCAMBIO);

    req.flush({ primera: [], segunda: [] });
  });

  /**
   * Las dos listas del 200 llegan SEPARADAS al suscriptor. No es cobertura del
   * wrapper —que no transforma nada—: es el aserto que ata el TIPO al contrato del
   * servidor, y el único de este fichero que sí mira el valor emitido.
   *
   * <p>Las dos listas tienen LONGITUDES distintas (1 y 2) y contenidos distintos a
   * propósito: una implementación que las concatenara, que devolviera dos veces la
   * misma o que las permutara cae aquí. Con listas iguales, las tres mutaciones
   * quedarían verdes.
   */
  it('(53) el 200 del intercambio llega con sus dos listas separadas, no concatenadas', () => {
    const cuerpo = {
      primera: [{ sesionId: 1 }],
      segunda: [{ sesionId: 2 }, { sesionId: 3 }],
    } as unknown as IntercambioRealizado;
    let recibido: IntercambioRealizado | null = null;

    servicio.intercambiar(HORARIO, INTERCAMBIO).subscribe((r) => (recibido = r));

    http.expectOne({ method: 'PUT', url: '/api/horarios/7/instancias/intercambio' }).flush(cuerpo);

    const r = recibido as unknown as IntercambioRealizado;
    expect(r).not.toBeNull();
    expect(r.primera.map((s) => s.sesionId)).toEqual([1]);
    expect(r.segunda.map((s) => s.sesionId)).toEqual([2, 3]);
  });
});
