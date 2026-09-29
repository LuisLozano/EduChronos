import { Component, inject, signal } from '@angular/core';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';

import { AvisoPrevalidacion } from '../../models/prevalidacion.model';

/**
 * Presupuestos que se ofrecen, en minutos (S184, decisión N de O-pre-demo). Opciones
 * FIJAS y no un campo libre: el máximo de 60 se verifica en la máquina virtual, y un valor
 * que nadie ha medido no se ofrece.
 */
export const MINUTOS_GENERACION: readonly number[] = [10, 20, 30, 60];

/** Opción marcada al abrir el diálogo. */
export const MINUTOS_POR_DEFECTO = 10;

/**
 * Diálogo de confirmación de TODA generación (Fase 8, gesto de generar; ensanchado
 * en S145). Presentacional puro sobre `@angular/cdk/dialog`: NO habla con ningún
 * servicio ni con el backend —el contenedor le pasa por `data` los avisos de
 * severidad ERROR y él los enumera—.
 *
 * <p>Lo que confirma es el COSTE —el tiempo de cálculo, sustituye el trabajo en curso,
 * no se deshace—, que existe con o sin avisos. El tiempo lo elige quien genera, entre
 * {@link MINUTOS_GENERACION} (S184, condición 1 de O-pre-demo). Hasta S144 solo se abría cuando había
 * errores de pre-validación, y sobre un catálogo sano no se abría nunca. Por eso
 * `data` puede llegar VACÍO y la plantilla lo contempla: sin avisos pinta solo el
 * coste, y el botón principal deja de decir «de todos modos», que sin hallazgos no
 * se referiría a nada.
 *
 * <p>Cierra por {@link DialogRef#close} con los MINUTOS elegidos al confirmar y sin
 * valor al cancelar. Cancelar, backdrop y Escape emiten `undefined` en `closed`, que el
 * contenedor trata como no generar: sólo un número lanza la generación.
 *
 * <p>El CDK aporta contenedor y overlay pero NINGÚN estilo: la caja, el fondo y la
 * sombra los pone el CSS de este componente (ver `.confirmar-generacion`).
 */
@Component({
  selector: 'app-confirmar-generacion',
  templateUrl: './confirmar-generacion.html',
  styleUrl: './confirmar-generacion.css',
})
export class ConfirmarGeneracion {
  /** Ref al diálogo; se cierra con los minutos elegidos, o sin valor al cancelar. */
  private readonly ref = inject<DialogRef<number>>(DialogRef);

  /**
   * Los avisos ERROR que el contenedor pasó por `data`; puede ser una lista VACÍA
   * (catálogo sano), que es el caso normal. Solo para enumerar.
   */
  protected readonly errores = inject<AvisoPrevalidacion[]>(DIALOG_DATA);

  /** Las opciones que pinta la plantilla, en su orden. */
  protected readonly opciones = MINUTOS_GENERACION;

  /** Presupuesto elegido, en minutos. */
  protected readonly minutos = signal(MINUTOS_POR_DEFECTO);

  protected confirmar(): void {
    this.ref.close(this.minutos());
  }

  protected cancelar(): void {
    this.ref.close();
  }
}
