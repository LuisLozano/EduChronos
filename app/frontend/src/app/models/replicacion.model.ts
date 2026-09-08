/**
 * Espejo del contrato REST del sub-recurso REPLICACIÓN, `/api/grupos/{id}/replicacion`
 * (Bloques S139 y S140): poblar un grupo ordinario recién creado copiando la estructura
 * de un hermano suyo del mismo nivel.
 *
 * Fuente: `app/src/main/java/.../web/dto/` — `PlanReplicacionDTO`, `BloqueDTO`, `ViaDTO`,
 * `ReplicacionRequest`, `AsignacionRequest`, `ParteDeshacerDTO` y `PlazaDescableadaDTO`.
 * Los siete, con sus nombres de campo tal cual; los tipos pierden el sufijo `DTO`, que es
 * lo que hace el resto de `models/` (`GrupoDTO` → {@link Grupo}).
 *
 * <p><b>`plazaCodigo` se recibe y NO SE PINTA:</b> es un ordinal técnico interno
 * (`{actividad}-P{n}`) que `PlazaRequest` documenta como INESTABLE entre ediciones, así
 * que enseñarlo daría al usuario un identificador que mañana nombra otra cosa. Viaja para
 * que el plan sea legible en un volcado, no para la pantalla; quien referencia una plaza
 * es `plazaId`.
 *
 * <p><b>Dos identificadores y ninguno intercambiable:</b> el HERMANO se nombra por CÓDIGO
 * (parámetro del `GET`) y la PLAZA por `plazaId` (cuerpo del `POST`). Los subgrupos se
 * nombran siempre por su código de ESPEJO, nunca por el del original del hermano.
 */

/**
 * Una VÍA de un bloque: una plaza entre las que el grupo nuevo puede entrar.
 *
 * <p>`gruposActuales` son códigos de GRUPO —no de subgrupo— y son lo único legible que
 * distingue una vía de otra: el contrato no manda profesor ni aula, y `asignatura` es el
 * CÓDIGO de la asignatura, no su nombre completo.
 *
 * <p>`espejos` son los códigos de subgrupo espejo cuyos ORIGINALES están en esta vía
 * (Bloque S141). Es el enlace que ata un espejo a su bloque de reparto; sin él,
 * {@link PlanReplicacion} dice qué bloques reparten y qué espejos se van a crear, pero no
 * cuáles de esos espejos decide cada bloque. Vacío es legítimo: `vias` trae TODAS las
 * plazas del bloque, también las que hoy no tocan al hermano.
 */
export interface Via {
  plazaId: number;
  /** Ordinal técnico inestable. Se recibe y NO se pinta; ver el javadoc del fichero. */
  plazaCodigo: string;
  /** CÓDIGO de la asignatura, no su `nombreCompleto`. */
  asignatura: string;
  gruposActuales: string[];
  espejos: string[];
  hermanoPresente: boolean;
}

/**
 * Un BLOQUE —actividad con más de una plaza—. `vias` son TODAS las plazas del bloque, no
 * solo las que tocan al hermano: para elegir hay que ver también las que hoy no le tocan.
 */
export interface Bloque {
  /** CÓDIGO de la actividad. */
  actividad: string;
  vias: Via[];
}

/**
 * Lo que la replicación HARÍA (`GET`) o lo que acaba de hacer (`POST`, 201): el mismo
 * record en los dos verbos, ya materializado en el segundo.
 *
 * <p>`replicados` y `reparto` son la PARTICIÓN de los bloques que tocan al hermano, y solo
 * de ellos: un bloque está en una lista o en la otra, nunca en las dos. En `replicados` no
 * hay nada que decidir —el grupo nuevo entra por la vía de su original—; en `reparto` la
 * decisión es del usuario, vía a vía.
 *
 * <p>Las actividades de UNA SOLA PLAZA no salen en ninguna de las dos y no se tocan: su
 * espejo (típicamente el `-Completo`) se crea con cero plazas. Por eso las dos listas
 * VACÍAS con `subgruposACrear` vacío significan «este hermano no tiene nada que copiar», y
 * el servidor responde a eso con un 200 y al `POST` con un 201 que no ha creado nada.
 */
export interface PlanReplicacion {
  grupo: string;
  hermano: string;
  /** Códigos de los subgrupos ESPEJO, derivados como `{grupo}-{sufijo}`. */
  subgruposACrear: string[];
  replicados: Bloque[];
  reparto: Bloque[];
}

/**
 * La decisión del usuario para UN subgrupo espejo de un bloque de reparto.
 *
 * <p>`subgrupo` es el código del ESPEJO —el de `PlanReplicacion.subgruposACrear`—, no el
 * del original del hermano.
 *
 * <p>`plaza` a `null` es LEGÍTIMO y no un error: significa «no cablear», el grupo nuevo no
 * tiene alumnos en esa vía. El espejo se crea igual y se queda sin plazas. Lo que sí es
 * error es omitir la asignación de un espejo de reparto, o mandarla dos veces.
 */
export interface AsignacionRequest {
  subgrupo: string;
  plaza: number | null;
}

/**
 * Cuerpo del `POST`. `asignaciones` debe traer EXACTAMENTE UNA entrada por cada espejo de
 * reparto: ni una menos ni una repetida. Los espejos de bloques replicados no se nombran
 * aquí; su vía la deduce el servidor.
 */
export interface ReplicacionRequest {
  /** CÓDIGO del hermano, no su id. */
  hermano: string;
  asignaciones: AsignacionRequest[];
}

/**
 * UNA retirada del deshacer: el subgrupo deja de estar en la plaza. El grano es el PAR
 * (plaza, subgrupo), porque un subgrupo puede estar en varias plazas y una plaza lleva
 * varios subgrupos. `actividad` acompaña para que el parte se lea sin abrir la base.
 */
export interface PlazaDescableada {
  plazaId: number;
  /** Ordinal técnico inestable. Se recibe y NO se pinta; ver el javadoc del fichero. */
  plazaCodigo: string;
  actividad: string;
  subgrupo: string;
}

/**
 * Lo que el `DELETE` acaba de hacer: hechos consumados, nunca un plan —no hay `GET` del
 * deshacer—. Las dos listas vacías con `grupo` relleno es la respuesta legítima al deshacer
 * de un grupo ya pelado: el `DELETE` es idempotente y llamarlo dos veces no es un error.
 */
export interface ParteDeshacer {
  grupo: string;
  subgruposBorrados: string[];
  plazasDescableadas: PlazaDescableada[];
}
