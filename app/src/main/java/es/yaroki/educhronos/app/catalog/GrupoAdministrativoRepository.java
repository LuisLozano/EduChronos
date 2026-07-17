package es.yaroki.educhronos.app.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de {@link GrupoAdministrativo}. Porta el mapa inverso de las dos FK RESTRICT
 * que apuntan a {@code grupo_administrativo} en {@code schema.sql} (Bloque 8.5-C2b; ver
 * {@link AulaRepository} para el porqué de las nativas). Una de las dos es la AUTORREFERENCIA
 * {@code grupo_padre_id}: un grupo con hijos PDC tampoco se borra.
 */
public interface GrupoAdministrativoRepository extends JpaRepository<GrupoAdministrativo, Long> {
    Optional<GrupoAdministrativo> findByCodigo(String codigo);

    /** FK {@code subgrupo_grupo.grupo_id} → grupo_administrativo (join del M:N de población). */
    @Query(value = "select count(*) from subgrupo_grupo where grupo_id = :id", nativeQuery = true)
    long contarSubgrupos(@Param("id") Long id);

    /** FK autorreferencial {@code grupo_administrativo.grupo_padre_id} (I5: hijos PDC). */
    @Query(value = "select count(*) from grupo_administrativo where grupo_padre_id = :id",
            nativeQuery = true)
    long contarGruposHijos(@Param("id") Long id);
}
