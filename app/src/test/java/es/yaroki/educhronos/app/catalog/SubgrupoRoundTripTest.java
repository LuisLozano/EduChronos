package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SubgrupoRoundTripTest {

    @Autowired
    private SubgrupoRepository subgrupoRepository;

    @Autowired
    private GrupoAdministrativoRepository grupoRepository;

    @Autowired
    private NivelRepository nivelRepository;

    @Test
    void subgrupoMultiGrupoSobreviveElRoundTrip() {
        // Población: caso "Lectura B" — alumnos de varios grupos del mismo nivel.
        Nivel nivel = nivelRepository.save(new Nivel("1BACH", 5));
        GrupoAdministrativo bachA =
                grupoRepository.save(new GrupoAdministrativo("1ºBach A", nivel, TipoGrupo.ORDINARIO, null));
        GrupoAdministrativo bachB =
                grupoRepository.save(new GrupoAdministrativo("1ºBach B", nivel, TipoGrupo.ORDINARIO, null));
        GrupoAdministrativo bachC =
                grupoRepository.save(new GrupoAdministrativo("1ºBach C", nivel, TipoGrupo.ORDINARIO, null));

        Subgrupo lecturaB =
                new Subgrupo("1Bach-Lectura-B", Set.of(bachA, bachB, bachC));
        Long id = subgrupoRepository.save(lecturaB).getId();

        subgrupoRepository.flush();

        Subgrupo recuperado = subgrupoRepository.findById(id).orElseThrow();
        assertThat(recuperado.getCodigo()).isEqualTo("1Bach-Lectura-B");
        assertThat(recuperado.getGrupos())
                .extracting(GrupoAdministrativo::getCodigo)
                .containsExactlyInAnyOrder("1ºBach A", "1ºBach B", "1ºBach C");
    }

    @Test
    void subgrupoMonoGrupoSobreviveElRoundTrip() {
        Nivel nivel = nivelRepository.save(new Nivel("1ESO", 1));
        GrupoAdministrativo grupo1A =
                grupoRepository.save(new GrupoAdministrativo("1ºA", nivel, TipoGrupo.ORDINARIO, null));

        Subgrupo completo = new Subgrupo("1ºA-Completo", Set.of(grupo1A));
        Long id = subgrupoRepository.save(completo).getId();

        subgrupoRepository.flush();

        Subgrupo recuperado = subgrupoRepository.findById(id).orElseThrow();
        assertThat(recuperado.getGrupos())
                .extracting(GrupoAdministrativo::getCodigo)
                .containsExactly("1ºA");
    }
}