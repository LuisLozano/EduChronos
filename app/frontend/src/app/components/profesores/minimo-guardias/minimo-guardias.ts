import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ConfiguracionGuardiasService } from '../../../services/configuracion-guardias.service';

/** Vacío (lo coge `required`) o un número entero: no hay medio profesor de guardia. */
const entero: ValidatorFn = (control) =>
  control.value === null || Number.isInteger(control.value) ? null : { entero: true };

/**
 * El mínimo de profesores de guardia por tramo del centro (S212, C-dato-guardias, E8). Va en la
 * pantalla de profesores, encima de la lista, pero es un dato del CENTRO: lo lee al montar
 * (`GET /api/configuracion-guardias`) y lo guarda con su botón (`PUT`). 0 quiere decir que el
 * centro no usa guardias.
 *
 * <p>Guardar solo se puede con un entero ≥ 0, y el botón lo dice deshabilitándose. Los errores,
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
  /** Hasta que responde el GET no hay nada que guardar. */
  protected readonly cargando = signal(true);
  protected readonly guardando = signal(false);
  /** Error del último GET o PUT. Vacío = sin error. */
  protected readonly error = signal('');

  ngOnInit(): void {
    this.service.obtener().subscribe({
      next: (configuracion) => {
        this.minimo.setValue(configuracion.minimoPorTramo);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, 'No se pudo cargar el mínimo de guardias'));
        this.cargando.set(false);
      },
    });
  }

  protected guardar(): void {
    const valor = this.minimo.value;
    if (this.minimo.invalid || valor === null) {
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.service.guardar(valor).subscribe({
      next: (configuracion) => {
        this.minimo.setValue(configuracion.minimoPorTramo);
        this.guardando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, 'No se pudo guardar el mínimo de guardias'));
        this.guardando.set(false);
      },
    });
  }

  /** Mismo patrón que `ProfesorLista.mensaje()`, con texto propio. */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
