package es.yaroki.educhronos.solver.io;

import es.yaroki.educhronos.solver.domain.Actividad;
import es.yaroki.educhronos.solver.domain.Asignatura;
import es.yaroki.educhronos.solver.domain.Aula;
import es.yaroki.educhronos.solver.domain.GrupoAdministrativo;
import es.yaroki.educhronos.solver.domain.PatronTemporal;
import es.yaroki.educhronos.solver.domain.Plaza;
import es.yaroki.educhronos.solver.domain.ProblemaHorario;
import es.yaroki.educhronos.solver.domain.Profesor;
import es.yaroki.educhronos.solver.domain.Subgrupo;
import es.yaroki.educhronos.solver.domain.TipoGrupo;
import es.yaroki.educhronos.solver.domain.Tramo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Construye un {@link ProblemaHorario} de dominio a partir de los DTOs deserializados.
 *
 * Reparto de validaciones:
 *  - El mapper valida: secciones obligatorias presentes, codigos no duplicados,
 *    integridad referencial por codigo (incluida la resolucion de
 *    aulasCandidatas a entidades de dominio, Fase 3), e I2 (subgrupos
 *    disjuntos por actividad).
 *  - Los records de dominio auto-validan I5, I7, el XOR aulaFija/aulasCandidatas
 *    y los rangos (diaSemana, ordenEnDia, repeticiones, duracion). El mapper
 *    solo envuelve esas excepciones en ProblemaInvalidoException con contexto.
 *
 * Las invariantes de particion I1, I3 e I6 NO se validan aqui: el JSON del solver
 * no transporta particiones (responsabilidad de la capa de configuracion, Fase 6/8).
 */
public final class ProblemaHorarioMapper {

    private ProblemaHorarioMapper() { }

