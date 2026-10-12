import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * Espejo de `ConfiguracionGuardiasDTO(Integer minimoPorTramo, Integer minimoPorRecreo,
 * Integer minimoBibliotecaPorRecreo)` (S212, C-dato-guardias; S219, C-dato-guardias-especiales).
 */
export interface ConfiguracionGuardias {
  minimoPorTramo: number;
  minimoPorRecreo: number;
  minimoBibliotecaPorRecreo: number;
}

/**
 * Cliente REST de los tres mínimos de guardia del centro: profesores de guardia por tramo (S212)
 * y, por recreo, profesores de guardia y, de ellos, en la biblioteca (S219).
 *
 * CADA CONTRATO SU CLIENTE (precedente `JornadaService`): wrappers pelados sin
 * `.pipe`/`catchError`; el componente traduce el error. Dos operaciones porque el recurso es
 * un SINGLETON sin id: nunca 404, y sin valor guardado el GET responde el de por defecto.
 */
@Injectable({ providedIn: 'root' })
export class ConfiguracionGuardiasService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/configuracion-guardias';

  /** GET /api/configuracion-guardias → los tres mínimos vigentes. */
  obtener(): Observable<ConfiguracionGuardias> {
    return this.http.get<ConfiguracionGuardias>(this.base);
  }

  /** PUT /api/configuracion-guardias con los tres mínimos; 400 si falta alguno o es negativo. */
  guardar(configuracion: ConfiguracionGuardias): Observable<ConfiguracionGuardias> {
    return this.http.put<ConfiguracionGuardias>(this.base, configuracion);
  }
}
