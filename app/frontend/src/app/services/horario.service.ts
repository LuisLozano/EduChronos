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

  /**
   * POST /api/horarios (body `{ maxSegundos }`) → genera y persiste un horario nuevo
   * con el presupuesto que eligió quien genera (S184, condición 1 de O-pre-demo) y
   * devuelve su proyección completa. El resto de parámetros cae a los defaults del
   * backend. Gemelo pelado de {@link getProyeccion}: un `return this.http.post` sin
   * `.pipe`.
   */
  generar(maxSegundos: number): Observable<HorarioProyeccion> {
    return this.http.post<HorarioProyeccion>('/api/horarios', { maxSegundos });
  }

  /**
   * GET /api/horarios/vigente → el horario de id mayor del curso abierto, o null si no
   * tiene ninguno (el backend responde 204 sin cuerpo). S161, O-curso condición 4.
   */
  getVigente(): Observable<{ id: number } | null> {
    return this.http.get<{ id: number } | null>('/api/horarios/vigente');
  }
}
