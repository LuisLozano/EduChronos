package es.yaroki.educhronos.app.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NivelRepository extends JpaRepository<Nivel, Long> {
    Optional<Nivel> findByCodigo(String codigo);
}
