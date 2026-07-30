import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { AulaService } from '../../services/aula.service';
import { Aula, AulaRequest, TIPOS_AULA } from '../../models/aula.model';

/**
 * Alta y edición de un aula en un mismo componente, presentado en diálogo.
 * SEGUNDO formulario del proyecto: replica el molde que fijó {@code ProfesorForm}
 * (S101) —`DIALOG_DATA` es la entidad a editar o `null` para alta, cierra con `true`
 * si se guardó y con `undefined` si se canceló, estado en signals, `mensaje()` propio
 * con degradado por status— y lo ESTIRA en las dos dimensiones que profesores no
 * tenía: un campo de enumerado y cuatro campos opcionales de verdad.
 *
 * <p>ENUMERADO. El selector ofrece {@link TIPOS_AULA}, los ocho valores con
 * semántica; COMUN queda fuera (D-F8.5-C3-a). El `<select>` con un `required` cubre
 * el hueco que en un `<input>` libre cubría el backend: aquí el valor inválido es
 * IMPOSIBLE de teclear, pero el 400 de "tipo invalido" se sigue traduciendo porque
 * el contrato lo puede devolver (un DTO editado por otra vía, un enum recortado).
 *
 * <p>OPCIONALES DE VERDAD (D-4). `capacidad`, `edificio`, `planta` y `sector` viajan
 * null si se dejan en blanco: `aRequest()` normaliza el `''` que devuelve un input de
 * texto vacío, para no persistir cadena vacía en columnas nullable. Los dos
 * numéricos ya llegan null desde el `NumberValueAccessor` cuando el input está vacío,
 * y por eso se declaran con `control<number | null>` explícito: el atajo de array
 * los inferiría como `FormControl<null>`.
 *
 * <p>La unicidad de `codigo` NO se valida aquí async, igual que en profesores: la
 * fuente de verdad es el backend (UNIQUE en esquema + findByCodigo) y un async
 * validator duplicaría la comprobación con una race condition. El 400 de código
 * duplicado se PRESENTA cuando llega, no se anticipa.
 */
@Component({
  selector: 'app-aula-form',
  imports: [ReactiveFormsModule],
  templateUrl: './aula-form.html',
  styleUrl: './aula-form.css',
})
export class AulaForm {
  private readonly service = inject(AulaService);
  private readonly fb = inject(FormBuilder);
  protected readonly ref = inject<DialogRef<boolean>>(DialogRef);
  private readonly editando = inject<Aula | null>(DIALOG_DATA);

  protected readonly guardando = signal(false);
  protected readonly error = signal('');
  protected readonly esEdicion = this.editando !== null;

  /**
   * Opciones del selector: los ocho con semántica y, SOLO si se está editando un
   * aula cuyo tipo no está entre ellos (hoy: COMUN), también el suyo. Omitir COMUN
   * del catálogo evita CREAR datos sin semántica; ocultarlo también al editar los
   * BORRARÍA en silencio —el `<select>` no casaría ninguna opción, se mostraría
   * vacío y el `required` obligaría a reasignar tipo para poder tocar la capacidad—.
   * Son dos cosas distintas y esta lista solo hace la primera.
   */
  protected readonly tipos: readonly string[] =
    this.editando && !TIPOS_AULA.includes(this.editando.tipo as (typeof TIPOS_AULA)[number])
      ? [...TIPOS_AULA, this.editando.tipo]
      : TIPOS_AULA;

  protected readonly form = this.fb.nonNullable.group({
    codigo: ['', Validators.required],
    tipo: ['', Validators.required],
    capacidad: this.fb.nonNullable.control<number | null>(null),
    edificio: [''],
    planta: this.fb.nonNullable.control<number | null>(null),
    sector: [''],
  });

  constructor() {
    if (this.editando) {
      this.form.setValue({
        codigo: this.editando.codigo,
        tipo: this.editando.tipo,
        capacidad: this.editando.capacidad,
        // Los nullable del DTO entran al form como '' (lo que un input de texto
        // vacío representa); `aRequest()` deshace la conversión al salir.
        edificio: this.editando.edificio ?? '',
        planta: this.editando.planta,
        sector: this.editando.sector ?? '',
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
    const req = this.aRequest();

    const peticion = this.editando
      ? this.service.editar(this.editando.id, req)
      : this.service.crear(req);

    peticion.subscribe({
      next: () => this.ref.close(true),
      error: (err: HttpErrorResponse) => {
        // 400 = código duplicado o tipo no parseable (los required descartan el
        // vacío). El backend compone "Ya existe un aula con codigo A12" o
        // "tipo invalido: 'X'. Valores validos: [...]" y viaja en `message`.
        this.error.set(this.mensaje(err, 'No se pudo guardar el aula'));
        this.guardando.set(false);
      },
    });
  }

  protected cancelar(): void {
    this.ref.close();
  }

  /**
   * Traduce el estado del formulario al cuerpo del contrato: los cuatro opcionales
   * viajan null cuando están en blanco, para no persistir `''` en columnas nullable.
   */
  private aRequest(): AulaRequest {
    const v = this.form.getRawValue();
    return {
      codigo: v.codigo,
      tipo: v.tipo,
      capacidad: v.capacidad,
      edificio: this.vacioANull(v.edificio),
      planta: v.planta,
      sector: this.vacioANull(v.sector),
    };
  }

  private vacioANull(s: string): string | null {
    return s.trim() === '' ? null : s;
  }

  /**
   * Traduce error Http a texto de usuario. Mismo patrón que `AulaLista` y
   * `profesor-form.mensaje()`, copiado con texto propio (no compartido: extraerlo
   * tocaría horario-view, D-F8.6, H1 cerrado).
   */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
