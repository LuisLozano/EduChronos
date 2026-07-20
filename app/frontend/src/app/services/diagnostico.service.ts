import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Diagnostico } from '../models/diagnostico.model';

/**
 * Cliente de la capa REST de diagnóstico (Fase 8, Bloque 8.3-C). Mismo enrutado
 * que `HorarioService` y `BloqueoService`: `/api` va al backend por proxy en dev
 * y al mismo origen en el jar.
 *
 * Vive aparte de `HorarioService` porque CADA CONTRATO TIENE SU CLIENTE
 * (precedente: `BloqueoService`), no por ser de solo lectura —`HorarioService`
 * también lo es y ese argumento no discriminaría.
 */
@Injectable({ providedIn: 'root' })
export class DiagnosticoService {
  private readonly http = inject(HttpClient);

  /**
   * GET /api/horarios/{id}/diagnostico → violaciones duras y penalizaciones
   * blandas atribuidas por celda, más los totales blandos.
   *
   * Id inexistente → 404, que se deja PROPAGAR sin traducir (igual que el resto
   * de servicios): "no hay horario" (404) y "horario sin violaciones" (200 con
   * listas vacías) son estados DISTINTOS, y colapsarlos aquí le quitaría al
   * consumidor la información para distinguirlos. Quién los distingue y cómo no
   * se decide en esta capa.
   */
  getDiagnostico(id: number): Observable<Diagnostico> {
    return this.http.get<Diagnostico>(`/api/horarios/${id}/diagnostico`);
  }
}
