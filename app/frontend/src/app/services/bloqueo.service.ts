import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Bloqueo, BloqueoRequest } from '../models/bloqueo.model';

/**
 * Cliente de la capa REST de bloqueos manuales (§4.7, Bloque 8.2b-iv). Mismo
 * enrutado que `HorarioService`: `/api` va al backend por proxy en dev y al
 * mismo origen en el jar.
 *
 * Las reglas de validación (D-3) viven ENTERAS en el backend; aquí no se
 * replican: un alta inválida vuelve como 400 con el mensaje del servidor.
 */
@Injectable({ providedIn: 'root' })
export class BloqueoService {
  private readonly http = inject(HttpClient);

  /** GET /api/bloqueos → todos los bloqueos persistidos. */
  listar(): Observable<Bloqueo[]> {
    return this.http.get<Bloqueo[]>('/api/bloqueos');
  }

  /**
   * POST /api/bloqueos → da de alta o reemplaza un bloqueo. IDEMPOTENTE POR
   * INSTANCIA (D-4): reemplaza el pin identificado por (`actividadCodigo`,
   * `indice`), NO la colección de bloqueos; los demás pines quedan intactos.
   */
  guardar(peticion: BloqueoRequest): Observable<Bloqueo> {
    return this.http.post<Bloqueo>('/api/bloqueos', peticion);
  }

  /** DELETE /api/bloqueos/{id} → 204; id inexistente → 404. */
  borrar(id: number): Observable<void> {
    return this.http.delete<void>(`/api/bloqueos/${id}`);
  }
}
