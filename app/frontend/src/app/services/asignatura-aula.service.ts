import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AsignaturaAula } from '../models/asignatura-aula.model';

/**
 * Cliente del sub-recurso de las AULAS de una asignatura, `/api/asignaturas/{id}/aulas` (S206,
 * C-reglas-aulas). Molde de `TutoriaService`: un GET que lista y un PUT de reemplazo total, sin
 * lógica propia. El orden de validación y la regla de no mezclar marcas viven en el backend.
 */
@Injectable({ providedIn: 'root' })
export class AsignaturaAulaService {
  private readonly http = inject(HttpClient);

  /**
   * GET /api/asignaturas/{idAsignatura}/aulas → las aulas de la asignatura, ordenadas por código
   * de aula. Lista VACÍA —200, no 404— si no tiene ninguna; 404 si la asignatura no existe.
   */
  listar(idAsignatura: number): Observable<AsignaturaAula[]> {
    return this.http.get<AsignaturaAula[]>(`/api/asignaturas/${idAsignatura}/aulas`);
  }

  /**
   * PUT /api/asignaturas/{idAsignatura}/aulas → REEMPLAZA las aulas de la asignatura y devuelve la
   * lista resultante. El cuerpo es el ARRAY DESNUDO; `[]` las borra todas. 400 si un aula no
   * existe, se repite, un rol no es válido o se mezclan las dos marcas; 404 si la asignatura no
   * existe.
   */
  reemplazar(idAsignatura: number, aulas: AsignaturaAula[]): Observable<AsignaturaAula[]> {
    return this.http.put<AsignaturaAula[]>(`/api/asignaturas/${idAsignatura}/aulas`, aulas);
  }
}
