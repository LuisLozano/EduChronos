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
import es.yaroki.educhronos.solver.domain.RestriccionHoraria;
import es.yaroki.educhronos.solver.domain.TipoRestriccion;
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
    /**
     * Peso de la penalización por incumplir una indisponibilidad BLANDA del
     * profesorado (Fase 5, Bloque 6c). Constante hardcodeada, igual que
     * {@link #PESO_VENTANAS}. Valor 1 = mismo peso que una ventana: decisión
     * consciente de NO calibrar el peso relativo sin datos del centro (gemela de
     * la decisión de no fijar el umbral del criterio 4 en S25). La parametrización
     * de ambos pesos y su calibración con un fixture multi-término se difiere
     * (deuda de pesos blandos). Cada fixture de 6c aísla un único término, así que
     * el peso relativo no afecta a lo que los tests demuestran.
     */
    private static final long PESO_INDISP_BLANDA = 1L;

    /**
     * Número máximo de sesiones consecutivas (sin hueco) de un profesor en un
     * mismo día que NO se penalizan; a partir de la (N+1)-ésima consecutiva, cada
     * sesión adicional de la racha suma una unidad al objetivo (Fase 5, Bloque
     * 6d-c). Constante hardcodeada, igual que {@link #PESO_VENTANAS} y
     * {@link #PESO_INDISP_BLANDA}.
     *
     * <p>Valor 3: la jornada real son 6 tramos lectivos partidos por el recreo en
     * dos bloques de 3 (8-9, 9-10, 10-11 | 11:30-12:30, 12:30-13:30, 13:30-14:30),
     * así que penalizar más de 3 seguidas encaja con esa estructura. Es una
     * conjetura razonable, NO un requisito verificado con el centro: ningún dato
     * confirma "los profesores no quieren más de 3 seguidas". Su parametrización y
     * calibración se difieren (deuda D21, gemela de la decisión de no fijar el
     * umbral del criterio 4 sin datos). Cada fixture de 6d-c usa este N=3 de
     * producción.
     */
    private static final int MAX_CONSECUTIVAS = 3;
    /**
     * Peso de la penalización por exceso de sesiones consecutivas del profesorado
     * (Fase 5, Bloque 6d-c). Constante hardcodeada a 1, mismo criterio que los
     * otros pesos blandos; calibración relativa diferida (deuda D21).
     */
    private static final long PESO_CONSECUTIVAS = 1L;
    /**
     * Umbral de poda de aulas candidatas (Fase 5, Bloque 16, palanca (b) de la
     * deuda D23). Una plaza con MÁS de {@code UMBRAL_PODA_AULA} aulas candidatas
     * se considera de "cola larga" y se poda a {@link #MAX_AULAS_PODA} candidatas;
     * las plazas con {@code <=} este umbral se dejan intactas (su margen es
     * pequeño y podarlas arriesga factibilidad sin reducir apenas el espacio de
     * búsqueda). La poda SOLO actúa en el régimen de optimización
     * ({@link #construirConObjetivo()}); en factibilidad pura ({@link #construir()})
     * no se aplica, para no alterar la curva de escala que cerró el criterio 1 de
     * Fase 5 en S36. Constante hardcodeada, misma decisión que los pesos blandos:
     * calibración con datos del centro diferida (deuda D21, ampliada).
     *
     * <p>Valor 8: sobre el instituto completo la distribución de tamaños de
     * candidatas es {2,3,4,25}; cualquier umbral en [4,24] toca solo las plazas de
     * 25 (modalidades de 2ºBach, el foco de D23) y respeta las pequeñas.
     */
    private static final int UMBRAL_PODA_AULA = 8;
    /**
     * Número de aulas candidatas que conserva la poda en una plaza de cola larga
     * (Fase 5, Bloque 16, palanca (b) de D23). Se conservan las {@code K} primeras
     * por orden de código de aula (determinista y auditable; NO inventa
     * preferencias del centro). El límite inferior seguro lo fija la saturación: K
     * debe bastar para colocar sin solape todas las plazas podadas que coincidan en
     * un tramo; un K demasiado pequeño produce INFEASIBLE (caracterizado en el
     * fixture de oro de este bloque). Suelo de saturación medido sobre el instituto
     * completo: 3 (máximo de plazas de cola larga simultáneas en un tramo, acotado
     * por el no-solape de grupo y de profesor; las 21 plazas de 25 candidatas son
     * modalidades de 2ºBach y casi todas se bloquean entre sí). K=8 mantiene margen
     * ×2,6 sobre ese suelo y reduce 25→8 (−68% de presencias en esas plazas).
     * Constante hardcodeada (deuda D21).
     */
    private static final int MAX_AULAS_PODA = 8;
    /**
     * Activa la poda de aulas candidatas. Falso por defecto (régimen de
     * factibilidad pura); {@link #construirConObjetivo()} lo pone a verdadero antes
     * de construir, de modo que la poda solo afecta al camino de optimización.
     */
    private boolean podarAulas = false;
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
        restriccionIndisponibilidadProfesor(); // Fase 5, Bloque 6b (solo DURA)
        return this;
    }

    /**
     * Construye el modelo en modo OPTIMIZACIÓN: las mismas restricciones duras
     * que {@link #construir()} más la función objetivo de restricciones blandas.
     * Los términos son la penalización de ventanas del profesorado (6a) y la
     * penalización por incumplir indisponibilidades BLANDA del profesorado (6c).
     *
     * <p>{@link #construir()} se mantiene intacto y separado a propósito
     * (decisión 1a): los tests de escala miden tiempo hasta primera solución
     * factible en factibilidad pura; añadir el objetivo a ese camino los
     * invalidaría. Aquí vive el camino de optimización; allí, el de factibilidad.
     *
     * @return {@code this} para encadenar.
     */
    ModeloCpSat construirConObjetivo() {
        this.podarAulas = true; // Fase 5, Bloque 16 (D23 palanca b): poda solo en optimización
        construir();
        objetivoVentanasProfesor();
        objetivoIndisponibilidadBlandaProfesor(); // Fase 5, Bloque 6c
        objetivoConsecutivasProfesor(); // Fase 5, Bloque 6d-c
        ensamblarObjetivo();
        return this;
    }

    /**
     * Siembra una solución factible como <i>hint</i> (arranque en caliente) de
     * CP-SAT, vía {@code CpModel#addHint} sobre las variables de decisión
     * primarias del modelo (Fase 5, Bloque 15b, deuda D23, palanca que NO degrada
     * calidad). Debe invocarse tras {@code construirConObjetivo()} (o
     * {@code construir()}), con las variables ya creadas.
     *
     * <p><b>Qué se siembra y qué no.</b> Solo las dos familias de variables de
     * decisión que {@link #extraerSolucion} lee al revés:
     * <ol>
     *   <li>{@code tramoIndex} de cada instancia ← índice del {@link Tramo} que la
     *       semilla le asignó.</li>
     *   <li>{@code presencia} de cada {@link AulaOpcion} ← 1 en la opción cuya aula
     *       coincide con la elegida por la semilla para esa (instancia, plaza), 0 en
     *       las demás. Solo existen en plazas con {@code aulasCandidatas}; las de
     *       {@code aulaFija} no tienen variable de aula que sembrar.</li>
     * </ol>
     * Los {@code IntervalVar} ({@code intervalo} y los opcionales por aula) NO se
     * siembran: están anclados a {@code tramoIndex} y a {@code presencia} por
     * construcción ({@code newFixedSizeIntervalVar} / {@code newOptionalFixedSize…}),
     * así que quedan determinados. Las variables auxiliares del objetivo
     * (primero/ultimo/huecos/penaliza/excede) tampoco se siembran: CP-SAT las deriva
     * del resto del hint durante el preprocesado.
     *
     * <p><b>Es un hint, no una restricción.</b> CP-SAT lo trata como sugerencia de
     * solución inicial; si la semilla es factible (lo es: viene de {@code resolver()}
     * sobre el mismo problema), la adopta como punto de partida en vez de buscar una
     * factible desde cero. El presolve puede transformar variables y descartar parte
     * del hint silenciosamente: el hint orienta, no garantiza. Una instancia ausente
     * en la semilla (tramo sin asignar) simplemente no aporta hint para esa variable.
     *
     * <p><b>Inverso de {@link #extraerSolucion}.</b> Recorre la misma lista
     * {@code instancias} y usa la misma identidad ({@code ip.instancia()}) para
     * mapear cada variable a su valor en la semilla, garantizando que el hint cae
     * sobre exactamente las variables que el extractor lee.
     *
     * @param semilla solución factible previa (típicamente de {@code resolver()}).
     * @return {@code this} para encadenar.
     */
    ModeloCpSat sembrarHint(SolucionHorario semilla) {
        Objects.requireNonNull(semilla, "semilla no puede ser null");
        for (InstanciaProgramada ip : instancias) {
            ActividadInstancia inst = ip.instancia();
            semilla.tramoDeInstancia(inst).ifPresent(
                    tramo -> model.addHint(ip.tramoIndex(), problema.indiceDeTramo(tramo)));
            for (Map.Entry<Plaza, List<AulaOpcion>> e : ip.opcionesDeAula().entrySet()) {
                Aula elegida = semilla.aulaElegida(inst, e.getKey()).orElse(null);
                if (elegida == null) {
                    continue; // sin dato de aula para esta plaza en la semilla
                }
                for (AulaOpcion opcion : e.getValue()) {
                    model.addHint(opcion.presencia(), opcion.aula().equals(elegida) ? 1 : 0);
                }
            }
        }
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
     * <p><b>D17, primer competidor analizado (6c):</b> el término de
     * indisponibilidad BLANDA ({@link #objetivoIndisponibilidadBlandaProfesor})
     * penaliza el {@code tramoIndex} de instancias concretas y NO toca
     * {@code primero}/{@code ultimo}/{@code huecos} ni el span. Es separable de
     * este término: no existe holgura por la que minimizar la blanda haga
     * preferible un span inflado. Las cotas tensadas siguen siendo correctas. La
     * deuda permanece viva para términos FUTUROS que sí miren posiciones (p.ej.
     * primeras/últimas horas).
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

    /**
     * Indisponibilidades horarias del profesorado (Fase 5, Bloque 6b).
     * Por cada restricción DURA, el profesor no puede ocupar el tramo: ninguna
     * instancia cuya actividad lo liste en alguna plaza puede caer en ese tramo.
     * Como todas las plazas de una instancia comparten {@code tramoIndex}, la
     * prohibición es a nivel de instancia (igual que {@code usaProfesor}).
     *
     * <p>Las restricciones BLANDA se ignoran aquí: en 6b se cargan y validan (I/O)
     * pero el solver no las consume. Su régimen (término del objetivo) se difiere
     * al Bloque 6c.
     *
     * <p>Es una restricción DURA: vive en {@code construir()} y por tanto aplica
     * en ambos regímenes (factibilidad pura y optimización). Si los tramos vetados
     * de un profesor cubren todos los tramos donde podría ir una clase suya, el
     * dominio permitido queda vacío y el problema es INFEASIBLE — respuesta
     * correcta; la detección temprana de esa situación es validación de
     * configuración (deuda D18), no trabajo del modelo CP-SAT.
     */
    private void restriccionIndisponibilidadProfesor() {
        int numTramos = problema.tramos().size();
        Map<Profesor, Set<Integer>> prohibidosPorProfesor = new LinkedHashMap<>();
        for (RestriccionHoraria r : problema.restriccionesHorarias()) {
            if (r.tipo() != TipoRestriccion.DURA) {
                continue; // BLANDA: cargada y validada, no consumida en 6b
            }
            int idx = problema.indiceDeTramo(r.tramo());
            prohibidosPorProfesor
                    .computeIfAbsent(r.profesor(), k -> new HashSet<>())
                    .add(idx);
        }
        for (Map.Entry<Profesor, Set<Integer>> e : prohibidosPorProfesor.entrySet()) {
            Profesor profesor = e.getKey();
            Domain permitido = complementoDe(e.getValue(), numTramos);
            for (InstanciaProgramada ip : instancias) {
                if (usaProfesor(ip, profesor)) {
                    model.addLinearExpressionInDomain(ip.tramoIndex(), permitido);
                }
            }
        }
    }

    /** Dominio con todos los índices de tramo salvo los del conjunto {@code prohibidos}. */
    private Domain complementoDe(Set<Integer> prohibidos, int numTramos) {
        long[] otros = new long[numTramos - prohibidos.size()];
        int j = 0;
        for (int x = 0; x < numTramos; x++) {
            if (!prohibidos.contains(x)) {
                otros[j++] = x;
            }
        }
        return Domain.fromValues(otros);
    }

    /**
     * Penalización por incumplir indisponibilidades BLANDA del profesorado
     * (Fase 5, Bloque 6c). Gemelo blando de
     * {@link #restriccionIndisponibilidadProfesor()}: donde aquella PROHÍBE el
     * tramo (restricción dura), esta lo PENALIZA (término del objetivo).
     *
     * <p>Por cada {@link RestriccionHoraria} de tipo {@code BLANDA} y cada
     * instancia que use a ese profesor (mismo criterio {@link #usaProfesor}), se
     * crea un literal {@code penaliza} reificado como "esta instancia cae en el
     * tramo vetado-blando". La suma de esos literales, ponderada por
     * {@link #PESO_INDISP_BLANDA}, se añade a {@link #terminosObjetivo}.
     *
     * <p>No comparte los literales de {@link #objetivoVentanasProfesor()} a
     * propósito: aquel reifica "ocupa tramo t" solo para los tramos de cada día y
     * solo para profesores con ≥2 sesiones; este necesita un único tramo concreto
     * (el vetado) para cualquier profesor con restricción blanda, tenga 1 ó N
     * sesiones. Mantenerlos separados es lo que hace que los dos términos sean
     * independientes (ver dictamen D17).
     *
     * <p>Como todas las plazas de una instancia comparten {@code tramoIndex}, la
     * penalización es a nivel de instancia, no de plaza (igual que la variante
     * DURA). Si dos restricciones BLANDA del mismo profesor vetan tramos
     * distintos, cada una aporta su propio literal por instancia; una instancia
     * que caiga en uno u otro penaliza una vez por cada coincidencia, que es lo
     * correcto (incumple dos preferencias distintas si cayera en ambas, imposible
     * al ser una sola instancia en un solo tramo).
     */
    private void objetivoIndisponibilidadBlandaProfesor() {
        int numTramos = problema.tramos().size();
        for (RestriccionHoraria r : problema.restriccionesHorarias()) {
            if (r.tipo() != TipoRestriccion.BLANDA) {
                continue; // DURA: la consume restriccionIndisponibilidadProfesor()
            }
            Profesor profesor = r.profesor();
            int tramoVetado = problema.indiceDeTramo(r.tramo());
            Domain soloVetado = Domain.fromValues(new long[] {tramoVetado});
            Domain noVetado = complemento(tramoVetado, numTramos);

            for (InstanciaProgramada ip : instancias) {
                if (!usaProfesor(ip, profesor)) {
                    continue;
                }
                BoolVar penaliza = model.newBoolVar(
                        "penalBlanda_" + profesor.codigo() + "_t" + tramoVetado
                                + "_" + ip.instancia().actividad().codigo()
                                + "#" + ip.instancia().indice());
                // penaliza == 1  <=>  la instancia cae en el tramo vetado.
                model.addLinearExpressionInDomain(ip.tramoIndex(), soloVetado)
                        .onlyEnforceIf(penaliza);
                model.addLinearExpressionInDomain(ip.tramoIndex(), noVetado)
                        .onlyEnforceIf(penaliza.not());
                terminosObjetivo.add(LinearExpr.term(penaliza, PESO_INDISP_BLANDA));
            }
        }
    }

    /**
     * Penalización por exceso de sesiones CONSECUTIVAS del profesorado — Forma de
     * ventanas deslizantes (Fase 5, Bloque 6d-c). Por cada (profesor, día) y cada
     * inicio de una ventana de {@code MAX_CONSECUTIVAS + 1} posiciones contiguas en
     * {@code ordenEnDia}, un literal {@code excede} reificado como "las N+1 están
     * todas ocupadas". La suma de esos literales, ponderada por
     * {@link #PESO_CONSECUTIVAS}, se añade a {@link #terminosObjetivo}.
     *
     * <p><b>Por qué esto = "suma de excesos sobre N":</b> una racha maximal de
     * longitud L contiene exactamente {@code max(0, L − N)} subventanas de tamaño
     * N+1 enteramente ocupadas, así que la suma de literales {@code excede} de un
     * día es {@code Σ_rachas max(0, L − N)}. Recomputo gemelo independiente en
     * {@link VerificadorSolucion#contarPenalizacionConsecutivasProfesor} (cuenta
     * rachas maximales, no ventanas deslizantes; debe coincidir).
     *
     * <p><b>Contigüidad sobre {@code ordenEnDia}, no sobre índice de tramo:</b> la
     * ventana deslizante se forma sobre las posiciones consecutivas
     * {@code pos, pos+1, …, pos+N} dentro de un día. El recreo no rompe racha (no
     * es un {@link Tramo}; los tramos lectivos de un día son contiguos en
     * {@code ordenEnDia}, igual que en el término de ventanas y en el verificador).
     *
     * <p><b>D17 (cotas tensadas de ventanas) NO se reactiva:</b> este término NO
     * mira primero/último/span ni las {@code IntVar primero}/{@code ultimo} del
     * término de ventanas. Solo usa literales de ocupación por posición
     * ({@code ocupa}) reconstruidos localmente y un {@code addBoolAnd} reificado
     * por ventana; no introduce holgura que afecte a las cotas tensadas de
     * {@link #objetivoVentanasProfesor}. Como la indisponibilidad blanda (6c), es
     * separable del término de ventanas. No comparte literales con él a propósito
     * (igual decisión que 6c), para mantener cada término auditable por su lado; el
     * coste de reconstruir {@code ocupa} es irrelevante a esta escala.
     *
     * <p>No usa ninguna API de CP-SAT nueva respecto a 6a/6c:
     * {@code newBoolVar}, {@code addLinearExpressionInDomain(...).onlyEnforceIf},
     * {@code Domain.fromValues}, {@code complemento}, {@code addBoolOr},
     * {@code addBoolAnd}, {@code addImplication}, {@code LinearExpr.term} ya están
     * en uso.
     */
    private void objetivoConsecutivasProfesor() {
        List<Tramo> tramos = problema.tramos();
        int numTramos = tramos.size();

        // Índices de tramo agrupados por día (mismo patrón que ventanas).
        Map<Integer, List<Integer>> indicesPorDia = new TreeMap<>();
        for (int t = 0; t < numTramos; t++) {
            indicesPorDia.computeIfAbsent(tramos.get(t).diaSemana(), k -> new ArrayList<>()).add(t);
        }

        for (Profesor profesor : problema.profesores()) {
            List<InstanciaProgramada> suyas = new ArrayList<>();
            for (InstanciaProgramada ip : instancias) {
                if (usaProfesor(ip, profesor)) {
                    suyas.add(ip);
                }
            }
            if (suyas.size() <= MAX_CONSECUTIVAS) {
                continue; // con ≤N sesiones en TODA la semana no puede haber racha de N+1
            }

            for (Map.Entry<Integer, List<Integer>> e : indicesPorDia.entrySet()) {
                int dia = e.getKey();
                List<Integer> idxDelDia = e.getValue();
                if (idxDelDia.size() < MAX_CONSECUTIVAS + 1) {
                    continue; // el día no tiene N+1 tramos: ninguna racha de N+1 cabe
                }

                // ocupa[t] (por índice de tramo del día) y su posición ordenEnDia.
                // Map ordenEnDia -> BoolVar ocupa, para localizar ventanas contiguas.
                Map<Integer, BoolVar> ocupaPorPos = new TreeMap<>();
                for (int t : idxDelDia) {
                    int pos = tramos.get(t).ordenEnDia();
                    BoolVar ocupa = model.newBoolVar(
                            "ocupaC_" + profesor.codigo() + "_d" + dia + "_t" + t);
                    // ocupa == 1  <=>  alguna instancia suya cae en el tramo t.
                    List<Literal> instEnT = new ArrayList<>();
                    Domain soloT = Domain.fromValues(new long[] {t});
                    for (InstanciaProgramada ip : suyas) {
                        BoolVar enT = model.newBoolVar(
                                "instEnTC_" + profesor.codigo() + "_d" + dia
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
                    Literal[] negados = new Literal[instEnT.size()];
                    for (int k = 0; k < instEnT.size(); k++) {
                        negados[k] = instEnT.get(k).not();
                    }
                    model.addBoolAnd(negados).onlyEnforceIf(ocupa.not());

                    ocupaPorPos.put(pos, ocupa);
                }

                // Ventanas deslizantes de tamaño N+1 sobre posiciones CONTIGUAS.
                // Por cada inicio 'p' tal que existen ocupa en p, p+1, ..., p+N,
                // excede[p] == AND(esas N+1 ocupa).
                List<Integer> posOrdenadas = new ArrayList<>(ocupaPorPos.keySet());
                for (int posInicio : posOrdenadas) {
                    // ¿están las N+1 posiciones contiguas presentes en el día?
                    List<BoolVar> ventana = new ArrayList<>();
                    boolean completa = true;
                    for (int d = 0; d <= MAX_CONSECUTIVAS; d++) {
                        BoolVar oc = ocupaPorPos.get(posInicio + d);
                        if (oc == null) {
                            completa = false;
                            break;
                        }
                        ventana.add(oc);
                    }
                    if (!completa) {
                        continue; // ventana de N+1 contiguos no existe desde aquí
                    }
                    BoolVar excede = model.newBoolVar(
                            "excedeC_" + profesor.codigo() + "_d" + dia + "_p" + posInicio);
                    // excede == 1  <=>  las N+1 ocupa de la ventana valen 1.
                    // (a) excede => cada ocupa de la ventana
                    for (BoolVar oc : ventana) {
                        model.addImplication(excede, oc);
                    }
                    // (b) AND(ventana) => excede (cierre del iff): si alguna ocupa
                    //     es 0, excede es 0; si todas 1, excede 1.
                    model.addBoolAnd(ventana.toArray(new Literal[0])).onlyEnforceIf(excede);
                    Literal[] negVent = new Literal[ventana.size()];
                    for (int k = 0; k < ventana.size(); k++) {
                        negVent[k] = ventana.get(k).not();
                    }
                    // excede == 0  <=>  al menos una ocupa de la ventana es 0.
                    model.addBoolOr(negVent).onlyEnforceIf(excede.not());

                    terminosObjetivo.add(LinearExpr.term(excede, PESO_CONSECUTIVAS));
                }
            }
        }
    }

    // ----------------------------------------------------------------- variables

    /**
     * {@code ordenEnDia} del primer tramo lectivo posterior al recreo. La jornada
     * del centro son 6 tramos partidos por el recreo en dos bloques de 3
     * (8-9, 9-10, 10-11 | 11:30-12:30, 12:30-13:30, 13:30-14:30), de modo que la
     * frontera del recreo cae entre {@code ordenEnDia=3} y {@code ordenEnDia=4}.
     *
     * <p>El recreo NO es un {@link Tramo} (no consume un valor de {@code ordenEnDia}):
     * los seis tramos lectivos numeran 1..6 sin hueco. Por eso "ordenEnDia
     * consecutivo" NO basta para impedir que un bloque cruce el recreo (3 y 4 son
     * consecutivos); hace falta esta frontera explícita.
     *
     * <p><b>Constante de estructura de jornada (deuda D22):</b> asume el recreo
     * tras el tercer tramo. El resto del módulo ya asume esta estructura
     * (ver {@link #MAX_CONSECUTIVAS}). Si un centro tuviera otra colocación del
     * recreo habría que parametrizarla; es trabajo de la UI de configuración
     * (Fase 8). Hoy ningún dato real la contradice.
     */
    private static final int ORDEN_TRAS_RECREO = 4;

    /**
     * Calcula los índices de tramo desde los que un bloque de {@code duracion}
     * tramos puede arrancar sin desbordar el día ni cruzar el recreo (D13).
     *
     * <p>Un inicio {@code t} es válido si los {@code duracion} tramos que ocuparía
     * existen todos en el mismo día con {@code ordenEnDia} consecutivos
     * ({@code s, s+1, …, s+duracion-1}) y el bloque NO contiene a la vez los
     * tramos {@code ORDEN_TRAS_RECREO-1} y {@code ORDEN_TRAS_RECREO} (no cruza el
     * recreo). Reproduce la S6 del modelo de papel (§6.6), que prohíbe por
     * construcción que un bloque ≥2 cruce la frontera de día o de recreo.
     *
     * <p>El cálculo es genérico sobre {@code diaSemana()}/{@code ordenEnDia()}: no
     * asume un número fijo de tramos por día (salvo la frontera del recreo, ver
     * {@link #ORDEN_TRAS_RECREO}). Para {@code duracion == 1} todo tramo es válido.
     */
    private Domain iniciosValidosDeBloque(long duracion) {
        List<Tramo> tramos = problema.tramos();
        // (diaSemana, ordenEnDia) -> índice plano, para resolver sucesores.
        Map<Integer, Map<Integer, Integer>> porDiaOrden = new HashMap<>();
        for (int t = 0; t < tramos.size(); t++) {
            Tramo tr = tramos.get(t);
            porDiaOrden
                    .computeIfAbsent(tr.diaSemana(), k -> new HashMap<>())
                    .put(tr.ordenEnDia(), t);
        }

        List<Long> validos = new ArrayList<>();
        for (int t = 0; t < tramos.size(); t++) {
            Tramo inicio = tramos.get(t);
            Map<Integer, Integer> ordenesDelDia = porDiaOrden.get(inicio.diaSemana());
            boolean ok = true;
            for (int i = 0; i < duracion; i++) {
                int orden = inicio.ordenEnDia() + i;
                if (!ordenesDelDia.containsKey(orden)) {
                    ok = false; // desborda el día (o hueco de orden inexistente)
                    break;
                }
                // Cruce de recreo: el bloque no puede contener orden (TRAS-1) y orden TRAS.
                if (orden == ORDEN_TRAS_RECREO
                        && inicio.ordenEnDia() <= ORDEN_TRAS_RECREO - 1) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                validos.add((long) t);
            }
        }
        long[] arr = new long[validos.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = validos.get(i);
        }
        return Domain.fromValues(arr);
    }

    private void crearVariables() {
        int numTramos = problema.tramos().size();
        if (numTramos == 0) {
            throw new HorarioInfactibleException("El problema no tiene tramos");
        }
        for (ActividadInstancia inst : Expansion.todas(problema)) {
            String nombre = inst.actividad().codigo() + "#" + inst.indice();
            IntVar tramoIndex = model.newIntVar(0, numTramos - 1L, "tramo_" + nombre);
            long duracion = inst.actividad().duracionTramos();
            // D13: un bloque de duracion>1 no puede desbordar el día ni cruzar el
            // recreo. Se restringe el dominio del inicio a los tramos desde los
            // que el bloque cabe entero en el día sin saltar la frontera del
            // recreo. Para duracion==1 la lista blanca es todos los tramos
            // (no-op), así que los datasets de Fase 2-4 no cambian de dominio.
            if (duracion > 1) {
                model.addLinearExpressionInDomain(tramoIndex, iniciosValidosDeBloque(duracion));
            }
            // newFixedSizeIntervalVar generaliza a los bloques multi-tramo
            // (tamaño conocido, inicio variable). El no-solape sobre el índice
            // plano ya impide solapes correctos; la lista blanca de arriba impide
            // además los inicios físicamente imposibles (D13).
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
            for (Aula aula : candidatasPodadas(plaza)) {
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

    /**
     * Aplica la poda de aulas candidatas (Fase 5, Bloque 16, palanca (b) de D23).
     * Si la poda no está activa, o la plaza tiene {@code <= UMBRAL_PODA_AULA}
     * candidatas, devuelve el conjunto íntegro (orden de iteración del Set, igual
     * que antes del bloque). Si la plaza supera el umbral, conserva las
     * {@link #MAX_AULAS_PODA} primeras por orden de código de aula: determinista,
     * auditable y estable entre ejecuciones (a diferencia del orden de un HashSet).
     *
     * <p>El recorte reduce el número de {@code BoolVar} de presencia y de
     * intervalos opcionales que el solver debe decidir; sobre el instituto
     * completo, las plazas de cola larga (modalidades de 2ºBach con 25 candidatas)
     * concentran el grueso de ese espacio. Riesgo: un recorte excesivo puede
     * volver INFEASIBLE un problema antes factible por saturación de aulas en un
     * tramo (caracterizado en el fixture de oro de este bloque).
     */
    private List<Aula> candidatasPodadas(Plaza plaza) {
        if (!podarAulas || plaza.aulasCandidatas().size() <= UMBRAL_PODA_AULA) {
            return new ArrayList<>(plaza.aulasCandidatas());
        }
        return plaza.aulasCandidatas().stream()
                .sorted(java.util.Comparator.comparing(Aula::codigo))
                .limit(MAX_AULAS_PODA)
                .collect(java.util.stream.Collectors.toList());
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
