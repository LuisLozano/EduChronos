package es.yaroki.educhronos.solver.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Entrada completa del solver. Agrupa todas las colecciones del dominio.
 * Los tramos deben estar ordenados por (diaSemana, ordenEnDia) y sin dos con el mismo par;
 * el constructor lo comprueba (S166, ver {@code comprobarOrdenDeTramos}).
 *
 * <p>{@code capacidadesDeAula} y {@code alumnosDeSubgrupo} (S207, C-deduccion-aulas, C2): plazas
 * de cada aula y alumnos de cada subgrupo, POR CÓDIGO. Viven aquí y no en {@link Aula} ni en
 * {@link Subgrupo} para no tocar su identidad: los dos se usan como clave de mapas en el modelo,
 * el verificador y la solución. Un código ausente es «sin dato» (lo que en la base es nulo); un
 * código que no está en {@code aulas} o {@code subgrupos}, o un valor negativo, se rechaza. El
 * constructor de diez listas equivale a no tener ningún dato.
 *
 * <p>{@code preferidasDePlaza} (S208, C-preferencias-aulas, C2): las PREFERIDAS EFECTIVAS de cada
 * plaza, código de plaza → códigos de aula, mismo patrón. Las calcula la aplicación (preferidas de
 * la asignatura que están entre las aulas posibles de la plaza); el problema solo comprueba que
 * cada clave sea el código de una sola plaza suya y que sus preferidas estén dentro de su aula
 * fija o sus candidatas. Una plaza sin entrada, o con el conjunto vacío, no tiene preferidas y
 * no paga nada en el término de aula no preferida. Los constructores de diez y doce componentes
 * equivalen a no tener ninguna.
 */
