import { Component, OnInit, inject, signal } from '@angular/core';
import { Dialog } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { GrupoService } from '../../services/grupo.service';
import { Grupo } from '../../models/grupo.model';
import { GrupoForm } from './grupo-form';
import { ConfirmarBorrado } from '../confirmar-borrado/confirmar-borrado';

/**
 * Lista del catálogo de grupos administrativos: carga en init, tabla con acciones por
 * fila, alta/edición en diálogo, borrado con confirmación previa. La escritura vive en
 * `GrupoForm` (diálogo); esta lista lo abre y recarga tras un guardado.
 *
 * <p>DOS COLUMNAS, TRES CAMPOS EN EL DTO. La tabla muestra Código y Nivel, y OMITE
 * `tipo`. No es un olvido: el backend restringe este flujo a `ORDINARIO` por lista
 * blanca (D-nueva-2), así que la columna llevaría el mismo valor en todas las filas
 * —ruido que ocupa ancho y no distingue una fila de otra—. Si algún día esta pantalla
 * pasara a listar también PDC o virtuales de optativa, `tipo` volvería a discriminar y
 * la columna tendría sentido; hoy no.
 *
 * <p>El 409 de borrado sí es rico aquí: un grupo con subgrupos o con hijos PDC no se
 * borra, y el backend nombra cuántos de cada tipo lo impiden.
 */
@Component({
  selector: 'app-grupo-lista',
  templateUrl: './grupo-lista.html',
  styleUrl: './grupo-lista.css',
})
export class GrupoLista implements OnInit {
  private readonly service = inject(GrupoService);
  private readonly dialog = inject(Dialog);

  protected readonly grupos = signal<Grupo[]>([]);
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
        this.grupos.set(lista);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, 'No se pudo cargar la lista de grupos'));
        this.cargando.set(false);
      },
    });
  }

  protected nuevo(): void {
    this.abrirForm(null);
  }

  protected editar(grupo: Grupo): void {
    this.abrirForm(grupo);
  }

  /** Abre el formulario en diálogo; recarga si se guardó (cierre con `true`). */
  private abrirForm(grupo: Grupo | null): void {
    this.dialog
      .open<boolean, Grupo | null>(GrupoForm, { data: grupo })
      .closed.subscribe((guardado) => {
        // backdrop y Escape emiten undefined: solo `true` estricto recarga.
        if (guardado === true) {
          this.cargar();
        }
      });
  }

  protected borrar(grupo: Grupo): void {
    const lineas = [`¿Borrar el grupo ${grupo.codigo}?`];
    this.dialog
      .open<boolean, string[]>(ConfirmarBorrado, { data: lineas })
      .closed.subscribe((confirmado) => {
        if (confirmado === true) {
          this.confirmarBorrado(grupo);
        }
      });
  }

  private confirmarBorrado(grupo: Grupo): void {
    this.error.set('');
    this.service.borrar(grupo.id).subscribe({
      next: () => this.cargar(),
      error: (err: HttpErrorResponse) => {
        // 409 = referencias entrantes. El backend compone el texto rico
        // ("No se puede borrar: referenciada por 2 subgrupo(s), 1 grupo(s) hijo(s)") y
        // viaja en `message` porque server.error.include-message=always. El degradado,
        // si no viajara, dice al menos qué pasó y con qué status.
        this.error.set(this.mensaje(err, `No se pudo borrar el grupo ${grupo.codigo}`));
      },
    });
  }

  /**
   * Traduce error Http a texto de usuario: mensaje del servidor primero
   * (`message`, luego `error`), degradado con status si no hay. Copiado del
   * patrón de `asignatura-lista.mensaje()` con texto propio; NO extraído a utilidad
   * compartida a propósito: hacerlo tocaría horario-view (D-F8.6, H1 cerrado).
   */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
