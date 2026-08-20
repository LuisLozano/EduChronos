import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Tutoria, TutoriaRequest } from '../models/tutoria.model';

/**
 * Cliente del sub-recurso TUTORÍA, `/api/grupos/{idGrupo}/tutoria` (§4.1, invariante
 * I4, Bloque 8.5-D2a): qué profesores tutorizan un grupo y con qué rol. Molde de
 * `PdcService`, el otro sub-recurso colgado de `/api/grupos/{id}`, y no el de
 * `GrupoService`, que es CRUD plano: sin campo `base`, la ruta se compone en cada
 * método porque depende del id.
 *
 * CADA CONTRATO SU CLIENTE (precedente `BloqueoService`): wrappers pelados sin
 * `.pipe`/`catchError`. El servicio PROPAGA el error de Http; el componente lo
 * traduce en su `subscribe({ next, error })`. Así la política de mensaje vive en el
 * componente, que conoce el idioma del usuario, y no se reparte por capas.
 *
 * <p><b>DOS verbos, y no es una carencia de este cliente.</b> `GrupoController` expone
 * `@GetMapping("/{id}/tutoria")` y `@PutMapping("/{id}/tutoria")` (`:103` y `:112`) y
 * nada más: NO hay DELETE ni PATCH en el sub-recurso. Quitar la tutoría de un grupo es
 * un `PUT` con `[]`, y el backend lo tiene congelado en
 * `putConListaVacia_borraLaTutoria`. Quien busque aquí un `borrar()` no lo va a
 * encontrar porque no existe al otro lado.
 *
 * <p><b>El PUT es un REEMPLAZO TOTAL, no una edición fila a fila</b>
 * (`TutoriaService.java:85-88`): borra las filas actuales del grupo y escribe las que
 * llegan. Quien llame debe reenviar TODAS las tutorías que quiera conservar; la que
 * omita, la borra. Es idempotente: el mismo cuerpo dos veces deja el mismo estado.
 *
 * <p><b>Un grupo sin tutoría responde 200 con LISTA VACÍA, nunca 404</b>
 * (`TutoriaService.java:75,81`, literal: «Lista vacía = grupo sin tutoría (no es un
 * 404)»). Contraste deliberado con `PdcService.obtener`, cuyo consumidor SÍ discrimina
 * por `err.status === 404` para derivar el estado «no tiene PDC»: ese patrón NO se
 * traslada aquí. «No hay tutor» es `length === 0`; un 404 de este GET significa otra
 * cosa —el grupo no existe— y pintarlo como «sin tutor» taparía el fallo real.
 *
 * <p><b>En ESCRITURA, un código de profesor inexistente da 404, no 400</b>
 * (`TutoriaService.java:147-152`, decisión explícita de bloque frente a
 * `GrupoService.resolverNivel`, que para el nivel devuelve 400). Es contraintuitivo en
 * un `PUT` sobre un grupo que sí existe, y el manejador de error del componente tiene
 * que contemplarlo: un 404 del `reemplazar` puede ser «el grupo no existe» o «ese
 * código de profesor no existe», y el mensaje del backend es lo único que los separa.
 */
@Injectable({ providedIn: 'root' })
export class TutoriaService {
  private readonly http = inject(HttpClient);

  /**
   * GET /api/grupos/{idGrupo}/tutoria → las tutorías del grupo, ORDENADAS por rol (el
   * principal primero, orden natural del enum) y, dentro del rol, por código de
   * profesor (`TutoriaService.java:179-187`).
   *
   * <p>Lista VACÍA —200, no 404— si el grupo no tiene ninguna. El 404 queda reservado
   * a «no existe grupo con ese id»; ver el javadoc de clase.
   */
  obtener(idGrupo: number): Observable<Tutoria[]> {
    return this.http.get<Tutoria[]>(`/api/grupos/${idGrupo}/tutoria`);
  }

  /**
   * PUT /api/grupos/{idGrupo}/tutoria → REEMPLAZA la tutoría entera del grupo y
   * devuelve la lista resultante, ya ordenada. `[]` la borra.
   *
   * <p>El cuerpo es el ARRAY DESNUDO, no un objeto que lo envuelva: el controlador
   * recibe `@RequestBody List<TutoriaRequest>` (`GrupoController.java:114`).
   *
   * <p>404 si el grupo no existe o si el código de algún profesor no existe; 400 si el
   * `rol` no parsea, si un profesor viene repetido en la lista, o si llegan DOS
   * `TUTOR_PRINCIPAL` —la mitad de I4 que la escritura hace cumplir
   * (`TutoriaService.java:123-134`)—. Cuando concurren un profesor inexistente y un rol
   * inválido gana el 404: el backend resuelve los profesores en una pasada completa
   * antes de parsear ningún rol.
   */
  reemplazar(idGrupo: number, tutorias: TutoriaRequest[]): Observable<Tutoria[]> {
    return this.http.put<Tutoria[]>(`/api/grupos/${idGrupo}/tutoria`, tutorias);
  }
}
