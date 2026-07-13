package es.yaroki.educhronos.solver.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Salida del solver: asignación de cada ActividadInstancia a un Tramo y, para
 * las plazas con aula variable ({@code aulasCandidatas}), el aula que el solver
 * eligió.
 *
 * <p>Las aulas de plazas con {@code aulaFija} NO se almacenan aquí: su aula no
 * es resultado del solve, sale de la propia plaza. El accessor unificado
 * {@link #aulaElegida(ActividadInstancia, Plaza)} oculta esa diferencia al
 * consumidor: devuelve el aula elegida si la plaza era variable, o su
 * {@code aulaFija} si era fija.
 *
 * <p>El constructor de un solo argumento se mantiene para soluciones sin aula
 * variable (incluidas las que se fabrican a mano en tests): equivale a pasar un
 * mapa de aulas elegidas vacío.
 */
public class SolucionHorario {

    private final Map<ActividadInstancia, Tramo> asignaciones;
    private final Map<ActividadInstancia, Map<Plaza, Aula>> aulasElegidas;

    public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones) {
        this(asignaciones, Map.of());
    }

    public SolucionHorario(Map<ActividadInstancia, Tramo> asignaciones,
                           Map<ActividadInstancia, Map<Plaza, Aula>> aulasElegidas) {
        Objects.requireNonNull(asignaciones, "asignaciones no puede ser null");
        Objects.requireNonNull(aulasElegidas, "aulasElegidas no puede ser null (usa Map.of())");
        this.asignaciones = Map.copyOf(asignaciones);
        // Copia defensiva anidada: Map.copyOf no congela los mapas interiores.
        Map<ActividadInstancia, Map<Plaza, Aula>> copia = new HashMap<>();
        for (Map.Entry<ActividadInstancia, Map<Plaza, Aula>> e : aulasElegidas.entrySet()) {
            copia.put(e.getKey(), Map.copyOf(e.getValue()));
        }
        this.aulasElegidas = Map.copyOf(copia);
    }

    public Optional<Tramo> tramoDeInstancia(ActividadInstancia instancia) {
        return Optional.ofNullable(asignaciones.get(instancia));
    }

    /**
     * Aula de una plaza concreta dentro de una instancia. Punto único de verdad
     * para consumidores (verificador, materializador): devuelve el aula que el
     * solver eligió si la plaza tenía {@code aulasCandidatas}, o su
     * {@code aulaFija} si era fija. Empty solo si la plaza no tiene aula fija y
     * tampoco se registró elección (no debería ocurrir en una solución válida).
     */
    public Optional<Aula> aulaElegida(ActividadInstancia instancia, Plaza plaza) {
        Map<Plaza, Aula> porPlaza = aulasElegidas.get(instancia);
        if (porPlaza != null && porPlaza.containsKey(plaza)) {
            return Optional.of(porPlaza.get(plaza));
        }
        return plaza.aulaFija();
    }

    public Map<ActividadInstancia, Tramo> asignaciones() {
        return asignaciones;
    }

    /**
     * Aulas que el solver ELIGIÓ, por instancia y plaza. Solo contiene las plazas con
     * {@code aulasCandidatas}: las de {@code aulaFija} NO aparecen aquí (su aula no es
     * resultado del solve). Para obtener el aula EFECTIVA de cualquier plaza —fija o
     * variable— usa {@link #aulaElegida(ActividadInstancia, Plaza)}, que oculta esa
     * diferencia; este accessor la EXPONE, y existe para que esa distinción sea
     * verificable desde fuera (Bloque 8.3-C).
     *
     * <p>El mapa devuelto es inmutable (congelado por el constructor con
     * {@code Map.copyOf} anidado).
     */
    public Map<ActividadInstancia, Map<Plaza, Aula>> aulasElegidas() {
        return aulasElegidas;
    }
}
