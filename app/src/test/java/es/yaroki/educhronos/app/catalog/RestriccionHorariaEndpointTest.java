package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.yaroki.educhronos.app.service.ProfesorService;
import es.yaroki.educhronos.app.service.RestriccionHorariaService;
import es.yaroki.educhronos.app.web.ProfesorController;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.exception.GenericJDBCException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Test de integración del sub-recurso RESTRICCIONES HORARIAS
 * {@code /api/profesores/{id}/restricciones-horarias} (Fase 8, Bloque 8.5-E). Ejerce
 * consulta y reemplazo total POR LA RED ({@code standaloneSetup} + servicios reales sobre
 * {@code @DataJpaTest}), calcando {@code TutoriaEndpointTest}, con asertos DISCRIMINANTES:
 * cada uno cae si se retira la regla que protege.
 *
 * <p>El calendario de {@link #setUp} incluye un RECREO deliberado (lunes, orden global 3)
 * para que la renumeración {@code ordenEnDia} no sea la identidad: lunes tiene órdenes
 * globales 1,2,3(recreo),4 que se renumeran a ordenEnDia 1,2,—,3. Un test que solo usara
 * días sin recreo no distinguiría {@code orden} de {@code ordenEnDia} y dejaría pasar una
 * implementación que confundiera ambos.
 *
 * <p><b>AVISO DE FRAMEWORK</b> (familia S73-S77): en {@code @DataJpaTest} todo comparte UNA
 * transacción, así que un {@code findBy...} tras una escritura puede servir la caché L1 de
 * Hibernate y no la BASE. Todo aserto de esta clase que mire el estado persistido pasa por
 * {@link #leerDeLaBase()}, que hace {@code flush() + clear()} antes de consultar. Los tests
 * donde eso fue NECESARIO (no decorativo) están anotados uno a uno.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ProfesorService.class, RestriccionHorariaService.class})
class RestriccionHorariaEndpointTest {

    @Autowired private ProfesorService profesorService;
    @Autowired private RestriccionHorariaService restriccionService;
    @Autowired private ProfesorRepository profesorRepository;
    @Autowired private TramoSemanalRepository tramoRepository;
    @Autowired private ProfesorRestriccionHorariaRepository restriccionRepository;
    @Autowired private TestEntityManager entityManager;

