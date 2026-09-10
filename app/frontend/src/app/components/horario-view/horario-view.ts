import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Dialog } from '@angular/cdk/dialog';

import { HorarioProyeccion, SesionVista } from '../../models/horario.model';
import { Diagnostico, Violacion } from '../../models/diagnostico.model';
import { FalloMovimiento, ReferenciaInstancia } from '../../models/ajuste.model';
import { AvisoPrevalidacion } from '../../models/prevalidacion.model';
import { TramoJornadaDTO } from '../../models/jornada.model';
import { HorarioService } from '../../services/horario.service';
import { BloqueoService } from '../../services/bloqueo.service';
import { AjusteService } from '../../services/ajuste.service';
import { DiagnosticoService } from '../../services/diagnostico.service';
import { PrevalidacionService } from '../../services/prevalidacion.service';
import { JornadaService } from '../../services/jornada.service';
import { Vista, entidadesDeVista, filtrar } from '../../horario/proyeccion';
import { clavePin, filaDeClave, indicePines } from '../../horario/pines';
import { tituloHorario } from '../../horario/titulo';
import { recreoTrasTramo } from '../../horario/recreo';
import { ViolacionEnCelda, indiceViolaciones, sumaDeltasPorInstancia } from '../../horario/diagnostico';
import { reemplazarInstancia, textoViolacion } from '../../horario/ajuste';
import { AjusteInstancia, HorarioGrid } from '../horario-grid/horario-grid';
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
  private readonly ajustes = inject(AjusteService);
  private readonly diagnosticos = inject(DiagnosticoService);
  private readonly prevalidacion = inject(PrevalidacionService);
  private readonly jornadas = inject(JornadaService);

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
  /** Último rechazo del backend al DESPINAR; se limpia al siguiente intento. */
  protected readonly errorPin = signal<string | null>(null);

  /**
   * Rechazo del último AJUSTE (S145). Señal PROPIA y DISJUNTA de {@link errorPin}
   * y de {@link errorGeneracion}, misma disciplina que ellas: mover una instancia,
   * despinarla y generar el horario son tres operaciones distintas y un aviso
   * heredado de una diría algo falso de las otras. Se pinta bajo `.error-ajuste`.
   */
  protected readonly errorAjuste = signal<string | null>(null);

  /**
   * Violaciones duras que el servidor adjuntó al rechazo del ajuste. Van aparte de
   * {@link errorAjuste} porque NO son texto: son una lista que la vista enumera. El
   * cuerpo las trae vacías en toda causa que no sea `VIOLA_REGLA_DURA`.
   */
  protected readonly violacionesAjuste = signal<readonly Violacion[]>([]);

  /**
   * Limitación CONOCIDA del gesto, no un error: el destino tiene dos o más clases
   * y no hay forma de saber con cuál intercambiar. Señal separada de
   * {@link errorAjuste} justamente por eso —aquí no ha habido petición ni rechazo,
   * y pintarlo como error del servidor sería mentir sobre qué pasó—.
   */
  protected readonly avisoAjuste = signal<string | null>(null);

  /** Espejo de la función pura para que la plantilla enumere las violaciones. */
  protected readonly textoViolacion = textoViolacion;

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
   * QUÉ hay en vuelo contra el backend, o `null` en reposo. `null` en éxito y en
   * error —las dos ramas, o un fallo dejaría el botón muerto para siempre—.
   *
   * <p>Existe desde S118 porque la generación tarda MINUTOS y la vista no lo decía:
   * el botón seguía pulsable y la pantalla no cambiaba, así que una espera normal era
   * indistinguible de un cuelgue, y volver a pulsar lanzaba un segundo solve encima
   * del primero.
   *
   * <p>S145 lo ENSANCHA de booleano a discriminante en vez de añadir una segunda
   * señal de espera para el ajuste: el estado «hay algo en vuelo» es uno solo —el
   * botón «Generar» debe estar cerrado mientras se aplica un cambio, igual que al
   * revés—, y dos booleanos independientes admitirían el estado imposible de tener
   * los dos a `true`. Lo único que depende de CUÁL es el texto ({@link textoGenerando}).
   */
  private readonly enVuelo = signal<'generacion' | 'ajuste' | null>(null);

  /** Hay una operación en vuelo: cierra el botón y pinta el aviso de espera. */
  protected readonly generando = computed(() => this.enVuelo() !== null);

  /**
   * Minutos que se anuncian durante la espera. ESPEJO del presupuesto por defecto del
   * backend (`educhronos.solver.max-segundos`, hoy 600 s): el servidor no lo publica
   * en ningún endpoint, así que esto es una copia y puede desincronizarse en silencio
   * si allí se cambia el valor sin tocar aquí. Es una cota anunciada, no una promesa;
   * se prefiere a no decir nada, porque un número —aunque sea aproximado— es lo que
   * distingue "está trabajando" de "se ha colgado".
   */
  private readonly MINUTOS_ANUNCIADOS = 10;

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

  /**
   * Título de la fila de cabecera (D9/D10). Se delega en la función pura
   * {@link tituloHorario}: este contenedor no formatea, igual que no filtra ni suma.
   *
   * <p>Vale también con `proyeccion()` en null —dice `Horario` a secas—, y eso no es
   * un detalle: desde D9 el título vive FUERA de la cadena `@if`, en la misma fila que
   * los controles, así que se pinta también mientras la proyección carga y cuando su
   * carga falla. Bajar la fila a la rama `@else if` dejaría al usuario sin botón
   * «Generar» justo en el arranque con 404, que es cuando más falta hace.
   */
  protected readonly titulo = computed(() => tituloHorario(this.proyeccion()?.nombre));

  protected readonly entidades = computed(() => {
    const p = this.proyeccion();
    return p ? entidadesDeVista(p.sesiones, this.vista()) : [];
  });

  protected readonly sesionesFiltradas = computed(() => {
    const p = this.proyeccion();
    const e = this.entidad();
    return p && e ? filtrar(p.sesiones, this.vista(), e) : [];
  });

  /**
   * Tras qué tramo lectivo va la fila de recreo (D7); `null` mientras la jornada
   * no llega o si su carga falla. SIN señal de error visible, y es la única de
   * esta vista que rompe esa disciplina a propósito: un séptimo bloque
   * condicional para anunciar que no se pudo pintar una banda le robaría altura a
   * la tabla justo donde D1 la reparte. La degradación es la ausencia de la fila.
   */
  private readonly tramosJornada = signal<readonly TramoJornadaDTO[]>([]);

  protected readonly recreoTras = computed(() => recreoTrasTramo(this.tramosJornada()));

  /**
   * Id del horario que {@link cargar} pidió por última vez. Campo PLANO, no
   * señal: ninguna plantilla lo lee —solo lo consulta {@link lanzarGeneracion}
   * para decidir entre recargar y navegar—, y una señal que nadie consume en la
   * vista sería más superficie de estado de la necesaria.
   */
  private idCargado: number | null = null;

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

  /**
   * Carga la jornada del centro. SIN parámetro `id` y desde {@link cargar}, como
   * {@link cargarPines} y {@link cargarPrevalidacion}: la jornada es del CENTRO, no
   * de un horario. El endpoint nunca da 404 —con la tabla vacía sintetiza la malla
   * de referencia—, así que el error es de transporte y su único efecto es que no
   * haya fila de recreo.
   */
  private cargarJornada(): void {
    this.jornadas.obtener().subscribe({
      next: (j) => this.tramosJornada.set(j.tramos),
      error: () => this.tramosJornada.set([]),
    });
  }

  private cargar(id: number): void {
    this.idCargado = id;
    this.error.set(null);
    this.cargarPines();
    this.cargarJornada();
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
   * AJUSTA el horario con la instancia soltada (S145). Sustituye al alta de pin que
   * este mismo gesto hacía hasta S144: arrastrar ya no pide «cuando regeneres,
   * ponla aquí», sino «ponla aquí AHORA».
   *
   * <p>La rama la decide cuántas instancias hay en el destino, y SOLO eso:
   *
   * <ul>
   *   <li>NINGUNA → `mover`: el tramo está libre en lo que se ve.</li>
   *   <li>UNA → `intercambiar`: las dos permutan tramo. El destino da la `segunda`
   *       referencia; la arrastrada es siempre la `primera`.</li>
   *   <li>DOS O MÁS → NO se pide nada. Es una limitación conocida del gesto, no un
   *       error: con varias candidatas el arrastre no expresa con cuál intercambiar,
   *       y elegir una por el orden de la celda sería inventar una intención que el
   *       usuario no manifestó.</li>
   * </ul>
   *
   * <p>NO valida nada más. Que la instancia esté pinada, que el tramo destino no
   * exista o que el movimiento rompa una regla dura son veredictos del SERVIDOR, y
   * anticiparlos aquí sería un cuarto espejo de las restricciones (misma razón por
   * la que `slotsOcupados` se queda en «hay clase» y no crece hacia una verificación).
   *
   * <p>Pintado NO optimista, como el despinado (D-F8.6-ii-5): la rejilla no se mueve
   * hasta el 200. Con el 200 se refresca solo lo afectado, sin recargar la proyección
   * entera y sin regenerar nada.
   */
  protected alSoltar(a: AjusteInstancia): void {
    this.limpiarAjuste();
    const id = this.idCargado;
    if (id === null) {
      return;
    }
    if (a.ocupantes.length >= 2) {
      this.avisoAjuste.set(
        `Hay ${a.ocupantes.length} clases en ese tramo: no puedo saber con cuál intercambiar.`,
      );
      return;
    }
    const arrastrada: ReferenciaInstancia = {
      actividadCodigo: a.actividadCodigo,
      indice: a.indice,
    };
    if (a.ocupantes.length === 0) {
      this.enVuelo.set('ajuste');
      this.ajustes
        .mover(id, { actividadCodigo: a.actividadCodigo, indice: a.indice, dia: a.dia, orden: a.orden })
        .subscribe({
          next: (filas) => {
            this.enVuelo.set(null);
            this.aplicarAjuste([{ ref: arrastrada, filas }]);
          },
          error: (err) => this.fallarAjuste(err),
        });
      return;
    }
    const ocupante = a.ocupantes[0];
    const segunda: ReferenciaInstancia = {
      actividadCodigo: ocupante.actividadCodigo,
      indice: ocupante.indice,
    };
    this.enVuelo.set('ajuste');
    this.ajustes.intercambiar(id, { primera: arrastrada, segunda }).subscribe({
      next: (r) => {
        this.enVuelo.set(null);
        // Cada lista a SU lado. El backend manda dos precisamente para no obligar a
        // reagrupar por actividadCodigo, que además sería irreversible cuando las dos
        // instancias son repeticiones de la MISMA actividad.
        this.aplicarAjuste([
          { ref: arrastrada, filas: r.primera },
          { ref: segunda, filas: r.segunda },
        ]);
      },
      error: (err) => this.fallarAjuste(err),
    });
  }

  /** Deja el bloque de ajuste en blanco. Se llama al empezar CADA gesto. */
  private limpiarAjuste(): void {
    this.errorAjuste.set(null);
    this.violacionesAjuste.set([]);
    this.avisoAjuste.set(null);
  }

  /**
   * Aplica al horario vigente las filas que devolvió el servidor, una entrada por
   * instancia afectada. NO recarga la proyección: el servidor ya devolvió el estado
   * nuevo de lo único que cambió, y un GET entero descartaría esa respuesta para
   * volver a pedir lo mismo.
   *
   * <p>Con `proyeccion()` en null no hay nada que refrescar y se calla: solo puede
   * pasar si la carga falló entre el gesto y la respuesta, y en ese caso la rejilla
   * ni siquiera está montada.
   */
  private aplicarAjuste(
    cambios: readonly { ref: ReferenciaInstancia; filas: readonly SesionVista[] }[],
  ): void {
    const p = this.proyeccion();
    if (p === null) {
      return;
    }
    let sesiones: readonly SesionVista[] = p.sesiones;
    for (const c of cambios) {
      sesiones = reemplazarInstancia(sesiones, c.ref, c.filas);
    }
    this.proyeccion.set({ ...p, sesiones: [...sesiones] });
  }

  /**
   * Cierra la espera y puebla el bloque de rechazo. La rejilla NO se toca: sin 200
   * no hubo movimiento, así que sigue pintando exactamente lo que pintaba.
   */
  private fallarAjuste(err: { status?: number; error?: FalloMovimiento }): void {
    this.enVuelo.set(null);
    this.errorAjuste.set(this.mensajeAjuste(err));
    this.violacionesAjuste.set(err?.error?.violaciones ?? []);
  }

  /**
   * Texto para un ajuste rechazado, decidido por la CAUSA del cuerpo. Función NUEVA
   * y no un ensanche de {@link mensaje}: aquella tiene un degradado fijo que habla
   * del PIN («El servidor rechazó el pin»), y es la única del proyecto que no toma
   * un degradado por parámetro. Meter aquí las causas del movimiento la obligaría a
   * decir dos cosas distintas según quién la llame.
   *
   * <p>La causa manda sobre el status —mismo criterio que {@link mensajeGeneracion}—
   * porque es el símbolo estable del hecho y los dos 409 (y los dos 404) no se
   * distinguen por el número. Sin causa reconocida, el degradado dice el estado y no
   * inventa un motivo: `TRAMO_INEXISTENTE` cae aquí a propósito —soltar en un recreo
   * no es alcanzable desde esta rejilla, que solo pinta tramos lectivos—, y también
   * cualquier causa que el backend añada después.
   *
   * <p>`INSTANCIA_INEXISTENTE` es la ÚNICA que arrastra la prosa del servidor, y es
   * deliberado: cuál de las dos instancias falta viaja SOLO ahí —el servidor la
   * interpola como `La instancia 'primera' (…)`— y no hay campo estructurado que lo
   * diga. Callarla dejaría al usuario con «una de las dos» sin saber cuál.
   */
  private mensajeAjuste(err: { status?: number; error?: FalloMovimiento }): string {
    const cuerpo = err?.error;
    switch (cuerpo?.causa) {
      case 'VIOLA_REGLA_DURA':
        return 'Ese cambio provoca conflictos que antes no existían:';
      case 'INSTANCIA_PINADA':
        return 'Esa clase está pinada y el pin manda sobre el arrastre: quita el pin antes de moverla.';
      case 'INSTANCIA_INEXISTENTE':
        return `Una de las dos clases ya no está en el horario. ${cuerpo.mensaje ?? ''}`.trim();
      case 'HORARIO_INEXISTENTE':
        return 'Ese horario ya no existe. Recarga la página.';
      case 'INSTANCIAS_IGUALES':
        return 'No se puede intercambiar una clase consigo misma.';
      default:
        return `El servidor rechazó el cambio (${err?.status ?? 'error'}).`;
    }
  }

  /**
   * PONE el pin de la instancia cuya CLAVE emite la rejilla (S145). Gemelo de
   * {@link alDespinar}: el mismo interruptor en el otro sentido.
   *
   * <p>El TRAMO no viaja en el evento —la rejilla emite solo la clave, simétrica con
   * el despinado— y se resuelve aquí contra la proyección VIGENTE, que es la única
   * autoridad sobre dónde está la instancia ahora mismo. Importa que sea la vigente y
   * no la del montaje: tras un ajuste la instancia se ha movido, y pinarla con el
   * tramo viejo clavaría la clase donde ya no está.
   *
   * <p>Si la clave no está en la proyección no hay tramo que mandar y se calla, con
   * el mismo criterio que {@link alDespinar} ante un id ausente: un error de UI no
   * ayuda a quien no tiene forma de arreglarlo.
   *
   * <p>`aulas: []` es DELIBERADO: el gesto fija el TRAMO y nada más. El cuerpo
   * describe el pin completo (D-5), así que el pin queda sin pines de aula.
   *
   * <p>SIN alta optimista, igual que el despinado (D-F8.6-ii-5): el candado no se
   * cierra hasta que llega la respuesta. La clave del índice se toma de esa
   * RESPUESTA, no de la petición: el backend es la autoridad sobre qué quedó pinado.
   */
  protected alPinar(clave: string): void {
    this.errorPin.set(null);
    const p = this.proyeccion();
    const fila = p === null ? undefined : filaDeClave(p.sesiones, clave);
    if (fila === undefined) {
      return;
    }
    this.bloqueos
      .guardar({
        actividadCodigo: fila.actividadCodigo,
        indice: fila.indice,
        tramo: { dia: fila.dia, orden: fila.tramo },
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
   * Dispara una generación de horario. Gateado por {@link avisosPrevalidacion}: si
   * es `null` (pre-validación no ejecutada) no hace nada —el botón ya está
   * deshabilitado, esta guarda es el cinturón—.
   *
   * <p>El diálogo se abre SIEMPRE (S145), no solo cuando hay avisos de severidad
   * `'ERROR'`. Hasta S144 la confirmación estaba condicionada a que la
   * pre-validación tuviera algo que decir, y sobre el centro real no tiene nada:
   * devuelve lista vacía, así que el diálogo NUNCA se abría y una generación de diez
   * minutos que sustituye el trabajo en curso salía con un solo clic y sin vuelta
   * atrás. Lo que hay que confirmar no son los avisos —eso es un agravante—, es el
   * COSTE de la operación, y ese existe con lista vacía igual que con lista llena.
   *
   * <p>Los errores se siguen filtrando y pasando por `data`: con lista vacía el
   * diálogo pinta solo el coste, y con avisos añade el detalle. La firma
   * `open<boolean, AvisoPrevalidacion[]>` no cambia.
   *
   * <p>Solo se procede si cierra con `true`; backdrop/Escape emiten `undefined` y
   * abortan sin lanzar nada.
   */
  protected generar(): void {
    const avisos = this.avisosPrevalidacion();
    if (avisos === null) {
      return;
    }
    const errores = avisos.filter((a) => a.severidad === 'ERROR');
    this.dialog
      .open<boolean, AvisoPrevalidacion[]>(ConfirmarGeneracion, { data: errores })
      .closed.subscribe((confirmado) => {
        if (confirmado === true) {
          this.lanzarGeneracion();
        }
      });
  }

  /**
   * Lanza el POST y refresca la vista con el horario que devuelve, por una de DOS
   * ramas según si ese id es el que ya está cargado ({@link idCargado}):
   *
   * <p>ID DISTINTO: se navega a `/horario/{id}` y NO se recarga aquí. La recarga la
   * dispara la emisión de `paramMap` al cambiar de ruta, igual que cualquier otra
   * entrada a la vista.
   *
   * <p>MISMO ID: se llama a {@link cargar} directamente, porque navegar NO serviría
   * de nada: el router IGNORA la navegación a la URL vigente —`onSameUrlNavigation`
   * vale `'ignore'` por defecto y `provideRouter(routes)` no pasa
   * `withRouterConfig`—, así que `paramMap` no reemite y la rejilla se quedaría con
   * el horario viejo. Se manifiesta en la PRIMERA generación de una instalación
   * nueva: la landing y el header apuntan a `/horario/1` clavado y el primer
   * horario recibe id 1, así que origen y destino coinciden.
   *
   * <p>En AMBAS ramas la proyección devuelta por el POST se descarta y la recarga es
   * por GET fresco (S93): rejilla, pines y diagnóstico no pueden pertenecer a
   * horarios distintos.
   *
   * <p>El error puebla {@link errorGeneracion} (señal propia, no gatea la rejilla).
   */
  private lanzarGeneracion(): void {
    this.errorGeneracion.set(null);
    this.enVuelo.set('generacion');
    this.service.generar().subscribe({
      next: (dto) => {
        this.enVuelo.set(null);
        if (dto.id === this.idCargado) {
          this.cargar(dto.id);
        } else {
          this.router.navigate(['/horario', dto.id]);
        }
      },
      error: (err) => {
        this.enVuelo.set(null);
        this.errorGeneracion.set(this.mensajeGeneracion(err));
      },
    });
  }

  /**
   * Texto para un fallo de generación, decidido por el STATUS y la `causa` del
   * cuerpo (S118) — NUNCA por la prosa del servidor, que es un mensaje de log en
   * bruto ("Estado CP-SAT: UNKNOWN") y no algo que enseñar a quien hace horarios.
   *
   * <p>Los cuatro textos existen para que cada uno diga qué HACER, y por eso no
   * pueden colapsarse en uno: ante un presupuesto agotado la acción es reintentar,
   * ante un catálogo infactible reintentar NO sirve —el resultado será idéntico— y
   * ante una jornada sin definir el sitio donde ir es otro. Un mensaje único
   * mandaría a esperar en balde a dos de cada tres.
   *
   * <p>La `causa` manda sobre el status cuando viene; el status es el respaldo para
   * un cuerpo que no la traiga (un proxy que lo recorte, una versión previa del
   * backend). Sin ninguno de los dos, el genérico con el número.
   */
  private mensajeGeneracion(err: {
    status?: number;
    error?: { causa?: string };
  }): string {
    const causa = err?.error?.causa;
    if (causa === 'PRESUPUESTO_AGOTADO' || err?.status === 503) {
      return 'Se agotó el tiempo de cálculo. Vuelve a intentarlo.';
    }
    if (causa === 'CONFIGURACION_INCOMPLETA') {
      return 'Falta configurar la jornada antes de generar un horario.';
    }
    if (causa === 'CATALOGO_INFACTIBLE' || err?.status === 422) {
      return 'Esta configuración no tiene solución. Revisa el catálogo.';
    }
    return `El servidor no pudo generar el horario (${err?.status ?? 'error'}).`;
  }

  /**
   * Texto de la espera, según QUÉ se espera. Un ajuste es una escritura corta y
   * anunciarle los minutos de un solve sería falso; una generación sin los minutos
   * vuelve a parecer un cuelgue, que es justo lo que S118 vino a arreglar. El
   * párrafo y la señal son los mismos: lo único que se bifurca es la frase.
   */
  protected textoGenerando(): string {
    return this.enVuelo() === 'ajuste'
      ? 'Aplicando el cambio…'
      : `Generando horario… puede tardar hasta ${this.MINUTOS_ANUNCIADOS} minutos.`;
  }

  protected cambiarVista(v: Vista): void {
    this.vista.set(v);
    this.entidad.set(this.entidades()[0] ?? '');
  }

  protected cambiarEntidad(e: string): void {
    this.entidad.set(e);
  }
}
