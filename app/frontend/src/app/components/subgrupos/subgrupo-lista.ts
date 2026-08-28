import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Dialog } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { SubgrupoService } from '../../services/subgrupo.service';
import { Subgrupo } from '../../models/subgrupo.model';
import { SubgrupoForm } from './subgrupo-form';
import { ConfirmarBorrado } from '../confirmar-borrado/confirmar-borrado';
import { CabeceraLista } from '../cabecera-lista/cabecera-lista';
import { coincide } from '../../catalogo/busqueda';

/**
 * Lista del catálogo de subgrupos de alumnos: carga en init, tabla con acciones por
 * fila, alta/edición en diálogo, borrado con confirmación previa. La escritura vive en
 * `SubgrupoForm` (diálogo); esta lista lo abre y recarga tras un guardado.
 *
 * <p>DOS COLUMNAS: Código y Grupos. `grupos` es un `string[]` (los códigos de la
 * población del subgrupo) y se pinta unido por comas —texto plano, sin regla CSS
 * propia—. La UX rica del listado de grupos (chips, truncado) se aplaza a la fase de
 * mejora de UX de subgrupos, decisión consciente del arquitecto.
 *
 * <p>El 409 de borrado se da cuando el subgrupo está referenciado por plazas; el
 * backend nombra cuántas. Mismo camino que el resto del catálogo.
 */
@Component({
  selector: 'app-subgrupo-lista',
  imports: [CabeceraLista],
  templateUrl: './subgrupo-lista.html',
  styleUrl: './subgrupo-lista.css',
})
export class SubgrupoLista implements OnInit {
  private readonly service = inject(SubgrupoService);
  private readonly dialog = inject(Dialog);

  protected readonly subgrupos = signal<Subgrupo[]>([]);
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
  protected textoFila(subgrupo: Subgrupo): string {
    return [subgrupo.codigo, subgrupo.grupos.join(', ')].join(' ');
  }

  /**
   * Filas que casan con la consulta. `filter` es el método del array, no un
   * nombre nuestro. Con la búsqueda vacía devuelve todas —{@link coincide} da
   * `true` sin consulta—, así que la lista arranca completa.
   */
  protected readonly visibles = computed(() =>
    this.subgrupos().filter((f) => coincide(this.textoFila(f), this.busqueda())),
  );

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set('');
    this.service.listar().subscribe({
      next: (lista) => {
        this.subgrupos.set(lista);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, 'No se pudo cargar la lista de subgrupos'));
        this.cargando.set(false);
      },
    });
  }

  protected nuevo(): void {
    this.abrirForm(null);
  }

  protected editar(subgrupo: Subgrupo): void {
    this.abrirForm(subgrupo);
  }

  /** Abre el formulario en diálogo; recarga si se guardó (cierre con `true`). */
  private abrirForm(subgrupo: Subgrupo | null): void {
    this.dialog
      .open<boolean, Subgrupo | null>(SubgrupoForm, { data: subgrupo })
      .closed.subscribe((guardado) => {
        // backdrop y Escape emiten undefined: solo `true` estricto recarga.
        if (guardado === true) {
          this.cargar();
        }
      });
  }

  protected borrar(subgrupo: Subgrupo): void {
    const lineas = [`¿Borrar el subgrupo ${subgrupo.codigo}?`];
    this.dialog
      .open<boolean, string[]>(ConfirmarBorrado, { data: lineas })
      .closed.subscribe((confirmado) => {
        if (confirmado === true) {
          this.confirmarBorrado(subgrupo);
        }
      });
  }

  private confirmarBorrado(subgrupo: Subgrupo): void {
    this.error.set('');
    this.service.borrar(subgrupo.id).subscribe({
      next: () => this.cargar(),
      error: (err: HttpErrorResponse) => {
        // 409 = referencias entrantes (plazas). El backend compone el texto rico y
        // viaja en `message`. El degradado, si no viajara, dice qué pasó y con qué status.
        this.error.set(this.mensaje(err, `No se pudo borrar el subgrupo ${subgrupo.codigo}`));
      },
    });
  }

  /**
   * Traduce error Http a texto de usuario: mensaje del servidor primero
   * (`message`, luego `error`), degradado con status si no hay. Copiado del patrón
   * de `grupo-lista.mensaje()` con texto propio; NO extraído a utilidad compartida a
   * propósito: hacerlo tocaría horario-view (D-F8.6, H1 cerrado).
   */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
