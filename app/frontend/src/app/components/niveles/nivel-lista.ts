import { Component, OnInit, inject, signal } from '@angular/core';
import { Dialog } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { NivelService } from '../../services/nivel.service';
import { Nivel } from '../../models/nivel.model';
import { NivelForm } from './nivel-form';
import { ConfirmarBorrado } from '../confirmar-borrado/confirmar-borrado';

/**
 * Lista del catálogo de niveles: carga en init, tabla con acciones por fila,
 * alta/edición en diálogo, borrado con confirmación previa. La escritura vive en
 * `NivelForm` (diálogo); esta lista lo abre y recarga tras un guardado.
 *
 * <p>NO ORDENA EN CLIENTE, y aquí no es solo regla de molde: el backend sirve la
 * lista ordenada por `orden` (D-1), que es el orden pedagógico y no el alfabético.
 * Reordenar por `codigo` pondría 1BACH antes que 1ESO y destruiría el único criterio
 * que el campo `orden` existe para expresar.
 */
@Component({
  selector: 'app-nivel-lista',
  templateUrl: './nivel-lista.html',
  styleUrl: './nivel-lista.css',
})
export class NivelLista implements OnInit {
  private readonly service = inject(NivelService);
  private readonly dialog = inject(Dialog);

  protected readonly niveles = signal<Nivel[]>([]);
  protected readonly cargando = signal(false);
  /** Error de la última operación de lista o borrado. Vacío = sin error. */
  protected readonly error = signal('');

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set('');
    this.service.listar().subscribe({
      next: (lista) => {
        this.niveles.set(lista);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, 'No se pudo cargar la lista de niveles'));
        this.cargando.set(false);
      },
    });
  }

  protected nuevo(): void {
    this.abrirForm(null);
  }

  protected editar(nivel: Nivel): void {
    this.abrirForm(nivel);
  }

  /** Abre el formulario en diálogo; recarga si se guardó (cierre con `true`). */
  private abrirForm(nivel: Nivel | null): void {
    this.dialog
      .open<boolean, Nivel | null>(NivelForm, { data: nivel })
      .closed.subscribe((guardado) => {
        // backdrop y Escape emiten undefined: solo `true` estricto recarga.
        if (guardado === true) {
          this.cargar();
        }
      });
  }

  protected borrar(nivel: Nivel): void {
    const lineas = [`¿Borrar el nivel ${nivel.codigo}?`];
    this.dialog
      .open<boolean, string[]>(ConfirmarBorrado, { data: lineas })
      .closed.subscribe((confirmado) => {
        if (confirmado === true) {
          this.confirmarBorrado(nivel);
        }
      });
  }

  private confirmarBorrado(nivel: Nivel): void {
    this.error.set('');
    this.service.borrar(nivel.id).subscribe({
      next: () => this.cargar(),
      error: (err: HttpErrorResponse) => {
        // 409 = grupos apuntando al nivel (único referente, verificado por FK en
        // S111). El backend compone "No se puede borrar: referenciada por N grupo(s)".
        this.error.set(this.mensaje(err, `No se pudo borrar el nivel ${nivel.codigo}`));
      },
    });
  }

  /**
   * Traduce error Http a texto de usuario: mensaje del servidor primero
   * (`message`, luego `error`), degradado con status si no hay. Copiado del
   * patrón de `horario-view.mensaje()` con texto propio; NO extraído a utilidad
   * compartida a propósito: hacerlo tocaría horario-view (D-F8.6, H1 cerrado).
   */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
