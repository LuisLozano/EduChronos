package es.yaroki.educhronos.app.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de {@link Asignatura}. Porta el mapa inverso de las dos FK RESTRICT que
 * apuntan a {@code asignatura} en {@code schema.sql} (Bloque 8.5-C2b; ver
 * {@link AulaRepository} para el porqué de las nativas). La FK de
 * {@code asignatura_aula_compatible} dejó de ser referencia entrante en 8.5-C3: pasa a
 * {@code on delete cascade} (las compatibilidades son subordinadas de la asignatura).
 */
public interface AsignaturaRepository extends JpaRepository<Asignatura, Long> {
    Optional<Asignatura> findByCodigo(String codigo);

    /** FK {@code actividad.asignatura_id} → asignatura (nullable: solo si la actividad la fija). */
    @Query(value = "select count(*) from actividad where asignatura_id = :id", nativeQuery = true)
    long contarActividades(@Param("id") Long id);

    /** FK {@code plaza.asignatura_id} → asignatura (obligatoria en cada plaza). */
    @Query(value = "select count(*) from plaza where asignatura_id = :id", nativeQuery = true)
    long contarPlazas(@Param("id") Long id);
}
