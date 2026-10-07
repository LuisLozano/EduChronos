import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { AsignaturaAulaService } from '../../services/asignatura-aula.service';
import { AulaService } from '../../services/aula.service';
import { Asignatura } from '../../models/asignatura.model';
import { Aula } from '../../models/aula.model';
import { AsignaturaAula, RolAulaAsignatura } from '../../models/asignatura-aula.model';
import { FiltroOpciones } from '../filtro-opciones/filtro-opciones';

/** Los tres estados del diálogo, gobernados por la respuesta del GET. */
type EstadoAulas = 'cargando' | 'listo' | 'error';

/**
 * Diálogo de las AULAS de una asignatura (S206, C-reglas-aulas), colgado de una fila de
 * `AsignaturaLista`. Molde de `TutoriaDialogo`: pide las aulas de la asignatura y el catálogo de
 * aulas, no deriva `'listo'` hasta tener las dos, y guarda con un PUT de reemplazo total.
 *
 * <p><b>Una marca para toda la lista.</b> El backend no admite que una asignatura mezcle aulas
 * `EXCLUSIVA` y `PREFERIDA` (400), así que la pantalla no pone la marca fila a fila: la elige una
 * vez y la aplica a todas al guardar. Por defecto, `PREFERIDA`. Si el GET trae roles mezclados
 * —solo posible escribiendo por la API—, el diálogo lo dice, deja la marca sin elegir y no
 * ofrece guardar hasta que se elija.
 *
 * <p>Añadir y quitar trabajan sobre la lista en memoria; nada se escribe hasta «Guardar». El
 * selector de añadir ofrece las aulas que aún no están, con el filtro de S185. Lista vacía es
 * válida: es como se quitan todas.
 *
 * <p>Cierre: `true` solo tras escribir; «Cancelar» cierra con `false`. Un error de escritura se
 * presenta con su mensaje y el diálogo sigue abierto.
 */
@Component({
  selector: 'app-aulas-asignatura-dialogo',
  imports: [ReactiveFormsModule, FiltroOpciones],
  templateUrl: './aulas-asignatura-dialogo.html',
  styleUrl: './aulas-asignatura-dialogo.css',
})
export class AulasAsignaturaDialogo implements OnInit {
  private readonly service = inject(AsignaturaAulaService);
  private readonly aulaService = inject(AulaService);
  private readonly fb = inject(FormBuilder);
  protected readonly ref = inject<DialogRef<boolean>>(DialogRef);

  /** La asignatura cuyas aulas se editan. Siempre presente: la pone quien abre el diálogo. */
  protected readonly asignatura = inject<Asignatura>(DIALOG_DATA);

  protected readonly estado = signal<EstadoAulas>('cargando');
  protected readonly guardando = signal(false);
  /** Error de consulta o de escritura. Vacío = sin error. */
  protected readonly error = signal('');
  /** El GET trajo roles mezclados: se avisa hasta que se elija una marca. */
  protected readonly mezcla = signal(false);

  /** Catálogo de aulas, en el orden en que llega. */
  protected readonly catalogo = signal<Aula[]>([]);
  /** Códigos de las aulas de la asignatura, en el orden en que se muestran. */
  protected readonly filas = signal<string[]>([]);
  /** Las aulas que aún no están en la lista: son las únicas que se pueden añadir. */
  protected readonly disponibles = computed(() => {
    const ya = new Set(this.filas());
    return this.catalogo().filter((a) => !ya.has(a.codigo));
  });
  protected readonly codigoDe = (x: { codigo: string }): string => x.codigo;

  /** La marca de toda la lista. `''` solo cuando el GET trajo roles mezclados. */
  protected readonly marca = this.fb.nonNullable.control<RolAulaAsignatura | ''>('PREFERIDA');
  /** El aula elegida en el selector de añadir; `''` = ninguna. */
  protected readonly nueva = this.fb.nonNullable.control('');

  ngOnInit(): void {
    forkJoin({
      aulas: this.service.listar(this.asignatura.id),
      catalogo: this.aulaService.listar(),
    }).subscribe({
      next: ({ aulas, catalogo }) => {
        this.catalogo.set(catalogo);
        this.filas.set(aulas.map((a) => a.aula));
        const roles = new Set(aulas.map((a) => a.rol));
        if (roles.size > 1) {
          this.mezcla.set(true);
          this.marca.setValue('');
        } else if (roles.size === 1) {
          this.marca.setValue(aulas[0].rol);
        }
        this.estado.set('listo');
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(
          err, `No se pudieron consultar las aulas de ${this.asignatura.codigo}`));
        this.estado.set('error');
      },
    });
  }

  /** Añade el aula elegida si no estaba; el selector ya no ofrece las que están. */
  protected anadir(): void {
    const codigo = this.nueva.value;
    if (codigo === '' || this.filas().includes(codigo)) {
      return;
    }
    this.filas.update((f) => [...f, codigo]);
    this.nueva.setValue('');
  }

  protected quitar(codigo: string): void {
    this.filas.update((f) => f.filter((c) => c !== codigo));
  }

  /** Sin marca elegida (roles mezclados en el GET) no se guarda. */
  protected puedeGuardar(): boolean {
    return this.marca.value !== '' && !this.guardando();
  }

  /** PUT con TODAS las filas y la marca elegida. En error, presenta el mensaje y no cierra. */
  protected guardar(): void {
    const rol = this.marca.value;
    if (rol === '') {
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    const cuerpo: AsignaturaAula[] = this.filas().map((aula) => ({ aula, rol }));
    this.service.reemplazar(this.asignatura.id, cuerpo).subscribe({
      next: () => this.ref.close(true),
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, 'No se pudieron guardar las aulas'));
        this.guardando.set(false);
      },
    });
  }

  /** Salida sin escribir: cierra con `false` y el consumidor no hace nada. */
  protected cancelar(): void {
    this.ref.close(false);
  }

  /**
   * Traduce error Http a texto de usuario. Mismo patrón que `TutoriaDialogo`, copiado con texto
   * propio; no extraído a utilidad compartida, por la misma razón que allí.
   */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
