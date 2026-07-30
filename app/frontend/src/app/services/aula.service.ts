import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Aula, AulaRequest } from '../models/aula.model';

/**
 * Cliente REST del catálogo de aulas.
 *
 * CADA CONTRATO SU CLIENTE (precedente `BloqueoService`/`ProfesorService`): wrappers
 * pelados sin `.pipe`/`catchError`. El servicio PROPAGA el error de Http; el
 * componente lo traduce en su `subscribe({ next, error })`. Así la política de
 * mensaje vive en el componente, que conoce el idioma del usuario, y no se reparte
 * por capas.
 */
@Injectable({ providedIn: 'root' })
export class AulaService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/aulas';

  /** GET /api/aulas → lista completa del catálogo, ya ORDENADA por código en el servidor. */
  listar(): Observable<Aula[]> {
    return this.http.get<Aula[]>(this.base);
  }

  /** GET /api/aulas/{id} → un aula. 404 si no existe. */
  obtener(id: number): Observable<Aula> {
    return this.http.get<Aula>(`${this.base}/${id}`);
  }

  /**
   * POST /api/aulas → crea (201). 400 si el código ya existe, si falta un
   * obligatorio (`codigo`, `tipo`) o si el tipo no parsea a `TipoAula`.
   */
  crear(req: AulaRequest): Observable<Aula> {
    return this.http.post<Aula>(this.base, req);
  }

  /**
   * PUT /api/aulas/{id} → edita. 400 si el código choca con OTRA aula o el tipo no
   * parsea; 404 si el id no existe.
   */
  editar(id: number, req: AulaRequest): Observable<Aula> {
    return this.http.put<Aula>(`${this.base}/${id}`, req);
  }

  /**
   * DELETE /api/aulas/{id} → borra (204). 409 si tiene referencias entrantes
   * (plazas fijas, plazas candidatas, bloqueos o sesiones que la usan).
   */
  borrar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
