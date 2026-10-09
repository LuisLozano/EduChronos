package es.yaroki.educhronos.app.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuardiaRepository extends JpaRepository<Guardia, Long> {

    /**
     * Las guardias de un horario. Por consulta y no por una colección inversa en
     * {@link HorarioGenerado}, por lo mismo que {@link SesionRepository#findByHorarioId}: esa
     * colección refleja lo que Hibernate tenga cargado, no lo que hay en la tabla
     * (D-post-horario-sin-sesiones).
     */
    List<Guardia> findByHorarioId(Long horarioId);

    /**
     * Borra de golpe las guardias de un horario, en el acto y no al vaciar la sesión (S214,
     * C-ajuste-guardias, E2). El ajuste da de alta las nuevas justo después y casi todas coinciden con
     * las de antes: con borrados por entidad, Hibernate haría las altas primero y la única
     * (horario, profesor, tramo) saltaría.
     *
     * <p>Vuelca antes y vacía después la sesión de Hibernate: una guardia del horario que siguiera
     * en ella tras el borrado chocaría con la nueva que reciba su mismo id (SQLite los reutiliza) y
     * una lectura posterior la devolvería con los datos viejos.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Guardia g where g.horario.id = :horarioId")
    int borrarDeHorario(@Param("horarioId") Long horarioId);
}
