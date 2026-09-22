import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';

import { CursoService } from '../../services/curso.service';
import { RecargaPagina } from '../../services/recarga-pagina';
import { CursoDTO, CursoListadoDTO, DuplicarCursoRequest } from '../../models/curso.model';

/**
 * En qué estado está la pantalla. UN valor, no banderas sueltas: mismo criterio que
 * {@code EstadoTutoria} y {@code EstadoPdc}, donde tres booleanos independientes admitirían
 * combinaciones imposibles —«cargando y cargado»— que el spec tendría que enumerar para
 * negarlas.
 */
export type EstadoCursos = 'cargando' | 'cargado' | 'error';

/**
 * Qué está mirando el usuario. Separado de {@link EstadoCursos} a propósito: son dos
 * preguntas independientes —«¿han llegado los datos?» y «¿en qué pantalla estoy?»— y
 * mezclarlas en una sola unión daría estados como «duplicar-cargando-con-error» que no
 * significan nada.
 */
export type ModoCursos = 'lista' | 'duplicar';

/** La forma que exige un nombre de curso: cuatro dígitos, barra, cuatro dígitos. */
const FORMA_CURSO = /^\d{4}\/\d{4}$/;

/**
 * El selector de cursos: cuáles hay, abrir otro y crear el siguiente (O-curso, S160,
 * C-selector-curso fase B).
 *
 * <p>Molde de {@code TutoriaDialogo}: estado en una unión de literales, `guardando` y
 * `error` separados de la carga, formulario reactivo `nonNullable`, cierre sólo en el
 * camino de éxito y `mensaje()` privado copiado con texto propio. Es la COPIA NÚMERO 20 de
 * ese `mensaje()`, y se copia a propósito: extraerlo a una utilidad compartida tocaría los
 * diecinueve componentes de H1 (D-F8.6, cerrado).
 *
 * <p><b>Lo que este diálogo hace y los otros diecinueve no: RECARGAR.</b> Abrir un curso y
 * duplicar cambian la base de datos que hay debajo, así que en el camino de éxito no se
 * cierra con `true` para que el consumidor refresque una lista: se llama a
 * {@link RecargaPagina#recargarEnInicio}, que tira la aplicación entera y la trae de nuevo
 * en `/`. Ver el javadoc de ese servicio. En el camino de ERROR no se recarga ni se cierra:
 * el mensaje se queda a la vista y el formulario intacto, porque el usuario tiene que poder
 * leerlo y corregir.
 *
 * <p><b>Por qué el `data` es el {@code CursoDTO} y no se pide aquí.</b> La barra ya lo tiene
 * —lo necesita para pintar el nombre y la marca de solo lectura—, y volver a pedirlo abriría
 * la puerta a que el diálogo enseñara un curso abierto distinto del que dice la barra que
 * está abierto. De él salen dos cosas: la propuesta con la que se prefija el campo, y si
 * hace falta preguntar el nombre del curso actual (condición 6).
 *
 * <p><b>El requisito (b) se calcula aquí y también en el backend, y no es un descuido.</b>
 * «Se puede duplicar un curso archivado si y sólo si no queda ninguno activo» lo decide
 * {@code CursoService.sinNingunCursoActivo()}, que es quien manda; esta copia sólo gobierna
 * si el BOTÓN se ofrece habilitado, para no llevar al usuario a un 409 que ya se sabe.
 * Cuando las dos no coincidan, gana el servidor y su mensaje sale en el diálogo.
 */
@Component({
  selector: 'app-cursos-dialogo',
  imports: [ReactiveFormsModule],
  templateUrl: './cursos-dialogo.html',
  styleUrl: './cursos-dialogo.css',
})
export class CursosDialogo implements OnInit {
  private readonly service = inject(CursoService);
  private readonly recarga = inject(RecargaPagina);
  private readonly fb = inject(FormBuilder);
  protected readonly ref = inject<DialogRef<void>>(DialogRef);

  /** El curso abierto, tal como lo pintó la barra. Siempre presente. */
  protected readonly curso = inject<CursoDTO>(DIALOG_DATA);

  protected readonly estado = signal<EstadoCursos>('cargando');
  protected readonly modo = signal<ModoCursos>('lista');
  protected readonly guardando = signal(false);

  /** Error de consulta o de escritura. Vacío = sin error. */
  protected readonly error = signal('');

  /** Los cursos de la carpeta, en el orden en que llegan del backend. */
  protected readonly cursos = signal<CursoListadoDTO[]>([]);

  /**
   * ¿Hace falta preguntar cómo se llama el curso actual? Sólo cuando la base abierta no lo
   * trae escrito (condición 6): ese nombre es el que quedará archivado y nadie más lo sabe.
   * Con un curso que sí lo trae, mandarlo sería arriesgarse a un 400 por discrepancia.
   */
  protected readonly pideNombreActual = computed(() => this.curso.nombre === null);

