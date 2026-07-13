package es.yaroki.educhronos.app.mapper;

import es.yaroki.educhronos.app.catalog.Aula;
import es.yaroki.educhronos.app.catalog.Dia;
import es.yaroki.educhronos.app.catalog.Plaza;
import es.yaroki.educhronos.app.catalog.TramoSemanal;
import es.yaroki.educhronos.app.persistence.HorarioGenerado;
import es.yaroki.educhronos.app.persistence.Sesion;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Convierte la salida del solver ({@code domain.SolucionHorario} sobre un
 * {@code domain.ProblemaHorario}) en las entidades JPA {@link Sesion} de un
 * {@link HorarioGenerado} (Fase 6, Bloque 9). Espejo de estilo de
 * {@code CatalogoMapper}: clase final, ctor privado, métodos estáticos.
 *
 * <p>Emite una {@code Sesion} por PLAZA colocada: recorre las actividades del
 * problema × sus repeticiones (índices) × sus plazas. El aula se toma SIEMPRE por
 * {@code SolucionHorario.aulaElegida(instancia, plaza)} (único punto de verdad
 * fija/variable); un {@code empty} ahí ABORTA (nunca se escribe una Sesion con FK
 * de aula null). Cualquier código o tramo sin correspondencia en los índices es un
 * error de integridad y aborta ruidosamente con el dato en el mensaje.
 */
public final class SolucionMapper {

    private SolucionMapper() { }

    /**
     * Puente P1 (deuda consciente, hermana de D21/D22): reconstruye la
     * correspondencia {@code domain.Tramo → TramoSemanal} REPLICANDO la
     * renumeración de {@code CatalogoMapper.aTramos} —filtrar {@code esLectivo},
     * ordenar por {@code orden} global, renumerar {@code ordenEnDia} 1..6 por
     * día— y emparejando cada {@code Tramo} de dominio por {@code (diaSemana,
     * ordenEnDia)}. El {@code domain.Tramo} porta un código sintético que la
     * entidad {@code TramoSemanal} no tiene, por eso no se puede cruzar por
     * código. Esta réplica DEBE permanecer idéntica a {@code aTramos} o divergirá
     * en silencio; se cierra cuando la jornada pase a configuración (Fase 8).
     */
    public static Map<Tramo, TramoSemanal> indiceTramos(
            ProblemaHorario problema, List<TramoSemanal> tramosJpa) {
        // Comparte la renumeración con CatalogoMapper.indiceOrdenEnDia; su unificación es deuda D30.
        Objects.requireNonNull(problema, "problema no puede ser null");
        Objects.requireNonNull(tramosJpa, "tramosJpa no puede ser null");

        List<TramoSemanal> lectivos = tramosJpa.stream()
                .filter(TramoSemanal::isEsLectivo)
                .sorted(Comparator.comparingInt((TramoSemanal t) -> t.getDia().ordinal())
                        .thenComparingInt(TramoSemanal::getOrden))
                .toList();

        Map<Dia, Integer> ordenPorDia = new EnumMap<>(Dia.class);
        Map<Long, TramoSemanal> porClave = new HashMap<>();
        for (TramoSemanal t : lectivos) {
            int ordenEnDia = ordenPorDia.merge(t.getDia(), 1, Integer::sum);
            int diaSemana = t.getDia().ordinal() + 1;
            porClave.put(clave(diaSemana, ordenEnDia), t);
        }

        Map<Tramo, TramoSemanal> indice = new HashMap<>();
        for (Tramo tramo : problema.tramos()) {
            TramoSemanal entidad = porClave.get(clave(tramo.diaSemana(), tramo.ordenEnDia()));
            if (entidad == null) {
                throw new IllegalArgumentException(
                        "No hay TramoSemanal lectivo para el tramo de dominio " + tramo.codigo()
                                + " (diaSemana=" + tramo.diaSemana()
                                + ", ordenEnDia=" + tramo.ordenEnDia() + ")");
            }
            indice.put(tramo, entidad);
        }
        return indice;
    }

