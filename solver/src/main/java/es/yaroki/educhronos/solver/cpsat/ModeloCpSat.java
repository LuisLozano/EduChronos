package es.yaroki.educhronos.solver.cpsat;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.IntervalVar;
import com.google.ortools.sat.Literal;
import com.google.ortools.util.Domain;
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
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Construye el modelo CP-SAT a partir de un {@link ProblemaHorario} y extrae
 * la {@link SolucionHorario} tras resolver.
 *
 * <p>Junto con {@link SolverHorario} es la única zona del módulo acoplada a
 * OR-Tools. Paquete-privada: el punto de entrada público es {@link SolverHorario}.
 *
 * <p><b>Alcance del solver mínimo (Fase 2, Bloque 4):</b> factibilidad pura,
 * sin función objetivo. Restricciones duras:
 * <ol>
 *   <li>No-solape de profesor (cuenta a TODOS los profesores de cada plaza).</li>
 *   <li>No-solape de aula (solo rama {@code aulaFija}; {@code aulasCandidatas}
 *       entra en Fase 3).</li>
 *   <li>No-solape de subgrupo.</li>
 *   <li>Distribución por día para actividades {@code DISTRIBUIDA}: a lo sumo
 *       una sesión de la actividad por día.</li>
 * </ol>
 * "Cada instancia en exactamente un tramo" y "recreo bloqueado" no son
 * restricciones: el {@code IntVar} toma un único valor por definición y el
 * recreo no es un {@link Tramo} (no entra en el dominio).
 */
final class ModeloCpSat {

    private final ProblemaHorario problema;
    private final CpModel model = new CpModel();
    private final List<InstanciaProgramada> instancias = new ArrayList<>();

    ModeloCpSat(ProblemaHorario problema) {
        this.problema = Objects.requireNonNull(problema, "problema no puede ser null");
    }

    CpModel model() {
        return model;
    }

    /** Construye variables y restricciones duras. Devuelve {@code this} para encadenar. */
    ModeloCpSat construir() {
        crearVariables();
        restriccionNoSolapeProfesor();
        restriccionNoSolapeAula();
        restriccionNoSolapeSubgrupo();
        restriccionNoSolapeGrupo(); // Fase 3
        restriccionDistribucionPorDia();
        return this;
    }

    // ----------------------------------------------------------------- variables

    private void crearVariables() {
        int numTramos = problema.tramos().size();
        if (numTramos == 0) {
            throw new HorarioInfactibleException("El problema no tiene tramos");
        }
        for (ActividadInstancia inst : Expansion.todas(problema)) {
            String nombre = inst.actividad().codigo() + "#" + inst.indice();
            IntVar tramoIndex = model.newIntVar(0, numTramos - 1L, "tramo_" + nombre);
            long duracion = inst.actividad().duracionTramos();
            // En Fase 2 duracion siempre es 1. newFixedSizeIntervalVar generaliza
            // a los bloques multi-tramo de Fase 5 (tamaño conocido, inicio variable).
            IntervalVar intervalo =
                    model.newFixedSizeIntervalVar(tramoIndex, duracion, "intv_" + nombre);
            instancias.add(new InstanciaProgramada(inst, tramoIndex, intervalo));
        }
    }

    // -------------------------------------------------------------- no-solapes

    private void restriccionNoSolapeProfesor() {
        for (Profesor profesor : problema.profesores()) {
            List<IntervalVar> intervalos = new ArrayList<>();
            for (InstanciaProgramada ip : instancias) {
                if (usaProfesor(ip, profesor)) {
                    intervalos.add(ip.intervalo());
                }
            }
            if (intervalos.size() >= 2) {
                model.addNoOverlap(intervalos.toArray(new IntervalVar[0]));
            }
        }
    }

    private void restriccionNoSolapeAula() {
        for (Aula aula : problema.aulas()) {
            List<IntervalVar> intervalos = new ArrayList<>();
            for (InstanciaProgramada ip : instancias) {
                if (usaAula(ip, aula)) {
                    intervalos.add(ip.intervalo());
                }
            }
            if (intervalos.size() >= 2) {
                model.addNoOverlap(intervalos.toArray(new IntervalVar[0]));
            }
        }
    }

