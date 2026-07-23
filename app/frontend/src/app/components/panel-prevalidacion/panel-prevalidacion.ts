import { Component, computed, input, linkedSignal } from '@angular/core';

import { AvisoPrevalidacion } from '../../models/prevalidacion.model';

/**
 * Panel presentacional de la pre-validación del catálogo (Fase 8, Bloque 8.4-B1).
 * CUATRO ramas con clases DISJUNTAS por DOM, gateadas por dos inputs; sin hablar
 * con ningún servicio —el contenedor le pasa `avisos` y `error`—:
 * <ul>
 *   <li>{@link error} no nulo → `.prevalidacion-error` (prioridad sobre todo);</li>
 *   <li>{@link avisos} `null` → `.prevalidacion-pendiente` (NO ejecutado);</li>
 *   <li>{@link avisos} `[]` → `.prevalidacion-limpia` (ejecutado y sano);</li>
 *   <li>hay avisos → `.prevalidacion-panel` con cabecera y cuerpo colapsable.</li>
 * </ul>
 */
@Component({
  selector: 'app-panel-prevalidacion',
  templateUrl: './panel-prevalidacion.html',
  styleUrl: './panel-prevalidacion.css',
})
export class PanelPrevalidacion {
  /**
   * Hallazgos a pintar. `null` = NO EJECUTADO (aún no llegó la respuesta); `[]` =
   * ejecutado y catálogo sano. Esa distinción es el eje del panel: separa la rama
   * `.prevalidacion-pendiente` de la `.prevalidacion-limpia`. INICIAL null.
   */
  readonly avisos = input<AvisoPrevalidacion[] | null>(null);
  /** Fallo de la carga; su rama `.prevalidacion-error` tiene prioridad sobre las demás. */
  readonly error = input<string | null>(null);

  /** Nº de hallazgos de severidad ERROR. */
  protected readonly numErrores = computed(
    () => (this.avisos() ?? []).filter((a) => a.severidad === 'ERROR').length,
  );
  /**
   * Nº del RESTO (no-ERROR), NO la longitud total: un hallazgo ERROR no se cuenta
   * aquí. Con dos valores de severidad hoy esto es «los AVISO», pero se filtra por
   * `!== 'ERROR'` para que un tercer valor futuro caiga de este lado, no se pierda.
   */
  protected readonly numAvisos = computed(
    () => (this.avisos() ?? []).filter((a) => a.severidad !== 'ERROR').length,
  );

  /** Hay al menos un ERROR: gobierna el estado inicial del colapso. */
  protected readonly hayError = computed(() => this.numErrores() > 0);

  /**
   * Colapso del CUERPO (la cabecera con los contadores SIEMPRE se ve). Inicial
   * colapsado salvo que haya algún ERROR —un ERROR aborta la generación, así que
   * se muestra desplegado sin pedir clic—. `linkedSignal` para que el usuario
   * pueda alternarlo con el botón y aun así se recomponga si cambian los avisos.
   */
  protected readonly colapsado = linkedSignal(() => !this.hayError());

  protected alternar(): void {
    this.colapsado.set(!this.colapsado());
  }
}
