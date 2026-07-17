package es.yaroki.educhronos.app.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de {@link Subgrupo}. Porta el mapa inverso de la ÚNICA FK RESTRICT ENTRANTE que
 * apunta a {@code subgrupo} en {@code schema.sql} (Bloque 8.5-C2b; ver {@link AulaRepository}
 * para el porqué de las nativas).
 *
 * <p>De las dos FK {@code *.subgrupo_id} del esquema, solo {@code plaza_subgrupo} es entrante:
 * {@code subgrupo_grupo} es la POBLACIÓN propia del subgrupo ({@code Subgrupo.grupos} es owner
 * del {@code @ManyToMany}), que Hibernate limpia al borrarlo. Contarla sería un falso positivo
 * —el mismo patrón de agregado que {@code Actividad}→plazas— y vetaría a todo subgrupo, que
 * exige ≥1 grupo.
 */
public interface SubgrupoRepository extends JpaRepository<Subgrupo, Long> {

    java.util.Optional<Subgrupo> findByCodigo(String codigo);

    /** FK {@code plaza_subgrupo.subgrupo_id} → subgrupo (join del M:N que posee Plaza). */
    @Query(value = "select count(*) from plaza_subgrupo where subgrupo_id = :id",
            nativeQuery = true)
    long contarPlazas(@Param("id") Long id);
}
