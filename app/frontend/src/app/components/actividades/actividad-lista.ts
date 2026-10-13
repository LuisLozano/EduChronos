import { Component, OnInit, computed, inject, linkedSignal, signal } from '@angular/core';
import { Dialog } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ActividadService } from '../../services/actividad.service';
import { AsignaturaService } from '../../services/asignatura.service';
import { GrupoService } from '../../services/grupo.service';
import { ProfesorService } from '../../services/profesor.service';
import { SubgrupoService } from '../../services/subgrupo.service';
import { Actividad, ETIQUETA_TIPO_ACTIVIDAD } from '../../models/actividad.model';
import { ActividadForm, ActividadGuardada } from './actividad-form';
import { ConfirmarBorrado } from '../confirmar-borrado/confirmar-borrado';
import { CabeceraLista } from '../cabecera-lista/cabecera-lista';
import { EstadoLista } from '../estado-lista/estado-lista';
import { coincide } from '../../catalogo/busqueda';
import { Catalogo, casarExacto, lineaPertenencia, nombresDeAsignaturas } from '../../catalogo/pertenencia';

/**
 * Lista del catálogo de actividades: carga en init, tabla con acciones por fila,
 * alta/edición en diálogo, borrado con confirmación previa. Molde de `SubgrupoLista`; la
 * escritura vive en {@link ActividadForm}.
 *
 * <p>SEIS COLUMNAS: Código, Asignatura, Patrón, Duración, Repeticiones y N.º de plazas.
 * La asignatura de la actividad es OPCIONAL en el contrato (`null` cuando sus plazas
 * tienen asignaturas distintas), y esa ausencia se pinta como «varias» —el dato dice
 * algo, no falta—.
 *
 * <p><b>La guarda de multiplaza se RETIRÓ en el trozo B.</b> Existió mientras el
 * formulario enviaba una sola plaza: abrir una actividad de tres y guardar habría borrado
 * dos por la reconciliación posicional del PUT. Ahora el formulario edita N plazas, así
 * que toda actividad es editable y el botón no distingue. Lo que protege el PUT
 * destructivo sigue estando donde importa: el 409 del backend ante sesiones o bloqueos.
 *
 * <p>El BORRADO se lleva la actividad con todas sus plazas por cascade, y la confirmación
 * lo dice cuando hay más de una.
 *
 * <p><b>Búsqueda por pertenencia</b> (S225, condiciones 3 y 4 de O-interfaz). Si lo escrito casa
 * EXACTAMENTE con el código de un profesor, una asignatura, un grupo o un subgrupo, o con el
 * nombre de una asignatura, se ven las actividades que les pertenecen y una línea bajo la caja
 * dice de quién son ({@link casarExacto}). Si no, la búsqueda por texto de siempre, con los
 * nombres de asignatura añadidos al texto de la fila. Las cuatro listas se cargan UNA vez al
 * iniciar; {@link cargar} sigue recargando solo las actividades. El término puede llegar en
 * `?buscar=`, que es lo que ponen los botones «Actividades» de las otras listas.
 */
@Component({
  selector: 'app-actividad-lista',
  imports: [CabeceraLista, EstadoLista],
  templateUrl: './actividad-lista.html',
  styleUrl: './actividad-lista.css',
})
export class ActividadLista implements OnInit {
  private readonly service = inject(ActividadService);
  private readonly dialog = inject(Dialog);
  private readonly ruta = inject(ActivatedRoute);
  private readonly profesores = inject(ProfesorService);
  private readonly asignaturas = inject(AsignaturaService);
  private readonly grupos = inject(GrupoService);
  private readonly subgrupos = inject(SubgrupoService);

  protected readonly actividades = signal<Actividad[]>([]);
  /** Texto visible de la columna Tipo (S201). */
  protected readonly etiquetaTipo = ETIQUETA_TIPO_ACTIVIDAD;
  protected readonly cargando = signal(false);
  /** Error de la última operación de lista o borrado. Vacío = sin error. */
  protected readonly error = signal('');

  /** Texto escrito en la caja de la cabecera. Vacío = se ven todas las filas. */
  protected readonly busqueda = signal('');

