package es.yaroki.educhronos.app.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsignaturaRepository extends JpaRepository<Asignatura, Long> {
    Optional<Asignatura> findByCodigo(String codigo);
}
