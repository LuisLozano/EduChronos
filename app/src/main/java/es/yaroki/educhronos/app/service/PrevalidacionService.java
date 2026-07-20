package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.GrupoAdministrativo;
import es.yaroki.educhronos.solver.domain.PatronTemporal;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.RestriccionHoraria;
import es.yaroki.educhronos.solver.domain.Subgrupo;
import es.yaroki.educhronos.solver.domain.TipoRestriccion;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pre-validación de CONDICIONES NECESARIAS del catálogo (Fase 8, Bloque 8.4-A,
 * deuda D18): comprobaciones de conteo que, si fallan, garantizan que no existe
 * horario posible —o avisan de que probablemente no exista— ANTES de gastar el
 * presupuesto del solver.
 *
 * <p><b>Qué NO es.</b> No es una validación de integridad del catálogo (huérfanos,
 * códigos duplicados): eso ya lo hace {@link es.yaroki.educhronos.app.mapper.CatalogoMapper}
 * al mapear. Aquí el catálogo YA es referencialmente sano; lo que se compara es
 * DEMANDA contra DISPONIBILIDAD. Son condiciones NECESARIAS, no suficientes: pasar
 * las tres no garantiza que el problema sea factible.
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
 * <p><b>Por qué el núcleo es estático.</b> {@link #prevalidar(ProblemaHorario)} es un
 * método estático puro, y {@code GeneradorHorarioService.generar()} lo invoca así, sin
 * inyectar este bean. Dos razones: (1) inyectarlo crearía un CICLO de beans —este
 * servicio ya depende de {@code GeneradorHorarioService} para cargar—, que Spring Boot
 * rechaza al arrancar; (2) evita añadir una decimotercera dependencia al servicio que
 * ya arrastra la deuda D-F8.2b-iii-A-a. El núcleo estático es además la ÚNICA
 * implementación de las tres reglas: el endpoint y la generación entran por ahí, uno
 * cargando el problema y otro reutilizando el que ya tiene en la mano. Ninguna regla
 * se escribe dos veces (familia D-F8.2b-iv-a).
 *
 * <p><b>Palomar de aulas: fuera de alcance</b> por decisión explícita de S79. Comparar
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

    /** Un grupo tiene más horas curriculares que tramos lectivos. AVISO. */
    public static final String REGLA_GRUPO_SOBRECARGADO = "GRUPO_SOBRECARGADO";

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
        return prevalidar(generadorService.cargarProblema());
    }

    /**
     * NÚCLEO: las tres comprobaciones sobre un {@link ProblemaHorario} ya cargado.
     * Puro y estático —ni repositorios, ni transacción, ni estado—, para que
     * {@code GeneradorHorarioService.generar()} lo llame con el problema que YA cargó,
     * sin volver a leer el catálogo y sin inyectar este bean (ver javadoc de clase).
     *
     * <p>El orden de salida es estable: primero profesores, luego actividades, luego
     * grupos, y dentro de cada bloque el orden del catálogo.
     */
    public static List<AvisoPrevalidacion> prevalidar(ProblemaHorario problema) {
        Objects.requireNonNull(problema, "problema no puede ser null");

        // Ambos techos salen del PROBLEMA, nunca de una constante: el catálogo real trae
        // los tramos ya sin recreos (CatalogoMapper los excluye) y los días que existan.
        // Hardcodear 30 y 5 crearía otro espejo de la familia D22/D30.
        int tramosLectivos = problema.tramos().size();
        int diasLectivos = (int) problema.tramos().stream()
                .map(Tramo::diaSemana).distinct().count();

        List<AvisoPrevalidacion> avisos = new ArrayList<>();
        avisos.addAll(sobrecargaProfesor(problema, tramosLectivos));
        avisos.addAll(repeticionesExcedenDias(problema, diasLectivos));
        avisos.addAll(sobrecargaGrupo(problema, tramosLectivos));
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
     */
    private static List<AvisoPrevalidacion> sobrecargaProfesor(
            ProblemaHorario problema, int tramosLectivos) {

        Map<Profesor, Set<Actividad>> actividadesPorProfesor = new LinkedHashMap<>();
        for (Actividad actividad : problema.actividades()) {
            for (Plaza plaza : actividad.plazas()) {
                for (Profesor profesor : plaza.profesores()) {
                    actividadesPorProfesor
                            .computeIfAbsent(profesor, p -> new LinkedHashSet<>())
                            .add(actividad);
                }
            }
        }

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
            int demanda = actividadesPorProfesor.getOrDefault(profesor, Set.of()).stream()
                    .mapToInt(PrevalidacionService::tramosQueOcupa).sum();
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
     * (c) SOBRECARGA DE GRUPO — AVISO, no ERROR. Avisa si las horas curriculares de un
     * grupo superan los tramos lectivos de la semana.
     *
     * <p><b>Por qué es AVISO y no ERROR.</b> Porque el modelo permite configurar como
     * actividades SEPARADAS lo que el centro imparte como un BLOQUE ALTERNATIVO: CyR y
     * OyD ocupan el mismo tramo, el alumno cursa una u otra, pero son dos actividades
     * distintas en el catálogo y ambas tocan al grupo. La suma las cuenta las dos y
     * SOBRESTIMA la demanda real. Un ERROR aquí bloquearía con un 422 un problema
     * perfectamente resoluble; el aviso informa sin impedir.
     *
     * <p><b>Deduplicación POR ACTIVIDAD, no por plaza</b> —el núcleo de esta regla—. Un
     * desdoble son DOS plazas de la MISMA actividad, con subgrupos distintos del MISMO
     * grupo, que ocurren SIMULTÁNEAMENTE. El grupo consume UN tramo, no dos. Contar por
     * plaza daría el doble en todo desdoble y convertiría el aviso en ruido permanente.
     * Por eso la ruta {@code Actividad → plazas → subgrupos → grupos} se recoge primero
     * en un {@code Set<Actividad>} por grupo y solo DESPUÉS se suma.
     *
     * <p>La pertenencia se toma de {@code Subgrupo.grupos()} directamente; NO se propaga
     * por {@code grupoPadre} (un grupo PDC no hereda las horas de su padre a efectos de
     * este conteo).
     */
    private static List<AvisoPrevalidacion> sobrecargaGrupo(
            ProblemaHorario problema, int tramosLectivos) {

        Map<GrupoAdministrativo, Set<Actividad>> actividadesPorGrupo = new LinkedHashMap<>();
        for (Actividad actividad : problema.actividades()) {
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

        List<AvisoPrevalidacion> avisos = new ArrayList<>();
        for (GrupoAdministrativo grupo : problema.grupos()) {
            int demanda = actividadesPorGrupo.getOrDefault(grupo, Set.of()).stream()
                    .mapToInt(PrevalidacionService::tramosQueOcupa).sum();

            if (demanda > tramosLectivos) {
                avisos.add(new AvisoPrevalidacion(
                        Severidad.AVISO,
                        REGLA_GRUPO_SOBRECARGADO,
                        grupo.codigo(),
                        demanda,
                        tramosLectivos,
                        "El grupo '" + grupo.codigo() + "' acumula " + demanda
                                + " tramos curriculares y la semana solo tiene "
                                + tramosLectivos + " tramos lectivos (puede ser una"
                                + " sobrestimacion si hay bloques alternativos)"));
            }
        }
        return avisos;
    }

    /**
     * Tramos que una actividad ocupa a lo largo de la semana en CUALQUIERA de los
     * recursos que toca: {@code duracionTramos × repeticionesPorSemana}. Fuente única de
     * la aritmética de (a) y (c); ambas cuentan por actividad, así que ambas cuentan
     * igual.
     */
    private static int tramosQueOcupa(Actividad actividad) {
        return actividad.duracionTramos() * actividad.repeticionesPorSemana();
    }
}
