package es.yaroki.educhronos.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * El presupuesto de solve deja de ser un literal y pasa a salir de la property
 * {@code educhronos.solver.max-segundos} (S118).
 *
 * <p><b>Por qué con Spring y no un {@code new}.</b> Lo que se mide NO es el
 * operador ternario —eso sería tautológico— sino que la inyección OCURRE: que la
 * property llega al campo y que el defecto sale de ella. Un {@code new
 * GeneradorHorarioService(...)} dejaría {@code maxSegundosDefecto} en 0 y el test
 * pasaría verde con el {@code @Value} borrado, que es justo la mutación que debe
 * cazar. De ahí el {@code @DataJpaTest} + {@code @Import}, mismo montaje que
 * {@link GeneradorHorarioServiceTest}: es el contenedor quien resuelve el
 * {@code @Value} contra {@code src/test/resources/application.properties}.
 *
 * <p><b>El 5 del nombre es el valor de TEST</b>, no el de producción (600). Que
 * el fichero de test declare 5 es parte de lo aseverado: si alguien lo borra, el
 * respaldo del {@code @Value} daría 600 y el test cae —correctamente, porque un
 * solve de 600 s no puede entrar en la suite rápida (D24/D25)—.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(GeneradorHorarioService.class)
class PresupuestoSolverTest {

    @Autowired
    private GeneradorHorarioService service;

    /**
     * Sin {@code maxSegundos} en el cuerpo, manda la property. Discriminante frente
     * a tres mutaciones: {@code @Value} borrado (daría 0), respaldo del {@code @Value}
     * usado en vez de la clave (daría 600) y ternario invertido (daría 0 también,
     * pero lo separa el caso siguiente).
     */
    @Test
    void sinSolicitado_elPresupuestoSaleDeLaProperty_5sEnTest() {
        assertThat(service.presupuestoSegundos(null)).isEqualTo(5);
    }

    /**
     * Con {@code maxSegundos} en el cuerpo, manda el cuerpo. 42 se elige distinto de
     * 5 y de 600 a propósito: ningún valor por defecto puede colarse como acierto.
     */
    @Test
    void conSolicitado_mandaElSolicitadoYnoLaProperty() {
        assertThat(service.presupuestoSegundos(42)).isEqualTo(42);
    }

    /**
     * La property NO es el único origen: 1 es un valor legítimo y debe pasar tal cual.
     * Junto al anterior fija que no hay pisos ni topes escondidos en la regla; el
     * único filtro sobre el valor es la guarda de {@code <= 0}, que vive en
     * {@link GeneradorHorarioService#generar} y se traduce a 400 en el borde HTTP.
     */
    @Test
    void unSegundoSolicitadoSeRespeta_laReglaNoImponeSuelo() {
        assertThat(service.presupuestoSegundos(1)).isEqualTo(1);
    }
}
