package es.yaroki.educhronos.app.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AulaBloqueadaRepository
        extends JpaRepository<AulaBloqueada, Long> {

    List<AulaBloqueada> findByActividadAndIndice(Actividad actividad, int indice);

    void deleteByActividadAndIndice(Actividad actividad, int indice);
}
