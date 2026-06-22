package es.yaroki.educhronos.solver.cpsat;

import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.ActividadInstancia;
import es.yaroki.educhronos.solver.domain.Aula;
import es.yaroki.educhronos.solver.domain.GrupoAdministrativo;
import es.yaroki.educhronos.solver.domain.PatronTemporal;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.RestriccionHoraria;
import es.yaroki.educhronos.solver.domain.SolucionHorario;
import es.yaroki.educhronos.solver.domain.Subgrupo;
import es.yaroki.educhronos.solver.domain.TipoRestriccion;
import es.yaroki.educhronos.solver.domain.Tramo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Verifica, de forma <b>independiente del solver</b>, que una
 * {@link SolucionHorario} cumple las restricciones duras del modelo.
 *
 * <p>No depende de OR-Tools: recorre la solución y comprueba propiedades sobre
 * el dominio. Esa independencia es deliberada — si la verificación reutilizara
 * el modelo CP-SAT, un fallo en el modelo pasaría desapercibido. La usan el
 * test del Bloque 4 y la salida a consola del Bloque 5.
 *
 * <p>Comprobaciones: todas las instancias colocadas; no-solape de profesor,
 * aula, subgrupo y grupo (S9, espejo de la quinta restricción dura del solver;
 * agrupa por subgrupo.grupo() directo, ciega al grupoPadre); distribución por
 * día de las actividades {@code DISTRIBUIDA} (con la misma guarda anti-palomar
 * D12 que el modelo).
 */
public final class VerificadorSolucion {

    public ResultadoVerificacion verificar(ProblemaHorario problema, SolucionHorario solucion) {
        List<String> violaciones = new ArrayList<>();
        List<ActividadInstancia> esperadas = Expansion.todas(problema);

        verificarTodasColocadas(esperadas, solucion, violaciones);
        verificarNoSolapes(esperadas, solucion, violaciones);
        verificarDistribucion(problema, esperadas, solucion, violaciones);

        return new ResultadoVerificacion(violaciones);
    }

    /**
     * Cuenta las "ventanas" (huecos) de cada profesor sobre una solución dada,
     * de forma <b>independiente del solver</b> (recorre el dominio, no usa
     * OR-Tools). Es el recomputo gemelo del término que {@code ModeloCpSat}
     * minimiza: si el modelo CP-SAT tuviera un error en su identidad
     * span−ocupados, este conteo —que cuenta de otra manera— lo delataría.
     *
     * <p>Definición de ventana: para un profesor en un día, un tramo libre
     * situado <i>entre</i> su primera y su última clase de ese día. Los tramos
     * antes de la primera clase o después de la última no son ventanas (entrar
     * tarde o salir pronto no penaliza). El recreo no genera ventanas: no es un
     * {@link Tramo}, así que los tramos lectivos de un día son contiguos en
     * {@code ordenEnDia} y la clase de antes y la de después del recreo quedan
     * adyacentes.
     *
     * <p>Identidad por (profesor, día) con ≥1 clase:
     * {@code ventanas = (ultimaPos − primeraPos + 1) − nClases}. Con 0 ó 1
     * clase ese día, 0 ventanas.
     *
     * @return mapa profesor → nº total de ventanas en la semana (suma de todos
     *         los días). Un profesor sin ventanas no aparece o aparece con 0;
     *         se incluyen todos los profesores del problema con valor ≥ 0.
     */
    public Map<Profesor, Integer> contarVentanasProfesor(ProblemaHorario problema,
                                                         SolucionHorario solucion) {
        // (profesor, diaSemana) -> conjunto de ordenEnDia ocupados por ese profesor.
        Map<Profesor, Map<Integer, Set<Integer>>> ocupacion = new HashMap<>();
        for (Profesor p : problema.profesores()) {
            ocupacion.put(p, new HashMap<>());
        }

        for (ActividadInstancia inst : Expansion.todas(problema)) {
            Optional<Tramo> tramoOpt = solucion.tramoDeInstancia(inst);
            if (tramoOpt.isEmpty()) {
                continue; // instancia sin colocar: ya lo reporta verificar()
            }
            Tramo tramo = tramoOpt.get();
            // Profesores de la instancia: unión de los de todas sus plazas, una
            // sola vez por instancia (un profesor que aparece en dos plazas de la
            // misma actividad ocupa el tramo una vez). Mismo criterio que el
            // no-solape de profesor del modelo.
            Set<Profesor> profesoresInstancia = new HashSet<>();
            for (Plaza plaza : inst.actividad().plazas()) {
                profesoresInstancia.addAll(plaza.profesores());
            }
            for (Profesor p : profesoresInstancia) {
                ocupacion.get(p)
                        .computeIfAbsent(tramo.diaSemana(), k -> new HashSet<>())
                        .add(tramo.ordenEnDia());
            }
        }

        Map<Profesor, Integer> ventanas = new HashMap<>();
        for (Map.Entry<Profesor, Map<Integer, Set<Integer>>> ep : ocupacion.entrySet()) {
            int total = 0;
            for (Set<Integer> posicionesDelDia : ep.getValue().values()) {
                if (posicionesDelDia.size() <= 1) {
                    continue; // 0 ó 1 clase: sin ventanas
                }
                int min = posicionesDelDia.stream().min(Integer::compareTo).get();
                int max = posicionesDelDia.stream().max(Integer::compareTo).get();
                int span = max - min + 1;
                total += span - posicionesDelDia.size();
            }
            ventanas.put(ep.getKey(), total);
        }
        return ventanas;
    }

