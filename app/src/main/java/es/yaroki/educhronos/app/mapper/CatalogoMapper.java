package es.yaroki.educhronos.app.mapper;

import es.yaroki.educhronos.app.catalog.Dia;
import es.yaroki.educhronos.app.catalog.PatronTemporal;
import es.yaroki.educhronos.app.catalog.ProfesorRestriccionHoraria;
import es.yaroki.educhronos.app.catalog.TramoSemanal;
import es.yaroki.educhronos.solver.domain.Asignatura;
import es.yaroki.educhronos.solver.domain.Aula;
import es.yaroki.educhronos.solver.domain.GrupoAdministrativo;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.RestriccionHoraria;
import es.yaroki.educhronos.solver.domain.TipoGrupo;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Convierte entidades JPA del catálogo del centro ({@code app.catalog}) al
 * modelo puro del solver ({@code solver.domain}). {@link #aProblemaHorario}
 * ensambla la entrada completa del solver a partir de las siete listas de
 * entidades del catálogo (Fase 6, Bloque 6); los métodos por-entidad que consume
 * se añadieron en los Bloques 3–5. El mapeo inverso (solución → entidades) llega
 * en un bloque posterior.
 *
 * <p>Respeta la frontera dura: esta clase vive en {@code app/} y depende de
 * {@code solver} en un solo sentido; el modelo del solver ignora que JPA existe.
 * El espejo de estilo es {@code solver.io.ProblemaHorarioMapper}.
 *
 * <p>Decisiones de mapeo cerradas en el Project (Bloque 3):
 * <ul>
 *   <li>{@code Aula.nombre = codigo}: ni la entidad JPA ni el modelo §4.1 tienen
 *       un {@code nombre} de aula; el dominio del solver lo exige como etiqueta.
 *       Se replica el código como nombre (decisión de producto, deuda consciente).
 *   <li>{@code TipoGrupo.VIRTUAL_OPTATIVA} no tiene equivalente en el solver:
 *       se lanza excepción explícita, no se degrada a ORDINARIO ni se filtra.
 *   <li>Los {@code TramoSemanal} de recreo ({@code esLectivo=false}) se excluyen:
 *       {@code domain.Tramo} no tiene ese concepto y numera 1..6 sin hueco.
 *   <li>Campos de {@code Aula} (tipo/capacidad/edificio/planta/sector),
 *       {@code Nivel} y {@code Tramo.siguienteInmediato} no se mapean: el dominio
 *       del solver no los consume todavía (deuda consciente).
 * </ul>
 */
public final class CatalogoMapper {

    private CatalogoMapper() { }

    /**
     * Ensambla un {@link ProblemaHorario} completo del solver a partir de las
     * siete listas de entidades JPA del catálogo, ya cargadas. No lee de
     * repositorios ni abre transacción: recibe las colecciones y las traduce.
     *
     * <p>El orden de resolución lo dictan las dependencias entre entidades: los
     * subgrupos se resuelven contra los grupos ya mapeados, y las actividades
     * (con sus plazas) contra asignaturas/profesores/aulas/subgrupos ya mapeados.
     * Cada índice {@code Map<String, ...>} se construye volcando la lista de
     * dominio del paso previo —nunca fabricando tipos de {@code solver.domain}—
     * y usa un colector que ABORTA ante código duplicado
     * ({@link Collectors#toMap(java.util.function.Function, java.util.function.Function)}
     * lanza {@link IllegalStateException}): un código repetido es un error de
     * integridad del catálogo, no algo a colapsar en silencio.
     *
     * <p>Las listas de dominio preservan el orden de entrada (los tramos, además,
     * quedan ordenados por {@code aTramos}). Las {@code restricciones} horarias
     * (deuda D28, cerrada en el Bloque 7) se resuelven contra los profesores ya
     * mapeados y contra el índice {@code TramoSemanal → Tramo} que produce el
     * mapeo de tramos: una restricción sobre un tramo no lectivo (excluido por
     * {@code aTramos}) aborta con excepción explícita.
     */
    public static ProblemaHorario aProblemaHorario(
            List<TramoSemanal> tramos,
            List<es.yaroki.educhronos.app.catalog.Aula> aulas,
            List<es.yaroki.educhronos.app.catalog.Asignatura> asignaturas,
            List<es.yaroki.educhronos.app.catalog.Profesor> profesores,
            List<es.yaroki.educhronos.app.catalog.GrupoAdministrativo> grupos,
            List<es.yaroki.educhronos.app.catalog.Subgrupo> subgrupos,
            List<es.yaroki.educhronos.app.catalog.Actividad> actividades,
            List<ProfesorRestriccionHoraria> restricciones) {

        Objects.requireNonNull(tramos,       "tramos no puede ser null");
        Objects.requireNonNull(aulas,        "aulas no puede ser null");
        Objects.requireNonNull(asignaturas,  "asignaturas no puede ser null");
        Objects.requireNonNull(profesores,   "profesores no puede ser null");
        Objects.requireNonNull(grupos,       "grupos no puede ser null");
        Objects.requireNonNull(subgrupos,    "subgrupos no puede ser null");
        Objects.requireNonNull(actividades,  "actividades no puede ser null");
        Objects.requireNonNull(restricciones, "restricciones no puede ser null");

        TramosMapeados tramosMapeados = aTramosConIndice(tramos);
        List<Tramo> tramosDom = tramosMapeados.lista();

        List<Aula> aulasDom = aulas.stream().map(CatalogoMapper::aAula).toList();
        Map<String, Aula> aulasPorCodigo =
                aulasDom.stream().collect(Collectors.toMap(Aula::codigo, a -> a));

        List<Asignatura> asignaturasDom =
                asignaturas.stream().map(CatalogoMapper::aAsignatura).toList();
        Map<String, Asignatura> asignaturasPorCodigo =
                asignaturasDom.stream().collect(Collectors.toMap(Asignatura::codigo, a -> a));

        List<Profesor> profesoresDom =
                profesores.stream().map(CatalogoMapper::aProfesor).toList();
        Map<String, Profesor> profesoresPorCodigo =
                profesoresDom.stream().collect(Collectors.toMap(Profesor::codigo, p -> p));

        List<GrupoAdministrativo> gruposDom =
                grupos.stream().map(CatalogoMapper::aGrupo).toList();
        Map<String, GrupoAdministrativo> gruposPorCodigo =
                gruposDom.stream().collect(Collectors.toMap(GrupoAdministrativo::codigo, g -> g));

        List<es.yaroki.educhronos.solver.domain.Subgrupo> subgruposDom =
                subgrupos.stream().map(sg -> aSubgrupo(sg, gruposPorCodigo)).toList();
        Map<String, es.yaroki.educhronos.solver.domain.Subgrupo> subgruposPorCodigo =
                subgruposDom.stream().collect(Collectors.toMap(
                        es.yaroki.educhronos.solver.domain.Subgrupo::codigo, s -> s));

        List<es.yaroki.educhronos.solver.domain.Actividad> actividadesDom =
                actividades.stream()
                        .map(act -> aActividad(act, asignaturasPorCodigo,
                                profesoresPorCodigo, aulasPorCodigo, subgruposPorCodigo))
                        .toList();

        List<RestriccionHoraria> restriccionesDom = restricciones.stream()
                .map(r -> aRestriccionHoraria(r, profesoresPorCodigo, tramosMapeados.porEntidad()))
                .toList();

        return new ProblemaHorario(
                tramosDom, aulasDom, asignaturasDom, profesoresDom,
                gruposDom, subgruposDom, actividadesDom, restriccionesDom);
    }

    /**
     * {@code Aula} JPA → {@code domain.Aula}. El {@code nombre} del dominio se
     * rellena con el {@code codigo} (ver decisión de producto en el javadoc de
     * clase): la entidad no tiene un nombre de aula propio.
     */
    public static Aula aAula(es.yaroki.educhronos.app.catalog.Aula aula) {
        Objects.requireNonNull(aula, "aula no puede ser null");
        return new Aula(aula.getCodigo(), aula.getCodigo());
    }

    /** {@code Asignatura} JPA → {@code domain.Asignatura}. */
    public static Asignatura aAsignatura(es.yaroki.educhronos.app.catalog.Asignatura asignatura) {
        Objects.requireNonNull(asignatura, "asignatura no puede ser null");
        return new Asignatura(asignatura.getCodigo(), asignatura.getNombreCompleto());
    }

    /** {@code Profesor} JPA → {@code domain.Profesor}. */
    public static Profesor aProfesor(es.yaroki.educhronos.app.catalog.Profesor profesor) {
        Objects.requireNonNull(profesor, "profesor no puede ser null");
        return new Profesor(profesor.getCodigo(), profesor.getNombreCompleto());
    }

    /**
     * {@code GrupoAdministrativo} JPA → {@code domain.GrupoAdministrativo}.
     * El {@code grupoPadre} se resuelve como referencia de objeto (ya cargada
     * por JPA), recursivamente. La invariante I5 (un grupo PDC debe tener padre)
     * la valida el propio record del dominio.
     */
    public static GrupoAdministrativo aGrupo(es.yaroki.educhronos.app.catalog.GrupoAdministrativo grupo) {
        Objects.requireNonNull(grupo, "grupo no puede ser null");
        Optional<GrupoAdministrativo> padre = grupo.getGrupoPadre() == null
                ? Optional.empty()
                : Optional.of(aGrupo(grupo.getGrupoPadre()));
        return new GrupoAdministrativo(grupo.getCodigo(), aTipoGrupo(grupo.getTipo()), padre);
    }

    /**
     * Convierte una entidad {@link es.yaroki.educhronos.app.catalog.Subgrupo} JPA
     * al {@code Subgrupo} del dominio del solver.
     *
     * <p>Los grupos de la población se resuelven por identidad de objeto contra los
     * grupos de dominio ya mapeados (mismo patrón que {@link #aGrupo} resuelve
     * {@code grupoPadre}): se busca cada {@code GrupoAdministrativo} JPA por su
     * código en {@code gruposPorCodigo}. Un código sin correspondencia es un error
     * de integridad referencial del catálogo y aborta con excepción explícita.
     *
     * @param entidad         subgrupo JPA de {@code app.catalog}
     * @param gruposPorCodigo  índice de grupos de dominio ya mapeados, por código
     * @return el {@code Subgrupo} de dominio con su {@code Set<GrupoAdministrativo>}
     */
    public static es.yaroki.educhronos.solver.domain.Subgrupo aSubgrupo(
            es.yaroki.educhronos.app.catalog.Subgrupo entidad,
            java.util.Map<String, es.yaroki.educhronos.solver.domain.GrupoAdministrativo> gruposPorCodigo) {

        java.util.Set<es.yaroki.educhronos.solver.domain.GrupoAdministrativo> grupos =
                new java.util.HashSet<>();
        for (es.yaroki.educhronos.app.catalog.GrupoAdministrativo g : entidad.getGrupos()) {
            es.yaroki.educhronos.solver.domain.GrupoAdministrativo mapeado =
                    gruposPorCodigo.get(g.getCodigo());
            if (mapeado == null) {
                throw new IllegalArgumentException(
                        "Subgrupo '" + entidad.getCodigo()
                                + "' referencia un grupo no presente en el catálogo mapeado: "
                                + g.getCodigo());
            }
            grupos.add(mapeado);
        }
        return new es.yaroki.educhronos.solver.domain.Subgrupo(entidad.getCodigo(), grupos);
    }

    /**
     * {@code Actividad} JPA → {@code domain.Actividad}. Entidad a entidad: una
     * actividad es autocontenida (no depende de las demás), a diferencia de
     * {@code aTramos}. El ensamblado de la lista completa lo hace el Bloque 6.
     *
     * <p>La {@code asignatura} de la actividad es opcional: si la entidad la trae
     * null (plazas de distinta asignatura, p. ej. bloque CyR/OyD/RefMt), se mapea
     * a {@code Optional.empty()}; mismo shape que {@code aGrupo} con {@code grupoPadre}.
     *
     * <p>{@code requiereTutor} de la entidad NO se propaga al dominio: el record
     * {@code domain.Actividad} no tiene ese campo (la invariante S8 no la consume
     * el solver hoy). Es dato de configuración de §4.6 que persiste para Fase 8
     * pero el mapper lo ignora, igual que ignora Nivel o los campos extra de Aula.
     * Decisión D-B5-5.
     *
     * <p>Las plazas se resuelven con el helper privado {@link #aPlaza}, que
     * comparte los mismos índices por código. Un código sin correspondencia en
     * cualquier índice es error de integridad referencial y aborta con excepción.
     *
     * @param entidad             actividad JPA de {@code app.catalog}
     * @param asignaturasPorCodigo índice de asignaturas de dominio ya mapeadas
     * @param profesoresPorCodigo  índice de profesores de dominio ya mapeados
     * @param aulasPorCodigo       índice de aulas de dominio ya mapeadas
     * @param subgruposPorCodigo   índice de subgrupos de dominio ya mapeados
     */
    public static es.yaroki.educhronos.solver.domain.Actividad aActividad(
            es.yaroki.educhronos.app.catalog.Actividad entidad,
            Map<String, Asignatura> asignaturasPorCodigo,
            Map<String, Profesor> profesoresPorCodigo,
            Map<String, Aula> aulasPorCodigo,
            Map<String, es.yaroki.educhronos.solver.domain.Subgrupo> subgruposPorCodigo) {

        Objects.requireNonNull(entidad, "actividad no puede ser null");

        Optional<Asignatura> asignatura = entidad.getAsignatura() == null
                ? Optional.empty()
                : Optional.of(resolver(asignaturasPorCodigo, entidad.getAsignatura().getCodigo(),
                "asignatura", entidad.getCodigo()));

        List<es.yaroki.educhronos.solver.domain.Plaza> plazas =
                new ArrayList<>(entidad.getPlazas().size());
        for (es.yaroki.educhronos.app.catalog.Plaza p : entidad.getPlazas()) {
            plazas.add(aPlaza(p, asignaturasPorCodigo, profesoresPorCodigo,
                    aulasPorCodigo, subgruposPorCodigo));
        }

        return new es.yaroki.educhronos.solver.domain.Actividad(
                entidad.getCodigo(),
                asignatura,
                entidad.getRepeticionesPorSemana(),
                entidad.getDuracionTramos(),
                aPatronTemporal(entidad.getPatronTemporal()),
                plazas);
    }

    /**
     * {@code Plaza} JPA → {@code domain.Plaza}. Helper privado de {@link #aActividad}:
     * una plaza no tiene identidad de mapeo fuera de su actividad. El XOR
     * aula_fija / aulasCandidatas lo hace cumplir el record del dominio; aquí
     * solo se traduce (aula_fija presente → aulaFija con candidatas vacías;
     * ausente → aulasCandidatas resueltas y aulaFija vacía).
     */
    private static es.yaroki.educhronos.solver.domain.Plaza aPlaza(
            es.yaroki.educhronos.app.catalog.Plaza entidad,
            Map<String, Asignatura> asignaturasPorCodigo,
            Map<String, Profesor> profesoresPorCodigo,
            Map<String, Aula> aulasPorCodigo,
            Map<String, es.yaroki.educhronos.solver.domain.Subgrupo> subgruposPorCodigo) {

        Asignatura asignatura = resolver(asignaturasPorCodigo,
                entidad.getAsignatura().getCodigo(), "asignatura", entidad.getCodigo());

        java.util.Set<Profesor> profesores = new java.util.HashSet<>();
        for (es.yaroki.educhronos.app.catalog.Profesor prof : entidad.getProfesores()) {
            profesores.add(resolver(profesoresPorCodigo, prof.getCodigo(),
                    "profesor", entidad.getCodigo()));
        }

        Optional<Aula> aulaFija = entidad.getAulaFija() == null
                ? Optional.empty()
                : Optional.of(resolver(aulasPorCodigo, entidad.getAulaFija().getCodigo(),
                "aula fija", entidad.getCodigo()));

        java.util.Set<Aula> aulasCandidatas = new java.util.HashSet<>();
        for (es.yaroki.educhronos.app.catalog.Aula a : entidad.getAulasCandidatas()) {
            aulasCandidatas.add(resolver(aulasPorCodigo, a.getCodigo(),
                    "aula candidata", entidad.getCodigo()));
        }

        java.util.Set<es.yaroki.educhronos.solver.domain.Subgrupo> subgrupos =
                new java.util.HashSet<>();
        for (es.yaroki.educhronos.app.catalog.Subgrupo sg : entidad.getSubgrupos()) {
            subgrupos.add(resolver(subgruposPorCodigo, sg.getCodigo(),
                    "subgrupo", entidad.getCodigo()));
        }

        return new es.yaroki.educhronos.solver.domain.Plaza(
                entidad.getCodigo(), asignatura, profesores,
                aulaFija, aulasCandidatas, subgrupos);
    }

    /** Resuelve un código contra un índice o aborta con error de integridad referencial. */
    private static <T> T resolver(Map<String, T> indice, String codigo,
                                  String tipoReferencia, String codigoPlazaOActividad) {
        T valor = indice.get(codigo);
        if (valor == null) {
            throw new IllegalArgumentException(
                    "'" + codigoPlazaOActividad + "' referencia un/a " + tipoReferencia
                            + " no presente en el catálogo mapeado: " + codigo);
        }
        return valor;
    }

    private static es.yaroki.educhronos.solver.domain.PatronTemporal aPatronTemporal(
            es.yaroki.educhronos.app.catalog.PatronTemporal patron) {
        return switch (patron) {
            case DISTRIBUIDA -> es.yaroki.educhronos.solver.domain.PatronTemporal.DISTRIBUIDA;
            case AGRUPADA -> es.yaroki.educhronos.solver.domain.PatronTemporal.AGRUPADA;
            case NEUTRA -> es.yaroki.educhronos.solver.domain.PatronTemporal.NEUTRA;
        };
    }

    /**
     * Lista de {@code TramoSemanal} JPA → lista de {@code domain.Tramo}. Es un
     * mapeo a nivel de lista (no entidad a entidad) porque {@code ordenEnDia}
     * (1..6) no es derivable de un tramo aislado: el {@code orden} de la entidad
     * es un índice GLOBAL que además cuenta el recreo. El método:
     * <ol>
     *   <li>excluye los tramos de recreo ({@code esLectivo=false});
     *   <li>ordena por día y por {@code orden} global;
     *   <li>numera {@code ordenEnDia} 1..6 reiniciando en cada día;
     *   <li>sintetiza el {@code codigo} como inicial del día + ordenEnDia
     *       ({@code "L1".."V6"}), porque la entidad no tiene código natural.
     * </ol>
     * Los rangos (día 1..5, ordenEnDia 1..6) los valida el record del dominio.
     */
    public static List<Tramo> aTramos(List<TramoSemanal> tramos) {
        return aTramosConIndice(tramos).lista();
    }

    /**
     * Núcleo de {@link #aTramos}: produce en un solo recorrido la lista de
     * {@code Tramo} de dominio y el índice {@code TramoSemanal → Tramo} (entidad
     * de origen → tramo mapeado). El índice usa identidad de objeto
     * ({@link IdentityHashMap}) porque la clave es la entidad JPA, no su código.
     * Solo los tramos lectivos entran (el recreo se excluye), así que una entidad
     * de recreo no aparece en el índice: {@link #aRestriccionHoraria} lo aprovecha
     * para abortar si una restricción apunta a un tramo no lectivo. {@link #aTramos}
     * expone solo la lista y conserva su comportamiento observable.
     */
    private static TramosMapeados aTramosConIndice(List<TramoSemanal> tramos) {
        Objects.requireNonNull(tramos, "tramos no puede ser null");
        List<TramoSemanal> lectivos = tramos.stream()
                .filter(TramoSemanal::isEsLectivo)
                .sorted(Comparator.comparingInt((TramoSemanal t) -> t.getDia().ordinal())
                        .thenComparingInt(TramoSemanal::getOrden))
                .toList();

        Map<Dia, Integer> ordenPorDia = new EnumMap<>(Dia.class);
        List<Tramo> resultado = new ArrayList<>(lectivos.size());
        Map<TramoSemanal, Tramo> porEntidad = new IdentityHashMap<>();
        for (TramoSemanal t : lectivos) {
            int ordenEnDia = ordenPorDia.merge(t.getDia(), 1, Integer::sum);
            int diaSemana = t.getDia().ordinal() + 1;
            Tramo tramo = new Tramo(codigoDe(t.getDia(), ordenEnDia), diaSemana, ordenEnDia);
            resultado.add(tramo);
            porEntidad.put(t, tramo);
        }
        return new TramosMapeados(resultado, porEntidad);
    }

    /** Par (lista ordenada, índice por identidad de entidad) que devuelve {@link #aTramosConIndice}. */
    private record TramosMapeados(List<Tramo> lista, Map<TramoSemanal, Tramo> porEntidad) { }

    /**
     * {@code ProfesorRestriccionHoraria} JPA → {@code domain.RestriccionHoraria}.
     * El profesor se resuelve por código contra {@code profesoresPorCodigo}; el
     * tramo por referencia de objeto contra {@code tramosPorEntidad} (el índice
     * que produce {@link #aTramosConIndice}). Un tramo ausente del índice —p. ej.
     * una restricción sobre un tramo de recreo, que {@code aTramos} excluye— es un
     * error de integridad y aborta con excepción explícita. {@code motivo} nullable
     * → {@code Optional.ofNullable}.
     */
    public static RestriccionHoraria aRestriccionHoraria(
            ProfesorRestriccionHoraria entidad,
            Map<String, Profesor> profesoresPorCodigo,
            Map<TramoSemanal, Tramo> tramosPorEntidad) {

        Objects.requireNonNull(entidad, "restriccion no puede ser null");

        String codigoProfesor = entidad.getProfesor().getCodigo();
        Profesor profesor = resolver(profesoresPorCodigo, codigoProfesor, "profesor", codigoProfesor);

        Tramo tramo = tramosPorEntidad.get(entidad.getTramo());
        if (tramo == null) {
            throw new IllegalArgumentException(
                    "La restricción horaria del profesor '" + codigoProfesor
                            + "' referencia un tramo que no está en el catálogo lectivo"
                            + " mapeado (¿un tramo de recreo, excluido por aTramos?)");
        }

        return new RestriccionHoraria(
                profesor,
                tramo,
                aTipoRestriccion(entidad.getTipo()),
                entidad.getPeso(),
                Optional.ofNullable(entidad.getMotivo()));
    }

    private static es.yaroki.educhronos.solver.domain.TipoRestriccion aTipoRestriccion(
            es.yaroki.educhronos.app.catalog.TipoRestriccion tipo) {
        return switch (tipo) {
            case DURA -> es.yaroki.educhronos.solver.domain.TipoRestriccion.DURA;
            case BLANDA -> es.yaroki.educhronos.solver.domain.TipoRestriccion.BLANDA;
        };
    }

    private static TipoGrupo aTipoGrupo(es.yaroki.educhronos.app.catalog.TipoGrupo tipo) {
        return switch (tipo) {
            case ORDINARIO -> TipoGrupo.ORDINARIO;
            case DIVERSIFICACION_PDC -> TipoGrupo.DIVERSIFICACION_PDC;
            case VIRTUAL_OPTATIVA -> throw new IllegalArgumentException(
                    "TipoGrupo VIRTUAL_OPTATIVA no tiene equivalente en el dominio del "
                    + "solver; no se mapea a ORDINARIO ni se filtra en silencio");
        };
    }

    private static String codigoDe(Dia dia, int ordenEnDia) {
        return inicial(dia) + ordenEnDia;
    }

    private static String inicial(Dia dia) {
        return switch (dia) {
            case LUNES -> "L";
            case MARTES -> "M";
            case MIERCOLES -> "X";
            case JUEVES -> "J";
            case VIERNES -> "V";
        };
    }
}
