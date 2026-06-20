package es.yaroki.educhronos.solver.cpsat;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.IntervalVar;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.LinearExpr;
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
 * <p><b>Alcance:</b> factibilidad pura, sin función objetivo. Restricciones
 * duras, en el orden en que las aplica {@code construir()}:
 * <ol>
 *   <li>No-solape de profesor (cuenta a TODOS los profesores de cada plaza).</li>
 *   <li>No-solape de aula (rama {@code aulaFija} e intervalos opcionales de
 *       {@code aulasCandidatas}, sobre el aula efectivamente elegida).</li>
 *   <li>No-solape de subgrupo.</li>
 *   <li>No-solape de grupo (S9, ciega al {@code grupoPadre}; impone I1 en el
 *       solver porque las particiones no viajan en el JSON). Añadida en Fase 3.</li>
 *   <li>Distribución por día para actividades {@code DISTRIBUIDA}: a lo sumo
 *       una sesión de la actividad por día.</li>
 * </ol>
 * "Cada instancia en exactamente un tramo" y "recreo bloqueado" no son
 * restricciones: el {@code IntVar} toma un único valor por definición y el
 * recreo no es un {@link Tramo} (no entra en el dominio).
 */
final class ModeloCpSat {

    /**
     * Peso de la penalización por ventanas del profesorado (decisión 6a:
     * constante hardcodeada; cuando concurran varios términos blandos con pesos
     * relativos relevantes, se evaluará introducirlos por configuración —
     * ampliando {@code ProblemaHorario}). Con un único término su valor absoluto
     * es irrelevante: solo importa que sea positivo para que el objetivo tense
     * las cotas (ver D17).
     */
    private static final long PESO_VENTANAS = 1L;

    private final ProblemaHorario problema;
    private final CpModel model = new CpModel();
    private final List<InstanciaProgramada> instancias = new ArrayList<>();

    /**
     * Andamiaje genérico de la función objetivo: términos blandos ya ponderados
     * (expresión × peso). El modo con objetivo los suma y minimiza. Vacío en el
     * modo de factibilidad pura. Bloques futuros (D11 indisponibilidades,
     * distribución blanda, etc.) añaden sus términos aquí sin tocar el ensamblado.
     */
    private final List<LinearArgument> terminosObjetivo = new ArrayList<>();

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

    /**
     * Construye el modelo en modo OPTIMIZACIÓN: las mismas restricciones duras
     * que {@link #construir()} más la función objetivo de restricciones blandas.
     * En Fase 5, Bloque 6a el único término es la penalización de ventanas del
     * profesorado (criterio 4 de Fase 5).
     *
     * <p>{@link #construir()} se mantiene intacto y separado a propósito
     * (decisión 1a): los tests de escala miden tiempo hasta primera solución
     * factible en factibilidad pura; añadir el objetivo a ese camino los
     * invalidaría. Aquí vive el camino de optimización; allí, el de factibilidad.
     *
     * @return {@code this} para encadenar.
     */
    ModeloCpSat construirConObjetivo() {
        construir();
        objetivoVentanasProfesor();
        ensamblarObjetivo();
        return this;
    }

    /** Suma los términos blandos ponderados y los fija como objetivo a minimizar. */
    private void ensamblarObjetivo() {
        if (terminosObjetivo.isEmpty()) {
            return; // sin términos: equivalente a factibilidad pura
        }
        model.minimize(LinearExpr.sum(terminosObjetivo.toArray(new LinearArgument[0])));
    }

