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
 * <p><b>GUARDA DE MULTIPLAZA (protección de datos, no cosmética).</b> El formulario de
 * este trozo envía UNA plaza, y la reconciliación del PUT es POSICIONAL sobre el estado
 * deseado completo: mandar una plaza a una actividad que tiene tres BORRARÍA las otras
 * dos sin preguntar. Así que mientras el editor de lista variable de plazas no exista
 * (trozo B), el botón de editar va DESHABILITADO en toda actividad con más de una plaza,
 * y la fila lo dice. El ALTA sigue disponible: nace con una plaza y no destruye nada.
 *
 * <p>El BORRADO sí se permite en multiplaza: borrar es íntegro, no parcial —se lleva la
 * actividad con todas sus plazas por cascade, que es exactamente lo que el usuario pide—
 * y el backend lo protege con su propio 409 si alguien las retiene.
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

  /**
   * Predicado de la guarda: `true` si la actividad tiene más de una plaza y por tanto no
   * puede editarse con el formulario de una plaza. La plantilla lo usa para deshabilitar
   * el botón Y para pintar el aviso de la fila; vive aquí y no duplicado en el HTML.
   */
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

  /**
   * Abre el formulario de edición. La guarda se comprueba TAMBIÉN aquí y no solo en el
   * `[disabled]` del botón: el atributo es presentación, y esta es la puerta real por la
   * que se llega al PUT destructivo.
   */
  protected editar(actividad: Actividad): void {
    if (this.esMultiplaza(actividad)) {
      return;
    }
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
