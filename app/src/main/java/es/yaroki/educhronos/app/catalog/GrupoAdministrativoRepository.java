package es.yaroki.educhronos.app.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrupoAdministrativoRepository extends JpaRepository<GrupoAdministrativo, Long> {
    Optional<GrupoAdministrativo> findByCodigo(String codigo);
}
