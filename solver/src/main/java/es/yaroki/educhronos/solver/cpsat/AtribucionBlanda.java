package es.yaroki.educhronos.solver.cpsat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Atribución CONTRAFACTUAL de las reglas blandas a las celdas del horario (Fase 8,
 * Bloque 8.3-B). Para cada celda, la lista de {@link Penalizacion} que aporta a los
 * términos blandos del objetivo. La produce
 * {@link VerificadorSolucion#atribuirBlandas}.
 *
 * <p>Una celda SIN aportación no aparece en {@link #porCelda()} (no aparece con
 * lista vacía: no aparece). Coherente con no emitir {@code Penalizacion} de
 * {@code delta == 0}.
 */
public record AtribucionBlanda(Map<CeldaRef, List<Penalizacion>> porCelda) {

    public AtribucionBlanda {
        Objects.requireNonNull(porCelda, "porCelda no puede ser null");
        // Copia defensiva profunda: congela el mapa y cada lista interior.
        Map<CeldaRef, List<Penalizacion>> copia = new LinkedHashMap<>();
        for (Map.Entry<CeldaRef, List<Penalizacion>> e : porCelda.entrySet()) {
            copia.put(e.getKey(), List.copyOf(e.getValue()));
        }
        porCelda = Map.copyOf(copia);
    }

    /**
     * Suma con signo de las aportaciones de una celda a TODAS las reglas blandas.
     * 0 si la celda no aparece (sin aportación). Es la ganancia neta esperada de
     * mover esa celda: {@code > 0} conviene moverla, {@code < 0} conviene dejarla.
     */
    public int deltaTotal(CeldaRef celda) {
        List<Penalizacion> lista = porCelda.get(celda);
        if (lista == null) {
            return 0;
        }
        int total = 0;
        for (Penalizacion p : lista) {
            total += p.delta();
        }
        return total;
    }
}