    /**
     * Penalización por ventanas (huecos) del profesorado — Forma A, cotas
     * tensadas. Por cada (profesor, día) con ≥1 clase, el número de huecos es
     * {@code (ultimaPos − primeraPos + 1) − nClases}: los tramos lectivos libres
     * intercalados entre la primera y la última clase del día. El recreo no
     * cuenta (no es un {@link Tramo}; los tramos de un día son contiguos en
     * {@code ordenEnDia}).
     *
     * <p><b>Cotas tensadas (D17):</b> {@code primero} y {@code ultimo} se acotan
     * (primero ≤ posición de cada clase; ultimo ≥ posición de cada clase) en vez
     * de fijarse con {@code addMinEquality}/{@code addMaxEquality}. Como el
     * objetivo MINIMIZA los huecos con peso positivo, el solver tensa
     * {@code primero} hacia su mayor valor admisible (= primera clase) y
     * {@code ultimo} hacia su menor admisible (= última clase), dando el span
     * exacto. Esto es correcto SOLO mientras el término se minimice con peso > 0;
     * si un futuro término compitiera y dejara estas cotas flojas, habría que
     * pasar a min/max explícito. Registrado como deuda D17.
     *
     * <p>El "día sin clases" se gestiona con {@code tieneClase}: si es 0, los
     * huecos del (profesor, día) se fuerzan a 0 y no se imponen las cotas.
     */
    private void objetivoVentanasProfesor() {
        List<Tramo> tramos = problema.tramos();
        int numTramos = tramos.size();

        // Índices de tramo y posiciones (ordenEnDia) agrupados por día.
        Map<Integer, List<Integer>> indicesPorDia = new TreeMap<>();
        for (int t = 0; t < numTramos; t++) {
            indicesPorDia.computeIfAbsent(tramos.get(t).diaSemana(), k -> new ArrayList<>()).add(t);
        }

        for (Profesor profesor : problema.profesores()) {
            // Instancias que usan a este profesor (una sola vez por instancia).
            List<InstanciaProgramada> suyas = new ArrayList<>();
            for (InstanciaProgramada ip : instancias) {
                if (usaProfesor(ip, profesor)) {
                    suyas.add(ip);
                }
            }
            if (suyas.size() < 2) {
                continue; // 0 ó 1 sesión en toda la semana: no puede haber ventana
            }

            for (Map.Entry<Integer, List<Integer>> e : indicesPorDia.entrySet()) {
                int dia = e.getKey();
                List<Integer> idxDelDia = e.getValue();           // índices planos
                if (idxDelDia.size() < 2) {
                    continue; // con <2 tramos en el día no cabe ninguna ventana
                }

                // ocupa[t] == 1  <=>  alguna instancia suya cae en el tramo t.
                // posiciones (ordenEnDia) de los tramos del día, alineadas con ocupa.
                List<BoolVar> ocupaDelDia = new ArrayList<>();
                List<Integer> posDelDia = new ArrayList<>();
                int minPos = Integer.MAX_VALUE;
                int maxPos = Integer.MIN_VALUE;

                for (int t : idxDelDia) {
                    int pos = tramos.get(t).ordenEnDia();
                    posDelDia.add(pos);
                    minPos = Math.min(minPos, pos);
                    maxPos = Math.max(maxPos, pos);

                    BoolVar ocupa = model.newBoolVar(
                            "ocupa_" + profesor.codigo() + "_d" + dia + "_t" + t);
                    // Literales "instancia i cae en el tramo t".
                    List<Literal> instEnT = new ArrayList<>();
                    Domain soloT = Domain.fromValues(new long[] {t});
                    for (InstanciaProgramada ip : suyas) {
                        BoolVar enT = model.newBoolVar(
                                "instEnT_" + profesor.codigo() + "_d" + dia
                                        + "_t" + t + "_" + ip.instancia().actividad().codigo()
                                        + "#" + ip.instancia().indice());
                        model.addLinearExpressionInDomain(ip.tramoIndex(), soloT)
                                .onlyEnforceIf(enT);
                        Domain noT = complemento(t, numTramos);
                        model.addLinearExpressionInDomain(ip.tramoIndex(), noT)
                                .onlyEnforceIf(enT.not());
                        instEnT.add(enT);
                    }
                    // ocupa == OR(instEnT). Por el no-solape de profesor, a lo sumo
                    // un literal es verdadero; modelar como OR es correcto igual.
                    model.addBoolOr(instEnT.toArray(new Literal[0])).onlyEnforceIf(ocupa);
                    for (Literal l : instEnT) {
                        model.addImplication(l, ocupa);
                    }
                    // ocupa == 0  =>  todos los instEnT == 0 (cierre del iff).
                    Literal[] negados = new Literal[instEnT.size()];
                    for (int k = 0; k < instEnT.size(); k++) {
                        negados[k] = instEnT.get(k).not();
                    }
                    model.addBoolAnd(negados).onlyEnforceIf(ocupa.not());

                    ocupaDelDia.add(ocupa);
                }

                // nClases = suma de ocupa del día.
                LinearArgument nClases =
                        LinearExpr.sum(ocupaDelDia.toArray(new LinearArgument[0]));

                // tieneClase == OR(ocupaDelDia).
                BoolVar tieneClase = model.newBoolVar(
                        "tieneClase_" + profesor.codigo() + "_d" + dia);
                model.addBoolOr(ocupaDelDia.toArray(new Literal[0])).onlyEnforceIf(tieneClase);
                for (BoolVar oc : ocupaDelDia) {
                    model.addImplication(oc, tieneClase);
                }
                Literal[] ocupaNeg = new Literal[ocupaDelDia.size()];
                for (int k = 0; k < ocupaDelDia.size(); k++) {
                    ocupaNeg[k] = ocupaDelDia.get(k).not();
                }
                model.addBoolAnd(ocupaNeg).onlyEnforceIf(tieneClase.not());

                // primero/ultimo en [minPos, maxPos]; cotas tensadas por ocupa.
                IntVar primero = model.newIntVar(minPos, maxPos,
                        "primero_" + profesor.codigo() + "_d" + dia);
                IntVar ultimo = model.newIntVar(minPos, maxPos,
                        "ultimo_" + profesor.codigo() + "_d" + dia);
                for (int k = 0; k < ocupaDelDia.size(); k++) {
                    int pos = posDelDia.get(k);
                    // ocupa[k] => primero <= pos  y  ultimo >= pos
                    model.addLessOrEqual(primero, pos).onlyEnforceIf(ocupaDelDia.get(k));
                    model.addGreaterOrEqual(ultimo, pos).onlyEnforceIf(ocupaDelDia.get(k));
                }

                // huecos >= 0; huecos == (ultimo - primero + 1) - nClases si tieneClase,
                // huecos == 0 si !tieneClase.
                IntVar huecos = model.newIntVar(0, maxPos - minPos,
                        "huecos_" + profesor.codigo() + "_d" + dia);
                // span = ultimo - primero + 1  (expresion lineal)
                LinearArgument spanMenosClases = LinearExpr.newBuilder()
                        .add(ultimo)
                        .addTerm(primero, -1)
                        .add(1)
                        .addTerm(nClases, -1)
                        .build();
                model.addEquality(huecos, spanMenosClases).onlyEnforceIf(tieneClase);
                model.addEquality(huecos, 0).onlyEnforceIf(tieneClase.not());

                terminosObjetivo.add(LinearExpr.term(huecos, PESO_VENTANAS));
            }
        }
    }

