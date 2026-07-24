import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Dialog } from '@angular/cdk/dialog';

import { HorarioProyeccion } from '../../models/horario.model';
import { Diagnostico } from '../../models/diagnostico.model';
import { AvisoPrevalidacion } from '../../models/prevalidacion.model';
import { HorarioService } from '../../services/horario.service';
import { BloqueoService } from '../../services/bloqueo.service';
import { DiagnosticoService } from '../../services/diagnostico.service';
import { PrevalidacionService } from '../../services/prevalidacion.service';
import { Vista, entidadesDeVista, filtrar } from '../../horario/proyeccion';
import { clavePin, indicePines } from '../../horario/pines';
import { ViolacionEnCelda, indiceViolaciones, sumaDeltasPorInstancia } from '../../horario/diagnostico';
import { HorarioGrid, SueltaInstancia } from '../horario-grid/horario-grid';
import { PanelPrevalidacion } from '../panel-prevalidacion/panel-prevalidacion';
import { ConfirmarGeneracion } from '../confirmar-generacion/confirmar-generacion';

/**
 * Contenedor de las tres vistas: carga la proyección del horario `{id}` (param
 * de ruta) y ofrece un selector de vista (grupo / profesor / aula) y otro de
 * entidad dentro de la vista, cuyas opciones se DERIVAN de la proyección. El
 * filtrado y el agrupamiento son lógica pura ({@link filtrar},
 * `agruparPorActividad`); este componente solo orquesta señales y delega la
 * rejilla.
 *
 * <p>También es el único que habla con {@link BloqueoService}: la rejilla emite
 * la suelta y aquí se persiste el pin. La proyección NO se recarga tras pinar
 * —el pin es una restricción para la PRÓXIMA generación, no un movimiento del
 * horario vigente—, de ahí el aviso persistente de pines sin aplicar.
 */
@Component({
  selector: 'app-horario-view',
  imports: [HorarioGrid, PanelPrevalidacion],
  templateUrl: './horario-view.html',
  styleUrl: './horario-view.css',
})
export class HorarioView {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly dialog = inject(Dialog);
  private readonly service = inject(HorarioService);
  private readonly bloqueos = inject(BloqueoService);
  private readonly diagnosticos = inject(DiagnosticoService);
  private readonly prevalidacion = inject(PrevalidacionService);

  protected readonly proyeccion = signal<HorarioProyeccion | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly vista = signal<Vista>('grupo');
  protected readonly entidad = signal<string>('');

  /**
   * Instancias pinadas en TODO el horario, de la clave de {@link clavePin} al
   * `id` del bloqueo, que es lo que el DELETE necesita. `null` significa pin vivo
   * sin id conocido: se pinta, pero no se puede borrar.
   */
  protected readonly pinadas = signal<ReadonlyMap<string, number | null>>(new Map<string, number | null>());
  /** Último rechazo del backend (reglas de D-3); se limpia al siguiente intento. */
  protected readonly errorPin = signal<string | null>(null);

  /** Diagnóstico del horario cargado; null mientras no llega o si su carga falla. */
  protected readonly diagnostico = signal<Diagnostico | null>(null);
  /** Fallo NO fatal de la carga del diagnóstico. Señal PROPIA: ver {@link cargarDiagnostico}. */
  protected readonly errorDiagnostico = signal<string | null>(null);

  /**
   * Hallazgos de la pre-validación del catálogo; `null` mientras no llega
   * (NO ejecutado), `[]` si el catálogo está sano. GLOBAL, no por horario: ver
   * {@link cargarPrevalidacion}.
   */
  protected readonly avisosPrevalidacion = signal<AvisoPrevalidacion[] | null>(null);
  /** Fallo NO fatal de la carga de pre-validación. Señal PROPIA: no gatea la rejilla. */
  protected readonly errorPrevalidacion = signal<string | null>(null);

  /**
   * Fallo del POST de generación. Señal PROPIA y DISJUNTA de {@link error}
   * (misma disciplina que {@link errorPrevalidacion}, S92): un rechazo del solver
   * no debe vaciar la rejilla del horario vigente. Se limpia al iniciar cada
   * intento. Se pinta bajo `.error-generacion`, clase propia que NO colisiona con
   * el `.error` de {@link error}/{@link errorPin}.
   */
  protected readonly errorGeneracion = signal<string | null>(null);

  /**
   * Suma con signo de los delta blandos por instancia (clave de {@link clavePin}),
   * lista para el input de la rejilla. La agregación es LÓGICA PURA
   * ({@link sumaDeltasPorInstancia}); este contenedor NO suma, igual que no filtra
   * ni agrupa a mano —solo orquesta señales—.
   *
   * <p>Esta suma NO es `Totales` y NO tiene por qué cuadrar con él: contrastarlos
   * es la trampa del contrato, no un bug (ver la función pura y el javadoc de
   * `TotalesDTO`). Las claves de suma 0 no llegan (C2/S65).
   */
  protected readonly badges = computed<ReadonlyMap<string, number>>(() => {
    const d = this.diagnostico();
    return d ? sumaDeltasPorInstancia(d.penalizaciones) : new Map<string, number>();
  });

