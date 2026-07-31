import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Subgrupo, SubgrupoRequest } from '../models/subgrupo.model';

/**
 * Cliente REST del catálogo de subgrupos de alumnos.
 *
 * CADA CONTRATO SU CLIENTE (precedente `BloqueoService`): wrappers pelados sin
 * `.pipe`/`catchError`. El servicio PROPAGA el error de Http; el componente lo
 * traduce en su `subscribe({ next, error })`. Así la política de mensaje vive en
 * el componente, que conoce el idioma del usuario, y no se reparte por capas.
 */
@Injectable({ providedIn: 'root' })
export class SubgrupoService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/subgrupos';

  /** GET /api/subgrupos → lista completa del catálogo. */
  listar(): Observable<Subgrupo[]> {
    return this.http.get<Subgrupo[]>(this.base);
  }

  /** GET /api/subgrupos/{id} → un subgrupo. 404 si no existe. */
  obtener(id: number): Observable<Subgrupo> {
    return this.http.get<Subgrupo>(`${this.base}/${id}`);
  }

  /** POST /api/subgrupos → crea (201). 400 si el código ya existe, la lista de
   * grupos está vacía, o algún código de grupo no existe. */
  crear(req: SubgrupoRequest): Observable<Subgrupo> {
    return this.http.post<Subgrupo>(this.base, req);
  }

  /** PUT /api/subgrupos/{id} → edita. 400 si el código choca o un grupo no existe;
   * 404 si no existe. */
  editar(id: number, req: SubgrupoRequest): Observable<Subgrupo> {
    return this.http.put<Subgrupo>(`${this.base}/${id}`, req);
  }

  /** DELETE /api/subgrupos/{id} → borra (204). 409 si está referenciado por plazas. */
  borrar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
