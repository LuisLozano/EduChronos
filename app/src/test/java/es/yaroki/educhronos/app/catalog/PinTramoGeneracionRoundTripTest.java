package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.Sesion;
import es.yaroki.educhronos.app.service.GeneradorHorarioService;
import jakarta.persistence.EntityManager;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * ORO end-to-end del cableado del servicio a los repos de bloqueo (Bloque
 * 8.2b-iii): comprueba que un solve lanzado por la VÍA REAL
 * ({@link GeneradorHorarioService#generar}) respeta un pin de TRAMO persistido en
 * {@code sesion_bloqueada}. A diferencia de {@code BloqueoPinRoundTripTest} —que
 * ensambla el {@code ProblemaHorario} llamando al mapper con las listas de bloqueo
 * a mano—, aquí NO se toca el mapper: se persiste el pin y se confía en que
 * {@code cargarProblema()} lo lea con {@code sesionBloqueadaRepository.findAll()}
 * dentro de su transacción (la única forma de conservar la identidad de objeto del
 * puente de tramo).
 *
 * <p>VALIDEZ DEL FIXTURE: sin pin, el solver coloca esta instancia (semilla 42) en
 * L1 (comprobado con una sonda); por eso se pina L5, un tramo que el solver NO
 * elegiría por su cuenta. Si se revierte {@code cargarProblema()} a {@code List.of()}
 * el pin se pierde y el test cae en L1 → ROJO (oro negativo). Vive en
 * {@code app.catalog} por el constructor {@code protected} de {@code Actividad}/{@code Plaza},
 * y corre sobre SQLite real ({@code replace = NONE}) para preservar esa identidad.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(GeneradorHorarioService.class)
class PinTramoGeneracionRoundTripTest {

    @Autowired private EntityManager entityManager;
    @Autowired private GeneradorHorarioService service;

    @Autowired private NivelRepository nivelRepository;
    @Autowired private GrupoAdministrativoRepository grupoRepository;
    @Autowired private SubgrupoRepository subgrupoRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private AulaRepository aulaRepository;
    @Autowired private TramoSemanalRepository tramoRepository;
    @Autowired private ActividadRepository actividadRepository;
    @Autowired private SesionBloqueadaRepository pinTramoRepository;

    private static final String ACTIVIDAD = "MAT-1ESO";

    @Test
    void elSolveDeLaViaRealRespetaElPinDeTramoPersistido() {
        TramoSemanal tramoPinado = poblarCatalogoConPin();
        entityManager.flush();
        entityManager.clear();

        // Vía REAL: generar() -> cargarProblema() lee el pin por findAll() en su
        // transacción. maxSegundos=10 explícito (el default de 30 s es veneno para
        // la suite, D24/D25).
        Long id = service.generar(10, 42, null, "test-pin").getId();
        entityManager.flush();
        entityManager.clear();

        HorarioGenerado recargado = service.cargarHorario(id);
        List<Sesion> sesiones = recargado.getSesiones().stream()
                .filter(s -> s.getPlaza().getActividad().getCodigo().equals(ACTIVIDAD))
                .toList();

        assertThat(sesiones).isNotEmpty();
        // TODAS las Sesion de la instancia pinada caen en el tramo pinado.
        assertThat(sesiones).allSatisfy(s ->
                assertThat(s.getTramoInicio().getId()).isEqualTo(tramoPinado.getId()));
    }

    // ------------------------------------------------------------- fixture builder

    /** Persiste un catálogo mínimo de una plaza + el pin de tramo (L5) y lo devuelve. */
    private TramoSemanal poblarCatalogoConPin() {
        Nivel eso1 = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo g = grupoRepository.save(
                new GrupoAdministrativo("1ºA", eso1, TipoGrupo.ORDINARIO, null));
        Subgrupo sg = subgrupoRepository.save(new Subgrupo("1ºA-Comp", Set.of(g)));
        Asignatura mat = asignaturaRepository.save(new Asignatura("MAT", "Matematicas"));
        Profesor prof = profesorRepository.save(new Profesor("MAT1", "Profesor MAT1"));
        Aula a1 = aulaRepository.save(new Aula("A1", TipoAula.ORDINARIA, null, null, null, null));

        // 5 tramos lectivos en lunes; se pina el quinto (L5): el solver, sin pin,
        // colocaría la instancia en L1 (ver nota de validez del fixture).
        TramoSemanal tramoPinado = null;
        for (int orden = 1; orden <= 5; orden++) {
            TramoSemanal t = tramoRepository.save(new TramoSemanal(
                    Dia.LUNES, LocalTime.of(7 + orden, 0), LocalTime.of(8 + orden, 0),
                    true, orden, null));
            if (orden == 5) {
                tramoPinado = t;
            }
        }

        Actividad act = new Actividad();
        act.setCodigo(ACTIVIDAD);
        act.setRepeticionesPorSemana(1);
        act.setDuracionTramos(1);
        act.setPatronTemporal(PatronTemporal.NEUTRA);
        Plaza p = new Plaza();
        p.setCodigo("MAT-1ESO-P1");
        p.setActividad(act);
        p.setAsignatura(mat);
        p.setProfesores(Set.of(prof));
        p.setAulaFija(a1);
        p.setSubgrupos(Set.of(sg));
        act.getPlazas().add(p);
        actividadRepository.save(act);

        pinTramoRepository.save(new SesionBloqueada(act, 1, tramoPinado));

        return tramoPinado;
    }
}
