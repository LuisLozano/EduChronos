package es.yaroki.educhronos.app.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SesionBloqueadaRepository
        extends JpaRepository<SesionBloqueada, Long> {

    Optional<SesionBloqueada> findByActividadAndIndice(Actividad actividad, int indice);
}
