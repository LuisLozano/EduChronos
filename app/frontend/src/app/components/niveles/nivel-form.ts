import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { NivelService } from '../../services/nivel.service';
import { Nivel, NivelRequest } from '../../models/nivel.model';

/**
 * Alta y edición de un nivel en un mismo componente, presentado en diálogo.
 * `DIALOG_DATA` es el nivel a editar, o `null` para alta. Cierra con `true`
 * si se guardó (la lista recarga), con `undefined` si se canceló.
 *
 * <p>Molde plano de catálogo (canon S102), calcado de {@code AsignaturaForm}: el
 * contrato es un par de campos obligatorios, sin enumerado ni opcionales.
 *
 * <p>ÚNICA DESVIACIÓN DEL MOLDE, y es deliberada: `orden` es
 * `FormControl<number | null>` y NO `nonNullable`, para que el campo NAZCA VACÍO.
 * El equivalente numérico del `''` de un campo de texto es `null`; un valor inicial
 * de 0 o de 1 sería un orden que el formulario decide por el usuario. Importa porque
 * `NivelRequest.orden` es `int` PRIMITIVO en el backend: un cuerpo sin la clave
 * deserializa a 0 en silencio y NINGUNA validación de servidor lo detecta (medido en
 * S111). El servidor no puede distinguir «ausente» de «cero»; el formulario sí.
 *
 * <p>Lo que este formulario NO hace, también a propósito: no valida rango, ni
 * positividad, ni unicidad de `orden`. El backend acepta 0, acepta negativos y acepta
 * repetidos, y un cliente que rechazara cuerpos que la API admite mentiría sobre el
 * contrato (precedente S109, decisión (c) sobre plazas sin subgrupos). `required` es
 * de otra clase: no restringe QUÉ valor es válido, impide enviar uno no elegido.
 *
 * <p>La unicidad de `codigo` NO se valida aquí async: la fuente de verdad es el
 * backend (UNIQUE en esquema + findByCodigo). El 400 de código duplicado se
 * PRESENTA cuando llega, no se anticipa.
 */
@Component({
  selector: 'app-nivel-form',
  imports: [ReactiveFormsModule],
  templateUrl: './nivel-form.html',
  styleUrl: './nivel-form.css',
})
export class NivelForm {
  private readonly service = inject(NivelService);
  private readonly fb = inject(FormBuilder);
  protected readonly ref = inject<DialogRef<boolean>>(DialogRef);
  private readonly editando = inject<Nivel | null>(DIALOG_DATA);

  protected readonly guardando = signal(false);
  protected readonly error = signal('');
  protected readonly esEdicion = this.editando !== null;

  protected readonly form = this.fb.group({
    codigo: this.fb.nonNullable.control('', Validators.required),
    orden: this.fb.control<number | null>(null, Validators.required),
  });

  constructor() {
    if (this.editando) {
      this.form.setValue({
        codigo: this.editando.codigo,
        orden: this.editando.orden,
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
    // `orden` es number|null en el tipo del control, pero el guard de arriba ya
    // descartó el null: el required no deja pasar un formulario con el campo vacío.
    const bruto = this.form.getRawValue();
    const req: NivelRequest = { codigo: bruto.codigo, orden: bruto.orden as number };

    const peticion = this.editando
      ? this.service.editar(this.editando.id, req)
      : this.service.crear(req);

    peticion.subscribe({
      next: () => this.ref.close(true),
      error: (err: HttpErrorResponse) => {
        // 400 = código duplicado o en blanco. El backend compone
        // "Ya existe un nivel con codigo 1ESO" y debería viajar en `message`.
        this.error.set(this.mensaje(err, 'No se pudo guardar el nivel'));
        this.guardando.set(false);
      },
    });
  }

  protected cancelar(): void {
    this.ref.close();
  }

  /**
   * Traduce error Http a texto de usuario. Mismo patrón que `NivelLista` y
   * `horario-view.mensaje()`, copiado con texto propio (no compartido: tocaría H1).
   */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
