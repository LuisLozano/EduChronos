/**
 * Espejo del contrato REST de la VERSIÓN de la aplicación (C-version-y-rastro, S192).
 *
 * Fuente: `app/src/main/java/.../web/dto/VersionDTO.java`.
 */

/**
 * Espejo de `VersionDTO(String version)`. El valor llega tal cual: la del tag en el bundle
 * de CI, `0.0.0-dev` en cualquier otro build, o `desconocida` si el jar no trae
 * `build-info`.
 */
export interface VersionDTO {
  version: string;
}
