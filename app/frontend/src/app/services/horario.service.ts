import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { HorarioProyeccion } from '../models/horario.model';

/**
 * Cliente de solo lectura contra la capa REST de Fase 7 (Bloque 7A). En dev, el
 * `/api` se enruta a `http://localhost:8080` vía proxy.conf.json; en el jar, la
 * UI se sirve desde el mismo origen (static/) y la ruta relativa resuelve sola.
 */
@Injectable({ providedIn: 'root' })
export class HorarioService {
  private readonly http = inject(HttpClient);

  /** GET /api/horarios/{id}/proyeccion → proyección plana del horario. */
  getProyeccion(id: number): Observable<HorarioProyeccion> {
    return this.http.get<HorarioProyeccion>(`/api/horarios/${id}/proyeccion`);
  }
}
