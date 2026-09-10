import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  IntercambiarInstanciasRequest,
  IntercambioRealizado,
  MoverInstanciaRequest,
} from '../models/ajuste.model';
import { SesionVista } from '../models/horario.model';

/**
 * Cliente de la capa REST del AJUSTE MANUAL de un horario ya generado (S143
 * mover, S144 intercambiar). Mismo enrutado que el resto: `/api` va al backend por
 * proxy en dev y al mismo origen en el jar.
 *
 * <p>Wrappers PELADOS, sin `.pipe` y sin `catchError`, igual que los quince
 * servicios que ya existen: el servicio PROPAGA el `HttpErrorResponse` y el
 * componente lo traduce. Aquí importa más que en ningún otro, porque el cuerpo del
 * rechazo (`FalloMovimiento`) lleva la CAUSA y las VIOLACIONES, y un `catchError`
 * en esta capa dejaría esa decisión sin dueño y sin test.
 *
 * <p>El VEREDICTO es del servidor y solo del servidor. Este cliente no comprueba
 * pines, ni tramos, ni reglas duras: manda el gesto y enseña lo que vuelve.
 */
@Injectable({ providedIn: 'root' })
export class AjusteService {
  private readonly http = inject(HttpClient);

  /**
   * PUT /api/horarios/{horarioId}/instancias → recoloca una instancia completa en
   * otro tramo. El 200 devuelve las filas resultantes de ESA instancia, con la
   * misma forma que las `sesiones` de la proyección. Idempotente: mover una
   * instancia al tramo que ya ocupa también da 200.
   */
  mover(horarioId: number, peticion: MoverInstanciaRequest): Observable<SesionVista[]> {
    return this.http.put<SesionVista[]>(`/api/horarios/${horarioId}/instancias`, peticion);
  }

  /**
   * PUT /api/horarios/{horarioId}/instancias/intercambio → permuta los tramos de
   * dos instancias. El 200 devuelve DOS listas, una por lado, y el genérico las
   * tipa literalmente: no se concatenan (ver {@link IntercambioRealizado}).
   */
  intercambiar(
    horarioId: number,
    peticion: IntercambiarInstanciasRequest,
  ): Observable<IntercambioRealizado> {
    return this.http.put<IntercambioRealizado>(
      `/api/horarios/${horarioId}/instancias/intercambio`,
      peticion,
    );
  }
}
