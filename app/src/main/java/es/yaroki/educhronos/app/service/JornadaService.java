package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.Dia;
import es.yaroki.educhronos.app.catalog.TramoSemanal;
import es.yaroki.educhronos.app.catalog.TramoSemanalRepository;
import es.yaroki.educhronos.app.mapper.CatalogoMapper;
import es.yaroki.educhronos.app.service.ReferenciaEntranteException.Referencia;
import es.yaroki.educhronos.app.web.dto.JornadaDTO;
import es.yaroki.educhronos.app.web.dto.JornadaRequest;
import es.yaroki.educhronos.app.web.dto.TramoJornadaDTO;
import es.yaroki.educhronos.app.web.dto.TramoJornadaRequest;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación de la JORNADA del centro (§4.1, C-jornada M3): la malla de
 * {@code TramoSemanal} que define qué horas existen en la semana. Es el CRUD que le
 * faltaba a la última entidad de catálogo sin puerta REST, y con él muere
 * {@code SeedCatalogoRunner}.
 *
 * <p><b>No es un CRUD normal, y por eso no imita a {@code AsignaturaService} entero.</b>
 * La jornada es un SINGLETON que se fija al empezar el curso: no hay alta, ni borrado, ni
 * edición tramo a tramo. Solo dos operaciones, {@link #obtenerJornada()} y
 * {@link #reemplazarJornada(JornadaRequest)}; la segunda sigue el molde de
 * {@code AsignaturaService.reemplazarAulasCompatibles} (validar todo → borrar → flush →
 * insertar) y la guarda de {@code GrupoService.borrar} (mapa inverso de FK →
 * {@link ReferenciaEntranteException} → 409).
 *
 * <p><b>La frontera es asimétrica a propósito: entra UN DÍA TIPO, sale la SEMANA.</b> El
 * {@code PUT} recibe los tramos de un solo día y este servicio los EXPANDE a
 * LUNES..VIERNES (la jornada es idéntica los cinco días, decisión de producto); el
 * {@code GET} devuelve siempre la malla completa. Así hay un único generador de malla
 * —éste, probado en JVM— y el cliente no replica ni numera. Que la entrada sea más
 * pequeña que la salida no es una inconsistencia: es lo que evita que el frontend tenga
 * que saber que {@code orden} es global y continuo entre días.
 *
 * <p><b>Las dos reglas que gobiernan la validación</b>, ambas → {@link IllegalArgumentException}
 * (→ 400 en el controlador) y aplicadas UNA vez sobre el día tipo, no cinco:
 * <ul>
 *   <li>(a) horas parseables como {@code "HH:mm"}, con {@code horaInicio < horaFin}, y sin
 *       SOLAPES entre tramos.
 *   <li>(b) <b>Techo del dominio:</b> como máximo {@value #MAX_LECTIVOS_POR_DIA} tramos
 *       LECTIVOS. No es una preferencia: {@code solver.domain.Tramo} valida
 *       {@code ordenEnDia} entre 1 y 6 en su constructor compacto, así que un séptimo
 *       lectivo no reventaría aquí sino mucho más tarde, al montar el problema en
 *       {@code CatalogoMapper.aTramos} —es decir, al GENERAR horario, con la jornada ya
 *       guardada y un 500 opaco—. Esta comprobación adelanta ese fallo al momento en que
 *       el usuario lo puede corregir. Si algún día se levanta el techo, hay que levantarlo
 *       en {@code domain.Tramo} PRIMERO y aquí después, nunca al revés.
 * </ul>
 * El día ya no se valida: no viaja en la petición. El techo de 5 días lo garantiza la
 * expansión sobre {@code Dia.values()}, no una comprobación.
 *
 * <p><b>Por qué el 409 es global.</b> Tres FK not-null apuntan a {@code tramo_semanal}
 * (restricciones horarias, sesiones de horario, sesiones bloqueadas) y el reemplazo borra
 * la tabla entera, así que basta con que exista UN dependiente para que la operación sea
 * imposible. Además de la FK, hay una razón semántica: {@code ordenEnDia} es POSICIONAL
 * —se deriva de {@code orden} y del filtro {@code esLectivo}, no se persiste—, de modo
 * que reordenar la malla bajo unos dependientes vivos movería de fila restricciones y
 * sesiones que nadie ha tocado. Reconciliar por id en vez de rechazar exigiría decidir
 * qué significa una restricción cuyo tramo ha cambiado de hora, y esa es una pregunta de
 * producto que este Cambio no contesta.
 */
@Service
public class JornadaService {

    /**
     * Techo de tramos lectivos por día. Réplica DELIBERADA del rango que
     * {@code solver.domain.Tramo} valida en su constructor compacto; ver regla (b) del
     * javadoc de clase. Cambiar uno sin el otro rompe el sistema en silencio.
     */
    static final int MAX_LECTIVOS_POR_DIA = 6;

    /** Formato de las horas en la frontera REST, en las dos direcciones. */
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * La MALLA DE REFERENCIA de un día, tal cual la sembraba {@code SeedCatalogoRunner}
     * (6 lectivos de 60' con un recreo de 30' tras el tercero). Es la propuesta que
     * devuelve {@link #obtenerJornada()} cuando la tabla está vacía, ya expandida a los
     * cinco días.
     *
     * <p>El {@code ordenEnDia} va escrito a mano, no calculado: aquí no hay entidades
     * persistidas sobre las que invocar {@code CatalogoMapper.indiceOrdenEnDia} (los ids
     * son null), y reimplementar la renumeración para este único caso añadiría un cuarto
     * espejo de la deuda D30. Como dato literal de una malla fija, no hay algoritmo que
     * pueda divergir.
     */
    private static final List<PlantillaTramo> PLANTILLA_DIA = List.of(
            new PlantillaTramo(LocalTime.of(8, 0), LocalTime.of(9, 0), true, 1),
            new PlantillaTramo(LocalTime.of(9, 0), LocalTime.of(10, 0), true, 2),
            new PlantillaTramo(LocalTime.of(10, 0), LocalTime.of(11, 0), true, 3),
            new PlantillaTramo(LocalTime.of(11, 0), LocalTime.of(11, 30), false, null),
            new PlantillaTramo(LocalTime.of(11, 30), LocalTime.of(12, 30), true, 4),
            new PlantillaTramo(LocalTime.of(12, 30), LocalTime.of(13, 30), true, 5),
            new PlantillaTramo(LocalTime.of(13, 30), LocalTime.of(14, 30), true, 6));

    private final TramoSemanalRepository repositorio;

    public JornadaService(TramoSemanalRepository repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * La jornada del centro, SIEMPRE la semana completa. NUNCA falla por ausencia: con la
     * tabla vacía sintetiza la malla de referencia expandida a los cinco días y la marca
     * {@code persistida=false}; con datos devuelve los tramos reales ordenados por
     * {@code orden} y {@code persistida=true}. El contrato de salida no cambió al pasar el
     * {@code PUT} a día tipo: un cliente que solo quiera el día tipo lee el primer día.
     */
    @Transactional(readOnly = true)
    public JornadaDTO obtenerJornada() {
        List<TramoSemanal> tramos = repositorio.findAllByOrderByOrdenAsc();
        return tramos.isEmpty() ? mallaDeReferencia() : proyectar(tramos);
    }

    /**
     * REEMPLAZO TOTAL de la malla horaria a partir del DÍA TIPO recibido. El orden de los
     * pasos es parte del contrato: primero se valida TODO contra la petición sin tocar la
     * BD, después se comprueba la guarda de dependientes, y solo entonces se expande,
     * borra e inserta. Así una petición inválida no deja la jornada a medias, y una
     * petición válida sobre un sistema con horarios vivos falla ANTES de haber borrado
     * nada.
     *
     * @throws IllegalArgumentException (→ 400) si el día tipo no cumple (a) o (b)
     * @throws ReferenciaEntranteException (→ 409) si existe cualquier dependiente
     */
    @Transactional
    public JornadaDTO reemplazarJornada(JornadaRequest peticion) {
        List<TramoValidado> diaTipo = validar(peticion);
        comprobarSinDependientes();

        // Borra lo actual y FLUSHEA antes de insertar: así el DELETE llega a la BD antes que
        // los INSERT, y no al revés (Hibernate ordena inserciones antes que borrados en su
        // cola de acciones). Mismo patrón que reemplazarAulasCompatibles.
        repositorio.deleteAll();
        repositorio.flush();

        repositorio.saveAll(expandir(diaTipo));
        // Flush para que los ids existan: proyectar() los necesita para invocar
        // CatalogoMapper.indiceOrdenEnDia, que indexa por getId().
        repositorio.flush();

        return proyectar(repositorio.findAllByOrderByOrdenAsc());
    }

    /**
     * EXPANSIÓN: replica el día tipo en los cinco días lectivos. El {@code orden} global es
     * continuo y se recorre DÍA POR DÍA (todo el lunes, luego todo el martes...), no tramo
     * por tramo entre días: con 7 tramos de día tipo salen 35 filas numeradas 1..35, el
     * lunes ocupa 1..7 y el martes arranca en 8. Ese es el orden que
     * {@code CatalogoMapper.renumerarLectivos} espera para derivar {@code ordenEnDia}.
     *
     * <p>{@code siguienteInmediato} queda a null en todos: poblar esa FK es semántica del
     * solver (invariante S6) y queda fuera de C-jornada. Deuda registrada.
     */
    private static List<TramoSemanal> expandir(List<TramoValidado> diaTipo) {
        List<TramoSemanal> semana = new ArrayList<>(Dia.values().length * diaTipo.size());
        int orden = 1;
        for (Dia dia : Dia.values()) {
            for (TramoValidado v : diaTipo) {
                semana.add(new TramoSemanal(
                        dia, v.horaInicio(), v.horaFin(), v.esLectivo(), orden++, null));
            }
        }
        return semana;
    }

    // ------------------------------------------------------------------ validación (a/b)

    /**
     * Aplica las dos reglas al día tipo y devuelve sus tramos ya parseados, en el mismo
     * orden en que llegaron (que es el que fija el {@code orden} dentro de cada día). Falla
     * al primer incumplimiento, con un mensaje que nombra el dato concreto.
     */
    private static List<TramoValidado> validar(JornadaRequest peticion) {
        Objects.requireNonNull(peticion, "peticion no puede ser null");
        if (peticion.tramos() == null || peticion.tramos().isEmpty()) {
            throw new IllegalArgumentException("tramos es obligatorio y no puede estar vacio");
        }

        List<TramoValidado> validados = new ArrayList<>(peticion.tramos().size());
        for (TramoJornadaRequest crudo : peticion.tramos()) {
            if (crudo == null) {
                throw new IllegalArgumentException("tramo en blanco");
            }
            LocalTime inicio = parseHora(crudo.horaInicio(), "horaInicio");
            LocalTime fin = parseHora(crudo.horaFin(), "horaFin");
            if (!inicio.isBefore(fin)) {
                throw new IllegalArgumentException(
                        "horaInicio debe ser anterior a horaFin en el tramo "
                                + HH_MM.format(inicio) + "-" + HH_MM.format(fin));
            }
            validados.add(new TramoValidado(inicio, fin, crudo.esLectivo()));
        }

        comprobarSinSolapes(validados);
        comprobarTechoDeLectivos(validados);
        return validados;
    }

    /** Regla (a), primera mitad: hora parseable como {@code "HH:mm"}. */
    private static LocalTime parseHora(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " es obligatorio, con formato HH:mm");
        }
        try {
            return LocalTime.parse(valor, HH_MM);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    campo + " invalido: '" + valor + "'. Formato esperado HH:mm (ej. 08:30)");
        }
    }

    /**
     * Regla (a), segunda mitad: dos tramos del día tipo no pueden solaparse. Los intervalos
     * son semiabiertos {@code [inicio, fin)}, así que encadenar 09:00-10:00 con 10:00-11:00
     * es legal —es justamente la malla de referencia—. Ya no hace falta comparar días: la
     * petición describe uno solo, y la expansión no puede introducir solapes que el día
     * tipo no tuviera.
     */
    private static void comprobarSinSolapes(List<TramoValidado> validados) {
        for (int i = 0; i < validados.size(); i++) {
            for (int j = i + 1; j < validados.size(); j++) {
                TramoValidado a = validados.get(i);
                TramoValidado b = validados.get(j);
                if (a.horaInicio().isBefore(b.horaFin()) && b.horaInicio().isBefore(a.horaFin())) {
                    throw new IllegalArgumentException(
                            "tramos solapados en el dia tipo: "
                                    + HH_MM.format(a.horaInicio()) + "-" + HH_MM.format(a.horaFin())
                                    + " y " + HH_MM.format(b.horaInicio()) + "-"
                                    + HH_MM.format(b.horaFin()));
                }
            }
        }
    }

    /**
     * Regla (b): el techo de {@value #MAX_LECTIVOS_POR_DIA} lectivos por día que impone
     * {@code solver.domain.Tramo}. Se cuenta sobre el día tipo, que por la expansión es
     * exactamente la cuenta que tendrá cada uno de los cinco días. El mensaje cita el techo
     * y la cuenta real, para que el usuario sepa cuántos sobran.
     */
    private static void comprobarTechoDeLectivos(List<TramoValidado> validados) {
        long lectivos = validados.stream().filter(TramoValidado::esLectivo).count();
        if (lectivos > MAX_LECTIVOS_POR_DIA) {
            throw new IllegalArgumentException(
                    "el maximo es " + MAX_LECTIVOS_POR_DIA + " tramos lectivos por dia; "
                            + "el dia tipo tiene " + lectivos);
        }
    }

    // ------------------------------------------------------------------ guarda 409

    /**
     * Guarda de referencias entrantes, molde exacto de {@code GrupoService.borrar} pero
     * con conteos AGREGADOS: el reemplazo borra la tabla entera, así que cualquier
     * dependiente vivo lo impide. El desglose nombra cada referente con su conteo real.
     */
    private void comprobarSinDependientes() {
        List<Referencia> entrantes = List.of(
                new Referencia("restricciones horarias", repositorio.contarRestriccionesHorarias()),
                new Referencia("sesiones de horario", repositorio.contarSesiones()),
                new Referencia("sesiones bloqueadas", repositorio.contarSesionesBloqueadas()));
        if (entrantes.stream().anyMatch(r -> r.conteo() > 0)) {
            throw new ReferenciaEntranteException(entrantes);
        }
    }

    // ------------------------------------------------------------------ proyección a DTO

    /**
     * Proyecta los tramos PERSISTIDOS al DTO, resolviendo {@code ordenEnDia} con
     * {@code CatalogoMapper.indiceOrdenEnDia} —la fuente única de la renumeración (deuda
     * D30)—, no con un cálculo propio: un cuarto espejo del algoritmo sería exactamente la
     * divergencia silenciosa contra la que avisa el javadoc de {@code renumerarLectivos}.
     * Los tramos no lectivos no están en ese índice, así que su {@code ordenEnDia} sale
     * null, que es justo lo que el DTO promete.
     */
    private static JornadaDTO proyectar(List<TramoSemanal> tramos) {
        Map<Long, Integer> ordenEnDia = CatalogoMapper.indiceOrdenEnDia(tramos);
        List<TramoJornadaDTO> filas = tramos.stream()
                .map(t -> new TramoJornadaDTO(
                        t.getDia().name(),
                        HH_MM.format(t.getHoraInicio()),
                        HH_MM.format(t.getHoraFin()),
                        t.isEsLectivo(),
                        t.getOrden(),
                        ordenEnDia.get(t.getId())))
                .toList();
        return new JornadaDTO(true, filas);
    }

    /**
     * La malla de referencia sintetizada: {@link #PLANTILLA_DIA} repetida en los cinco
     * días, con {@code orden} global CONTINUO 1..35 (no reinicia por día, igual que hacía
     * el seed). {@code persistida=false}: nadie la ha guardado, es una propuesta.
     */
    private static JornadaDTO mallaDeReferencia() {
        List<TramoJornadaDTO> filas = new ArrayList<>(Dia.values().length * PLANTILLA_DIA.size());
        int orden = 1;
        for (Dia dia : Dia.values()) {
            for (PlantillaTramo p : PLANTILLA_DIA) {
                filas.add(new TramoJornadaDTO(
                        dia.name(),
                        HH_MM.format(p.horaInicio()),
                        HH_MM.format(p.horaFin()),
                        p.esLectivo(),
                        orden++,
                        p.ordenEnDia()));
            }
        }
        return new JornadaDTO(false, filas);
    }

    /** Un tramo del día tipo ya parseado y validado individualmente (regla a). */
    private record TramoValidado(LocalTime horaInicio, LocalTime horaFin, boolean esLectivo) { }

    /** Una fila de la malla de referencia; {@code ordenEnDia} null en el recreo. */
    private record PlantillaTramo(
            LocalTime horaInicio, LocalTime horaFin, boolean esLectivo, Integer ordenEnDia) { }
}
