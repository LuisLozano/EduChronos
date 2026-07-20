package es.yaroki.educhronos.app.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de {@link ProfesorRestriccionHoraria}. Los dos finders por profesor los
 * añade el Bloque 8.5-E (sub-recurso REST): {@code findByProfesor} lo consume el
 * reemplazo total dentro de su transacción; {@code findByProfesor_Id} existe para que
 * los tests puedan releer la BASE tras un {@code clear()} sin rehidratar el
 * {@link Profesor} (mismo par que {@code ProfesorTutoriaRepository}).
 *
 * <p>No hay conteo inverso aquí: el 409 al borrar un profesor con restricciones lo
 * resuelve {@code ProfesorRepository.contarRestriccionesHorarias}, que ya existía
 * (Bloque 8.5-C2b) y que este bloque NO ha tocado.
 */
public interface ProfesorRestriccionHorariaRepository
        extends JpaRepository<ProfesorRestriccionHoraria, Long> {

    List<ProfesorRestriccionHoraria> findByProfesor(Profesor profesor);

    List<ProfesorRestriccionHoraria> findByProfesor_Id(Long profesorId);
}
