package es.yaroki.educhronos.app.service;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Lo que el cuadre de horas declaradas necesita y el {@code ProblemaHorario} del solver no lleva
 * (S203, C-totales-y-cargo, D1): los totales declarados y qué actividades no son CLASE. Vive en
 * la capa app; el solver no sabe de totales ni de tipos de actividad.
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
 */
public record DatosCuadre(
        Map<String, Integer> declaradasProfesor,
        Map<String, Integer> declaradasGrupo,
        Set<String> actividadesNoClase) {

    /** Sin totales y todo CLASE: el cuadre no emite nada. Para quien no lo necesite. */
    public static final DatosCuadre VACIO = new DatosCuadre(Map.of(), Map.of(), Set.of());

    public DatosCuadre {
        Objects.requireNonNull(declaradasProfesor, "declaradasProfesor no puede ser null");
        Objects.requireNonNull(declaradasGrupo, "declaradasGrupo no puede ser null");
        Objects.requireNonNull(actividadesNoClase, "actividadesNoClase no puede ser null");
        declaradasProfesor = Map.copyOf(declaradasProfesor);
        declaradasGrupo = Map.copyOf(declaradasGrupo);
        actividadesNoClase = Set.copyOf(actividadesNoClase);
    }
}
