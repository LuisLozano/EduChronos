import {
  afterNextRender,
  afterRenderEffect,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  forwardRef,
  inject,
  input,
  linkedSignal,
  model,
  signal,
  viewChild,
  ChangeDetectionStrategy,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Combobox, ComboboxPopup, ComboboxWidget } from '@angular/aria/combobox';
import { Listbox, Option } from '@angular/aria/listbox';

import { filtrarOpciones } from '../../catalogo/busqueda';

/** Instancias creadas, para que cada una tenga un id propio aunque haya varias en pantalla. */
let instancias = 0;

/**
 * Selector de una entidad con el filtro DENTRO del control (S260, C-combobox, condiciones 2 y 3
 * de O-combobox). Sustituye al `<select>` con el campo de filtro aparte delante (S185): se escribe
 * en el propio campo y la lista se acorta. Sobre el combobox y el listbox de Angular Aria.
 *
 * <p><b>El filtro es el de los selectores (S185), sin umbral.</b> {@link visibles} es
 * {@link filtrarOpciones} con lo elegido como seleccionado, así que la opción elegida no
 * desaparece aunque no case; filtra con cualquier número de opciones (DA1 de S260). Abrir sin
 * escribir enseña todas: la consulta es lo escrito desde que se abrió, no el texto del campo.
 *
 * <p><b>El valor es el CÓDIGO</b>, el mismo string que llevaba el select. Se vincula con
 * `formControlName` o, sin formulario (la vista de horario), con {@link valor}. Cerrado, el campo
 * muestra el texto de lo elegido; Intro o clic eligen y cierran; Escape y perder el foco cierran
 * sin cambiar nada y devuelven ese texto.
 *
 * <p><b>Selección explícita</b> (DA5): las flechas mueven la opción activa, no el valor. Al
 * escribir, la activa pasa a la primera visible que casa con lo escrito (EN-18): Intro elige
 * lo que se busca y no la elegida que se ve arriba. Si no casa nada, Intro no hace nada.
 *
 * <p><b>La opción vacía</b> ({@link opcionVacia}) es la del select de hoy con value '': va
 * siempre primera, no se filtra y, con {@link vaciaElegible} a false, se ve y no se puede elegir,
 * como el «— elige —» `disabled`.
 */
@Component({
  selector: 'app-selector-filtrable',
  imports: [Combobox, ComboboxPopup, ComboboxWidget, Listbox, Option],
  templateUrl: './selector-filtrable.html',
  styleUrl: './selector-filtrable.css',
  changeDetection: ChangeDetectionStrategy.Eager,
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => SelectorFiltrable), multi: true },
  ],
})
export class SelectorFiltrable<T> implements ControlValueAccessor {
  /** Catálogo completo, en el orden en que se pinta. */
  readonly opciones = input.required<readonly T[]>();
  /** Texto visible de la opción: lo que se lee y sobre lo que se filtra. */
  readonly texto = input.required<(o: T) => string>();
  /** Código con que la opción entra en el formulario. */
  readonly codigo = input.required<(o: T) => string>();
  /** Qué se elige, en plural y minúscula: «grupos». Nombra la lista para el lector de pantalla. */
  readonly etiqueta = input.required<string>();
  /** Texto de la opción con value '', o null si el selector no la tiene. */
  readonly opcionVacia = input<string | null>(null);
  /** Si la opción vacía se puede elegir; false es el «— elige —» `disabled`. */
  readonly vaciaElegible = input(true);
  /** Id del campo, al que apunta el `<label for>` de quien lo usa. */
  readonly idEntrada = input(`selector-filtrable-${++instancias}`);

  /** El código elegido ('' si ninguno). Vínculo sin formulario: `[(valor)]` o `(valorChange)`. */
  readonly valor = model('');

  protected readonly abierta = signal(false);
  protected readonly deshabilitado = signal(false);
  /** Lo escrito desde que se abrió la lista: '' al abrir con ↓ o con clic. */
  protected readonly consulta = signal('');

  /** Texto de lo elegido: el de su opción, el de la vacía con '', o nada si no está. */
  protected readonly textoElegida = computed(() => {
    const valor = this.valor();
    if (valor === '') {
      return this.opcionVacia() ?? '';
    }
    const elegida = this.opciones().find((o) => this.codigo()(o) === valor);
    return elegida === undefined ? '' : this.texto()(elegida);
  });

  /** Lo que muestra el campo: el texto de lo elegido, salvo mientras se escribe. */
  protected readonly textoCampo = linkedSignal(() => this.textoElegida());

  readonly visibles = computed(() =>
    filtrarOpciones(this.opciones(), this.texto(), this.codigo(), this.consulta(), [this.valor()]),
  );

  /**
   * El valor que se da al listbox: lo elegido si es una de sus opciones, o nada. El listbox
   * quita de su valor lo que no encuentra entre sus opciones y lo emite como cambio; con ''
   * sin opción vacía, o con un código que no está, cerraría la lista nada más abrirla.
   */
  protected readonly valorEnLista = computed((): string[] => {
    const valor = this.valor();
    if (valor === '') {
      return this.opcionVacia() === null ? [] : [''];
    }
    return this.visibles().some((o) => this.codigo()(o) === valor) ? [valor] : [];
  });

