import {
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
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
import { GrupoService } from '../../services/grupo.service';
import { NivelService } from '../../services/nivel.service';
import { ProfesorService } from '../../services/profesor.service';
import { SubgrupoService } from '../../services/subgrupo.service';
import {
  Actividad,
  ActividadRequest,
  ETIQUETA_TIPO_ACTIVIDAD,
  PATRONES_TEMPORALES,
  Plaza,
  PlazaRequest,
  TipoActividad,
} from '../../models/actividad.model';
import { Asignatura } from '../../models/asignatura.model';
import { Aula } from '../../models/aula.model';
import { Grupo } from '../../models/grupo.model';
import { Nivel } from '../../models/nivel.model';
import { Profesor } from '../../models/profesor.model';
import { Subgrupo } from '../../models/subgrupo.model';
import { SelectorFiltrable } from '../selector-filtrable/selector-filtrable';
import { ListaDoble } from '../lista-doble/lista-doble';

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

/**
 * Plazas implicadas en I2 (S230, condición 2 de O-acabado-pre-prueba): las que contienen algún
 * subgrupo que aparece también en otra. Misma semántica que {@link subgruposDisjuntos} —un `Set`
 * por plaza, sin normalizar la caja—, para que la pestaña marcada y el aviso digan lo mismo. Se
 * calcula aparte y no dentro del validador: el validador decide si se envía, esto solo marca.
 */
export function plazasConSubgrupoRepetido(
  subgruposPorPlaza: readonly (readonly string[])[],
): number[] {
  const plazasDe = new Map<string, number>();
  for (const subgrupos of subgruposPorPlaza) {
    for (const codigo of new Set(subgrupos)) {
      plazasDe.set(codigo, (plazasDe.get(codigo) ?? 0) + 1);
    }
  }
  return subgruposPorPlaza.flatMap((subgrupos, i) =>
    subgrupos.some((codigo) => (plazasDe.get(codigo) ?? 0) > 1) ? [i] : [],
  );
}

/**
 * Opciones de un selector de una CLASE filtradas por su nivel (S251, C-editor-por-nivel, condición 4
 * de O-actividad-por-nivel): una asignatura entra si es de ese nivel; un subgrupo, si TODOS sus grupos
 * lo son (`nivelDeGrupo` da el nivel de cada grupo por código). Con `todos` («Mostrar todos los
 * niveles») no filtra; sin nivel, no entra ninguna (decisión EY).
 *
 * <p><b>Lo ya elegido en el control sigue entre las opciones</b> aunque no sea del nivel (decisión
 * FA, enmienda E8): la lista doble suelta al primer cambio un código que no esté entre sus opciones, y
 * el select se quedaría en blanco. Cambiar de nivel no borra nada; la mezcla que quede la avisa la
 * prevalidación. Conserva el orden de entrada, que es el del backend.
 */
export function opcionesDelNivel<T extends Asignatura | Subgrupo>(
  opciones: readonly T[],
  nivel: string,
  nivelDeGrupo: ReadonlyMap<string, string>,
  todos: boolean,
  elegidos: readonly string[],
): T[] {
  if (todos) {
    return [...opciones];
  }
  const elegido = new Set(elegidos);
  return opciones.filter(
    (o) => elegido.has(o.codigo) || (nivel !== '' && esDelNivel(o, nivel, nivelDeGrupo)),
  );
}

/** Si una asignatura es del nivel, o si todos los grupos de un subgrupo lo son. */
function esDelNivel(
  o: Asignatura | Subgrupo,
  nivel: string,
  nivelDeGrupo: ReadonlyMap<string, string>,
): boolean {
  return 'grupos' in o
    ? o.grupos.length > 0 && o.grupos.every((grupo) => nivelDeGrupo.get(grupo) === nivel)
    : o.nivel === nivel;
}

/**
 * Nivel de una CLASE que se edita (S251, decisión EZ): el de los grupos de los subgrupos de todas sus
 * plazas si es uno solo; '' si son varios o ninguno, y entonces el formulario marca «Mostrar todos los
 * niveles». Un subgrupo o un grupo que no está en las listas no aporta nivel.
 */
export function nivelDeLasPlazas(
  subgruposPorPlaza: readonly (readonly string[])[],
  subgrupos: readonly Subgrupo[],
  nivelDeGrupo: ReadonlyMap<string, string>,
): string {
  const gruposDe = new Map(subgrupos.map((s) => [s.codigo, s.grupos]));
  const niveles = new Set<string>();
  for (const plaza of subgruposPorPlaza) {
    for (const codigo of plaza) {
      for (const grupo of gruposDe.get(codigo) ?? []) {
        const nivel = nivelDeGrupo.get(grupo);
        if (nivel !== undefined) {
          niveles.add(nivel);
        }
      }
    }
  }
  return niveles.size === 1 ? [...niveles][0] : '';
}

/** Diálogos de actividad creados, para que los ids de sus controles no se repitan. */
let instancias = 0;

/** Rama de aula que el usuario ha elegido. Control de UI: NO viaja al backend. NINGUNA es la
 *  plaza sin aula escrita: en una CLASE, «La elige el generador» (S207); en una reunión o una
 *  función, «Sin aula» (S201). Se envía igual en los dos casos. */
export type ModoAula = 'FIJA' | 'CANDIDATAS' | 'NINGUNA';

/** Con qué cierra el diálogo un guardado correcto (S227): el id de la actividad guardada, para
 *  que la lista pueda decir si ha dejado de cumplir su búsqueda. Cancelar cierra sin valor. */
export interface ActividadGuardada {
  id: number;
}

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
 * `undefined` si se canceló. Molde de `SubgrupoForm` para las selecciones múltiples
 * pobladas por red y de `Jornada` para el `FormArray` y para el 400/409 por caminos distintos.
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
 * <p><b>El aula se resuelve POR FILA</b> con un control de UI que no viaja.
 * `modoAula` ('FIJA' | 'CANDIDATAS' | 'NINGUNA') decide qué rama está activa; al cambiar,
 * las inactivas se LIMPIAN y pierden su validación. NINGUNA se ofrece en todos los tipos, con
 * etiqueta según el tipo ({@link #etiquetaSinAula}): en una CLASE el aula la elige el generador
 * con las reglas de aulas (S207); fuera, la plaza no ocupa aula (S201). La suscripción vive en la
 * fábrica, con la fila capturada en el closure, para que cada fila gobierne SOLO la suya.
 *
 * <p><b>Los subgrupos dependen del TIPO</b> (S201, {@link #aplicarTipo}). El contrato exige
 * al menos un subgrupo por plaza en una CLASE (salda D-plaza-sin-subgrupos) y ninguno en una
 * REUNIÓN o una FUNCIÓN, y el formulario lo refleja: en una CLASE la fila exige subgrupos; en
 * las otras dos ni se pintan y se envían vacíos. Lo que se valida entre filas es I2 (ver
 * {@link subgruposDisjuntos}).
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
 *
 * <p><b>Disposición de S230</b> (C-editor-plazas, condición 2 de O-acabado-pre-prueba; maqueta
 * de `s229/f1b`): tres filas y UNA PESTAÑA POR PLAZA, con solo la plaza activa pintada
 * ({@link activa}). «+» añade y activa la nueva; «Quitar esta plaza» deja activa la vecina.
 * Profesores, subgrupos y aulas candidatas se eligen con {@link ListaDoble}. Una pestaña lleva
 * un punto rojo si su plaza está en I2 ({@link plazasConI2}) o si tiene un campo inválido
 * ({@link plazaConErrores}, regla de F4): sin él, un error en otra pestaña dejaría «Guardar» sin
 * explicación. El aviso de I2 va arriba del panel, a la vista sin desplazarse.
 *
 * <p><b>El nivel va primero</b> (S251, C-editor-por-nivel, condición 4 de O-actividad-por-nivel). En
 * una CLASE, la fila 1 lleva el nivel y la casilla «Mostrar todos los niveles», que no viajan. Las
 * asignaturas y los subgrupos se filtran por él con {@link opcionesDelNivel}; en el alta es
 * obligatorio y, sin él, los selectores quedan vacíos; en la edición se deduce de los subgrupos
 * ({@link nivelDeLasPlazas}).
 */
@Component({
  selector: 'app-actividad-form',
  imports: [ReactiveFormsModule, SelectorFiltrable, ListaDoble],
  templateUrl: './actividad-form.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './actividad-form.css',
})
export class ActividadForm implements OnInit {
  private readonly service = inject(ActividadService);
  private readonly asignaturaService = inject(AsignaturaService);
  private readonly aulaService = inject(AulaService);
  private readonly profesorService = inject(ProfesorService);
  private readonly subgrupoService = inject(SubgrupoService);
  private readonly nivelService = inject(NivelService);
  private readonly grupoService = inject(GrupoService);
  private readonly fb = inject(FormBuilder);
  protected readonly ref = inject<DialogRef<ActividadGuardada>>(DialogRef);
  private readonly editando = inject<Actividad | null>(DIALOG_DATA);

  protected readonly guardando = signal(false);
  /** Error de carga o 400 de guardado. Inline junto al formulario. Vacío = sin error. */
  protected readonly error = signal('');
  /** Texto del 409 del PUT. No vacío ⇒ aviso rojo aparte, el formulario no lo arregla. */
  protected readonly conflicto = signal('');
  /**
   * Si {@link error} viene de GUARDAR (S226, condición 8 de O-interfaz). Solo ese se borra al
   * editar: habla del formulario que el usuario está corrigiendo. Un fallo de CARGA no lo
   * arregla editar —el desplegable sigue vacío— y se queda.
   */
  private errorDeGuardado = false;
  protected readonly esEdicion = this.editando !== null;

  /** Opciones de los desplegables, en el orden en que llegan del backend. */
  protected readonly asignaturas = signal<Asignatura[]>([]);
  protected readonly aulas = signal<Aula[]>([]);
  protected readonly profesores = signal<Profesor[]>([]);
  protected readonly subgrupos = signal<Subgrupo[]>([]);
  /** Niveles, ya en orden pedagógico, y grupos, que dan el nivel de cada subgrupo (S251). */
  protected readonly niveles = signal<Nivel[]>([]);
  private readonly grupos = signal<Grupo[]>([]);
  private readonly nivelDeGrupo = computed(
    () => new Map(this.grupos().map((g) => [g.codigo, g.nivel])),
  );
  /** Han llegado las dos listas que la deducción del nivel necesita en la edición. */
  private subgruposCargados = false;
  private gruposCargados = false;

  protected readonly patrones = PATRONES_TEMPORALES;

  /** Filas visibles de las listas dobles de este diálogo (maqueta de `s229/f1b`). */
  protected readonly filasLista = 5;

  /** Plaza cuya pestaña está abierta (S230): es la única que se pinta. */
  protected readonly activa = signal(0);

  /** Si ya se ha pulsado «Guardar» alguna vez (S230-F4, R1): desde entonces se marca toda plaza
   *  inválida, la abierta incluida. Señal porque la marca se pinta desde ella (zoneless). */
  private readonly intentoDeGuardar = signal(false);

  /** Prefijo de los ids de este diálogo (S230, decisión 11): `id` y `label for` únicos. */
  private readonly prefijoId = `actividad-form-${++instancias}`;

  /** Opciones del select de tipo: el código que viaja y su texto visible. */
  protected readonly tipos = Object.entries(ETIQUETA_TIPO_ACTIVIDAD) as [TipoActividad, string][];

  /** Accesores del filtro de selectores (S185): código y texto visible de cada opción. */
  protected readonly codigoDe = (x: { codigo: string }): string => x.codigo;
  protected readonly textoAula = (a: Aula): string => `${a.codigo} (${a.tipo})`;
  /**
   * Texto de una asignatura en los dos selectores (S230, condición 4 de O-acabado-pre-prueba):
   * «código — nombre», como {@link textoProfesor}. Es también lo que recorre su filtro, así que
   * se encuentra por el nombre. Sin nombre, solo el código.
   */
  protected readonly textoAsignatura = (a: Asignatura): string =>
    a.nombreCompleto?.trim() ? `${a.codigo} — ${a.nombreCompleto}` : a.codigo;
  /**
   * Texto de un profesor en el selector de la plaza (S227, condición 10): el código solo no
   * dice quién es. Es también lo que recorre el filtro, así que se encuentra por nombre. La
   * identidad sigue siendo {@link codigoDe}: el valor que viaja y el de la elegida.
   */
  protected readonly textoProfesor = (p: Profesor): string =>
    p.nombreCompleto?.trim() ? `${p.codigo} — ${p.nombreCompleto}` : p.codigo;

  protected readonly form = this.fb.nonNullable.group({
    codigo: ['', Validators.required],
    tipo: ['CLASE' as TipoActividad, Validators.required],
    /** S251: controles de UI que NO viajan, como `modoAula`. El nivel de la CLASE ('' = sin
     *  elegir), que filtra asignaturas y subgrupos, y la casilla «Mostrar todos los niveles».
     *  Su validador lo pone {@link #aplicarTipo}. */
    nivel: [''],
    todosLosNiveles: [false],
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

  /**
   * Valor del formulario como señal (S251): lo que eligen los filtros de nivel. Se copia en cada
   * cambio, así que las opciones derivadas son `computed` y no se rehacen en cada pintado.
   */
  private readonly valor = signal(this.form.getRawValue());

  /** El filtro de nivel se aplica en una CLASE sin «Mostrar todos los niveles». */
  private readonly filtraPorNivel = computed(
    () => this.valor().tipo === 'CLASE' && !this.valor().todosLosNiveles,
  );

  /** Una CLASE sin nivel y sin «Mostrar todos»: los selectores quedan vacíos y se dice por qué (EY). */
  protected readonly faltaNivel = computed(
    () => this.filtraPorNivel() && this.valor().nivel === '',
  );

  /** Opciones de la asignatura de la actividad. */
  protected readonly asignaturasDeLaActividad = computed(() =>
    this.delNivel(this.asignaturas(), [this.valor().asignatura]),
  );

  /** Opciones de asignatura y de subgrupos de cada plaza, con lo elegido en ESA plaza (E8). */
  protected readonly opcionesDePlazas = computed(() =>
    this.valor().plazas.map((plaza) => ({
      asignaturas: this.delNivel(this.asignaturas(), [plaza.asignatura]),
      subgrupos: this.delNivel(this.subgrupos(), plaza.subgrupos),
    })),
  );

  private delNivel<T extends Asignatura | Subgrupo>(
    opciones: readonly T[],
    elegidos: readonly string[],
  ): T[] {
    return opcionesDelNivel(
      opciones,
      this.valor().nivel,
      this.nivelDeGrupo(),
      !this.filtraPorNivel(),
      elegidos,
    );
  }

  constructor() {
    // Antes de la precarga: precargar fija el tipo y la regla de subgrupos tiene que seguirlo.
    this.form.controls.tipo.valueChanges.subscribe(() => this.aplicarTipo());
    const editando = this.editando;
    if (editando) {
      this.precargar(editando);
    } else {
      this.plazas.push(this.crearPlaza());
    }
    // DESPUÉS de precargar, que también emite: editar borra el error de guardado y el 409, que ya
    // no hablan de lo que hay en pantalla (S226). Cancelada como en generando-dialogo. Copia además
    // el valor en su señal (S251), que arranca con el de la precarga.
    this.valor.set(this.form.getRawValue());
    const cambios = this.form.valueChanges.subscribe(() => {
      this.valor.set(this.form.getRawValue());
      if (this.errorDeGuardado) {
        this.ponerError('', false);
      }
      this.conflicto.set('');
    });
    inject(DestroyRef).onDestroy(() => cambios.unsubscribe());
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
      // Su validador de fila lo pone aplicarTipo: en una CLASE, al menos un subgrupo (el
      // contrato lo exige desde S201); fuera de ella, ninguno. I2 sigue siendo del array.
      subgrupos: [[] as string[]],
    }) as PlazaFila;
    fila.controls.modoAula.valueChanges.subscribe(() => this.aplicarModo(fila));
    this.aplicarTipo([fila]);
    return fila;
  }

  /**
   * La regla de subgrupos según el TIPO, en un solo sitio (S201). Se llama al cambiar el tipo
   * —sobre todas las filas— y al nacer cada fila —sobre esa—. En una CLASE cada plaza exige al
   * menos un subgrupo; en una REUNIÓN o una FUNCIÓN los subgrupos no se validan: ni se pintan
   * ni viajan (ver {@link #aPlazaRequest}). El modo de aula NO se toca: desde S207 una CLASE
   * también puede ir sin aula escrita, así que una plaza en NINGUNA conserva el modo y solo
   * cambia su etiqueta.
   */
  private aplicarTipo(filas: PlazaFila[] = this.plazas.controls): void {
    const clase = this.form.controls.tipo.value === 'CLASE';
    // El nivel (S251) se exige solo al dar de alta una CLASE: en la edición se deduce y puede
    // quedar vacío si sus subgrupos mezclan niveles (decisión EZ); una reunión o una función no
    // lo piden.
    const nivel = this.form.controls.nivel;
    if (clase && !this.esEdicion) {
      nivel.setValidators(Validators.required);
    } else {
      nivel.clearValidators();
    }
    nivel.updateValueAndValidity();
    for (const fila of filas) {
      if (clase) {
        fila.controls.subgrupos.setValidators(arrayNoVacio);
      } else {
        fila.controls.subgrupos.clearValidators();
      }
      fila.controls.subgrupos.updateValueAndValidity();
    }
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
    const modo = fila.controls.modoAula.value;
    if (modo === 'FIJA') {
      fila.controls.aulasCandidatas.setValue([]);
      fila.controls.aulasCandidatas.clearValidators();
      fila.controls.aulaFija.setValidators(Validators.required);
    } else if (modo === 'CANDIDATAS') {
      fila.controls.aulaFija.setValue('');
      fila.controls.aulaFija.clearValidators();
      fila.controls.aulasCandidatas.setValidators(arrayNoVacio);
    } else {
      // NINGUNA: ni aula fija ni candidatas, y ninguna de las dos se valida. En una CLASE el
      // aula la elige el generador (S207); fuera, la plaza no ocupa aula (S201).
      fila.controls.aulaFija.setValue('');
      fila.controls.aulaFija.clearValidators();
      fila.controls.aulasCandidatas.setValue([]);
      fila.controls.aulasCandidatas.clearValidators();
    }
    fila.controls.aulaFija.updateValueAndValidity();
    fila.controls.aulasCandidatas.updateValueAndValidity();
  }

  /**
   * Etiqueta del modo NINGUNA según el tipo (S207): en una CLASE el aula no falta, la elige el
   * generador con las reglas de aulas; en una reunión o una función, no hay aula.
   */
  protected etiquetaSinAula(): string {
    return this.form.controls.tipo.value === 'CLASE' ? 'La elige el generador' : 'Sin aula';
  }

  /** Añade una fila VACÍA al final y abre su pestaña. Nace por la fábrica: no hereda nada de la
   *  anterior. */
  protected anadirPlaza(): void {
    this.plazas.push(this.crearPlaza());
    this.activa.set(this.plazas.length - 1);
  }

  protected activar(indice: number): void {
    this.activa.set(indice);
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
    // La vecina: la que ocupa ahora su sitio, o la anterior si era la última (maqueta).
    this.activa.set(Math.min(indice, this.plazas.length - 1));
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
    // El tipo va PRIMERO y antes de reconstruir las plazas: cada fila nace con la regla de
    // subgrupos de SU tipo (aplicarTipo en la fábrica) y no con la de CLASE por defecto.
    this.form.controls.tipo.setValue(actividad.tipo);
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
    fila.controls.modoAula.setValue(
      plaza.aulaFija !== null
        ? 'FIJA'
        : plaza.aulasCandidatas.length > 0
          ? 'CANDIDATAS'
          : 'NINGUNA',
    );
    fila.controls.asignatura.setValue(plaza.asignatura);
    fila.controls.aulaFija.setValue(plaza.aulaFija ?? '');
    fila.controls.aulasCandidatas.setValue([...plaza.aulasCandidatas]);
    fila.controls.profesores.setValue([...plaza.profesores]);
    fila.controls.subgrupos.setValue([...plaza.subgrupos]);
  }

  /**
   * Puebla los CUATRO desplegables y, desde S251, los niveles y los grupos que filtran por nivel.
   * Un fallo en cualquiera se presenta en el mismo hueco que los errores de guardado: sin catálogo
   * el formulario no puede completarse.
   */
  ngOnInit(): void {
    this.asignaturaService.listar().subscribe({
      next: (lista) => this.asignaturas.set(lista),
      error: (err: HttpErrorResponse) =>
        this.ponerError(this.mensaje(err, 'No se pudieron cargar las asignaturas'), false),
    });
    this.profesorService.listar().subscribe({
      next: (lista) => this.profesores.set(lista),
      error: (err: HttpErrorResponse) =>
        this.ponerError(this.mensaje(err, 'No se pudieron cargar los profesores'), false),
    });
    this.aulaService.listar().subscribe({
      next: (lista) => this.aulas.set(lista),
      error: (err: HttpErrorResponse) =>
        this.ponerError(this.mensaje(err, 'No se pudieron cargar las aulas'), false),
    });
    this.subgrupoService.listar().subscribe({
      next: (lista) => {
        this.subgrupos.set(lista);
        this.subgruposCargados = true;
        this.deducirNivel();
      },
      error: (err: HttpErrorResponse) =>
        this.ponerError(this.mensaje(err, 'No se pudieron cargar los subgrupos'), false),
    });
    this.nivelService.listar().subscribe({
      next: (lista) => this.niveles.set(lista),
      error: (err: HttpErrorResponse) =>
        this.ponerError(this.mensaje(err, 'No se pudieron cargar los niveles'), false),
    });
    this.grupoService.listar().subscribe({
      next: (lista) => {
        this.grupos.set(lista);
        this.gruposCargados = true;
        this.deducirNivel();
      },
      error: (err: HttpErrorResponse) =>
        this.ponerError(this.mensaje(err, 'No se pudieron cargar los grupos'), false),
    });
  }

  /**
   * En la edición, el nivel se deduce de los subgrupos de las plazas cuando han llegado subgrupos
   * y grupos (S251, decisión EZ): uno solo, ese; varios o ninguno, vacío y «Mostrar todos los
   * niveles» marcado. No pisa lo que el usuario ya haya tocado mientras llegaban.
   */
  private deducirNivel(): void {
    if (!this.esEdicion || !this.subgruposCargados || !this.gruposCargados) {
      return;
    }
    const { nivel, todosLosNiveles } = this.form.controls;
    if (nivel.dirty || todosLosNiveles.dirty) {
      return;
    }
    const deducido = nivelDeLasPlazas(
      this.plazas.controls.map((f) => f.controls.subgrupos.value),
      this.subgrupos(),
      this.nivelDeGrupo(),
    );
    nivel.setValue(deducido);
    todosLosNiveles.setValue(deducido === '');
  }

  /** Código del subgrupo que I2 ha encontrado repetido, o '' si no hay repetición. */
  protected get subgrupoRepetido(): string {
    return (this.plazas.errors?.['subgrupoRepetido'] as string | undefined) ?? '';
  }

  /** Índices de las plazas cuya pestaña lleva la marca de I2. */
  protected get plazasConI2(): number[] {
    return plazasConSubgrupoRepetido(this.plazas.controls.map((f) => f.controls.subgrupos.value));
  }

  /**
   * Si la pestaña de la plaza lleva la marca de campos inválidos (S230-F4, R1, decidido por el
   * usuario): la plaza es inválida y, o bien NO es la abierta y está tocada, o bien ya se ha
   * intentado guardar. En la abierta, antes de guardar, no hay marca: sus errores se ven en su
   * sitio. Una plaza recién añadida y sin tocar no se marca hasta el primer «Guardar».
   */
  protected plazaConErrores(fila: PlazaFila, indice: number): boolean {
    return fila.invalid && ((indice !== this.activa() && fila.touched) || this.intentoDeGuardar());
  }

  /** Id de un control del diálogo; con `plaza`, el de esa plaza. */
  protected id(campo: string, plaza?: number): string {
    return plaza === undefined
      ? `${this.prefijoId}-${campo}`
      : `${this.prefijoId}-plaza-${plaza}-${campo}`;
  }

  protected guardar(): void {
    this.intentoDeGuardar.set(true);
    if (this.form.invalid) {
      // Incluye el error de I2: el validador del array pone `form.invalid` a true, así
      // que esta misma guarda corta el envío sin comprobación aparte.
      this.form.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.ponerError('', false);
    this.conflicto.set('');
    const req = this.aRequest();
    const peticion = this.editando
      ? this.service.editar(this.editando.id, req)
      : this.service.crear(req);
    peticion.subscribe({
      next: (guardada) => this.ref.close({ id: guardada.id }),
      error: (err: HttpErrorResponse) => {
        if (err?.status === 409) {
          this.conflicto.set(this.textoConflicto(err));
        } else {
          // 400 = validación del backend (XOR, I7, I2, I3, código duplicado o no
          // resoluble). El texto que compone es accionable y viaja en `message`.
          this.ponerError(this.mensaje(err, 'No se pudo guardar la actividad'), true);
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
    const clase = v.tipo === 'CLASE';
    return {
      codigo: v.codigo,
      tipo: v.tipo,
      asignatura: v.asignatura === '' ? null : v.asignatura,
      duracionTramos: v.duracionTramos,
      repeticionesPorSemana: v.repeticionesPorSemana,
      patronTemporal: v.patronTemporal,
      // Fuera de una CLASE el control está oculto y el contrato no admite tutor (S201).
      requiereTutor: clase ? v.requiereTutor : false,
      plazas: this.plazas.controls.map((fila) => this.aPlazaRequest(fila, clase)),
    };
  }

  /** Una fila → `PlazaRequest`, resolviendo el aula por el modo elegido y los subgrupos por
   *  el tipo. Sin `id` ni `codigo`: el código de plaza lo deriva el backend. */
  private aPlazaRequest(fila: PlazaFila, clase: boolean): PlazaRequest {
    const v = fila.getRawValue();
    return {
      asignatura: v.asignatura,
      aulaFija: v.modoAula === 'FIJA' ? v.aulaFija : null,
      aulasCandidatas: v.modoAula === 'CANDIDATAS' ? v.aulasCandidatas : [],
      profesores: v.profesores,
      // Fuera de una CLASE no hay alumnos: los subgrupos ni se pintan ni viajan (S201).
      subgrupos: clase ? v.subgrupos : [],
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

  /** Fija {@link error} y recuerda si viene de guardar; vacío lo limpia (S226). */
  private ponerError(texto: string, deGuardado: boolean): void {
    this.error.set(texto);
    this.errorDeGuardado = deGuardado;
  }

  /** Traduce error Http a texto de usuario. Mismo patrón que O-catálogo, copiado con
   *  texto propio (no compartido: tocaría horario-view). */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
