import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Grupo, PdcRequest } from '../models/grupo.model';

/**
 * Cliente del sub-recurso PDC, `/api/grupos/{idPadre}/pdc` (Bloque 8.5-D1): el grupo
 * de Diversificación colgado de su grupo ordinario padre. Molde de
 * `DiagnosticoService`, que es el precedente del proyecto para una ruta
 * `/api/{entidad}/{id}/{algo}`, y no el de `GrupoService`, que es CRUD plano.
 *
 * CADA CONTRATO SU CLIENTE (precedente `BloqueoService`): wrappers pelados sin
 * `.pipe`/`catchError`. El servicio PROPAGA el error de Http; el componente lo
 * traduce en su `subscribe({ next, error })`.
 *
 * <p><b>TRES verbos, no cuatro: no hay EDICIÓN.</b> `PdcController` no expone `PUT`
 * ni `PATCH` —el ciclo del sub-recurso es alta / consulta / borrado—, así que este
 * cliente tampoco tiene un `editar`. Renombrar un PDC se hace BORRÁNDOLO y
 * volviéndolo a crear con el código nuevo, con la consecuencia que eso arrastra: el
 * borrado se lleva por delante el subgrupo mono-Di, y devuelve 409 si alguna plaza
 * lo está usando. No es una carencia que este cliente pueda tapar.
 *
 * <p><b>El tipo de respuesta es {@link Grupo}</b> porque el backend devuelve un
 * `GrupoDTO` en los tres verbos: mismo record, mismos cuatro campos.
 */
@Injectable({ providedIn: 'root' })
export class PdcService {
  private readonly http = inject(HttpClient);

  /**
   * GET /api/grupos/{idPadre}/pdc → el PDC de ese padre.
   *
   * <p>`idPadre` es el id del grupo ORDINARIO PADRE, nunca el del PDC. Pasar el del
   * PDC no da error de tipos —los dos son `number`— y devuelve 404, que aquí se lee
   * como "este padre no tiene PDC": el fallo se disfraza del caso normal.
   *
   * <p><b>El 404 se PROPAGA sin traducir, y es deliberado.</b> Significa "este padre
   * no tiene PDC", que es un ESTADO legítimo y no un error; aun así no se convierte
   * aquí en `null` ni en un `Observable` vacío, porque hacerlo sacaría de su sitio la
   * lógica que el componente va a probar: es él quien decide si eso se pinta como
   * "sin PDC" o como un fallo, y quién distingue ese 404 del 404 de "el padre no
   * existe". Un `catchError` en esta capa dejaría esa decisión sin dueño y sin test.
   */
  obtener(idPadre: number): Observable<Grupo> {
    return this.http.get<Grupo>(`/api/grupos/${idPadre}/pdc`);
  }

  /**
   * POST /api/grupos/{idPadre}/pdc → crea el PDC y su subgrupo mono-Di (201).
   *
   * <p>`idPadre` es el id del grupo ORDINARIO PADRE, nunca el del PDC (que aún no
   * existe). 404 si ese padre no existe; 400 si el padre no es ORDINARIO, si el
   * código falta o choca, o si el padre ya tiene un PDC.
   */
  crear(idPadre: number, peticion: PdcRequest): Observable<Grupo> {
    return this.http.post<Grupo>(`/api/grupos/${idPadre}/pdc`, peticion);
  }

  /**
   * DELETE /api/grupos/{idPadre}/pdc → borra el PDC y su subgrupo mono-Di (204).
   *
   * <p>`idPadre` es el id del grupo ORDINARIO PADRE, nunca el del PDC: quien tenga a
   * mano la fila del PDC y pase SU id recibirá un 404 ("no tiene PDC") sobre un padre
   * que no era el que quería tocar. 409 si el subgrupo mono-Di está en alguna plaza,
   * en cuyo caso no se borra nada.
   */
  borrar(idPadre: number): Observable<void> {
    return this.http.delete<void>(`/api/grupos/${idPadre}/pdc`);
  }
}
