import { Component, OnInit, inject, signal, ChangeDetectionStrategy } from '@angular/core';
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
import { ListaDoble } from '../lista-doble/lista-doble';

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
 * un `string[]`. Desde S230 lo elige una {@link ListaDoble} (C-lista-doble, condición 3 de
 * O-acabado-pre-prueba) vinculada por `formControlName` como cualquier control: el
 * `<select multiple>` nativo, su `alSeleccionar()` y su `[selected]` a mano se retiraron con
 * ella. El filtro de grupos es el de la propia lista.
 *
 * <p>`Validators.required` NO basta para `grupos`: un array vacío lo pasa. El validator
 * `arrayNoVacio` cubre el hueco y replica la invariante I6 del backend en cliente.
 *
 * <p>La unicidad de `codigo` NO se valida async, igual que en O-catálogo: la fuente de
 * verdad es el backend (UNIQUE + findByCodigo) y un async validator duplicaría esa
 * comprobación con race condition. El 400 de duplicado se PRESENTA cuando llega.
 */
@Component({
  selector: 'app-subgrupo-form',
  imports: [ReactiveFormsModule, ListaDoble],
  templateUrl: './subgrupo-form.html',
  changeDetection: ChangeDetectionStrategy.Eager,
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
  /**
   * S206: el subgrupo es el mono-Di de un PDC (`delPdc`, lo calcula el backend). Su código y sus
   * grupos no se cambian por aquí —el backend lo rechaza con un 400—, solo sus alumnos: el
   * formulario los abre DESHABILITADOS y los reenvía tal cual llegaron. Desde S230 los dos van
   * deshabilitados en el CONTROL (`disable()`), y la lista doble se pinta bloqueada.
   */
  protected readonly delPdc = this.editando?.delPdc === true;

  /** Opciones de la lista doble, en el orden en que llegan del backend. */
  protected readonly grupos = signal<Grupo[]>([]);
  /** Código y texto de cada opción de la lista doble. */
  protected readonly codigoDe = (x: { codigo: string }): string => x.codigo;

  protected readonly form = this.fb.nonNullable.group({
    codigo: ['', Validators.required],
    grupos: [[] as string[], [Validators.required, arrayNoVacio]],
  });

  /**
   * Alumnos del subgrupo (S206): opcional; el input vacío da null. Fuera del grupo, como el
   * total de `GrupoForm`, y SIN validador propio: un negativo lo rechaza el backend con su
   * 400, que se presenta como cualquier otro. Se envía siempre: en el backend unos alumnos
   * ausentes en el PUT quedan en null.
   */
  protected readonly alumnos = this.fb.control<number | null>(null);

  constructor() {
    if (this.editando) {
      this.form.setValue({
        codigo: this.editando.codigo,
        grupos: [...this.editando.grupos],
      });
      this.alumnos.setValue(this.editando.alumnos ?? null);
      if (this.delPdc) {
        // getRawValue() sigue incluyendo código y grupos: viajan tal cual, que es lo que pide el
        // backend.
        this.form.controls.codigo.disable();
        this.form.controls.grupos.disable();
      }
    }
  }

  /**
   * Carga los grupos de la lista doble. En `ngOnInit` por el molde de la lista, no por
   * necesidad técnica. El valor de `grupos` ya está puesto por el constructor cuando
   * esto resuelve, y la lista doble lo cruza con las opciones al llegar: no hay que
   * re-aplicarlo aquí.
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

  protected guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    const req: SubgrupoRequest = { ...this.form.getRawValue(), alumnos: this.alumnos.value };
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
