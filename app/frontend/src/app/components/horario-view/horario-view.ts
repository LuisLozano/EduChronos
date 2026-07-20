import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { HorarioProyeccion } from '../../models/horario.model';
import { HorarioService } from '../../services/horario.service';
import { BloqueoService } from '../../services/bloqueo.service';
import { Vista, entidadesDeVista, filtrar } from '../../horario/proyeccion';
import { clavePin, indicePines } from '../../horario/pines';
import { HorarioGrid, SueltaInstancia } from '../horario-grid/horario-grid';

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
  imports: [HorarioGrid],
  templateUrl: './horario-view.html',
  styleUrl: './horario-view.css',
})
export class HorarioView {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(HorarioService);
  private readonly bloqueos = inject(BloqueoService);

  protected readonly proyeccion = signal<HorarioProyeccion | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly vista = signal<Vista>('grupo');
  protected readonly entidad = signal<string>('');

  /** Claves de {@link clavePin} de las instancias pinadas, en TODO el horario. */
  protected readonly pinadas = signal<ReadonlySet<string>>(new Set<string>());
  /** Último rechazo del backend (reglas de D-3); se limpia al siguiente intento. */
  protected readonly errorPin = signal<string | null>(null);

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
    this.bloqueos.listar().subscribe({
      next: (bs) => this.pinadas.set(indicePines(bs)),
      error: () => this.errorPin.set('No se pudieron cargar los pines existentes.'),
    });
  }

  private cargar(id: number): void {
    this.error.set(null);
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
        next: (b) => this.pinadas.set(new Set([...this.pinadas(), clavePin(b.actividadCodigo, b.indice)])),
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

  protected cambiarVista(v: Vista): void {
    this.vista.set(v);
    this.entidad.set(this.entidades()[0] ?? '');
  }

  protected cambiarEntidad(e: string): void {
    this.entidad.set(e);
  }
}
