package es.yaroki.educhronos.app.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HorarioGeneradoRepository extends JpaRepository<HorarioGenerado, Long> {

    /**
     * El horario vigente del curso abierto: el de id MAYOR (O-curso, condición 4; S161).
     * Se ordena por id y no por {@code fecha_generacion} porque la fecha depende del reloj
     * del equipo. Sin AUTOINCREMENT, SQLite asigna max(id)+1, así que el id mayor es siempre
     * el último insertado de los que existen. Los ids se repiten entre ficheros de curso
     * (nota de S160 en D-horario-id-a-fuego), pero cada consulta mira sólo la base abierta.
     */
    Optional<HorarioGenerado> findFirstByOrderByIdDesc();
}
