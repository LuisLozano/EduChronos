package es.yaroki.educhronos.app.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfesorRepository extends JpaRepository<Profesor, Long> {
    Optional<Profesor> findByCodigo(String codigo);
}
