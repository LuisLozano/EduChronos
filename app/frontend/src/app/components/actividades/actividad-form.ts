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
  Plaza,
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

/**
 * Validator de ARRAY (I2): ningún subgrupo aparece en dos plazas de la misma actividad.
 * Devuelve el CÓDIGO del primer repetido para que el mensaje pueda nombrarlo.
 *
 * <p><b>Replica la semántica del backend, con sus dos rarezas, a propósito.</b>
 * `ActividadService.validarPlazas` cruza los códigos con un `HashSet<String>`:
 * (1) NO normaliza mayúsculas —dos códigos que solo difieren en caja son subgrupos
 * distintos, y así se quedan aquí—; (2) deduplica DENTRO de cada plaza antes de cruzar,
 * así que un subgrupo repetido en la MISMA plaza no es error (de ahí el `Set` por fila).
 * Añadir aquí un `toLowerCase()` «por robustez» haría que el formulario rechazara cuerpos
 * que la API acepta: el mismo error de fondo que D-plaza-sin-subgrupos. El 400 del backend
 * («el subgrupo X aparece en mas de una plaza de la actividad») sigue siendo la red.
 */
function subgruposDisjuntos(array: AbstractControl): ValidationErrors | null {
  const vistos = new Set<string>();
  for (const fila of (array as FormArray).controls) {
    for (const codigo of new Set(fila.get('subgrupos')!.value as string[])) {
      if (vistos.has(codigo)) {
        return { subgrupoRepetido: codigo };
      }
      vistos.add(codigo);
    }
  }
  return null;
}

/** Rama del XOR de aula que el usuario ha elegido. Control de UI: NO viaja al backend. */
export type ModoAula = 'FIJA' | 'CANDIDATAS';

