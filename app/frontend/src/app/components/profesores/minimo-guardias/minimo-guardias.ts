import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ConfiguracionGuardias,
  ConfiguracionGuardiasService,
} from '../../../services/configuracion-guardias.service';

/** Vacío (lo coge `required`) o un número entero: no hay medio profesor de guardia. */
const entero: ValidatorFn = (control) =>
  control.value === null || Number.isInteger(control.value) ? null : { entero: true };

/**
 * Los tres mínimos de guardia del CENTRO: profesores de guardia por tramo (S212, C-dato-guardias,
 * E8) y, por recreo, profesores de guardia y, de ellos, en la biblioteca (S219,
 * C-dato-guardias-especiales). Van en la pantalla de profesores, encima de la lista, pero son datos
 * del centro: los lee al montar (`GET /api/configuracion-guardias`) y los guarda los tres con su
 * botón (`PUT`, reemplazo total). 0 en el de tramo quiere decir que el centro no usa guardias.
 * Que el de biblioteca supere al de recreo no se impide aquí: lo juzga la prevalidación.
 *
 * <p>Guardar solo se puede con tres enteros ≥ 0, y el botón lo dice deshabilitándose. Los errores,
 * con el molde de `ProfesorLista`: un signal propio y `mensaje(err, degradado)`, el texto del
 * backend primero.
 */
@Component({
  selector: 'app-minimo-guardias',
  imports: [ReactiveFormsModule],
  templateUrl: './minimo-guardias.html',
  styleUrl: './minimo-guardias.css',
})
export class MinimoGuardias implements OnInit {
  private readonly service = inject(ConfiguracionGuardiasService);

  protected readonly minimo = new FormControl<number | null>(null, [
    Validators.required,
    Validators.min(0),
    entero,
  ]);
  /** Mínimo de profesores de guardia por recreo (S219). */
  protected readonly minimoRecreo = new FormControl<number | null>(null, [
    Validators.required,
    Validators.min(0),
    entero,
  ]);
  /** De ellos, mínimo en la biblioteca (S219). */
  protected readonly minimoBiblioteca = new FormControl<number | null>(null, [
    Validators.required,
    Validators.min(0),
    entero,
  ]);
  /** Hasta que responde el GET no hay nada que guardar. */
  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);
  /** Error del último GET o PUT. Vacío = sin error. */
  protected readonly error = signal('');

  ngOnInit(): void {
    this.service.obtener().subscribe({
      next: (configuracion) => {
        this.asignar(configuracion);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, 'No se pudo cargar el mínimo de guardias'));
        this.cargando.set(false);
      },
    });
  }

  protected guardar(): void {
    const minimoPorTramo = this.minimo.value;
    const minimoPorRecreo = this.minimoRecreo.value;
    const minimoBibliotecaPorRecreo = this.minimoBiblioteca.value;
    if (
      this.minimo.invalid ||
      this.minimoRecreo.invalid ||
      this.minimoBiblioteca.invalid ||
      minimoPorTramo === null ||
      minimoPorRecreo === null ||
      minimoBibliotecaPorRecreo === null
    ) {
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.service.guardar({ minimoPorTramo, minimoPorRecreo, minimoBibliotecaPorRecreo }).subscribe({
      next: (configuracion) => {
        this.asignar(configuracion);
        this.guardando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, 'No se pudo guardar el mínimo de guardias'));
        this.guardando.set(false);
      },
    });
  }

  /** Los tres mínimos, tal como los responde el servidor. */
  private asignar(configuracion: ConfiguracionGuardias): void {
    this.minimo.setValue(configuracion.minimoPorTramo);
    this.minimoRecreo.setValue(configuracion.minimoPorRecreo);
    this.minimoBiblioteca.setValue(configuracion.minimoBibliotecaPorRecreo);
  }

  /** Mismo patrón que `ProfesorLista.mensaje()`, con texto propio. */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
