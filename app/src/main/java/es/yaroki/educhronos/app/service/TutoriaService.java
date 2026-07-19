package es.yaroki.educhronos.app.service;

import es.yaroki.educhronos.app.catalog.GrupoAdministrativo;
import es.yaroki.educhronos.app.catalog.GrupoAdministrativoRepository;
import es.yaroki.educhronos.app.catalog.Profesor;
import es.yaroki.educhronos.app.catalog.ProfesorRepository;
import es.yaroki.educhronos.app.catalog.ProfesorTutoria;
import es.yaroki.educhronos.app.catalog.ProfesorTutoriaRepository;
import es.yaroki.educhronos.app.catalog.RolTutoria;
import es.yaroki.educhronos.app.web.dto.TutoriaDTO;
import es.yaroki.educhronos.app.web.dto.TutoriaRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación del sub-recurso TUTORÍA de un grupo (§4.1, invariante I4,
 * Bloque 8.5-D2a): consulta y REEMPLAZO TOTAL de los tutores de un grupo. Réplica del
 * patrón de {@code AsignaturaService.reemplazarAulasCompatibles} (S75): el {@code PUT}
 * no edita fila a fila, borra lo que hay y escribe lo que llega, y es IDEMPOTENTE.
 *
 * <p><b>Orden de validación</b> (importa: el primer fallo gana y determina el HTTP):
 * <ol>
 *   <li>grupo inexistente por id → {@link NoSuchElementException} (→ 404);
 *   <li>profesor inexistente por código → {@link NoSuchElementException} (→ 404);
 *   <li>{@code rol} no parseable a {@link RolTutoria} → {@link IllegalArgumentException}
 *       (→ 400) que NOMBRA el valor malo y lista los válidos (patrón {@code parseTipoAula});
 *   <li>el mismo profesor repetido en la lista entrante → 400 que lo nombra;
 *   <li><b>I4</b>: más de un {@code TUTOR_PRINCIPAL} en la lista entrante → 400 que
 *       NOMBRA a todos los implicados.
 * </ol>
 * Cada paso se resuelve en una pasada COMPLETA sobre la lista antes de empezar el
 * siguiente (los pasos 2 y 3 son dos pasadas, no un bucle único por elemento): así el
 * orden de arriba se cumple ENTRE ELEMENTOS DISTINTOS y no depende de en qué posición
 * venga cada fallo. Un cuerpo con rol malo en el elemento 1 y profesor inexistente en el
 * 2 da 404, no 400.
 *
 * <p><b>I4 se valida SOBRE LA LISTA ENTRANTE y ANTES de escribir nada.</b> No se consulta
 * el estado actual del grupo: como el {@code PUT} es un reemplazo total, lo que ya hubiera
 * en la tabla va a desaparecer y no puede entrar en conflicto con lo entrante. Y no hay
 * red debajo: la PK compuesta (profesor, grupo) NO impide dos TUTOR_PRINCIPAL del mismo
 * grupo (ver {@link ProfesorTutoria}); si esta guarda se retira, la escritura pasa y la
 * base queda violando I4 sin que nada proteste.
 *
 * <p><b>Lo que este servicio NO exige.</b> La mitad "existe AL MENOS un TUTOR_PRINCIPAL"
 * de I4 no se valida: una lista vacía es un {@code PUT} legítimo (borra la tutoría) y un
 * grupo puede vivir sin tutor. Lo que se hace cumplir es "COMO MUCHO uno", no "exactamente
 * uno". La totalidad de I4 queda como responsabilidad de una futura validación de
 * configuración completa, no de la escritura fila a fila.
 */
@Service
public class TutoriaService {

    private final GrupoAdministrativoRepository grupoRepositorio;
    private final ProfesorRepository profesorRepositorio;
    private final ProfesorTutoriaRepository tutoriaRepositorio;

    public TutoriaService(GrupoAdministrativoRepository grupoRepositorio,
                          ProfesorRepository profesorRepositorio,
                          ProfesorTutoriaRepository tutoriaRepositorio) {
        this.grupoRepositorio = grupoRepositorio;
        this.profesorRepositorio = profesorRepositorio;
        this.tutoriaRepositorio = tutoriaRepositorio;
    }

    /**
     * Las tutorías de un grupo, ORDENADAS por rol (el principal primero, orden natural del
     * enum) y, dentro del rol, por código de profesor. {@link NoSuchElementException}
     * (→ 404) si el grupo no existe. Lista vacía = grupo sin tutoría (no es un 404).
     */
    @Transactional(readOnly = true)
    public List<TutoriaDTO> obtener(Long idGrupo) {
        GrupoAdministrativo grupo = grupoRepositorio.findById(idGrupo)
                .orElseThrow(() -> new NoSuchElementException("No existe grupo con id " + idGrupo));
        return ordenar(tutoriaRepositorio.findByGrupo(grupo));
    }

