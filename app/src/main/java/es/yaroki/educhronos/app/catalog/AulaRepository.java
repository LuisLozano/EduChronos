package es.yaroki.educhronos.app.catalog;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositorio de {@link Aula}. Además del CRUD, porta el MAPA INVERSO de las cuatro FK
 * RESTRICT que apuntan a {@code aula} en {@code schema.sql} (Bloque 8.5-C2b): son las
 * consultas que {@code AulaService.borrar} ejecuta antes del {@code delete} para dar un
 * 409 con desglose en vez del 500 opaco del mordisco de la FK.
 *
 * <p><b>Por qué nativas.</b> Tres de las cuatro FK entrantes no son navegables desde JPA:
 * {@code plaza.aula_fija_id} y {@code sesion.aula_id} viven en entidades sin repositorio
 * propio ({@code Plaza} es dependiente del agregado {@code Actividad} y no se le abre uno,
 * D-C1-A), y {@code plaza_aula_candidata} es una join table de un {@code @ManyToMany}
 * unidireccional cuyo propietario es {@code Plaza}, sin lado inverso en {@code Aula}.
 * Consultar el mapa de FK es integridad referencial, no navegación de dominio: cada raíz
 * pregunta "¿quién me referencia?" contra las tablas, y {@code schema.sql} es la autoridad
 * de esos nombres (decisión D-C2b-b).
 */
public interface AulaRepository extends JpaRepository<Aula, Long> {
    Optional<Aula> findByCodigo(String codigo);

    /** FK {@code plaza.aula_fija_id} → aula (rama aulaFija del XOR). */
    @Query(value = "select count(*) from plaza where aula_fija_id = :id", nativeQuery = true)
    long contarPlazasConAulaFija(@Param("id") Long id);

    /** FK {@code plaza_aula_candidata.aula_id} → aula (rama candidatas del XOR). */
    @Query(value = "select count(*) from plaza_aula_candidata where aula_id = :id",
            nativeQuery = true)
    long contarPlazasCandidatas(@Param("id") Long id);

    /** FK {@code aula_bloqueada.aula_id} → aula (pin de aula). */
    @Query(value = "select count(*) from aula_bloqueada where aula_id = :id", nativeQuery = true)
    long contarAulasBloqueadas(@Param("id") Long id);

    /** FK {@code sesion.aula_id} → aula (sesión de un horario generado). */
    @Query(value = "select count(*) from sesion where aula_id = :id", nativeQuery = true)
    long contarSesiones(@Param("id") Long id);
}
