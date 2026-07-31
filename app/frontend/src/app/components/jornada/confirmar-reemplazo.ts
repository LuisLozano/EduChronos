import { Component, inject } from '@angular/core';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';

/**
 * Diálogo de confirmación previo a REEMPLAZAR una jornada ya guardada. Presentacional
 * puro sobre `@angular/cdk/dialog`: no habla con ningún servicio: quien lo abre le pasa
 * por `data` las líneas ya compuestas y él las enumera.
 *
 * <p><b>Por qué un componente nuevo y no `ConfirmarGeneracion`.</b> Ese es el precedente
 * correcto en FORMA —confirmación de una acción destructiva que no es un borrado de
 * fila, cierre por boolean, cero servicios—, pero no es reutilizable: su `DIALOG_DATA`
 * está tipado a `AvisoPrevalidacion[]` y su plantilla pinta `regla`/`entidadCodigo`/
 * `demanda`/`disponible`, con el título «Generar pese a la pre-validación». Generalizarlo
 * obligaría a tocar `horario-view`, que está cerrado (D-F8.6, H1). Se calca, no se
 * comparte, igual que el helper `mensaje()`.
 *
 * <p>Tampoco es `ConfirmarBorrado`: ese sí es genérico (recibe `string[]`), pero su botón
 * dice «Borrar» y aquí no se borra nada, se sustituye una malla por otra.
 *
 * <p>Cierra con `true` (reemplazar) o `undefined` (cancelar / backdrop / Escape). Quien
 * lo abre trata como cancelar todo lo que no sea `=== true`.
 */
@Component({
  selector: 'app-confirmar-reemplazo',
  templateUrl: './confirmar-reemplazo.html',
  styleUrl: './confirmar-reemplazo.css',
})
export class ConfirmarReemplazo {
  private readonly ref = inject<DialogRef<boolean>>(DialogRef);

  /** Líneas a mostrar, ya compuestas por quien abre el diálogo. */
  protected readonly lineas = inject<string[]>(DIALOG_DATA);

  protected confirmar(): void {
    this.ref.close(true);
  }

  protected cancelar(): void {
    this.ref.close();
  }
}
