import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Grupo, GrupoRequest } from '../models/grupo.model';

/**
 * Cliente REST del catálogo de grupos administrativos.
 *
 * CADA CONTRATO SU CLIENTE (precedente `BloqueoService`): wrappers pelados sin
 * `.pipe`/`catchError`. El servicio PROPAGA el error de Http; el componente lo
 * traduce en su `subscribe({ next, error })`. Así la política de mensaje vive en
 * el componente, que conoce el idioma del usuario, y no se reparte por capas.
 */
@Injectable({ providedIn: 'root' })
export class GrupoService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/grupos';

  /** GET /api/grupos → lista completa del catálogo. */
  listar(): Observable<Grupo[]> {
    return this.http.get<Grupo[]>(this.base);
  }

  /** GET /api/grupos/{id} → un grupo. 404 si no existe. */
  obtener(id: number): Observable<Grupo> {
    return this.http.get<Grupo>(`${this.base}/${id}`);
  }

  /** POST /api/grupos → crea (201). 400 si el código ya existe o el nivel no existe. */
  crear(req: GrupoRequest): Observable<Grupo> {
    return this.http.post<Grupo>(this.base, req);
  }

  /** PUT /api/grupos/{id} → edita. 400 si el código choca con otro; 404 si no existe. */
  editar(id: number, req: GrupoRequest): Observable<Grupo> {
    return this.http.put<Grupo>(`${this.base}/${id}`, req);
  }

  /** DELETE /api/grupos/{id} → borra (204). 409 si tiene referencias entrantes. */
  borrar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
