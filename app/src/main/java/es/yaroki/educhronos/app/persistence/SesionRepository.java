package es.yaroki.educhronos.app.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SesionRepository extends JpaRepository<Sesion, Long> {

    /**
     * TODAS las filas de un horario. Se usa para reconstruir la solución en vez de
     * {@code HorarioGenerado.getSesiones()}: esa colección inversa refleja lo que
     * Hibernate tenga cargado, no lo que hay en la tabla, y ya se midió que puede
     * venir vacía cuando las filas se insertaron sin actualizarla
     * (D-post-horario-sin-sesiones). Reconsultar no depende de ese estado.
     */
    List<Sesion> findByHorarioId(Long horarioId);

    /**
     * Filas de UNA instancia ({@code actividadCodigo}, {@code indice}) dentro de un
     * horario: de 1 a 6 según el desdoble. Consulta explícita y no derivada del nombre
     * porque el cruce va por {@code plaza.actividad.codigo}, dos saltos de relación que
     * un nombre derivado dejaría ilegible.
     *
     * <p>Existe para localizar y luego RECONSULTAR la instancia movida, en vez de
     * filtrar la colección inversa del {@code HorarioGenerado}
     * (D-post-horario-sin-sesiones): mismo patrón que {@code BloqueoService.aulasDe}.
     */
    @Query("""
            select s from Sesion s
            where s.horario.id = :horarioId
              and s.plaza.actividad.codigo = :actividadCodigo
              and s.indice = :indice
            """)
    List<Sesion> findParaInstancia(
            @Param("horarioId") Long horarioId,
            @Param("actividadCodigo") String actividadCodigo,
            @Param("indice") int indice);
}