    private void restriccionNoSolapeSubgrupo() {
        for (Subgrupo subgrupo : problema.subgrupos()) {
            List<IntervalVar> intervalos = new ArrayList<>();
            for (InstanciaProgramada ip : instancias) {
                if (cubreSubgrupo(ip, subgrupo)) {
                    intervalos.add(ip.intervalo());
                }
            }
            if (intervalos.size() >= 2) {
                model.addNoOverlap(intervalos.toArray(new IntervalVar[0]));
            }
        }
    }

    /**
     * Traslada la invariante I1 al solver: dos sesiones que toquen al mismo
     * {@link GrupoAdministrativo} no pueden caer en el mismo tramo. El JSON del
     * solver no transporta particiones, asi que I1 no se puede validar en la
     * carga; se impone aqui, sobre el grupo de cada subgrupo.
     *
     * <p>Igual que las otras tres, el intervalo de la instancia se anade una
     * sola vez por grupo: una actividad cuyas plazas cubren los cuatro grupos
     * de 1ESO (bloque CyR/OyD/RefMt) aporta UN intervalo a la lista de cada uno
     * de los cuatro grupos, sin pedir que se no-solape consigo misma. Lo que
     * queda prohibido es que OTRA actividad que toque ese grupo caiga en el
     * mismo tramo.
     *
     * <p>Es CIEGA al {@code grupoPadre}: agrupa por la identidad del grupo del
     * subgrupo, no por su padre. En Fase 4, un PDC (3ºADi) y su grupo padre
     * (3ºA) son grupos independientes para esta restriccion; sus sesiones
     * compartidas se modelan con una plaza que lista subgrupos de ambos, y sus
     * sesiones propias quedan libres.
     */
    private void restriccionNoSolapeGrupo() {
        for (GrupoAdministrativo grupo : problema.grupos()) {
            List<IntervalVar> intervalos = new ArrayList<>();
            for (InstanciaProgramada ip : instancias) {
                if (tocaGrupo(ip, grupo)) {
                    intervalos.add(ip.intervalo());
                }
            }
            if (intervalos.size() >= 2) {
                model.addNoOverlap(intervalos.toArray(new IntervalVar[0]));
            }
        }
    }

