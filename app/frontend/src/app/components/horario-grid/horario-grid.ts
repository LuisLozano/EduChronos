import { Component, computed, input } from '@angular/core';

import { SesionVista } from '../../models/horario.model';
import { DIAS, TRAMOS, agruparPorSlot, claveSlot } from '../../horario/proyeccion';

/**
 * Rejilla reutilizable de 5 días × 6 tramos. Recibe una lista de `SesionVista`
 * YA filtrada (por grupo, profesor o aula) y la agrupa por `(dia, tramo)`. Un
 * slot con varias entradas se pinta como celda-como-lista, una sub-entrada por
 * plaza; cada sub-entrada muestra asignatura, profesores (lista), aula y grupos
 * (lista): no asume cardinalidad 1 en profesores ni en grupos.
 */
@Component({
  selector: 'app-horario-grid',
  imports: [],
  templateUrl: './horario-grid.html',
  styleUrl: './horario-grid.css',
})
export class HorarioGrid {
  readonly sesiones = input.required<readonly SesionVista[]>();

  protected readonly dias = DIAS;
  protected readonly tramos = TRAMOS;

  private readonly slots = computed(() => agruparPorSlot(this.sesiones()));

  /** Sub-entradas del slot (dia, tramo); vacío si no hay ninguna. */
  protected entradas(dia: number, tramo: number): SesionVista[] {
    return this.slots().get(claveSlot(dia, tramo)) ?? [];
  }
}