    private MockMvc mockMvc;
    private long profesorId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ProfesorController(profesorService, restriccionService)).build();

        profesorId = profesorRepository.save(new Profesor("MAT8", "Ada Lovelace")).getId();

        // Lunes: 1, 2, RECREO, 4  →  ordenEnDia 1, 2, (excluido), 3
        tramo(Dia.LUNES, 1, true);
        tramo(Dia.LUNES, 2, true);
        tramo(Dia.LUNES, 3, false);
        tramo(Dia.LUNES, 4, true);
        // Martes: 1, 2 → ordenEnDia 1, 2
        tramo(Dia.MARTES, 1, true);
        tramo(Dia.MARTES, 2, true);
    }

    // ------------------------------------------------------------------ camino feliz

    @Test
    void getSinRestricciones_devuelveListaVacia_200() throws Exception {
        mockMvc.perform(get("/api/profesores/" + profesorId + "/restricciones-horarias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void get_profesorInexistente_404() throws Exception {
        mockMvc.perform(get("/api/profesores/9999/restricciones-horarias"))
                .andExpect(status().isNotFound());
    }

    @Test
    void put_creaYdevuelveLaListaOrdenadaPorDiaYorden() throws Exception {
        mockMvc.perform(put("/api/profesores/" + profesorId + "/restricciones-horarias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + cuerpo("BLANDA", 2, 2, "Reunion de departamento") + ","
                                + cuerpo("DURA", 1, 1, null) + "]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // Orden por (dia, ordenEnDia): lunes-1 antes que martes-2, pese al orden
                // inverso en el body.
                .andExpect(jsonPath("$[0].dia").value(1))
                .andExpect(jsonPath("$[0].ordenEnDia").value(1))
                .andExpect(jsonPath("$[0].tipo").value("DURA"))
                .andExpect(jsonPath("$[0].motivo").value(nullValue()))
                .andExpect(jsonPath("$[1].dia").value(2))
                .andExpect(jsonPath("$[1].ordenEnDia").value(2))
                .andExpect(jsonPath("$[1].tipo").value("BLANDA"))
                .andExpect(jsonPath("$[1].motivo").value("Reunion de departamento"));
    }

    /**
     * El {@code ordenEnDia} 3 del lunes es el tramo de orden GLOBAL 4 (el recreo de orden 3
     * no cuenta). Discrimina que se usa la renumeración de
     * {@code CatalogoMapper.indiceOrdenEnDia} y no el {@code orden} crudo: una
     * implementación que casara contra {@code getOrden()} resolvería el recreo aquí.
     */
    @Test
    void put_ordenEnDiaSaltaElRecreo() throws Exception {
        ponerRestricciones("[" + cuerpo("DURA", 1, 3, null) + "]");

        TramoSemanal resuelto = leerDeLaBase().get(0).getTramo();
        assertThat(resuelto.getOrden()).isEqualTo(4);
        assertThat(resuelto.isEsLectivo()).isTrue();
    }

    @Test
    void putConListaVacia_borraTodas_200() throws Exception {
        ponerRestricciones("[" + cuerpo("DURA", 1, 1, null) + "]");

        mockMvc.perform(put("/api/profesores/" + profesorId + "/restricciones-horarias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        assertThat(leerDeLaBase()).isEmpty();
    }

    // ------------------------------------------------------- validaciones y su ORDEN

    @Test
    void put_profesorInexistente_404() throws Exception {
        mockMvc.perform(put("/api/profesores/9999/restricciones-horarias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + cuerpo("DURA", 1, 1, null) + "]"))
                .andExpect(status().isNotFound());
    }

    @Test
    void put_tramoInexistente_400QueNombraElPar() throws Exception {
        mockMvc.perform(put("/api/profesores/" + profesorId + "/restricciones-horarias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + cuerpo("DURA", 1, 9, null) + "]"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("dia=1")))
                .andExpect(status().reason(containsString("ordenEnDia=9")));
    }

    /**
     * Un (dia, ordenEnDia) que apunta a un tramo de RECREO no empareja: el índice de
     * {@code indiceOrdenEnDia} ya los excluye. Aquí (lunes, 4) no existe porque el lunes
     * solo tiene TRES tramos lectivos.
     */
    @Test
    void put_tramoDeRecreo_400() throws Exception {
        mockMvc.perform(put("/api/profesores/" + profesorId + "/restricciones-horarias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + cuerpo("DURA", 1, 4, null) + "]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void put_tipoInvalido_400QueNombraElValorYlistaLosValidos() throws Exception {
        mockMvc.perform(put("/api/profesores/" + profesorId + "/restricciones-horarias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + cuerpo("PREFERENTE", 1, 1, null) + "]"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("PREFERENTE")))
                .andExpect(status().reason(containsString("BLANDA")));
    }

    /**
     * El ORDEN de validación importa: con un tipo inválido en el elemento 1 Y un tramo
     * inexistente en el 2, gana el mensaje del TRAMO, porque la resolución del tramo es una
     * pasada COMPLETA anterior al parseo del tipo. Si las dos se fundieran en un bucle por
     * elemento, ganaría el tipo del elemento 1 y este test caería.
     */
    @Test
    void put_tipoInvalidoYtramoInexistente_ganaElMensajeDelTramo() throws Exception {
        mockMvc.perform(put("/api/profesores/" + profesorId + "/restricciones-horarias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + cuerpo("TIPO_MALO", 1, 1, null) + ","
                                + cuerpo("DURA", 5, 9, null) + "]"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("ordenEnDia=9")));
    }

    // ============================ ASERTOS DISCRIMINANTES EXIGIDOS ============================

    /**
     * (A1) IDEMPOTENCIA REAL: el MISMO cuerpo dos veces → 200, y el CONTENIDO de la base es
     * idéntico. Compara el CONJUNTO de (dia, ordenEnDia, tipo), no {@code size()}: un
     * tamaño correcto con contenido distinto —p. ej. si el reemplazo insertara antes de
     * borrar y quedara la fila equivocada— pasaría un aserto de tamaño y falla este.
     *
     * <p>Framework: el {@code flush + clear} de {@link #leerDeLaBase()} es NECESARIO aquí.
     * Sin él la consulta se sirve de la caché L1 y el aserto pasaría aunque el segundo
     * {@code deleteAll + INSERT} no hubiera llegado a la base.
     */
    @Test
    void a1_idempotencia_mismoCuerpoDosVeces_contenidoIdenticoEnBase() throws Exception {
        String cuerpo = "[" + cuerpo("DURA", 1, 1, null) + ","
                + cuerpo("BLANDA", 2, 2, "Conciliacion") + "]";

        ponerRestricciones(cuerpo);
        Set<String> trasPrimerPut = contenidoEnBase();

        // Segunda vez: el deleteAll + flush ANTES de insertar es lo que lo hace pasar.
        mockMvc.perform(put("/api/profesores/" + profesorId + "/restricciones-horarias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk());
        Set<String> trasSegundoPut = contenidoEnBase();

        assertThat(trasPrimerPut).containsExactlyInAnyOrder("1-1-DURA", "2-2-BLANDA");
        assertThat(trasSegundoPut).isEqualTo(trasPrimerPut);
    }

    /**
     * (A2) REEMPLAZO TOTAL, NO MERGE. Es el aserto que distingue las dos semánticas: sin el
     * {@code doesNotContain}, un servicio que hiciera MERGE dejaría lunes-1 vivo y el test
     * seguiría verde. El {@code containsExactly} sobre el conjunto completo lo cierra por
     * los dos lados: está lo nuevo y NO está nada más.
     */
    @Test
    void a2_reemplazoTotalNoMerge_loAnteriorDesaparece() throws Exception {
        ponerRestricciones("[" + cuerpo("DURA", 1, 1, null) + "]");
        assertThat(contenidoEnBase()).containsExactly("1-1-DURA");

        ponerRestricciones("[" + cuerpo("BLANDA", 2, 2, null) + "]");

        Set<String> finales = contenidoEnBase();
        assertThat(finales).contains("2-2-BLANDA");
        // EL ASERTO QUE DISTINGUE REEMPLAZO DE MERGE: lunes-1 ya NO existe.
        assertThat(finales).doesNotContain("1-1-DURA");
        assertThat(finales).containsExactly("2-2-BLANDA");
    }

    /**
     * (A3) El servicio escribe {@code peso = 1} SIEMPRE, también en las BLANDA, y lo hace de
     * verdad en la BASE (no queda en el 0 por defecto del {@code int}). Se lee la fila
     * releída tras {@code clear()}: si el aserto mirase la entidad gestionada en memoria, un
     * servicio que no tocara el campo podría engañarlo.
     */
    @Test
    void a3_peso_esUnoEnLaBase_traUnPutBlanda() throws Exception {
        ponerRestricciones("[" + cuerpo("BLANDA", 1, 1, "Cuidado familiar") + "]");

        List<ProfesorRestriccionHoraria> enBase = leerDeLaBase();
        assertThat(enBase).hasSize(1);
        assertThat(enBase.get(0).getTipo()).isEqualTo(TipoRestriccion.BLANDA);
        assertThat(enBase.get(0).getPeso()).isEqualTo(1);
    }

    /**
     * (A4) DUPLICADO EN EL BODY → 400 y la base queda INTACTA, no a medias. Escribe algo
     * antes, manda un cuerpo con (lunes,2) repetido y comprueba que lo PREVIO sigue ahí. Es
     * el aserto que discrimina que la guarda corre ANTES del {@code deleteAll}: si corriera
     * después (o no existiera y saltara la BD), lo previo ya estaría borrado y el estado
     * final sería el vacío, no el original.
     */
    @Test
    void a4_duplicadoEnElBody_400YlaBaseQuedaIntacta() throws Exception {
        ponerRestricciones("[" + cuerpo("DURA", 1, 1, "Previo que debe sobrevivir") + "]");

        mockMvc.perform(put("/api/profesores/" + profesorId + "/restricciones-horarias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[" + cuerpo("DURA", 1, 2, null) + ","
                                + cuerpo("BLANDA", 1, 2, null) + "]"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(containsString("dia=1")))
                .andExpect(status().reason(containsString("ordenEnDia=2")));

        // La base NO se ha tocado: sigue EXACTAMENTE lo previo, ni borrado ni a medias.
        assertThat(contenidoEnBase()).containsExactly("1-1-DURA");
    }

    /**
     * (A6) La FK {@code profesor_restriccion_horaria.profesor_id} NO cascadea: borrar un
     * profesor con restricciones da 409 por la guarda de referencia entrante de
     * {@code ProfesorService.borrar} (que ya contaba restricciones desde 8.5-C2b), y por
     * debajo la propia BD lo rechazaría igualmente. Este test comprueba las DOS capas: si
     * alguien retirase la guarda del servicio, el segundo bloque seguiría documentando cuál
     * es el comportamiento real del esquema.
     */
    @Test
    void a6_borrarProfesorConRestricciones_409YlaFkEsRestrictEnLaBase() throws Exception {
        ponerRestricciones("[" + cuerpo("DURA", 1, 1, null) + "]");
        entityManager.flush();

        // Capa 1: el servicio veta el borrado con 409 y NOMBRA la referencia.
        mockMvc.perform(delete("/api/profesores/" + profesorId))
                .andExpect(status().isConflict())
                .andExpect(status().reason(containsString("restriccion(es) horaria(s)")));

        // Capa 2: por debajo, la FK es RESTRICT (sin ON DELETE CASCADE en schema.sql). Se
        // salta el servicio y se borra por el repositorio: la BD lo rechaza al flush. El
        // aserto exige la CAUSA concreta —violación de FK— y no un Exception cualquiera:
        // con isInstanceOf(Exception.class) el test pasaría también con un NPE y no
        // probaría nada sobre el esquema.
        //
        // El tipo es GenericJDBCException, NO DataIntegrityViolationException, por dos
        // motivos medidos (no supuestos): el flush() de TestEntityManager no cruza la
        // frontera del repositorio, que es donde Spring traduce a su jerarquía DAO; y el
        // SQLiteDialect community 7.4.1 no clasifica el SQLITE_CONSTRAINT_FOREIGNKEY como
        // violación de constraint, así que cae al genérico. Por eso el aserto de MENSAJE
        // es aquí el que discrimina de verdad, y el de tipo solo acompaña.
        entityManager.clear();
        assertThatThrownBy(() -> {
            profesorRepository.deleteById(profesorId);
            entityManager.flush();
        })
                .isInstanceOf(GenericJDBCException.class)
                .hasMessageContaining("FOREIGN KEY constraint failed");
    }

    // ------------------------------------------------------------------------- helpers

    /** Pone las restricciones por la red y exige 200. */
    private void ponerRestricciones(String cuerpoJson) throws Exception {
        mockMvc.perform(put("/api/profesores/" + profesorId + "/restricciones-horarias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoJson))
                .andExpect(status().isOk());
    }

    /**
     * Relee las restricciones del profesor DESDE LA BASE. El {@code flush + clear} es el
     * punto entero del helper: desliga las entidades para que la consulta no la sirva la
     * caché L1 de Hibernate dentro de la única transacción del {@code @DataJpaTest}.
     */
    private List<ProfesorRestriccionHoraria> leerDeLaBase() {
        entityManager.flush();
        entityManager.clear();
        return restriccionRepository.findByProfesor_Id(profesorId);
    }

    /** El contenido persistido como conjunto de {@code "dia-ordenEnDia-TIPO"}, para comparar
     * CONTENIDO y no tamaño. El ordenEnDia se recalcula con la fuente única de renumeración. */
    private Set<String> contenidoEnBase() {
        List<ProfesorRestriccionHoraria> filas = leerDeLaBase();
        var ordenEnDia = es.yaroki.educhronos.app.mapper.CatalogoMapper
                .indiceOrdenEnDia(tramoRepository.findAll());
        return filas.stream()
                .map(r -> (r.getTramo().getDia().ordinal() + 1)
                        + "-" + ordenEnDia.get(r.getTramo().getId())
                        + "-" + r.getTipo().name())
                .collect(Collectors.toSet());
    }

    private void tramo(Dia dia, int orden, boolean esLectivo) {
        tramoRepository.save(new TramoSemanal(
                dia, LocalTime.of(7 + orden, 0), LocalTime.of(8 + orden, 0),
                esLectivo, orden, null));
    }

    /** {@code {"tipo":"..","dia":N,"ordenEnDia":N,"motivo":".."}} ({@code motivo} nulo → JSON null). */
    private static String cuerpo(String tipo, int dia, int ordenEnDia, String motivo) {
        String motivoJson = motivo == null ? "null" : "\"" + motivo + "\"";
        return "{\"tipo\":\"" + tipo + "\",\"dia\":" + dia
                + ",\"ordenEnDia\":" + ordenEnDia + ",\"motivo\":" + motivoJson + "}";
    }
}
