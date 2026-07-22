import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { Component, computed, input, output } from '@angular/core';

import { SesionVista } from '../../models/horario.model';
import { DIAS, InstanciaCelda, TRAMOS, agruparPorActividad, claveSlot } from '../../horario/proyeccion';
import { clavePin } from '../../horario/pines';
import { ViolacionEnCelda } from '../../horario/diagnostico';

/** Instancia soltada en un slot destino, en coordenadas de `TramoRef`. */
export interface SueltaInstancia {
  actividadCodigo: string;
  indice: number;
  dia: number;
  orden: number;
}

/**
 * Rejilla reutilizable de 5 días × 6 tramos. Recibe una lista de `SesionVista`
 * YA filtrada (por grupo, profesor o aula) y la agrupa por `(dia, tramo)` y, ya
 * dentro del slot, por instancia. Cada sub-entrada muestra asignatura,
 * profesores (lista), aula y grupos (lista): no asume cardinalidad 1 en
 * profesores ni en grupos.
 *
 * <p>La unidad arrastrable es la INSTANCIA, nunca la sub-entrada (D-F8.6-A-2):
 * las 6 plazas de un bloque comparten tramo y se mueven juntas. La rejilla NO
 * mueve nada al soltar —sigue pintando la proyección vigente del servidor, que
 * no cambia hasta regenerar— ni habla con el servicio: emite `soltar` y
 * `despinar`, y el contenedor decide qué hacer con ambos.
 *
 * <p>Los dos outputs hablan el vocabulario de la INSTANCIA, no el de la
 * persistencia: `despinar` emite la CLAVE de {@link clavePin}, nunca el `id` del
 * bloqueo. La rejilla ignora que los pines tengan id, y por eso también ignora
 * que ese id pueda faltar: el candado es SIEMPRE un botón, y resolver la clave
 * —o descubrir que no se puede— es trabajo del contenedor.
 */
@Component({
  selector: 'app-horario-grid',
  imports: [DragDropModule],
  templateUrl: './horario-grid.html',
  styleUrl: './horario-grid.css',
})
export class HorarioGrid {
  readonly sesiones = input.required<readonly SesionVista[]>();
  /**
   * Instancias ya pinadas, indexadas por la clave de {@link clavePin}: pintan
   * candado. El valor del mapa —el id del bloqueo— NO se lee aquí; se acepta en
   * el tipo para no obligar al contenedor a construir una proyección aparte.
   */
  readonly pinadas = input<ReadonlyMap<string, number | null>>(new Map<string, number | null>());
  /**
   * Suma con signo del coste blando de cada instancia (clave de {@link clavePin}),
   * YA agregada por el contenedor: la rejilla PINTA el número, no lo calcula ni
   * conoce las penalizaciones que lo componen. No es `Totales` y no debe
   * "cuadrarse" con nada. Una clave ausente significa "sin badge" —las de suma 0
   * no llegan (C2/S65)—, así que el predicado es `has`, sin comparar con 0.
   */
  readonly badges = input<ReadonlyMap<string, number>>(new Map<string, number>());
  /**
   * Violaciones DURAS por instancia (clave de {@link clavePin}), cada una vista
   * desde una de sus celdas ({@link ViolacionEnCelda}). La rejilla PINTA el
   * resalte; no calcula el índice —lo recibe ya construido por la capa pura—.
   *
   * <p>La asimetría D15 se resuelve AQUÍ, al pintar, y no antes: solo esta capa
   * enumera las sub-entradas y conoce su `plazaCodigo`, así que solo aquí se puede
   * decidir si una violación de aula (plaza no-null) casa con ESTA sub-entrada o
   * si una violación de instancia (plaza null) tiñe la celda entera. Por eso el
   * input es UN mapa sin partir, no dos ya separados por granularidad.
   */
  readonly violaciones = input<ReadonlyMap<string, readonly ViolacionEnCelda[]>>(
    new Map<string, readonly ViolacionEnCelda[]>(),
  );

  readonly soltar = output<SueltaInstancia>();
  /** Petición de quitar el pin de una instancia, por CLAVE de {@link clavePin}. */
  readonly despinar = output<string>();

