import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { GrupoService } from '../../services/grupo.service';
import { NivelService } from '../../services/nivel.service';
import { Grupo, GrupoRequest } from '../../models/grupo.model';
import { Nivel } from '../../models/nivel.model';

/**
 * Alta y edición de un grupo administrativo en un mismo componente, presentado en
 * diálogo. `DIALOG_DATA` es el grupo a editar, o `null` para alta. Cierra con `true`
 * si se guardó (la lista recarga), con `undefined` si se canceló.
 *
 * <p>CUARTO y último formulario de O-catálogo. Replica el molde de {@code ProfesorForm}
 * (S101) tal como lo dejó el caso plano {@code AsignaturaForm} (S103), y lo estira en
 * UNA dimensión nueva que ninguno de los tres anteriores tenía: un desplegable poblado
 * POR RED. El de {@code AulaForm} (S102) se llena de una constante local
 * ({@code TIPOS_AULA}); este pide `/api/niveles` en el `ngOnInit`, lo que convierte al
 * componente en consumidor de DOS servicios y añade una petición al montaje —con las
 * consecuencias que su spec documenta—.
 *
 * <p>DOS CONTROLES, TRES CAMPOS EN EL CONTRATO. `tipo` NO se expone en la UI: el
 * backend aplica una lista blanca (D-nueva-2) y solo acepta `ORDINARIO`, así que un
 * selector con una única opción sería un campo que el usuario ve y no puede decidir.
 * `guardar()` lo inyecta con ese valor fijo al construir el cuerpo. El 400 de "tipo no
 * permitido" queda inalcanzable desde esta pantalla, pero se sigue traduciendo porque
 * el contrato lo puede devolver.
 *
 * <p>EL DESPLEGABLE NO REORDENA. `/api/niveles` sirve la lista ordenada por `orden`
 * (D-1), que es el orden pedagógico; ordenarla aquí por código la rompería (1BACH
 * antes de 1ESO). Arranca sin preselección y con `required`, para que elegir nivel sea
 * un acto explícito del usuario y no el primer valor que llegó: la opción de arranque
 * es un placeholder `disabled` con `value=""` —simetría con el `<select>` de
 * {@code AulaForm}—, que casa el `''` inicial del control y no es seleccionable.
 *
 * <p>`nonNullable` evita el `| null` de FormControl en TS y encaja con los
 * Validators.required.
 *
 * <p>La unicidad de `codigo` NO se valida aquí async, igual que en las tres anteriores:
 * la fuente de verdad es el backend (UNIQUE en esquema + findByCodigo) y un async
 * validator duplicaría esa comprobación con una race condition. El 400 de código
 * duplicado se PRESENTA cuando llega, no se anticipa.
 *
 * <p>FUERA DE ALCANCE: el sub-recurso `tutoria` que {@code GrupoController} expone en
 * `GET/PUT /{id}/tutoria`. Mismo criterio con que S103 dejó fuera
 * `aulas-compatibles`: estirar el molde con un sub-recurso es objeto de sesión propia.
 */
@Component({
  selector: 'app-grupo-form',
  imports: [ReactiveFormsModule],
  templateUrl: './grupo-form.html',
  styleUrl: './grupo-form.css',
})
export class GrupoForm implements OnInit {
  private readonly service = inject(GrupoService);
  private readonly nivelService = inject(NivelService);
  private readonly fb = inject(FormBuilder);
  protected readonly ref = inject<DialogRef<boolean>>(DialogRef);
  private readonly editando = inject<Grupo | null>(DIALOG_DATA);

  protected readonly guardando = signal(false);
  protected readonly error = signal('');
  protected readonly esEdicion = this.editando !== null;

  /** Opciones del desplegable, en el orden en que llegan del backend. */
  protected readonly niveles = signal<Nivel[]>([]);

  protected readonly form = this.fb.nonNullable.group({
    codigo: ['', Validators.required],
    nivel: ['', Validators.required],
  });

  constructor() {
    if (this.editando) {
      this.form.setValue({
        codigo: this.editando.codigo,
        nivel: this.editando.nivel,
      });
    }
  }

  /**
   * Carga los niveles del desplegable. Va en `ngOnInit` y no en el constructor por
   * el molde de la lista (`AsignaturaLista`), no por necesidad técnica.
   *
   * <p>El valor de `nivel` ya está puesto por el constructor cuando esto resuelve: el
   * `<select>` no casa ninguna opción mientras la lista está vacía y Angular lo
   * reconcilia al pintarlas. Por eso el orden (setValue antes, opciones después) es
   * correcto y no hay que re-aplicar el valor aquí.
   *
   * <p>Un fallo aquí se PRESENTA en el mismo hueco que los errores de guardado: sin
   * niveles el formulario no puede completarse, y callarlo dejaría un desplegable
   * vacío sin explicación.
   */
  ngOnInit(): void {
    this.nivelService.listar().subscribe({
      next: (lista) => this.niveles.set(lista),
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, 'No se pudieron cargar los niveles'));
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
    // `tipo` no sale del formulario: es constante de este flujo (ver javadoc). El
    // contrato lo exige, así que se inyecta aquí y no en el servicio, que es un
    // wrapper pelado y no debe conocer reglas de la pantalla.
    const req: GrupoRequest = { ...this.form.getRawValue(), tipo: 'ORDINARIO' };

    const peticion = this.editando
      ? this.service.editar(this.editando.id, req)
      : this.service.crear(req);

    peticion.subscribe({
      next: () => this.ref.close(true),
      error: (err: HttpErrorResponse) => {
        // 400 = código duplicado o nivel inexistente (los required descartan el
        // vacío). El backend compone "Ya existe un grupo con codigo 1ESOA" o
        // "No existe nivel con codigo 9ESO" y viaja en `message`.
        this.error.set(this.mensaje(err, 'No se pudo guardar el grupo'));
        this.guardando.set(false);
      },
    });
  }

  protected cancelar(): void {
    this.ref.close();
  }

  /**
   * Traduce error Http a texto de usuario. Mismo patrón que `GrupoLista` y
   * `asignatura-form.mensaje()`, copiado con texto propio (no compartido: tocaría H1).
   */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