    public static ProblemaHorario aDominio(ProblemaHorarioDto dto) {
        Objects.requireNonNull(dto, "dto no puede ser null");

        List<TramoDto> tramosDto      = requiereSeccion(dto.tramos(), "tramos");
        List<AulaDto> aulasDto        = requiereSeccion(dto.aulas(), "aulas");
        List<AsignaturaDto> asigsDto  = requiereSeccion(dto.asignaturas(), "asignaturas");
        List<ProfesorDto> profesDto   = requiereSeccion(dto.profesores(), "profesores");
        List<GrupoDto> gruposDto      = requiereSeccion(dto.grupos(), "grupos");
        List<SubgrupoDto> subgsDto    = requiereSeccion(dto.subgrupos(), "subgrupos");
        List<ActividadDto> actsDto    = requiereSeccion(dto.actividades(), "actividades");

        // ---- Pasada 1: catalogos (codigo -> entidad de dominio) ----
        Map<String, Tramo> tramos = new LinkedHashMap<>();
        for (TramoDto t : tramosDto) {
            String cod = exigirCodigo(t.codigo(), "tramo");
            comprobarNoDuplicado(tramos, cod, "tramo");
            int dia   = exigir(t.diaSemana(), "diaSemana", "tramo '" + cod + "'");
            int orden = exigir(t.ordenEnDia(), "ordenEnDia", "tramo '" + cod + "'");
            tramos.put(cod, construir(() -> new Tramo(cod, dia, orden), "tramo '" + cod + "'"));
        }

        Map<String, Aula> aulas = new LinkedHashMap<>();
        for (AulaDto a : aulasDto) {
            String cod = exigirCodigo(a.codigo(), "aula");
            comprobarNoDuplicado(aulas, cod, "aula");
            aulas.put(cod, construir(() -> new Aula(a.codigo(), a.nombre()), "aula '" + cod + "'"));
        }

        Map<String, Asignatura> asignaturas = new LinkedHashMap<>();
        for (AsignaturaDto a : asigsDto) {
            String cod = exigirCodigo(a.codigo(), "asignatura");
            comprobarNoDuplicado(asignaturas, cod, "asignatura");
            asignaturas.put(cod, construir(() -> new Asignatura(a.codigo(), a.nombre()),
                    "asignatura '" + cod + "'"));
        }

        Map<String, Profesor> profesores = new LinkedHashMap<>();
        for (ProfesorDto p : profesDto) {
            String cod = exigirCodigo(p.codigo(), "profesor");
            comprobarNoDuplicado(profesores, cod, "profesor");
            profesores.put(cod, construir(() -> new Profesor(p.codigo(), p.nombre()),
                    "profesor '" + cod + "'"));
        }

        // Grupos: resolucion con grupoPadre, tolerante a orden de declaracion y con deteccion de ciclos.
        Map<String, GrupoDto> grupoDtos = new LinkedHashMap<>();
        for (GrupoDto g : gruposDto) {
            String cod = exigirCodigo(g.codigo(), "grupo");
            comprobarNoDuplicado(grupoDtos, cod, "grupo");
            grupoDtos.put(cod, g);
        }
        Map<String, GrupoAdministrativo> grupos = new LinkedHashMap<>();
        for (String cod : grupoDtos.keySet()) {
            resolverGrupo(cod, grupoDtos, grupos, new LinkedHashSet<>());
        }

        Map<String, Subgrupo> subgrupos = new LinkedHashMap<>();
        for (SubgrupoDto s : subgsDto) {
            String cod = exigirCodigo(s.codigo(), "subgrupo");
            comprobarNoDuplicado(subgrupos, cod, "subgrupo");
            GrupoAdministrativo g = resolver(grupos, s.grupo(), "grupo", "subgrupo '" + cod + "'");
            subgrupos.put(cod, construir(() -> new Subgrupo(s.codigo(), g), "subgrupo '" + cod + "'"));
        }

        // ---- Pasada 2: actividades ----
        List<Actividad> actividades = new ArrayList<>();
        for (ActividadDto act : actsDto) {
            String cod = exigirCodigo(act.codigo(), "actividad");
            String ctx = "actividad '" + cod + "'";

            Optional<Asignatura> asigAct = act.asignatura() == null
                    ? Optional.empty()
                    : Optional.of(resolver(asignaturas, act.asignatura(), "asignatura", ctx));

            String patronTxt   = exigirTexto(act.patronTemporal(), "patronTemporal", ctx);
            PatronTemporal pat = construir(() -> PatronTemporal.valueOf(patronTxt),
                    ctx + ": patronTemporal");
            int rep = exigir(act.repeticionesPorSemana(), "repeticionesPorSemana", ctx);
            int dur = exigir(act.duracionTramos(), "duracionTramos", ctx);

            List<PlazaDto> plazasDto = act.plazas();
            if (plazasDto == null || plazasDto.isEmpty()) {
                throw new ProblemaInvalidoException(ctx + ": debe declarar al menos una plaza");
            }
            List<Plaza> plazas = new ArrayList<>();
            for (PlazaDto pl : plazasDto) {
                plazas.add(mapearPlaza(pl, ctx, asignaturas, profesores, aulas, subgrupos));
            }
            verificarI2(plazas, ctx);

            actividades.add(construir(
                    () -> new Actividad(cod, asigAct, rep, dur, pat, plazas), ctx));
        }

        // ProblemaHorario exige tramos ordenados por (diaSemana, ordenEnDia).
        // El mapper los ordena: el autor del JSON no tiene que preocuparse del orden.
        List<Tramo> tramosOrdenados = new ArrayList<>(tramos.values());
        tramosOrdenados.sort(Comparator.comparingInt(Tramo::diaSemana)
                .thenComparingInt(Tramo::ordenEnDia));

        return construir(() -> new ProblemaHorario(
                tramosOrdenados,
                new ArrayList<>(aulas.values()),
                new ArrayList<>(asignaturas.values()),
                new ArrayList<>(profesores.values()),
                new ArrayList<>(grupos.values()),
                new ArrayList<>(subgrupos.values()),
                actividades), "problema");
    }

    private static Plaza mapearPlaza(PlazaDto pl, String ctxActividad,
                                     Map<String, Asignatura> asignaturas,
                                     Map<String, Profesor> profesores,
                                     Map<String, Aula> aulas,
                                     Map<String, Subgrupo> subgrupos) {
        String cod = exigirCodigo(pl.codigo(), "plaza");
        String ctx = ctxActividad + ", plaza '" + cod + "'";

        Asignatura asig = resolver(asignaturas, pl.asignatura(), "asignatura", ctx);

        List<String> profCodes = pl.profesores();
        if (profCodes == null || profCodes.isEmpty()) {
            throw new ProblemaInvalidoException(
                    ctx + ": 'profesores' debe ser un array no vacio (I7)");
        }
        Set<Profesor> profs = new LinkedHashSet<>();
        for (String pc : profCodes) {
            profs.add(resolver(profesores, pc, "profesor", ctx));
        }

        List<String> candCodes = pl.aulasCandidatas() == null ? List.of() : pl.aulasCandidatas();
        Set<Aula> aulasCandidatas = new LinkedHashSet<>();
        for (String ac : candCodes) {
            aulasCandidatas.add(resolver(aulas, ac, "aula", ctx));
        }
        Optional<Aula> aulaFija = pl.aulaFija() == null
                ? Optional.empty()
                : Optional.of(resolver(aulas, pl.aulaFija(), "aula", ctx));

        List<String> subCodes = pl.subgrupos();
        if (subCodes == null || subCodes.isEmpty()) {
            throw new ProblemaInvalidoException(ctx + ": 'subgrupos' debe ser un array no vacio");
        }
        Set<Subgrupo> subs = new LinkedHashSet<>();
        for (String sc : subCodes) {
            subs.add(resolver(subgrupos, sc, "subgrupo", ctx));
        }

        return construir(() -> new Plaza(cod, asig, profs, aulaFija, aulasCandidatas, subs), ctx);
    }

