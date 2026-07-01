package es.yaroki.educhronos.app.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.yaroki.educhronos.app.catalog.Dia;
import es.yaroki.educhronos.app.catalog.Nivel;
import es.yaroki.educhronos.app.catalog.TipoAula;
import es.yaroki.educhronos.app.catalog.TipoGrupo;
import es.yaroki.educhronos.app.catalog.TramoSemanal;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios del {@link CatalogoMapper} (Fase 6, Bloque 3). Puros: las
 * entidades JPA se construyen en memoria con sus constructores públicos, sin
 * contexto Spring ni base de datos. Un método (o caso) por conversión.
 */
class CatalogoMapperTest {

    private static final Nivel ESO1 = new Nivel("1ESO", 1);

    @Test
    void aAula_replicaCodigoComoNombre() {
        es.yaroki.educhronos.app.catalog.Aula jpa =
                new es.yaroki.educhronos.app.catalog.Aula("LAB1", TipoAula.LAB_CIENCIAS, 30, "B", 1, "Norte");

        es.yaroki.educhronos.solver.domain.Aula dominio = CatalogoMapper.aAula(jpa);

        assertThat(dominio.codigo()).isEqualTo("LAB1");
        // Decisión de producto (Opción 1): sin nombre de aula en el modelo, nombre = codigo.
        assertThat(dominio.nombre()).isEqualTo("LAB1");
    }

    @Test
    void aAsignatura_mapeaCodigoYNombreCompleto() {
        es.yaroki.educhronos.app.catalog.Asignatura jpa =
                new es.yaroki.educhronos.app.catalog.Asignatura("ByG", "Biología y Geología");

        es.yaroki.educhronos.solver.domain.Asignatura dominio = CatalogoMapper.aAsignatura(jpa);

        assertThat(dominio.codigo()).isEqualTo("ByG");
        assertThat(dominio.nombre()).isEqualTo("Biología y Geología");
    }

    @Test
    void aProfesor_mapeaCodigoYNombreCompleto() {
        es.yaroki.educhronos.app.catalog.Profesor jpa =
                new es.yaroki.educhronos.app.catalog.Profesor("MAT8", "María Martínez");

        es.yaroki.educhronos.solver.domain.Profesor dominio = CatalogoMapper.aProfesor(jpa);

        assertThat(dominio.codigo()).isEqualTo("MAT8");
        assertThat(dominio.nombre()).isEqualTo("María Martínez");
    }

    @Test
    void aGrupo_ordinarioSinPadre() {
        es.yaroki.educhronos.app.catalog.GrupoAdministrativo jpa =
                new es.yaroki.educhronos.app.catalog.GrupoAdministrativo("1ºA", ESO1, TipoGrupo.ORDINARIO, null);

        es.yaroki.educhronos.solver.domain.GrupoAdministrativo dominio = CatalogoMapper.aGrupo(jpa);

        assertThat(dominio.codigo()).isEqualTo("1ºA");
        assertThat(dominio.tipo()).isEqualTo(es.yaroki.educhronos.solver.domain.TipoGrupo.ORDINARIO);
        assertThat(dominio.grupoPadre()).isEmpty();
    }

    @Test
    void aGrupo_pdcResuelvePadreRecursivo() {
        es.yaroki.educhronos.app.catalog.GrupoAdministrativo ordinario =
                new es.yaroki.educhronos.app.catalog.GrupoAdministrativo("1ºA", ESO1, TipoGrupo.ORDINARIO, null);
        es.yaroki.educhronos.app.catalog.GrupoAdministrativo pdc =
                new es.yaroki.educhronos.app.catalog.GrupoAdministrativo(
                        "3ºADi", ESO1, TipoGrupo.DIVERSIFICACION_PDC, ordinario);

        es.yaroki.educhronos.solver.domain.GrupoAdministrativo dominio = CatalogoMapper.aGrupo(pdc);

        assertThat(dominio.tipo()).isEqualTo(es.yaroki.educhronos.solver.domain.TipoGrupo.DIVERSIFICACION_PDC);
        assertThat(dominio.grupoPadre()).isPresent();
        assertThat(dominio.grupoPadre().orElseThrow().codigo()).isEqualTo("1ºA");
        assertThat(dominio.grupoPadre().orElseThrow().tipo())
                .isEqualTo(es.yaroki.educhronos.solver.domain.TipoGrupo.ORDINARIO);
    }

    @Test
    void aGrupo_virtualOptativaLanzaExcepcionExplicita() {
        es.yaroki.educhronos.app.catalog.GrupoAdministrativo virtual =
                new es.yaroki.educhronos.app.catalog.GrupoAdministrativo(
                        "OPT-4ESO", ESO1, TipoGrupo.VIRTUAL_OPTATIVA, null);

        assertThatThrownBy(() -> CatalogoMapper.aGrupo(virtual))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("VIRTUAL_OPTATIVA");
    }

    @Test
    void aTramos_excluyeRecreoNumeraPorDiaYSintetizaCodigo() {
        // Un lunes completo: 3 lectivos, recreo intercalado, otro lectivo; y un
        // martes con 1 lectivo. Orden global con hueco por el recreo, desordenado
        // de entrada para verificar que el mapper ordena.
        TramoSemanal l1 = tramo(Dia.LUNES, 8, 0, true, 1);
        TramoSemanal l2 = tramo(Dia.LUNES, 9, 0, true, 2);
        TramoSemanal recreo = tramo(Dia.LUNES, 10, 0, false, 3);
        TramoSemanal l3 = tramo(Dia.LUNES, 10, 30, true, 4);
        TramoSemanal m1 = tramo(Dia.MARTES, 8, 0, true, 5);

        List<Tramo> dominio = CatalogoMapper.aTramos(List.of(l3, recreo, m1, l1, l2));

        // El recreo desaparece: 3 lectivos del lunes + 1 del martes.
        assertThat(dominio).hasSize(4);

        // Lunes: ordenEnDia 1,2,3 (sin hueco por el recreo), diaSemana 1, codigos L1..L3.
        assertThat(dominio).extracting(Tramo::codigo).containsExactly("L1", "L2", "L3", "M1");
        assertThat(dominio.subList(0, 3)).allSatisfy(t -> assertThat(t.diaSemana()).isEqualTo(1));
        assertThat(dominio.subList(0, 3)).extracting(Tramo::ordenEnDia).containsExactly(1, 2, 3);

        // Martes: reinicia ordenEnDia a 1, diaSemana 2.
        Tramo martes = dominio.get(3);
        assertThat(martes.diaSemana()).isEqualTo(2);
        assertThat(martes.ordenEnDia()).isEqualTo(1);
    }

    private static TramoSemanal tramo(Dia dia, int hora, int minuto, boolean esLectivo, int orden) {
        return new TramoSemanal(
                dia, LocalTime.of(hora, minuto), LocalTime.of(hora, minuto).plusMinutes(55),
                esLectivo, orden, null);
    }
}
