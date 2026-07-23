import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { AvisoPrevalidacion } from '../models/prevalidacion.model';

/**
 * Cliente de la capa REST de pre-validación (Fase 8, Bloque 8.4-B1). Mismo
 * enrutado que el resto de servicios: `/api` va al backend por proxy en dev y al
 * mismo origen en el jar.
 *
 * Vive aparte —CADA CONTRATO SU CLIENTE, precedente `BloqueoService`/
 * `DiagnosticoService`—, no por ser de solo lectura.
 */
@Injectable({ providedIn: 'root' })
export class PrevalidacionService {
  private readonly http = inject(HttpClient);

  /**
   * GET /api/prevalidacion → todos los hallazgos del catálogo actual (ERROR y
   * AVISO), o lista vacía si está sano. SIN id: la pre-validación es del catálogo
   * GLOBAL, no de un horario —contraste con `DiagnosticoService.getDiagnostico`,
   * que sí es por horario—. Wrapper pelado, misma forma que `diagnostico.service.ts`.
   */
  getPrevalidacion(): Observable<AvisoPrevalidacion[]> {
    return this.http.get<AvisoPrevalidacion[]>('/api/prevalidacion');
  }
}
