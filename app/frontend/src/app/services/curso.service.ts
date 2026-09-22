import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AbrirCursoRequest,
  CursoCreadoDTO,
  CursoDTO,
  CursoListadoDTO,
  DuplicarCursoRequest,
} from '../models/curso.model';

/**
 * Cliente REST de los cursos: el abierto, los que hay, abrir otro y crear el siguiente
 * (O-curso, S160, C-selector-curso).
 *
 * CADA CONTRATO SU CLIENTE (precedente `JornadaService`): wrappers pelados, sin `.pipe` y
 * sin `catchError`. El servicio PROPAGA el error de Http; el componente lo traduce en su
 * `subscribe({ next, error })`. Aquí importa más que en ningún otro cliente, porque de
 * estos endpoints salen SIETE causas distintas —desde «ese nombre ya existe» hasta «no se
 * ha podido abrir el fichero»— y cada una lleva al usuario por un camino diferente; un
 * `catchError` en esta capa dejaría esa decisión sin dueño y sin test.
 *
 * <p><b>Dos bases y no una</b>, porque son dos recursos: `/api/curso` es el curso abierto
 * (singleton, sin id, como la jornada) y `/api/cursos` la colección de la carpeta. Meterlos
 * bajo una raíz común obligaría a que uno de los dos mintiera sobre su cardinalidad.
 */
@Injectable({ providedIn: 'root' })
export class CursoService {
  private readonly http = inject(HttpClient);

  /** El curso ABIERTO. Singleton. */
  private readonly abierto = '/api/curso';

  /** La COLECCIÓN de cursos de la carpeta. */
  private readonly coleccion = '/api/cursos';

  /**
   * GET /api/curso → el curso abierto. Nunca 404: una base sin nombre de curso no es «no
   * encontrada», es una base anterior a S159 y responde 200 con `nombre: null`.
   */
  obtener(): Observable<CursoDTO> {
    return this.http.get<CursoDTO>(this.abierto);
  }

  /**
   * GET /api/cursos → los cursos de la carpeta. Nunca vacío: la base abierta siempre es
   * uno de ellos.
   */
  listar(): Observable<CursoListadoDTO[]> {
    return this.http.get<CursoListadoDTO[]>(this.coleccion);
  }

  /**
   * POST /api/cursos/abrir → cambia la base abierta y devuelve el estado resultante, con
   * la misma forma que el GET.
   *
   * <p>Es un POST bajo la colección y no un PUT sobre el singleton: abrir no es escribir
   * el estado del curso abierto, es una operación con turno y marcha atrás que puede
   * contestar 400, 404, 409, 503 o 500 por razones que no tienen que ver con lo enviado.
   *
   * @param fichero nombre simple del fichero, tal como vino en el listado
   */
  abrir(fichero: string): Observable<CursoDTO> {
    const cuerpo: AbrirCursoRequest = { fichero };
    return this.http.post<CursoDTO>(`${this.coleccion}/abrir`, cuerpo);
  }

  /**
   * POST /api/cursos → crea el curso siguiente, archiva el actual y DEJA ABIERTO EL NUEVO.
   * 201 con el nombre y el fichero creados.
   */
  duplicar(peticion: DuplicarCursoRequest): Observable<CursoCreadoDTO> {
    return this.http.post<CursoCreadoDTO>(this.coleccion, peticion);
  }
}