    /**
     * Materializa las {@link Sesion} de un {@link HorarioGenerado} a partir de la
     * solución. Una fila por plaza colocada. Aborta si una instancia no está
     * colocada (sin tramo), si una plaza/aula/tramo no tiene correspondencia en su
     * índice, o si una plaza no tiene aula asignada.
     *
     * @param horario   cabecera dueña de las sesiones (ya persistida o a persistir)
     * @param problema  problema de dominio (fuente de actividades × índices × plazas)
     * @param solucion  salida del solver (asignación instancia→tramo y plaza→aula)
     * @param idxPlaza  {@code codigo → Plaza} JPA
     * @param idxAula   {@code codigo → Aula} JPA
     * @param idxTramo  {@code domain.Tramo → TramoSemanal} (ver {@link #indiceTramos})
     */
    public static List<Sesion> aSesiones(
            HorarioGenerado horario,
            ProblemaHorario problema,
            SolucionHorario solucion,
            Map<String, Plaza> idxPlaza,
            Map<String, Aula> idxAula,
            Map<Tramo, TramoSemanal> idxTramo) {

        Objects.requireNonNull(horario, "horario no puede ser null");
        Objects.requireNonNull(problema, "problema no puede ser null");
        Objects.requireNonNull(solucion, "solucion no puede ser null");
        Objects.requireNonNull(idxPlaza, "idxPlaza no puede ser null");
        Objects.requireNonNull(idxAula, "idxAula no puede ser null");
        Objects.requireNonNull(idxTramo, "idxTramo no puede ser null");

        List<Sesion> sesiones = new ArrayList<>();
        for (es.yaroki.educhronos.solver.domain.Actividad actividad : problema.actividades()) {
            for (int indice = 1; indice <= actividad.repeticionesPorSemana(); indice++) {
                ActividadInstancia instancia = new ActividadInstancia(actividad, indice);

                Optional<Tramo> tramoOpt = solucion.tramoDeInstancia(instancia);
                if (tramoOpt.isEmpty()) {
                    throw new IllegalArgumentException("La instancia " + actividad.codigo()
                            + "#" + indice + " no está colocada en ningún tramo en la solución");
                }
                TramoSemanal tramoJpa = idxTramo.get(tramoOpt.get());
                if (tramoJpa == null) {
                    throw new IllegalArgumentException("El tramo " + tramoOpt.get().codigo()
                            + " de la instancia " + actividad.codigo() + "#" + indice
                            + " no tiene TramoSemanal correspondiente");
                }

                for (es.yaroki.educhronos.solver.domain.Plaza plaza : actividad.plazas()) {
                    Plaza plazaJpa = idxPlaza.get(plaza.codigo());
                    if (plazaJpa == null) {
                        throw new IllegalArgumentException("La plaza " + plaza.codigo()
                                + " no está en el catálogo de plazas persistido");
                    }

                    Optional<es.yaroki.educhronos.solver.domain.Aula> aulaOpt =
                            solucion.aulaElegida(instancia, plaza);
                    if (aulaOpt.isEmpty()) {
                        throw new IllegalArgumentException("La plaza " + plaza.codigo()
                                + " de la instancia " + actividad.codigo() + "#" + indice
                                + " no tiene aula asignada (aulaElegida vacío)");
                    }
                    Aula aulaJpa = idxAula.get(aulaOpt.get().codigo());
                    if (aulaJpa == null) {
                        throw new IllegalArgumentException("El aula " + aulaOpt.get().codigo()
                                + " no está en el catálogo de aulas persistido");
                    }

                    sesiones.add(new Sesion(horario, plazaJpa, indice, tramoJpa, aulaJpa));
                }
            }
        }
        return sesiones;
    }