  /**
   * ¿Se puede duplicar? Dos vías, y la segunda es el requisito (b):
   *
   * <ul>
   *   <li>el curso abierto está ACTIVO —el caso de todos los años—, o
   *   <li>no queda ningún curso activo en la carpeta, que es la salida del callejón de un
   *       centro que archivó el último que tenía.
   * </ul>
   *
   * Un curso SIN nombre cuenta como activo, igual que en el backend: una base anterior a
   * S159 es un curso en marcha, no un archivo.
   */
  protected readonly puedeDuplicar = computed(
    () => !this.curso.archivado || this.cursos().every((c) => c.archivado),
  );

  /**
   * El formulario del modo duplicar. `nombreNuevo` siempre; `nombreActual` sólo se valida
   * cuando se pregunta —ver {@link pideNombreActual}—, porque un `required` permanente
   * bloquearía el envío en el caso normal, donde el campo ni se pinta.
   *
   * <p>El patrón repite la forma que el backend exige y NO la regla entera: que los dos años
   * sean consecutivos lo comprueba él, y duplicarlo aquí daría dos sitios donde equivocarse.
   * Lo que esta validación compra es no gastar una ida y vuelta por un «2026-2027».
   */
  protected readonly form = this.fb.nonNullable.group({
    nombreNuevo: ['', [Validators.required, Validators.pattern(FORMA_CURSO)]],
    nombreActual: [''],
  });

  ngOnInit(): void {
    this.cargar();
  }

  /** Pide la lista. Un fallo aquí deja el diálogo en `'error'`: sin lista no hay selector. */
  private cargar(): void {
    this.service.listar().subscribe({
      next: (cursos) => {
        this.cursos.set(cursos);
        this.estado.set('cargado');
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, 'No se pudieron consultar los cursos'));
        this.estado.set('error');
      },
    });
  }

  /**
   * Abre otro curso. En éxito RECARGA; en error presenta y NO recarga: la aplicación sigue
   * en el curso de antes, que es justo lo que el backend garantiza cuando la apertura falla.
   */
  protected abrir(curso: CursoListadoDTO): void {
    this.guardando.set(true);
    this.error.set('');

    this.service.abrir(curso.fichero).subscribe({
      next: () => this.recarga.recargarEnInicio(),
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, `No se pudo abrir ${curso.fichero}`));
        this.guardando.set(false);
      },
    });
  }

  /**
   * Pasa al formulario de duplicar y prefija el nombre nuevo con la propuesta. Vacío cuando
   * no hay propuesta —base sin nombre—, que es el caso en que además se pide el actual.
   */
  protected irADuplicar(): void {
    this.error.set('');
    this.form.reset({
      nombreNuevo: this.curso.propuestaSiguiente ?? '',
      nombreActual: '',
    });
    this.modo.set('duplicar');
  }

  /** Vuelve a la lista sin escribir nada. El error se limpia: era de la otra pantalla. */
  protected volverALista(): void {
    this.error.set('');
    this.modo.set('lista');
  }

  /**
   * Crea el curso siguiente. En éxito RECARGA —el curso nuevo ya queda abierto en el
   * servidor—; en error presenta el mensaje y CONSERVA el formulario, sin cerrar ni
   * recargar: el caso corriente es un nombre que ya existe, y hacerle teclearlo otra vez
   * sería castigarle por una errata.
   */
  protected duplicar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.error.set('');

    const valores = this.form.getRawValue();
    const peticion: DuplicarCursoRequest = { nombreNuevo: valores.nombreNuevo };
    if (this.pideNombreActual()) {
      peticion.nombreActual = valores.nombreActual;
    }

    this.service.duplicar(peticion).subscribe({
      next: () => this.recarga.recargarEnInicio(),
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, 'No se pudo crear el curso'));
        this.guardando.set(false);
      },
    });
  }

  /** Salida sin efecto. Cerrar por backdrop o Escape hace lo mismo. */
  protected cerrar(): void {
    this.ref.close();
  }

  /** Rótulo de una fila: el nombre si lo hay, y si no el fichero con su aviso. */
  protected rotulo(curso: CursoListadoDTO): string {
    return curso.nombre ?? 'Sin nombre';
  }

  /**
   * Traduce error Http a texto de usuario. Mismo patrón que `TutoriaDialogo`, `PdcDialogo` y
   * `GrupoForm`, copiado con texto propio; NO extraído a utilidad compartida a propósito:
   * hacerlo tocaría los componentes de H1 (D-F8.6, cerrado).
   */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
