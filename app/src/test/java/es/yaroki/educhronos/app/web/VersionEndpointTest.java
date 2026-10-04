package es.yaroki.educhronos.app.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * {@code GET /api/version} devuelve {@code educhronos.version} (C-version-y-rastro,
 * condición 2, S192).
 *
 * <p>La versión se fija por propiedad del test con un valor que no puede salir del
 * {@code build-info} de la suite ({@code 0.0.0-dev}): si el JSON lo trae, viene de la
 * propiedad. Contexto completo y base propia en un {@code @TempDir}, como
 * {@code GeneracionDuranteCambioTest}.
 */
@SpringBootTest(properties = "educhronos.version=7.7.7-prueba")
class VersionEndpointTest {

    @TempDir static Path carpeta;

    @DynamicPropertySource
    static void baseDelTest(DynamicPropertyRegistry propiedades) {
        propiedades.add(
                "spring.datasource.url",
                () -> "jdbc:sqlite:" + carpeta.resolve("educhronos.db").toAbsolutePath());
    }

    @Autowired private WebApplicationContext contexto;

    @Test
    void devuelveLaVersionPublicada() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(contexto).build();

        mockMvc.perform(get("/api/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("7.7.7-prueba"));
    }
}
