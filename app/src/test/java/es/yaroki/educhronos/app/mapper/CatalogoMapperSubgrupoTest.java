package es.yaroki.educhronos.app.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.yaroki.educhronos.app.catalog.GrupoAdministrativo;
import es.yaroki.educhronos.app.catalog.Subgrupo;
import es.yaroki.educhronos.app.catalog.TipoGrupo;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CatalogoMapperSubgrupoTest {

    @Test
    void aSubgrupoResuelveGruposPorIdentidadDeObjeto() {
        // Grupos de dominio obtenidos del propio mapper (patrón de CatalogoMapperTest,
        // no fabricados con new): así el índice contiene EXACTAMENTE los objetos que
        // produce el flujo real, y la aserción de identidad de objeto es significativa.
        GrupoAdministrativo jpaA = new GrupoAdministrativo("1ºBach A", null, TipoGrupo.ORDINARIO, null);
        GrupoAdministrativo jpaB = new GrupoAdministrativo("1ºBach B", null, TipoGrupo.ORDINARIO, null);

        es.yaroki.educhronos.solver.domain.GrupoAdministrativo domA = CatalogoMapper.aGrupo(jpaA);
        es.yaroki.educhronos.solver.domain.GrupoAdministrativo domB = CatalogoMapper.aGrupo(jpaB);
        Map<String, es.yaroki.educhronos.solver.domain.GrupoAdministrativo> indice =
                Map.of("1ºBach A", domA, "1ºBach B", domB);

        Subgrupo entidad = new Subgrupo("1Bach-Lectura-B", Set.of(jpaA, jpaB));

        es.yaroki.educhronos.solver.domain.Subgrupo resultado =
                CatalogoMapper.aSubgrupo(entidad, indice);

        assertThat(resultado.codigo()).isEqualTo("1Bach-Lectura-B");
        // Identidad de objeto: los grupos del subgrupo son EXACTAMENTE los del índice.
        assertThat(resultado.grupos()).containsExactlyInAnyOrder(domA, domB);
        assertThat(resultado.grupos()).allMatch(g -> g == domA || g == domB);
    }

    @Test
    void aSubgrupoAbortaSiUnGrupoNoEstaEnElIndice() {
        Map<String, es.yaroki.educhronos.solver.domain.GrupoAdministrativo> indiceVacio = Map.of();
        GrupoAdministrativo jpaHuerfano =
                new GrupoAdministrativo("1ºZ", null, TipoGrupo.ORDINARIO, null);
        Subgrupo entidad = new Subgrupo("sg-huerfano", Set.of(jpaHuerfano));

        assertThatThrownBy(() -> CatalogoMapper.aSubgrupo(entidad, indiceVacio))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1ºZ");
    }
}