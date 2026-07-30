import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Asignatura, AsignaturaRequest } from '../models/asignatura.model';

/**
 * Cliente REST del catálogo de asignaturas.
 *
 * CADA CONTRATO SU CLIENTE (precedente `BloqueoService`): wrappers pelados sin
 * `.pipe`/`catchError`. El servicio PROPAGA el error de Http; el componente lo
 * traduce en su `subscribe({ next, error })`. Así la política de mensaje vive en
 * el componente, que conoce el idioma del usuario, y no se reparte por capas.
 */
@Injectable({ providedIn: 'root' })
export class AsignaturaService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/asignaturas';

  /** GET /api/asignaturas → lista completa del catálogo. */
  listar(): Observable<Asignatura[]> {
    return this.http.get<Asignatura[]>(this.base);
  }

  /** GET /api/asignaturas/{id} → una asignatura. 404 si no existe. */
  obtener(id: number): Observable<Asignatura> {
    return this.http.get<Asignatura>(`${this.base}/${id}`);
  }

  /** POST /api/asignaturas → crea (201). 400 si el código ya existe o hay campo vacío. */
  crear(req: AsignaturaRequest): Observable<Asignatura> {
    return this.http.post<Asignatura>(this.base, req);
  }

  /** PUT /api/asignaturas/{id} → edita. 400 si el código choca con otro; 404 si no existe. */
  editar(id: number, req: AsignaturaRequest): Observable<Asignatura> {
    return this.http.put<Asignatura>(`${this.base}/${id}`, req);
  }

  /** DELETE /api/asignaturas/{id} → borra (204). 409 si tiene referencias entrantes. */
  borrar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
