import { CeldaRef, Violacion } from '../models/diagnostico.model';
import { ReferenciaInstancia } from '../models/ajuste.model';
import { SesionVista } from '../models/horario.model';

/**
 * Lógica PURA del ajuste manual (S145): sustituir las filas de una instancia en la
 * proyección vigente, y redactar una violación rechazada. Sin señales, sin HTTP y
 * sin Angular, igual que `proyeccion.ts`, `pines.ts` y `diagnostico.ts`.
 *
 * <p>NADA de esto valida un movimiento. El veredicto es del servidor: aquí solo se
 * aplica lo que YA respondió y se redacta lo que YA rechazó.
 */

/**
 * La proyección con las filas de `ref` SUSTITUIDAS por `filas`. Devuelve una lista
 * NUEVA; no muta la de entrada.
 *
 * <p>La identidad es la INSTANCIA —el par (`actividadCodigo`, `indice`), D-6—, no
 * la actividad: dos repeticiones de la misma actividad son dos instancias, y
 * filtrar solo por `actividadCodigo` borraría la hermana que nadie movió. Esta es
 * la razón de que el intercambio se aplique llamando DOS veces, una por lado con
 * SU lista, en vez de concatenar las dos respuestas y reagrupar.
 *
 * <p>Las filas nuevas se añaden AL FINAL. El orden de `sesiones` no es contrato de
 * nada aguas abajo: `agruparPorSlot` reindexa por (dia, tramo) y `filtrar` conserva
 * el orden que reciba, así que el único orden que se ve en pantalla es el de las
 * sub-entradas DENTRO de una celda, y ahí las filas de una instancia siguen juntas
 * y en el orden en que las mandó el servidor.
 */
export function reemplazarInstancia(
  sesiones: readonly SesionVista[],
  ref: ReferenciaInstancia,
  filas: readonly SesionVista[],
): SesionVista[] {
  const otras = sesiones.filter(
    (s) => !(s.actividadCodigo === ref.actividadCodigo && s.indice === ref.indice),
  );
  return [...otras, ...filas];
}

/**
 * Una celda culpable, en el vocabulario de la instancia. `plazaCodigo` solo se
 * nombra cuando lo hay —no-null únicamente en `SOLAPE_AULA`, asimetría D15—:
 * escribirlo siempre pondría «(null)» en las otras cinco reglas.
 */
function textoCelda(c: CeldaRef): string {
  const plaza = c.plazaCodigo === null ? '' : ` (${c.plazaCodigo})`;
  return `${c.actividadCodigo} #${c.indice}${plaza}`;
}

/**
 * Una violación rechazada, en una línea legible.
 *
 * <p>CON recurso se nombra el recurso concreto —el profesor, el subgrupo, el aula
 * que quedaría doblemente ocupado—, que es lo accionable: dice QUÉ está de más.
 *
 * <p>SIN recurso se nombran las CELDAS culpables, y ese no es un caso defensivo:
 * `DISTRIBUCION_MISMO_DIA` es una regla dura que NUNCA trae `recursoCodigo`, porque
 * el conflicto es de la actividad CONSIGO MISMA —dos repeticiones el mismo día— y
 * no hay tercero al que señalar (medido en S144). Una plantilla que asumiera el
 * recurso escribiría «recurso: undefined» justo en la regla que más veces salta al
 * arrastrar, y ocultar la violación sería peor todavía: el usuario vería un rechazo
 * sin motivo.
 *
 * <p>El tramo se añade cuando lo hay, en las dos ramas, por la misma razón que la
 * plaza: `tramoCodigo` es nullable de verdad.
 */
export function textoViolacion(v: Violacion): string {
  const donde = v.tramoCodigo === null ? '' : ` en ${v.tramoCodigo}`;
  if (v.recursoCodigo !== null) {
    return `${v.regla} — ${v.recursoCodigo}${donde}`;
  }
  if (v.celdas.length === 0) {
    return `${v.regla}${donde}`;
  }
  return `${v.regla} — ${v.celdas.map(textoCelda).join(', ')}${donde}`;
}
