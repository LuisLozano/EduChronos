import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { AsignaturaService } from '../../services/asignatura.service';
import { Asignatura, AsignaturaRequest } from '../../models/asignatura.model';

/**
 * Alta y edición de una asignatura en un mismo componente, presentado en diálogo.
 * `DIALOG_DATA` es la asignatura a editar, o `null` para alta. Cierra con `true`
 * si se guardó (la lista recarga), con `undefined` si se canceló.
 *
 * <p>TERCER formulario del catálogo, y el CASO PLANO del molde: replica lo que fijó
 * {@code ProfesorForm} (S101) sin necesitar NINGUNA de las dos extensiones que S102
 * añadió en {@code AulaForm} —ni campo de enumerado ni opcionales de verdad—, porque
 * el contrato es el mismo par `(codigo, nombreCompleto)` con ambos obligatorios. Su
 * valor es justamente ese: comprobar que el molde se aplica sin estirarlo.
 *
 * <p>`nonNullable` evita el `| null` de FormControl en TS y encaja con los
 * Validators.required.
 *
 * <p>La unicidad de `codigo` NO se valida aquí async a propósito: la fuente de
 * verdad es el backend (UNIQUE en esquema + findByCodigo). Un async validator
 * duplicaría esa comprobación con una race condition. El 400 de código duplicado
 * se PRESENTA cuando llega, no se anticipa.
 *
 * <p>FUERA DE ALCANCE en S103: el sub-recurso `aulas-compatibles` que
 * {@code AsignaturaController} expone en `GET/PUT /{id}/aulas-compatibles`. Estirar
 * el molde con un sub-recurso es objeto de sesión propia.
 */
@Component({
  selector: 'app-asignatura-form',
  imports: [ReactiveFormsModule],
  templateUrl: './asignatura-form.html',
  styleUrl: './asignatura-form.css',
})
export class AsignaturaForm {
  private readonly service = inject(AsignaturaService);
  private readonly fb = inject(FormBuilder);
  protected readonly ref = inject<DialogRef<boolean>>(DialogRef);
  private readonly editando = inject<Asignatura | null>(DIALOG_DATA);

  protected readonly guardando = signal(false);
  protected readonly error = signal('');
  protected readonly esEdicion = this.editando !== null;

  protected readonly form = this.fb.nonNullable.group({
    codigo: ['', Validators.required],
    nombreCompleto: ['', Validators.required],
  });

  constructor() {
    if (this.editando) {
      this.form.setValue({
        codigo: this.editando.codigo,
        nombreCompleto: this.editando.nombreCompleto,
      });
    }
  }

  protected guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    const req: AsignaturaRequest = this.form.getRawValue();

    const peticion = this.editando
      ? this.service.editar(this.editando.id, req)
      : this.service.crear(req);

    peticion.subscribe({
      next: () => this.ref.close(true),
      error: (err: HttpErrorResponse) => {
        // 400 = código duplicado (los required descartan el vacío). El backend
        // compone "Ya existe una asignatura con codigo MAT8" y viaja en `message`.
        this.error.set(this.mensaje(err, 'No se pudo guardar la asignatura'));
        this.guardando.set(false);
      },
    });
  }

  protected cancelar(): void {
    this.ref.close();
  }

  /**
   * Traduce error Http a texto de usuario. Mismo patrón que `AsignaturaLista` y
   * `horario-view.mensaje()`, copiado con texto propio (no compartido: tocaría H1).
   */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