  protected readonly dias = DIAS;
  protected readonly tramos = TRAMOS;

  private readonly celdas = computed(() => agruparPorActividad(this.sesiones()));

  /** Instancias del slot (dia, tramo); vacío si no hay ninguna. */
  protected instancias(dia: number, tramo: number): InstanciaCelda[] {
    return this.celdas().get(claveSlot(dia, tramo)) ?? [];
  }

  protected clave(inst: InstanciaCelda): string {
    return clavePin(inst.actividadCodigo, inst.indice);
  }

  protected estaPinada(inst: InstanciaCelda): boolean {
    return this.pinadas().has(this.clave(inst));
  }

  /** Hay badge si la instancia tiene clave en el mapa. Suma 0 no llega (C2/S65). */
  protected tieneBadge(inst: InstanciaCelda): boolean {
    return this.badges().has(this.clave(inst));
  }

  /** El número del badge; `undefined` si no hay, pero solo se lee tras {@link tieneBadge}. */
  protected badge(inst: InstanciaCelda): number | undefined {
    return this.badges().get(this.clave(inst));
  }

  /**
   * Resalte de la INSTANCIA entera: hay alguna violación de esta instancia con
   * `plazaCodigo === null` —las de profesor y subgrupo, que no hablan de una
   * plaza concreta (D15)—. `.some` porque una instancia puede tener a la vez
   * violaciones de instancia y de aula, y basta una de las primeras.
   */
  protected tieneViolacionInstancia(inst: InstanciaCelda): boolean {
    return (this.violaciones().get(this.clave(inst)) ?? []).some((v) => v.plazaCodigo === null);
  }

  /**
   * Resalte de UNA sub-entrada: hay alguna violación de aula de esta instancia
   * cuya plaza sea la de ESTA sub-entrada (D15). Se compara `plazaCodigo`, nunca
   * `aulaCodigo`: dos plazas distintas pueden compartir aula y la clave de la
   * violación es la plaza. `.some` evalúa cada entrada por separado, así que en un
   * desdoble (una violación con dos celdas, una por plaza) cada sub-entrada casa
   * con la SUYA (ver T5).
   */
  protected tieneViolacionAula(inst: InstanciaCelda, e: SesionVista): boolean {
    return (this.violaciones().get(this.clave(inst)) ?? []).some((v) => v.plazaCodigo === e.plazaCodigo);
  }

  /**
   * Emite la petición de despinar. El `stopPropagation` es DEFENSIVO: hoy ningún
   * ancestro escucha `click`, pero el botón vive dentro del `cdkDrag`, y dejar
   * que el evento suba invitaría a que un listener futuro en la instancia o en la
   * celda tratase el despinado como una selección.
   */
  protected alDespinar(inst: InstanciaCelda, evento: Event): void {
    evento.stopPropagation();
    this.despinar.emit(this.clave(inst));
  }

  /**
   * Traduce el drop del CDK a `soltar`. Soltar en el mismo slot no es un pin
   * nuevo y no emite nada; el resto se emite tal cual, sin validar (las reglas
   * de D-3 son del backend).
   *
   * <p>AVISO: `evento.item.data as InstanciaCelda` es un cast SIN comprobación.
   * Solo es válido mientras el `cdkDropListGroup` de esta plantilla conecte
   * únicamente celdas de esta rejilla, todas con `[cdkDragData]` de ese tipo. Si
   * algún día se conecta otra fuente de arrastre (una paleta lateral, otra
   * rejilla), el cast pasa a ser mentira y falla en runtime sin que el
   * compilador avise: entonces habrá que discriminar el tipo del `data`.
   */
  protected alSoltar(evento: CdkDragDrop<{ dia: number; orden: number }>, dia: number, orden: number): void {
    const inst = evento.item.data as InstanciaCelda;
    const origen = inst.entradas[0];
    if (origen.dia === dia && origen.tramo === orden) {
      return;
    }
    this.soltar.emit({ actividadCodigo: inst.actividadCodigo, indice: inst.indice, dia, orden });
  }
}
