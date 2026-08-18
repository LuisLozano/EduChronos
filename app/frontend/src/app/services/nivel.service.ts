import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Nivel, NivelRequest } from '../models/nivel.model';

/**
 * Cliente REST del catálogo de niveles.
 *
 * CADA CONTRATO SU CLIENTE (precedente `BloqueoService`, molde de catálogo S101-S103):
 * wrappers pelados sin `.pipe`/`catchError`. El servicio PROPAGA el error de Http; el
 * componente lo traduce en su `subscribe({ next, error })`. Así la política de mensaje
 * vive en el componente, que conoce el idioma del usuario, y no se reparte por capas.
 *
 * <p>Los cinco wrappers desde S111 (C-niveles). Hasta S110 solo existía `listar()`,
 * porque ninguna pantalla escribía niveles y los otros cuatro habrían sido código
 * muerto; con la sección de niveles en Configuración, los cinco tienen consumidor.
 */
@Injectable({ providedIn: 'root' })
export class NivelService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/niveles';

  /** GET /api/niveles → lista completa, ya ordenada por `orden` desde el backend. */
  listar(): Observable<Nivel[]> {
    return this.http.get<Nivel[]>(this.base);
  }

  /** GET /api/niveles/{id} → un nivel. 404 si no existe. */
  obtener(id: number): Observable<Nivel> {
    return this.http.get<Nivel>(`${this.base}/${id}`);
  }

  /** POST /api/niveles → crea (201). 400 si el código ya existe o está en blanco. */
  crear(req: NivelRequest): Observable<Nivel> {
    return this.http.post<Nivel>(this.base, req);
  }

  /** PUT /api/niveles/{id} → edita. 400 si el código choca con otro; 404 si no existe. */
  editar(id: number, req: NivelRequest): Observable<Nivel> {
    return this.http.put<Nivel>(`${this.base}/${id}`, req);
  }

  /** DELETE /api/niveles/{id} → borra (204). 409 si hay grupos apuntando al nivel. */
  borrar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
