package es.yaroki.educhronos.app.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de {@link Profesor}. Porta el mapa inverso de las FK RESTRICT que
 * apuntan a {@code profesor} en {@code schema.sql} (la de {@code guardia}, desde S213) (Bloque 8.5-C2b; ver
 * {@link AulaRepository} para el porqué de las nativas).
 */
public interface ProfesorRepository extends JpaRepository<Profesor, Long> {
    Optional<Profesor> findByCodigo(String codigo);

    /** FK {@code plaza_profesor.profesor_id} → profesor (join del M:N que posee Plaza). */
    @Query(value = "select count(*) from plaza_profesor where profesor_id = :id",
            nativeQuery = true)
    long contarPlazas(@Param("id") Long id);

    /** Códigos de esas plazas, sin orden: los ordena y corta {@code ReferenciaEntranteException} (S225). */
    @Query(value = "select p.codigo from plaza p join plaza_profesor pp on pp.plaza_id = p.id"
            + " where pp.profesor_id = :id", nativeQuery = true)
    List<String> codigosDePlazas(@Param("id") Long id);

    /** FK {@code profesor_restriccion_horaria.profesor_id} → profesor. */
    @Query(value = "select count(*) from profesor_restriccion_horaria where profesor_id = :id",
            nativeQuery = true)
    long contarRestriccionesHorarias(@Param("id") Long id);

    /** FK {@code guardia.profesor_id} → profesor (S213): las guardias repartidas en los horarios. */
    @Query(value = "select count(*) from guardia where profesor_id = :id",
            nativeQuery = true)
    long contarGuardias(@Param("id") Long id);
}
