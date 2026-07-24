import { Component, inject } from '@angular/core';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';

import { AvisoPrevalidacion } from '../../models/prevalidacion.model';

/**
 * Diálogo de confirmación previo a una generación que la pre-validación ya sabe
 * condenada (Fase 8, gesto de generar). Presentacional puro sobre
 * `@angular/cdk/dialog`: NO habla con ningún servicio ni con el backend —el
 * contenedor le pasa por `data` los avisos de severidad ERROR y él los enumera—.
 *
 * <p>Cierra con un boolean por {@link DialogRef#close}: `true` = generar de todos
 * modos, `false` = cancelar. Cerrar por backdrop o Escape emite `undefined` en
 * `closed`, que el contenedor trata como cancelar (nunca `=== true`).
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
  /** Ref al diálogo; se cierra con el boolean de la acción elegida. */
  private readonly ref = inject<DialogRef<boolean>>(DialogRef);

  /** Los avisos ERROR que el contenedor pasó por `data`. Solo para enumerar. */
  protected readonly errores = inject<AvisoPrevalidacion[]>(DIALOG_DATA);

  protected confirmar(): void {
    this.ref.close(true);
  }

  protected cancelar(): void {
    this.ref.close(false);
  }
}