    /**
     * Una instancia "toca" un grupo si alguna plaza de su actividad tiene un
     * subgrupo cuyo grupo es ese (por identidad, no por padre). El intervalo se
     * cuenta una sola vez por grupo aunque varias plazas o subgrupos coincidan.
     */
    private boolean tocaGrupo(InstanciaProgramada ip, GrupoAdministrativo grupo) {
        for (Plaza plaza : ip.instancia().actividad().plazas()) {
            for (Subgrupo sg : plaza.subgrupos()) {
                if (sg.grupo().equals(grupo)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Una instancia usa al profesor si <i>alguna</i> de las plazas de su
     * actividad lo lista. El intervalo de la instancia se añade una sola vez
     * a la lista del profesor (clave para no pedir "no se solapa consigo
     * mismo", lo que haría el problema infactible).
     */
    private boolean usaProfesor(InstanciaProgramada ip, Profesor profesor) {
        for (Plaza plaza : ip.instancia().actividad().plazas()) {
            if (plaza.profesores().contains(profesor)) {
                return true;
            }
        }
        return false;
    }

    private boolean usaAula(InstanciaProgramada ip, Aula aula) {
        for (Plaza plaza : ip.instancia().actividad().plazas()) {
            // Solo aulaFija en Fase 2. aulasCandidatas: Fase 3 (decisión táctica del plan).
            if (plaza.aulaFija().isPresent() && plaza.aulaFija().get().equals(aula)) {
                return true;
            }
        }
        return false;
    }

    private boolean cubreSubgrupo(InstanciaProgramada ip, Subgrupo subgrupo) {
        for (Plaza plaza : ip.instancia().actividad().plazas()) {
            if (plaza.subgrupos().contains(subgrupo)) {
                return true;
            }
        }
        return false;
    }

    // --------------------------------------------------------- distribucion/dia

    /**
     * Para cada actividad {@code DISTRIBUIDA}, a lo sumo una de sus instancias
     * cae en un día dado. Se modela con un {@code BoolVar enDia} por
     * (instancia, día), reificado <i>iff</i> "el tramoIndex pertenece a ese
     * día", más un {@code addAtMostOne} por (actividad, día).
     *
     * <p>El día de un tramo se obtiene de su {@code diaSemana()}; no se asume
     * ni un número fijo de tramos por día ni que los tramos de un día sean
     * contiguos. El conjunto de índices de cada día se pasa como {@link Domain}
     * de valores explícitos.
     *
     * <p><b>Guarda anti-palomar (deuda D12):</b> si una actividad tiene más
     * repeticiones que días distintos, el reparto es imposible y la restricción
     * se omite. En Fase 2 ninguna actividad llega a ese caso. Revisar en Fase 5.
     */
    private void restriccionDistribucionPorDia() {
        List<Tramo> tramos = problema.tramos();
        int numTramos = tramos.size();

        // Índices de tramo agrupados por día.
        Map<Integer, List<Integer>> indicesPorDia = new TreeMap<>();
        for (int t = 0; t < numTramos; t++) {
            indicesPorDia.computeIfAbsent(tramos.get(t).diaSemana(), k -> new ArrayList<>()).add(t);
        }
        int numDias = indicesPorDia.size();

        // Dominios precomputados: para cada día, índices dentro y fuera.
        Map<Integer, Domain> dentroDelDia = new HashMap<>();
        Map<Integer, Domain> fueraDelDia = new HashMap<>();
        for (Map.Entry<Integer, List<Integer>> e : indicesPorDia.entrySet()) {
            Set<Integer> dentro = new HashSet<>(e.getValue());
            long[] in = new long[dentro.size()];
            long[] out = new long[numTramos - dentro.size()];
            int iIn = 0;
            int iOut = 0;
            for (int t = 0; t < numTramos; t++) {
                if (dentro.contains(t)) {
                    in[iIn++] = t;
                } else {
                    out[iOut++] = t;
                }
            }
            dentroDelDia.put(e.getKey(), Domain.fromValues(in));
            fueraDelDia.put(e.getKey(), Domain.fromValues(out));
        }

        for (Map.Entry<Actividad, List<InstanciaProgramada>> e : instanciasPorActividad().entrySet()) {
            Actividad actividad = e.getKey();
            if (actividad.patronTemporal() != PatronTemporal.DISTRIBUIDA) {
                continue;
            }
            if (actividad.repeticionesPorSemana() > numDias) {
                continue; // guarda anti-palomar (D12)
            }

            // enDia[día] -> literales de las instancias de esta actividad.
            Map<Integer, List<Literal>> literalesPorDia = new TreeMap<>();
            for (InstanciaProgramada ip : e.getValue()) {
                for (Integer dia : indicesPorDia.keySet()) {
                    BoolVar enDia = model.newBoolVar(
                            "enDia_" + actividad.codigo() + "#" + ip.instancia().indice() + "_d" + dia);
                    // enDia == 1  <=>  tramoIndex pertenece al día.
                    model.addLinearExpressionInDomain(ip.tramoIndex(), dentroDelDia.get(dia))
                            .onlyEnforceIf(enDia);
                    model.addLinearExpressionInDomain(ip.tramoIndex(), fueraDelDia.get(dia))
                            .onlyEnforceIf(enDia.not());
                    literalesPorDia.computeIfAbsent(dia, k -> new ArrayList<>()).add(enDia);
                }
            }
            // A lo sumo una instancia de la actividad por día.
            for (List<Literal> literales : literalesPorDia.values()) {
                if (literales.size() >= 2) {
                    model.addAtMostOne(literales.toArray(new Literal[0]));
                }
            }
        }
    }

    private Map<Actividad, List<InstanciaProgramada>> instanciasPorActividad() {
        Map<Actividad, List<InstanciaProgramada>> out = new LinkedHashMap<>();
        for (InstanciaProgramada ip : instancias) {
            out.computeIfAbsent(ip.instancia().actividad(), k -> new ArrayList<>()).add(ip);
        }
        return out;
    }

    // ------------------------------------------------------------- extraccion

    /** Lee los valores de las variables resueltas y construye la SolucionHorario. */
    SolucionHorario extraerSolucion(CpSolver solver) {
        Map<ActividadInstancia, Tramo> asignaciones = new LinkedHashMap<>();
        for (InstanciaProgramada ip : instancias) {
            int idx = (int) solver.value(ip.tramoIndex());
            asignaciones.put(ip.instancia(), problema.tramos().get(idx));
        }
        return new SolucionHorario(asignaciones);
    }
}
