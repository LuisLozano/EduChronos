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
 * Lo que el contenedor pasa por `data` (S203, salda D-aviso-fuera-del-dialogo): los hallazgos
 * de la pre-validación ya repartidos por severidad. Las dos listas pueden llegar vacías.
 */
export interface DatosConfirmarGeneracion {
  /** Severidad ERROR: el servidor rechazará la generación. Deciden el rótulo del botón. */
  errores: AvisoPrevalidacion[];
  /** Severidad AVISO: se enseñan, pero no impiden generar ni cambian el botón. */
  avisos: AvisoPrevalidacion[];
}

/**
 * Diálogo de confirmación de TODA generación (Fase 8, gesto de generar; ensanchado
 * en S145). Presentacional puro sobre `@angular/cdk/dialog`: NO habla con ningún
 * servicio ni con el backend —el contenedor le pasa por `data` los hallazgos de la
 * pre-validación repartidos en errores y avisos ({@link DatosConfirmarGeneracion}) y él los
 * enumera—.
 *
 * <p>Lo que confirma es el COSTE —el tiempo de cálculo, sustituye el trabajo en curso,
 * no se deshace—, que existe con o sin avisos. El tiempo lo elige quien genera, entre
 * {@link MINUTOS_GENERACION} (S184, condición 1 de O-pre-demo). Hasta S144 solo se abría cuando había
 * errores de pre-validación, y sobre un catálogo sano no se abría nunca. Por eso
 * `data` puede llegar VACÍO y la plantilla lo contempla: sin avisos pinta solo el
 * coste, y el botón principal deja de decir «de todos modos», que sin hallazgos no
 * se referiría a nada.
 *
 * <p><b>Avisos (S203).</b> Hasta S203 solo llegaban los ERROR, y los AVISO —S8 y el cuadre de
 * horas— se quedaban en el panel sin que quien genera los viera (D-aviso-fuera-del-dialogo).
 * Ahora tienen su bloque, «Avisos (no impiden generar)», separado del de errores y solo si
 * hay alguno. El rótulo del botón sigue dependiendo SOLO de los errores: un aviso no hace
 * que se genere «de todos modos».
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

  /** Lo que el contenedor pasó por `data`: errores y avisos, ya repartidos. */
  private readonly datos = inject<DatosConfirmarGeneracion>(DIALOG_DATA);

  /**
   * Los hallazgos ERROR; puede ser una lista VACÍA (catálogo sano), que es el caso normal.
   * Solo para enumerar y para el rótulo del botón.
   */
  protected readonly errores = this.datos.errores;

  /** Los hallazgos AVISO; solo para enumerar en su bloque. */
  protected readonly avisos = this.datos.avisos;

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
