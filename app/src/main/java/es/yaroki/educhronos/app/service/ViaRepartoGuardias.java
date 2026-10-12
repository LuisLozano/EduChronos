package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.Profesor;
import es.yaroki.educhronos.app.catalog.TramoSemanal;
import es.yaroki.educhronos.app.persistence.Guardia;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.TipoGuardia;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
 *
 * <p>Desde S220 (C-reparto-equilibrado) reparte las DOS familias en el mismo paso: primero las
 * ordinarias y, si llegan, las de recreo sobre la lista de {@link #recreos}. Las filas de recreo
 * resuelven la posición del recreo con esa misma lista, así que la posición quiere decir lo mismo al
 * repartir y al escribir.
 */
final class ViaRepartoGuardias {

    /** La hora de un recreo en los mensajes, como la escribe {@code JornadaService}. */
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    /** Lo que antecede a los fallos del recreo en un ajuste (S220, decisión N). */
    static final String PREFIJO_AJUSTE_RECREO = "No se puede hacer este cambio: las guardias de recreo no se"
            + " pueden repartir con la configuración actual. ";

    /**
     * Las dos familias repartidas (S220): las ordinarias y, solo si no fallan, las de recreo; si las
     * ordinarias fallan, {@code recreo} es null y todo sigue como antes de S220.
     */
    record Reparto(RepartoGuardias.Resultado ordinarias, RepartoGuardias.ResultadoRecreo recreo) { }

    private ViaRepartoGuardias() {
    }

    /**
     * Reparte sobre {@code solucion} las guardias ordinarias con las guardias por profesor y el mínimo
     * del cuadre y, si llegan, las de recreo sobre {@code recreos} con las de recreo por profesor y
     * los dos mínimos por recreo.
     *
     * @param recreos los recreos de la jornada, de {@link #recreos}
     */
    static Reparto repartir(ProblemaHorario problema, SolucionHorario solucion, DatosCuadre cuadre,
                            List<TramoSemanal> recreos) {
        RepartoGuardias.Resultado ordinarias = RepartoGuardias.repartir(problema, solucion,
                cuadre.guardiasPorProfesor(), cuadre.minimoGuardiasPorTramo());
        if (ordinarias.fallo()) {
            return new Reparto(ordinarias, null);
        }
        RepartoGuardias.ResultadoRecreo recreo = RepartoGuardias.repartirRecreo(recreos.size(),
                cuadre.guardiasRecreoPorProfesor(), cuadre.minimoGuardiasPorRecreo(),
                cuadre.minimoBibliotecaPorRecreo());
        return new Reparto(ordinarias, recreo);
    }

    /**
     * Los recreos de la jornada (S220): sus tramos no lectivos, por día y orden, con el comparador de
     * {@code CatalogoMapper.renumerarLectivos}. La posición en esta lista es la del recreo en el reparto.
     */
    static List<TramoSemanal> recreos(Collection<TramoSemanal> tramos) {
        return tramos.stream()
                .filter(tramo -> !tramo.isEsLectivo())
                .sorted(Comparator.comparingInt((TramoSemanal t) -> t.getDia().ordinal())
                        .thenComparingInt(TramoSemanal::getOrden))
                .toList();
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
     * Las filas de {@code guardia} de recreo de {@code horario} (S220): RECREO o BIBLIOTECA, en el
     * recreo de esa posición de {@code recreos}. Un profesor sin entidad o una posición fuera de la
     * lista aborta, como en {@link #filas}.
     */
    static List<Guardia> filasDeRecreo(HorarioGenerado horario, List<TramoSemanal> recreos,
                                       Collection<Profesor> profesores,
                                       List<RepartoGuardias.AsignacionRecreo> guardias) {
        Map<String, Profesor> idxProfesor = profesores.stream()
                .collect(Collectors.toMap(Profesor::getCodigo, Function.identity()));
        List<Guardia> filas = new ArrayList<>(guardias.size());
        for (RepartoGuardias.AsignacionRecreo guardia : guardias) {
            Profesor profesor = idxProfesor.get(guardia.profesorCodigo());
            if (profesor == null || guardia.recreo() >= recreos.size()) {
                throw new IllegalArgumentException("La guardia de recreo de " + guardia.profesorCodigo()
                        + " en el recreo " + guardia.recreo() + " no tiene profesor o recreo persistido");
            }
            filas.add(new Guardia(horario, profesor, recreos.get(guardia.recreo()),
                    guardia.biblioteca() ? TipoGuardia.BIBLIOTECA : TipoGuardia.RECREO));
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

    /**
     * El fallo del recreo al generar (S220, decisión N): T-R1 con los recreos que no llegan al mínimo,
     * o los textos de la pre-validación si el fallo es de configuración (biblioteca por encima del
     * recreo, profesores excedidos).
     */
    static String mensajeRecreoAlGenerar(RepartoGuardias.ResultadoRecreo recreo, List<TramoSemanal> recreos,
                                         DatosCuadre cuadre) {
        if (recreo.deficits().isEmpty()) {
            return textosDeConfiguracion(recreo, cuadre);
        }
        return "No se pueden repartir las guardias de recreo: en estos recreos no se llega al mínimo de "
                + recreo.deficits().get(0).minimo() + " profesores de guardia: "
                + listaDeRecreos(recreo.deficits(), recreos) + ". Baja el mínimo o da más guardias de recreo a"
                + " los profesores.";
    }

    /** El mismo fallo en un ajuste (S220, decisión N): T-R2, o los textos de configuración con su prefijo. */
    static String mensajeRecreoAlAjustar(RepartoGuardias.ResultadoRecreo recreo, List<TramoSemanal> recreos,
                                         DatosCuadre cuadre) {
        if (recreo.deficits().isEmpty()) {
            return PREFIJO_AJUSTE_RECREO + textosDeConfiguracion(recreo, cuadre);
        }
        return PREFIJO_AJUSTE_RECREO + "Recreos que no llegan al mínimo de profesores de guardia: "
                + listaDeRecreos(recreo.deficits(), recreos) + ".";
    }

    /** {@code recreo1:5/7,recreo3:6/7} o {@code configuracion}: el fallo del recreo en una palabra, para el log. */
    static String resumenRecreo(RepartoGuardias.ResultadoRecreo recreo) {
        if (recreo.deficits().isEmpty()) {
            return "configuracion";
        }
        return recreo.deficits().stream()
                .map(d -> "recreo" + (d.recreo() + 1) + ":" + d.alcanzadas() + "/" + d.minimo())
                .collect(Collectors.joining(","));
    }

    /**
     * {@code LUNES 11:00-11:30: 5 de 7; MARTES 11:00-11:30: 6 de 7}: los recreos que no llegan al
     * mínimo, en el orden recibido.
     */
    static String listaDeRecreos(List<RepartoGuardias.DeficitRecreo> deficits, List<TramoSemanal> recreos) {
        return deficits.stream()
                .map(d -> nombreDeRecreo(recreos.get(d.recreo())) + ": " + d.alcanzadas() + " de " + d.minimo())
                .collect(Collectors.joining("; "));
    }

    /** {@code LUNES 11:00-11:30}: un recreo por su día y sus horas. */
    static String nombreDeRecreo(TramoSemanal recreo) {
        return recreo.getDia().name() + " " + HH_MM.format(recreo.getHoraInicio()) + "-"
                + HH_MM.format(recreo.getHoraFin());
    }

    /**
     * Los textos de la pre-validación para un fallo de configuración del recreo, en su orden: el de
     * {@code BIBLIOTECA_SUPERA_RECREO} y uno de {@code GUARDIAS_RECREO_EXCEDEN} por profesor excedido.
     */
    private static String textosDeConfiguracion(RepartoGuardias.ResultadoRecreo recreo, DatosCuadre cuadre) {
        List<String> textos = new ArrayList<>();
        if (recreo.bibliotecaSuperaRecreo()) {
            textos.add(PrevalidacionService.textoBibliotecaSuperaRecreo(
                    cuadre.minimoBibliotecaPorRecreo(), cuadre.minimoGuardiasPorRecreo()));
        }
        for (RepartoGuardias.Excedido excedido : recreo.excedidos()) {
            textos.add(PrevalidacionService.textoGuardiasRecreoExceden(
                    cuadre.nombresProfesor().getOrDefault(excedido.profesorCodigo(), excedido.profesorCodigo()),
                    excedido.guardias(), excedido.recreos()));
        }
        return String.join(" ", textos);
    }
}