  /**
   * Violaciones duras por instancia (clave de {@link clavePin}), listas para el
   * input de la rejilla. Hermano exacto de {@link badges}: el contenedor NO indexa
   * —delega en la capa pura ({@link indiceViolaciones})—, igual que no suma ni
   * agrupa a mano. La rejilla resuelve la asimetría D15 al pintar.
   */
  protected readonly violaciones = computed<ReadonlyMap<string, readonly ViolacionEnCelda[]>>(() => {
    const d = this.diagnostico();
    return d ? indiceViolaciones(d.violaciones) : new Map<string, readonly ViolacionEnCelda[]>();
  });

  protected readonly entidades = computed(() => {
    const p = this.proyeccion();
    return p ? entidadesDeVista(p.sesiones, this.vista()) : [];
  });

  protected readonly sesionesFiltradas = computed(() => {
    const p = this.proyeccion();
    const e = this.entidad();
    return p && e ? filtrar(p.sesiones, this.vista(), e) : [];
  });

  constructor() {
    this.route.paramMap.subscribe((pm) => this.cargar(Number(pm.get('id'))));
  }

  /**
   * Refresca el índice de pines. NO se llama desde el constructor: `paramMap` ya
   * emite en el arranque y {@link cargar} corre con él, así que llamarlo en ambos
   * sitios dispararía dos GET /api/bloqueos por montaje.
   *
   * <p>Tampoco se llama al cambiar de vista o de entidad: el índice es de TODO el
   * horario, no del filtro, y esos dos gestos no lo pueden invalidar.
   */
  private cargarPines(): void {
    this.bloqueos.listar().subscribe({
      next: (bs) => this.pinadas.set(indicePines(bs)),
      error: () => this.errorPin.set('No se pudieron cargar los pines existentes.'),
    });
  }

  /**
   * Carga el diagnóstico del horario `{id}`. A DIFERENCIA de {@link cargarPines},
   * que no lleva parámetro porque el índice de pines es de TODO el horario y se
   * relee entero, este SÍ toma el id: el diagnóstico es POR horario. Esa asimetría
   * es la razón de que no compartan forma —a este no le falta el id por descuido—.
   *
   * <p>Señal de error PROPIA ({@link errorDiagnostico}), nunca {@link error}: un
   * fallo del diagnóstico no debe vaciar la rejilla —la rama `@else if` de la
   * plantilla la gatea con `error()`— y la proyección vigente sigue siendo válida
   * sin diagnóstico. Tampoco {@link errorPin}, que habla de otra cosa. Se limpia
   * el diagnóstico anterior al empezar para no arrastrar badges de otro horario.
   */
  private cargarDiagnostico(id: number): void {
    this.errorDiagnostico.set(null);
    this.diagnostico.set(null);
    this.diagnosticos.getDiagnostico(id).subscribe({
      next: (d) => this.diagnostico.set(d),
      error: () => this.errorDiagnostico.set('No se pudo cargar el diagnóstico.'),
    });
  }

  /**
   * Carga la pre-validación del catálogo. SIN parámetro `id` —a diferencia de
   * {@link cargarDiagnostico}, que sí lo toma—: la pre-validación es del catálogo
   * GLOBAL, no de un horario, exactamente como {@link cargarPines}. Es la MISMA
   * asimetría que S87 documentó entre pines (global) y diagnóstico (por horario),
   * y por eso este método comparte forma con `cargarPines`, no con `cargarDiagnostico`.
   *
   * <p>Se llama desde {@link cargar}, no desde el constructor, por la misma razón
   * que `cargarPines`: `paramMap` ya emite en el arranque y `cargar` corre con
   * esa emisión, así que invocarlo también en el constructor dispararía dos
   * GET /api/prevalidacion por montaje.
   *
   * <p>Señal de error PROPIA ({@link errorPrevalidacion}), nunca {@link error}: un
   * fallo de la pre-validación no debe vaciar la rejilla —la proyección vigente no
   * depende de ella—. Se limpia el estado anterior al empezar para no arrastrar
   * hallazgos de otra carga.
   */
  private cargarPrevalidacion(): void {
    this.errorPrevalidacion.set(null);
    this.avisosPrevalidacion.set(null);
    this.prevalidacion.getPrevalidacion().subscribe({
      next: (avisos) => this.avisosPrevalidacion.set(avisos),
      error: () => this.errorPrevalidacion.set('No se pudo cargar la pre-validación.'),
    });
  }

