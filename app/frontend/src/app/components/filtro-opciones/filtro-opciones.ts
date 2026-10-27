import { Component, computed, input, signal, ChangeDetectionStrategy } from '@angular/core';

import { UMBRAL_FILTRO, filtrarOpciones } from '../../catalogo/busqueda';

/**
 * Campo de filtro para un selector de entidades (S185, C-filtro-selectores). Sólo se pinta
 * con más de {@link UMBRAL_FILTRO} opciones. La vista itera sobre {@link visibles} en lugar
 * de sobre la lista completa; su enlace con el formulario no cambia.
 */
@Component({
  selector: 'app-filtro-opciones',
  templateUrl: './filtro-opciones.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './filtro-opciones.css',
})
export class FiltroOpciones<T> {
  readonly opciones = input.required<readonly T[]>();
  /** Texto visible de la opción: sobre él se filtra (el tutor, también por nombre). */
  readonly texto = input.required<(o: T) => string>();
  /** Código con que la opción entra en el formulario. */
  readonly codigo = input.required<(o: T) => string>();
  readonly seleccionados = input<readonly string[]>([]);
  /** Qué se filtra, para el aria-label: «Filtrar subgrupos». */
  readonly etiqueta = input.required<string>();

  protected readonly consulta = signal('');
  protected readonly total = computed(() => this.opciones().length);
  protected readonly activo = computed(() => this.total() > UMBRAL_FILTRO);

  /**
   * Lo que pinta el select. Con el filtro inactivo, todas: una consulta escrita cuando la
   * lista era larga no se aplica a ciegas cuando el campo ya no se ve.
   */
  readonly visibles = computed(() =>
    filtrarOpciones(
      this.opciones(),
      this.texto(),
      this.codigo(),
      this.activo() ? this.consulta() : '',
      this.seleccionados(),
    ),
  );

  limpiar(): void {
    this.consulta.set('');
  }

  protected alEscribir(event: Event): void {
    this.consulta.set((event.target as HTMLInputElement).value);
  }
}
