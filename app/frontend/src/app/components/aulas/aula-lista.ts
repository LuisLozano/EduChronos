import { Component, OnInit, inject, signal } from '@angular/core';
import { Dialog } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { AulaService } from '../../services/aula.service';
import { Aula } from '../../models/aula.model';
import { AulaForm } from './aula-form';
import { ConfirmarBorrado } from '../confirmar-borrado/confirmar-borrado';

/**
 * Lista del catálogo de aulas: carga en init, tabla con acciones por fila,
 * alta/edición en diálogo, borrado con confirmación previa. Calco del molde que
 * fijó `ProfesorLista` (S101), incluido el detalle de que el ORDEN NO se hace aquí:
 * `AulaService.listar()` del backend ya devuelve ordenado por código
 * (`Comparator.comparing(Aula::getCodigo)`), y reordenar en el cliente duplicaría
 * esa regla en dos sitios.
 *
 * <p>La escritura vive en `AulaForm` (diálogo); esta lista lo abre y recarga tras un
 * guardado. La confirmación de borrado reutiliza el genérico `ConfirmarBorrado`, que
 * recibe LÍNEAS de texto (`string[]`) ya compuestas por quien lo abre.
 */
@Component({
  selector: 'app-aula-lista',
  templateUrl: './aula-lista.html',
  styleUrl: './aula-lista.css',
})
export class AulaLista implements OnInit {
  private readonly service = inject(AulaService);
  private readonly dialog = inject(Dialog);

  protected readonly aulas = signal<Aula[]>([]);
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
        this.aulas.set(lista);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, 'No se pudo cargar la lista de aulas'));
        this.cargando.set(false);
      },
    });
  }

  protected nueva(): void {
    this.abrirForm(null);
  }

  protected editar(a: Aula): void {
    this.abrirForm(a);
  }

  /** Abre el formulario en diálogo; recarga si se guardó (cierre con `true`). */
  private abrirForm(aula: Aula | null): void {
    this.dialog
      .open<boolean, Aula | null>(AulaForm, { data: aula })
      .closed.subscribe((guardado) => {
        // backdrop y Escape emiten undefined: solo `true` estricto recarga.
        if (guardado === true) {
          this.cargar();
        }
      });
  }

  protected borrar(a: Aula): void {
    const lineas = [`¿Borrar el aula ${a.codigo} (${a.tipo})?`];
    this.dialog
      .open<boolean, string[]>(ConfirmarBorrado, { data: lineas })
      .closed.subscribe((confirmado) => {
        if (confirmado === true) {
          this.confirmarBorrado(a);
        }
      });
  }

  private confirmarBorrado(a: Aula): void {
    this.error.set('');
    this.service.borrar(a.id).subscribe({
      next: () => this.cargar(),
      error: (err: HttpErrorResponse) => {
        // 409 = referencias entrantes. El backend compone el texto rico
        // ("No se puede borrar: referenciada por N plaza(s)…") con las CUATRO FK
        // que consulta antes del delete, y viaja en `message` porque
        // server.error.include-message=always. El degradado, si no viajara, dice
        // al menos qué pasó y con qué status.
        this.error.set(this.mensaje(err, `No se pudo borrar el aula ${a.codigo}`));
      },
    });
  }

  /**
   * Traduce error Http a texto de usuario: mensaje del servidor primero
   * (`message`, luego `error`), degradado con status si no hay. Copiado del patrón
   * de `profesor-lista.mensaje()` con texto propio; NO extraído a utilidad
   * compartida a propósito: hacerlo tocaría horario-view (D-F8.6, H1 cerrado).
   */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
