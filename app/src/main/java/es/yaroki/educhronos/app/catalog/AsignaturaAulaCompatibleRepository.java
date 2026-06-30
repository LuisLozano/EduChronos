package es.yaroki.educhronos.app.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsignaturaAulaCompatibleRepository
        extends JpaRepository<AsignaturaAulaCompatible, Long> {
    List<AsignaturaAulaCompatible> findByAsignatura(Asignatura asignatura);
}
