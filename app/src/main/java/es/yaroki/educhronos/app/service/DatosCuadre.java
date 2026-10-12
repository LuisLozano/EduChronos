package es.yaroki.educhronos.app.service;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Lo que el cuadre de horas declaradas necesita y el {@code ProblemaHorario} del solver no lleva
 * (S203, C-totales-y-cargo, D1): los totales declarados y qué actividades no son CLASE. Desde S212
 * (C-dato-guardias) lleva también las guardias: el mínimo de profesores de guardia por tramo del
 * centro y las guardias ordinarias de cada profesor; desde S219 (C-dato-guardias-especiales), las
 * de recreo, los dos mínimos por recreo, el número de recreos de la jornada y el nombre de cada
 * profesor para los mensajes. Vive en la capa app; el solver no sabe de totales, ni de tipos de
 * actividad, ni de guardias, ni de recreos.
 *
 * <p>Todo va por CÓDIGO, la clave natural con la que el dominio del solver identifica profesores,
 * grupos y actividades. Lo construye {@link GeneradorHorarioService#cargarDatosCuadre()} desde el
 * catálogo JPA.
 *
 * @param declaradasProfesor total declarado por código de profesor; SOLO los que lo tienen
 * @param declaradasGrupo    total declarado por código de grupo (ordinario o PDC); SOLO los que lo
 *                           tienen
 * @param actividadesNoClase códigos de las actividades de tipo distinto de CLASE (reuniones y
 *                           funciones), que el cuadre no cuenta
 * @param minimoGuardiasPorTramo profesores de guardia que el centro pide en cada tramo de clase;
 *                           0 = el centro no usa guardias
 * @param guardiasPorProfesor guardias ordinarias por código de profesor, la clave de
 *                           {@code PROFESOR_SOBRECARGADO}; un profesor ausente tiene 0
 * @param guardiasRecreoPorProfesor guardias de recreo por código de profesor, con la misma clave;
 *                           un profesor ausente tiene 0 (S219)
 * @param nombresProfesor    nombre completo por código de profesor, para los mensajes (S219)
 * @param minimoGuardiasPorRecreo profesores de guardia que el centro pide en cada recreo (S219)
 * @param minimoBibliotecaPorRecreo de ellos, los que están en la biblioteca (S219)
 * @param recreos            tramos no lectivos de la jornada, que el {@code ProblemaHorario} no
 *                           lleva (S219)
 */
public record DatosCuadre(
        Map<String, Integer> declaradasProfesor,
        Map<String, Integer> declaradasGrupo,
        Set<String> actividadesNoClase,
        int minimoGuardiasPorTramo,
        Map<String, Integer> guardiasPorProfesor,
        Map<String, Integer> guardiasRecreoPorProfesor,
        Map<String, String> nombresProfesor,
        int minimoGuardiasPorRecreo,
        int minimoBibliotecaPorRecreo,
        int recreos) {

    /**
     * Sin totales, todo CLASE y sin guardias (mínimo 0): ni el cuadre ni las reglas de guardias
     * emiten nada. Para quien no lo necesite.
     */
    public static final DatosCuadre VACIO = new DatosCuadre(Map.of(), Map.of(), Set.of(), 0, Map.of());

    public DatosCuadre {
        Objects.requireNonNull(declaradasProfesor, "declaradasProfesor no puede ser null");
        Objects.requireNonNull(declaradasGrupo, "declaradasGrupo no puede ser null");
        Objects.requireNonNull(actividadesNoClase, "actividadesNoClase no puede ser null");
        Objects.requireNonNull(guardiasPorProfesor, "guardiasPorProfesor no puede ser null");
        Objects.requireNonNull(guardiasRecreoPorProfesor, "guardiasRecreoPorProfesor no puede ser null");
        Objects.requireNonNull(nombresProfesor, "nombresProfesor no puede ser null");
        declaradasProfesor = Map.copyOf(declaradasProfesor);
        declaradasGrupo = Map.copyOf(declaradasGrupo);
        actividadesNoClase = Set.copyOf(actividadesNoClase);
        guardiasPorProfesor = Map.copyOf(guardiasPorProfesor);
        guardiasRecreoPorProfesor = Map.copyOf(guardiasRecreoPorProfesor);
        nombresProfesor = Map.copyOf(nombresProfesor);
    }

    /** Cuadre y guardias ordinarias (S212): sin guardias de recreo ni recreos. */
    public DatosCuadre(Map<String, Integer> declaradasProfesor, Map<String, Integer> declaradasGrupo,
                       Set<String> actividadesNoClase, int minimoGuardiasPorTramo,
                       Map<String, Integer> guardiasPorProfesor) {
        this(declaradasProfesor, declaradasGrupo, actividadesNoClase, minimoGuardiasPorTramo,
                guardiasPorProfesor, Map.of(), Map.of(), 0, 0, 0);
    }

    /** Solo el cuadre (S203): sin guardias, como {@link #VACIO}. */
    public DatosCuadre(Map<String, Integer> declaradasProfesor, Map<String, Integer> declaradasGrupo,
                       Set<String> actividadesNoClase) {
        this(declaradasProfesor, declaradasGrupo, actividadesNoClase, 0, Map.of());
    }
}
