import { Component, OnInit, inject, signal } from '@angular/core';
import { Dialog } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { ProfesorService } from '../../services/profesor.service';
import { Profesor } from '../../models/profesor.model';
import { ProfesorForm } from './profesor-form';
import { ConfirmarBorrado } from '../confirmar-borrado/confirmar-borrado';

/**
 * Lista del catálogo de profesores: carga en init, tabla con acciones por fila,
 * alta/edición en diálogo, borrado con confirmación previa. La escritura vive en
 * `ProfesorForm` (diálogo); esta lista lo abre y recarga tras un guardado.
 */
@Component({
  selector: 'app-profesor-lista',
  templateUrl: './profesor-lista.html',
  styleUrl: './profesor-lista.css',
})
export class ProfesorLista implements OnInit {
  private readonly service = inject(ProfesorService);
  private readonly dialog = inject(Dialog);

  protected readonly profesores = signal<Profesor[]>([]);
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
        this.profesores.set(lista);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, 'No se pudo cargar la lista de profesores'));
        this.cargando.set(false);
      },
    });
  }

  protected nuevo(): void {
    this.abrirForm(null);
  }

  protected editar(p: Profesor): void {
    this.abrirForm(p);
  }

  /** Abre el formulario en diálogo; recarga si se guardó (cierre con `true`). */
  private abrirForm(profesor: Profesor | null): void {
    this.dialog
      .open<boolean, Profesor | null>(ProfesorForm, { data: profesor })
      .closed.subscribe((guardado) => {
        // backdrop y Escape emiten undefined: solo `true` estricto recarga.
        if (guardado === true) {
          this.cargar();
        }
      });
  }

  protected borrar(p: Profesor): void {
    const lineas = [`¿Borrar a ${p.nombreCompleto} (${p.codigo})?`];
    this.dialog
      .open<boolean, string[]>(ConfirmarBorrado, { data: lineas })
      .closed.subscribe((confirmado) => {
        if (confirmado === true) {
          this.confirmarBorrado(p);
        }
      });
  }

  private confirmarBorrado(p: Profesor): void {
    this.error.set('');
    this.service.borrar(p.id).subscribe({
      next: () => this.cargar(),
      error: (err: HttpErrorResponse) => {
        // 409 = referencias entrantes. El backend compone el texto rico
        // ("No se puede borrar: referenciada por N plaza(s)…") y viaja en
        // `message` porque server.error.include-message=always. El degradado,
        // si no viajara, dice al menos qué pasó y con qué status.
        this.error.set(this.mensaje(err, `No se pudo borrar a ${p.nombreCompleto}`));
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
