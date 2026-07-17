package es.yaroki.educhronos.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.app.catalog.Nivel;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

/**
 * Prueba que la PK sintética se PERSISTE en la columna {@code id} y se relee por
 * la capa ORM (Bloque 8.5-C2a-DDL, corrección de la PK). Cubre lo que la sonda SQL
 * nativa no llegaba a cubrir: el puente Hibernate {@code Long} ↔ {@code INTEGER
 * PRIMARY KEY} de SQLite.
 *
 * <p>{@code nivel} es una de las 8 tablas cuyo DDL verbatim declaraba {@code id}
 * sin tipo (no alias de rowid → columna NULL, id inservible como destino de FK).
 * Era el padre de la cascada de fallos de FK. El aserto discriminante fuerza un
 * {@code flush} + {@code clear} del contexto de persistencia para VACIAR el caché
 * de primer nivel: así el {@code findById} vuelve a la BD de verdad y demuestra
 * que el id vive en la columna, no solo en el rowid oculto.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IdPrimaryKeyRoundTripTest {

    @PersistenceContext
    private EntityManager em;

    @Test
    void elIdSePersisteEnLaColumnaYSeReleeTrasVaciarElCacheL1() {
        Nivel nivel = new Nivel("1ESO", 1);
        em.persist(nivel);
        em.flush();

        Long idGenerado = nivel.getId();
        assertThat(idGenerado).as("IDENTITY debe asignar un id no nulo").isNotNull();

        // Vaciar el caché L1: sin esto, findById devolvería la instancia gestionada
        // en memoria y no probaría nada sobre lo persistido en la columna.
        em.clear();

        Nivel recuperado = em.find(Nivel.class, idGenerado);
        assertThat(recuperado)
                .as("con id NULL en la columna, find por el id devuelto seria null")
                .isNotNull();
        assertThat(recuperado.getId()).isEqualTo(idGenerado);
        assertThat(recuperado.getCodigo()).isEqualTo("1ESO");
    }
}