  private cargar(id: number): void {
    this.error.set(null);
    this.cargarPines();
    this.cargarPrevalidacion();
    this.cargarDiagnostico(id);
    this.service.getProyeccion(id).subscribe({
      next: (p) => {
        this.proyeccion.set(p);
        this.entidad.set(entidadesDeVista(p.sesiones, this.vista())[0] ?? '');
      },
      error: (err) => {
        this.proyeccion.set(null);
        this.error.set(`No se pudo cargar el horario ${id} (${err?.status ?? 'error'}).`);
      },
    });
  }

  /**
   * Persiste el pin de tramo de la instancia soltada. `aulas: []` porque la
   * suelta solo fija el TRAMO; el body describe el pin completo (D-5), así que
   * el pin queda sin pines de aula. La rejilla no se mueve: en OK solo aparece
   * el candado, y en ERROR se muestra el rechazo del servidor sin reimplementar
   * aquí ninguna de sus reglas.
   */
  protected alSoltar(s: SueltaInstancia): void {
    this.errorPin.set(null);
    this.bloqueos
      .guardar({
        actividadCodigo: s.actividadCodigo,
        indice: s.indice,
        tramo: { dia: s.dia, orden: s.orden },
        aulas: [],
      })
      .subscribe({
        next: (b) =>
          this.pinadas.set(new Map(this.pinadas()).set(clavePin(b.actividadCodigo, b.indice), b.id)),
        error: (err) => this.errorPin.set(this.mensaje(err)),
      });
  }

  /**
   * Quita el pin de la instancia cuya CLAVE emite la rejilla. El id se resuelve
   * aquí —la rejilla no lo conoce—; si falta, no hay DELETE que emitir y se calla:
   * un error de UI no ayudaría a quien no tiene forma de arreglarlo.
   *
   * <p>SIN movimiento optimista (D-F8.6-ii-5): el candado sigue pintado hasta el
   * 204. Si el DELETE falla, no hay nada que revertir.
   */
  protected alDespinar(clave: string): void {
    this.errorPin.set(null);
    const id = this.pinadas().get(clave);
    if (id === null || id === undefined) {
      return;
    }
    this.bloqueos.borrar(id).subscribe({
      next: () => {
        const restantes = new Map(this.pinadas());
        restantes.delete(clave);
        this.pinadas.set(restantes);
      },
      error: (err) => this.errorPin.set(this.mensaje(err)),
    });
  }

  /**
   * Mensaje del servidor. El `reason` del `ResponseStatusException` solo viaja
   * en el body si `server.error.include-message` está activo (hoy no lo está):
   * por eso el degradado a `error` + estado, en vez de inventar un texto propio.
   */
  private mensaje(err: { status?: number; error?: { message?: string; error?: string } }): string {
    const cuerpo = err?.error;
    return cuerpo?.message || cuerpo?.error || `El servidor rechazó el pin (${err?.status ?? 'error'}).`;
  }

  /**
   * Dispara una generación de horario. Gateado por {@link avisosPrevalidacion}:
   * si es `null` (pre-validación no ejecutada) no hace nada —el botón ya está
   * deshabilitado, esta guarda es el cinturón—. Si hay algún aviso de severidad
   * `'ERROR'`, la generación está condenada: se pide confirmación explícita y solo
   * se procede si el diálogo cierra con `true` (backdrop/Escape emiten `undefined`
   * y abortan). Sin errores, procede directo.
   */
  protected generar(): void {
    const avisos = this.avisosPrevalidacion();
    if (avisos === null) {
      return;
    }
    const errores = avisos.filter((a) => a.severidad === 'ERROR');
    if (errores.length > 0) {
      this.dialog
        .open<boolean, AvisoPrevalidacion[]>(ConfirmarGeneracion, { data: errores })
        .closed.subscribe((confirmado) => {
          if (confirmado === true) {
            this.lanzarGeneracion();
          }
        });
      return;
    }
    this.lanzarGeneracion();
  }

  /**
   * Lanza el POST y, en el next, navega a la ruta del horario nuevo. La proyección
   * devuelta NO se consume: la recarga la dispara la emisión de `paramMap` al
   * cambiar de ruta —igual que cualquier otra entrada a la vista—, no este next.
   * El error puebla {@link errorGeneracion} (señal propia, no gatea la rejilla).
   */
  private lanzarGeneracion(): void {
    this.errorGeneracion.set(null);
    this.service.generar().subscribe({
      next: (dto) => {
        this.router.navigate(['/horario', dto.id]);
      },
      error: (err) => this.errorGeneracion.set(this.mensaje(err)),
    });
  }

  protected cambiarVista(v: Vista): void {
    this.vista.set(v);
    this.entidad.set(this.entidades()[0] ?? '');
  }

  protected cambiarEntidad(e: string): void {
    this.entidad.set(e);
  }
}
