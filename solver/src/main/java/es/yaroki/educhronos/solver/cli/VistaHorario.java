package es.yaroki.educhronos.solver.cli;

import es.yaroki.educhronos.solver.domain.ProblemaHorario;

import java.util.List;
import java.util.Set;

/**
 * Configuración de una vista tabular del horario (por grupo, por profesor, ...).
 *
 * La clave K identifica una "fila lógica" de la vista; el HorarioPrinter
 * genera una sub-tabla 6×5 por cada K presente.
 *
 * Una misma sesión puede pertenecer a varias filas (co-docencia en
 * vista por profesor, agrupamiento transversal en vista por grupo): por eso
 * filasDe devuelve Set<K> y no K.
 */
interface VistaHorario<K> {

    String titulo();

    List<K> filas(ProblemaHorario problema);

    String etiquetaFila(K clave);

    Set<K> filasDe(SesionMaterializada sesion);

    String contenidoCelda(SesionMaterializada sesion);
}