import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, ValidatorFn, Validators } from '@angular/forms';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { ProfesorService } from '../../services/profesor.service';
import { Cargo, ETIQUETA_CARGO, Profesor, ProfesorRequest } from '../../models/profesor.model';

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
 *
 * <p><b>Horas declaradas y cargo (S203) van en controles APARTE del `form`.</b> Los specs de
 * antes hacen `form.setValue({codigo, nombreCompleto})`, que con dos controles más en el grupo
 * fallaría (NG01002), y O-interfaz rehará los formularios. Las dos condiciones que eso exige
 * están aquí: la validez de guardar incluye la del total (un negativo no se envía), y en
 * edición los dos se cargan con lo que tiene el profesor, porque el PUT es reemplazo total y
 * sin cargarlos guardar otro cambio borraría el total y devolvería el cargo a Profesor/a. Se
 * mandan SIEMPRE, el total vacío como null.
 *
 * <p><b>Guardias ordinarias (S212, C-dato-guardias)</b>, con el mismo patrón y por la misma razón:
 * control aparte, cargado en edición y enviado siempre. A diferencia del total es obligatorio y
 * entero: 0 por defecto, vacío o decimal no se guarda.
 */
/** Vacío (lo coge `required`) o un número entero; los decimales no son guardias. */
const entero: ValidatorFn = (control) =>
  control.value === null || Number.isInteger(control.value) ? null : { entero: true };

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

  /** Opciones del select de cargo, en el orden del enum. */
  protected readonly cargos = Object.entries(ETIQUETA_CARGO) as [Cargo, string][];

  /** Horas de clase declaradas: opcional, ≥ 0. El input vacío da null (NumberValueAccessor). */
  protected readonly totalDeclarado = this.fb.control<number | null>(null, Validators.min(0));

  /** Cargo, por defecto Profesor/a. */
  protected readonly cargo = this.fb.nonNullable.control<Cargo>('PROFESOR');

  /** Guardias ordinarias semanales (S212): obligatorio, entero, ≥ 0, 0 por defecto. */
  protected readonly guardiasOrdinarias = this.fb.control<number | null>(0, [
    Validators.required,
    Validators.min(0),
    entero,
  ]);

  constructor() {
    if (this.editando) {
      this.form.setValue({
        codigo: this.editando.codigo,
        nombreCompleto: this.editando.nombreCompleto,
      });
      this.totalDeclarado.setValue(this.editando.totalDeclarado ?? null);
      this.cargo.setValue(this.editando.cargo ?? 'PROFESOR');
      this.guardiasOrdinarias.setValue(this.editando.guardiasOrdinarias ?? 0);
    }
  }

  protected guardar(): void {
    if (this.form.invalid || this.totalDeclarado.invalid || this.guardiasOrdinarias.invalid) {
      this.form.markAllAsTouched();
      this.totalDeclarado.markAsTouched();
      this.guardiasOrdinarias.markAsTouched();
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    const req: ProfesorRequest = {
      ...this.form.getRawValue(),
      totalDeclarado: this.totalDeclarado.value,
      cargo: this.cargo.value,
      guardiasOrdinarias: this.guardiasOrdinarias.value ?? 0,
    };

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
