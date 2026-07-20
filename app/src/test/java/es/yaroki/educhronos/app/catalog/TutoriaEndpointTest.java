package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.yaroki.educhronos.app.service.GrupoService;
import es.yaroki.educhronos.app.service.PdcService;
import es.yaroki.educhronos.app.service.ProfesorService;
import es.yaroki.educhronos.app.service.RestriccionHorariaService;
import es.yaroki.educhronos.app.service.TutoriaService;
import es.yaroki.educhronos.app.web.GrupoController;
import es.yaroki.educhronos.app.web.PdcController;
import es.yaroki.educhronos.app.web.ProfesorController;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test de integración del sub-recurso TUTORÍA {@code /api/grupos/{id}/tutoria} (Fase 8,
 * Bloque 8.5-D2a). Ejerce consulta y reemplazo total POR LA RED ({@code standaloneSetup} +
 * servicios reales sobre {@code @DataJpaTest}), calcando {@code PdcEndpointTest}, con
 * asertos DISCRIMINANTES: cada uno cae si se retira la regla que protege.
 *
 * <p>Da de alta en {@link #setUp} un nivel, dos grupos ordinarios ("3A", "3B") y tres
 * profesores ("MAT8", "LEN2", "FIS1").
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({GrupoService.class, TutoriaService.class, PdcService.class, ProfesorService.class,
        RestriccionHorariaService.class})
class TutoriaEndpointTest {

    @Autowired private GrupoService grupoService;
    @Autowired private TutoriaService tutoriaService;
    @Autowired private PdcService pdcService;
    @Autowired private ProfesorService profesorService;
    @Autowired private RestriccionHorariaService restriccionService;
    @Autowired private NivelRepository nivelRepository;
    @Autowired private GrupoAdministrativoRepository grupoRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private ProfesorTutoriaRepository tutoriaRepository;
    @Autowired private TestEntityManager entityManager;

    private MockMvc mockMvc;
    private long grupoAId;
    private long grupoBId;
    private long profesorMatId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new GrupoController(grupoService, tutoriaService),
                        new PdcController(pdcService),
                        new ProfesorController(profesorService, restriccionService))
                .build();
        Nivel nivel = nivelRepository.save(new Nivel("1ESO", 1));
        grupoAId = grupoRepository.save(
                new GrupoAdministrativo("3A", nivel, TipoGrupo.ORDINARIO, null)).getId();
        grupoBId = grupoRepository.save(
                new GrupoAdministrativo("3B", nivel, TipoGrupo.ORDINARIO, null)).getId();
        profesorMatId = profesorRepository.save(new Profesor("MAT8", "Ada Lovelace")).getId();
        profesorRepository.save(new Profesor("LEN2", "Rosalia de Castro"));
        profesorRepository.save(new Profesor("FIS1", "Lise Meitner"));
    }

    // ------------------------------------------------------------------ camino feliz

    @Test
    void getSinTutoria_devuelveListaVacia_200() throws Exception {
        mockMvc.perform(get("/api/grupos/" + grupoAId + "/tutoria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void put_creaTutoriaYdevuelveLaListaOrdenada_principalPrimero() throws Exception {
        mockMvc.perform(put("/api/grupos/" + grupoAId + "/tutoria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + cuerpo("LEN2", "CO_TUTOR") + ","
                                + cuerpo("MAT8", "TUTOR_PRINCIPAL") + "]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // Orden por rol (natural del enum): el principal SIEMPRE primero.
                .andExpect(jsonPath("$[0].profesor").value("MAT8"))
                .andExpect(jsonPath("$[0].rol").value("TUTOR_PRINCIPAL"))
                .andExpect(jsonPath("$[1].profesor").value("LEN2"))
                .andExpect(jsonPath("$[1].rol").value("CO_TUTOR"));
    }

    @Test
    void putIdempotente_elMismoCuerpoDosVeces_mismoResultado() throws Exception {
        String cuerpo = "[" + cuerpo("MAT8", "TUTOR_PRINCIPAL") + ","
                + cuerpo("LEN2", "CO_TUTOR") + "]";
        ponerTutoria(grupoAId, cuerpo);

        // Segunda vez: el deleteAll + flush ANTES de insertar evita el choque con la PK.
        mockMvc.perform(put("/api/grupos/" + grupoAId + "/tutoria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        entityManager.flush();
        entityManager.clear();
        assertThat(tutoriaRepository.findByGrupo_Id(grupoAId)).hasSize(2);
    }

    @Test
    void putConListaVacia_borraLaTutoria() throws Exception {
        ponerTutoria(grupoAId, "[" + cuerpo("MAT8", "TUTOR_PRINCIPAL") + "]");

        mockMvc.perform(put("/api/grupos/" + grupoAId + "/tutoria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        entityManager.flush();
        entityManager.clear();
        assertThat(tutoriaRepository.findByGrupo_Id(grupoAId)).isEmpty();
    }

    // ------------------------------------------------------- validaciones y su ORDEN

    @Test
    void put_grupoInexistente_404() throws Exception {
        mockMvc.perform(put("/api/grupos/9999/tutoria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + cuerpo("MAT8", "TUTOR_PRINCIPAL") + "]"))
                .andExpect(status().isNotFound());
    }

    @Test
    void put_profesorInexistente_404ConCodigoEnMensaje() throws Exception {
        mockMvc.perform(put("/api/grupos/" + grupoAId + "/tutoria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + cuerpo("NO_EXISTE", "TUTOR_PRINCIPAL") + "]"))
                .andExpect(status().isNotFound())
                .andExpect(status().reason(containsString("NO_EXISTE")));
    }

    @Test
    void put_rolInvalido_400QueNombraElValorYlistaLosValidos() throws Exception {
        mockMvc.perform(put("/api/grupos/" + grupoAId + "/tutoria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + cuerpo("MAT8", "JEFE_DE_ESTUDIOS") + "]"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("JEFE_DE_ESTUDIOS")))
                .andExpect(status().reason(containsString("TUTOR_PRINCIPAL")));
    }

    @Test
    void put_profesorRepetido_400QueLoNombra() throws Exception {
        mockMvc.perform(put("/api/grupos/" + grupoAId + "/tutoria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + cuerpo("MAT8", "TUTOR_PRINCIPAL") + ","
                                + cuerpo("MAT8", "CO_TUTOR") + "]"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("MAT8")));
    }

    /**
     * El ORDEN de validación importa: con un profesor inexistente Y un rol inválido en la
     * MISMA petición gana el 404, porque la resolución del profesor va antes que el parseo
     * del rol. Si las dos pasadas se fundieran en un bucle por elemento, este cuerpo daría
     * 400 (el rol malo del primer elemento se evaluaría antes que el profesor malo del
     * segundo) y el test caería.
     */
    @Test
    void put_profesorInexistenteYrolInvalido_gana404() throws Exception {
        mockMvc.perform(put("/api/grupos/" + grupoAId + "/tutoria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + cuerpo("MAT8", "ROL_MALO") + ","
                                + cuerpo("NO_EXISTE", "CO_TUTOR") + "]"))
                .andExpect(status().isNotFound());
    }

    // ============================ ASERTOS DISCRIMINANTES EXIGIDOS ============================

    /**
     * (A1) I4: dos {@code TUTOR_PRINCIPAL} en el MISMO PUT → 400 cuyo mensaje NOMBRA A LOS
     * DOS profesores. Nombrar solo a uno no diría cuál sobra. Si se retira la guarda de I4
     * la escritura pasa y la base queda violando la invariante: ver la mutación registrada
     * en la crónica del bloque (no hay red de esquema debajo, la PK compuesta no lo impide).
     */
    @Test
    void a1_dosTutoresPrincipales_400QueNombraAlosDos() throws Exception {
        mockMvc.perform(put("/api/grupos/" + grupoAId + "/tutoria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + cuerpo("MAT8", "TUTOR_PRINCIPAL") + ","
                                + cuerpo("LEN2", "TUTOR_PRINCIPAL") + "]"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("MAT8")))
                .andExpect(status().reason(containsString("LEN2")));

        // Y no ha escrito NADA: I4 se valida antes de tocar la base.
        entityManager.flush();
        entityManager.clear();
        assertThat(tutoriaRepository.findByGrupo_Id(grupoAId)).isEmpty();
    }

    /**
     * (A2) El MISMO profesor es {@code TUTOR_PRINCIPAL} de DOS grupos distintos → ambos PUT
     * 200. Discrimina I4 ("un principal POR GRUPO") de una regla más fuerte que NADIE pidió
     * ("un profesor tutoriza como mucho un grupo"). Si alguien implementara la unicidad
     * global del tutor, este test caería y el A1 seguiría verde.
     */
    @Test
    void a2_mismoProfesorPrincipalEnDosGrupos_ambos200() throws Exception {
        String cuerpo = "[" + cuerpo("MAT8", "TUTOR_PRINCIPAL") + "]";

        mockMvc.perform(put("/api/grupos/" + grupoAId + "/tutoria")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/grupos/" + grupoBId + "/tutoria")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();
        assertThat(tutoriaRepository.findByGrupo_Id(grupoAId)).hasSize(1);
        assertThat(tutoriaRepository.findByGrupo_Id(grupoBId)).hasSize(1);
    }

    /**
     * (A3 + A4) Herencia del tutor principal en el alta del PDC. Padre CON tutor → el PDC
     * nace con ESE profesor como principal; padre SIN tutor → 201 y tutoría VACÍA (no es un
     * error). AVISO DE FRAMEWORK (A4): se desligan las entidades ({@code flush + clear})
     * antes de comprobar, para leer la BASE y no la caché L1 de Hibernate — sin el clear el
     * aserto pasaría aunque el INSERT no hubiera llegado a la base.
     */
    @Test
    void a3a4_pdcDePadreConTutor_heredaElPrincipal_leidoDeLaBase() throws Exception {
        ponerTutoria(grupoAId, "[" + cuerpo("MAT8", "TUTOR_PRINCIPAL") + ","
                + cuerpo("LEN2", "CO_TUTOR") + "]");
        long pdcId = crearPdc(grupoAId, "3ADI");

        entityManager.flush();
        entityManager.clear();

        List<ProfesorTutoria> delPdc = tutoriaRepository.findByGrupo_Id(pdcId);
        // Hereda el principal y SOLO el principal: el co-tutor LEN2 no se arrastra.
        assertThat(delPdc).hasSize(1);
        assertThat(delPdc.get(0).getProfesor().getCodigo()).isEqualTo("MAT8");
        assertThat(delPdc.get(0).getRol()).isEqualTo(RolTutoria.TUTOR_PRINCIPAL);
    }

    @Test
    void a3_pdcDePadreSinTutor_201YtutoriaVacia() throws Exception {
        long pdcId = crearPdc(grupoBId, "3BDI");

        entityManager.flush();
        entityManager.clear();

        assertThat(tutoriaRepository.findByGrupo_Id(pdcId)).isEmpty();
        // Y el GET del sub-recurso lo confirma por la red: 200 con lista vacía, no 404.
        mockMvc.perform(get("/api/grupos/" + pdcId + "/tutoria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    /**
     * (A7) Borrar un profesor que es tutor → 409, por la guarda de referencia entrante de
     * {@code ProfesorService.borrar}. La FK {@code profesor_tutoria.profesor_id} es RESTRICT
     * (no cascadea) a propósito: un profesor tutor no desaparece en silencio.
     */
    @Test
    void a7_borrarProfesorQueEsTutor_409() throws Exception {
        ponerTutoria(grupoAId, "[" + cuerpo("MAT8", "TUTOR_PRINCIPAL") + "]");
        entityManager.flush();

        mockMvc.perform(delete("/api/profesores/" + profesorMatId))
                .andExpect(status().isConflict())
                .andExpect(status().reason(containsString("tutoria(s)")));
    }

    /**
     * (A8) Borrar un GRUPO con tutoría → 204, y la fila desaparece de la BASE. NO es 409: la
     * tutoría es población PROPIA del grupo (FK {@code grupo_id} con {@code ON DELETE
     * CASCADE}), no una referencia entrante que deba vetar el borrado — mismo criterio que
     * S75 con las compatibilidades de aula, y por eso {@code GrupoService.borrar} NO se tocó.
     * Se relee de la BASE ({@code flush + clear}): la caché L1 seguiría sirviendo la fila.
     */
    @Test
    void a8_borrarGrupoConTutoria_204YlaFilaDesapareceDeLaBase() throws Exception {
        ponerTutoria(grupoAId, "[" + cuerpo("MAT8", "TUTOR_PRINCIPAL") + "]");
        // flush + clear ANTES del borrado, no solo después: en @DataJpaTest todo comparte UNA
        // transacción, así que la ProfesorTutoria recién escrita seguiría GESTIONADA y su flush
        // reventaría con TransientPropertyValueException al quedar su grupo borrado. En
        // producción el DELETE llega en su propia transacción y nadie ha cargado la tutoría:
        // el clear() es lo que hace que el test modele esa situación y no una artificial.
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/api/grupos/" + grupoAId))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();
        assertThat(tutoriaRepository.findByGrupo_Id(grupoAId)).isEmpty();
        // El profesor SIGUE vivo: la cascada se lleva la tutoría, no al tutor.
        assertThat(profesorRepository.findByCodigo("MAT8")).isPresent();
    }

    /**
     * (A9) Borrar un PDC con tutoría HEREDADA → la tutoría del PDC desaparece (misma cascada
     * de A8, ejercida por la vía compuesta de {@code PdcService.borrar}), y la del PADRE
     * sobrevive intacta: borrar el hijo no toca la tutoría del ordinario.
     */
    @Test
    void a9_borrarPdcConTutoriaHeredada_desapareceLaSuyaYsobreviveLaDelPadre() throws Exception {
        ponerTutoria(grupoAId, "[" + cuerpo("MAT8", "TUTOR_PRINCIPAL") + "]");
        long pdcId = crearPdc(grupoAId, "3ADI");
        // Ver A8: clear() antes del borrado para no arrastrar la tutoría heredada gestionada.
        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/api/grupos/" + grupoAId + "/pdc"))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();
        assertThat(tutoriaRepository.findByGrupo_Id(pdcId)).isEmpty();
        assertThat(tutoriaRepository.findByGrupo_Id(grupoAId)).hasSize(1);
    }

    // ------------------------------------------------------------------------- helpers

    /** Pone la tutoría de un grupo por la red y exige 200. */
    private void ponerTutoria(long idGrupo, String cuerpoJson) throws Exception {
        mockMvc.perform(put("/api/grupos/" + idGrupo + "/tutoria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoJson))
                .andExpect(status().isOk());
    }

    /** Da de alta un PDC por la red bajo el padre indicado y devuelve su id. */
    private long crearPdc(long idPadre, String codigo) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/grupos/" + idPadre + "/pdc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + codigo + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    /** {@code {"profesor":"..","rol":".."}} */
    private static String cuerpo(String profesor, String rol) {
        return "{\"profesor\":\"" + profesor + "\",\"rol\":\"" + rol + "\"}";
    }
}