  /**
   * Aviso tras guardar (S227, condición 10 de O-interfaz): la actividad guardada ya no casa con
   * la búsqueda y desaparecería sin explicación. `linkedSignal` de la búsqueda: cambiarla lo
   * borra; abrir otra vez el editor también ({@link abrirForm}). `null` = sin aviso.
   */
  protected readonly aviso = linkedSignal<string, string | null>({
    source: this.busqueda,
    computation: () => null,
  });

  /** Las cuatro listas de la búsqueda por pertenencia; `null` mientras no han llegado. */
  protected readonly catalogo = signal<Catalogo | null>(null);
  /** Falló alguno de los cuatro GET: solo queda la búsqueda por texto, y se dice. */
  protected readonly falloCatalogo = signal(false);

  private readonly nombrePorCodigo = computed(
    () => new Map((this.catalogo()?.asignaturas ?? []).map((a) => [a.codigo, a.nombreCompleto])),
  );

  /** Lo que casa exactamente con la búsqueda, o `null`: entonces manda el texto. */
  protected readonly pertenencia = computed(() => {
    const catalogo = this.catalogo();
    return catalogo ? casarExacto(this.busqueda(), catalogo) : null;
  });

  /** La línea bajo la caja con las entidades que casan; vacía si no casa ninguna. */
  protected readonly linea = computed(() => {
    const pertenencia = this.pertenencia();
    return pertenencia ? lineaPertenencia(pertenencia.entidades) : '';
  });

  /**
   * Texto de UNA fila para buscar en él, compuesto con los campos QUE LA TABLA
   * PINTA y con las mismas expresiones que usa la plantilla. La regla es esa: si
   * la fila lo enseña, la búsqueda lo encuentra; si no lo enseña, no.
   *
   * <p><b>Nada vigila que siga a la plantilla.</b> Añadir una columna a la tabla y
   * olvidarla aquí no rompe ningún test ni da error de compilación: deja una
   * columna visible por la que no se puede buscar, en silencio. Es la limitación
   * ACEPTADA del Cambio (S124), y el sitio donde mirar cuando alguien diga que la
   * búsqueda «no encuentra» algo que está en pantalla.
   *
   * <p><b>Una excepción a la regla, deliberada</b> (S225): los NOMBRES de la asignatura de la
   * actividad y de las de sus plazas, que la tabla no pinta. La columna enseña el código, y
   * «Latín» no encontraba «LAT» (S188).
   */
  protected textoFila(actividad: Actividad): string {
    return [actividad.codigo, this.asignaturaDe(actividad), actividad.patronTemporal,
      actividad.duracionTramos, actividad.repeticionesPorSemana, actividad.plazas.length,
      ...nombresDeAsignaturas(actividad, this.nombrePorCodigo())].join(' ');
  }

  /**
   * Filas que casan con la consulta: las que pertenecen a lo que casa exactamente o, si no casa
   * nada, las que casan por texto. `filter` es el método del array, no un nombre nuestro. Con
   * la búsqueda vacía devuelve todas —{@link coincide} da `true` sin consulta y
   * {@link casarExacto} da `null`—, así que la lista arranca completa.
   */
  protected readonly visibles = computed(() => {
    const pertenencia = this.pertenencia();
    return pertenencia
      ? this.actividades().filter(pertenencia.pertenece)
      : this.actividades().filter((f) => coincide(this.textoFila(f), this.busqueda()));
  });

  ngOnInit(): void {
    const buscar = this.ruta.snapshot.queryParamMap.get('buscar');
    if (buscar) {
      this.busqueda.set(buscar);
    }
    this.cargar();
    this.cargarCatalogo();
  }

  /**
   * Las cuatro listas de la pertenencia, una sola vez. Si falla cualquiera, no hay coincidencia
   * exacta posible: la lista y la búsqueda por texto siguen, y la línea lo avisa.
   */
  private cargarCatalogo(): void {
    forkJoin({
      profesores: this.profesores.listar(),
      asignaturas: this.asignaturas.listar(),
      grupos: this.grupos.listar(),
      subgrupos: this.subgrupos.listar(),
    }).subscribe({
      next: (catalogo) => this.catalogo.set(catalogo),
      error: () => this.falloCatalogo.set(true),
    });
  }

