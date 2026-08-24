package es.yaroki.educhronos.app.web;

import static org.assertj.core.api.Assertions.assertThat;

import es.yaroki.educhronos.app.web.MapeoFalloSolver.RespuestaFallo;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Los tres desenlaces de un solve sin solución dejan de ser el mismo 422 (S118).
 *
 * <p>Unidad PURA: {@code mapear} no tiene colaboradores, así que no hay contenedor,
 * ni MockMvc, ni dobles. Lo que se fija aquí es la TABLA de traducción; que el
 * controlador la use es asunto del test de endpoint.
 *
 * <p><b>Los casos son discriminantes por pares</b>, no una lista de ejemplos: cada
 * uno fija un status o una causa DISTINTOS de los demás, así que una tabla colapsada
 * a un único status —la de hoy— no puede pasar más de uno. El de la rama por defecto
 * no es relleno: sin él, un {@code switch} sin defecto compilaría y devolvería null
 * ante un estado que CP-SAT añadiera en una versión futura, y ese null llegaría al
 * borde HTTP como un 500 sin causa en vez de como un 500 con ella.
 *
 * <p>Los dos ÚLTIMOS casos son el par más fino del fichero: null y un estado
 * desconocido son ambos "no lo reconozco", y sin embargo tienen que separarse. Que
 * los dos comparten la rama por defecto es exactamente la implementación que este
 * par debe impedir.
 */
class MapeoFalloSolverTest {

    /**
     * INFEASIBLE es el único de los cuatro que sigue siendo 422, y es el único caso
     * en que el status NO cambia respecto a la conducta previa. La causa SÍ cambia:
     * pasa de no existir a nombrar el hecho, que es lo que la vista necesita para
     * decir "esta configuración no tiene solución" sin adivinarlo del status.
     */
    @Test
    void infeasible_es422YCatalogoInfactible() {
        RespuestaFallo r = MapeoFalloSolver.mapear("INFEASIBLE");

        assertThat(r.status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r.causa()).isEqualTo("CATALOGO_INFACTIBLE");
    }

    /**
     * UNKNOWN es el caso que motiva la sesión: el solver no probó NADA —ni que hay
     * solución ni que no la hay—, solo se le acabó el tiempo. Devolverlo como 422
     * afirma "no tiene solución", que es una mentira. 503 es el status del "vuelve a
     * intentarlo", y es lo único que permite a la vista ofrecer un reintento sin
     * proponerlo también ante un catálogo imposible.
     */
    @Test
    void unknown_es503YPresupuestoAgotado() {
        RespuestaFallo r = MapeoFalloSolver.mapear("UNKNOWN");

        assertThat(r.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(r.causa()).isEqualTo("PRESUPUESTO_AGOTADO");
    }

    /**
     * MODEL_INVALID no es culpa del usuario ni del tiempo: es un modelo que nosotros
     * construimos mal. 4xx lo achacaría a quien pulsó el botón; 500 lo pone donde
     * corresponde y lo hace visible como error nuestro.
     */
    @Test
    void modelInvalid_es500YErrorInterno() {
        RespuestaFallo r = MapeoFalloSolver.mapear("MODEL_INVALID");

        assertThat(r.status()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(r.causa()).isEqualTo("ERROR_INTERNO");
    }

    /**
     * La rama por defecto. Un estado desconocido —una versión futura de OR-Tools, un
     * null que se coló— no puede caer en ninguna de las tres ramas nombradas: hacerlo
     * afirmaría un hecho que no se ha comprobado. Cae en el mismo cajón que
     * MODEL_INVALID porque es lo que es: algo que no previmos.
     */
    @Test
    void estadoDesconocido_caeEnLaRamaPorDefecto_500YErrorInterno() {
        RespuestaFallo r = MapeoFalloSolver.mapear("CUALQUIER_OTRO");

        assertThat(r.status()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(r.causa()).isEqualTo("ERROR_INTERNO");
    }
    /**
     * {@code estado == null} NO es un estado desconocido: es que NO HUBO SOLVE. La
     * excepción se construyó con el constructor de un argumento, desde
     * {@code ModeloCpSat}, abortando la construcción del modelo antes de que
     * existiera ningún {@code CpSolver} (hoy: un catálogo sin tramos).
     *
     * <p>Por eso no puede caer en la rama por defecto. 500 significa "bug nuestro" y
     * dispararía un parte de error ante el caso MÁS probable de una instalación
     * recién estrenada: un centro que aún no ha definido su jornada. Es un 422 —el
     * catálogo, tal como está, no permite construir el problema— y la causa lo dice
     * sin culpar a nadie.
     *
     * <p>Antes de S118 este caso ya salía por 422, así que mandarlo a 500 sería una
     * REGRESIÓN silenciosa introducida al separar los otros tres. Este test es lo
     * que la impide.
     */
    @Test
    void sinSolve_estadoNull_es422YConfiguracionIncompleta() {
        RespuestaFallo r = MapeoFalloSolver.mapear(null);

        assertThat(r.status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(r.causa()).isEqualTo("CONFIGURACION_INCOMPLETA");
    }
}
