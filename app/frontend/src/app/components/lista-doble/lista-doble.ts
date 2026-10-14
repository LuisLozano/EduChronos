import { Component, computed, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { UMBRAL_FILTRO, filtrarOpciones } from '../filtro-opciones/filtro-opciones';

/** Lado de una lista doble: lo que ya está elegido o lo que queda por elegir. */
export type Lado = 'elegidos' | 'disponibles';

/** Instancias creadas, para que cada una tenga ids propios aunque haya varias en pantalla. */
let instancias = 0;

/**
 * Lista doble para elegir varias entidades (S230, C-lista-doble, condición 1 de
 * O-acabado-pre-prueba). Sustituye al `<select multiple>` nativo en los cuatro sitios que lo
 * tenían —grupos del subgrupo; profesores, subgrupos y aulas candidatas de la plaza— y se
 * vincula con `formControlName` como cualquier control: su valor es el mismo `string[]` de
 * CÓDIGOS que llevaba el select. Disposición, rótulos y gestos, los de la maqueta aprobada
 * (`s229/f1b/maquetas/lista-doble.js`): Elegidos a la izquierda, Disponibles a la derecha.
 *
 * <p><b>Ningún gesto pide una tecla modificadora.</b> Un clic marca o desmarca una fila (y
 * Espacio, con el foco en ella); «‹ Añadir» y «Quitar ›» pasan lo marcado de un lado al otro;
 * el doble clic o Intro pasan la fila directamente.
 *
 * <p><b>Se PINTA por código y se EMITE en el orden del catálogo.</b> Cada lista se ordena por
 * código para leerla (la maqueta), pero el valor que recibe el formulario va en el orden de
 * {@link opciones}, que es el que daba `selectedOptions`: el cuerpo enviado al backend no
 * cambia. Un código del valor que no esté entre las opciones se conserva mientras no se mueva
 * nada y se suelta al primer cambio, como hacía el select (enmienda 2 de S230).
 *
 * <p><b>El filtro es el de los selectores (S185).</b> Solo filtra Disponibles, con
 * {@link filtrarOpciones} sobre el texto visible, y lo marcado no se oculta aunque no case:
 * así no se pasa nada que no se vea. Se pinta con más de {@link UMBRAL_FILTRO} opciones.
 *
 * <p><b>Deshabilitado es BLOQUEADO</b> (el subgrupo de un PDC): sin filtro, sin botones y con
 * las filas inertes. Lo decide el estado del control (`disable()`), no un atributo.
 */
@Component({
  selector: 'app-lista-doble',
  templateUrl: './lista-doble.html',
  styleUrl: './lista-doble.css',
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => ListaDoble), multi: true }],
})
export class ListaDoble<T> implements ControlValueAccessor {
  /** Catálogo completo, en el orden del backend. */
  readonly opciones = input.required<readonly T[]>();
  /** Texto visible de la opción: lo que se lee y lo que filtra. */
  readonly texto = input.required<(o: T) => string>();
  /** Código con que la opción entra en el formulario. */
  readonly codigo = input.required<(o: T) => string>();
  /** Qué se elige, en plural y minúscula: «Filtrar grupos». */
  readonly etiqueta = input.required<string>();
  /** Nombre de la lista en el marcado (`data-lista`), para localizarla. */
  readonly nombre = input('');
  /** Filas visibles de cada lista; lo demás desplaza dentro. */
  readonly filas = input(8);
  /** Id del rótulo del bloque («Grupos»), que nombra las dos listas junto a su título. */
  readonly rotuloId = input('');

  private readonly base = `lista-doble-${++instancias}`;
  protected readonly ids = {
    filtro: `${this.base}-filtro`,
    elegidos: `${this.base}-elegidos`,
    disponibles: `${this.base}-disponibles`,
    tituloElegidos: `${this.base}-titulo-elegidos`,
    tituloDisponibles: `${this.base}-titulo-disponibles`,
  };

  private readonly elegidos = signal<ReadonlySet<string>>(new Set());
  protected readonly marcados = signal<Record<Lado, ReadonlySet<string>>>({
    elegidos: new Set(),
    disponibles: new Set(),
  });
  protected readonly consulta = signal('');
  protected readonly bloqueada = signal(false);

