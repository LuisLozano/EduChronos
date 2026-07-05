import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { HorarioProyeccion } from '../../models/horario.model';
import { HorarioService } from '../../services/horario.service';
import { Vista, entidadesDeVista, filtrar } from '../../horario/proyeccion';
import { HorarioGrid } from '../horario-grid/horario-grid';

/**
 * Contenedor de las tres vistas: carga la proyección del horario `{id}` (param
 * de ruta) y ofrece un selector de vista (grupo / profesor / aula) y otro de
 * entidad dentro de la vista, cuyas opciones se DERIVAN de la proyección. El
 * filtrado y el agrupamiento por slot son lógica pura ({@link filtrar},
 * `agruparPorSlot`); este componente solo orquesta señales y delega la rejilla.
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

  protected readonly proyeccion = signal<HorarioProyeccion | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly vista = signal<Vista>('grupo');
  protected readonly entidad = signal<string>('');

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

  protected cambiarVista(v: Vista): void {
    this.vista.set(v);
    this.entidad.set(this.entidades()[0] ?? '');
  }

  protected cambiarEntidad(e: string): void {
    this.entidad.set(e);
  }
}