    /**
     * Cuenta la penalización por indisponibilidades BLANDA incumplidas sobre una
     * solución dada, de forma <b>independiente del solver</b>. Recomputo gemelo
     * del término que {@code ModeloCpSat.objetivoIndisponibilidadBlandaProfesor}
     * minimiza: si el modelo CP-SAT contara mal las penalizaciones blandas, este
     * conteo —que recorre el dominio, sin OR-Tools— lo delataría.
     *
     * <p>Una restricción BLANDA (profesor P, tramo T) se incumple por cada
     * instancia que usa a P y cae en T. El conteo es a nivel de instancia (igual
     * que el modelo): un profesor que aparece en varias plazas de la misma
     * actividad cuenta una vez, porque la actividad ocupa el tramo una vez.
     *
     * @return número total de incumplimientos blandos en la solución (suma sobre
     *         todas las restricciones BLANDA y todas las instancias). Es el conteo
     *         SIN ponderar por peso: con {@code PESO_INDISP_BLANDA = 1} coincide
     *         con el valor del término; si el peso dejara de ser 1, el test debe
     *         multiplicar por el peso conocido.
     */
    public int contarPenalizacionIndisponibilidadBlanda(ProblemaHorario problema,
                                                        SolucionHorario solucion) {
        List<ActividadInstancia> todas = Expansion.todas(problema);
        int total = 0;
        for (RestriccionHoraria r : problema.restriccionesHorarias()) {
            if (r.tipo() != TipoRestriccion.BLANDA) {
                continue;
            }
            for (ActividadInstancia inst : todas) {
                Optional<Tramo> tramoOpt = solucion.tramoDeInstancia(inst);
                if (tramoOpt.isEmpty()) {
                    continue; // instancia sin colocar: ya lo reporta verificar()
                }
                if (!tramoOpt.get().equals(r.tramo())) {
                    continue;
                }
                // ¿usa esta instancia al profesor de la restricción? Unión de los
                // profesores de todas sus plazas, una vez por instancia.
                boolean usaProfesor = false;
                for (Plaza plaza : inst.actividad().plazas()) {
                    if (plaza.profesores().contains(r.profesor())) {
                        usaProfesor = true;
                        break;
                    }
                }
                if (usaProfesor) {
                    total++;
                }
            }
        }
        return total;
    }

