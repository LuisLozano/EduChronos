package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

/**
 * Round-trip de persistencia del catálogo del centro (Fase 6, Bloque 2).
 *
 * <p>Persiste un puñado de filas de cada entidad de catálogo sobre la SQLite
 * real (no H2: {@code replace = NONE}) y las recupera tras vaciar el contexto
 * de persistencia, verificando que las relaciones se reconstruyen:
 * Grupo→Nivel, Grupo→grupoPadre, Tramo→siguienteInmediato y
 * Asignatura↔tipoAula. Verifica además que {@code LocalTime} sobrevive el
 * viaje a SQLite sin alteración.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CatalogoRoundTripTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired private NivelRepository niveles;
    @Autowired private GrupoAdministrativoRepository grupos;
    @Autowired private ProfesorRepository profesores;
    @Autowired private AulaRepository aulas;
    @Autowired private AsignaturaRepository asignaturas;
    @Autowired private AsignaturaAulaCompatibleRepository compatibilidades;
    @Autowired private TramoSemanalRepository tramos;
    @Autowired private ConfiguracionRepository configuraciones;

    @Test
    void persisteYRecuperaElCatalogoConSusRelaciones() {
        // --- Nivel + grupo ordinario + grupo PDC con grupo padre (I5) ---
        Nivel eso1 = niveles.save(new Nivel("1ESO", 1));
        GrupoAdministrativo ordinario =
                grupos.save(new GrupoAdministrativo("1ºA", eso1, TipoGrupo.ORDINARIO, null));
        GrupoAdministrativo pdc =
                grupos.save(new GrupoAdministrativo("3ºADi", eso1, TipoGrupo.DIVERSIFICACION_PDC, ordinario));

        // --- Profesor / Aula con todos sus campos ---
        profesores.save(new Profesor("MAT8", "María Martínez"));
        Aula lab = aulas.save(new Aula("LAB1", TipoAula.LAB_CIENCIAS, 30, "B", 1, "Norte"));

        // --- Asignatura + compatibilidad con tipo de aula ---
        Asignatura byg = asignaturas.save(new Asignatura("ByG", "Biología y Geología"));
        compatibilidades.save(new AsignaturaAulaCompatible(byg, TipoAula.LAB_CIENCIAS));

        // --- Dos tramos encadenados (siguienteInmediato) ---
        TramoSemanal segundo =
                tramos.save(new TramoSemanal(Dia.LUNES, LocalTime.of(9, 0), LocalTime.of(9, 55), true, 2, null));
        TramoSemanal primero =
                tramos.save(new TramoSemanal(Dia.LUNES, LocalTime.of(8, 0), LocalTime.of(8, 55), true, 1, segundo));

        // --- Configuración clave-valor ---
        configuraciones.save(new Configuracion("w_planta", "5"));

        // Fuerza el INSERT y limpia el contexto: las lecturas vienen de la BD.
        entityManager.flush();
        entityManager.clear();

        // --- Verificación: Grupo→Nivel y Grupo→grupoPadre ---
        GrupoAdministrativo pdcLeido = grupos.findByCodigo("3ºADi").orElseThrow();
        assertThat(pdcLeido.getNivel().getCodigo()).isEqualTo("1ESO");
        assertThat(pdcLeido.getTipo()).isEqualTo(TipoGrupo.DIVERSIFICACION_PDC);
        assertThat(pdcLeido.getGrupoPadre()).isNotNull();
        assertThat(pdcLeido.getGrupoPadre().getCodigo()).isEqualTo("1ºA");
        assertThat(grupos.findByCodigo("1ºA").orElseThrow().getGrupoPadre()).isNull();

        // --- Verificación: Aula con todos los campos ---
        Aula labLeida = aulas.findByCodigo("LAB1").orElseThrow();
        assertThat(labLeida.getTipo()).isEqualTo(TipoAula.LAB_CIENCIAS);
        assertThat(labLeida.getCapacidad()).isEqualTo(30);
        assertThat(labLeida.getEdificio()).isEqualTo("B");
        assertThat(labLeida.getPlanta()).isEqualTo(1);
        assertThat(labLeida.getSector()).isEqualTo("Norte");

        // --- Verificación: Asignatura↔tipoAula ---
        Asignatura bygLeida = asignaturas.findByCodigo("ByG").orElseThrow();
        assertThat(compatibilidades.findByAsignatura(bygLeida))
                .singleElement()
                .extracting(AsignaturaAulaCompatible::getTipoAula)
                .isEqualTo(TipoAula.LAB_CIENCIAS);

        // --- Verificación: Tramo→siguienteInmediato y LocalTime intacto ---
        TramoSemanal primeroLeido = tramos.findById(primero.getId()).orElseThrow();
        assertThat(primeroLeido.getHoraInicio()).isEqualTo(LocalTime.of(8, 0));
        assertThat(primeroLeido.getHoraFin()).isEqualTo(LocalTime.of(8, 55));
        assertThat(primeroLeido.isEsLectivo()).isTrue();
        assertThat(primeroLeido.getSiguienteInmediato()).isNotNull();
        assertThat(primeroLeido.getSiguienteInmediato().getHoraInicio()).isEqualTo(LocalTime.of(9, 0));

        // --- Verificación: Configuración clave-valor ---
        assertThat(configuraciones.findById("w_planta").orElseThrow().getValor()).isEqualTo("5");
    }
}
