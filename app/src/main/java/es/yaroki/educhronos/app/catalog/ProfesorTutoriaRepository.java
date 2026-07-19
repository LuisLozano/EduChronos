package es.yaroki.educhronos.app.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de {@link ProfesorTutoria} (Bloque 8.5-D2a). Porta además el mapa inverso
 * de la FK RESTRICT {@code profesor_tutoria.profesor_id} → {@code profesor}, que
 * {@code ProfesorService.borrar} consulta para su 409 (ver {@link AulaRepository} para el
 * porqué de las nativas).
 *
 * <p>No hay conteo inverso por {@code grupo_id}: esa FK es {@code ON DELETE CASCADE}
 * porque la tutoría es POBLACIÓN PROPIA del grupo, no una referencia entrante que deba
 * vetar su borrado (mismo criterio que S75 con las compatibilidades de aula).
 */
public interface ProfesorTutoriaRepository
        extends JpaRepository<ProfesorTutoria, ProfesorTutoria.ProfesorTutoriaId> {

    List<ProfesorTutoria> findByGrupo(GrupoAdministrativo grupo);

    List<ProfesorTutoria> findByGrupo_Id(Long grupoId);

    /** FK {@code profesor_tutoria.profesor_id} → profesor. */
    @Query(value = "select count(*) from profesor_tutoria where profesor_id = :id",
            nativeQuery = true)
    long contarTutorias(@Param("id") Long id);
}