public record ProblemaHorario(
        List<Tramo> tramos,
        List<Aula> aulas,
        List<Asignatura> asignaturas,
        List<Profesor> profesores,
        List<GrupoAdministrativo> grupos,
        List<Subgrupo> subgrupos,
        List<Actividad> actividades,
        List<RestriccionHoraria> restriccionesHorarias,
        List<SesionBloqueada> bloqueos,
        List<ProfesorTutoria> tutorias,
        Map<String, Integer> capacidadesDeAula,
        Map<String, Integer> alumnosDeSubgrupo,
        Map<String, Set<String>> preferidasDePlaza) {

    /** Problema sin capacidades de aula ni alumnos de subgrupo: el de antes de S207. */
    public ProblemaHorario(
            List<Tramo> tramos,
            List<Aula> aulas,
            List<Asignatura> asignaturas,
            List<Profesor> profesores,
            List<GrupoAdministrativo> grupos,
            List<Subgrupo> subgrupos,
            List<Actividad> actividades,
            List<RestriccionHoraria> restriccionesHorarias,
            List<SesionBloqueada> bloqueos,
            List<ProfesorTutoria> tutorias) {
        this(tramos, aulas, asignaturas, profesores, grupos, subgrupos, actividades,
                restriccionesHorarias, bloqueos, tutorias, Map.of(), Map.of(), Map.of());
    }

    /** Problema sin preferidas efectivas: el de antes de S208. */
    public ProblemaHorario(
            List<Tramo> tramos,
            List<Aula> aulas,
            List<Asignatura> asignaturas,
            List<Profesor> profesores,
            List<GrupoAdministrativo> grupos,
            List<Subgrupo> subgrupos,
            List<Actividad> actividades,
            List<RestriccionHoraria> restriccionesHorarias,
            List<SesionBloqueada> bloqueos,
            List<ProfesorTutoria> tutorias,
            Map<String, Integer> capacidadesDeAula,
            Map<String, Integer> alumnosDeSubgrupo) {
        this(tramos, aulas, asignaturas, profesores, grupos, subgrupos, actividades,
                restriccionesHorarias, bloqueos, tutorias, capacidadesDeAula, alumnosDeSubgrupo,
                Map.of());
    }

    public ProblemaHorario {
        Objects.requireNonNull(tramos,      "tramos no puede ser null");
        Objects.requireNonNull(aulas,       "aulas no puede ser null");
        Objects.requireNonNull(asignaturas, "asignaturas no puede ser null");
        Objects.requireNonNull(profesores,  "profesores no puede ser null");
        Objects.requireNonNull(grupos,      "grupos no puede ser null");
        Objects.requireNonNull(subgrupos,   "subgrupos no puede ser null");
        Objects.requireNonNull(actividades, "actividades no puede ser null");
        Objects.requireNonNull(restriccionesHorarias, "restriccionesHorarias no puede ser null");
        Objects.requireNonNull(bloqueos,    "bloqueos no puede ser null");
        Objects.requireNonNull(tutorias,    "tutorias no puede ser null");
        Objects.requireNonNull(capacidadesDeAula, "capacidadesDeAula no puede ser null (usa Map.of())");
        Objects.requireNonNull(alumnosDeSubgrupo, "alumnosDeSubgrupo no puede ser null (usa Map.of())");
        Objects.requireNonNull(preferidasDePlaza, "preferidasDePlaza no puede ser null (usa Map.of())");

        tramos      = List.copyOf(tramos);
        comprobarOrdenDeTramos(tramos);
        aulas       = List.copyOf(aulas);
        asignaturas = List.copyOf(asignaturas);
        profesores  = List.copyOf(profesores);
        grupos      = List.copyOf(grupos);
        subgrupos   = List.copyOf(subgrupos);
        actividades = List.copyOf(actividades);
        restriccionesHorarias = List.copyOf(restriccionesHorarias);
        bloqueos    = List.copyOf(bloqueos);
        tutorias    = List.copyOf(tutorias);
        capacidadesDeAula = Map.copyOf(capacidadesDeAula);
        alumnosDeSubgrupo = Map.copyOf(alumnosDeSubgrupo);
        comprobarDatosPorCodigo("capacidad", capacidadesDeAula,
                codigos(aulas.stream().map(Aula::codigo).toList()), "aula");
        comprobarDatosPorCodigo("alumnos", alumnosDeSubgrupo,
                codigos(subgrupos.stream().map(Subgrupo::codigo).toList()), "subgrupo");
        Map<String, Set<String>> copiaPreferidas = new HashMap<>();
        preferidasDePlaza.forEach((plaza, preferidas) -> copiaPreferidas.put(plaza, Set.copyOf(preferidas)));
        preferidasDePlaza = Map.copyOf(copiaPreferidas);
        comprobarPreferidas(preferidasDePlaza, actividades);
    }

    /** Códigos de las aulas preferidas efectivas de la plaza; vacío si no tiene. */
    public Set<String> preferidasDe(Plaza plaza) {
        return preferidasDePlaza.getOrDefault(plaza.codigo(), Set.of());
    }

    /** Plazas del aula, o vacío si no se conocen (sin límite). */
    public Optional<Integer> capacidadDe(Aula aula) {
        return Optional.ofNullable(capacidadesDeAula.get(aula.codigo()));
    }

    /** Alumnos del subgrupo, o vacío si no se conocen. */
    public Optional<Integer> alumnosDe(Subgrupo subgrupo) {
        return Optional.ofNullable(alumnosDeSubgrupo.get(subgrupo.codigo()));
    }

    private static Set<String> codigos(List<String> lista) {
        return new HashSet<>(lista);
    }

    /** Cada clave es un código del problema y cada valor, no negativo. */
    private static void comprobarDatosPorCodigo(String dato, Map<String, Integer> porCodigo,
                                                Set<String> codigosValidos, String entidad) {
        for (Map.Entry<String, Integer> e : porCodigo.entrySet()) {
            if (!codigosValidos.contains(e.getKey()))
                throw new IllegalArgumentException(
                        dato + " de un " + entidad + " que no está en el problema: '" + e.getKey()
                                + "'");
            if (e.getValue() < 0)
                throw new IllegalArgumentException(
                        "valor negativo de " + dato + " en el " + entidad + " '" + e.getKey() + "': "
                                + e.getValue());
        }
    }

    /**
     * Cada clave es el código de UNA plaza del problema, y sus preferidas están dentro de sus
     * aulas posibles (aula fija o candidatas). Un código de plaza repetido se rechaza: el dato va
     * por código y no sabría a cuál de las dos se refiere.
     */
    private static void comprobarPreferidas(Map<String, Set<String>> preferidasDePlaza,
                                            List<Actividad> actividades) {
        if (preferidasDePlaza.isEmpty()) {
            return;
        }
        Map<String, List<Plaza>> plazasPorCodigo = new HashMap<>();
        for (Actividad actividad : actividades) {
            for (Plaza plaza : actividad.plazas()) {
                plazasPorCodigo.computeIfAbsent(plaza.codigo(), c -> new ArrayList<>())
                        .add(plaza);
            }
        }
        for (Map.Entry<String, Set<String>> e : preferidasDePlaza.entrySet()) {
            List<Plaza> plazas = plazasPorCodigo.getOrDefault(e.getKey(), List.of());
            if (plazas.isEmpty())
                throw new IllegalArgumentException(
                        "preferidas de una plaza que no está en el problema: '" + e.getKey() + "'");
            if (plazas.size() > 1)
                throw new IllegalArgumentException(
                        "preferidas de un código de plaza repetido en el problema: '" + e.getKey() + "'");
            Plaza plaza = plazas.get(0);
            Set<String> posibles = new HashSet<>();
            plaza.aulaFija().ifPresent(a -> posibles.add(a.codigo()));
            plaza.aulasCandidatas().forEach(a -> posibles.add(a.codigo()));
            for (String preferida : e.getValue()) {
                if (!posibles.contains(preferida))
                    throw new IllegalArgumentException(
                            "la preferida '" + preferida + "' de la plaza '" + e.getKey()
                                    + "' no es ninguna de sus aulas posibles "
                                    + posibles.stream().sorted().toList());
            }
        }
    }

    /** Orden de la invariante: por día y, dentro del día, por {@code ordenEnDia}. */
    private static final Comparator<Tramo> ORDEN_DE_TRAMOS =
            Comparator.comparingInt(Tramo::diaSemana).thenComparingInt(Tramo::ordenEnDia);

    /**
     * Invariante de indexación (S166): tramos ordenados por {@code (diaSemana, ordenEnDia)} y
     * sin dos con el mismo par. Así los índices de un día son contiguos y crecen con
     * {@code ordenEnDia}, que es lo que el solver supone cuando un bloque que empieza en el
     * índice {@code t} ocupa {@code t..t+d−1}. NO exige {@code ordenEnDia} sin huecos: un bloque
     * no puede arrancar donde cruzaría el hueco (lista blanca de inicios del modelo).
     */
    private static void comprobarOrdenDeTramos(List<Tramo> tramos) {
        for (int i = 1; i < tramos.size(); i++) {
            Tramo antes = tramos.get(i - 1);
            Tramo t = tramos.get(i);
            int comparacion = ORDEN_DE_TRAMOS.compare(antes, t);
            if (comparacion == 0)
                throw new IllegalArgumentException(
                        "cada (diaSemana, ordenEnDia) debe ser de un solo tramo, recibido: '"
                                + antes.codigo() + "' y '" + t.codigo() + "' en ("
                                + t.diaSemana() + ", " + t.ordenEnDia() + ")");
            if (comparacion > 0)
                throw new IllegalArgumentException(
                        "los tramos deben ir ordenados por (diaSemana, ordenEnDia), recibido: '"
                                + antes.codigo() + "' (" + antes.diaSemana() + ", "
                                + antes.ordenEnDia() + ") antes que '" + t.codigo() + "' ("
                                + t.diaSemana() + ", " + t.ordenEnDia() + ")");
        }
    }

    /** Índice del tramo en la lista ordenada. Usado por el solver para construir IntVar. */
    public int indiceDeTramo(Tramo tramo) {
        int idx = tramos.indexOf(tramo);
        if (idx < 0)
            throw new IllegalArgumentException("Tramo no pertenece al problema: " + tramo.codigo());
        return idx;
    }
}