import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * Estados compartidos de las listas de catálogo: error, carga, catálogo vacío y
 * consulta sin coincidencias. Montado en S128 sobre el molde que las SIETE
 * listas repetían LITERALMENTE —estructura idéntica carácter a carácter, medida
 * en el M2—, igual que S124 hizo con {@link CabeceraLista} y las siete
 * cabeceras.
 *
 * <p>PRESENTACIONAL PURO: no conoce entidades, no carga nada y no cuenta nada.
 * Recibe {@link cargando} y {@link error} ya resueltos por la lista, que es
 * quien sabe, y los dos recuentos ya hechos.
 *
 * <p>LA TABLA SE QUEDA FUERA, y no por gusto: el M2 de S128 midió que el
 * contenido de un `<ng-content>` lo CONSTRUYE el padre en su sitio de
 * declaración, y el `@if` del hijo solo decide si lo inserta en el DOM
 * (sonda: `construido=1` con la rama apagada). Proyectar la tabla la
 * instanciaría también mientras carga y tras un error, evaluando su `@for` en
 * estados donde hoy ni existe. La alternativa —`ngTemplateOutlet`— se descartó
 * por ser un mecanismo estructural sin precedente en este repo, por el mismo
 * criterio con que `styles.css` se niega a introducir un `:host` que sería «un
 * segundo patrón conviviendo con el único que hay». La lista pinta su tabla con
 * el COMPLEMENTO de esta cadena: `@if (!cargando() && visibles().length > 0)`,
 * escrito idéntico en las siete y verificable por grep.
 *
 * <p>{@link textoVacio} es TEXTO y no se deriva del rótulo, por el mismo motivo
 * que `rotuloAlta` en la cabecera —la concordancia de género no se calcula a
 * partir de «Aulas»— y por uno propio: Niveles añade una segunda frase sobre la
 * dependencia con Grupos, así que el texto de vacío no es una plantilla con un
 * hueco.
 *
 * <p>El prefijo BEM es PROPIO (`estado-lista__…`): con la encapsulación de vista
 * el CSS del padre ya no alcanza a este DOM, así que las ~21 reglas repartidas
 * por las siete hojas viajan aquí con el componente.
 */
@Component({
  selector: 'app-estado-lista',
  templateUrl: './estado-lista.html',
  styleUrl: './estado-lista.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EstadoLista {
  /** Si la lista está esperando la carga. */
  readonly cargando = input.required<boolean>();
  /**
   * Texto del fallo YA traducido por la lista, o `null`. Es cadena y no
   * booleano porque el mensaje viaja con su `role="alert"` y quien lo traduce
   * es `mensaje()` en la lista.
   */
  readonly error = input.required<string | null>();
  /** Filas que tiene la lista en total, casen o no con la consulta. */
  readonly total = input.required<number>();
  /** Filas que casan con la consulta vigente, ya contadas por la lista. */
  readonly coincidencias = input.required<number>();
  /** Texto escrito en la caja de búsqueda, para citarlo en «sin resultados». */
  readonly busqueda = input.required<string>();
  /** Texto exacto del catálogo vacío, con su género y sus frases. */
  readonly textoVacio = input.required<string>();

  /**
   * Qué rama de estado toca, resuelta como UNA cadena en vez de como cuatro
   * ramas encadenadas en la plantilla. Va aquí por dos motivos y el segundo es
   * el que importa:
   *
   * <p>(1) Se asevera comparando un valor, sin depender de qué nodos monte cada
   * rama (mismo criterio que `textoContador` de la cabecera).
   *
   * <p>(2) LA PRECEDENCIA DEL ERROR NO ES UNIFORME, y escrita en la plantilla se
   * rompe sola. `D-vacio-miente-con-error` pide condicionar el VACÍO a
   * `!error()` y NADA MÁS: S124 probó encadenar el error con el resto y lo
   * revirtió midiendo que un 409 de borrado hacía desaparecer la tabla entera
   * (8 filas → 0), y ese 409 es camino de uso normal desde S113. Pero
   * «`total === 0 && !error()`» escrito tal cual en un `@else if` deja caer el
   * caso al hermano siguiente, `coincidencias === 0`, que también es cierto: el
   * vacío dejaría de mentir y empezaría a mentir «Ningún resultado para «»».
   * Con `ninguno` explícito, el error apaga las dos ramas de recuento sin tocar
   * la tabla, que no mira el error nunca.
   */
  protected readonly estado = computed<'cargando' | 'vacio' | 'sin-resultados' | 'ninguno'>(() => {
    if (this.cargando()) return 'cargando';
    if (this.total() === 0) return this.error() ? 'ninguno' : 'vacio';
    if (this.coincidencias() === 0) return 'sin-resultados';
    return 'ninguno';
  });
}
