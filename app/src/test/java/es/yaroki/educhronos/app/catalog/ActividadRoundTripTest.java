package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

/**
 * Round-trip de persistencia del agregado Actividad→Plaza (Fase 6, Bloque 5)
 * sobre la SQLite real ({@code replace = NONE}, no H2 en memoria). Cada test
 * persiste, hace {@code flush()+clear()} del EntityManager para forzar la
 * recarga desde BD (no leer del caché de primer nivel) y verifica.
 *
 * <p>Cubre los tres riesgos de persistencia que B5 introduce y que no probaba
 * ningún test previo:
 * <ol>
 *   <li>el cascade {@code @OneToMany} Actividad→Plaza guarda las plazas con un
 *       único save de la actividad;
 *   <li>la densidad de relaciones de Plaza (aula_fija {@code @ManyToOne} + tres
 *       {@code @ManyToMany}: profesores/candidatas/subgrupos) sobrevive el
 *       dialecto de comunidad SQLite;
 *   <li>las DOS ramas del XOR aula_fija/aulasCandidatas se conservan (la entidad
 *       JPA no valida el XOR —D-B5-2, lo valida el record de dominio—, así que
 *       ambas ramas se persisten sin queja y aquí se comprueba que SQLite las
 *       preserva).
 * </ol>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ActividadRoundTripTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ActividadRepository actividadRepository;

    @Autowired
    private NivelRepository nivelRepository;

    @Autowired
    private GrupoAdministrativoRepository grupoRepository;

    @Autowired
    private SubgrupoRepository subgrupoRepository;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private AulaRepository aulaRepository;

    @Autowired
    private AsignaturaRepository asignaturaRepository;

    // ── Riesgo 1: cascade Actividad→Plaza ──────────────────────────────
    @Test
    void elCascadeGuardaLasPlazasConUnSoloSaveDeLaActividad() {
        Asignatura mat = asignaturaRepository.save(new Asignatura("Mat", "Matemáticas"));

        Actividad actividad = new Actividad();
        actividad.setCodigo("ACT-CASCADE");
        // Dos plazas añadidas al agregado; NO se guardan explícitamente: el
        // cascade de la actividad debe persistirlas.
        actividad.getPlazas().add(nuevaPlaza("ACT-CASCADE-P1", actividad, mat));
        actividad.getPlazas().add(nuevaPlaza("ACT-CASCADE-P2", actividad, mat));

        Long id = actividadRepository.save(actividad).getId();

        entityManager.flush();
        entityManager.clear();

        Actividad recuperada = actividadRepository.findById(id).orElseThrow();
        assertThat(recuperada.getPlazas())
                .extracting(Plaza::getCodigo)
                .containsExactlyInAnyOrder("ACT-CASCADE-P1", "ACT-CASCADE-P2");
    }

    // ── Riesgo 2 + Riesgo 3 rama A: densidad de relaciones con aula_fija ──
    @Test
    void plazaConCoDocenciaAulaFijaYSubgrupoSobreviveElRoundTrip() {
        Nivel nivel = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo grupo =
                grupoRepository.save(new GrupoAdministrativo("1ºA", nivel, TipoGrupo.ORDINARIO, null));
        Subgrupo subgrupo = subgrupoRepository.save(new Subgrupo("1ºA-Completo", Set.of(grupo)));
        Profesor prof1 = profesorRepository.save(new Profesor("MAT8", "María Martínez"));
        Profesor prof2 = profesorRepository.save(new Profesor("FIS3", "Luis López"));
        Aula aula = aulaRepository.save(new Aula("LAB1", TipoAula.LAB_CIENCIAS, 30, "B", 1, "Norte"));
        Asignatura asig = asignaturaRepository.save(new Asignatura("ByG", "Biología y Geología"));

        Actividad actividad = new Actividad();
        actividad.setCodigo("ACT-DENS");
        Plaza plaza = nuevaPlaza("ACT-DENS-P1", actividad, asig);
        // Co-docencia: 2 profesores. Rama A del XOR: aula_fija puesta, candidatas vacías.
        plaza.setProfesores(new HashSet<>(Set.of(prof1, prof2)));
        plaza.setAulaFija(aula);
        plaza.setSubgrupos(new HashSet<>(Set.of(subgrupo)));
        actividad.getPlazas().add(plaza);

        Long id = actividadRepository.save(actividad).getId();

        entityManager.flush();
        entityManager.clear();

        Actividad recuperada = actividadRepository.findById(id).orElseThrow();
        assertThat(recuperada.getPlazas()).hasSize(1);
        Plaza plazaLeida = recuperada.getPlazas().get(0);
        assertThat(plazaLeida.getProfesores())
                .extracting(Profesor::getCodigo)
                .containsExactlyInAnyOrder("MAT8", "FIS3");
        assertThat(plazaLeida.getSubgrupos())
                .extracting(Subgrupo::getCodigo)
                .containsExactly("1ºA-Completo");
        assertThat(plazaLeida.getAulaFija()).isNotNull();
        assertThat(plazaLeida.getAulaFija().getCodigo()).isEqualTo("LAB1");
        assertThat(plazaLeida.getAulasCandidatas()).isEmpty();
    }

    // ── Riesgo 3 rama B: sin aula_fija, con aulasCandidatas ──────────────
    @Test
    void plazaSinAulaFijaYConAulasCandidatasSobreviveElRoundTrip() {
        Nivel nivel = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo grupo =
                grupoRepository.save(new GrupoAdministrativo("1ºA", nivel, TipoGrupo.ORDINARIO, null));
        Subgrupo subgrupo = subgrupoRepository.save(new Subgrupo("1ºA-Completo", Set.of(grupo)));
        Profesor prof = profesorRepository.save(new Profesor("TEC1", "Ana Ares"));
        Aula aula1 = aulaRepository.save(new Aula("A5", TipoAula.ORDINARIA, null, null, null, null));
        Aula aula2 = aulaRepository.save(new Aula("A6", TipoAula.ORDINARIA, null, null, null, null));
        Asignatura asig = asignaturaRepository.save(new Asignatura("Tec", "Tecnología"));

        Actividad actividad = new Actividad();
        actividad.setCodigo("ACT-CAND");
        Plaza plaza = nuevaPlaza("ACT-CAND-P1", actividad, asig);
        plaza.setProfesores(new HashSet<>(Set.of(prof)));
        // Rama B del XOR: aula_fija null, dos candidatas.
        plaza.setAulasCandidatas(new HashSet<>(Set.of(aula1, aula2)));
        plaza.setSubgrupos(new HashSet<>(Set.of(subgrupo)));
        actividad.getPlazas().add(plaza);

        Long id = actividadRepository.save(actividad).getId();

        entityManager.flush();
        entityManager.clear();

        Actividad recuperada = actividadRepository.findById(id).orElseThrow();
        Plaza plazaLeida = recuperada.getPlazas().get(0);
        assertThat(plazaLeida.getAulaFija()).isNull();
        assertThat(plazaLeida.getAulasCandidatas())
                .extracting(Aula::getCodigo)
                .containsExactlyInAnyOrder("A5", "A6");
    }

    /**
     * Plaza mínima válida a nivel de BD: codigo + su actividad (lado dueño de la
     * FK, nullable=false) + asignatura obligatoria. El llamador añade las
     * relaciones M:N y el aula según el caso.
     */
    private static Plaza nuevaPlaza(String codigo, Actividad actividad, Asignatura asignatura) {
        Plaza plaza = new Plaza();
        plaza.setCodigo(codigo);
        plaza.setActividad(actividad);
        plaza.setAsignatura(asignatura);
        return plaza;
    }
}
