package es.yaroki.educhronos.app.curso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import es.yaroki.educhronos.app.config.BaseConmutable;
import es.yaroki.educhronos.app.config.FabricaDeBases;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Lo que {@link CursoService} añade sobre el duplicador: el indicador de duplicado en curso
 * (O-curso, S159, fase B) y de dónde saca la base abierta (S160).
 *
 * <p>La base del caso lleva el esquema real y NINGUNA fila de curso, que es todo lo que el
 * duplicador mira antes de rechazar: aquí no se prueba el duplicado —eso es
 * {@code DuplicadorCursoTest}—, se prueba que el indicador se apaga pase lo que pase.
 *
 * <p><b>Cómo se construye el servicio desde S160.</b> Ya no recibe una URL por
 * {@code @Value}, sino la {@link BaseConmutable} y la {@link FabricaDeBases} de verdad,
 * montadas a mano por {@link BancoDeCursos}. El cambio no es cosmético: la URL se fija en el
 * constructor y deja de ser cierta en cuanto se cambia de curso, mientras que la conmutable
 * contesta siempre por el fichero que está abierto AHORA.
 */
class CursoServiceTest {

    /**
     * (9) Un duplicado RECHAZADO deja el indicador apagado. Si {@code terminarDuplicado()}
     * viviera sólo en el camino de éxito, un nombre mal escrito dejaría la aplicación
     * rechazando toda escritura con un 403 hasta que alguien la reiniciara: el usuario habría
     * perdido el curso por una errata.
     */
    @Test
    void unDuplicadoRechazadoApagaElIndicador(@TempDir Path carpeta) throws Exception {
        Path base = BancoDeCursos.fabricar(carpeta.resolve("educhronos.db"), null, false);
        FabricaDeBases fabrica = BancoDeCursos.fabrica();
        BaseConmutable conmutable = BancoDeCursos.conmutableSobre(fabrica, base);
        EstadoCurso estado = new EstadoCurso(null);
        CursoService servicio = new CursoService(estado, conmutable, fabrica, "");

        try {
            try {
                servicio.duplicar("2026-2027", "2025/2026");
                fail("se esperaba el rechazo del nombre con guion");
            } catch (RechazoCursoException e) {
                assertThat(e.causa()).isEqualTo(DuplicadorCurso.NOMBRE_INVALIDO);
            }

            assertThat(estado.duplicando())
                    .as("el indicador se apaga en el finally, también cuando se rechaza")
                    .isFalse();
            assertThat(estado.archivado()).as("un rechazo no archiva nada").isFalse();
            assertThat(estado.cambiando())
                    .as("un duplicado rechazado no llega a intentar el cambio")
                    .isFalse();
        } finally {
            conmutable.close();
        }
    }

    /**
     * (14, S160) La base abierta sale de la conmutable, y el puntero sólo existe si hay
     * carpeta de datos que gobernar (invariante I4). Con la cadena vacía —que es lo que llega
     * cuando el post-procesador no publicó {@code educhronos.datos.carpeta}, o sea cuando se
     * arrancó con {@code --spring.datasource.url}— no hay puntero que escribir.
     */
    @Test
    void laBaseAbiertaSaleDeLaConmutableYSinCarpetaNoHayPuntero(@TempDir Path carpeta)
            throws Exception {
        Path base = BancoDeCursos.fabricar(carpeta.resolve("educhronos.db"), "2025/2026", false);
        FabricaDeBases fabrica = BancoDeCursos.fabrica();
        BaseConmutable conmutable = BancoDeCursos.conmutableSobre(fabrica, base);

        try {
            CursoService sinCarpeta =
                    new CursoService(new EstadoCurso(null), conmutable, fabrica, "");
            assertThat(sinCarpeta.baseAbierta()).isEqualTo(base);
            assertThat(sinCarpeta.puntero()).as("sin carpeta de datos, sin puntero").isNull();

            CursoService conCarpeta =
                    new CursoService(
                            new EstadoCurso(null), conmutable, fabrica, carpeta.toString());
            assertThat(conCarpeta.puntero()).isEqualTo(carpeta.resolve("curso-abierto"));
        } finally {
            conmutable.close();
        }
    }
}
