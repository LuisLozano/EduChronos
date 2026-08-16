import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Actividad, ActividadRequest } from '../models/actividad.model';

/**
 * Cliente REST del CRUD de actividades (agregado con sus plazas embebidas).
 *
 * CADA CONTRATO SU CLIENTE (precedente `BloqueoService`, molde `SubgrupoService`):
 * wrappers pelados sin `.pipe`/`catchError`. El servicio PROPAGA el error de Http; el
 * componente lo traduce en su `subscribe({ next, error })`. Así la política de mensaje
 * vive en el componente, que conoce el idioma del usuario, y no se reparte por capas.
 *
 * <p>Esta es la razón de que aquí no haya lógica: la actividad tiene TRES códigos de
 * error distintos (400 de validación, 404 de id, 409 de dependientes) y quién los
 * distingue es el componente, no el cliente.
 */
@Injectable({ providedIn: 'root' })
export class ActividadService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/actividades';

  /** GET /api/actividades → lista completa, ordenada por código. */
  listar(): Observable<Actividad[]> {
    return this.http.get<Actividad[]>(this.base);
  }

  /** GET /api/actividades/{id} → una actividad con sus plazas. 404 si no existe. */
  obtener(id: number): Observable<Actividad> {
    return this.http.get<Actividad>(`${this.base}/${id}`);
  }

  /** POST /api/actividades → crea (201). 400 si falla cualquier validación (escalares,
   *  patrón, XOR de aula, I7, I2, código no resoluble) o el código ya existe. */
  crear(req: ActividadRequest): Observable<Actividad> {
    return this.http.post<Actividad>(this.base, req);
  }

  /** PUT /api/actividades/{id} → edita. 404 si el id no existe; 409 si la actividad
   *  tiene horario o bloqueos colgando (la estructura no se edita en caliente); 400 si
   *  falla la validación. El 409 GANA al 400: el backend lo comprueba primero. */
  editar(id: number, req: ActividadRequest): Observable<Actividad> {
    return this.http.put<Actividad>(`${this.base}/${id}`, req);
  }

  /** DELETE /api/actividades/{id} → borra (204). 409 con desglose si algo la retiene. */
  borrar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
