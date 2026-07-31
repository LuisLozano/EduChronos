import { Component, OnInit, inject, signal } from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Dialog } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';

import { JornadaService } from '../../services/jornada.service';
import { JornadaDTO, JornadaRequest, TramoJornadaDTO } from '../../models/jornada.model';
import { ConfirmarReemplazo } from './confirmar-reemplazo';

/** Controles de una fila de la rejilla. Ver {@link Jornada} para el porqué de `esRecreo`. */
type FilaTramo = FormGroup<{
  horaInicio: FormControl<string>;
  horaFin: FormControl<string>;
  esRecreo: FormControl<boolean>;
}>;

/**
 * Pantalla de la JORNADA del centro (C-jornada M4). SINGLETON: no hay par lista/form
 * como en los CRUD de catálogo —no hay nada que listar ni que dar de alta—, así que
 * todo vive en un solo componente que hace `GET` al entrar y `PUT` al guardar.
 *
 * <p><b>Cero lógica de dominio aquí.</b> El backend valida, expande a los cinco días y
 * numera (`orden` global, `ordenEnDia` derivado). Este componente no numera, no replica
 * días, no comprueba solapes ni el techo de 6 lectivos: solo recoge el día tipo, lo
 * manda y presenta lo que vuelva. La validación de patrón `HH:mm` que sí hace es de
 * FORMATO de campo, no de dominio, y su fuente de verdad sigue siendo el 400 del
 * servidor.
 *
 * <p><b>La asimetría del contrato se resuelve en dos puntos y solo ahí.</b> Al entrar,
 * {@link #primerDia} recorta los 35 tramos que llegan a los del primer día (los cinco son
 * idénticos por construcción del backend); al guardar, {@link #aRequest} suelta `dia`,
 * `orden` y `ordenEnDia`. Ese recorte es la razón de que `JornadaRequest` sea un tipo
 * distinto de `JornadaDTO` en el modelo: el compilador impide reenviar lo recibido.
 *
 * <p><b>`esRecreo`, no `esLectivo`.</b> La rejilla tiene una columna «Recreo» cuyo toggle
 * se marca en las filas no lectivas, así que el control se declara con la polaridad de lo
 * que el usuario ve y {@link #aRequest} la invierte al salir. Atar el control a
 * `esLectivo` y pintarlo invertido en la plantilla dejaría la inversión repartida entre
 * TS y HTML.
 *
 * <p><b>El 400 y el 409 son sucesos distintos y van por caminos distintos.</b> Un 400 es
 * «corrige la malla»: mensaje inline junto al formulario, que sigue editable. Un 409 no
 * se corrige tocando el formulario —hay horarios o restricciones vivos— así que pone la
 * pantalla en SOLO LECTURA con un aviso destacado y deshabilita Guardar; se sale de ahí
 * con {@link #recargar}, no editando.
 */
@Component({
  selector: 'app-jornada',
  imports: [ReactiveFormsModule],
  templateUrl: './jornada.html',
  styleUrl: './jornada.css',
})
export class Jornada implements OnInit {
  /** `HH:mm` de 24 h. Formato de campo, no regla de dominio: la de verdad es el 400. */
  private static readonly PATRON_HORA = /^([01]\d|2[0-3]):[0-5]\d$/;

  private readonly service = inject(JornadaService);
  private readonly dialog = inject(Dialog);
  private readonly fb = inject(FormBuilder);

