import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Dialog } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { ProfesorService } from '../../services/profesor.service';
import { Profesor } from '../../models/profesor.model';
import { ProfesorForm } from './profesor-form';
import { ConfirmarBorrado } from '../confirmar-borrado/confirmar-borrado';
import { CabeceraLista } from '../cabecera-lista/cabecera-lista';
import { EstadoLista } from '../estado-lista/estado-lista';
import { coincide } from '../../catalogo/busqueda';

/**
 * Lista del catálogo de profesores: carga en init, tabla con acciones por fila,
 * alta/edición en diálogo, borrado con confirmación previa. La escritura vive en
 * `ProfesorForm` (diálogo); esta lista lo abre y recarga tras un guardado.
 */
@Component({
  selector: 'app-profesor-lista',
  imports: [CabeceraLista, EstadoLista],
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

  /** Texto escrito en la caja de la cabecera. Vacío = se ven todas las filas. */
  protected readonly busqueda = signal('');

  /**
   * Texto de UNA fila para buscar en él, compuesto con los campos QUE LA TABLA
   * PINTA y con las mismas expresiones que usa la plantilla. La regla es esa: si
   * la fila lo enseña, la búsqueda lo encuentra; si no lo enseña, no.
   *
   * <p><b>Nada vigila que siga a la plantilla.</b> Añadir una columna a la tabla y
   * olvidarla aquí no rompe ningún test ni da error de compilación: deja una
   * columna visible por la que no se puede buscar, en silencio. Es la limitación
   * ACEPTADA del Cambio (S124), y el sitio donde mirar cuando alguien diga que la
   * búsqueda «no encuentra» algo que está en pantalla.
   */
  protected textoFila(p: Profesor): string {
    return [p.codigo, p.nombreCompleto].join(' ');
  }

  /**
   * Filas que casan con la consulta. `filter` es el método del array, no un
   * nombre nuestro. Con la búsqueda vacía devuelve todas —{@link coincide} da
   * `true` sin consulta—, así que la lista arranca completa.
   */
  protected readonly visibles = computed(() =>
    this.profesores().filter((f) => coincide(this.textoFila(f), this.busqueda())),
  );

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