    /**
     * INVERSO de {@link #aSesiones}: reconstruye el {@link SolucionHorario} de dominio
     * a partir de las {@link Sesion} persistidas de un horario (Fase 8, Bloque 8.3-C).
     * Lo consume el diagnóstico: {@code verificar}/{@code atribuirBlandas} necesitan una
     * {@code SolucionHorario}, y la BD solo guarda filas {@code Sesion}.
     *
     * <p>La correspondencia de tramos NO se recalcula: se OBTIENE de {@link #indiceTramos}
     * (el mismo mapa {@code Tramo → TramoSemanal} que usó la ida) y se INVIERTE recorriendo
     * su {@code entrySet()}. Así NO se añade un espejo más de la renumeración de jornada
     * (deuda D30). Si dos {@code Tramo} distintos mapearan al mismo {@code TramoSemanal}
     * (no debería), aborta.
     *
     * <p>La {@code ActividadInstancia} de cada fila es {@code (plaza.actividad, indice)}:
     * la actividad de dominio se cruza por CÓDIGO contra {@code problema.actividades()}
     * (la instancia lógica no está materializada, D-B5-1). Todas las {@code Sesion} de una
     * misma instancia deben dar el mismo {@code Tramo}; si no, aborta.
     *
     * <p><b>{@code aulasElegidas} omite las plazas con {@code aulaFija}</b> (D-F8.3-C-3,
     * FIDELIDAD no equivalencia): {@link SolucionHorario#aulaElegida} cae a
     * {@code plaza.aulaFija()} cuando no hay entrada, así que meter las fijas daría una
     * solución EQUIVALENTE pero NO IDÉNTICA y mentiría a quien inspeccione
     * {@code aulasElegidas} esperando "solo lo que el solver eligió". Para una plaza fija,
     * si el aula de la fila no coincide con su {@code aulaFija} se ABORTA (corrupción de
     * datos, no un caso a tolerar). Para una plaza de aulas candidatas SÍ se añade la
     * entrada {@code instancia → (plaza → aula de la fila)}, con el aula tomada del conjunto
     * de candidatas del problema.
     *
     * @param problema  problema de dominio (fuente de actividades × plazas × aulas)
     * @param sesiones  filas persistidas del horario a reconstruir
     * @param idxTramo  {@code domain.Tramo → TramoSemanal} de {@link #indiceTramos}
     * @throws IllegalArgumentException si el índice de tramos no es invertible, si una
     *         actividad/plaza/aula de una fila no está en el problema, si un
     *         {@code TramoSemanal} no tiene {@code Tramo} inverso, si una instancia cae en
     *         tramos distintos, o si el aula de una plaza fija no coincide con su aulaFija.
     */
    public static SolucionHorario aSolucionHorario(
            ProblemaHorario problema,
            List<Sesion> sesiones,
            Map<Tramo, TramoSemanal> idxTramo) {

        Objects.requireNonNull(problema, "problema no puede ser null");
        Objects.requireNonNull(sesiones, "sesiones no puede ser null");
        Objects.requireNonNull(idxTramo, "idxTramo no puede ser null");

        // Inversión de idxTramo (TramoSemanal -> Tramo). NO se recalcula la renumeración.
        Map<TramoSemanal, Tramo> tramoInverso = new HashMap<>();
        for (Map.Entry<Tramo, TramoSemanal> e : idxTramo.entrySet()) {
            Tramo previo = tramoInverso.put(e.getValue(), e.getKey());
            if (previo != null) {
                throw new IllegalArgumentException("idxTramo no es invertible: los tramos de "
                        + "dominio " + previo.codigo() + " y " + e.getKey().codigo()
                        + " mapean al mismo TramoSemanal id=" + e.getValue().getId());
            }
        }

        // Índice de actividades de dominio por código, para cruzar cada Sesion.
        Map<String, es.yaroki.educhronos.solver.domain.Actividad> actPorCodigo = new HashMap<>();
        for (es.yaroki.educhronos.solver.domain.Actividad a : problema.actividades()) {
            actPorCodigo.put(a.codigo(), a);
        }

        Map<ActividadInstancia, Tramo> asignaciones = new HashMap<>();
        Map<ActividadInstancia, Map<es.yaroki.educhronos.solver.domain.Plaza,
                es.yaroki.educhronos.solver.domain.Aula>> aulasElegidas = new HashMap<>();

        for (Sesion sesion : sesiones) {
            String actCodigo = sesion.getPlaza().getActividad().getCodigo();
            es.yaroki.educhronos.solver.domain.Actividad actividad = actPorCodigo.get(actCodigo);
            if (actividad == null) {
                throw new IllegalArgumentException("La actividad " + actCodigo
                        + " de una sesion no está en el problema");
            }
            ActividadInstancia instancia = new ActividadInstancia(actividad, sesion.getIndice());

            TramoSemanal tramoFila = sesion.getTramoInicio();
            Tramo tramo = tramoInverso.get(tramoFila);
            if (tramo == null) {
                throw new IllegalArgumentException("El TramoSemanal id=" + tramoFila.getId()
                        + " de la sesion de la instancia " + actCodigo + "#" + sesion.getIndice()
                        + " no tiene Tramo de dominio inverso en idxTramo");
            }
            Tramo tramoPrevio = asignaciones.put(instancia, tramo);
            if (tramoPrevio != null && !tramoPrevio.equals(tramo)) {
                throw new IllegalArgumentException("La instancia " + actCodigo + "#"
                        + sesion.getIndice() + " aparece en tramos distintos: "
                        + tramoPrevio.codigo() + " y " + tramo.codigo());
            }

            String plazaCodigo = sesion.getPlaza().getCodigo();
            es.yaroki.educhronos.solver.domain.Plaza plazaDominio = null;
            for (es.yaroki.educhronos.solver.domain.Plaza p : actividad.plazas()) {
                if (p.codigo().equals(plazaCodigo)) {
                    plazaDominio = p;
                    break;
                }
            }
            if (plazaDominio == null) {
                throw new IllegalArgumentException("La plaza " + plazaCodigo
                        + " de una sesion no está en la actividad " + actCodigo + " del problema");
            }

            String aulaCodigoFila = sesion.getAula().getCodigo();
            if (plazaDominio.aulaFija().isPresent()) {
                // Fidelidad (D-F8.3-C-3): plaza fija -> NO se añade entrada; se GUARDA la
                // coherencia entre el aula de la fila y la aulaFija de la plaza.
                String aulaFijaCodigo = plazaDominio.aulaFija().get().codigo();
                if (!aulaFijaCodigo.equals(aulaCodigoFila)) {
                    throw new IllegalArgumentException("La sesion de la plaza " + plazaCodigo
                            + " tiene aula '" + aulaCodigoFila + "' que no coincide con su aulaFija '"
                            + aulaFijaCodigo + "' (corrupción de datos)");
                }
            } else {
                // Aula variable: el aula elegida DEBE estar entre las candidatas del problema.
                es.yaroki.educhronos.solver.domain.Aula aulaDominio = null;
                for (es.yaroki.educhronos.solver.domain.Aula candidata : plazaDominio.aulasCandidatas()) {
                    if (candidata.codigo().equals(aulaCodigoFila)) {
                        aulaDominio = candidata;
                        break;
                    }
                }
                if (aulaDominio == null) {
                    throw new IllegalArgumentException("El aula '" + aulaCodigoFila
                            + "' de la plaza " + plazaCodigo
                            + " no está entre sus aulasCandidatas en el problema");
                }
                aulasElegidas
                        .computeIfAbsent(instancia, k -> new HashMap<>())
                        .put(plazaDominio, aulaDominio);
            }
        }

        return new SolucionHorario(asignaciones, aulasElegidas);
    }

    /** Empaqueta (diaSemana 1..5, ordenEnDia 1..6) en una clave estable. */
    private static long clave(int diaSemana, int ordenEnDia) {
        return (long) diaSemana * 100 + ordenEnDia;
    }
}
