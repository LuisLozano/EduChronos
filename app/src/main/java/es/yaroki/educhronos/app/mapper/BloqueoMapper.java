package es.yaroki.educhronos.app.mapper;

import es.yaroki.educhronos.app.catalog.AulaBloqueada;
import es.yaroki.educhronos.app.catalog.SesionBloqueada;
import es.yaroki.educhronos.app.catalog.TramoSemanal;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Convierte los bloqueos manuales del catálogo JPA ({@link SesionBloqueada} pin de
 * tramo + {@link AulaBloqueada} pin de aula por plaza) en la lista de
 * {@code solver.domain.SesionBloqueada} que consume el solver (§4.7, Bloque
 * 8.2b-ii). Espejo de estilo de {@code SolucionMapper}: clase final, ctor privado,
 * métodos estáticos. El record homónimo del dominio se referencia SIEMPRE por su
 * FQN, igual que {@code CatalogoMapper} distingue {@code domain.Plaza} de
 * {@code catalog.Plaza}.
 *
 * <p>El dominio agrega ambas entidades: una {@code domain.SesionBloqueada} por
 * instancia pinada (par actividad, {@code indice}), con su {@code Tramo} y un
 * {@code Map<Plaza, Aula>} construido a partir de las {@link AulaBloqueada} de esa
 * misma instancia. Un pin de aula SIN su pin de tramo es entrada incoherente
 * (el record de dominio no soporta el pin de aula suelto) y aborta.
 *
 * <p><b>Precondición de IDENTIDAD.</b> El puente de tramo {@code tramosPorEntidad}
 * es el {@link java.util.IdentityHashMap} que produce {@code CatalogoMapper} (se
 * REUTILIZA, no se duplica: deuda D30 no se agrava): la
 * {@link SesionBloqueada#getTramoInicio()} debe ser la MISMA instancia
 * {@code TramoSemanal} que la lista pasada a {@code aProblemaHorario}. Se cumple
 * porque toda la carga y el mapeo ocurren en una única transacción JPA (misma
 * frontera transaccional que el resto del {@code CatalogoMapper}).
 *
 * <p>Validaciones de entrada replicadas aquí con {@link IllegalArgumentException}
 * (no se importa {@code ProblemaInvalidoException} de {@code solver.io}: cruzaría la
 * frontera de capas; {@code CatalogoMapper} tampoco la importa).
 */
public final class BloqueoMapper {

    private BloqueoMapper() { }

    /**
     * Ensambla la lista de {@code domain.SesionBloqueada} a partir de las dos
     * tablas de bloqueo del catálogo y los índices por código ya construidos por
     * {@code CatalogoMapper}.
     *
     * @param pinesTramo           pines de tramo por instancia (tabla {@code sesion_bloqueada})
     * @param pinesAula            pines de aula por plaza (tabla {@code aula_bloqueada})
     * @param actividadesPorCodigo índice de actividades de dominio ya mapeadas
     * @param plazasPorCodigo      índice de plazas de dominio ya mapeadas
     * @param aulasPorCodigo       índice de aulas de dominio ya mapeadas
     * @param tramosPorEntidad     puente {@code TramoSemanal → Tramo} por IDENTIDAD
     * @return una {@code domain.SesionBloqueada} por pin de tramo, con sus pines de aula agregados
     */
    public static List<es.yaroki.educhronos.solver.domain.SesionBloqueada> aBloqueos(
            List<SesionBloqueada> pinesTramo,
            List<AulaBloqueada> pinesAula,
            Map<String, es.yaroki.educhronos.solver.domain.Actividad> actividadesPorCodigo,
            Map<String, es.yaroki.educhronos.solver.domain.Plaza> plazasPorCodigo,
            Map<String, es.yaroki.educhronos.solver.domain.Aula> aulasPorCodigo,
            Map<TramoSemanal, Tramo> tramosPorEntidad) {

        Objects.requireNonNull(pinesTramo,           "pinesTramo no puede ser null");
        Objects.requireNonNull(pinesAula,            "pinesAula no puede ser null");
        Objects.requireNonNull(actividadesPorCodigo, "actividadesPorCodigo no puede ser null");
        Objects.requireNonNull(plazasPorCodigo,      "plazasPorCodigo no puede ser null");
        Objects.requireNonNull(aulasPorCodigo,       "aulasPorCodigo no puede ser null");
        Objects.requireNonNull(tramosPorEntidad,     "tramosPorEntidad no puede ser null");

        // Paso 1: agrupar los pines de aula por instancia (actividad_codigo, indice).
        Map<Clave, Map<es.yaroki.educhronos.solver.domain.Plaza,
                es.yaroki.educhronos.solver.domain.Aula>> aulasPorInstancia = new HashMap<>();
        for (AulaBloqueada pin : pinesAula) {
            String codigoActividad = pin.getActividad().getCodigo();
            Clave clave = new Clave(codigoActividad, pin.getIndice());

            String codigoPlaza = pin.getPlaza().getCodigo();
            es.yaroki.educhronos.solver.domain.Plaza plaza = plazasPorCodigo.get(codigoPlaza);
            if (plaza == null) {
                throw new IllegalArgumentException(
                        "El pin de aula de la instancia " + codigoActividad + "#" + pin.getIndice()
                                + " referencia una plaza no presente en el catálogo mapeado: "
                                + codigoPlaza);
            }

            String codigoAula = pin.getAula().getCodigo();
            es.yaroki.educhronos.solver.domain.Aula aula = aulasPorCodigo.get(codigoAula);
            if (aula == null) {
                throw new IllegalArgumentException(
                        "El pin de aula de la instancia " + codigoActividad + "#" + pin.getIndice()
                                + " referencia un aula no presente en el catálogo mapeado: "
                                + codigoAula);
            }

            if (plaza.aulaFija().isPresent()) {
                throw new IllegalArgumentException(
                        "pin de aula sobre plaza de aula fija: " + codigoPlaza);
            }
            if (!plaza.aulasCandidatas().contains(aula)) {
                throw new IllegalArgumentException(
                        "aula pinada " + codigoAula + " no es candidata de la plaza " + codigoPlaza);
            }

            Map<es.yaroki.educhronos.solver.domain.Plaza,
                    es.yaroki.educhronos.solver.domain.Aula> submapa =
                    aulasPorInstancia.computeIfAbsent(clave, k -> new HashMap<>());
            submapa.put(plaza, aula);
        }

        // Paso 2: un domain.SesionBloqueada por pin de tramo, agregando sus pines de aula.
        List<es.yaroki.educhronos.solver.domain.SesionBloqueada> bloqueos =
                new ArrayList<>(pinesTramo.size());
        for (SesionBloqueada pin : pinesTramo) {
            String codigoActividad = pin.getActividad().getCodigo();
            es.yaroki.educhronos.solver.domain.Actividad actividad =
                    actividadesPorCodigo.get(codigoActividad);
            if (actividad == null) {
                throw new IllegalArgumentException(
                        "El pin de tramo referencia una actividad no presente en el catálogo"
                                + " mapeado: " + codigoActividad);
            }

            ActividadInstancia instancia = new ActividadInstancia(actividad, pin.getIndice());

            Tramo tramo = tramosPorEntidad.get(pin.getTramoInicio());
            if (tramo == null) {
                throw new IllegalArgumentException(
                        "El pin de tramo de la instancia " + codigoActividad + "#" + pin.getIndice()
                                + " referencia un tramo que no está en el catálogo lectivo"
                                + " mapeado (¿un tramo de recreo, excluido por aTramos?)");
            }

            Clave clave = new Clave(codigoActividad, pin.getIndice());
            Map<es.yaroki.educhronos.solver.domain.Plaza,
                    es.yaroki.educhronos.solver.domain.Aula> aulas =
                    aulasPorInstancia.remove(clave);
            if (aulas == null) {
                aulas = Map.of();
            }

            bloqueos.add(new es.yaroki.educhronos.solver.domain.SesionBloqueada(
                    instancia, tramo, aulas));
        }

        // Todo pin de aula debe haber sido consumido por un pin de tramo de su instancia.
        if (!aulasPorInstancia.isEmpty()) {
            Clave huerfana = aulasPorInstancia.keySet().iterator().next();
            throw new IllegalArgumentException(
                    "pin de aula sin pin de tramo para " + huerfana.actividad() + "#" + huerfana.indice());
        }

        return bloqueos;
    }

    /** Clave lógica de una instancia pinada: (código de actividad, índice de repetición). */
    private record Clave(String actividad, int indice) { }
}
