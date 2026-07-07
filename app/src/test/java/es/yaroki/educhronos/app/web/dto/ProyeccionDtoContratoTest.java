package es.yaroki.educhronos.app.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * Blinda el contrato de serialización JSON de los DTOs de proyección (Fase 7)
 * contra divergencia silenciosa (deuda D-F8.1-8). Origen: un fixture del frontend
 * inventó un profesor TEC4 en 7B sin que nada saltara; este test fija la FORMA del
 * JSON (claves y tipos), no el contenido, de modo que un cambio en el DTO rompa el
 * test a propósito y obligue a actualizar la interfaz espejo del frontend.
 *
 * <p>Usa el ObjectMapper REAL de la ruta de serialización de Spring MVC: el mismo
 * {@link MappingJackson2HttpMessageConverter} que {@code MockMvcBuilders.standaloneSetup}
 * registra en producción (SB 4.1 no trae el slice web de Boot, así que no hay
 * {@code @JsonTest}). No configura inclusión de nulls: verifica el comportamiento
 * por defecto (nulls serializados como null explícito), que es el contrato real.
 */
class ProyeccionDtoContratoTest {

    private final ObjectMapper mapper = new MappingJackson2HttpMessageConverter().getObjectMapper();

    @Test
    void sesionVistaDto_serializaExactamenteLas12ClavesConSusTipos() throws Exception {
        // CENTINELA D-F8.1-8: este contrato refleja app.web.dto.SesionVistaDTO. Si cambias el
        // DTO (añadir/quitar/renombrar/re-tipar un campo), este test salta A PROPÓSITO.
        // Al actualizarlo, actualiza TAMBIÉN app/frontend/src/app/models/horario.model.ts
        // (interfaz espejo, no atada automáticamente).
        List<String> clavesEsperadas = List.of(
                "sesionId", "indice", "dia", "tramo",
                "asignaturaCodigo", "asignaturaNombre",
                "profesores", "aulaCodigo",
                "subgrupos", "grupos",
                "actividadCodigo", "plazaCodigo");

        SesionVistaDTO sesion = new SesionVistaDTO(
                10L, 1, 2, 3, "Mat", "Matematicas", List.of("MATA"), "A1",
                List.of("1ºA-Completo"), List.of("1ºA"), "Mat-1ºA", "Mat-1ºA-P1");

        JsonNode json = mapper.readTree(mapper.writeValueAsString(sesion));

        assertThat(clavesRaiz(json)).containsExactlyInAnyOrderElementsOf(clavesEsperadas);

        assertThat(json.get("sesionId").isNumber()).isTrue();
        assertThat(json.get("indice").isNumber()).isTrue();
        assertThat(json.get("dia").isNumber()).isTrue();
        assertThat(json.get("tramo").isNumber()).isTrue();

        assertThat(json.get("asignaturaCodigo").isTextual()).isTrue();
        assertThat(json.get("asignaturaNombre").isTextual()).isTrue();
        assertThat(json.get("aulaCodigo").isTextual()).isTrue();
        assertThat(json.get("actividadCodigo").isTextual()).isTrue();
        assertThat(json.get("plazaCodigo").isTextual()).isTrue();

        assertThat(json.get("profesores").isArray()).isTrue();
        assertThat(json.get("subgrupos").isArray()).isTrue();
        assertThat(json.get("grupos").isArray()).isTrue();
    }

    @Test
    void horarioProyeccionDto_serializaExactamenteLas8ClavesConSusTipos() throws Exception {
        // CENTINELA D-F8.1-8: este contrato refleja app.web.dto.HorarioProyeccionDTO. Si cambias el
        // DTO (añadir/quitar/renombrar/re-tipar un campo), este test salta A PROPÓSITO.
        // Al actualizarlo, actualiza TAMBIÉN app/frontend/src/app/models/horario.model.ts
        // (interfaz espejo, no atada automáticamente).
        List<String> clavesEsperadas = List.of(
                "id", "nombre", "estado", "estadoSolver",
                "objetivo", "cotaInferior", "fechaGeneracion", "sesiones");

        SesionVistaDTO sesion = new SesionVistaDTO(
                10L, 1, 2, 3, "Mat", "Matematicas", List.of("MATA"), "A1",
                List.of("1ºA-Completo"), List.of("1ºA"), "Mat-1ºA", "Mat-1ºA-P1");
        HorarioProyeccionDTO dto = new HorarioProyeccionDTO(
                1L, "Horario seed 7B", "BORRADOR", "OPTIMAL", 12.0, 8.0,
                "2026-07-05T00:00:00Z", List.of(sesion));

        JsonNode json = mapper.readTree(mapper.writeValueAsString(dto));

        assertThat(clavesRaiz(json)).containsExactlyInAnyOrderElementsOf(clavesEsperadas);

        assertThat(json.get("id").isNumber()).isTrue();
        assertThat(json.get("objetivo").isNumber()).isTrue();
        assertThat(json.get("cotaInferior").isNumber()).isTrue();

        assertThat(json.get("nombre").isTextual()).isTrue();
        assertThat(json.get("estado").isTextual()).isTrue();
        assertThat(json.get("estadoSolver").isTextual()).isTrue();
        assertThat(json.get("fechaGeneracion").isTextual()).isTrue();

        assertThat(json.get("sesiones").isArray()).isTrue();
    }

    @Test
    void horarioProyeccionDto_conObjetivoYCotaNull_losSerializaComoNullExplicito() throws Exception {
        // Contrato con horario.model.ts: objetivo/cotaInferior son number|null SIEMPRE
        // presentes. Si estas claves DESAPARECIERAN del JSON, sería la config NON_NULL
        // (punto 2 de la parada de lectura): habría que parar, no "arreglar" el test.
        HorarioProyeccionDTO dto = new HorarioProyeccionDTO(
                1L, "Horario sin medir", "BORRADOR", "UNKNOWN", null, null,
                "2026-07-05T00:00:00Z", List.of());

        JsonNode json = mapper.readTree(mapper.writeValueAsString(dto));

        assertThat(json.has("objetivo")).isTrue();
        assertThat(json.get("objetivo").isNull()).isTrue();

        assertThat(json.has("cotaInferior")).isTrue();
        assertThat(json.get("cotaInferior").isNull()).isTrue();
    }

    /** Conjunto de claves de primer nivel del objeto JSON, derivado iterando fieldNames(). */
    private static List<String> clavesRaiz(JsonNode json) {
        List<String> claves = new ArrayList<>();
        json.fieldNames().forEachRemaining(claves::add);
        return claves;
    }
}
