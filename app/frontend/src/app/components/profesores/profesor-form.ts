import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { ProfesorService } from '../../services/profesor.service';
import { Profesor, ProfesorRequest } from '../../models/profesor.model';

/**
 * Alta y edición de un profesor en un mismo componente, presentado en diálogo.
 * `DIALOG_DATA` es el profesor a editar, o `null` para alta. Cierra con `true`
 * si se guardó (la lista recarga), con `undefined` si se canceló.
 *
 * PRIMER formulario del proyecto: fija el molde de Reactive tipado para el resto
 * del catálogo (candidato a molde, a validar en aulas). `nonNullable` evita el
 * `| null` de FormControl en TS y encaja con los Validators.required.
 *
 * La unicidad de `codigo` NO se valida aquí async a propósito: la fuente de
 * verdad es el backend (UNIQUE en esquema + findByCodigo). Un async validator
 * duplicaría esa comprobación con una race condition. El 400 de código duplicado
 * se PRESENTA cuando llega, no se anticipa.
 */
@Component({
  selector: 'app-profesor-form',
  imports: [ReactiveFormsModule],
  templateUrl: './profesor-form.html',
  styleUrl: './profesor-form.css',
})
export class ProfesorForm {
  private readonly service = inject(ProfesorService);
  private readonly fb = inject(FormBuilder);
  protected readonly ref = inject<DialogRef<boolean>>(DialogRef);
  private readonly editando = inject<Profesor | null>(DIALOG_DATA);

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
    const req: ProfesorRequest = this.form.getRawValue();

    const peticion = this.editando
      ? this.service.editar(this.editando.id, req)
      : this.service.crear(req);

    peticion.subscribe({
      next: () => this.ref.close(true),
      error: (err: HttpErrorResponse) => {
        // 400 = código duplicado (los required descartan el vacío). El backend
        // compone "Ya existe un profesor con codigo MAT8" y viaja en `message`.
        this.error.set(this.mensaje(err, 'No se pudo guardar el profesor'));
        this.guardando.set(false);
      },
    });
  }

  protected cancelar(): void {
    this.ref.close();
  }

  /**
   * Traduce error Http a texto de usuario. Mismo patrón que `ProfesorLista` y
   * `horario-view.mensaje()`, copiado con texto propio (no compartido: tocaría H1).
   */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
