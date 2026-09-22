import { Component, OnInit, inject, signal } from '@angular/core';
import { Dialog } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';

import { CursoService } from '../../services/curso.service';
import { CursoDTO } from '../../models/curso.model';
import { CursosDialogo } from '../cursos-dialogo/cursos-dialogo';

/** Lo que se enseña cuando la base abierta no trae nombre de curso (condición 6). */
const SIN_NOMBRE = 'Curso sin nombre';

/** Lo que se enseña cuando el GET falla: se dice que no se sabe, no se inventa un curso. */
const NO_DISPONIBLE = 'Curso: no disponible';

/**
 * El curso abierto en la barra de aplicación, y la puerta al selector (O-curso, S160,
 * C-selector-curso fase B).
 *
 * <p>Ocupa el hueco que S127 reservó en `app.html` para el criterio 1 de O-navegación. Ese
 * hueco reserva una ALTURA medida —ver `.app__curso` en `app.css`—, no espacio a la derecha:
 * el compromiso es que lo que entre aquí quepa en los 27 px de la línea de la marca sin
 * engordar la barra. Por eso el control es texto más un botón compacto, y no un `<select>`,
 * que con borde y padding ronda los 30 px y obligaría a recontar las marcas de la rejilla.
 *
 * <p><b>Tres cosas que esta barra NO hace.</b> No lista cursos —eso es del diálogo, que los
 * pide cuando se abre—, no recarga la página, y no vuelve a consultar tras un cambio: si el
 * curso cambia, la aplicación entera se recarga y esta barra nace otra vez. Lo único que
 * conserva es el `CursoDTO`, que le pasa al diálogo para que los dos hablen del mismo curso
 * abierto.
 *
 * <p><b>Un fallo del GET no rompe la barra.</b> Se enseña «Curso: no disponible» con el
 * motivo en el `title`, y la navegación sigue entera: el nombre del curso es información, no
 * un requisito para usar el programa. La alternativa —dejar el hueco vacío— haría creer que
 * la aplicación no tiene selector.
 */
@Component({
  selector: 'app-curso-barra',
  templateUrl: './curso-barra.html',
  styleUrl: './curso-barra.css',
})
export class CursoBarra implements OnInit {
  private readonly service = inject(CursoService);
  private readonly dialog = inject(Dialog);

  /** El curso abierto; `null` mientras carga o si la consulta falló. */
  protected readonly curso = signal<CursoDTO | null>(null);

  /** Motivo del fallo, para el `title`. Vacío = sin fallo. */
  protected readonly error = signal('');

  ngOnInit(): void {
    this.service.obtener().subscribe({
      next: (curso) => this.curso.set(curso),
      error: (err: HttpErrorResponse) =>
        this.error.set(this.mensaje(err, 'No se pudo consultar el curso abierto')),
    });
  }

  /**
   * Lo que se lee en la barra. Tres casos, y ninguno es la cadena vacía: un hueco mudo no
   * distingue «aún no ha llegado» de «no hay curso».
   */
  protected texto(): string {
    if (this.error()) {
      return NO_DISPONIBLE;
    }
    const curso = this.curso();
    if (curso === null) {
      return '';
    }
    return curso.nombre ?? SIN_NOMBRE;
  }

  /** ¿Se pinta la marca de solo lectura? Sólo con un curso cargado y archivado. */
  protected esArchivado(): boolean {
    return this.curso()?.archivado === true;
  }

  /**
   * La explicación de la marca, para `title` y `aria-label`. Es el par completo del molde de
   * `horario-grid`: el `title` solo sería mudo para un lector de pantalla, y la regla de
   * severidad de `styles.css` exige que el dato no viaje únicamente por color.
   */
  protected explicacionArchivado(): string {
    const nombre = this.curso()?.nombre ?? 'Este curso';
    return `${nombre} está archivado: se puede consultar, pero no admite cambios.`;
  }

  /**
   * Abre el selector, pasándole el curso que ESTA barra está enseñando. No se vuelve a pedir
   * dentro del diálogo: dos consultas podrían dar dos respuestas y la pantalla se
   * contradiría consigo misma.
   */
  protected abrirDialogo(): void {
    const curso = this.curso();
    if (curso === null) {
      return;
    }
    this.dialog.open<void, CursoDTO>(CursosDialogo, { data: curso });
  }

  /**
   * Traduce error Http a texto de usuario. Mismo patrón copiado que en los veinte
   * componentes que ya lo tienen (D-F8.6, cerrado).
   */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
