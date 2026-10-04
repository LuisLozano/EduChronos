import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { VersionDTO } from '../models/version.model';

/**
 * Cliente REST de la versión de la aplicación (C-version-y-rastro, condición 2, S193).
 *
 * CADA CONTRATO SU CLIENTE (precedente `JornadaService`): wrapper pelado, sin `.pipe` y sin
 * `catchError`. El servicio PROPAGA el error de Http; el componente lo traduce en su
 * `subscribe({ next, error })`.
 *
 * Una sola operación: el recurso es un singleton de solo lectura.
 */
@Injectable({ providedIn: 'root' })
export class VersionService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/version';

  /** GET /api/version → la versión con la que se construyó el jar. */
  obtener(): Observable<VersionDTO> {
    return this.http.get<VersionDTO>(this.base);
  }
}
