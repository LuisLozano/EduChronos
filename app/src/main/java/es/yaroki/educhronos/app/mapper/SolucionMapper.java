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

    /** Empaqueta (diaSemana 1..5, ordenEnDia 1..6) en una clave estable. */
    private static long clave(int diaSemana, int ordenEnDia) {
        return (long) diaSemana * 100 + ordenEnDia;
    }
}
