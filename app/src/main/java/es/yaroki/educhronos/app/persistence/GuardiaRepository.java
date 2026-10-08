package es.yaroki.educhronos.app.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardiaRepository extends JpaRepository<Guardia, Long> {

    /**
     * Las guardias de un horario. Por consulta y no por una colección inversa en
     * {@link HorarioGenerado}, por lo mismo que {@link SesionRepository#findByHorarioId}: esa
     * colección refleja lo que Hibernate tenga cargado, no lo que hay en la tabla
     * (D-post-horario-sin-sesiones).
     */
    List<Guardia> findByHorarioId(Long horarioId);
}
