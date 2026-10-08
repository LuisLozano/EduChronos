package es.yaroki.educhronos.app.service;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Lo que el cuadre de horas declaradas necesita y el {@code ProblemaHorario} del solver no lleva
 * (S203, C-totales-y-cargo, D1): los totales declarados y qué actividades no son CLASE. Desde S212
 * (C-dato-guardias) lleva también las guardias: el mínimo de profesores de guardia por tramo del
 * centro y las guardias ordinarias de cada profesor. Vive en la capa app; el solver no sabe de
 * totales, ni de tipos de actividad, ni de guardias.
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
 */
public record DatosCuadre(
        Map<String, Integer> declaradasProfesor,
        Map<String, Integer> declaradasGrupo,
        Set<String> actividadesNoClase,
        int minimoGuardiasPorTramo,
        Map<String, Integer> guardiasPorProfesor) {

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
        declaradasProfesor = Map.copyOf(declaradasProfesor);
        declaradasGrupo = Map.copyOf(declaradasGrupo);
        actividadesNoClase = Set.copyOf(actividadesNoClase);
        guardiasPorProfesor = Map.copyOf(guardiasPorProfesor);
    }

    /** Solo el cuadre (S203): sin guardias, como {@link #VACIO}. */
    public DatosCuadre(Map<String, Integer> declaradasProfesor, Map<String, Integer> declaradasGrupo,
                       Set<String> actividadesNoClase) {
        this(declaradasProfesor, declaradasGrupo, actividadesNoClase, 0, Map.of());
    }
}
