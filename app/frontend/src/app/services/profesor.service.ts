import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Profesor, ProfesorRequest } from '../models/profesor.model';

/**
 * Cliente REST del catálogo de profesores.
 *
 * CADA CONTRATO SU CLIENTE (precedente `BloqueoService`): wrappers pelados sin
 * `.pipe`/`catchError`. El servicio PROPAGA el error de Http; el componente lo
 * traduce en su `subscribe({ next, error })`. Así la política de mensaje vive en
 * el componente, que conoce el idioma del usuario, y no se reparte por capas.
 */
@Injectable({ providedIn: 'root' })
export class ProfesorService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/profesores';

  /** GET /api/profesores → lista completa del catálogo. */
  listar(): Observable<Profesor[]> {
    return this.http.get<Profesor[]>(this.base);
  }

  /** GET /api/profesores/{id} → un profesor. 404 si no existe. */
  obtener(id: number): Observable<Profesor> {
    return this.http.get<Profesor>(`${this.base}/${id}`);
  }

  /** POST /api/profesores → crea (201). 400 si el código ya existe o hay campo vacío. */
  crear(req: ProfesorRequest): Observable<Profesor> {
    return this.http.post<Profesor>(this.base, req);
  }

  /** PUT /api/profesores/{id} → edita. 400 si el código choca con otro; 404 si no existe. */
  editar(id: number, req: ProfesorRequest): Observable<Profesor> {
    return this.http.put<Profesor>(`${this.base}/${id}`, req);
  }

  /** DELETE /api/profesores/{id} → borra (204). 409 si tiene referencias entrantes. */
  borrar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
