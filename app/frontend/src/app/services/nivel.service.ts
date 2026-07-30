import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Nivel } from '../models/nivel.model';

/**
 * Cliente REST del catálogo de niveles, RESTRINGIDO a la lectura de la lista.
 *
 * CADA CONTRATO SU CLIENTE (precedente `BloqueoService`, molde de catálogo S101-S103):
 * wrapper pelado sin `.pipe`/`catchError`. El servicio PROPAGA el error de Http; el
 * componente lo traduce en su `subscribe({ next, error })`. Así la política de mensaje
 * vive en el componente, que conoce el idioma del usuario, y no se reparte por capas.
 *
 * <p>SOLO `listar()`, a propósito. `/api/niveles` expone los cinco endpoints del CRUD
 * en backend, pero nivel no es una de las cuatro entidades de O-catálogo y ninguna
 * pantalla crea, edita ni borra niveles: su único consumidor es el desplegable de
 * `GrupoForm`. Añadir los otros cuatro wrappers sería código muerto que miente sobre
 * el alcance real de la UI.
 */
@Injectable({ providedIn: 'root' })
export class NivelService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/niveles';

  /** GET /api/niveles → lista completa, ya ordenada por `orden` desde el backend. */
  listar(): Observable<Nivel[]> {
    return this.http.get<Nivel[]>(this.base);
  }
}
