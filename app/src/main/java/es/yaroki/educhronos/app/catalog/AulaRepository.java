package es.yaroki.educhronos.app.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AulaRepository extends JpaRepository<Aula, Long> {
    Optional<Aula> findByCodigo(String codigo);
}
