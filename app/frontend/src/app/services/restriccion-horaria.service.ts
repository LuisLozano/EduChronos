import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { RestriccionHoraria, RestriccionHorariaRequest } from '../models/restriccion-horaria.model';

/**
 * Cliente del sub-recurso RESTRICCIONES HORARIAS de un profesor,
 * `/api/profesores/{idProfesor}/restricciones-horarias` (§4.3, Bloque 8.5-E). Molde de
 * `TutoriaService`: sin campo `base` —la ruta depende del id— y wrappers pelados sin
 * `.pipe`/`catchError`; el componente traduce el error en su `subscribe`.
 *
 * <p><b>DOS verbos.</b> `ProfesorController` expone `@GetMapping` y `@PutMapping` sobre
 * el sub-recurso (`:105` y `:119`) y nada más. Quitar todas las restricciones es un
 * `PUT` con `[]`.
 *
 * <p><b>El PUT es un REEMPLAZO TOTAL</b> (`RestriccionHorariaService.java:102-169`): borra
 * las filas del profesor y escribe las que llegan. Lo que no se reenvía se pierde, y eso
 * incluye el `motivo` de una fila que se reenvía sin él.
 *
 * <p><b>Un profesor sin restricciones responde 200 con LISTA VACÍA</b>; el 404 es «no
 * existe profesor con ese id» (`RestriccionHorariaService.java:87-100`).
 */
@Injectable({ providedIn: 'root' })
export class RestriccionHorariaService {
  private readonly http = inject(HttpClient);

  /** GET → las restricciones del profesor, ordenadas por (dia, ordenEnDia) en el backend. */
  obtener(idProfesor: number): Observable<RestriccionHoraria[]> {
    return this.http.get<RestriccionHoraria[]>(
      `/api/profesores/${idProfesor}/restricciones-horarias`);
  }

  /** PUT → reemplaza TODAS las restricciones del profesor y devuelve las resultantes. */
  reemplazar(
    idProfesor: number,
    restricciones: RestriccionHorariaRequest[],
  ): Observable<RestriccionHoraria[]> {
    return this.http.put<RestriccionHoraria[]>(
      `/api/profesores/${idProfesor}/restricciones-horarias`, restricciones);
  }
}
