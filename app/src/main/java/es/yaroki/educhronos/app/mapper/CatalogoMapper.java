package es.yaroki.educhronos.app.mapper;

import es.yaroki.educhronos.app.catalog.Dia;
import es.yaroki.educhronos.app.catalog.PatronTemporal;
import es.yaroki.educhronos.app.catalog.TramoSemanal;
import es.yaroki.educhronos.solver.domain.Asignatura;
import es.yaroki.educhronos.solver.domain.Aula;
import es.yaroki.educhronos.solver.domain.GrupoAdministrativo;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.TipoGrupo;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Convierte entidades JPA del catálogo del centro ({@code app.catalog}) al
 * modelo puro del solver ({@code solver.domain}). Primer tramo del mapper
 * (Fase 6, Bloque 3): solo el catálogo que el solver consume; el ensamblado de
 * {@code ProblemaHorario} (faltan Subgrupo/Actividad como entidades) y el mapeo
 * inverso (solución → entidades) llegan en bloques posteriores.
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
        Objects.requireNonNull(tramos, "tramos no puede ser null");
        List<TramoSemanal> lectivos = tramos.stream()
                .filter(TramoSemanal::isEsLectivo)
                .sorted(Comparator.comparingInt((TramoSemanal t) -> t.getDia().ordinal())
                        .thenComparingInt(TramoSemanal::getOrden))
                .toList();

        Map<Dia, Integer> ordenPorDia = new EnumMap<>(Dia.class);
        List<Tramo> resultado = new ArrayList<>(lectivos.size());
        for (TramoSemanal t : lectivos) {
            int ordenEnDia = ordenPorDia.merge(t.getDia(), 1, Integer::sum);
            int diaSemana = t.getDia().ordinal() + 1;
            resultado.add(new Tramo(codigoDe(t.getDia(), ordenEnDia), diaSemana, ordenEnDia));
        }
        return resultado;
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
