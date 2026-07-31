import { Component, OnInit, inject, signal } from '@angular/core';
import {
  ReactiveFormsModule,
  FormBuilder,
  Validators,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { SubgrupoService } from '../../services/subgrupo.service';
import { GrupoService } from '../../services/grupo.service';
import { Subgrupo, SubgrupoRequest } from '../../models/subgrupo.model';
import { Grupo } from '../../models/grupo.model';

/** Validator: un `string[]` debe tener al menos un elemento. `Validators.required`
 *  no sirve para esto: da por válido un array vacío (lo trata como valor presente).
 *  Replica en cliente la invariante I6 del backend (≥1 grupo), sin adivinar el 400. */
function arrayNoVacio(control: AbstractControl): ValidationErrors | null {
  const v = control.value as unknown[];
  return Array.isArray(v) && v.length > 0 ? null : { arrayVacio: true };
}

/**
 * Alta y edición de un subgrupo de alumnos en un mismo componente, en diálogo.
 * `DIALOG_DATA` es el subgrupo a editar, o `null` para alta. Cierra con `true` si se
 * guardó (la lista recarga), con `undefined` si se canceló.
 *
 * <p>PRIMER formulario de O-estructura con selección MÚLTIPLE poblada por red. Replica
 * el molde de {@code GrupoForm} (S104) —desplegable poblado en `ngOnInit`, precarga en
 * el constructor, `mensaje()` copiado por valor— con la desviación de que `grupos` es
 * un {@code <select multiple>} y el control es un `string[]`.
 *
 * <p>DOS PUNTOS DONDE EL MULTISELECT NO HEREDA EL MOLDE:
 * <ol>
 *   <li>El {@code <select multiple>} de Reactive Forms NO vincula un array por
 *   `formControlName` como el `<select>` único. Se lee la selección en
 *   `alSeleccionar()` desde `selectedOptions` y se hace `setValue`. Es la única lógica
 *   propia del componente.</li>
 *   <li>La precarga en edición NO se refleja sola en el DOM: `setValue` en el
 *   constructor pone el array en el control, pero el `<select multiple>` no marca sus
 *   `option.selected` por reconciliación (a diferencia del `<select>` único). El HTML
 *   pinta `[selected]` comparando cada opción contra el valor del control.</li>
 * </ol>
 *
 * <p>`Validators.required` NO basta para `grupos`: un array vacío lo pasa. El validator
 * `arrayNoVacio` cubre el hueco y replica la invariante I6 del backend en cliente.
 *
 * <p>La unicidad de `codigo` NO se valida async, igual que en O-catálogo: la fuente de
 * verdad es el backend (UNIQUE + findByCodigo) y un async validator duplicaría esa
 * comprobación con race condition. El 400 de duplicado se PRESENTA cuando llega.
 *
 * <p>La UX del multiselect (buscar, agrupar, chips) se aplaza a la fase de mejora de
 * UX de subgrupos, decisión consciente del arquitecto: aquí es un `<select multiple>`
 * nativo, la mínima desviación del canon.
 */
@Component({
  selector: 'app-subgrupo-form',
  imports: [ReactiveFormsModule],
  templateUrl: './subgrupo-form.html',
  styleUrl: './subgrupo-form.css',
})
export class SubgrupoForm implements OnInit {
  private readonly service = inject(SubgrupoService);
  private readonly grupoService = inject(GrupoService);
  private readonly fb = inject(FormBuilder);
  protected readonly ref = inject<DialogRef<boolean>>(DialogRef);
  private readonly editando = inject<Subgrupo | null>(DIALOG_DATA);

  protected readonly guardando = signal(false);
  protected readonly error = signal('');
  protected readonly esEdicion = this.editando !== null;

  /** Opciones del multiselect, en el orden en que llegan del backend. */
  protected readonly grupos = signal<Grupo[]>([]);

  protected readonly form = this.fb.nonNullable.group({
    codigo: ['', Validators.required],
    grupos: [[] as string[], [Validators.required, arrayNoVacio]],
  });

  constructor() {
    if (this.editando) {
      this.form.setValue({
        codigo: this.editando.codigo,
        grupos: [...this.editando.grupos],
      });
    }
  }

  /** Códigos actualmente seleccionados en el control (para reflejar `[selected]` en
   *  el DOM: el `<select multiple>` no lo hace por reconciliación). */
  protected seleccionados(): string[] {
    return this.form.controls.grupos.value;
  }

  /**
   * Carga los grupos del multiselect. En `ngOnInit` por el molde de la lista, no por
   * necesidad técnica. El valor de `grupos` ya está puesto por el constructor cuando
   * esto resuelve; el HTML refleja la selección con `[selected]` al pintar las
   * opciones, así que no hay que re-aplicar el valor aquí.
   *
   * <p>Un fallo aquí se PRESENTA en el mismo hueco que los errores de guardado: sin
   * grupos el formulario no puede completarse.
   */
  ngOnInit(): void {
    this.grupoService.listar().subscribe({
      next: (lista) => this.grupos.set(lista),
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, 'No se pudieron cargar los grupos'));
      },
    });
  }

  /**
   * Lee la selección del `<select multiple>` y la vuelca al control. Sustituye a la
   * vinculación automática que el `<select>` único tenía por `formControlName`.
   */
  protected alSeleccionar(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const codigos = Array.from(select.selectedOptions).map((o) => o.value);
    this.form.controls.grupos.setValue(codigos);
    this.form.controls.grupos.markAsTouched();
  }

  protected guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    const req: SubgrupoRequest = this.form.getRawValue();
    const peticion = this.editando
      ? this.service.editar(this.editando.id, req)
      : this.service.crear(req);
    peticion.subscribe({
      next: () => this.ref.close(true),
      error: (err: HttpErrorResponse) => {
        // 400 = código duplicado o un código de grupo inexistente (el validator
        // descarta el vacío en cliente). El backend compone el texto y viaja en
        // `message`.
        this.error.set(this.mensaje(err, 'No se pudo guardar el subgrupo'));
        this.guardando.set(false);
      },
    });
  }

  protected cancelar(): void {
    this.ref.close();
  }

  /** Traduce error Http a texto de usuario. Mismo patrón que O-catálogo, copiado con
   *  texto propio (no compartido: tocaría H1). */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
