import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Espejo de `ConfiguracionGuardiasDTO(Integer minimoPorTramo)` (S212, C-dato-guardias). */
export interface ConfiguracionGuardias {
  minimoPorTramo: number;
}

/**
 * Cliente REST del mínimo de profesores de guardia por tramo del centro (S212).
 *
 * CADA CONTRATO SU CLIENTE (precedente `JornadaService`): wrappers pelados sin
 * `.pipe`/`catchError`; el componente traduce el error. Dos operaciones porque el recurso es
 * un SINGLETON sin id: nunca 404, y sin valor guardado el GET responde el de por defecto.
 */
@Injectable({ providedIn: 'root' })
export class ConfiguracionGuardiasService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/configuracion-guardias';

  /** GET /api/configuracion-guardias → el mínimo vigente. */
  obtener(): Observable<ConfiguracionGuardias> {
    return this.http.get<ConfiguracionGuardias>(this.base);
  }

  /** PUT /api/configuracion-guardias con `{ minimoPorTramo }`. 400 si falta o es negativo. */
  guardar(minimoPorTramo: number): Observable<ConfiguracionGuardias> {
    return this.http.put<ConfiguracionGuardias>(this.base, { minimoPorTramo });
  }
}