  /** Pide la lista. Con `guardada`, al llegar decide el aviso sobre la lista ya filtrada. */
  protected cargar(guardada?: ActividadGuardada): void {
    this.cargando.set(true);
    this.error.set('');
    this.service.listar().subscribe({
      next: (lista) => {
        this.actividades.set(lista);
        this.cargando.set(false);
        if (guardada) {
          this.aviso.set(this.avisoTrasGuardar(guardada.id));
        }
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, 'No se pudo cargar la lista de actividades'));
        this.cargando.set(false);
      },
    });
  }

  /** `true` si la actividad tiene más de una plaza. Ya no gobierna la edición: lo usa la
   *  confirmación de borrado para avisar de cuántas plazas caen. */
  protected esMultiplaza(actividad: Actividad): boolean {
    return actividad.plazas.length > 1;
  }

  /** Asignatura de la actividad, o «varias» cuando es `null` (plazas heterogéneas). */
  protected asignaturaDe(actividad: Actividad): string {
    return actividad.asignatura ?? 'varias';
  }

  protected nuevo(): void {
    this.abrirForm(null);
  }

  protected editar(actividad: Actividad): void {
    this.abrirForm(actividad);
  }

  /** Abre el formulario en diálogo; recarga si se guardó (cierre con el id de la guardada). */
  private abrirForm(actividad: Actividad | null): void {
    this.aviso.set(null);
    this.dialog
      .open<ActividadGuardada, Actividad | null>(ActividadForm, { data: actividad })
      .closed.subscribe((guardada) => {
        // backdrop, Escape y Cancelar emiten undefined: solo un cierre con id recarga.
        if (guardada?.id !== undefined) {
          this.cargar(guardada);
        }
      });
  }

  /**
   * El aviso de una actividad recién guardada: solo si la actividad está en la lista completa
   * pero no en la filtrada. Si no está en ninguna —otra pestaña la borró—, no hay nada que
   * explicar.
   */
  private avisoTrasGuardar(id: number): string | null {
    const busqueda = this.busqueda();
    // Sin búsqueda la filtrada ES la completa (pertenencia.ts:65-67, busqueda.ts:88-94): la
    // condición de abajo basta y no hace falta preguntar si hay búsqueda (M3c, S227).
    const guardada = this.actividades().find((a) => a.id === id);
    if (!guardada || this.visibles().some((a) => a.id === id)) {
      return null;
    }
    return `Actividad «${guardada.codigo}» guardada. No aparece en la lista porque no corresponde a «${busqueda}».`;
  }

  protected borrar(actividad: Actividad): void {
    const lineas = [`¿Borrar la actividad ${actividad.codigo}?`];
    if (this.esMultiplaza(actividad)) {
      lineas.push(`Se borrarán sus ${actividad.plazas.length} plazas.`);
    }
    this.dialog
      .open<boolean, string[]>(ConfirmarBorrado, { data: lineas })
      .closed.subscribe((confirmado) => {
        if (confirmado === true) {
          this.confirmarBorrado(actividad);
        }
      });
  }

  private confirmarBorrado(actividad: Actividad): void {
    this.error.set('');
    this.service.borrar(actividad.id).subscribe({
      next: () => this.cargar(),
      error: (err: HttpErrorResponse) => {
        // 409 = referencias entrantes (sesiones del horario, bloqueos). El backend
        // compone el texto rico con el desglose y viaja en `message`.
        this.error.set(this.mensaje(err, `No se pudo borrar la actividad ${actividad.codigo}`));
      },
    });
  }

  /**
   * Traduce error Http a texto de usuario: mensaje del servidor primero (`message`,
   * luego `error`), degradado con status si no hay. Copiado del patrón de
   * `subgrupo-lista.mensaje()` con texto propio; NO extraído a utilidad compartida a
   * propósito: hacerlo tocaría horario-view (D-F8.6, H1 cerrado).
   */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
