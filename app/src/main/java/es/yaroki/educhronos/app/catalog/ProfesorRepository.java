package es.yaroki.educhronos.app.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de {@link Profesor}. Porta el mapa inverso de las dos FK RESTRICT que
 * apuntan a {@code profesor} en {@code schema.sql} (Bloque 8.5-C2b; ver
 * {@link AulaRepository} para el porqué de las nativas).
 */
public interface ProfesorRepository extends JpaRepository<Profesor, Long> {
    Optional<Profesor> findByCodigo(String codigo);

    /** FK {@code plaza_profesor.profesor_id} → profesor (join del M:N que posee Plaza). */
    @Query(value = "select count(*) from plaza_profesor where profesor_id = :id",
            nativeQuery = true)
    long contarPlazas(@Param("id") Long id);

    /** FK {@code profesor_restriccion_horaria.profesor_id} → profesor. */
    @Query(value = "select count(*) from profesor_restriccion_horaria where profesor_id = :id",
            nativeQuery = true)
    long contarRestriccionesHorarias(@Param("id") Long id);
}