    /**
     * Cuenta la penalización por exceso de sesiones CONSECUTIVAS de cada profesor
     * sobre una solución dada, de forma <b>independiente del solver</b> (recorre el
     * dominio, no usa OR-Tools). Recomputo gemelo del término que
     * {@code ModeloCpSat.objetivoConsecutivasProfesor} minimiza: si el modelo
     * CP-SAT contara mal el exceso, este conteo —que cuenta de otra manera, por
     * rachas maximales en vez de por ventanas deslizantes— lo delataría.
     *
     * <p>Definición: para un profesor en un día, se ordenan las posiciones
     * ({@code ordenEnDia}) ocupadas y se parten en rachas maximales de posiciones
     * contiguas. Cada racha de longitud L aporta {@code max(0, L − N)} con
     * {@code N = MAX_CONSECUTIVAS}. El recreo no rompe racha (no es un {@link Tramo};
     * los tramos lectivos de un día son contiguos en {@code ordenEnDia}, igual
     * criterio que {@link #contarVentanasProfesor}).
     *
     * <p><b>Duplicación consciente de N:</b> {@code N = 3} aquí es espejo de
     * {@code ModeloCpSat.MAX_CONSECUTIVAS}, que es privado. El verificador es
     * independiente del modelo CP-SAT por diseño (no puede importar su constante);
     * la duplicación es frágil (cambiar el modelo y olvidar esto haría mentir al
     * gemelo) y se resuelve cuando N pase a configuración (deuda D21), momento en
     * que ambos leerán el mismo origen.
     *
     * @return número total de unidades de exceso de consecutivas en la solución
     *         (suma sobre todos los profesores y días). Conteo SIN ponderar: con
     *         {@code PESO_CONSECUTIVAS = 1} coincide con el valor del término; si el
     *         peso dejara de ser 1, el test debe multiplicar por el peso conocido.
     */
    public int contarPenalizacionConsecutivasProfesor(ProblemaHorario problema,
                                                      SolucionHorario solucion) {
        final int n = 3; // MAX_CONSECUTIVAS (espejo de la constante privada del modelo)

        // (profesor, diaSemana) -> conjunto de ordenEnDia ocupados por ese profesor.
        Map<Profesor, Map<Integer, Set<Integer>>> ocupacion = new HashMap<>();
        for (Profesor p : problema.profesores()) {
            ocupacion.put(p, new HashMap<>());
        }

        for (ActividadInstancia inst : Expansion.todas(problema)) {
            Optional<Tramo> tramoOpt = solucion.tramoDeInstancia(inst);
            if (tramoOpt.isEmpty()) {
                continue; // instancia sin colocar: ya lo reporta verificar()
            }
            Tramo tramo = tramoOpt.get();
            Set<Profesor> profesoresInstancia = new HashSet<>();
            for (Plaza plaza : inst.actividad().plazas()) {
                profesoresInstancia.addAll(plaza.profesores());
            }
            for (Profesor p : profesoresInstancia) {
                ocupacion.get(p)
                        .computeIfAbsent(tramo.diaSemana(), k -> new HashSet<>())
                        .add(tramo.ordenEnDia());
            }
        }

        int total = 0;
        for (Map<Integer, Set<Integer>> porDia : ocupacion.values()) {
            for (Set<Integer> posiciones : porDia.values()) {
                if (posiciones.size() <= n) {
                    continue; // con ≤N clases ese día no puede haber racha de N+1
                }
                List<Integer> ordenadas = new ArrayList<>(posiciones);
                ordenadas.sort(Integer::compareTo);
                int longitudRacha = 1;
                for (int i = 1; i < ordenadas.size(); i++) {
                    if (ordenadas.get(i) == ordenadas.get(i - 1) + 1) {
                        longitudRacha++;
                    } else {
                        total += Math.max(0, longitudRacha - n);
                        longitudRacha = 1;
                    }
                }
                total += Math.max(0, longitudRacha - n);
            }
        }
        return total;
    }
    
    private void verificarTodasColocadas(List<ActividadInstancia> esperadas,
                                         SolucionHorario solucion,
                                         List<String> violaciones) {
        for (ActividadInstancia inst : esperadas) {
            if (solucion.tramoDeInstancia(inst).isEmpty()) {
                violaciones.add("Instancia sin colocar: " + etiqueta(inst));
            }
        }
    }

