package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.yaroki.educhronos.app.service.AulaService;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException.Referencia;
import es.yaroki.educhronos.app.web.AulaController;
import java.util.Set;
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
 * Test de integración del CRUD {@code /api/aulas} (Fase 8, Bloque 8.5-A', réplica
 * del piloto {@code AsignaturaEndpointTest}). Ejerce alta/consulta/listado/edición/
 * borrado POR LA RED ({@code standaloneSetup} + {@code AulaService} real sobre
 * {@code @DataJpaTest}), con asertos discriminantes. Además del par crítico de
 * unicidad-en-edición, blinda las decisiones propias del aula:
 * <ul>
 *   <li>D-3: {@link #alta_tipoInvalido_400ConValorEnMensaje} — el 400 NOMBRA el valor
 *       inválido;
 *   <li>D-4: {@link #alta_sinLosCuatroNullable_201YLosDevuelveNull} — los cuatro
 *       campos opcionales llegan/persisten/viajan null;
 *   <li>{@link #alta_seisCamposPoblados_201YRoundTrip} — round-trip del enum (como
 *       String, {@code TipoAula.name()}) y de los Integer.
 * </ul>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AulaService.class)
class AulaEndpointTest {

    /** Cuántas plazas usan el aula del test del 409. Cambiarlo debe cambiar el conteo del 409. */
    private static final int PLAZAS_MONTADAS = 2;

    /**
     * Aulas de relleno que {@link #desalinearIdsDeAula} siembra para que el aula del test reciba
     * un id ALTO, distinto de los ids bajos de asignatura/actividad/plaza. Con margen holgado
     * sobre {@link #PLAZAS_MONTADAS} para que ninguna plaza alcance ese id.
     */
    private static final int AULAS_RELLENO = 10;

    @Autowired private AulaService service;
    @Autowired private AulaRepository aulaRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private ActividadRepository actividadRepository;
    @Autowired private TestEntityManager entityManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AulaController(service)).build();
    }

    @Test
    void alta_creaYDevuelve201ConId() throws Exception {
        mockMvc.perform(post("/api/aulas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodySoloTipo("A1", "ORDINARIA")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.codigo").value("A1"))
                .andExpect(jsonPath("$.tipo").value("ORDINARIA"));
    }

    @Test
    void getPorId_devuelveElAula() throws Exception {
        long id = crear(bodySoloTipo("LAB1", "LAB_CIENCIAS"));

        mockMvc.perform(get("/api/aulas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.codigo").value("LAB1"))
                .andExpect(jsonPath("$.tipo").value("LAB_CIENCIAS"));
    }

    @Test
    void getPorId_inexistente_404() throws Exception {
        mockMvc.perform(get("/api/aulas/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listar_devuelveOrdenEstablePorCodigo() throws Exception {
        crear(bodySoloTipo("B", "ORDINARIA"));
        crear(bodySoloTipo("A", "ORDINARIA"));
        crear(bodySoloTipo("C", "ORDINARIA"));

        mockMvc.perform(get("/api/aulas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].codigo").value("A"))
                .andExpect(jsonPath("$[1].codigo").value("B"))
                .andExpect(jsonPath("$[2].codigo").value("C"));
    }

    @Test
    void edicion_cambiaTipoManteniendoCodigo_200() throws Exception {
        long id = crear(bodySoloTipo("A1", "ORDINARIA"));

        mockMvc.perform(put("/api/aulas/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodySoloTipo("A1", "INFORMATICA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.codigo").value("A1"))
                .andExpect(jsonPath("$.tipo").value("INFORMATICA"));
    }

    @Test
    void edicion_inexistente_404() throws Exception {
        mockMvc.perform(put("/api/aulas/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodySoloTipo("A1", "ORDINARIA")))
                .andExpect(status().isNotFound());
    }

    @Test
    void alta_codigoDuplicado_400() throws Exception {
        crear(bodySoloTipo("A1", "ORDINARIA"));

        mockMvc.perform(post("/api/aulas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodySoloTipo("A1", "INFORMATICA")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edicion_codigoQuePisaAOtra_400() throws Exception {
        crear(bodySoloTipo("A", "ORDINARIA"));
        long idB = crear(bodySoloTipo("B", "ORDINARIA"));

        // PUT sobre B pidiendo el código de A → colisión con OTRA entidad → 400.
        mockMvc.perform(put("/api/aulas/" + idB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodySoloTipo("A", "ORDINARIA")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edicion_guardaMismoCodigo_200() throws Exception {
        long id = crear(bodySoloTipo("A", "ORDINARIA"));

        // PUT sobre A con su MISMO código: la unicidad debe excluirse a sí misma → 200,
        // NO 400. Es el test que un findByCodigo ingenuo (sin comparar id) rompe.
        mockMvc.perform(put("/api/aulas/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodySoloTipo("A", "GIMNASIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("A"))
                .andExpect(jsonPath("$.tipo").value("GIMNASIO"));
    }

    @Test
    void alta_tipoInvalido_400ConValorEnMensaje() throws Exception {
        // D-3: un tipo que no existe en TipoAula → 400 cuyo mensaje NOMBRA el valor malo.
        mockMvc.perform(post("/api/aulas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodySoloTipo("A1", "CHUCHE")))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("CHUCHE")));
    }

    @Test
    void alta_tipoEnBlanco_400() throws Exception {
        mockMvc.perform(post("/api/aulas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodySoloTipo("A1", "   ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void alta_codigoEnBlanco_400() throws Exception {
        mockMvc.perform(post("/api/aulas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodySoloTipo("   ", "ORDINARIA")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void alta_sinLosCuatroNullable_201YLosDevuelveNull() throws Exception {
        // D-4: capacidad/edificio/planta/sector AUSENTES en el body → 201, y el GET
        // posterior los devuelve como null explícito (contrato de serialización de 7A).
        long id = crear(bodySoloTipo("A1", "ORDINARIA"));

        mockMvc.perform(get("/api/aulas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("ORDINARIA"))
                .andExpect(jsonPath("$.capacidad").value(nullValue()))
                .andExpect(jsonPath("$.edificio").value(nullValue()))
                .andExpect(jsonPath("$.planta").value(nullValue()))
                .andExpect(jsonPath("$.sector").value(nullValue()));
    }

    @Test
    void alta_seisCamposPoblados_201YRoundTrip() throws Exception {
        // Round-trip completo: el enum viaja como String (TipoAula.name()) y los dos
        // Integer conservan su valor a través del alta y del GET posterior.
        long id = crear(bodyCompleto("LAB1", "LAB_CIENCIAS", 30, "A", 1, "Norte"));

        mockMvc.perform(get("/api/aulas/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("LAB1"))
                .andExpect(jsonPath("$.tipo").value("LAB_CIENCIAS"))
                .andExpect(jsonPath("$.capacidad").value(30))
                .andExpect(jsonPath("$.edificio").value("A"))
                .andExpect(jsonPath("$.planta").value(1))
                .andExpect(jsonPath("$.sector").value("Norte"));
    }

    @Test
    void borrado_204_yLuego404() throws Exception {
        long id = crear(bodySoloTipo("A1", "ORDINARIA"));

        mockMvc.perform(delete("/api/aulas/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/aulas/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void borrado_inexistente_404() throws Exception {
        mockMvc.perform(delete("/api/aulas/9999"))
                .andExpect(status().isNotFound());
    }

    /**
     * PILOTO del borrado amable (8.5-C2b): un aula que {@code PLAZAS_MONTADAS} plazas usan como
     * aula fija NO se borra; el 409 nombra al referente con su conteo REAL.
     *
     * <p><b>Este test es la única red del {@code @Query} nativo</b> {@code plaza.aula_fija_id}:
     * al ser SQL en un String, ningún compilador comprueba que apunte a la columna correcta. Por
     * eso el aserto no mira el status: compara el DESGLOSE estructurado
     * ({@link ReferenciaEntranteException#getReferencias()}) contra el número de plazas que el
     * fixture montó de verdad —montar 3 tendría que reportar 3—. El {@code containsExactly}
     * añade el otro lado del discriminante: exige que las otras TRES FK entrantes del aula
     * (candidatas, aula_bloqueada, sesion) NO se reporten, porque ninguna tiene filas. Si alguna
     * consulta apuntase a la tabla o columna equivocada, sobraría o faltaría un referente aquí.
     */
    @Test
    void borrado_aulaFijaDeDosPlazas_409YDesgloseConElConteoReal() throws Exception {
        desalinearIdsDeAula();
        long id = crear(bodySoloTipo("A1", "ORDINARIA"));
        int plazasMontadas = montarPlazasConAulaFija(id, PLAZAS_MONTADAS);

        // (a) Contrato HTTP: el conflicto viaja como 409, no como el 500 del mordisco de la FK.
        mockMvc.perform(delete("/api/aulas/" + id))
                .andExpect(status().isConflict());

        // (b) Desglose: un solo referente, "plaza(s)", con el conteo REAL del fixture.
        ReferenciaEntranteException error = catchThrowableOfType(
                () -> service.borrar(id), ReferenciaEntranteException.class);
        assertThat(error).isNotNull();
        assertThat(error.getReferencias())
                .containsExactly(new Referencia("plaza(s)", plazasMontadas));
    }

    /**
     * DESALINEA el id del aula del test respecto de todos los ids del fixture, sembrando
     * {@code AULAS_RELLENO} aulas antes de crear la del test: así esta recibe un id ALTO
     * (≈ {@code AULAS_RELLENO}+1) que no coincide con ningún id de plaza (1..PLAZAS_MONTADAS),
     * asignatura (1), actividad (1) ni sesion (0) montados después.
     *
     * <p><b>Por qué es imprescindible</b> (D-C2b-5). El id del aula es el parámetro común a las
     * CUATRO {@code @Query} del mapa inverso. Si arrancara en 1 —como arrancan también la primera
     * asignatura, la primera actividad y la primera plaza—, una consulta que por error apuntara a
     * {@code asignatura_id}, {@code actividad_id} o al {@code id} de plaza contaría LAS MISMAS
     * filas que la correcta ({@code aula_fija_id}) y el test pasaría en verde MINTIENDO. Con el id
     * desalineado, una columna equivocada cuenta 0 y el test cae: ver la verificación por mutación
     * en el javadoc de {@link #borrado_aulaFijaDeDosPlazas_409YDesgloseConElConteoReal}.
     */
    private void desalinearIdsDeAula() {
        for (int i = 1; i <= AULAS_RELLENO; i++) {
            aulaRepository.save(new Aula("RELLENO-" + i, TipoAula.ORDINARIA, null, null, null, null));
        }
    }

    /**
     * Monta {@code cuantas} plazas con {@code aulaFija} = el aula dada y devuelve cuántas montó
     * (el aserto usa ESE número, no un literal). Las plazas se crean desde su raíz de agregado
     * con {@link Actividad#agregarPlaza} y se persisten por cascade: {@code Plaza} no tiene
     * repositorio propio (D-C1-A) y su ctor es de paquete, accesible desde este test como en
     * {@code BloqueoMapperPinAulaHuerfanoTest}.
     *
     * <p>El {@code flush} final NO es ceremonia: las consultas del mapa inverso son nativas, e
     * Hibernate no sincroniza el contexto de persistencia antes de un SQL que no sabe interpretar.
     * Sin él las plazas seguirían en memoria y el conteo daría 0 dentro de esta transacción.
     */
    private int montarPlazasConAulaFija(long idAula, int cuantas) {
        Aula aula = aulaRepository.findById(idAula).orElseThrow();
        Asignatura asignatura = asignaturaRepository.save(new Asignatura("MAT", "Matematicas"));
        Profesor profesor = profesorRepository.save(new Profesor("MAT1", "Profesor MAT1"));
        Actividad actividad = new Actividad("ACT", asignatura, 1, 1, PatronTemporal.NEUTRA, false);
        for (int i = 1; i <= cuantas; i++) {
            actividad.agregarPlaza("ACT-P" + i, asignatura, aula,
                    Set.of(profesor), Set.of(), Set.of());
        }
        actividadRepository.save(actividad);
        entityManager.flush();
        return cuantas;
    }

    /** Da de alta por la red con el body dado y devuelve el id sintético asignado. */
    private long crear(String body) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/aulas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    /** {@code {"codigo":..,"tipo":..}} — los cuatro campos nullable AUSENTES (D-4). */
    private static String bodySoloTipo(String codigo, String tipo) {
        return "{\"codigo\":\"" + codigo + "\",\"tipo\":\"" + tipo + "\"}";
    }

    /** {@code {"codigo":..,"tipo":..,"capacidad":..,"edificio":..,"planta":..,"sector":..}} */
    private static String bodyCompleto(String codigo, String tipo, int capacidad,
                                       String edificio, int planta, String sector) {
        return "{\"codigo\":\"" + codigo + "\",\"tipo\":\"" + tipo + "\""
                + ",\"capacidad\":" + capacidad
                + ",\"edificio\":\"" + edificio + "\""
                + ",\"planta\":" + planta
                + ",\"sector\":\"" + sector + "\"}";
    }
}