  protected readonly cargando = signal(false);
  protected readonly guardando = signal(false);
  /** `false` = el servidor devolvió la malla de referencia y nadie la ha guardado. */
  protected readonly persistida = signal(true);
  /** Error de carga o 400 de guardado. Inline junto al formulario. Vacío = sin error. */
  protected readonly error = signal('');
  /** Texto del 409. No vacío ⇒ pantalla en solo lectura. */
  protected readonly conflicto = signal('');
  protected readonly modoSoloLectura = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    tramos: this.fb.nonNullable.array<FilaTramo>([]),
  });

  protected get tramos() {
    return this.form.controls.tramos;
  }

  ngOnInit(): void {
    this.cargar();
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.error.set('');
    this.service.obtener().subscribe({
      next: (dto) => {
        this.aplicar(dto);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, 'No se pudo cargar la jornada'));
        this.cargando.set(false);
      },
    });
  }

  /** Sale del solo lectura releyendo del servidor. Único camino de vuelta tras un 409. */
  protected recargar(): void {
    this.conflicto.set('');
    this.modoSoloLectura.set(false);
    this.form.enable();
    this.cargar();
  }

  protected guardar(): void {
    if (this.modoSoloLectura()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    // Con la malla ya guardada, el PUT DESTRUYE la anterior: se confirma antes. Si es una
    // propuesta (persistida=false) no hay nada que destruir y se guarda directo.
    if (!this.persistida()) {
      this.enviar();
      return;
    }
    const lineas = [
      'Se sustituirá la jornada guardada por la de esta pantalla, en los cinco días.',
      'Solo es posible si no hay horarios, restricciones ni bloqueos que dependan de ella.',
    ];
    this.dialog
      .open<boolean, string[]>(ConfirmarReemplazo, { data: lineas })
      .closed.subscribe((confirmado) => {
        // backdrop y Escape emiten undefined: solo `true` estricto reemplaza.
        if (confirmado === true) {
          this.enviar();
        }
      });
  }

  private enviar(): void {
    this.guardando.set(true);
    this.error.set('');
    this.conflicto.set('');
    this.service.reemplazar(this.aRequest()).subscribe({
      next: (dto) => {
        this.aplicar(dto);
        this.modoSoloLectura.set(false);
        this.guardando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        if (err?.status === 409) {
          this.conflicto.set(this.textoConflicto(err));
          this.modoSoloLectura.set(true);
          this.form.disable();
        } else {
          // 400 = día tipo inválido (horas, solapes, techo de lectivos). Corregible aquí.
          this.error.set(this.mensaje(err, 'No se pudo guardar la jornada'));
        }
        this.guardando.set(false);
      },
    });
  }

  /** Vuelca un DTO recibido en el estado: badge de propuesta y filas del día tipo. */
  private aplicar(dto: JornadaDTO): void {
    this.persistida.set(dto.persistida);
    this.rellenar(this.primerDia(dto.tramos));
  }

  /**
   * De los 35 tramos que llegan, los del PRIMER día. Los cinco días son réplicas exactas
   * —lo garantiza la expansión del backend— así que uno cualquiera describe la jornada;
   * se toma el primero por ser el que encabeza el `orden` global.
   */
  private primerDia(tramos: TramoJornadaDTO[]): TramoJornadaDTO[] {
    if (tramos.length === 0) {
      return [];
    }
    const dia = tramos[0].dia;
    return tramos.filter((t) => t.dia === dia);
  }

  /** Rehace el FormArray con una fila por tramo del día tipo. */
  private rellenar(delDia: TramoJornadaDTO[]): void {
    this.tramos.clear();
    for (const t of delDia) {
      this.tramos.push(
        this.fb.nonNullable.group({
          horaInicio: this.fb.nonNullable.control(t.horaInicio, [
            Validators.required,
            Validators.pattern(Jornada.PATRON_HORA),
          ]),
          horaFin: this.fb.nonNullable.control(t.horaFin, [
            Validators.required,
            Validators.pattern(Jornada.PATRON_HORA),
          ]),
          esRecreo: this.fb.nonNullable.control(!t.esLectivo),
        }) as FilaTramo,
      );
    }
  }

  /**
   * Traduce el formulario al cuerpo del contrato. AQUÍ vive el recorte de la asimetría:
   * salen `horaInicio`, `horaFin` y `esLectivo` y NADA más —ni `dia`, ni `orden`, ni
   * `ordenEnDia`—, y `esRecreo` se invierte a `esLectivo`.
   */
  private aRequest(): JornadaRequest {
    return {
      tramos: this.tramos.controls.map((fila) => {
        const v = fila.getRawValue();
        return { horaInicio: v.horaInicio, horaFin: v.horaFin, esLectivo: !v.esRecreo };
      }),
    };
  }

  /**
   * Texto del 409. El cuerpo NO trae el desglose estructurado —`ReferenciaEntranteException`
   * expone `Referencia(referente, conteo)` en Java, pero el controlador solo pasa
   * `getMessage()` al `ResponseStatusException`, así que por la red viaja la cadena ya
   * compuesta («…referenciada por 3 sesiones de horario, 2 restricciones horarias»)—.
   * Se reutiliza esa cadena, que ya nombra QUÉ y CUÁNTO, y se le añade qué hacer.
   */
  private textoConflicto(err: HttpErrorResponse): string {
    const delServidor = this.mensaje(err, 'Hay datos que dependen de la jornada actual');
    return `${delServidor} Borra los horarios generados, las restricciones horarias y los bloqueos antes de reconfigurar la jornada.`;
  }

  /**
   * Traduce error Http a texto de usuario: mensaje del servidor primero (`message`, luego
   * `error`), degradado con status si no hay. Copiado del patrón de `aula-lista.mensaje()`
   * con texto propio; NO extraído a utilidad compartida a propósito: hacerlo tocaría
   * horario-view (D-F8.6, H1 cerrado).
   */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
