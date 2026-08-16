/**
 * Espejo del contrato REST de la ACTIVIDAD como AGREGADO (§4.6, Bloque 8.5-C1).
 *
 * Fuente: `app/src/main/java/.../web/dto/ActividadDTO.java`, `ActividadRequest.java`,
 * `PlazaDTO.java` y `PlazaRequest.java`.
 *
 * <p>La actividad es la RAÍZ y sus plazas viajan EMBEBIDAS: no hay `/api/plazas` ni
 * modelo de plaza independiente. Toda escritura de plaza pasa por el cuerpo de la
 * actividad.
 *
 * <p>Todas las referencias viajan por CÓDIGO (`string`), nunca por id ni como objeto
 * anidado —patrón 8.5-B, el mismo de `subgrupo.model.ts`—: el backend resuelve cada
 * código y devuelve un 400 que NOMBRA el primero no resoluble. El consumidor razona en
 * códigos.
 *
 * <p><b>XOR de aula.</b> Por contrato, cada plaza lleva `aulaFija` O `aulasCandidatas`,
 * exactamente una de las dos ramas: fija presente y candidatas vacías, o fija `null` y
 * ≥1 candidata. Las dos a la vez, o ninguna, son 400. El modelo no puede expresar el XOR
 * en tipos sin partir `PlazaRequest` en dos, así que lo garantiza quien construye el
 * cuerpo (`ActividadForm`) y, en última instancia, el backend.
 */

/** Espejo de `PlazaDTO(Long id, String codigo, String asignatura, String aulaFija,
 *  List<String> aulasCandidatas, List<String> profesores, List<String> subgrupos)`. */
export interface Plaza {
  /** `Long` en el backend; siempre presente en un DTO devuelto por el servidor. */
  id: number;
  /** DERIVADO por el backend como `{codigoActividad}-P{n}`; el usuario no lo teclea. */
  codigo: string;
  /** CÓDIGO de la asignatura de la plaza: OBLIGATORIA aunque la de la actividad sea null. */
  asignatura: string;
  /** Rama fija del XOR: código del aula, o `null` si la plaza va por candidatas. */
  aulaFija: string | null;
  /** Rama candidatas del XOR: vacía si la plaza tiene aula fija. */
  aulasCandidatas: string[];
  /** CÓDIGOS de los profesores; ≥1 por contrato (invariante I7). */
  profesores: string[];
  /** CÓDIGOS de los subgrupos que pueblan la plaza. Puede venir VACÍA. */
  subgrupos: string[];
}

/** Espejo de `ActividadDTO(Long id, String codigo, String asignatura, int duracionTramos,
 *  int repeticionesPorSemana, String patronTemporal, boolean requiereTutor,
 *  List<PlazaDTO> plazas)`. */
export interface Actividad {
  id: number;
  codigo: string;
  /** OPCIONAL (§4.6): `null` cuando las plazas tienen distintas asignaturas. */
  asignatura: string | null;
  duracionTramos: number;
  repeticionesPorSemana: number;
  /** `DISTRIBUIDA` | `AGRUPADA` | `NEUTRA`; llega como `string`, no como enum. */
  patronTemporal: string;
  requiereTutor: boolean;
  /** ≥1 por contrato: una actividad sin plazas es 400. */
  plazas: Plaza[];
}

/**
 * Espejo de `PlazaRequest(String asignatura, String aulaFija, List<String>
 * aulasCandidatas, List<String> profesores, List<String> subgrupos)`.
 *
 * <p><b>SIN `id` y SIN `codigo`, a propósito.</b> El código de plaza NO lo teclea el
 * usuario: lo DERIVA el backend como `{codigoActividad}-P{n}` y es INESTABLE entre
 * ediciones —la reconciliación del PUT empareja las plazas POR POSICIÓN, no por código
 * ni por id, así que el mismo `-P2` puede describir otra cosa tras una edición—. Añadir
 * aquí un `id` sería reconciliar por identidad, que está explícitamente descartado.
 */
export interface PlazaRequest {
  asignatura: string;
  aulaFija: string | null;
  aulasCandidatas: string[];
  profesores: string[];
  subgrupos: string[];
}

/**
 * Espejo de `ActividadRequest(String codigo, String asignatura, int duracionTramos,
 * int repeticionesPorSemana, String patronTemporal, boolean requiereTutor,
 * List<PlazaRequest> plazas)`.
 *
 * <p>Sin `id`: en el alta lo asigna JPA; en la edición lo lleva la URL, no el cuerpo.
 *
 * <p>En la edición la lista de `plazas` es el ESTADO DESEADO completo, no un delta: el
 * backend reconcilia por posición y BORRA las que sobran. Enviar menos plazas de las que
 * la actividad tiene destruye las restantes.
 */
export interface ActividadRequest {
  codigo: string;
  asignatura: string | null;
  duracionTramos: number;
  repeticionesPorSemana: number;
  patronTemporal: string;
  requiereTutor: boolean;
  plazas: PlazaRequest[];
}

/** Los tres patrones que admite el backend (`PatronTemporal`), para poblar el select. */
export const PATRONES_TEMPORALES = ['DISTRIBUIDA', 'AGRUPADA', 'NEUTRA'] as const;