    /**
     * REEMPLAZO TOTAL e idempotente de la tutoría de un grupo: borra las filas actuales y
     * crea las de {@code peticiones}. Lista vacía o nula = borrar todas (grupo sin tutoría).
     * Ver el orden de validación en el Javadoc de clase. Devuelve la lista resultante
     * ordenada por rol y código.
     */
    @Transactional
    public List<TutoriaDTO> reemplazar(Long idGrupo, List<TutoriaRequest> peticiones) {
        GrupoAdministrativo grupo = grupoRepositorio.findById(idGrupo)
                .orElseThrow(() -> new NoSuchElementException("No existe grupo con id " + idGrupo));

        List<TutoriaRequest> entrantes =
                peticiones == null ? List.<TutoriaRequest>of() : peticiones;

        // Pasada 1a: resolver el profesor de TODOS los elementos (404). Completa, no fundida
        // con 1b: si el elemento 1 trae un rol malo y el 2 un profesor inexistente, manda el
        // 404 del profesor. Un bucle único por elemento daría 400 y rompería el orden.
        List<Profesor> profesores = new ArrayList<>();
        for (TutoriaRequest peticion : entrantes) {
            profesores.add(resolverProfesor(peticion));
        }

        // Pasada 1b: parsear el rol de TODOS los elementos (400).
        List<ProfesorTutoria> nuevas = new ArrayList<>();
        for (int i = 0; i < entrantes.size(); i++) {
            RolTutoria rol = parseRol(entrantes.get(i) == null ? null : entrantes.get(i).rol());
            nuevas.add(new ProfesorTutoria(profesores.get(i), grupo, rol));
        }

        // Pasada 2: ningún profesor repetido (la PK compuesta lo rechazaría, pero con un
        // 500 opaco de constraint en vez de un 400 que diga QUIÉN viene dos veces).
        Set<String> vistos = new LinkedHashSet<>();
        for (ProfesorTutoria tutoria : nuevas) {
            if (!vistos.add(tutoria.getProfesor().getCodigo())) {
                throw new IllegalArgumentException(
                        "profesor repetido en la tutoria: " + tutoria.getProfesor().getCodigo());
            }
        }

        // Pasada 3 (I4): como mucho un TUTOR_PRINCIPAL. Nombra a TODOS los implicados: con
        // solo uno de los dos el mensaje no diría cuál sobra.
        List<String> principales = nuevas.stream()
                .filter(t -> t.getRol() == RolTutoria.TUTOR_PRINCIPAL)
                .map(t -> t.getProfesor().getCodigo())
                .toList();
        if (principales.size() > 1) {
            throw new IllegalArgumentException(
                    "I4: el grupo " + grupo.getCodigo() + " no puede tener mas de un "
                            + RolTutoria.TUTOR_PRINCIPAL.name() + "; recibidos: "
                            + String.join(", ", principales));
        }

        // Borra lo actual y FLUSHEA antes de insertar: así el DELETE precede al INSERT y no
        // choca con la PK (profesor, grupo) cuando la lista repite un profesor ya presente
        // (idempotencia). Patrón LITERAL de AsignaturaService.reemplazarAulasCompatibles.
        tutoriaRepositorio.deleteAll(tutoriaRepositorio.findByGrupo(grupo));
        tutoriaRepositorio.flush();
        for (ProfesorTutoria tutoria : nuevas) {
            tutoriaRepositorio.save(tutoria);
        }
        return ordenar(nuevas);
    }

    /**
     * Resuelve el profesor por su código de negocio. Código en blanco o inexistente →
     * {@link NoSuchElementException} (→ 404), por decisión de bloque: a diferencia de
     * {@code GrupoService.resolverNivel} (que da 400), aquí una referencia que no existe
     * se trata como "no encontrado".
     */
    private Profesor resolverProfesor(TutoriaRequest peticion) {
        String codigo = peticion == null ? null : peticion.profesor();
        if (codigo == null || codigo.isBlank()) {
            throw new NoSuchElementException("profesor es obligatorio");
        }
        return profesorRepositorio.findByCodigo(codigo)
                .orElseThrow(() -> new NoSuchElementException(
                        "No existe profesor con codigo " + codigo));
    }

    /** Parsea un nombre a {@link RolTutoria}; valor malo → 400 que lo nombra y lista los
     * válidos (mismo patrón que {@code parseTipoAula} en {@code AsignaturaService}). */
    private static RolTutoria parseRol(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "rol en blanco. Valores validos: " + Arrays.toString(RolTutoria.values()));
        }
        try {
            return RolTutoria.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "rol invalido: '" + valor + "'. Valores validos: "
                            + Arrays.toString(RolTutoria.values()));
        }
    }

    /** Tutorías ordenadas por rol (orden natural del enum: principal primero) y luego por
     * código de profesor, para que la respuesta sea determinista. */
    private static List<TutoriaDTO> ordenar(List<ProfesorTutoria> tutorias) {
        return tutorias.stream()
                .sorted(Comparator.comparing(ProfesorTutoria::getRol)
                        .thenComparing(t -> t.getProfesor().getCodigo()))
                .map(t -> new TutoriaDTO(t.getProfesor().getCodigo(), t.getRol().name()))
                .toList();
    }
}
