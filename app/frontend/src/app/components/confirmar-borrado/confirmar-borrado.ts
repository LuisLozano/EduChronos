import { Component, inject } from '@angular/core';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';

/**
 * Diálogo de confirmación de borrado, genérico. Hace UNA pregunta y cierra con
 * `true` (confirmar) o `undefined` (cancelar / backdrop / Escape).
 *
 * NO enumera referentes: el 409 con "referenciada por N plaza(s)…" solo se
 * conoce DESPUÉS del DELETE, así que ese texto es un error en la lista, no
 * contenido de este diálogo (mismo criterio que `errorPin` en horario-view).
 * Este diálogo es solo el gesto previo "¿seguro?". Nace con profesores pero es
 * el molde de confirmación de las 4 entidades de catálogo (candidato a molde,
 * a validar en aulas).
 */
@Component({
  selector: 'app-confirmar-borrado',
  templateUrl: './confirmar-borrado.html',
  styleUrl: './confirmar-borrado.css',
})
export class ConfirmarBorrado {
  protected readonly ref = inject<DialogRef<boolean>>(DialogRef);
  /** Línea(s) a mostrar: p. ej. ["¿Borrar a Ana Ruiz (MAT8)?"]. */
  protected readonly lineas = inject<string[]>(DIALOG_DATA);

  protected confirmar(): void {
    this.ref.close(true);
  }

  protected cancelar(): void {
    this.ref.close();
  }
}