    /** I2: dentro de una actividad, ningun subgrupo aparece en dos plazas. */
    private static void verificarI2(List<Plaza> plazas, String ctx) {
        Set<Subgrupo> vistos = new LinkedHashSet<>();
        for (Plaza p : plazas) {
            for (Subgrupo s : p.subgrupos()) {
                if (!vistos.add(s)) {
                    throw new ProblemaInvalidoException(ctx + ": el subgrupo '" + s.codigo()
                            + "' aparece en mas de una plaza de la misma actividad (I2)");
                }
            }
        }
    }

    private static GrupoAdministrativo resolverGrupo(
            String codigo,
            Map<String, GrupoDto> dtos,
            Map<String, GrupoAdministrativo> resueltos,
            Set<String> enCurso) {

        GrupoAdministrativo ya = resueltos.get(codigo);
        if (ya != null) {
            return ya;
        }
        if (!enCurso.add(codigo)) {
            throw new ProblemaInvalidoException(
                    "ciclo de grupoPadre detectado en grupo '" + codigo + "'");
        }
        GrupoDto dto = dtos.get(codigo);
        if (dto == null) {
            throw new ProblemaInvalidoException("no existe grupo con codigo '" + codigo + "'");
        }

        String tipoTxt   = exigirTexto(dto.tipo(), "tipo", "grupo '" + codigo + "'");
        TipoGrupo tipo   = construir(() -> TipoGrupo.valueOf(tipoTxt), "grupo '" + codigo + "': tipo");

        Optional<GrupoAdministrativo> padre = Optional.empty();
        if (dto.grupoPadre() != null) {
            padre = Optional.of(resolverGrupo(dto.grupoPadre(), dtos, resueltos, enCurso));
        }
        final Optional<GrupoAdministrativo> padreF = padre;
        GrupoAdministrativo g = construir(
                () -> new GrupoAdministrativo(codigo, tipo, padreF), "grupo '" + codigo + "'");
        enCurso.remove(codigo);
        resueltos.put(codigo, g);
        return g;
    }

    // ---- helpers ----

    private static <T> List<T> requiereSeccion(List<T> lista, String nombre) {
        if (lista == null) {
            throw new ProblemaInvalidoException("falta la seccion obligatoria '" + nombre + "'");
        }
        return lista;
    }

    private static String exigirCodigo(String codigo, String tipo) {
        if (codigo == null || codigo.isBlank()) {
            throw new ProblemaInvalidoException("un " + tipo + " tiene 'codigo' vacio o ausente");
        }
        return codigo;
    }

    private static String exigirTexto(String valor, String campo, String ctx) {
        if (valor == null || valor.isBlank()) {
            throw new ProblemaInvalidoException(ctx + ": falta '" + campo + "'");
        }
        return valor;
    }

    private static int exigir(Integer valor, String campo, String ctx) {
        if (valor == null) {
            throw new ProblemaInvalidoException(ctx + ": falta '" + campo + "'");
        }
        return valor;
    }

    private static void comprobarNoDuplicado(Map<String, ?> mapa, String codigo, String tipo) {
        if (mapa.containsKey(codigo)) {
            throw new ProblemaInvalidoException(tipo + " con codigo duplicado: '" + codigo + "'");
        }
    }

    private static <T> T resolver(Map<String, T> mapa, String codigo, String tipoRef, String ctx) {
        if (codigo == null || codigo.isBlank()) {
            throw new ProblemaInvalidoException(ctx + ": referencia a " + tipoRef + " vacia o ausente");
        }
        T valor = mapa.get(codigo);
        if (valor == null) {
            throw new ProblemaInvalidoException(
                    ctx + ": no existe " + tipoRef + " con codigo '" + codigo + "'");
        }
        return valor;
    }

    /** Envuelve las excepciones de validacion de los records de dominio. */
    private static <T> T construir(Supplier<T> supplier, String ctx) {
        try {
            return supplier.get();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ProblemaInvalidoException(ctx + ": " + e.getMessage(), e);
        }
    }
}