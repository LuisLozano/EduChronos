package es.yaroki.educhronos.app.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubgrupoRepository extends JpaRepository<Subgrupo, Long> {

    java.util.Optional<Subgrupo> findByCodigo(String codigo);
}