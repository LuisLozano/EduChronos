import { Component, OnInit, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormArray,
  FormControl,
  FormGroup,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { ActividadService } from '../../services/actividad.service';
import { AsignaturaService } from '../../services/asignatura.service';
import { AulaService } from '../../services/aula.service';
import { ProfesorService } from '../../services/profesor.service';
import { SubgrupoService } from '../../services/subgrupo.service';
import {
  Actividad,
  ActividadRequest,
  PATRONES_TEMPORALES,
  PlazaRequest,
} from '../../models/actividad.model';
import { Asignatura } from '../../models/asignatura.model';
import { Aula } from '../../models/aula.model';
import { Profesor } from '../../models/profesor.model';
import { Subgrupo } from '../../models/subgrupo.model';

/** Validator: un `string[]` debe tener al menos un elemento. `Validators.required` no
 *  sirve para esto: da por válido un array vacío (lo trata como valor presente).
 *  Molde de `SubgrupoForm`. */
function arrayNoVacio(control: AbstractControl): ValidationErrors | null {
  const v = control.value as unknown[];
  return Array.isArray(v) && v.length > 0 ? null : { arrayVacio: true };
}

/** Rama del XOR de aula que el usuario ha elegido. Control de UI: NO viaja al backend. */
export type ModoAula = 'FIJA' | 'CANDIDATAS';

/**
 * Controles de UNA plaza. Alias al estilo de `FilaTramo` (molde `Jornada`), para que el
 * `FormArray` esté tipado y el trozo B —lista variable— sea un delta y no una reescritura.
 */
type PlazaFila = FormGroup<{
  asignatura: FormControl<string>;
  modoAula: FormControl<ModoAula>;
  aulaFija: FormControl<string>;
  aulasCandidatas: FormControl<string[]>;
  profesores: FormControl<string[]>;
  subgrupos: FormControl<string[]>;
}>;

/**
 * Alta y edición de una ACTIVIDAD con UNA plaza, en diálogo. `DIALOG_DATA` es la
 * actividad a editar, o `null` para alta. Cierra con `true` si se guardó (la lista
 * recarga), con `undefined` si se canceló. Molde de `SubgrupoForm` para el multiselect
 * poblado por red y de `Jornada` para el `FormArray` y para el 400/409 por caminos
 * distintos.
 *
 * <p><b>TROZO A: la lista de plazas tiene longitud FIJA 1.</b> La fila nace DENTRO del
 * `FormArray` desde el principio, con su fábrica {@link #crearPlaza}, aunque hoy no haya
 * botones de añadir ni de eliminar: son el trozo B. Así el trozo B añade botones y quita
 * el tope, sin rehacer el formulario.
 *
 * <p><b>El XOR de aula se resuelve con un control de UI que no viaja.</b> `modoAula`
 * ('FIJA' | 'CANDIDATAS') es un radio que decide QUÉ RAMA está activa: al cambiar, la
 * rama inactiva se LIMPIA y pierde su validación —si no se limpiara, un aula elegida y
 * luego abandonada seguiría ahí y reaparecería al volver—. {@link #aPlazaRequest} traduce
 * el modo al contrato: FIJA → `aulaFija` con valor y `aulasCandidatas: []`; CANDIDATAS →
 * `aulaFija: null` y ≥1 candidata. Nunca las dos ramas, que es exactamente lo que el
 * backend rechaza con 400.
 *
 * <p><b>Los subgrupos NO llevan validador, a propósito.</b> El contrato del backend
 * ACEPTA una plaza con cero subgrupos (`validarPlazas` no lo comprueba; solo el I2 cruza
 * los que haya). Exigir aquí ≥1 sería inventar una regla que el contrato no tiene y
 * divergir del servidor: el formulario rechazaría cuerpos que la API acepta. Queda
 * anotado como deuda **D-plaza-sin-subgrupos**: si el dominio decide que una plaza sin
 * población no tiene sentido, la regla se añade EN EL BACKEND y este validador la sigue,
 * no al revés.
 *
 * <p><b>Las aulas NO se filtran por compatibilidad I3.</b> Decisión tomada: el select
 * ofrece todas las aulas y se deja hablar al 400 del backend, que ya nombra asignatura,
 * aula, tipo del aula y tipos compatibles. Replicar la tabla de compatibilidades en
 * cliente sería una segunda fuente de verdad que se desincroniza sola.
 *
 * <p><b>400 y 409 son sucesos distintos y van por caminos distintos</b> (molde `Jornada`):
 * el 400 es «corrige el formulario» y va inline con el mensaje accionable del backend; el
 * 409 no se arregla tocando el formulario —hay horario o bloqueos colgando— así que va en
 * aviso rojo aparte, con el desglose y qué hay que borrar antes. El 409 solo puede venir
 * del PUT: un alta no tiene dependientes.
 */
@Component({
  selector: 'app-actividad-form',
  imports: [ReactiveFormsModule],
  templateUrl: './actividad-form.html',
  styleUrl: './actividad-form.css',
})
export class ActividadForm implements OnInit {
  private readonly service = inject(ActividadService);
  private readonly asignaturaService = inject(AsignaturaService);
  private readonly aulaService = inject(AulaService);
  private readonly profesorService = inject(ProfesorService);
  private readonly subgrupoService = inject(SubgrupoService);
  private readonly fb = inject(FormBuilder);
  protected readonly ref = inject<DialogRef<boolean>>(DialogRef);
  private readonly editando = inject<Actividad | null>(DIALOG_DATA);

  protected readonly guardando = signal(false);
  /** Error de carga o 400 de guardado. Inline junto al formulario. Vacío = sin error. */
  protected readonly error = signal('');
  /** Texto del 409 del PUT. No vacío ⇒ aviso rojo aparte, el formulario no lo arregla. */
  protected readonly conflicto = signal('');
  protected readonly esEdicion = this.editando !== null;

  /**
   * `true` si llegó una actividad con MÁS de una plaza. La lista ya deshabilita ese
   * botón, así que esto no debería pasar; si pasa, NO se pinta el formulario a medias
   * —pintarlo invitaría a guardar una plaza y borrar el resto— sino solo el aviso.
   */
  protected readonly multiplaza = signal(false);

  /** Opciones de los desplegables, en el orden en que llegan del backend. */
  protected readonly asignaturas = signal<Asignatura[]>([]);
  protected readonly aulas = signal<Aula[]>([]);
  protected readonly profesores = signal<Profesor[]>([]);
  protected readonly subgrupos = signal<Subgrupo[]>([]);

  protected readonly patrones = PATRONES_TEMPORALES;

  protected readonly form = this.fb.nonNullable.group({
    codigo: ['', Validators.required],
    /** '' = sin asignatura de actividad; {@link #aRequest} lo traduce a `null`. */
    asignatura: [''],
    duracionTramos: [1, [Validators.required, Validators.min(1)]],
    repeticionesPorSemana: [1, [Validators.required, Validators.min(1)]],
    patronTemporal: ['NEUTRA', Validators.required],
    requiereTutor: [false],
    plazas: this.fb.nonNullable.array<PlazaFila>([]),
  });

  protected get plazas(): FormArray<PlazaFila> {
    return this.form.controls.plazas;
  }

  constructor() {
    const editando = this.editando;
    if (editando && editando.plazas.length > 1) {
      this.multiplaza.set(true);
      return;   // ni fila ni precarga: no hay formulario que pintar.
    }
    this.plazas.push(this.crearPlaza());
    if (editando) {
      this.precargar(editando);
    }
  }

  /**
   * FÁBRICA de una fila de plaza. Toda fila nace por aquí —hoy una, en el trozo B las que
   * el usuario pida— con sus validadores en el estado que corresponde al modo inicial
   * ('FIJA'), y con la suscripción que mantiene el XOR al cambiar de rama.
   *
   * <p>La suscripción va en la fábrica y no en un `(change)` de la plantilla para que el
   * invariante se sostenga también cuando el modo se cambia por código (precarga en
   * edición, y los tests).
   */
  private crearPlaza(): PlazaFila {
    const fila = this.fb.nonNullable.group({
      asignatura: ['', Validators.required],
      modoAula: ['FIJA' as ModoAula, Validators.required],
      aulaFija: ['', Validators.required],
      aulasCandidatas: [[] as string[]],
      profesores: [[] as string[], arrayNoVacio],
      // subgrupos SIN validador: ver D-plaza-sin-subgrupos en el javadoc de la clase.
      subgrupos: [[] as string[]],
    }) as PlazaFila;
    fila.controls.modoAula.valueChanges.subscribe(() => this.aplicarModo(fila));
    return fila;
  }

  /**
   * Mantiene el XOR vivo en el formulario: la rama ACTIVA exige valor, la INACTIVA se
   * vacía y deja de validar. Vaciar no es cosmético —sin ello, elegir un aula fija,
   * cambiar a candidatas y guardar dejaría el aula fija elegida en el control, lista para
   * reaparecer si el usuario vuelve, y el formulario mostraría dos ramas rellenas cuando
   * el contrato solo admite una—.
   */
  private aplicarModo(fila: PlazaFila): void {
    const fija = fila.controls.modoAula.value === 'FIJA';
    if (fija) {
      fila.controls.aulasCandidatas.setValue([]);
      fila.controls.aulasCandidatas.clearValidators();
      fila.controls.aulaFija.setValidators(Validators.required);
    } else {
      fila.controls.aulaFija.setValue('');
      fila.controls.aulaFija.clearValidators();
      fila.controls.aulasCandidatas.setValidators(arrayNoVacio);
    }
    fila.controls.aulaFija.updateValueAndValidity();
    fila.controls.aulasCandidatas.updateValueAndValidity();
  }

  /**
   * Vuelca la actividad a editar en el formulario. El `modoAula` se DERIVA del dato: si
   * la plaza trae `aulaFija`, la rama es FIJA; si no, CANDIDATAS. Se asigna ANTES que las
   * aulas para que {@link #aplicarModo} haya dejado la rama correcta activa cuando llega
   * el valor.
   */
  private precargar(actividad: Actividad): void {
    this.form.patchValue({
      codigo: actividad.codigo,
      asignatura: actividad.asignatura ?? '',
      duracionTramos: actividad.duracionTramos,
      repeticionesPorSemana: actividad.repeticionesPorSemana,
      patronTemporal: actividad.patronTemporal,
      requiereTutor: actividad.requiereTutor,
    });
    const plaza = actividad.plazas[0];
    if (!plaza) {
      return;
    }
    const fila = this.plazas.at(0);
    fila.controls.modoAula.setValue(plaza.aulaFija !== null ? 'FIJA' : 'CANDIDATAS');
    fila.controls.asignatura.setValue(plaza.asignatura);
    fila.controls.aulaFija.setValue(plaza.aulaFija ?? '');
    fila.controls.aulasCandidatas.setValue([...plaza.aulasCandidatas]);
    fila.controls.profesores.setValue([...plaza.profesores]);
    fila.controls.subgrupos.setValue([...plaza.subgrupos]);
  }

  /**
   * Puebla los CUATRO desplegables. Un fallo en cualquiera se presenta en el mismo hueco
   * que los errores de guardado: sin catálogo el formulario no puede completarse.
   *
   * <p>No se piden si llegó una actividad multiplaza: no hay formulario que poblar.
   */
  ngOnInit(): void {
    if (this.multiplaza()) {
      return;
    }
    this.asignaturaService.listar().subscribe({
      next: (lista) => this.asignaturas.set(lista),
      error: (err: HttpErrorResponse) =>
        this.error.set(this.mensaje(err, 'No se pudieron cargar las asignaturas')),
    });
    this.profesorService.listar().subscribe({
      next: (lista) => this.profesores.set(lista),
      error: (err: HttpErrorResponse) =>
        this.error.set(this.mensaje(err, 'No se pudieron cargar los profesores')),
    });
    this.aulaService.listar().subscribe({
      next: (lista) => this.aulas.set(lista),
      error: (err: HttpErrorResponse) =>
        this.error.set(this.mensaje(err, 'No se pudieron cargar las aulas')),
    });
    this.subgrupoService.listar().subscribe({
      next: (lista) => this.subgrupos.set(lista),
      error: (err: HttpErrorResponse) =>
        this.error.set(this.mensaje(err, 'No se pudieron cargar los subgrupos')),
    });
  }

  /**
   * Lee la selección de un `<select multiple>` y la vuelca al control indicado de la
   * fila. Sustituye a la vinculación automática que el `<select>` único tiene por
   * `formControlName` (molde `SubgrupoForm`, extendido para elegir el control).
   */
  protected alSeleccionar(
    event: Event,
    fila: PlazaFila,
    campo: 'aulasCandidatas' | 'profesores' | 'subgrupos',
  ): void {
    const select = event.target as HTMLSelectElement;
    const codigos = Array.from(select.selectedOptions).map((o) => o.value);
    fila.controls[campo].setValue(codigos);
    fila.controls[campo].markAsTouched();
  }

  /** Códigos seleccionados de un multiselect (el `<select multiple>` no refleja el valor
   *  por reconciliación: el HTML pinta `[selected]` comparando contra esto). */
  protected seleccionados(
    fila: PlazaFila,
    campo: 'aulasCandidatas' | 'profesores' | 'subgrupos',
  ): string[] {
    return fila.controls[campo].value;
  }

  protected guardar(): void {
    if (this.multiplaza()) {
      return;   // no se guarda lo que no se ha pintado.
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.error.set('');
    this.conflicto.set('');
    const req = this.aRequest();
    const peticion = this.editando
      ? this.service.editar(this.editando.id, req)
      : this.service.crear(req);
    peticion.subscribe({
      next: () => this.ref.close(true),
      error: (err: HttpErrorResponse) => {
        if (err?.status === 409) {
          this.conflicto.set(this.textoConflicto(err));
        } else {
          // 400 = validación del backend (XOR, I7, I2, I3, código duplicado o no
          // resoluble). El texto que compone es accionable y viaja en `message`.
          this.error.set(this.mensaje(err, 'No se pudo guardar la actividad'));
        }
        this.guardando.set(false);
      },
    });
  }

  protected cancelar(): void {
    this.ref.close();
  }

  /**
   * Traduce el formulario al cuerpo del contrato. AQUÍ se sueltan los controles de UI:
   * `modoAula` NO viaja, y la asignatura vacía de la actividad se traduce a `null` —el
   * contrato distingue «sin asignatura propia» (null) de una cadena vacía, que sería un
   * código no resoluble y un 400—.
   */
  private aRequest(): ActividadRequest {
    const v = this.form.getRawValue();
    return {
      codigo: v.codigo,
      asignatura: v.asignatura === '' ? null : v.asignatura,
      duracionTramos: v.duracionTramos,
      repeticionesPorSemana: v.repeticionesPorSemana,
      patronTemporal: v.patronTemporal,
      requiereTutor: v.requiereTutor,
      plazas: this.plazas.controls.map((fila) => this.aPlazaRequest(fila)),
    };
  }

  /** Una fila → `PlazaRequest`, resolviendo el XOR por el modo elegido. Sin `id` ni
   *  `codigo`: el código de plaza lo deriva el backend. */
  private aPlazaRequest(fila: PlazaFila): PlazaRequest {
    const v = fila.getRawValue();
    const fija = v.modoAula === 'FIJA';
    return {
      asignatura: v.asignatura,
      aulaFija: fija ? v.aulaFija : null,
      aulasCandidatas: fija ? [] : v.aulasCandidatas,
      profesores: v.profesores,
      subgrupos: v.subgrupos,
    };
  }

  /**
   * Texto del 409. El cuerpo trae la cadena ya compuesta por el backend
   * («No se puede editar: referenciada por 1 sesion(es), 1 aula(s) bloqueada(s)»), que
   * ya nombra QUÉ y CUÁNTO; se le añade qué hacer para salir de ahí.
   */
  private textoConflicto(err: HttpErrorResponse): string {
    const delServidor = this.mensaje(err, 'Hay datos que dependen de esta actividad');
    return `${delServidor} Borra el horario generado y los bloqueos que dependen de ella antes de editarla.`;
  }

  /** Traduce error Http a texto de usuario. Mismo patrón que O-catálogo, copiado con
   *  texto propio (no compartido: tocaría H1). */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
