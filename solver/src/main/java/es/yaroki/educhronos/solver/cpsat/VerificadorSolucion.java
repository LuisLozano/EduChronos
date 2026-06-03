package es.yaroki.educhronos.solver.cpsat;

import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.Aula;
import es.yaroki.educhronos.solver.domain.GrupoAdministrativo;
import es.yaroki.educhronos.solver.domain.PatronTemporal;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Subgrupo;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Verifica, de forma <b>independiente del solver</b>, que una
 * {@link SolucionHorario} cumple las restricciones duras del modelo.
 *
 * <p>No depende de OR-Tools: recorre la solución y comprueba propiedades sobre
 * el dominio. Esa independencia es deliberada — si la verificación reutilizara
 * el modelo CP-SAT, un fallo en el modelo pasaría desapercibido. La usan el
 * test del Bloque 4 y la salida a consola del Bloque 5.
 *
 * <p>Comprobaciones: todas las instancias colocadas; no-solape de profesor,
 * aula, subgrupo y grupo (S9, espejo de la quinta restricción dura del solver;
 * agrupa por subgrupo.grupo() directo, ciega al grupoPadre); distribución por
 * día de las actividades {@code DISTRIBUIDA} (con la misma guarda anti-palomar
 * D12 que el modelo).
 */
public final class VerificadorSolucion {

    public ResultadoVerificacion verificar(ProblemaHorario problema, SolucionHorario solucion) {
        List<String> violaciones = new ArrayList<>();
        List<ActividadInstancia> esperadas = Expansion.todas(problema);

        verificarTodasColocadas(esperadas, solucion, violaciones);
        verificarNoSolapes(esperadas, solucion, violaciones);
        verificarDistribucion(problema, esperadas, solucion, violaciones);

        return new ResultadoVerificacion(violaciones);
    }

    private void verificarTodasColocadas(List<ActividadInstancia> esperadas,
                                         SolucionHorario solucion,
                                         List<String> violaciones) {
        for (ActividadInstancia inst : esperadas) {
            if (solucion.tramoDeInstancia(inst).isEmpty()) {
                violaciones.add("Instancia sin colocar: " + etiqueta(inst));
            }
        }
    }

    private void verificarNoSolapes(List<ActividadInstancia> esperadas,
                                    SolucionHorario solucion,
                                    List<String> violaciones) {
        Map<Tramo, List<ActividadInstancia>> porTramo = new LinkedHashMap<>();
        for (ActividadInstancia inst : esperadas) {
            solucion.tramoDeInstancia(inst).ifPresent(tramo ->
                    porTramo.computeIfAbsent(tramo, k -> new ArrayList<>()).add(inst));
        }

        for (Map.Entry<Tramo, List<ActividadInstancia>> entrada : porTramo.entrySet()) {
            Tramo tramo = entrada.getKey();

            Map<Profesor, Integer> profesores = new HashMap<>();
            Map<Aula, Integer> aulas = new HashMap<>();
            Map<Subgrupo, Integer> subgrupos = new HashMap<>();
            Map<GrupoAdministrativo, Integer> grupos = new HashMap<>();

            for (ActividadInstancia inst : entrada.getValue()) {
                // Set por instancia: si dos plazas listan el mismo recurso, la
                // instancia lo ocupa una vez, no dos. Correcto para profesor y
                // subgrupo (S1 permite un profesor en varias plazas de la misma
                // actividad). Para AULA es una debilidad conocida: por S2 dos
                // plazas de la misma instancia con la misma aula son colisión, no
                // uso compartido (el uso compartido legítimo es UNA plaza con
                // varios profesores, no varias plazas con un aula). El Set la
                // enmascara. Debilidad preexistente en aulaFija, no introducida
                // por el aula variable; el solver sí la previene vía addNoOverlap.
                // Reforzar el verificador (conteo de aula por plaza) queda pendiente.
                Set<Profesor> ps = new HashSet<>();
                Set<Aula> as = new HashSet<>();
                Set<Subgrupo> ss = new HashSet<>();
                for (Plaza plaza : inst.actividad().plazas()) {
                    ps.addAll(plaza.profesores());
                    solucion.aulaElegida(inst, plaza).ifPresent(as::add);
                    ss.addAll(plaza.subgrupos());
                }
                // Grupo derivado de los subgrupos de la instancia. Como ss ya es
                // un Set por instancia, varios subgrupos del mismo grupo en la
                // misma actividad (desdoble) colapsan a un único grupo: la
                // instancia ocupa el grupo una vez. Espejo de S9 en el solver,
                // ciega al grupoPadre (agrupa por subgrupo.grupo() directo).
                Set<GrupoAdministrativo> gs = new HashSet<>();
                ss.forEach(s -> gs.add(s.grupo()));

                ps.forEach(p -> profesores.merge(p, 1, Integer::sum));
                as.forEach(a -> aulas.merge(a, 1, Integer::sum));
                ss.forEach(s -> subgrupos.merge(s, 1, Integer::sum));
                gs.forEach(g -> grupos.merge(g, 1, Integer::sum));
            }

            reportarColisiones("Profesor", tramo, profesores, Profesor::codigo, violaciones);
            reportarColisiones("Aula", tramo, aulas, Aula::codigo, violaciones);
            reportarColisiones("Subgrupo", tramo, subgrupos, Subgrupo::codigo, violaciones);
            reportarColisiones("Grupo", tramo, grupos, GrupoAdministrativo::codigo, violaciones);
        }
    }

    private <T> void reportarColisiones(String tipo,
                                        Tramo tramo,
                                        Map<T, Integer> conteo,
                                        Function<T, String> codigo,
                                        List<String> violaciones) {
        for (Map.Entry<T, Integer> e : conteo.entrySet()) {
            if (e.getValue() > 1) {
                violaciones.add(tipo + " " + codigo.apply(e.getKey())
                        + " usado " + e.getValue() + " veces en el tramo " + tramo.codigo());
            }
        }
    }

    private void verificarDistribucion(ProblemaHorario problema,
                                       List<ActividadInstancia> esperadas,
                                       SolucionHorario solucion,
                                       List<String> violaciones) {
        int numDias = (int) problema.tramos().stream()
                .map(Tramo::diaSemana)
                .distinct()
                .count();

        Map<Actividad, List<ActividadInstancia>> porActividad = new LinkedHashMap<>();
        for (ActividadInstancia inst : esperadas) {
            porActividad.computeIfAbsent(inst.actividad(), k -> new ArrayList<>()).add(inst);
        }

        for (Map.Entry<Actividad, List<ActividadInstancia>> e : porActividad.entrySet()) {
            Actividad actividad = e.getKey();
            if (actividad.patronTemporal() != PatronTemporal.DISTRIBUIDA) {
                continue;
            }
            if (actividad.repeticionesPorSemana() > numDias) {
                continue; // misma guarda anti-palomar (D12) que el modelo
            }
            Set<Integer> diasUsados = new HashSet<>();
            for (ActividadInstancia inst : e.getValue()) {
                Optional<Tramo> tramo = solucion.tramoDeInstancia(inst);
                if (tramo.isEmpty()) {
                    continue; // ya reportado por verificarTodasColocadas
                }
                int dia = tramo.get().diaSemana();
                if (!diasUsados.add(dia)) {
                    violaciones.add("Actividad " + actividad.codigo()
                            + " tiene dos sesiones el mismo día (diaSemana=" + dia + ")");
                }
            }
        }
    }

    private String etiqueta(ActividadInstancia inst) {
        return inst.actividad().codigo() + "#" + inst.indice();
    }
}
