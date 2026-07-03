package es.yaroki.educhronos.app.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.yaroki.educhronos.app.mapper.CatalogoMapper;
import es.yaroki.educhronos.solver.domain.RestriccionHoraria;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.time.LocalTime;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests unitarios de {@link CatalogoMapper#aRestriccionHoraria} (Fase 6, Bloque 7).
 * Puros: entidades JPA con {@code new}+setters, sin Spring ni BD.
 *
 * <p>Los índices que recibe el mapper se construyen CONSUMIENDO la salida del
 * propio mapper ({@code aProfesor}, {@code aTramos}), no fabricando tipos de
 * {@code solver.domain} con {@code new} (aprendizaje B4/B5). El índice de tramos
 * mapea la entidad {@code TramoSemanal} a su {@code Tramo} de dominio por
 * identidad de objeto, igual que hace {@code aProblemaHorario} internamente; aquí
 * se reconstruye día a día (cada tramo es el primer lectivo de su día, así el
 * código sintetizado coincide con el del ensamblado completo).
 */
class CatalogoMapperRestriccionTest {

    // ── Caso 1: mapeo feliz DURA con motivo presente ──────────────────────
    @Test
    void mapeoFelizDuraConMotivo() {
        Profesor jpaProf = new Profesor("MAT8", "María Martínez");
        TramoSemanal jpaTramo = tramo(Dia.LUNES, 1, true);
        ProfesorRestriccionHoraria entidad = new ProfesorRestriccionHoraria(
                jpaProf, jpaTramo, TipoRestriccion.DURA, 0, "No disponible a primera hora");

        RestriccionHoraria dominio = CatalogoMapper.aRestriccionHoraria(
                entidad, indiceProf(jpaProf), indiceTramos(jpaTramo));

        assertThat(dominio.profesor().codigo()).isEqualTo("MAT8");
        assertThat(dominio.tramo().codigo()).isEqualTo("L1");
        assertThat(dominio.tipo())
                .isEqualTo(es.yaroki.educhronos.solver.domain.TipoRestriccion.DURA);
        assertThat(dominio.peso()).isEqualTo(0);
        assertThat(dominio.motivo()).contains("No disponible a primera hora");
    }

    // ── Caso 2: mapeo feliz BLANDA con motivo ausente ─────────────────────
    @Test
    void mapeoFelizBlandaSinMotivo() {
        Profesor jpaProf = new Profesor("LEN2", "Lucía Lévy");
        TramoSemanal jpaTramo = tramo(Dia.MARTES, 3, true);
        ProfesorRestriccionHoraria entidad = new ProfesorRestriccionHoraria(
                jpaProf, jpaTramo, TipoRestriccion.BLANDA, 5, null); // sin motivo

        RestriccionHoraria dominio = CatalogoMapper.aRestriccionHoraria(
                entidad, indiceProf(jpaProf), indiceTramos(jpaTramo));

        assertThat(dominio.tipo())
                .isEqualTo(es.yaroki.educhronos.solver.domain.TipoRestriccion.BLANDA);
        assertThat(dominio.peso()).isEqualTo(5);
        assertThat(dominio.motivo()).isEmpty(); // Optional.ofNullable(null)
    }

    // ── Caso 3: profesor no indexado → aborta citando su código ───────────
    @Test
    void profesorHuerfanoAborta() {
        Profesor jpaProf = new Profesor("GHOST", "No indexado");
        TramoSemanal jpaTramo = tramo(Dia.LUNES, 1, true);
        ProfesorRestriccionHoraria entidad = new ProfesorRestriccionHoraria(
                jpaProf, jpaTramo, TipoRestriccion.DURA, 0, null);

        assertThatThrownBy(() -> CatalogoMapper.aRestriccionHoraria(
                entidad, Map.of(), indiceTramos(jpaTramo))) // profesores: vacío
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GHOST");
    }

    // ── Caso 4: restricción sobre un tramo de recreo (ausente del índice) ──
    @Test
    void tramoDeRecreoAborta() {
        Profesor jpaProf = new Profesor("MAT8", "María Martínez");
        TramoSemanal lectivo = tramo(Dia.LUNES, 1, true);
        TramoSemanal recreo = tramo(Dia.LUNES, 2, false); // aTramos lo excluye
        ProfesorRestriccionHoraria entidad = new ProfesorRestriccionHoraria(
                jpaProf, recreo, TipoRestriccion.DURA, 0, null);

        // El índice solo contiene el tramo lectivo; el recreo no está → aborta.
        assertThatThrownBy(() -> CatalogoMapper.aRestriccionHoraria(
                entidad, indiceProf(jpaProf), indiceTramos(lectivo)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MAT8");
    }

    // ── Caso 5: resolución por referencia con varios tramos en el índice ──
    @Test
    void resolucionPorReferenciaConVariosTramos() {
        Profesor jpaProf = new Profesor("MAT8", "María Martínez");
        TramoSemanal lunes = tramo(Dia.LUNES, 1, true);
        TramoSemanal martes = tramo(Dia.MARTES, 1, true);
        TramoSemanal miercoles = tramo(Dia.MIERCOLES, 1, true);
        // La restricción apunta al martes: debe resolverse a ese tramo, no a otro.
        ProfesorRestriccionHoraria entidad = new ProfesorRestriccionHoraria(
                jpaProf, martes, TipoRestriccion.BLANDA, 2, null);

        RestriccionHoraria dominio = CatalogoMapper.aRestriccionHoraria(
                entidad, indiceProf(jpaProf), indiceTramos(lunes, martes, miercoles));

        assertThat(dominio.tramo().codigo()).isEqualTo("M1");
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private static TramoSemanal tramo(Dia dia, int orden, boolean esLectivo) {
        return new TramoSemanal(dia, LocalTime.of(8, 0), LocalTime.of(9, 0), esLectivo, orden, null);
    }

    /** Índice {código → domain.Profesor} obtenido de la salida del mapper. */
    private static Map<String, es.yaroki.educhronos.solver.domain.Profesor> indiceProf(Profesor jpa) {
        return Map.of(jpa.getCodigo(), CatalogoMapper.aProfesor(jpa));
    }

    /**
     * Índice {entidad TramoSemanal → domain.Tramo} por identidad de objeto,
     * construido consumiendo {@code aTramos}. Cada entidad se mapea aislada (es el
     * único lectivo de su día en la llamada), de modo que su código coincide con
     * el que tendría en el ensamblado completo. Los tests usan días distintos.
     */
    private static Map<TramoSemanal, Tramo> indiceTramos(TramoSemanal... entidades) {
        Map<TramoSemanal, Tramo> indice = new IdentityHashMap<>();
        for (TramoSemanal t : entidades) {
            indice.put(t, CatalogoMapper.aTramos(List.of(t)).get(0));
        }
        return indice;
    }
}