    private void verificarNoSolapes(List<ActividadInstancia> esperadas,
                                    SolucionHorario solucion,
                                    List<String> violaciones) {
        Map<Tramo, List<ActividadInstancia>> porTramo = new LinkedHashMap<>();
        for (ActividadInstancia inst : esperadas) {
            solucion.tramoDeInstancia(inst).ifPresent(tramo ->
                    porTramo.computeIfAbsent(tramo, k -> new ArrayList<>()).add(inst));
        }

        for (Map.Entry<Tramo, List<ActividadInstancia>> entrada : porTramo.entrySet()) {
            Tramo tramo = entrada.getKey();

            Map<Profesor, Integer> profesores = new HashMap<>();
            Map<Aula, Integer> aulas = new HashMap<>();
            Map<Subgrupo, Integer> subgrupos = new HashMap<>();
            Map<GrupoAdministrativo, Integer> grupos = new HashMap<>();

            for (ActividadInstancia inst : entrada.getValue()) {
                // Set por instancia para profesor y subgrupo: si dos plazas de la
                // misma actividad listan el mismo recurso, la instancia lo ocupa
                // una vez, no dos. Correcto: S1 permite un profesor en varias
                // plazas de la misma actividad (co-docencia es varios profesores
                // en UNA plaza); idem subgrupo.
                //
                // El AULA, en cambio, se cuenta POR PLAZA (D15): por S2, dos
                // plazas de la misma instancia con la misma aula son colision, no
                // uso compartido. El uso compartido legitimo de un aula es UNA
                // plaza con varios profesores, nunca dos plazas. Contar por
                // instancia (Set) enmascararia esa colision; contar por plaza la
                // detecta. La cara de configuracion (dos aulaFija iguales en una
                // actividad) la rechaza el mapper antes del solver; aqui se cubre
                // tambien el caso de dos candidatas que resuelvan al mismo aula.
                Set<Profesor> ps = new HashSet<>();
                Set<Subgrupo> ss = new HashSet<>();
                for (Plaza plaza : inst.actividad().plazas()) {
                    ps.addAll(plaza.profesores());
                    ss.addAll(plaza.subgrupos());
                    solucion.aulaElegida(inst, plaza)
                            .ifPresent(a -> aulas.merge(a, 1, Integer::sum));
                }
                // Grupo derivado de los subgrupos de la instancia. Como ss ya es
                // un Set por instancia, varios subgrupos del mismo grupo en la
                // misma actividad (desdoble) colapsan a un único grupo: la
                // instancia ocupa el grupo una vez. Espejo de S9 en el solver,
                // ciega al grupoPadre (agrupa por subgrupo.grupo() directo).
                Set<GrupoAdministrativo> gs = new HashSet<>();
                ss.forEach(s -> gs.addAll(s.grupos()));

                ps.forEach(p -> profesores.merge(p, 1, Integer::sum));
                ss.forEach(s -> subgrupos.merge(s, 1, Integer::sum));
                gs.forEach(g -> grupos.merge(g, 1, Integer::sum));
            }

            reportarColisiones("Profesor", tramo, profesores, Profesor::codigo, violaciones);
            reportarColisiones("Aula", tramo, aulas, Aula::codigo, violaciones);
            reportarColisiones("Subgrupo", tramo, subgrupos, Subgrupo::codigo, violaciones);
            reportarColisiones("Grupo", tramo, grupos, GrupoAdministrativo::codigo, violaciones);
        }
    }

    private <T> void reportarColisiones(String tipo,
                                        Tramo tramo,
                                        Map<T, Integer> conteo,
                                        Function<T, String> codigo,
                                        List<String> violaciones) {
        for (Map.Entry<T, Integer> e : conteo.entrySet()) {
            if (e.getValue() > 1) {
                violaciones.add(tipo + " " + codigo.apply(e.getKey())
                        + " usado " + e.getValue() + " veces en el tramo " + tramo.codigo());
            }
        }
    }

    private void verificarDistribucion(ProblemaHorario problema,
                                       List<ActividadInstancia> esperadas,
                                       SolucionHorario solucion,
                                       List<String> violaciones) {
        int numDias = (int) problema.tramos().stream()
                .map(Tramo::diaSemana)
                .distinct()
                .count();

        Map<Actividad, List<ActividadInstancia>> porActividad = new LinkedHashMap<>();
        for (ActividadInstancia inst : esperadas) {
            porActividad.computeIfAbsent(inst.actividad(), k -> new ArrayList<>()).add(inst);
        }

        for (Map.Entry<Actividad, List<ActividadInstancia>> e : porActividad.entrySet()) {
            Actividad actividad = e.getKey();
            if (actividad.patronTemporal() != PatronTemporal.DISTRIBUIDA) {
                continue;
            }
            if (actividad.repeticionesPorSemana() > numDias) {
                continue; // misma guarda anti-palomar (D12) que el modelo
            }
            Set<Integer> diasUsados = new HashSet<>();
            for (ActividadInstancia inst : e.getValue()) {
                Optional<Tramo> tramo = solucion.tramoDeInstancia(inst);
                if (tramo.isEmpty()) {
                    continue; // ya reportado por verificarTodasColocadas
                }
                int dia = tramo.get().diaSemana();
                if (!diasUsados.add(dia)) {
                    violaciones.add("Actividad " + actividad.codigo()
                            + " tiene dos sesiones el mismo día (diaSemana=" + dia + ")");
                }
            }
        }
    }

    private String etiqueta(ActividadInstancia inst) {
        return inst.actividad().codigo() + "#" + inst.indice();
    }
}
