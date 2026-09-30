import { Component, computed, input, signal } from '@angular/core';

import { coincide } from '../../catalogo/busqueda';

/** El filtro aparece cuando la lista tiene MÁS de estas opciones (condición 5 de O-pre-demo). */
export const UMBRAL_FILTRO = 10;

/**
 * Opciones que se muestran para una consulta: las que casan con {@link coincide} sobre su
 * texto visible y, además, las ya seleccionadas aunque no casen, en el orden de entrada.
 *
 * <p><b>Nada elegido queda fuera de la vista (S185, F2).</b> Los multiselect calculan su
 * valor con `select.selectedOptions`, que sólo ve el DOM: una opción elegida y oculta se
 * perdería en el siguiente cambio sin aviso. En los simples, el select nativo se quedaría
 * en blanco aunque el formulario conservara el valor.
 */
export function filtrarOpciones<T>(
  opciones: readonly T[],
  texto: (o: T) => string,
  codigo: (o: T) => string,
  consulta: string,
  seleccionados: readonly string[],
): T[] {
  const elegidos = new Set(seleccionados);
  return opciones.filter((o) => elegidos.has(codigo(o)) || coincide(texto(o), consulta));
}

/**
 * Campo de filtro para un selector de entidades (S185, C-filtro-selectores). Sólo se pinta
 * con más de {@link UMBRAL_FILTRO} opciones. La vista itera sobre {@link visibles} en lugar
 * de sobre la lista completa; su enlace con el formulario no cambia.
 */
@Component({
  selector: 'app-filtro-opciones',
  templateUrl: './filtro-opciones.html',
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
