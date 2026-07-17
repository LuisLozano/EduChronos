package es.yaroki.educhronos.app.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de {@link Nivel}. Porta el mapa inverso de la única FK RESTRICT que apunta
 * a {@code nivel} en {@code schema.sql} (Bloque 8.5-C2b; ver {@link AulaRepository} para
 * el porqué de las nativas).
 */
public interface NivelRepository extends JpaRepository<Nivel, Long> {
    Optional<Nivel> findByCodigo(String codigo);

    /** FK {@code grupo_administrativo.nivel_id} → nivel. */
    @Query(value = "select count(*) from grupo_administrativo where nivel_id = :id",
            nativeQuery = true)
    long contarGrupos(@Param("id") Long id);
}
