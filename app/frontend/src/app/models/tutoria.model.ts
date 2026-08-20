/**
 * Espejo del contrato REST del sub-recurso de TUTORÍA de un grupo,
 * `/api/grupos/{idGrupo}/tutoria` (§4.1, invariante I4, Bloque 8.5-D2a).
 *
 * Fuente: `app/src/main/java/.../web/dto/TutoriaDTO.java` y `TutoriaRequest.java`.
 *
 * <p>`profesor` viaja como el CÓDIGO de negocio del profesor (string), NO como su id
 * sintético (`TutoriaRequest.java:8-10`, literal: «viaja como el CÓDIGO de negocio del
 * profesor (String), NO como su id sintético»). El backend lo resuelve con
 * `ProfesorRepository.findByCodigo` y responde 404 si ese código no existe. Asignar
 * directamente el `id` de un `Profesor` a este campo NO compila —`number` no es
 * `string`—, pero el id colado a través del `value` de un `<select>` llega ya como
 * texto y sí pasa el compilador: muere en tiempo de ejecución con ese 404. El
 * desplegable de tutor tiene que enlazar el `codigo`, no el `id`.
 *
 * <p>El GRUPO no viaja en el cuerpo: lo lleva la URL del sub-recurso. Por eso ningún
 * tipo de este fichero tiene campo de grupo, ni en la respuesta ni en la petición.
 */

/**
 * Los dos valores de `RolTutoria` del backend (`catalog/RolTutoria.java:10-13`).
 *
 * <p>I4 los trata de forma ASIMÉTRICA: el `TUTOR_PRINCIPAL` es único por grupo —el
 * backend rechaza con 400 un cuerpo con dos— mientras que los `CO_TUTOR` pueden ser
 * varios. El tipo no expresa esa asimetría; solo enumera los valores que el backend
 * acepta.
 *
 * <p>UNIÓN DE LITERALES, y no un `enum` de TypeScript: un `enum` genera código en
 * runtime y obligaría a importarlo para escribir un valor, cuando lo que viaja por el
 * cable es la cadena pelada del `name()` del enum Java.
 *
 * <p>DESVIACIÓN CONSCIENTE del molde de modelos: los demás enums espejados aquí son
 * `string` pelado (`Aula.tipo`, `Actividad.patronTemporal`, `Grupo.tipo`,
 * `Violacion.regla`). Aquellos son campos de LECTURA cuyo enum puede crecer en el
 * backend sin que este lado se entere —el TSDoc de `Grupo.tipo` lo dice: «quien consuma
 * este campo no puede dar por hecho un único valor»—. Este se ESCRIBE desde un
 * desplegable de dos opciones, así que cerrarlo convierte en error de compilación un
 * rol mal tecleado que si no sería un 400 en runtime.
 */
export type RolTutoria = 'TUTOR_PRINCIPAL' | 'CO_TUTOR';

/**
 * Espejo de `TutoriaDTO(String profesor, String rol)`. Una tutoría tal como sale del
 * `GET` y del `PUT` del sub-recurso.
 *
 * <p>Sin `id`: la fila no tiene identidad propia en el backend más allá del par
 * (profesor, grupo), que es su clave compuesta (`ProfesorTutoria.java:18-23`).
 */
export interface Tutoria {
  /** CÓDIGO del profesor (p. ej. `'MAT1'`), no su id. Ver la cabecera del fichero. */
  profesor: string;
  rol: RolTutoria;
}

/**
 * Espejo de `TutoriaRequest(String profesor, String rol)`: UN elemento del cuerpo del
 * `PUT /api/grupos/{idGrupo}/tutoria`.
 *
 * <p>ALIAS y no una interfaz aparte: los dos records del backend son IDÉNTICOS campo a
 * campo —`TutoriaDTO(String profesor, String rol)` y `TutoriaRequest(String profesor,
 * String rol)`—, así que duplicar la forma aquí sería una segunda fuente de verdad que
 * se desincroniza sola. El alias existe para nombrar la INTENCIÓN en las firmas: leer
 * `reemplazar(id, TutoriaRequest[])` dice que eso va hacia el servidor, y si algún día
 * los dos records divergen, este es el punto donde se separan.
 */
export type TutoriaRequest = Tutoria;
