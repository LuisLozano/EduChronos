import { CdkDragDrop, DragDropModule } from '@angular/cdk/drag-drop';
import { Component, computed, input, output } from '@angular/core';

import { SesionVista } from '../../models/horario.model';
import { DIAS, InstanciaCelda, TRAMOS, agruparPorActividad, claveSlot } from '../../horario/proyeccion';
import { clavePin } from '../../horario/pines';

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
