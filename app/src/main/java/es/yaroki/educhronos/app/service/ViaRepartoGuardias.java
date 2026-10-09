package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.Profesor;
import es.yaroki.educhronos.app.catalog.TramoSemanal;
import es.yaroki.educhronos.app.persistence.Guardia;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lo común del reparto de las guardias entre la generación y el ajuste a mano (S214,
 * C-ajuste-guardias, E3): la entrada desde el cuadre, la llamada a {@link RepartoGuardias}, las filas
 * de {@code guardia} que salen del resultado y la lista de tramos sin mínimo de los mensajes.
 *
 * <p>Código sin estado y no un bean, como {@link ConfiguracionGuardias}: un colaborador nuevo en
 * {@code GeneradorHorarioService} o en {@code MovimientoInstanciaService} obligaría a tocar el
 * {@code @Import} de cada slice de test que los monta. Quien llama pone las cargas y la transacción.
 */
final class ViaRepartoGuardias {

    private ViaRepartoGuardias() {
    }

    /** Reparte sobre {@code solucion} con las guardias por profesor y el mínimo del cuadre. */
    static RepartoGuardias.Resultado repartir(ProblemaHorario problema, SolucionHorario solucion,
                                              DatosCuadre cuadre) {
        return RepartoGuardias.repartir(problema, solucion, cuadre.guardiasPorProfesor(),
                cuadre.minimoGuardiasPorTramo());
    }

    /**
     * Las filas de {@code guardia} de {@code horario}: el tramo, por su posición en el problema y
     * {@code idxTramo}; el profesor, por código. Un profesor o un tramo sin entidad aborta, como las
     * sesiones sin plaza o sin tramo.
     */
    static List<Guardia> filas(HorarioGenerado horario, ProblemaHorario problema,
                               Map<Tramo, TramoSemanal> idxTramo, Collection<Profesor> profesores,
                               List<RepartoGuardias.Asignacion> guardias) {
        Map<String, Profesor> idxProfesor = profesores.stream()
                .collect(Collectors.toMap(Profesor::getCodigo, Function.identity()));
        List<Guardia> filas = new ArrayList<>(guardias.size());
        for (RepartoGuardias.Asignacion guardia : guardias) {
            Profesor profesor = idxProfesor.get(guardia.profesorCodigo());
            TramoSemanal tramo = idxTramo.get(problema.tramos().get(guardia.tramo()));
            if (profesor == null || tramo == null) {
                throw new IllegalArgumentException("La guardia de " + guardia.profesorCodigo()
                        + " en el tramo " + problema.tramos().get(guardia.tramo()).codigo()
                        + " no tiene profesor o tramo persistido");
            }
            filas.add(new Guardia(horario, profesor, tramo));
        }
        return filas;
    }

    /**
     * {@code tramo L2 (día 1, tramo 2): 1 de 2; tramo L4 (día 1, tramo 4): 0 de 2}: los tramos que no
     * llegan al mínimo, con {@link PrevalidacionService#nombreDeTramo}, en el orden recibido.
     */
    static String listaDeTramos(List<RepartoGuardias.Deficit> deficits) {
        return deficits.stream()
                .map(d -> PrevalidacionService.nombreDeTramo(d.tramo()) + ": " + d.alcanzadas() + " de "
                        + d.minimo())
                .collect(Collectors.joining("; "));
    }
}
