package es.yaroki.educhronos.app.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio de {@link AsignaturaAula} (S206). No porta conteo inverso por
 * {@code asignatura_id}: esa FK es {@code ON DELETE CASCADE}. El de {@code aula_id} vive en
 * {@link AulaRepository}, con el resto del mapa inverso del aula.
 */
public interface AsignaturaAulaRepository
        extends JpaRepository<AsignaturaAula, AsignaturaAula.AsignaturaAulaId> {

    List<AsignaturaAula> findByAsignatura(Asignatura asignatura);
}
