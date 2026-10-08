package es.yaroki.educhronos.app.mapper;

import es.yaroki.educhronos.app.catalog.Actividad;
import es.yaroki.educhronos.app.catalog.AsignaturaAula;
import es.yaroki.educhronos.app.catalog.Aula;
import es.yaroki.educhronos.app.catalog.GrupoAdministrativo;
import es.yaroki.educhronos.app.catalog.Plaza;
import es.yaroki.educhronos.app.catalog.RolAulaAsignatura;
import es.yaroki.educhronos.app.catalog.Subgrupo;
import es.yaroki.educhronos.app.catalog.TipoActividad;
import es.yaroki.educhronos.app.catalog.TipoGrupo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Aulas posibles de cada plaza de CLASE a partir de las reglas del centro (S207,
 * C-deduccion-aulas, A). Es el ÚNICO sitio que las calcula: lo usan la construcción del problema
 * ({@link CatalogoMapper}) y la prevalidación (vía {@code GeneradorHorarioService.cargarDatosAulas}),
 * para que nunca discrepen. El verificador del solver no lo usa: es oráculo independiente.
 *
 * <ul>
 *   <li>A1. Plaza con aula escrita (fija o candidatas): su dominio es lo escrito.</li>
 *   <li>A2. Sin aula escrita: S = grupos efectivos de sus subgrupos (un PDC cuenta como su
 *       padre). Si la asignatura de la plaza tiene aulas EXCLUSIVAS, el dominio son ellas; si
 *       no, el aula de grupo de cada G de S que la tenga más las PREFERIDAS de la asignatura.</li>
 *   <li>A3. Fuera, sobre el dominio escrito o deducido, las aulas marcadas «No se usa» y las de
 *       capacidad conocida menor que la suma de alumnos conocidos de los subgrupos de la plaza
 *       (alumnos desconocidos cuentan 0; capacidad desconocida no limita; igual cabe).</li>
 *   <li>A5. Las plazas que no son de CLASE no pasan por aquí: {@link #aplica} es falso y el
 *       mapper las traduce como siempre.</li>
 *   <li>A6. Orden estable: aulas por id ascendente (y por código si no hay id).</li>
 * </ul>
 * A4 (una aula → fija, varias → candidatas) lo aplica el mapper sobre el resultado.
 *
 * <p><b>Preferidas efectivas (S208, C-preferencias-aulas).</b> {@link Dominio#preferidas()} son
 * las PREFERIDAS de la asignatura que siguen en el dominio tras A3 («No se usa» y capacidad).
 * Solo las tiene una plaza cuyo dominio sale de A2 sin EXCLUSIVAS: con aula escrita (A1,
 * decisión L) o con EXCLUSIVAS no hay ninguna, y una asignatura sin PREFERIDAS tampoco las da.
 * El aula de grupo es posible pero no preferida. Se calculan aquí, en el mismo paso que el
 * dominio, para que el rol no se pierda; la prevalidación sigue mirando solo {@link Dominio#vacio()}
 * y {@link Dominio#motivo()}.
 *
 * <p>Si el dominio queda vacío, {@link Dominio#motivo()} dice por qué, en el texto que verá el
 * usuario. Si lo vació más de una causa, manda la que eliminó la ÚLTIMA aula, en orden de id; y
 * de un aula «No se usa» que además no tiene sitio se dice que no se usa. Los motivos del aula
 * escrita nombran el aula y se usan cuando lo escrito era UNA sola aula; con varias candidatas
 * escritas se usan los motivos generales.
 */
public final class DeduccionAulas {

    /** No hay aula escrita ni nada que deducir de las reglas. */
    public static final String SIN_REGLAS = "sin aula de grupo ni aulas de la asignatura";
    /** Todas las aulas posibles, escritas o deducidas, eliminadas por «No se usa». */
    public static final String TODAS_NO_SE_USAN = "todas sus aulas posibles están marcadas No se usa";

    private DeduccionAulas() { }

    /**
     * Dominio de aulas de una plaza: la lista ordenada (vacía si no hay ninguna posible), sus
     * PREFERIDAS EFECTIVAS (S208) y, solo si está vacía, el motivo. Las preferidas son siempre
     * parte de las aulas, en el mismo orden.
     */
    public record Dominio(List<Aula> aulas, List<Aula> preferidas, String motivo) {
        public Dominio {
            aulas = List.copyOf(aulas);
            preferidas = List.copyOf(preferidas);
            if (aulas.isEmpty() == (motivo == null)) {
                throw new IllegalArgumentException("un dominio vacío lleva motivo, y uno con aulas no");
            }
            TreeSet<Aula> posibles = new TreeSet<>(ORDEN);
            posibles.addAll(aulas);
            if (!posibles.containsAll(preferidas)) {
                throw new IllegalArgumentException("las preferidas tienen que ser aulas del dominio");
            }
        }

        /** Dominio sin preferidas. */
        public Dominio(List<Aula> aulas, String motivo) {
            this(aulas, List.of(), motivo);
        }

        public boolean vacio() {
            return aulas.isEmpty();
        }
    }

    /** Orden estable de A6: id ascendente; sin id (entidad sin persistir), por código. */
    static final Comparator<Aula> ORDEN = Comparator
            .comparing(Aula::getId, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Aula::getCodigo);

    /** Las aulas de cada asignatura, por id de asignatura, a partir de todas las filas. */
    public static Map<Long, List<AsignaturaAula>> indicePorAsignatura(List<AsignaturaAula> filas) {
        Objects.requireNonNull(filas, "filas no puede ser null");
        Map<Long, List<AsignaturaAula>> indice = new HashMap<>();
        for (AsignaturaAula fila : filas) {
            indice.computeIfAbsent(fila.getAsignatura().getId(), id -> new ArrayList<>()).add(fila);
        }
        return indice;
    }

    /**
     * Si la deducción aplica a las plazas de la actividad: solo a las de CLASE (A5). Recibe la
     * actividad y no la saca de la plaza porque el mapper la tiene a mano y una plaza recién
     * construida puede no tener todavía su enlace a la actividad.
     */
    public static boolean aplica(Actividad actividad) {
        return actividad.getTipo() == TipoActividad.CLASE;
    }

    /**
     * Dominio de una plaza de CLASE.
     *
     * @param actividad      actividad de la plaza, de CLASE ({@link #aplica} verdadero)
     * @param plaza          la plaza
     * @param aulasPorAsignatura índice de {@link #indicePorAsignatura}
     */
    public static Dominio dominio(Actividad actividad, Plaza plaza,
                                  Map<Long, List<AsignaturaAula>> aulasPorAsignatura) {
        Objects.requireNonNull(actividad, "actividad no puede ser null");
        Objects.requireNonNull(plaza, "plaza no puede ser null");
        Objects.requireNonNull(aulasPorAsignatura, "aulasPorAsignatura no puede ser null");
        if (!aplica(actividad)) {
            throw new IllegalArgumentException(
                    "la deducción de aulas es solo para plazas de CLASE: " + plaza.getCodigo());
        }

        TreeSet<Aula> previas = new TreeSet<>(ORDEN);
        TreeSet<Aula> preferidasDeLaAsignatura = new TreeSet<>(ORDEN);       // S208: el rol
        boolean escrita = plaza.getAulaFija() != null || !plaza.getAulasCandidatas().isEmpty();
        if (plaza.getAulaFija() != null) {
            previas.add(plaza.getAulaFija());                                   // A1
        } else if (escrita) {
            previas.addAll(plaza.getAulasCandidatas());                         // A1
        } else {
            Deducidas deducidas = deducidas(plaza, aulasPorAsignatura);         // A2
            previas.addAll(deducidas.aulas());
            preferidasDeLaAsignatura.addAll(deducidas.preferidas());
        }
        if (previas.isEmpty()) {
            return new Dominio(List.of(), SIN_REGLAS);
        }

        int alumnos = alumnosDe(plaza);
        List<Aula> posibles = new ArrayList<>();
        String motivo = null;
        for (Aula aula : previas) {                                            // A3, en orden A6
            if (!aula.isEnUso()) {
                motivo = previas.size() == 1 && escrita
                        ? "el aula escrita " + aula.getCodigo() + " está marcada No se usa"
                        : TODAS_NO_SE_USAN;
            } else if (aula.getCapacidad() != null && aula.getCapacidad() < alumnos) {
                motivo = previas.size() == 1 && escrita
                        ? "el aula escrita " + aula.getCodigo() + " no tiene sitio para " + alumnos + " alumnos"
                        : "ninguna de sus aulas posibles tiene sitio para " + alumnos + " alumnos";
            } else {
                posibles.add(aula);
            }
        }
        if (posibles.isEmpty()) {
            return new Dominio(List.of(), motivo);
        }
        List<Aula> preferidas = posibles.stream()                              // S208: tras A3
                .filter(preferidasDeLaAsignatura::contains)
                .toList();
        return new Dominio(posibles, preferidas, null);
    }

    /** Lo que deduce A2: las aulas y, de ellas, cuáles son PREFERIDAS de la asignatura. */
    private record Deducidas(List<Aula> aulas, List<Aula> preferidas) {
    }

    /**
     * A2: exclusivas de la asignatura (ninguna preferida: exclusiva no es preferida), o aula de
     * grupo de cada grupo efectivo más preferidas, conservando cuáles son las preferidas.
     */
    private static Deducidas deducidas(Plaza plaza, Map<Long, List<AsignaturaAula>> aulasPorAsignatura) {
        List<AsignaturaAula> deLaAsignatura =
                aulasPorAsignatura.getOrDefault(plaza.getAsignatura().getId(), List.of());
        List<Aula> exclusivas = deLaAsignatura.stream()
                .filter(f -> f.getRol() == RolAulaAsignatura.EXCLUSIVA)
                .map(AsignaturaAula::getAula)
                .toList();
        if (!exclusivas.isEmpty()) {
            return new Deducidas(exclusivas, List.of());
        }
        List<Aula> resultado = new ArrayList<>();
        for (Subgrupo subgrupo : plaza.getSubgrupos()) {
            for (GrupoAdministrativo grupo : subgrupo.getGrupos()) {
                Optional.ofNullable(efectivo(grupo).getAulaReferencia()).ifPresent(resultado::add);
            }
        }
        List<Aula> preferidas = deLaAsignatura.stream()
                .filter(f -> f.getRol() == RolAulaAsignatura.PREFERIDA)
                .map(AsignaturaAula::getAula)
                .toList();
        resultado.addAll(preferidas);
        return new Deducidas(resultado, preferidas);
    }

    /** Grupo efectivo: un PDC cuenta como su padre; cualquier otro, como él mismo. */
    static GrupoAdministrativo efectivo(GrupoAdministrativo grupo) {
        return grupo.getTipo() == TipoGrupo.DIVERSIFICACION_PDC && grupo.getGrupoPadre() != null
                ? grupo.getGrupoPadre()
                : grupo;
    }

    /** Suma de alumnos conocidos de los subgrupos de la plaza; los desconocidos cuentan 0. */
    static int alumnosDe(Plaza plaza) {
        int total = 0;
        for (Subgrupo subgrupo : plaza.getSubgrupos()) {
            if (subgrupo.getAlumnos() != null) {
                total += subgrupo.getAlumnos();
            }
        }
        return total;
    }
}
