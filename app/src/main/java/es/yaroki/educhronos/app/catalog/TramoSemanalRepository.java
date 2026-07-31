package es.yaroki.educhronos.app.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Repositorio de {@link TramoSemanal}. Porta el mapa inverso de las tres FK que apuntan
 * a {@code tramo_semanal} en {@code schema.sql} (C-jornada M3; ver {@link AulaRepository}
 * para el porqué de las nativas).
 *
 * <p><b>Los conteos son AGREGADOS, sin {@code :id}</b>, a diferencia de los de
 * {@link AsignaturaRepository}. La razón es la forma del recurso: la jornada no se edita
 * tramo a tramo, se REEMPLAZA entera ({@code PUT /api/jornada}), así que la pregunta que
 * hay que contestar no es "¿quién apunta a ESTE tramo?" sino "¿apunta alguien a ALGÚN
 * tramo?". Una variante por-id sería código muerto y sugeriría un borrado selectivo que
 * este recurso no ofrece.
 *
 * <p>La cuarta FK que llega a la tabla es la autorreferencial
 * {@code tramo_semanal.siguiente_inmediato_id}, que NO se cuenta: siempre vale null
 * (deuda registrada; poblarla es semántica del solver —invariante S6— y queda fuera de
 * C-jornada), y el reemplazo total borra la tabla entera de una vez.
 */
public interface TramoSemanalRepository extends JpaRepository<TramoSemanal, Long> {

    /** Los tramos en su orden global de la jornada ({@code orden} 1..N ascendente). */
    List<TramoSemanal> findAllByOrderByOrdenAsc();

    /** FK {@code profesor_restriccion_horaria.tramo_id} → tramo_semanal (not null). */
    @Query(value = "select count(*) from profesor_restriccion_horaria", nativeQuery = true)
    long contarRestriccionesHorarias();

    /** FK {@code sesion.tramo_inicio_id} → tramo_semanal (not null). */
    @Query(value = "select count(*) from sesion", nativeQuery = true)
    long contarSesiones();

    /** FK {@code sesion_bloqueada.tramo_inicio_id} → tramo_semanal (not null). */
    @Query(value = "select count(*) from sesion_bloqueada", nativeQuery = true)
    long contarSesionesBloqueadas();
}
