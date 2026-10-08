package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.web.dto.CuadreDTO;
import es.yaroki.educhronos.app.web.dto.CuadreEntidadDTO;
import es.yaroki.educhronos.solver.cpsat.ReglaDura;
import es.yaroki.educhronos.solver.cpsat.VerificadorSolucion;
import es.yaroki.educhronos.solver.cpsat.Violacion;
import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.Aula;
import es.yaroki.educhronos.solver.domain.GrupoAdministrativo;
import es.yaroki.educhronos.solver.domain.PatronTemporal;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.RestriccionHoraria;
import es.yaroki.educhronos.solver.domain.SesionBloqueada;
import es.yaroki.educhronos.solver.domain.Subgrupo;
import es.yaroki.educhronos.solver.domain.TipoRestriccion;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pre-validación de CONDICIONES NECESARIAS del catálogo (Fase 8, Bloque 8.4-A,
 * deuda D18): comprobaciones de conteo que, si fallan, garantizan que no existe
 * horario posible —o avisan de que probablemente no exista— ANTES de gastar el
 * presupuesto del solver.
 *
 * <p><b>La cuarta regla (S8) REUTILIZA la del solver, no la reimplementa.</b> Las
 * tres primeras son aritmética de conteo propia de esta capa; S8 ya está escrita —y
 * probada— en {@link VerificadorSolucion}, y desde S146 esa clase expone
 * {@code verificarTutorias(ProblemaHorario)}, un punto de entrada que NO pide
 * {@link es.yaroki.educhronos.solver.domain.SolucionHorario} porque S8 es propiedad del
 * CATÁLOGO. Aquí solo se traducen sus {@link Violacion} a {@link AvisoPrevalidacion}. NO
 * es un espejo: si fuera una segunda implementación caería en la familia D-F8.2b-iv-a
 * (misma regla escrita dos veces, que divergen en silencio). El verificador se instancia
 * con {@code new} —igual que en {@code DiagnosticoService}—, no es un bean; aquí el campo
 * es {@code static} porque el núcleo que lo usa lo es.
 *
 * <p><b>Por qué S8 es AVISO y no ERROR.</b> Por DOS razones independientes:
 * <ol>
 *   <li>No implica infactibilidad: el solver coloca perfectamente una actividad tutorial
 *       cuyo profesor no es tutor. Abortar sería un falso positivo sobre un problema
 *       resoluble, justo lo que el criterio de {@link Severidad} prohíbe.</li>
 *   <li>No depende de la COLOCACIÓN. El horario generado es válido en todo lo demás, y
 *       la violación se corrige cambiando el tutor del grupo —un
 *       {@code PUT /api/grupos/&#123;id&#125;/tutoria}— SIN regenerar nada. Bloquear la
 *       generación por esto empujaría al usuario a falsear tutores para poder generar,
 *       que es peor que el dato que se quería proteger.</li>
 * </ol>
 *
 * <p><b>Por qué demanda 1 y disponible 0.</b> {@link AvisoPrevalidacion} contractualiza
 * que las reglas de capacidad se emiten cuando {@code demanda > disponible} (y que la
 * igualdad NO es fallo). S8 no es una comparación de conteo, así que se codifica su cardinalidad real:
 * hace falta UN tutor principal que imparta la actividad, y hay CERO. 1 &gt; 0 respeta el
 * contrato sin inventar una aritmética que no existe.
 *
 * <p><b>Qué NO es.</b> No es una validación de integridad del catálogo (huérfanos,
 * códigos duplicados): eso ya lo hace {@link es.yaroki.educhronos.app.mapper.CatalogoMapper}
 * al mapear. Aquí el catálogo YA es referencialmente sano. Las reglas de CAPACIDAD —(a),
 * (c) y (d)— comparan DEMANDA contra DISPONIBILIDAD y fallan si la demanda la supera. S8
 * no compara nada: es una propiedad del catálogo, y por eso avisa en vez de abortar. Las
 * de CUADRE (S203) comparan las horas de CLASE configuradas con las DECLARADAS y avisan si
 * no son iguales, por exceso o por defecto: también son AVISO, porque no impiden que haya
 * horario y se corrigen editando el catálogo. Las de capacidad son condiciones
 * NECESARIAS, no suficientes: pasarlas no garantiza que el problema sea factible. La (f) sí es una
 * infactibilidad garantizada: un pin sobre un tramo DURA de un profesor de la sesión
 * pinada. La (e), un cortafuegos contra restricciones horarias con bloques (S165), se
 * retiró en S166, cuando el solver pasó a vetar y penalizar todos los tramos que ocupa
 * cada sesión; la letra no se reutiliza.
 *
 * <p><b>Por qué delega en {@link GeneradorHorarioService#cargarProblema()}</b> en vez
 * de cargar el catálogo por su cuenta: mismo motivo que {@link DiagnosticoService}.
 * Ese método es {@code @Transactional(readOnly = true)} y lee los bloqueos vigentes
 * DENTRO de la misma transacción que el resto del catálogo, porque
 * {@code BloqueoMapper} cruza el pin de tramo por IDENTIDAD DE OBJETO contra un
 * {@code IdentityHashMap}; una carga propia daría instancias {@code TramoSemanal}
 * distintas y perdería el pin SIN EXCEPCIÓN (S62). NO reimplementar la carga aquí.
 * Se delega por método público; NO se heredan sus repositorios
 * (D-F8.2b-iii-A-a: 12 repos inyectados).
 *
 * <p><b>Por qué el núcleo es estático.</b> {@link #prevalidar(ProblemaHorario, DatosCuadre)} es un
 * método estático puro, y {@code GeneradorHorarioService.generar()} lo invoca así, sin
 * inyectar este bean. Dos razones: (1) inyectarlo crearía un CICLO de beans —este
 * servicio ya depende de {@code GeneradorHorarioService} para cargar—, que Spring Boot
 * rechaza al arrancar; (2) evita añadir una decimotercera dependencia al servicio que
 * ya arrastra la deuda D-F8.2b-iii-A-a. El núcleo estático es además la ÚNICA
 * implementación de las reglas: el endpoint y la generación entran por ahí, uno
 * cargando el problema y otro reutilizando el que ya tiene en la mano. Ninguna regla
 * se escribe dos veces (familia D-F8.2b-iv-a).
 *
 * <p><b>Palomar de aulas: fuera de alcance</b> por decisión explícita de S79. Lo que sí entra
 * desde S207 es más estrecho y exacto: una clase sin ninguna aula posible y una actividad cuyas
 * plazas simultáneas no caben en aulas distintas (emparejamiento, no palomar global). Comparar
 * plazas forzosamente simultáneas contra aulas compatibles produce falsos positivos
 * probables con el catálogo actual (aulas candidatas amplias, aula fija implícita), y
 * un falso positivo en un ERROR bloquea un problema resoluble.
 */
@Service
public class PrevalidacionService {

    /** Un profesor necesita más tramos de docencia de los que tiene libres. ERROR. */
    public static final String REGLA_PROFESOR_SOBRECARGADO = "PROFESOR_SOBRECARGADO";

    /** Una actividad DISTRIBUIDA se repite más veces que días lectivos hay. ERROR. */
    public static final String REGLA_REPETICIONES_EXCEDEN_DIAS = "REPETICIONES_EXCEDEN_DIAS";

    /** Un grupo tiene más horas curriculares que tramos lectivos. ERROR. */
    public static final String REGLA_GRUPO_SOBRECARGADO = "GRUPO_SOBRECARGADO";

    /**
     * Un pin (bloqueo de tramo) coloca una sesión en un tramo en el que alguno de sus
     * profesores tiene una restricción DURA. ERROR, ver {@link #pinSobreTramoDura}.
     */
    public static final String REGLA_PIN_SOBRE_TRAMO_DURA = "PIN_SOBRE_TRAMO_DURA";

    /**
     * Una actividad {@code requiereTutor} no la imparte ningún TUTOR_PRINCIPAL de un
     * grupo que cubre (S8, §4.6). AVISO. El identificador NO se escribe a mano: se toma
     * del enum del solver, que es quien nombra la regla, para que la constante y la
     * {@link Violacion#regla()} que se está traduciendo no puedan divergir.
     */
    public static final String REGLA_TUTORIA_SIN_TUTOR = ReglaDura.TUTORIA_SIN_TUTOR.name();

    /**
     * Las horas de CLASE configuradas de un profesor con total declarado no son las
     * declaradas (S203, C-totales-y-cargo). AVISO, ver {@link #profesoresDescuadrados}.
     */
    public static final String REGLA_PROFESOR_HORAS_DESCUADRADAS = "PROFESOR_HORAS_DESCUADRADAS";

    /**
     * Las horas configuradas de un grupo con total declarado no son las declaradas (S203).
     * AVISO, ver {@link #gruposDescuadrados}.
     */
    public static final String REGLA_GRUPO_HORAS_DESCUADRADAS = "GRUPO_HORAS_DESCUADRADAS";

    /**
     * Una plaza de CLASE se queda sin ninguna aula posible tras aplicar las reglas de aulas (S207,
     * C-deduccion-aulas, B1). ERROR, ver {@link #clasesSinAulaPosible}.
     */
    public static final String REGLA_CLASE_SIN_AULA_POSIBLE = "CLASE_SIN_AULA_POSIBLE";

    /**
     * Las plazas de una actividad, que ocurren a la vez, no pueden recibir aulas distintas (S207,
     * B2). ERROR, ver {@link #repartoDeAulasImposible}.
     */
    public static final String REGLA_REPARTO_DE_AULAS_IMPOSIBLE = "REPARTO_DE_AULAS_IMPOSIBLE";

    /**
     * Las clases que solo pueden ir a un conjunto de aulas suman más tramos que los que esas aulas
     * tienen en la semana (S209, T4). ERROR, ver {@link #cargaDeAulasExcedida}.
     */
    public static final String REGLA_CARGA_DE_AULAS_EXCEDIDA = "CARGA_DE_AULAS_EXCEDIDA";

    /** Filtro de actividades de las reglas de capacidad: todas cuentan, también REUNION y FUNCION. */
    private static final Predicate<Actividad> TODAS = actividad -> true;

    /**
     * Verificador del solver, dueño de la implementación de S8. {@code new} y no bean
     * (patrón de {@code DiagnosticoService}); {@code static} porque el núcleo
     * {@link #prevalidar(ProblemaHorario, DatosCuadre)} que lo usa es estático. No guarda estado.
     */
    private static final VerificadorSolucion VERIFICADOR = new VerificadorSolucion();

    private final GeneradorHorarioService generadorService;

    public PrevalidacionService(GeneradorHorarioService generadorService) {
        this.generadorService = generadorService;
    }

    /**
     * Carga el catálogo y lo pre-valida. Devuelve la lista COMPLETA de hallazgos
     * (ERROR y AVISO); NO lanza excepción aunque haya errores: quien decide abortar es
     * el llamante ({@code GeneradorHorarioService.generar()}), no esta consulta. El
     * endpoint {@code GET /api/prevalidacion} la expone tal cual.
     *
     * <p>Transaccional de solo lectura por la misma frontera que {@code cargarProblema()}
     * (ver javadoc de clase); el cómputo en sí corre sobre un POJO ya desligado de JPA.
     */
    @Transactional(readOnly = true)
    public List<AvisoPrevalidacion> prevalidar() {
        return prevalidar(generadorService.cargarProblema(), generadorService.cargarDatosCuadre(),
                generadorService.cargarDatosAulas());
    }

    /**
     * Carga el catálogo y devuelve el cuadre de horas de todos los profesores y grupos
     * ({@code GET /api/prevalidacion/cuadre}, S203). Misma frontera transaccional que
     * {@link #prevalidar()}; el cálculo es el núcleo estático {@link #cuadre(ProblemaHorario, DatosCuadre)}.
     */
    @Transactional(readOnly = true)
    public CuadreDTO cuadre() {
        return cuadre(generadorService.cargarProblema(), generadorService.cargarDatosCuadre());
    }

    /**
     * NÚCLEO: todas las comprobaciones sobre un {@link ProblemaHorario} ya cargado.
     * Puro y estático —ni repositorios, ni transacción, ni estado—, para que
     * {@code GeneradorHorarioService.generar()} lo llame con el problema que YA cargó,
     * sin volver a leer el catálogo y sin inyectar este bean (ver javadoc de clase).
     *
     * <p>{@code datos} trae lo que el cuadre necesita y el problema no lleva (S203). El GET y
     * la generación pasan los de {@code GeneradorHorarioService.cargarDatosCuadre()}; con
     * {@link DatosCuadre#VACIO} las reglas de cuadre no emiten nada.
     *
     * <p>El orden de salida es estable. Primero los ERROR: profesores, actividades, grupos,
     * pines sobre DURA, clases sin aula posible y repartos de aulas imposibles (S207). Después
     * los AVISO: tutorías (S8), cuadre de profesores y cuadre de
     * grupos. Dentro de cada bloque, el orden del problema. Así los hallazgos que abortan la
     * generación quedan agrupados al principio de la lista.
     */
    public static List<AvisoPrevalidacion> prevalidar(ProblemaHorario problema, DatosCuadre datos) {
        return prevalidar(problema, datos, DatosAulas.VACIO);
    }

    /**
     * Como el de arriba, con los datos de aulas que el problema no lleva (S207): qué plazas de
     * CLASE se han quedado sin aula posible y por qué. Los ERROR de aulas van tras los pines.
     */
    public static List<AvisoPrevalidacion> prevalidar(
            ProblemaHorario problema, DatosCuadre datos, DatosAulas aulas) {
        Objects.requireNonNull(problema, "problema no puede ser null");
        Objects.requireNonNull(datos, "datos no puede ser null");
        Objects.requireNonNull(aulas, "aulas no puede ser null");

        // Ambos techos salen del PROBLEMA, nunca de una constante: el catálogo real trae
        // los tramos ya sin recreos (CatalogoMapper los excluye) y los días que existan.
        // Hardcodear 30 y 5 crearía otro espejo de la familia D22/D30.
        int tramosLectivos = problema.tramos().size();
        int diasLectivos = (int) problema.tramos().stream()
                .map(Tramo::diaSemana).distinct().count();

        List<AvisoPrevalidacion> avisos = new ArrayList<>();
        avisos.addAll(sobrecargaProfesor(problema, tramosLectivos, TODAS));
        avisos.addAll(repeticionesExcedenDias(problema, diasLectivos));
        avisos.addAll(sobrecargaGrupo(problema, tramosLectivos, TODAS));
        avisos.addAll(pinSobreTramoDura(problema));
        avisos.addAll(clasesSinAulaPosible(aulas));
        avisos.addAll(repartoDeAulasImposible(problema, aulas));
        avisos.addAll(cargaDeAulasExcedida(problema, tramosLectivos, soloClase(datos)));
        avisos.addAll(tutoriasSinTutor(problema));
        avisos.addAll(profesoresDescuadrados(problema, datos));
        avisos.addAll(gruposDescuadrados(problema, datos));
        return List.copyOf(avisos);
    }

    /**
     * (a) SOBRECARGA DE PROFESOR — ERROR. Falla si un profesor debe impartir más tramos
     * de los que le quedan libres.
     *
     * <p>La disponibilidad se computa como (tramos lectivos − restricciones DURA). Que
     * guardias y reducciones se declaren como DURA es DECISIÓN DEL ARQUITECTO (S79), no
     * dato derivado: los volcados de docs/horario-referencia/ no contienen ocupación no
     * docente y no pueden contenerla (sus fuentes son rejillas por grupo y por aula; una
     * guardia no ocupa ninguno de los dos). Si el centro no las declara, esta
     * comprobación produce falsos NEGATIVOS, nunca falsos positivos.
     *
     * <p><b>La demanda se cuenta por ACTIVIDAD, no por plaza</b> (decisión S79). Las
     * plazas de una actividad OCURREN SIMULTÁNEAMENTE (javadoc de {@code domain.Actividad})
     * y comparten un único {@code tramoIndex} por instancia en {@code ModeloCpSat}: un
     * profesor que figura en dos plazas de la misma actividad ocupa UN tramo, no dos.
     * Sumar por plaza sobrestimaría y podría bloquear con un 422 un problema resoluble
     * —falso positivo—, justo la dirección que esta regla debe evitar. Es la misma
     * deduplicación que (c), con el mismo helper {@link #tramosQueOcupa}.
     *
     * <p>Las restricciones DURA se cuentan por TRAMO DISTINTO: dos filas DURA sobre el
     * mismo tramo no restan dos veces.
     *
     * <p>Cuenta TODAS las actividades ({@code filtro} es {@link #TODAS}): una REUNIÓN o una
     * FUNCIÓN ocupa al profesor igual que una clase. La agregación es la misma que la del
     * cuadre, {@link #demandaPorProfesor}, con otro filtro.
     */
    private static List<AvisoPrevalidacion> sobrecargaProfesor(
            ProblemaHorario problema, int tramosLectivos, Predicate<Actividad> filtro) {

        Map<Profesor, Integer> demandaPorProfesor = demandaPorProfesor(problema, filtro);

        Map<Profesor, Set<Tramo>> durasPorProfesor = new LinkedHashMap<>();
        for (RestriccionHoraria restriccion : problema.restriccionesHorarias()) {
            if (restriccion.tipo() == TipoRestriccion.DURA) {
                durasPorProfesor
                        .computeIfAbsent(restriccion.profesor(), p -> new LinkedHashSet<>())
                        .add(restriccion.tramo());
            }
        }

        List<AvisoPrevalidacion> avisos = new ArrayList<>();
        for (Profesor profesor : problema.profesores()) {
            int demanda = demandaPorProfesor.getOrDefault(profesor, 0);
            int duras = durasPorProfesor.getOrDefault(profesor, Set.of()).size();
            int disponible = tramosLectivos - duras;

            if (demanda > disponible) {
                avisos.add(new AvisoPrevalidacion(
                        Severidad.ERROR,
                        REGLA_PROFESOR_SOBRECARGADO,
                        profesor.codigo(),
                        demanda,
                        disponible,
                        "El profesor '" + profesor.codigo() + "' debe impartir " + demanda
                                + " tramos y solo dispone de " + disponible + " ("
                                + tramosLectivos + " tramos lectivos - " + duras
                                + " restricciones DURA)"));
            }
        }
        return avisos;
    }

    /**
     * (d) REPETICIONES &gt; DÍAS LECTIVOS — ERROR. Falla si una actividad DISTRIBUIDA se
     * repite más veces por semana que días lectivos tiene la rejilla.
     *
     * <p><b>Por qué existe.</b> Hoy este caso NO falla: {@code ModeloCpSat:1164-1165}
     * hace {@code continue} ante él —la guarda anti-palomar de la deuda D12— y con ello
     * DESACTIVA EN SILENCIO la restricción de distribución por día para esa actividad.
     * El solver devuelve entonces un horario que apila repeticiones en el mismo día sin
     * que nadie se entere: no hay excepción, no hay infactibilidad, solo un horario peor.
     * Esta comprobación convierte esa degradación muda en un error visible y atribuido a
     * la actividad concreta.
     *
     * <p><b>Solo DISTRIBUIDA</b> (decisión S79), porque {@code ModeloCpSat:1161} descarta
     * lo no-DISTRIBUIDA ANTES de llegar a la guarda de {@code :1164}: para AGRUPADA y
     * NEUTRA no hay restricción de distribución que desactivar, y repetir 7 veces en 5
     * días es legal y resoluble (dos repeticiones caen el mismo día a propósito).
     * Marcarlas ERROR sería un falso positivo.
     */
    private static List<AvisoPrevalidacion> repeticionesExcedenDias(
            ProblemaHorario problema, int diasLectivos) {

        List<AvisoPrevalidacion> avisos = new ArrayList<>();
        for (Actividad actividad : problema.actividades()) {
            if (actividad.patronTemporal() != PatronTemporal.DISTRIBUIDA) {
                continue;
            }
            int repeticiones = actividad.repeticionesPorSemana();
            if (repeticiones > diasLectivos) {
                avisos.add(new AvisoPrevalidacion(
                        Severidad.ERROR,
                        REGLA_REPETICIONES_EXCEDEN_DIAS,
                        actividad.codigo(),
                        repeticiones,
                        diasLectivos,
                        "La actividad DISTRIBUIDA '" + actividad.codigo() + "' se repite "
                                + repeticiones + " veces por semana pero solo hay "
                                + diasLectivos + " dias lectivos: no caben en dias distintos"));
            }
        }
        return avisos;
    }

    /**
     * (c) SOBRECARGA DE GRUPO — ERROR. Falla si las horas curriculares de un grupo
     * superan los tramos lectivos de la semana.
     *
     * <p><b>Por qué es ERROR y no un simple aviso.</b> {@code ModeloCpSat:1046-1074}
     * impone {@code addNoOverlap} POR GRUPO, y su helper {@code tocaGrupo} deduplica UN
     * INTERVALO POR INSTANCIA DE ACTIVIDAD: exactamente la misma unidad de conteo que usa
     * esta regla. Si un grupo acumula más tramos de actividad que tramos lectivos hay, ese
     * {@code addNoOverlap} es insatisfacible. Es infactibilidad GARANTIZADA, no una
     * sobrestimación.
     *
     * <p>El motivo que figuraba aquí antes —"el centro imparte CyR/OyD como bloque
     * alternativo en el mismo tramo, luego configurarlas separadas hace que la suma
     * sobrestime"— es FALSO bajo el modelo actual: si se configuran separadas, el solver
     * TAMPOCO las deja compartir tramo, así que el problema es igual de infactible. Y si
     * se configuran bien —una actividad con varias plazas de distinta asignatura, que es
     * lo que contempla {@code CatalogoMapper}—, la deduplicación por actividad de abajo ya
     * las cuenta una sola vez y no hay nada que sobrestimar.
     *
     * <p><b>Salvedad.</b> A diferencia de (a) y (d), que son estructurales, la garantía de
     * esta regla es DERIVADA del modelo CP-SAT: depende de que restriccionNoSolapeGrupo
     * siga deduplicando por instancia de actividad. Si el no-solape por grupo se relajara,
     * esta regla pasaría a producir falsos positivos EN SILENCIO. Ver deuda D-F8.4-A-a.
     *
     * <p><b>Deduplicación POR ACTIVIDAD, no por plaza</b> —el núcleo de esta regla—. Un
     * desdoble son DOS plazas de la MISMA actividad, con subgrupos distintos del MISMO
     * grupo, que ocurren SIMULTÁNEAMENTE. El grupo consume UN tramo, no dos. Contar por
     * plaza daría el doble en todo desdoble y —ahora que esto aborta con 422— bloquearía
     * catálogos perfectamente sanos.
     * Por eso la ruta {@code Actividad → plazas → subgrupos → grupos} se recoge primero
     * en un {@code Set<Actividad>} por grupo y solo DESPUÉS se suma.
     *
     * <p>La pertenencia se toma de {@code Subgrupo.grupos()} directamente; NO se propaga
     * por {@code grupoPadre} (un grupo PDC no hereda las horas de su padre a efectos de
     * este conteo). La agregación es {@link #demandaPorGrupo}, la misma del cuadre, con el
     * filtro {@link #TODAS}.
     */
    private static List<AvisoPrevalidacion> sobrecargaGrupo(
            ProblemaHorario problema, int tramosLectivos, Predicate<Actividad> filtro) {

        Map<GrupoAdministrativo, Integer> demandaPorGrupo = demandaPorGrupo(problema, filtro);

        List<AvisoPrevalidacion> avisos = new ArrayList<>();
        for (GrupoAdministrativo grupo : problema.grupos()) {
            int demanda = demandaPorGrupo.getOrDefault(grupo, 0);

            if (demanda > tramosLectivos) {
                avisos.add(new AvisoPrevalidacion(
                        Severidad.ERROR,
                        REGLA_GRUPO_SOBRECARGADO,
                        grupo.codigo(),
                        demanda,
                        tramosLectivos,
                        "El grupo '" + grupo.codigo() + "' acumula " + demanda
                                + " tramos curriculares y la semana solo tiene "
                                + tramosLectivos + " tramos lectivos"));
            }
        }
        return avisos;
    }

    /**
     * (f) PIN SOBRE TRAMO DURA — ERROR. Falla por cada terna (pin, tramo que ocupa la
     * sesión pinada, profesor de la sesión) en la que ese profesor tiene una restricción
     * DURA sobre ese tramo (condición 6 de {@code O-disponibilidad}).
     *
     * <p><b>Por qué es ERROR.</b> Es infactibilidad GARANTIZADA: el pin fija el tramo de
     * inicio con una igualdad ({@code ModeloCpSat.restriccionSesionBloqueada}) y la DURA
     * lo prohíbe; medido en S165 (M2, T3), el solver acaba en {@code INFEASIBLE} tras gastar
     * un solve y sin decir por qué. Aquí se rechaza antes, nombrando profesor y tramo.
     *
     * <p><b>Tramos OCUPADOS, no solo el de inicio.</b> Se usa la definición del
     * verificador, {@link VerificadorSolucion#tramosOcupados}: con duración 1 es el tramo
     * del pin, y con un bloque son todos los que cubre. Si el bloque del pin es imposible
     * (desborda el día o cruza el recreo) se mira solo el tramo del pin, mismo criterio
     * que los recuentos BLANDA del verificador.
     *
     * <p>Una DURA repetida sobre el mismo (profesor, tramo) cuenta una vez: los vetos se
     * agrupan en un {@code Set} de tramos por profesor, igual que en el verificador y en
     * la regla (a). Solo DURA: una BLANDA es preferencia y el pin la incumple a sabiendas.
     *
     * <p>Señala al PROFESOR ({@code entidadCodigo}); la sesión y el TRAMO van en la
     * descripción, porque {@link AvisoPrevalidacion} no tiene campo de tramo. Cardinalidad
     * en el contrato {@code demanda > disponible}: el pin pide 1 tramo en el que el
     * profesor tiene 0 disponibles (mismo criterio que S8, 1 contra 0).
     */
    private static List<AvisoPrevalidacion> pinSobreTramoDura(ProblemaHorario problema) {
        Map<Profesor, Set<Tramo>> vetadosPorProfesor = new LinkedHashMap<>();
        for (RestriccionHoraria restriccion : problema.restriccionesHorarias()) {
            if (restriccion.tipo() == TipoRestriccion.DURA) {
                vetadosPorProfesor
                        .computeIfAbsent(restriccion.profesor(), p -> new LinkedHashSet<>())
                        .add(restriccion.tramo());
            }
        }

        List<AvisoPrevalidacion> avisos = new ArrayList<>();
        if (vetadosPorProfesor.isEmpty()) {
            return avisos;
        }
        for (SesionBloqueada pin : problema.bloqueos()) {
            ActividadInstancia instancia = pin.instancia();
            List<Tramo> ocupados = VerificadorSolucion.tramosOcupados(
                            pin.tramo(), instancia.actividad().duracionTramos(), problema)
                    .orElse(List.of(pin.tramo()));
            List<Profesor> profesores = instancia.actividad().plazas().stream()
                    .flatMap(plaza -> plaza.profesores().stream())
                    .distinct()
                    .sorted(Comparator.comparing(Profesor::codigo))
                    .toList();
            for (Tramo tramo : ocupados) {
                for (Profesor profesor : profesores) {
                    if (!vetadosPorProfesor.getOrDefault(profesor, Set.of()).contains(tramo)) {
                        continue;
                    }
                    String donde = tramo.equals(pin.tramo())
                            ? "en el tramo " + tramo.codigo()
                            : "desde el tramo " + pin.tramo().codigo()
                                    + ", y la sesión ocupa el tramo " + tramo.codigo();
                    avisos.add(new AvisoPrevalidacion(
                            Severidad.ERROR,
                            REGLA_PIN_SOBRE_TRAMO_DURA,
                            profesor.codigo(),
                            1,
                            0,
                            "La sesión '" + instancia.actividad().codigo() + "' #"
                                    + instancia.indice() + " está fijada " + donde
                                    + " (día " + tramo.diaSemana() + ", tramo "
                                    + tramo.ordenEnDia() + "), en el que el profesor '"
                                    + profesor.codigo() + "' no puede dar clase"
                                    + " (restricción DURA)"));
                }
            }
        }
        return avisos;
    }

    /**
     * (S207, B1) CLASE SIN AULA POSIBLE — ERROR. Una por plaza de CLASE cuyo dominio de aulas,
     * escrito o deducido de las reglas, se ha quedado vacío. Sin aula no hay horario posible para
     * esa clase, y el problema la llevaría al solver como si fuera una reunión: abortar es la única
     * respuesta correcta. Señala la ACTIVIDAD, que es lo que se abre para arreglarlo; la plaza y el
     * motivo, que escribe {@code DeduccionAulas}, van en la descripción. 1 contra 0, como S8.
     */
    private static List<AvisoPrevalidacion> clasesSinAulaPosible(DatosAulas aulas) {
        List<AvisoPrevalidacion> avisos = new ArrayList<>();
        for (DatosAulas.PlazaSinAula sinAula : aulas.sinAulaPosible()) {
            avisos.add(new AvisoPrevalidacion(
                    Severidad.ERROR,
                    REGLA_CLASE_SIN_AULA_POSIBLE,
                    sinAula.actividadCodigo(),
                    1,
                    0,
                    "La plaza '" + sinAula.plazaCodigo() + "' de la actividad '"
                            + sinAula.actividadCodigo() + "' no tiene ninguna aula posible: "
                            + sinAula.motivo() + "."));
        }
        return avisos;
    }

    /**
     * (S207, B2) REPARTO DE AULAS IMPOSIBLE — ERROR. Las plazas de una actividad ocurren a la vez
     * y cada una necesita un aula distinta (C1); si el emparejamiento máximo plazas → aulas
     * posibles es menor que el número de plazas con aula, no hay horario. Es certeza, no
     * estimación: el emparejamiento es exacto. {@code demanda} = plazas con aula,
     * {@code disponible} = las que pueden emparejarse.
     *
     * <p>Las plazas sin aula en el problema (reuniones, funciones) no necesitan aula y no cuentan.
     * Una actividad con alguna plaza en {@link #clasesSinAulaPosible} no se evalúa: ya tiene su
     * ERROR, y aquí esa plaza parecería una reunión. Implementación propia por caminos de aumento.
     */
    private static List<AvisoPrevalidacion> repartoDeAulasImposible(
            ProblemaHorario problema, DatosAulas aulas) {
        Set<String> actividadesConPlazaSinAula = new HashSet<>();
        for (DatosAulas.PlazaSinAula sinAula : aulas.sinAulaPosible()) {
            actividadesConPlazaSinAula.add(sinAula.actividadCodigo());
        }
        List<AvisoPrevalidacion> avisos = new ArrayList<>();
        for (Actividad actividad : problema.actividades()) {
            if (actividadesConPlazaSinAula.contains(actividad.codigo())) {
                continue;
            }
            List<Plaza> conAula = new ArrayList<>();
            List<Set<Aula>> dominios = new ArrayList<>();
            for (Plaza plaza : actividad.plazas()) {
                Set<Aula> dominio = new HashSet<>(plaza.aulasCandidatas());
                plaza.aulaFija().ifPresent(dominio::add);
                if (!dominio.isEmpty()) {
                    conAula.add(plaza);
                    dominios.add(dominio);
                }
            }
            int emparejadas = emparejamientoMaximo(dominios);
            if (emparejadas < conAula.size()) {
                StringBuilder detalle = new StringBuilder();
                Set<String> enJuego = new TreeSet<>();
                for (int i = 0; i < conAula.size(); i++) {
                    List<String> codigos = dominios.get(i).stream().map(Aula::codigo).sorted().toList();
                    enJuego.addAll(codigos);
                    detalle.append(i == 0 ? "" : "; ").append(conAula.get(i).codigo()).append(": ")
                            .append(String.join(", ", codigos));
                }
                avisos.add(new AvisoPrevalidacion(
                        Severidad.ERROR,
                        REGLA_REPARTO_DE_AULAS_IMPOSIBLE,
                        actividad.codigo(),
                        conAula.size(),
                        emparejadas,
                        "La actividad '" + actividad.codigo() + "' tiene " + conAula.size()
                                + " plazas a la vez y solo " + emparejadas
                                + " pueden ir a aulas distintas. Aulas en juego: "
                                + String.join(", ", enJuego) + " (" + detalle + ")."));
            }
        }
        return avisos;
    }

    /** Tamaño del emparejamiento máximo de cada plaza (índice) a un aula de su dominio. */
    static int emparejamientoMaximo(List<Set<Aula>> dominios) {
        Map<Aula, Integer> duenoDe = new HashMap<>();
        int emparejadas = 0;
        for (int i = 0; i < dominios.size(); i++) {
            if (aumentar(i, dominios, duenoDe, new HashSet<>())) {
                emparejadas++;
            }
        }
        return emparejadas;
    }

    /** Camino de aumento desde la plaza {@code i} (algoritmo de Kuhn). */
    private static boolean aumentar(int i, List<Set<Aula>> dominios, Map<Aula, Integer> duenoDe,
                                    Set<Aula> visitadas) {
        for (Aula aula : dominios.get(i)) {
            if (!visitadas.add(aula)) {
                continue;
            }
            Integer dueno = duenoDe.get(aula);
            if (dueno == null || aumentar(dueno, dominios, duenoDe, visitadas)) {
                duenoDe.put(aula, i);
                return true;
            }
        }
        return false;
    }

    /**
     * (S209, T4) CARGA DE AULAS EXCEDIDA — ERROR. Cada sesión de una clase ocupa un aula de su
     * dominio durante un tramo, y un aula da como mucho un tramo lectivo a una sesión: si las
     * clases que solo pueden ir a un conjunto de aulas suman más tramos que los de esas aulas en la
     * semana, no hay horario. Flujo máximo (Edmonds-Karp): fuente → cada plaza de CLASE con aula
     * posible, capacidad {@link #tramosQueOcupa} de su actividad; plaza → cada aula de su dominio,
     * sin límite; aula → sumidero, capacidad {@code tramosLectivos}. Si el flujo no cubre la
     * demanda, el lado de la FUENTE del corte mínimo (lo alcanzable en el residual) se parte en
     * componentes por las aristas plaza-aula, y sale un aviso por componente con demanda (tramos
     * de sus plazas) mayor que lo disponible (aulas × tramos lectivos).
     *
     * <p>El dominio es el del problema, que ya trae lo que dedujo {@code DeduccionAulas} (A4: aula
     * fija o candidatas): no se recalcula nada. Las plazas sin aula —las que nombra
     * {@link #clasesSinAulaPosible} y las de reuniones y funciones— no entran; las actividades que
     * no son CLASE se filtran con {@code filtro}. Es condición necesaria, no suficiente: no mira a
     * qué hora va cada sesión. {@code entidadCodigo} son los códigos de las aulas, ordenados.
     */
    private static List<AvisoPrevalidacion> cargaDeAulasExcedida(
            ProblemaHorario problema, int tramosLectivos, Predicate<Actividad> filtro) {
        Comparator<Aula> porCodigo = Comparator.comparing(Aula::codigo);
        List<Actividad> actividadDe = new ArrayList<>();
        List<List<Aula>> dominios = new ArrayList<>();
        TreeSet<Aula> enJuego = new TreeSet<>(porCodigo);
        for (Actividad actividad : problema.actividades()) {
            if (!filtro.test(actividad)) {
                continue;
            }
            for (Plaza plaza : actividad.plazas()) {
                TreeSet<Aula> dominio = new TreeSet<>(porCodigo);
                dominio.addAll(plaza.aulasCandidatas());
                plaza.aulaFija().ifPresent(dominio::add);
                if (!dominio.isEmpty()) {
                    actividadDe.add(actividad);
                    dominios.add(List.copyOf(dominio));
                    enJuego.addAll(dominio);
                }
            }
        }
        List<Aula> aulas = List.copyOf(enJuego);
        Map<Aula, Integer> nodoDeAula = new HashMap<>();
        int plazas = dominios.size();
        for (int j = 0; j < aulas.size(); j++) {
            nodoDeAula.put(aulas.get(j), 1 + plazas + j);
        }
        int sumidero = 1 + plazas + aulas.size();
        int demandaTotal = 0;
        for (Actividad actividad : actividadDe) {
            demandaTotal += tramosQueOcupa(actividad);
        }
        RedDeFlujo red = new RedDeFlujo(sumidero + 1);
        for (int i = 0; i < plazas; i++) {
            red.arista(0, 1 + i, tramosQueOcupa(actividadDe.get(i)));
            for (Aula aula : dominios.get(i)) {
                red.arista(1 + i, nodoDeAula.get(aula), demandaTotal + 1);
            }
        }
        for (Aula aula : aulas) {
            red.arista(nodoDeAula.get(aula), sumidero, tramosLectivos);
        }
        if (red.flujoMaximo(0, sumidero) >= demandaTotal) {
            return List.of();
        }

        boolean[] alcanzable = red.alcanzables(0);
        int[] raiz = new int[sumidero + 1];
        for (int v = 0; v <= sumidero; v++) {
            raiz[v] = v;
        }
        for (int i = 0; i < plazas; i++) {
            if (!alcanzable[1 + i]) {
                continue;
            }
            for (Aula aula : dominios.get(i)) {
                if (alcanzable[nodoDeAula.get(aula)]) {
                    raiz[raizDe(raiz, 1 + i)] = raizDe(raiz, nodoDeAula.get(aula));
                }
            }
        }
        Map<Integer, Integer> demandaDe = new HashMap<>();
        Map<Integer, Set<String>> actividadesDe = new HashMap<>();
        Map<Integer, Set<String>> aulasDe = new HashMap<>();
        for (int i = 0; i < plazas; i++) {
            if (alcanzable[1 + i]) {
                int r = raizDe(raiz, 1 + i);
                demandaDe.merge(r, tramosQueOcupa(actividadDe.get(i)), Integer::sum);
                actividadesDe.computeIfAbsent(r, k -> new TreeSet<>()).add(actividadDe.get(i).codigo());
            }
        }
        for (Aula aula : aulas) {
            int nodo = nodoDeAula.get(aula);
            if (alcanzable[nodo]) {
                aulasDe.computeIfAbsent(raizDe(raiz, nodo), k -> new TreeSet<>()).add(aula.codigo());
            }
        }
        List<AvisoPrevalidacion> avisos = new ArrayList<>();
        for (Map.Entry<Integer, Integer> componente : demandaDe.entrySet()) {
            Set<String> codigos = aulasDe.getOrDefault(componente.getKey(), Set.of());
            int demanda = componente.getValue();
            int disponible = codigos.size() * tramosLectivos;
            if (demanda > disponible) {
                String lista = String.join(", ", codigos);
                avisos.add(new AvisoPrevalidacion(
                        Severidad.ERROR,
                        REGLA_CARGA_DE_AULAS_EXCEDIDA,
                        lista,
                        demanda,
                        disponible,
                        "Las aulas " + lista + " tienen " + disponible + " horas de clase y las clases que"
                                + " solo pueden ir a ellas suman " + demanda + ". Actividades: "
                                + String.join(", ", actividadesDe.get(componente.getKey())) + "."));
            }
        }
        avisos.sort(Comparator.comparing(AvisoPrevalidacion::entidadCodigo));
        return avisos;
    }

    /** Raíz de {@code v} en la partición por componentes, comprimiendo el camino. */
    private static int raizDe(int[] raiz, int v) {
        while (raiz[v] != v) {
            raiz[v] = raiz[raiz[v]];
            v = raiz[v];
        }
        return v;
    }

    /**
     * Red de flujo con capacidades enteras y Edmonds-Karp (caminos de aumento más cortos por BFS),
     * para {@link #cargaDeAulasExcedida}. Implementación propia, sin dependencias.
     */
    private static final class RedDeFlujo {
        private final List<List<int[]>> salientes = new ArrayList<>();   // {destino, capacidad, inversa}

        RedDeFlujo(int nodos) {
            for (int v = 0; v < nodos; v++) {
                salientes.add(new ArrayList<>());
            }
        }

        void arista(int desde, int hasta, int capacidad) {
            salientes.get(desde).add(new int[] {hasta, capacidad, salientes.get(hasta).size()});
            salientes.get(hasta).add(new int[] {desde, 0, salientes.get(desde).size() - 1});
        }

        int flujoMaximo(int fuente, int sumidero) {
            int total = 0;
            while (true) {
                int[] previo = new int[salientes.size()];
                int[] porArista = new int[salientes.size()];
                java.util.Arrays.fill(previo, -1);
                previo[fuente] = fuente;
                java.util.ArrayDeque<Integer> cola = new java.util.ArrayDeque<>(List.of(fuente));
                while (!cola.isEmpty() && previo[sumidero] < 0) {
                    int u = cola.poll();
                    for (int k = 0; k < salientes.get(u).size(); k++) {
                        int[] e = salientes.get(u).get(k);
                        if (e[1] > 0 && previo[e[0]] < 0) {
                            previo[e[0]] = u;
                            porArista[e[0]] = k;
                            cola.add(e[0]);
                        }
                    }
                }
                if (previo[sumidero] < 0) {
                    return total;
                }
                int cuello = Integer.MAX_VALUE;
                for (int v = sumidero; v != fuente; v = previo[v]) {
                    cuello = Math.min(cuello, salientes.get(previo[v]).get(porArista[v])[1]);
                }
                for (int v = sumidero; v != fuente; v = previo[v]) {
                    int[] e = salientes.get(previo[v]).get(porArista[v]);
                    e[1] -= cuello;
                    salientes.get(v).get(e[2])[1] += cuello;
                }
                total += cuello;
            }
        }

        /** Nodos alcanzables desde {@code origen} por aristas con capacidad residual. */
        boolean[] alcanzables(int origen) {
            boolean[] vistos = new boolean[salientes.size()];
            vistos[origen] = true;
            java.util.ArrayDeque<Integer> cola = new java.util.ArrayDeque<>(List.of(origen));
            while (!cola.isEmpty()) {
                for (int[] e : salientes.get(cola.poll())) {
                    if (e[1] > 0 && !vistos[e[0]]) {
                        vistos[e[0]] = true;
                        cola.add(e[0]);
                    }
                }
            }
            return vistos;
        }
    }

    /**
     * (S8) TUTORÍA SIN TUTOR — AVISO. Una actividad {@code requiereTutor} que no imparte
     * ningún TUTOR_PRINCIPAL de un grupo que cubre (§4.6).
     *
     * <p><b>Delega, no reimplementa.</b> La regla vive en
     * {@link VerificadorSolucion#verificarTutorias(ProblemaHorario)}; aquí solo se
     * traduce cada {@link Violacion} al vocabulario de la pre-validación. Ver el javadoc
     * de clase para el porqué de AVISO y del 1/0.
     *
     * <p><b>La entidad señalada es la ACTIVIDAD, no el grupo.</b> Se saca de las celdas
     * de la violación y NO de su {@code recursoCodigo()}, que lleva el grupo afectado
     * (ver {@code VerificadorSolucion.grupoAfectado}). Las demás reglas nombran la
     * entidad que hay que TOCAR para arreglar el hallazgo, y aquí lo accionable es la
     * actividad concreta que se quedó sin tutor.
     *
     * <p>{@code celdas().get(0)} es seguro y no necesita guarda: el constructor compacto
     * de {@link Violacion} RECHAZA una lista de celdas vacía, y S8 la puebla con una
     * celda por repetición ({@code repeticionesPorSemana >= 1} por invariante de
     * {@code Actividad}). Todas las celdas de una violación S8 llevan además el MISMO
     * {@code actividadCodigo}, así que la primera no desempata nada: es el único valor.
     */
    private static List<AvisoPrevalidacion> tutoriasSinTutor(ProblemaHorario problema) {
        List<AvisoPrevalidacion> avisos = new ArrayList<>();
        for (Violacion violacion : VERIFICADOR.verificarTutorias(problema)) {
            avisos.add(new AvisoPrevalidacion(
                    Severidad.AVISO,
                    REGLA_TUTORIA_SIN_TUTOR,
                    violacion.celdas().get(0).actividadCodigo(),
                    1,
                    0,
                    violacion.descripcion()));
        }
        return avisos;
    }

    /**
     * (S203) PROFESOR CON HORAS DESCUADRADAS — AVISO. Un profesor con total declarado cuyas
     * horas de CLASE configuradas no son las declaradas, por exceso o por defecto
     * ({@link #descuadra}). Recorre {@code problema.profesores()} entero, así que un profesor
     * declarado sin ninguna actividad tiene 0 configuradas y avisa.
     *
     * <p>Por qué AVISO: el horario existe igual y se arregla editando el catálogo, vía (2)
     * de {@link Severidad}. {@code demanda} son las configuradas y {@code disponible} las
     * declaradas.
     *
     * <p>Solo CLASE: las reuniones y funciones ocupan al profesor —las cuenta (a)— pero no son
     * horas de clase. Misma agregación que (a), {@link #demandaPorProfesor}, con el filtro
     * {@link #soloClase}.
     */
    private static List<AvisoPrevalidacion> profesoresDescuadrados(
            ProblemaHorario problema, DatosCuadre datos) {
        Map<Profesor, Integer> configuradasPorProfesor = demandaPorProfesor(problema, soloClase(datos));
        List<AvisoPrevalidacion> avisos = new ArrayList<>();
        for (Profesor profesor : problema.profesores()) {
            Integer declaradas = datos.declaradasProfesor().get(profesor.codigo());
            int configuradas = configuradasPorProfesor.getOrDefault(profesor, 0);
            if (descuadra(declaradas, configuradas)) {
                avisos.add(new AvisoPrevalidacion(
                        Severidad.AVISO,
                        REGLA_PROFESOR_HORAS_DESCUADRADAS,
                        profesor.codigo(),
                        configuradas,
                        declaradas,
                        profesor.codigo() + ": " + configuradas
                                + " horas de clase configuradas y " + declaradas + " declaradas."));
            }
        }
        return avisos;
    }

    /**
     * (S203) GRUPO CON HORAS DESCUADRADAS — AVISO. Como {@link #profesoresDescuadrados} para
     * los grupos, ordinarios y PDC. Cada grupo cuenta SUS actividades: un PDC no hereda las de
     * su padre, igual que en (c), con la misma agregación {@link #demandaPorGrupo}.
     */
    private static List<AvisoPrevalidacion> gruposDescuadrados(
            ProblemaHorario problema, DatosCuadre datos) {
        Map<GrupoAdministrativo, Integer> configuradasPorGrupo = demandaPorGrupo(problema, soloClase(datos));
        List<AvisoPrevalidacion> avisos = new ArrayList<>();
        for (GrupoAdministrativo grupo : problema.grupos()) {
            Integer declaradas = datos.declaradasGrupo().get(grupo.codigo());
            int configuradas = configuradasPorGrupo.getOrDefault(grupo, 0);
            if (descuadra(declaradas, configuradas)) {
                avisos.add(new AvisoPrevalidacion(
                        Severidad.AVISO,
                        REGLA_GRUPO_HORAS_DESCUADRADAS,
                        grupo.codigo(),
                        configuradas,
                        declaradas,
                        grupo.codigo() + ": " + configuradas
                                + " horas configuradas y " + declaradas + " declaradas."));
            }
        }
        return avisos;
    }

    /**
     * NÚCLEO del cuadre ({@code GET /api/prevalidacion/cuadre}, S203): TODOS los profesores y
     * TODOS los grupos, con total declarado o sin él, en el orden del problema. Las cifras
     * salen de la misma agregación que las reglas de cuadre y la marca de {@link #descuadra},
     * así que una entidad lleva {@code descuadre} si y solo si su regla emite un AVISO.
     */
    public static CuadreDTO cuadre(ProblemaHorario problema, DatosCuadre datos) {
        Objects.requireNonNull(problema, "problema no puede ser null");
        Objects.requireNonNull(datos, "datos no puede ser null");
        Map<Profesor, Integer> configuradasPorProfesor = demandaPorProfesor(problema, soloClase(datos));
        Map<GrupoAdministrativo, Integer> configuradasPorGrupo = demandaPorGrupo(problema, soloClase(datos));
        List<CuadreEntidadDTO> profesores = problema.profesores().stream()
                .map(p -> entidadDeCuadre(p.codigo(), configuradasPorProfesor.getOrDefault(p, 0),
                        datos.declaradasProfesor().get(p.codigo())))
                .toList();
        List<CuadreEntidadDTO> grupos = problema.grupos().stream()
                .map(g -> entidadDeCuadre(g.codigo(), configuradasPorGrupo.getOrDefault(g, 0),
                        datos.declaradasGrupo().get(g.codigo())))
                .toList();
        return new CuadreDTO(profesores, grupos);
    }

    private static CuadreEntidadDTO entidadDeCuadre(String codigo, int configuradas, Integer declaradas) {
        return new CuadreEntidadDTO(codigo, configuradas, declaradas, descuadra(declaradas, configuradas));
    }

    /**
     * La única definición de descuadre (S203): hay total declarado y las configuradas no son
     * esas. Sin total no hay nada con qué cuadrar. La usan las dos reglas y el GET de cuadre.
     */
    static boolean descuadra(Integer declaradas, int configuradas) {
        return declaradas != null && configuradas != declaradas;
    }

    /** Filtro del cuadre: solo las CLASE; fuera las actividades que {@code datos} marca como no CLASE. */
    private static Predicate<Actividad> soloClase(DatosCuadre datos) {
        return actividad -> !datos.actividadesNoClase().contains(actividad.codigo());
    }

    /**
     * Tramos semanales de cada profesor, sumando UNA VEZ cada actividad que pasa el
     * {@code filtro} aunque figure en varias de sus plazas (plazas simultáneas, ver (a)).
     * Solo aparecen los profesores con alguna actividad. La comparten (a), con {@link #TODAS},
     * y el cuadre, con {@link #soloClase}.
     */
    private static Map<Profesor, Integer> demandaPorProfesor(
            ProblemaHorario problema, Predicate<Actividad> filtro) {
        Map<Profesor, Set<Actividad>> actividadesPorProfesor = new LinkedHashMap<>();
        for (Actividad actividad : problema.actividades()) {
            if (!filtro.test(actividad)) {
                continue;
            }
            for (Plaza plaza : actividad.plazas()) {
                for (Profesor profesor : plaza.profesores()) {
                    actividadesPorProfesor
                            .computeIfAbsent(profesor, p -> new LinkedHashSet<>())
                            .add(actividad);
                }
            }
        }
        Map<Profesor, Integer> demanda = new LinkedHashMap<>();
        actividadesPorProfesor.forEach((profesor, actividades) -> demanda.put(profesor,
                actividades.stream().mapToInt(PrevalidacionService::tramosQueOcupa).sum()));
        return demanda;
    }

    /**
     * Tramos semanales de cada grupo por la ruta {@code actividad → plazas → subgrupos →
     * grupos}, UNA VEZ por actividad que pasa el {@code filtro} (desdobles, ver (c)). Sin
     * herencia por {@code grupoPadre}. Solo aparecen los grupos con alguna actividad. La
     * comparten (c), con {@link #TODAS}, y el cuadre, con {@link #soloClase}.
     */
    private static Map<GrupoAdministrativo, Integer> demandaPorGrupo(
            ProblemaHorario problema, Predicate<Actividad> filtro) {
        Map<GrupoAdministrativo, Set<Actividad>> actividadesPorGrupo = new LinkedHashMap<>();
        for (Actividad actividad : problema.actividades()) {
            if (!filtro.test(actividad)) {
                continue;
            }
            for (Plaza plaza : actividad.plazas()) {
                for (Subgrupo subgrupo : plaza.subgrupos()) {
                    for (GrupoAdministrativo grupo : subgrupo.grupos()) {
                        actividadesPorGrupo
                                .computeIfAbsent(grupo, g -> new LinkedHashSet<>())
                                .add(actividad);
                    }
                }
            }
        }
        Map<GrupoAdministrativo, Integer> demanda = new LinkedHashMap<>();
        actividadesPorGrupo.forEach((grupo, actividades) -> demanda.put(grupo,
                actividades.stream().mapToInt(PrevalidacionService::tramosQueOcupa).sum()));
        return demanda;
    }

    /**
     * Tramos que una actividad ocupa a lo largo de la semana en CUALQUIERA de los
     * recursos que toca: {@code duracionTramos × repeticionesPorSemana}. Fuente única de
     * la aritmética de (a), (c) y el cuadre; todas cuentan por actividad, así que todas
     * cuentan igual.
     */
    private static int tramosQueOcupa(Actividad actividad) {
        return actividad.duracionTramos() * actividad.repeticionesPorSemana();
    }
}
