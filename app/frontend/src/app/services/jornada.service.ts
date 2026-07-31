import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { JornadaDTO, JornadaRequest } from '../models/jornada.model';

/**
 * Cliente REST de la jornada del centro.
 *
 * CADA CONTRATO SU CLIENTE (precedente `AulaService`/`ProfesorService`): wrappers
 * pelados sin `.pipe`/`catchError`. El servicio PROPAGA el error de Http; el
 * componente lo traduce en su `subscribe({ next, error })`. Aquí eso importa más que
 * en los CRUD de catálogo, porque el 400 y el 409 del `PUT` no son el mismo suceso y
 * el componente los lleva por caminos distintos.
 *
 * Solo DOS operaciones: el recurso es un SINGLETON. No hay `crear`, ni `borrar`, ni
 * `obtener(id)` —la jornada es una y no tiene id—.
 */
@Injectable({ providedIn: 'root' })
export class JornadaService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/jornada';

  /**
   * GET /api/jornada → la SEMANA completa (35 tramos). Nunca 404: con la tabla vacía
   * devuelve la malla de referencia sintetizada con `persistida=false`.
   */
  obtener(): Observable<JornadaDTO> {
    return this.http.get<JornadaDTO>(this.base);
  }

  /**
   * PUT /api/jornada → reemplaza la malla a partir del DÍA TIPO enviado y devuelve la
   * semana expandida. 400 si el día tipo es inválido (horas mal formadas, solapes, más
   * de 6 lectivos); 409 si hay horarios, restricciones o bloqueos que dependen de la
   * jornada actual.
   */
  reemplazar(req: JornadaRequest): Observable<JornadaDTO> {
    return this.http.put<JornadaDTO>(this.base, req);
  }
}
