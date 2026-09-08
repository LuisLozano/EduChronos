import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  ParteDeshacer,
  PlanReplicacion,
  ReplicacionRequest,
} from '../models/replicacion.model';

/**
 * Cliente del sub-recurso REPLICACIÓN, `/api/grupos/{idGrupo}/replicacion` (Bloques S139 y
 * S140): poblar un grupo ordinario nuevo copiando la estructura de un hermano, y deshacerlo.
 * Molde de `PdcService`, que es el precedente del proyecto para una ruta
 * `/api/{entidad}/{id}/{algo}`, y no el de `GrupoService`, que es CRUD plano.
 *
 * CADA CONTRATO SU CLIENTE (precedente `BloqueoService`): wrappers pelados sin
 * `.pipe`/`catchError`. El servicio PROPAGA el error de Http; el componente lo traduce en
 * su `subscribe({ next, error })`.
 *
 * <p><b>TRES verbos y TRES tipos de respuesta distintos</b>, al revés que `PdcService`,
 * donde los tres devuelven el mismo `GrupoDTO`. Aquí el `GET` y el `POST` comparten
 * {@link PlanReplicacion} —el `POST` devuelve el mismo plan, ya materializado— pero el
 * `DELETE` devuelve un {@link ParteDeshacer}, que es otro record: el deshacer no ejecuta un
 * plan, lee el estado y cuenta lo que ha retirado.
 *
 * <p><b>`idGrupo` es SIEMPRE el del grupo NUEVO, el que se puebla, nunca el del hermano.</b>
 * Los dos son `number` y confundirlos no da error de tipos: replicaría al revés, poblando al
 * hermano —que tiene subgrupos y por tanto daría 400— o, peor en el `DELETE`, pelaría al
 * hermano de verdad.
 *
 * <p><b>El hermano viaja por CÓDIGO y la plaza por `plazaId`</b>, y no es un descuido del
 * contrato: el código de plaza es un ordinal técnico que `PlazaRequest` documenta como
 * inestable entre ediciones, mientras que el código de grupo es el identificador de negocio
 * con el que el servidor resuelve al hermano (`findByCodigo`).
 */
@Injectable({ providedIn: 'root' })
export class ReplicacionService {
  private readonly http = inject(HttpClient);

  private ruta(idGrupo: number): string {
    return `/api/grupos/${idGrupo}/replicacion`;
  }

  /**
   * GET /api/grupos/{idGrupo}/replicacion?hermano={código} → lo que la replicación haría.
   *
   * <p>El hermano va por `params` y no interpolado en la URL: es texto del catálogo y los
   * códigos reales del centro llevan caracteres que hay que escapar (`1ºE`). `HttpParams`
   * lo codifica; una plantilla a pelo mandaría el literal. Es la PRIMERA petición con query
   * del frontend —ningún otro servicio de `services/` tenía una—, así que la forma se fija
   * aquí.
   *
   * <p>404 si el grupo o el hermano no existen. 400 si el par no es replicable, y ese 400
   * llega SIN texto que lo distinga: son siete guardas y una sola respuesta. Quién lo
   * interpreta es el componente, no este cliente; ver el javadoc de `ReplicacionDialogo`.
   */
  plan(idGrupo: number, codigoHermano: string): Observable<PlanReplicacion> {
    return this.http.get<PlanReplicacion>(this.ruta(idGrupo), {
      params: { hermano: codigoHermano },
    });
  }

  /**
   * POST /api/grupos/{idGrupo}/replicacion → crea los espejos y los cablea (201).
   *
   * <p>404 si el grupo o el hermano no existen; 409 si alguna actividad afectada tiene
   * dependientes (un horario colgando); 400 si las asignaciones no cubren EXACTAMENTE los
   * espejos de reparto, o por cualquiera de las guardas del plan.
   *
   * <p><b>Un 201 no significa que se haya creado algo.</b> Sobre un hermano sin subgrupos el
   * servidor acepta el `POST` y devuelve un plan vacío: éxito que no ha hecho nada. Pararlo
   * es de la pantalla; este cliente no filtra.
   */
  replicar(idGrupo: number, peticion: ReplicacionRequest): Observable<PlanReplicacion> {
    return this.http.post<PlanReplicacion>(this.ruta(idGrupo), peticion);
  }

  /**
   * DELETE /api/grupos/{idGrupo}/replicacion → deja el grupo sin subgrupos y devuelve el
   * parte de lo retirado (200, no 204: el cuerpo es el parte).
   *
   * <p>No revierte «la última replicación» —nada persistido la identifica—: borra TODOS los
   * subgrupos que el grupo tenga hoy, incluida cualquier edición manual posterior. Es
   * idempotente: sobre un grupo ya pelado devuelve 200 con las dos listas vacías, no 404.
   *
   * <p>404 solo si el grupo no existe; 409 si algún subgrupo no es exclusivo del grupo o si
   * alguna actividad afectada tiene dependientes.
   */
  deshacer(idGrupo: number): Observable<ParteDeshacer> {
    return this.http.delete<ParteDeshacer>(this.ruta(idGrupo));
  }
}
