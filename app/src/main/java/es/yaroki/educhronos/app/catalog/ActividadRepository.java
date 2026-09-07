package es.yaroki.educhronos.app.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de {@link Actividad}. Porta el mapa inverso de las FK que impiden borrarla
 * (Bloque 8.5-C2b; ver {@link AulaRepository} para el porqué de las nativas). Es el CASO
 * PROPIO del bloque: su mapa inverso tiene DOS naturalezas.
 *
 * <p><b>Directas</b> ({@code sesion_bloqueada.actividad_id}, {@code aula_bloqueada.actividad_id}):
 * RESTRICT contra la propia actividad, como cualquier otra raíz.
 *
 * <p><b>Travesía por sus plazas.</b> Preguntar "¿tiene plazas?" sería un FALSO POSITIVO: las
 * plazas son parte del agregado y {@code plaza.actividad_id} es ON DELETE CASCADE, así que
 * una actividad con plazas y nada más SÍ se borra (las plazas caen con ella, junto a las tres
 * join tables, también en cascade). Lo que aborta el borrado es que el intento de cascadear
 * las plazas choque con las FK RESTRICT que apuntan a ESAS plazas desde fuera del agregado:
 * {@code sesion.plaza_id} y {@code aula_bloqueada.plaza_id}. De ahí la subconsulta
 * {@code plaza_id in (select id from plaza where actividad_id = :id)}: no cuenta plazas,
 * cuenta quién las retiene.
 */
public interface ActividadRepository extends JpaRepository<Actividad, Long> {
    Optional<Actividad> findByCodigo(String codigo);

    /**
     * Las actividades que TOCAN a un grupo —alguna de sus plazas tiene un subgrupo cuya
     * población incluye ese grupo—, con la travesía {@code plazas → subgrupos → grupos}
     * traída en la MISMA consulta (Bloque S139, C-replicación-alta). Sin el {@code join
     * fetch}, clasificar un bloque obliga a navegar tres colecciones {@code LAZY} por plaza
     * y dispara un N+1 de cientos de SELECT.
     *
     * <p><b>El filtro va en SUBCONSULTA, no en el {@code where} del fetch, y no es un
     * detalle de estilo.</b> Un {@code join fetch p.subgrupos s ... where s.grupos.codigo =
     * :codigo} devuelve las actividades con sus colecciones TRUNCADAS a los elementos que
     * casan el filtro: {@code a.plazas} se quedaría solo con las plazas que tocan al grupo y
     * {@code p.subgrupos} solo con los suyos. Sobre eso, la unión de grupos de una actividad
     * valdría siempre {@code {grupo}} y TODO bloque se clasificaría como replicado. Peor: esas
     * colecciones truncadas son las de entidades GESTIONADAS, así que un flush posterior
     * podría interpretar lo que falta como filas a borrar. La subconsulta selecciona QUÉ
     * actividades, y el fetch las trae ENTERAS.
     *
     * <p>Los {@code left join fetch} conservan la plaza sin subgrupos y el subgrupo sin
     * grupos: hoy no existe ninguno en el catálogo real, pero un {@code inner join} los
     * borraría de la colección en memoria en vez de fallar, que es el modo de error caro.
     *
     * <p>Una sola bag en juego ({@code Actividad.plazas} es {@code List}; {@code subgrupos} y
     * {@code grupos} son {@code Set}), así que no hay {@code MultipleBagFetchException}.
     */
    @Query("select distinct a from Actividad a"
            + " left join fetch a.plazas p"
            + " left join fetch p.subgrupos s"
            + " left join fetch s.grupos"
            + " where a.id in ("
            + "   select a2.id from Actividad a2"
            + "     join a2.plazas p2"
            + "     join p2.subgrupos s2"
            + "     join s2.grupos g2"
            + "   where g2.codigo = :codigo)")
    List<Actividad> findConPoblacionPorGrupo(@Param("codigo") String codigo);

    /** Directa: FK {@code sesion_bloqueada.actividad_id} → actividad (pin de tramo). */
    @Query(value = "select count(*) from sesion_bloqueada where actividad_id = :id",
            nativeQuery = true)
    long contarSesionesBloqueadas(@Param("id") Long id);

    /** Travesía: FK RESTRICT {@code sesion.plaza_id} → alguna plaza de esta actividad. */
    @Query(value = "select count(*) from sesion"
            + " where plaza_id in (select id from plaza where actividad_id = :id)",
            nativeQuery = true)
    long contarSesionesSobreSusPlazas(@Param("id") Long id);

    /**
     * Directa Y travesía EN UNA consulta: {@code aula_bloqueada} referencia a la actividad por
     * DOS FK a la vez ({@code actividad_id} y {@code plaza_id}), y una misma fila satisface
     * ambas —una {@code AulaBloqueada} siempre cuelga de una plaza DE su actividad—. Contarlas
     * por separado reportaría la MISMA fila dos veces ("1 aula(s) bloqueada(s), 1 aula(s)
     * bloqueada(s) sobre sus plazas"), así que el {@code or} las une y el {@code count} las
     * deduplica por fila. Es la única FK entrante del bloque que no va 1:1 con su consulta.
     */
    @Query(value = "select count(*) from aula_bloqueada where actividad_id = :id"
            + " or plaza_id in (select id from plaza where actividad_id = :id)",
            nativeQuery = true)
    long contarAulasBloqueadas(@Param("id") Long id);
}
