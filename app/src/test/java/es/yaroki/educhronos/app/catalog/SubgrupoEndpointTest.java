package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import es.yaroki.educhronos.app.service.PdcService;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException.Referencia;
import es.yaroki.educhronos.app.service.SubgrupoService;
import es.yaroki.educhronos.app.web.SubgrupoController;
import es.yaroki.educhronos.app.web.dto.PdcRequest;
import java.util.List;
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
 * Test de integración del CRUD {@code /api/subgrupos} (Fase 8, Bloque 8.5-B, réplica
 * del piloto {@code AulaEndpointTest}). Ejerce alta/consulta/listado/edición/borrado
 * POR LA RED ({@code standaloneSetup} + {@code SubgrupoService} real sobre
 * {@code @DataJpaTest}), con asertos discriminantes sobre la relación N:M con grupos:
 * <ul>
 *   <li>D-nueva-5: {@link #alta_conDosGrupos_201YRoundTripDeCodigos} — round-trip POR
 *       CONTENIDO ({@code containsInAnyOrder}), no por tamaño;
 *   <li>reemplazo total: {@link #edicion_reemplazaGruposTotal_200} — el PUT SUSTITUYE
 *       el set, no lo une;
 *   <li>D-nueva-3 (borrado fuerte): {@link #borrado_limpiaJoinTableYNoBorraGrupos} —
 *       el cascade limpia {@code subgrupo_grupo} (0 filas) y NO borra los grupos;
 *   <li>guarda G2: {@link #edicionDelMonoDiDeUnPdc_400ConCodigoEnMensaje} y sus tres
 *       compañeros — el subgrupo mono-Di pertenece al agregado PDC, y SOLO él.
 * </ul>
 * Da de alta grupos de apoyo ("G_A", "G_B", "G_C") sobre un nivel en {@link #setUp}.
 *
 * <p><b>Importa {@link PdcService}</b> desde la guarda G2: los PDC y sus subgrupos mono-Di
 * son fixture aquí, no sujeto, y se montan por el alta compuesta real en vez de a mano —un
 * {@code new Subgrupo(...)} fabricado podría diferir de lo que el sub-recurso produce y
 * dejar la guarda probada contra una imitación—.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SubgrupoService.class, PdcService.class})
class SubgrupoEndpointTest {

    @Autowired private SubgrupoService service;
    @Autowired private PdcService pdcService;
    @Autowired private NivelRepository nivelRepository;
    @Autowired private GrupoAdministrativoRepository grupoRepository;
    @Autowired private SubgrupoRepository subgrupoRepository;
    @Autowired private AsignaturaRepository asignaturaRepository;
    @Autowired private AulaRepository aulaRepository;
    @Autowired private ActividadRepository actividadRepository;
    @Autowired private TestEntityManager entityManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SubgrupoController(service)).build();
        // Grupos de apoyo (ordinarios): el subgrupo los referencia por CÓDIGO.
        Nivel nivel = nivelRepository.save(new Nivel("1ESO", 1));
        grupoRepository.save(new GrupoAdministrativo("G_A", nivel, TipoGrupo.ORDINARIO, null));
        grupoRepository.save(new GrupoAdministrativo("G_B", nivel, TipoGrupo.ORDINARIO, null));
        grupoRepository.save(new GrupoAdministrativo("G_C", nivel, TipoGrupo.ORDINARIO, null));
    }

    @Test
    void alta_creaYDevuelve201ConId() throws Exception {
        mockMvc.perform(post("/api/subgrupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("SG1", "G_A")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.codigo").value("SG1"));
    }

    @Test
    void getPorId_devuelveElSubgrupo() throws Exception {
        long id = crear(body("SG1", "G_A"));

        mockMvc.perform(get("/api/subgrupos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.codigo").value("SG1"))
                .andExpect(jsonPath("$.grupos", containsInAnyOrder("G_A")));
    }

    @Test
    void getPorId_inexistente_404() throws Exception {
        mockMvc.perform(get("/api/subgrupos/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listar_devuelveOrdenEstablePorCodigo() throws Exception {
        crear(body("B", "G_A"));
        crear(body("A", "G_A"));
        crear(body("C", "G_A"));

        mockMvc.perform(get("/api/subgrupos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].codigo").value("A"))
                .andExpect(jsonPath("$[1].codigo").value("B"))
                .andExpect(jsonPath("$[2].codigo").value("C"));
    }

    @Test
    void edicion_cambiaCodigoManteniendoGrupos_200() throws Exception {
        long id = crear(body("SG1", "G_A"));

        mockMvc.perform(put("/api/subgrupos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("SG1_BIS", "G_A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.codigo").value("SG1_BIS"))
                .andExpect(jsonPath("$.grupos", containsInAnyOrder("G_A")));
    }

    @Test
    void edicion_inexistente_404() throws Exception {
        mockMvc.perform(put("/api/subgrupos/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("SG1", "G_A")))
                .andExpect(status().isNotFound());
    }

    @Test
    void alta_codigoDuplicado_400() throws Exception {
        crear(body("SG1", "G_A"));

        mockMvc.perform(post("/api/subgrupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("SG1", "G_B")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edicion_codigoQuePisaAOtro_400() throws Exception {
        crear(body("A", "G_A"));
        long idB = crear(body("B", "G_A"));

        // PUT sobre B pidiendo el código de A → colisión con OTRA entidad → 400.
        mockMvc.perform(put("/api/subgrupos/" + idB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("A", "G_A")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edicion_guardaMismoCodigo_200() throws Exception {
        long id = crear(body("A", "G_A"));

        // PUT sobre A con su MISMO código: la unicidad debe excluirse a sí misma → 200,
        // NO 400. Es el test que un findByCodigo ingenuo (sin comparar id) rompe.
        mockMvc.perform(put("/api/subgrupos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("A", "G_B")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("A"))
                .andExpect(jsonPath("$.grupos", containsInAnyOrder("G_B")));
    }

    @Test
    void alta_conDosGrupos_201YRoundTripDeCodigos() throws Exception {
        // D-nueva-5 (aserto más importante del bloque): el round-trip fija QUÉ DOS
        // códigos, no solo cuántos. Un length()==2 pasaría con grupos equivocados.
        long id = crear(body("SG1", "G_A", "G_B"));

        mockMvc.perform(get("/api/subgrupos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("SG1"))
                .andExpect(jsonPath("$.grupos", containsInAnyOrder("G_A", "G_B")));
    }

    @Test
    void grupoInexistenteEnAlta_400ConCodigoEnMensaje() throws Exception {
        // D-nueva-4: si ALGÚN código de grupo no resuelve → 400 que lo NOMBRA.
        mockMvc.perform(post("/api/subgrupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("SG1", "G_A", "NOEXISTE")))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("NOEXISTE")));
    }

    @Test
    void gruposVacio_400() throws Exception {
        // D-nueva-1: un subgrupo necesita ≥1 grupo (invariante I6). grupos=[] → 400.
        mockMvc.perform(post("/api/subgrupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"SG1\",\"grupos\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edicion_reemplazaGruposTotal_200() throws Exception {
        // Reemplazo TOTAL, no unión: subgrupo con [G_A,G_B]; PUT con [G_C] → el GET solo
        // ve G_C. Si actualizar() uniese, el GET tendría 3 códigos y esto caería.
        long id = crear(body("SG1", "G_A", "G_B"));

        mockMvc.perform(put("/api/subgrupos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("SG1", "G_C")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupos", containsInAnyOrder("G_C")));

        mockMvc.perform(get("/api/subgrupos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grupos", containsInAnyOrder("G_C")));
    }

    @Test
    void borrado_limpiaJoinTableYNoBorraGrupos() throws Exception {
        // D-nueva-3 (borrado fuerte): dar de alta SG1 con [G_A,G_B], contar la join
        // table ANTES (2), borrar, y verificar que (a) queda a 0 filas para ese
        // subgrupo y (b) los grupos G_A/G_B SIGUEN existiendo.
        long id = crear(body("SG1", "G_A", "G_B"));

        assertEquals(2L, contarJoinTable(id), "el alta debe crear 2 filas en subgrupo_grupo");

        mockMvc.perform(delete("/api/subgrupos/" + id))
                .andExpect(status().isNoContent());

        // (a) el cascade del lado propietario limpia la join table.
        assertEquals(0L, contarJoinTable(id),
                "el borrado del subgrupo debe dejar 0 filas en subgrupo_grupo");
        // (b) los grupos, entidades independientes, sobreviven al borrado del subgrupo.
        assertTrue(grupoRepository.findByCodigo("G_A").isPresent(), "G_A no debe borrarse");
        assertTrue(grupoRepository.findByCodigo("G_B").isPresent(), "G_B no debe borrarse");
    }

    @Test
    void borrado_204_yLuego404() throws Exception {
        long id = crear(body("SG1", "G_A"));

        mockMvc.perform(delete("/api/subgrupos/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/subgrupos/" + id))
                .andExpect(status().isNotFound());
    }

    /**
     * Borrado amable (8.5-C2b) de subgrupo: un subgrupo usado por una {@link Plaza} (FK
     * {@code plaza_subgrupo.subgrupo_id}) NO se borra → 409. Complementa los dos tests del 204:
     * su población en {@code subgrupo_grupo} NO cuenta (es agregado propio que Hibernate limpia),
     * solo una referencia ENTRANTE desde fuera —una plaza— lo veta.
     *
     * <p><b>El {@code containsExactly} con un ÚNICO referente {@code plaza(s)} es el aserto que
     * blinda el fix del falso positivo</b>: si {@code subgrupo_grupo} volviera a contarse, aquí
     * sobraría un referente {@code grupo(s)} y el test caería. El desalineado de ids (un subgrupo
     * de relleno) evita que una {@code @Query} desviada a otra columna acierte por colisión;
     * verificado por la mutación de {@code contarPlazas}.
     */
    @Test
    void borrado_subgrupoUsadoPorPlaza_409YSoloPlazaComoReferente() throws Exception {
        // Desalineado: un subgrupo de relleno empuja el id del subgrupo real fuera del id de la plaza.
        subgrupoRepository.save(new Subgrupo("SG_RELLENO",
                Set.of(grupoRepository.findByCodigo("G_C").orElseThrow())));
        Subgrupo subgrupo = subgrupoRepository.save(new Subgrupo("SG1",
                Set.of(grupoRepository.findByCodigo("G_A").orElseThrow())));

        // Una plaza (dentro de una actividad) que referencia el subgrupo por su M:N.
        Asignatura mat = asignaturaRepository.save(new Asignatura("MAT", "Matematicas"));
        Aula aula = aulaRepository.save(new Aula("AULA_T", TipoAula.ORDINARIA, null, null, null, null));
        Actividad actividad = new Actividad("ACT", mat, 1, 1, PatronTemporal.NEUTRA, false);
        Plaza plaza = actividad.agregarPlaza("ACT-P1", mat, aula, Set.of(), Set.of(), Set.of(subgrupo));
        actividadRepository.save(actividad);
        entityManager.flush();

        // Precondición del desalineado: el id del subgrupo no coincide con el de la plaza ni la actividad.
        assertThat(List.of(plaza.getId(), actividad.getId())).doesNotContain(subgrupo.getId());

        mockMvc.perform(delete("/api/subgrupos/" + subgrupo.getId()))
                .andExpect(status().isConflict());

        ReferenciaEntranteException error = catchThrowableOfType(
                () -> service.borrar(subgrupo.getId()), ReferenciaEntranteException.class);
        assertThat(error).isNotNull();
        assertThat(error.getReferencias())
                .containsExactly(new Referencia("plaza(s)", 1L));
    }

    // ============== GUARDA G2: el mono-Di de un PDC no se edita ni se borra aquí ==============

    /**
     * (4) PUT sobre el subgrupo mono-Di automático de un PDC → 400 que NOMBRA el subgrupo.
     *
     * <p>El cuerpo pide un RENOMBRE, que es el daño concreto que la guarda evita: el agregado
     * PDC localiza su subgrupo por el código DERIVADO ({@code codigo + "-Completo"}), no por
     * FK, así que un rename deja el {@code DELETE /api/grupos/{idPadre}/pdc} en 404 para
     * siempre y el agregado partido en dos mitades que ya no se encuentran.
     */
    @Test
    void edicionDelMonoDiDeUnPdc_400ConCodigoEnMensaje() throws Exception {
        long idMonoDi = crearPdcYDevolverIdDeSuMonoDi("G_A", "G_A_DI");

        mockMvc.perform(put("/api/subgrupos/" + idMonoDi)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("OTRO_NOMBRE", "G_A_DI")))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("G_A_DI-Completo")));
    }

    /**
     * (5) DELETE sobre el mismo subgrupo → 400, y sigue ahí.
     *
     * <p>400 y no 409 a propósito: no hay ninguna referencia entrante que contar —el mono-Di
     * recién creado no está en ninguna plaza—, lo que falla es la petición contra el estado
     * de la propia entidad. El 409 de este endpoint sigue siendo suyo y solo suyo
     * ({@link #borrado_subgrupoUsadoPorPlaza_409YSoloPlazaComoReferente}).
     */
    @Test
    void borradoDelMonoDiDeUnPdc_400YNoLoBorra() throws Exception {
        long idMonoDi = crearPdcYDevolverIdDeSuMonoDi("G_A", "G_A_DI");

        mockMvc.perform(delete("/api/subgrupos/" + idMonoDi))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("G_A_DI-Completo")));

        // El rechazo no es cosmético: el subgrupo sigue en la base.
        assertTrue(subgrupoRepository.findByCodigo("G_A_DI-Completo").isPresent(),
                "el mono-Di rechazado no debe haberse borrado");
    }

    /**
     * (6) DISCRIMINANTE DEL "EXACTAMENTE UNO": un subgrupo cuya población son DOS grupos PDC
     * se edita con normalidad → 200.
     *
     * <p>Es el ámbito COMPARTIDO de los Di (§6.2, Nota S23): los diversificados de varios
     * grupos cursando juntos su tronco alternativo. No pertenece a ningún agregado PDC —no lo
     * creó ningún alta compuesta y ningún borrado de PDC lo va a limpiar—, así que este CRUD
     * es el único sitio desde el que se gestiona.
     *
     * <p><b>Este test existe para que nadie relaje el predicado más adelante.</b> La
     * formulación tentadora —"contiene algún grupo PDC"— deja (4) y (5) igual de verdes y
     * deja este caso legítimo sin forma de editarse ni borrarse por la API. Aquí es donde se
     * pone rojo.
     */
    @Test
    void edicionDeSubgrupoConDosGruposPdc_200() throws Exception {
        crearPdcYDevolverIdDeSuMonoDi("G_A", "G_A_DI");
        crearPdcYDevolverIdDeSuMonoDi("G_B", "G_B_DI");

        // El ámbito compartido: población = los DOS Di, de dos padres distintos.
        long idAmbito = crear(body("AMBITO_DI", "G_A_DI", "G_B_DI"));

        mockMvc.perform(put("/api/subgrupos/" + idAmbito)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("AMBITO_DI_BIS", "G_A_DI", "G_B_DI")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("AMBITO_DI_BIS"))
                .andExpect(jsonPath("$.grupos", containsInAnyOrder("G_A_DI", "G_B_DI")));
    }

    /**
     * (7) NO REGRESIÓN: un subgrupo mono-grupo ORDINARIO se sigue editando (200) y borrando
     * (204). Duplica a propósito lo que ya cubren {@link #edicion_cambiaCodigoManteniendoGrupos_200}
     * y {@link #borrado_204_yLuego404}: los tiene al lado de la guarda para que el radio de
     * acción de G2 —mono-grupo sí, pero solo si es PDC— se lea de una vez en este bloque.
     */
    @Test
    void noRegresion_subgrupoDeUnGrupoOrdinario_seEditaYSeBorra() throws Exception {
        long id = crear(body("SG1", "G_A"));

        mockMvc.perform(put("/api/subgrupos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("SG1_BIS", "G_B")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("SG1_BIS"))
                .andExpect(jsonPath("$.grupos", containsInAnyOrder("G_B")));

        mockMvc.perform(delete("/api/subgrupos/" + id))
                .andExpect(status().isNoContent());
    }

    /**
     * Cuelga un PDC del grupo ordinario indicado por el alta compuesta REAL y devuelve el id
     * de su subgrupo mono-Di, que el alta deriva como {@code codigoPdc + "-Completo"}.
     *
     * <p>El {@code flush + clear} desliga las entidades para que la lectura siguiente vaya a
     * la BASE y no a la caché L1 de Hibernate (mismo aviso de framework que
     * {@code PdcEndpointTest}): el sujeto de estos tests es lo que el servicio releerá, no el
     * objeto que acaba de construir en memoria.
     */
    private long crearPdcYDevolverIdDeSuMonoDi(String codigoPadre, String codigoPdc) {
        long idPadre = grupoRepository.findByCodigo(codigoPadre).orElseThrow().getId();
        pdcService.crear(idPadre, new PdcRequest(codigoPdc));
        entityManager.flush();
        entityManager.clear();
        return subgrupoRepository.findByCodigo(codigoPdc + "-Completo").orElseThrow().getId();
    }

    /**
     * Cuenta las filas de la join table {@code subgrupo_grupo} para un subgrupo dado.
     * Query NATIVA: no hay lado inverso del @ManyToMany (es unidireccional) y tras el
     * borrado el Subgrupo ya no existe, así que es el único camino. El nombre
     * {@code subgrupo_grupo} es parte del contrato de la entidad (@JoinTable name).
     */
    private long contarJoinTable(long subgrupoId) {
        Number filas = (Number) entityManager.getEntityManager()
                .createNativeQuery("SELECT count(*) FROM subgrupo_grupo WHERE subgrupo_id = ?")
                .setParameter(1, subgrupoId)
                .getSingleResult();
        return filas.longValue();
    }

    /** Da de alta por la red con el body dado y devuelve el id sintético asignado. */
    private long crear(String body) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/subgrupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    /** {@code {"codigo":..,"grupos":["..",..]}} a partir de los códigos de grupo. */
    private static String body(String codigo, String... grupos) {
        StringBuilder sb = new StringBuilder("{\"codigo\":\"").append(codigo).append("\",\"grupos\":[");
        for (int i = 0; i < grupos.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(grupos[i]).append("\"");
        }
        return sb.append("]}").toString();
    }
}