    /** Dominio con todos los índices de tramo salvo {@code t}. Para reificar "no está en t". */
    private Domain complemento(int t, int numTramos) {
        long[] otros = new long[numTramos - 1];
        int j = 0;
        for (int x = 0; x < numTramos; x++) {
            if (x != t) {
                otros[j++] = x;
            }
        }
        return Domain.fromValues(otros);
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
            Map<Plaza, List<AulaOpcion>> opciones =
                    crearOpcionesDeAula(inst, tramoIndex, duracion, nombre);
            instancias.add(new InstanciaProgramada(inst, tramoIndex, intervalo, opciones));
        }
    }

    /**
     * Por cada plaza de la instancia con {@code aulasCandidatas}, crea una
     * {@link AulaOpcion} por candidata (intervalo OPCIONAL atado al
     * {@code tramoIndex} compartido) y un {@code addExactlyOne} sobre sus
     * literales de presencia: el solver elige exactamente una aula por plaza.
     * Las plazas con {@code aulaFija} no entran (su aula no es variable).
     */
    private Map<Plaza, List<AulaOpcion>> crearOpcionesDeAula(
            ActividadInstancia inst, IntVar tramoIndex, long duracion, String nombre) {
        Map<Plaza, List<AulaOpcion>> porPlaza = new LinkedHashMap<>();
        for (Plaza plaza : inst.actividad().plazas()) {
            if (plaza.aulasCandidatas().isEmpty()) {
                continue; // plaza con aulaFija: aula no variable
            }
            List<AulaOpcion> opciones = new ArrayList<>();
            List<Literal> presencias = new ArrayList<>();
            for (Aula aula : plaza.aulasCandidatas()) {
                BoolVar presencia = model.newBoolVar(
                        "aula_" + nombre + "_" + plaza.codigo() + "_" + aula.codigo());
                IntervalVar opcional = model.newOptionalFixedSizeIntervalVar(
                        tramoIndex, duracion, presencia,
                        "intvAula_" + nombre + "_" + plaza.codigo() + "_" + aula.codigo());
                opciones.add(new AulaOpcion(aula, presencia, opcional));
                presencias.add(presencia);
            }
            model.addExactlyOne(presencias.toArray(new Literal[0]));
            porPlaza.put(plaza, opciones);
        }
        return porPlaza;
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
                    intervalos.add(ip.intervalo());        // rama aulaFija (como en Fase 2)
                }
                intervalos.addAll(intervalosOpcionalesEn(ip, aula)); // rama aulasCandidatas
            }
            if (intervalos.size() >= 2) {
                model.addNoOverlap(intervalos.toArray(new IntervalVar[0]));
            }
        }
    }

    /** Intervalos opcionales de la instancia cuyas opciones apuntan a este aula. */
    private List<IntervalVar> intervalosOpcionalesEn(InstanciaProgramada ip, Aula aula) {
        List<IntervalVar> out = new ArrayList<>();
        for (List<AulaOpcion> opciones : ip.opcionesDeAula().values()) {
            for (AulaOpcion opcion : opciones) {
                if (opcion.aula().equals(aula)) {
                    out.add(opcion.intervalo());
                }
            }
        }
        return out;
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
                if (sg.grupos().contains(grupo)) {
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
            // Solo rama aulaFija. Las aulasCandidatas las aporta a la lista de
            // no-solape el helper intervalosOpcionalesEn (intervalos opcionales).
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
        Map<ActividadInstancia, Map<Plaza, Aula>> aulasElegidas = new LinkedHashMap<>();
        for (InstanciaProgramada ip : instancias) {
            int idx = (int) solver.value(ip.tramoIndex());
            asignaciones.put(ip.instancia(), problema.tramos().get(idx));

            Map<Plaza, Aula> porPlaza = new LinkedHashMap<>();
            for (Map.Entry<Plaza, List<AulaOpcion>> e : ip.opcionesDeAula().entrySet()) {
                for (AulaOpcion opcion : e.getValue()) {
                    if (solver.booleanValue(opcion.presencia())) {
                        porPlaza.put(e.getKey(), opcion.aula());
                        break; // addExactlyOne garantiza una sola presente
                    }
                }
            }
            if (!porPlaza.isEmpty()) {
                aulasElegidas.put(ip.instancia(), porPlaza);
            }
        }
        return new SolucionHorario(asignaciones, aulasElegidas);
    }
}