  protected readonly filtroActivo = computed(
    () => !this.bloqueada() && this.opciones().length > UMBRAL_FILTRO,
  );

  private readonly porCodigo = computed(() => {
    const codigo = this.codigo();
    return (a: T, b: T): number => codigo(a).localeCompare(codigo(b), 'es');
  });

  protected readonly listaElegidos = computed(() =>
    this.opciones()
      .filter((o) => this.elegidos().has(this.codigo()(o)))
      .sort(this.porCodigo()),
  );
  protected readonly listaDisponibles = computed(() =>
    this.opciones()
      .filter((o) => !this.elegidos().has(this.codigo()(o)))
      .sort(this.porCodigo()),
  );
  protected readonly visiblesDisponibles = computed(() =>
    this.filtroActivo()
      ? filtrarOpciones(
          this.listaDisponibles(),
          this.texto(),
          this.codigo(),
          this.consulta(),
          [...this.marcados().disponibles],
        )
      : this.listaDisponibles(),
  );

  private alCambiar: (valor: string[]) => void = () => {};
  private alTocar: () => void = () => {};

  writeValue(valor: readonly string[] | null): void {
    this.elegidos.set(new Set(valor ?? []));
    this.marcados.set({ elegidos: new Set(), disponibles: new Set() });
  }

  registerOnChange(fn: (valor: string[]) => void): void {
    this.alCambiar = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.alTocar = fn;
  }

  setDisabledState(deshabilitada: boolean): void {
    this.bloqueada.set(deshabilitada);
    this.marcados.set({ elegidos: new Set(), disponibles: new Set() });
    this.consulta.set('');
  }

  protected marcada(lado: Lado, codigo: string): boolean {
    return this.marcados()[lado].has(codigo);
  }

  /** Marca o desmarca una fila. No mueve nada. */
  protected marcar(lado: Lado, codigo: string): void {
    if (this.bloqueada()) {
      return;
    }
    const lista = new Set(this.marcados()[lado]);
    if (lista.has(codigo)) {
      lista.delete(codigo);
    } else {
      lista.add(codigo);
    }
    this.marcados.update((m) => ({ ...m, [lado]: lista }));
  }

  /**
   * Pasa al otro lado lo marcado en `desde`, o solo `codigo` si se indica (doble clic, Intro),
   * y avisa al formulario con el valor nuevo.
   */
  protected mover(desde: Lado, codigo?: string): void {
    if (this.bloqueada()) {
      return;
    }
    const codigos = codigo !== undefined ? [codigo] : [...this.marcados()[desde]];
    if (codigos.length === 0) {
      return;
    }
    const elegidos = new Set(this.elegidos());
    const marcados = new Set(this.marcados()[desde]);
    for (const c of codigos) {
      if (desde === 'disponibles') {
        elegidos.add(c);
      } else {
        elegidos.delete(c);
      }
      marcados.delete(c);
    }
    // En el orden del catálogo, no en el de la vista: es el que daba el select (enmienda 2).
    const valor = this.opciones()
      .map((o) => this.codigo()(o))
      .filter((c) => elegidos.has(c));
    this.elegidos.set(new Set(valor));
    this.marcados.update((m) => ({ ...m, [desde]: marcados }));
    this.alCambiar(valor);
    this.alTocar();
  }

  protected alTeclear(event: KeyboardEvent, lado: Lado, codigo: string): void {
    if (event.key === ' ') {
      event.preventDefault();
      this.marcar(lado, codigo);
    } else if (event.key === 'Enter') {
      event.preventDefault();
      this.mover(lado, codigo);
    }
  }

  protected alEscribir(event: Event): void {
    this.consulta.set((event.target as HTMLInputElement).value);
  }

  /** Lo que nombra una lista: el rótulo del bloque, si lo hay, y su título. */
  protected nombradaPor(titulo: string): string {
    return this.rotuloId() ? `${this.rotuloId()} ${titulo}` : titulo;
  }
}
