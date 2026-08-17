import { Component, OnInit, inject, signal } from '@angular/core';
import { Dialog } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { ActividadService } from '../../services/actividad.service';
import { Actividad } from '../../models/actividad.model';
import { ActividadForm } from './actividad-form';
import { ConfirmarBorrado } from '../confirmar-borrado/confirmar-borrado';

/**
 * Lista del catálogo de actividades: carga en init, tabla con acciones por fila,
 * alta/edición en diálogo, borrado con confirmación previa. Molde de `SubgrupoLista`; la
 * escritura vive en {@link ActividadForm}.
 *
 * <p>SEIS COLUMNAS: Código, Asignatura, Patrón, Duración, Repeticiones y N.º de plazas.
 * La asignatura de la actividad es OPCIONAL en el contrato (`null` cuando sus plazas
 * tienen asignaturas distintas), y esa ausencia se pinta como «varias» —el dato dice
 * algo, no falta—.
 *
 * <p><b>La guarda de multiplaza se RETIRÓ en el trozo B.</b> Existió mientras el
 * formulario enviaba una sola plaza: abrir una actividad de tres y guardar habría borrado
 * dos por la reconciliación posicional del PUT. Ahora el formulario edita N plazas, así
 * que toda actividad es editable y el botón no distingue. Lo que protege el PUT
 * destructivo sigue estando donde importa: el 409 del backend ante sesiones o bloqueos.
 *
 * <p>El BORRADO se lleva la actividad con todas sus plazas por cascade, y la confirmación
 * lo dice cuando hay más de una.
 */
@Component({
  selector: 'app-actividad-lista',
  templateUrl: './actividad-lista.html',
  styleUrl: './actividad-lista.css',
})
export class ActividadLista implements OnInit {
  private readonly service = inject(ActividadService);
  private readonly dialog = inject(Dialog);

  protected readonly actividades = signal<Actividad[]>([]);
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
        this.actividades.set(lista);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, 'No se pudo cargar la lista de actividades'));
        this.cargando.set(false);
      },
    });
  }

  /** `true` si la actividad tiene más de una plaza. Ya no gobierna la edición: lo usa la
   *  confirmación de borrado para avisar de cuántas plazas caen. */
  protected esMultiplaza(actividad: Actividad): boolean {
    return actividad.plazas.length > 1;
  }

  /** Asignatura de la actividad, o «varias» cuando es `null` (plazas heterogéneas). */
  protected asignaturaDe(actividad: Actividad): string {
    return actividad.asignatura ?? 'varias';
  }

  protected nuevo(): void {
    this.abrirForm(null);
  }

  protected editar(actividad: Actividad): void {
    this.abrirForm(actividad);
  }

  /** Abre el formulario en diálogo; recarga si se guardó (cierre con `true`). */
  private abrirForm(actividad: Actividad | null): void {
    this.dialog
      .open<boolean, Actividad | null>(ActividadForm, { data: actividad })
      .closed.subscribe((guardado) => {
        // backdrop y Escape emiten undefined: solo `true` estricto recarga.
        if (guardado === true) {
          this.cargar();
        }
      });
  }

  protected borrar(actividad: Actividad): void {
    const lineas = [`¿Borrar la actividad ${actividad.codigo}?`];
    if (this.esMultiplaza(actividad)) {
      lineas.push(`Se borrarán sus ${actividad.plazas.length} plazas.`);
    }
    this.dialog
      .open<boolean, string[]>(ConfirmarBorrado, { data: lineas })
      .closed.subscribe((confirmado) => {
        if (confirmado === true) {
          this.confirmarBorrado(actividad);
        }
      });
  }

  private confirmarBorrado(actividad: Actividad): void {
    this.error.set('');
    this.service.borrar(actividad.id).subscribe({
      next: () => this.cargar(),
      error: (err: HttpErrorResponse) => {
        // 409 = referencias entrantes (sesiones del horario, bloqueos). El backend
        // compone el texto rico con el desglose y viaja en `message`.
        this.error.set(this.mensaje(err, `No se pudo borrar la actividad ${actividad.codigo}`));
      },
    });
  }

  /**
   * Traduce error Http a texto de usuario: mensaje del servidor primero (`message`,
   * luego `error`), degradado con status si no hay. Copiado del patrón de
   * `subgrupo-lista.mensaje()` con texto propio; NO extraído a utilidad compartida a
   * propósito: hacerlo tocaría horario-view (D-F8.6, H1 cerrado).
   */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
