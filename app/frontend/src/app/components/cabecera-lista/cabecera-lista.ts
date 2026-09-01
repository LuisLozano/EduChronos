import { ChangeDetectionStrategy, Component, computed, input, model, output } from '@angular/core';

/**
 * Cabecera compartida de las listas de catálogo: rótulo, contador, caja de
 * búsqueda y botón de alta, fija en lo alto del panel del destino. Es la
 * cabecera que `D16` describe entera —contador, búsqueda y alta—, montada en
 * S124 sobre el molde que las SIETE listas repetían literalmente.
 *
 * <p>PRESENTACIONAL PURA: no conoce entidades, no habla con servicios y no
 * busca nada. No decide cuándo se ve el contador —recibe {@link mostrarContador}
 * ya resuelto por la lista, que es quien sabe si está cargando o en error— ni
 * cuántas filas casan: {@link coincidencias} llega contado desde fuera. Lo único
 * que sí gobierna es el TEXTO de la consulta, que publica por {@link busqueda}
 * en dos direcciones.
 *
 * <p>El prefijo BEM es PROPIO (`cabecera-lista__…`), no el de cada entidad: con
 * la encapsulación de vista de Angular el CSS del padre ya no alcanza a este
 * DOM, así que las reglas viajaron aquí con el componente. La única que se queda
 * fuera es `.cabecera-lista-fija` (`styles.css`), global a propósito.
 *
 * <p>{@link rotuloAlta} es TEXTO, no se deriva del título: el e2e localiza el
 * botón por su texto exacto («Nueva aula», «Nuevo grupo»…) y la concordancia de
 * género no se puede calcular a partir de «Aulas» o «Grupos».
 */
@Component({
  selector: 'app-cabecera-lista',
  templateUrl: './cabecera-lista.html',
  styleUrl: './cabecera-lista.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  // La clase que la pega va en el ELEMENTO del componente, no en el div de dentro:
  // un `position: sticky` no puede salir de su contenedor de bloque, y ese div lo
  // tiene a él, que mide justo lo que mide la cabecera —cero recorrido, se iría con
  // el scroll—. En el host, el contenedor es `.actividades`/`.subgrupos`… y ahí sí
  // hay toda la altura de la lista por la que quedarse pegada. Medido en S124.
  host: { class: 'cabecera-lista-fija' },
})
export class CabeceraLista {
  /** Rótulo del destino: «Actividades», «Aulas»… Se pinta tal cual en el `<h2>`. */
  readonly titulo = input.required<string>();
  /** Filas que tiene la lista en total, casen o no con la consulta. */
  readonly total = input.required<number>();
  /**
   * Filas que casan con la consulta vigente, YA contadas por la lista. Con la
   * consulta vacía vale lo mismo que {@link total} y no se pinta: el contador de
   * dos números solo aparece cuando hay algo escrito.
   */
  readonly coincidencias = input.required<number>();
    /**
   * Si el contador se pinta. Lo decide la LISTA, no este componente: es quien
   * sabe si está cargando y si tiene datos.
   *
   * <p>El criterio es `!cargando() && <entidad>().length > 0` (S129). Decía
   * `!cargando() && !error()`, y eso apagaba el contador ante un 409 de borrado
   * con la tabla llena debajo, justo cuando el total es indiscutible
   * (`D-contador-se-apaga-con-error`). Lo que aquel criterio protegía sigue
   * cubierto por el nuevo: si la carga falla desde vacío el array está vacío, así
   * que tampoco se pinta el «0» que mentiría diciendo que el catálogo lo está.
   */
  readonly mostrarContador = input.required<boolean>();
  /** Texto exacto del botón de alta, con su género: «Nueva aula», «Nuevo grupo». */
  readonly rotuloAlta = input.required<string>();

  /**
   * Texto que hay escrito en la caja, en las DOS direcciones: la cabecera lo
   * escribe al teclear y la lista lo lee para contar. Arranca vacío, que es
   * «enséñamelo todo».
   */
  readonly busqueda = model('');

  /** Se pulsó el botón de alta. La lista decide qué abrir. */
  readonly alta = output<void>();

  /**
   * Si hay una consulta DE VERDAD, y por tanto el contador pasa a «n de N».
   * Se compara sobre `trim()` a propósito: teclear dos espacios no casa menos
   * filas —{@link coincidencias} seguiría valiendo {@link total}—, así que
   * anunciar «334 de 334» sería ruido que no informa de nada.
   */
  private readonly hayConsulta = computed(() => this.busqueda().trim().length > 0);

  /**
   * Texto ya montado del contador: «334» sin consulta, «17 de 334» con ella. Va
   * aquí y no como dos ramas en la plantilla porque así el estado se asevera
   * comparando UNA cadena, sin depender de qué nodos monte cada rama.
   */
  protected readonly textoContador = computed(() =>
    this.hayConsulta() ? `${this.coincidencias()} de ${this.total()}` : `${this.total()}`,
  );
}
