/**
 * Espejo del contrato REST de catálogo de aulas.
 *
 * Fuente: `app/src/main/java/.../web/dto/AulaDTO.java` y `AulaRequest.java`.
 * `tipo` viaja como String (el `name()` del enum `TipoAula` del dominio), NO como
 * el enum crudo: el backend lo desenumera con `.name()` a la salida y lo parsea con
 * `TipoAula.valueOf(...)` a la entrada, devolviendo un 400 accionable que nombra el
 * valor recibido y lista los válidos si no existe. Aquí se calca ese contrato de
 * borde; el componente ofrece solo los tipos con semántica definida (COMUN queda
 * fuera del selector, D-F8.5-C3-a: "NO USAR hasta definirlo").
 */

/**
 * Espejo de `AulaDTO(Long id, String codigo, String tipo, Integer capacidad,
 * String edificio, Integer planta, String sector)`.
 */
export interface Aula {
  /** `Long` en el backend; siempre presente en un DTO devuelto por el servidor. */
  id: number;
  codigo: string;
  tipo: string;
  /** Opcionales de verdad (D-4): pueden llegar null. */
  capacidad: number | null;
  edificio: string | null;
  planta: number | null;
  sector: string | null;
}

/**
 * Espejo de `AulaRequest(String codigo, String tipo, Integer capacidad,
 * String edificio, Integer planta, String sector)`.
 * Sin `id`: el id va en la URL (`PUT /{id}`), no en el cuerpo.
 */
export interface AulaRequest {
  codigo: string;
  tipo: string;
  capacidad: number | null;
  edificio: string | null;
  planta: number | null;
  sector: string | null;
}

/**
 * Tipos de aula que el formulario OFRECE. Son los ocho valores de `TipoAula` con
 * semántica definida en `modelo_datos_fase1.md §4.1`. `COMUN` (noveno valor del
 * enum) se OMITE a propósito: D-F8.5-C3-a lo marca "sin semántica definida, NO USAR
 * hasta definirlo". El enum de backend lo conserva (retirarlo tocaría dos CHECK de
 * `schema.sql`, coste desproporcionado); aquí solo se mantiene fuera del alcance de
 * la UI, de modo que ningún dato con `tipo=COMUN` se cree desde el formulario.
 *
 * Omitir del CATÁLOGO no es lo mismo que borrar del DATO: si se edita un aula que
 * ya tiene `tipo=COMUN` (importada, o creada antes de esta pantalla), `AulaForm`
 * añade ese valor a las opciones de ESA edición para no perderlo en silencio.
 */
export const TIPOS_AULA = [
  'ORDINARIA',
  'LAB_CIENCIAS',
  'INFORMATICA',
  'TALLER_TEC',
  'TALLER_PLASTICA',
  'GIMNASIO',
  'PISTA',
  'TALLER_FPB',
] as const;