  /**
   * La opción que se activa al escribir (EN-18): la primera visible que casa con lo escrito, sin
   * contar la elegida si no casa. Null sin búsqueda (manda el listbox: la elegida) o si no casa
   * nada.
   */
  protected readonly activaBuscada = computed((): T | null => {
    const consulta = this.consulta();
    if (consulta === '') {
      return null;
    }
    return filtrarOpciones(this.visibles(), this.texto(), this.codigo(), consulta, [])[0] ?? null;
  });

  private readonly lista = viewChild(Listbox);
  private readonly campo = viewChild.required<ElementRef<HTMLInputElement>>('campo');
  private readonly envoltorio = viewChild.required<ElementRef<HTMLElement>>('envoltorio');

  private alCambiar: (valor: string) => void = () => {};
  private alTocar: () => void = () => {};

  constructor() {
    // Tras pintar la lista filtrada: el listbox deja activa la elegida mientras se vea, así que
    // la activa se fija aquí, en su índice entre las opciones pintadas (la vacía va primera).
    afterRenderEffect(() => {
      const lista = this.lista();
      const activa = this.activaBuscada();
      if (lista === undefined || activa === null) {
        return;
      }
      const previas = this.opcionVacia() === null ? 0 : 1;
      lista.gotoIndex(previas + this.visibles().indexOf(activa));
    });

    // Intro con una búsqueda que no casa con nada no elige ni cierra (EN-18). En CAPTURA sobre el
    // envoltorio: el combobox maneja Intro en el propio campo y lo reenvía a la lista.
    const destruir = inject(DestroyRef);
    afterNextRender(() => {
      const el = this.envoltorio().nativeElement;
      const sinCoincidencias = (event: KeyboardEvent): void => {
        if (event.key === 'Enter' && this.abierta() && this.consulta() !== '' && this.activaBuscada() === null) {
          event.preventDefault();
          event.stopPropagation();
        }
      };
      el.addEventListener('keydown', sinCoincidencias, { capture: true });
      destruir.onDestroy(() => el.removeEventListener('keydown', sinCoincidencias, { capture: true }));
    });
  }

  writeValue(valor: string | null): void {
    this.valor.set(valor ?? '');
    this.textoCampo.set(this.textoElegida());
  }

  registerOnChange(fn: (valor: string) => void): void {
    this.alCambiar = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.alTocar = fn;
  }

  setDisabledState(deshabilitado: boolean): void {
    this.deshabilitado.set(deshabilitado);
  }

  /** Cierra la lista y vacía la búsqueda: la vista de horario lo pide al cambiar de modo. */
  limpiar(): void {
    this.abierta.set(false);
    this.restaurar();
  }

  /** Clic en la flecha (EN-19): abre la lista con todas o la cierra, y deja el foco en el campo. */
  protected alternar(): void {
    if (this.deshabilitado()) {
      return;
    }
    if (this.abierta()) {
      this.abierta.set(false);
      this.restaurar();
    } else {
      this.consulta.set('');
      this.abierta.set(true);
    }
    this.campo().nativeElement.focus();
  }

  /** Lo que se escribe filtra; el combobox ya abre la lista al escribir. */
  protected alEscribir(texto: string): void {
    this.textoCampo.set(texto);
    this.consulta.set(texto);
  }

  /** Al cerrar sin elegir (Escape, perder el foco), todo vuelve a lo elegido. */
  protected alAbrirOCerrar(abierta: boolean): void {
    this.abierta.set(abierta);
    if (!abierta) {
      this.restaurar();
    }
  }

  /**
   * El listbox cambia su valor con Intro o clic. Sobre la opción ya elegida lo VACÍA (alterna):
   * eso no es elegir otra cosa, así que el valor se queda como estaba.
   */
  protected alElegir(valores: readonly string[]): void {
    const nuevo = valores[0];
    if (nuevo !== undefined && nuevo !== this.valor()) {
      this.valor.set(nuevo);
      this.alCambiar(nuevo);
    }
    this.abierta.set(false);
    this.restaurar();
  }

  /** Al entrar en el campo se selecciona su texto: lo que se escriba lo sustituye. */
  protected alEnfocar(event: FocusEvent): void {
    (event.target as HTMLInputElement).select();
  }

  /**
   * Intro no envía el formulario que lo contiene, como un `<select>` (EN-12 de S260): en un
   * `<input>` el navegador lo enviaría con la lista cerrada (envío implícito). Con la lista
   * abierta Intro elige, y eso lo hace el listbox.
   */
  protected noEnviar(event: Event): void {
    event.preventDefault();
  }

  protected alSalir(): void {
    this.alTocar();
  }

  private restaurar(): void {
    this.consulta.set('');
    this.textoCampo.set(this.textoElegida());
  }
}