/**
 * Controles de UNA plaza. Alias al estilo de `FilaTramo` (molde `Jornada`), para que el
 * `FormArray` esté tipado.
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
 * Alta y edición de una ACTIVIDAD con N plazas, en diálogo. `DIALOG_DATA` es la actividad
 * a editar, o `null` para alta. Cierra con `true` si se guardó (la lista recarga), con
 * `undefined` si se canceló. Molde de `SubgrupoForm` para los multiselects poblados por
 * red y de `Jornada` para el `FormArray` y para el 400/409 por caminos distintos.
 *
 * <p><b>LISTA DE PLAZAS VARIABLE (trozo B).</b> El usuario añade y quita filas. Toda fila
 * nace por la fábrica {@link #crearPlaza}, incluidas las de la precarga: no se clona la
 * anterior ni se reutiliza una existente. El MÍNIMO es una plaza, porque el contrato lo
 * exige (400 «una actividad necesita al menos una plaza»); no hay máximo, porque el
 * contrato tampoco lo tiene.
 *
 * <p><b>Quitar una fila intermedia mueve contenidos entre plazas vivas.</b> La
 * reconciliación del PUT es POSICIONAL: el backend empareja la fila i del cuerpo con la
 * plaza i existente (ordenadas por id), actualiza las comunes, borra las sobrantes por el
 * final y crea las que falten. Es seguro porque el código de plaza es interno e inestable
 * Y porque el PUT rechaza con 409 si hay sesiones o bloqueos colgando: cuando se
 * reconcilia, no hay nadie apuntando por id a esas filas.
 *
 * <p><b>El XOR de aula se resuelve POR FILA</b> con un control de UI que no viaja.
 * `modoAula` ('FIJA' | 'CANDIDATAS') decide qué rama está activa; al cambiar, la rama
 * inactiva se LIMPIA y pierde su validación. La suscripción vive en la fábrica, con la
 * fila capturada en el closure, para que cada fila gobierne SOLO la suya.
 *
 * <p><b>Los subgrupos NO llevan validador de fila, a propósito.</b> El contrato ACEPTA
 * una plaza con cero subgrupos. Exigir aquí ≥1 sería inventar una regla que el contrato
 * no tiene. Queda anotado como deuda **D-plaza-sin-subgrupos**: si el dominio decide que
 * una plaza sin población no tiene sentido, la regla se añade EN EL BACKEND y este
 * formulario la sigue, no al revés. Lo que sí se valida entre filas es I2, que el backend
 * sí tiene (ver {@link subgruposDisjuntos}).
 *
 * <p><b>Las aulas NO se filtran por compatibilidad I3.</b> Decisión tomada: el select
 * ofrece todas las aulas y habla el 400 del backend, que ya nombra asignatura, aula, tipo
 * del aula y tipos compatibles. Replicar la tabla de compatibilidades en cliente sería
 * una segunda fuente de verdad que se desincroniza sola.
 *
 * <p><b>400 y 409 son sucesos distintos y van por caminos distintos</b> (molde `Jornada`):
 * el 400 es «corrige el formulario» y va inline; el 409 no se arregla tocando el
 * formulario —hay horario o bloqueos colgando— así que va en aviso rojo aparte. El 409
 * solo puede venir del PUT: un alta no tiene dependientes.
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
    // El validador de I2 se instala EN LA CONSTRUCCIÓN del array: instalarlo después con
    // setValidators exigiría un updateValueAndValidity explícito y deja una ventana en la
    // que el array existe sin la regla.
    plazas: this.fb.nonNullable.array<PlazaFila>([], subgruposDisjuntos),
  });

  protected get plazas(): FormArray<PlazaFila> {
    return this.form.controls.plazas;
  }

  constructor() {
    const editando = this.editando;
    if (editando) {
      this.precargar(editando);
    } else {
      this.plazas.push(this.crearPlaza());
    }
  }

  /**
   * FÁBRICA de una fila de plaza. TODA fila nace por aquí —el alta, la precarga y el
   * botón de añadir— con sus validadores en el estado que corresponde al modo inicial
   * ('FIJA') y con la suscripción que mantiene el XOR al cambiar de rama.
   *
   * <p>La suscripción va en la fábrica y no en un `(change)` de la plantilla para que el
   * invariante se sostenga también cuando el modo se cambia por código (la precarga, y
   * los tests), y captura ESTA fila: cada fila gobierna solo la suya.
   */
  private crearPlaza(): PlazaFila {
    const fila = this.fb.nonNullable.group({
      asignatura: ['', Validators.required],
      modoAula: ['FIJA' as ModoAula, Validators.required],
      aulaFija: ['', Validators.required],
      aulasCandidatas: [[] as string[]],
      profesores: [[] as string[], arrayNoVacio],
      // subgrupos SIN validador de fila: ver D-plaza-sin-subgrupos en el javadoc de la
      // clase. La regla que SÍ existe (I2) es del array, no de la fila.
      subgrupos: [[] as string[]],
    }) as PlazaFila;
    fila.controls.modoAula.valueChanges.subscribe(() => this.aplicarModo(fila));
    return fila;
  }

  /**
   * Mantiene el XOR vivo EN SU FILA: la rama ACTIVA exige valor, la INACTIVA se vacía y
   * deja de validar. Vaciar no es cosmético —sin ello, elegir un aula fija, cambiar a
   * candidatas y guardar dejaría el aula fija en el control, lista para reaparecer, y el
   * formulario tendría las dos ramas rellenas cuando el contrato solo admite una—.
   *
   * <p><b>Sobre `{ onlySelf: true }` en estos `setValue`.</b> No se usa, aunque en esta
   * implementación concreta sería inocuo: las dos llamadas siguientes a
   * `updateValueAndValidity()` van sin opciones y propagan al padre igualmente, así que
   * el array se revalidaría de todas formas. Se evita porque la inocuidad depende de esas
   * dos líneas: quien las cambie o las reordene se llevaría por delante la revalidación
   * de I2 sin que ningún test se entere (medido: la campaña de mutación de S110 no lo
   * detecta, porque I2 depende de `subgrupos` y este método no lo toca).
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

  /** Añade una fila VACÍA al final. Nace por la fábrica: no hereda nada de la anterior. */
  protected anadirPlaza(): void {
    this.plazas.push(this.crearPlaza());
  }

  /**
   * Quita la fila indicada. La guarda del mínimo vive AQUÍ además de en el `[disabled]`
   * del botón: el atributo es presentación, y este método es la puerta real por la que se
   * llegaría a un cuerpo sin plazas (400 del backend).
   */
  protected quitarPlaza(indice: number): void {
    if (this.plazas.length <= 1) {
      return;
    }
    this.plazas.removeAt(indice);
  }

  /**
   * Vuelca la actividad a editar en el formulario, RECONSTRUYENDO el array con una fila
   * por plaza del dato (molde `Jornada.rellenar`: `clear()` + `push` en bucle).
   *
   * <p>Las filas retiradas por `clear()` conservan su suscripción a `modoAula`, y eso NO
   * fuga: la referencia va de la fila huérfana al componente, nunca al revés, y el array
   * no las retiene. La condición de que siga siendo así es no guardar punteros a filas
   * retiradas —un futuro «deshacer quitar plaza» mantendría viva la isla y podría
   * revalidar el formulario vivo desde fuera—.
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
    this.plazas.clear();
    for (const plaza of actividad.plazas) {
      const fila = this.crearPlaza();
      this.plazas.push(fila);
      this.volcar(fila, plaza);
    }
    if (this.plazas.length === 0) {
      // El contrato no permite una actividad sin plazas; si llegara, el formulario abre
      // con una fila vacía en vez de con ninguna, que no sería editable.
      this.plazas.push(this.crearPlaza());
    }
  }

  /**
   * Vuelca UNA plaza en SU fila. El `modoAula` se DERIVA del dato (si trae `aulaFija`, la
   * rama es FIJA) y se asigna PRIMERO, por defensa: {@link #aplicarModo} limpia la rama
   * contraria, y asignarlo al final la limpiaría después de escribirla.
   *
   * <p>Con datos que respetan el contrato ese peligro no se materializa —el XOR garantiza
   * que la rama contraria ya viene vacía, así que limpiarla no borra nada—, y por eso el
   * orden NO está cubierto por ningún test: mover esta línea al final es un mutante
   * equivalente, comprobado en la campaña de S110. Se conserva el orden porque deja de ser
   * equivalente en cuanto el backend devuelva una plaza con las dos ramas llenas.
   */
  private volcar(fila: PlazaFila, plaza: Plaza): void {
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
   */
  ngOnInit(): void {
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

  /** Código del subgrupo que I2 ha encontrado repetido, o '' si no hay repetición. */
  protected get subgrupoRepetido(): string {
    return (this.plazas.errors?.['subgrupoRepetido'] as string | undefined) ?? '';
  }

  /**
   * Lee la selección de un `<select multiple>` y la vuelca al control indicado de la
   * fila. Sustituye a la vinculación automática que el `<select>` único tiene por
   * `formControlName` (molde `SubgrupoForm`, extendido para elegir fila y control).
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
    if (this.form.invalid) {
      // Incluye el error de I2: el validador del array pone `form.invalid` a true, así
      // que esta misma guarda corta el envío sin comprobación aparte.
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
   * código no resoluble y un 400—. El ORDEN de las plazas es el de la pantalla, y ese
   * orden es significativo: la reconciliación del PUT es posicional.
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
   *  texto propio (no compartido: tocaría horario-view). */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
