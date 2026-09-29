import { Component, OnDestroy, inject, signal } from '@angular/core';
import { DIALOG_DATA } from '@angular/cdk/dialog';

/** Lo que recibe el diálogo de espera: los minutos que eligió quien genera. */
export interface DatosGenerando {
  minutos: number;
}

/**
 * Valor de la barra: el tiempo transcurrido, TOPADO al total. El solve puede pasarse
 * unos segundos del presupuesto —cargar, guardar—, y una barra que se sale de su
 * máximo no dice nada que «Terminando…» no diga mejor.
 */
export function valorBarra(transcurrido: number, total: number): number {
  return Math.min(transcurrido, total);
}

/** Segundos como «m:ss»: 0 → «0:00», 75 → «1:15», 3600 → «60:00» (los minutos no se parten en horas). */
export function formatoMinSeg(segundos: number): string {
  const minutos = Math.floor(segundos / 60);
  const resto = segundos % 60;
  return `${minutos}:${String(resto).padStart(2, '0')}`;
}

/**
 * Diálogo MODAL de espera mientras se genera un horario (S184, condición 3 de
 * O-pre-demo). Lo abre `HorarioView` con `disableClose` y lo cierra ella misma cuando
 * llega la respuesta: este componente no tiene botones ni nada enfocable, y no se cierra
 * solo. Por eso no inyecta `DialogRef`, a diferencia de `ConfirmarGeneracion`: nadie
 * cierra desde dentro.
 *
 * <p>La barra avanza con el TIEMPO TRANSCURRIDO sobre el elegido (decisión P), no con el
 * progreso del solver, que no lo publica: es una cota, no una promesa. El reloj es de
 * pared (`Date.now()` al crearse) y se refresca cada segundo; el intervalo se limpia al
 * destruir el diálogo, que es lo que pasa cuando la vista lo cierra.
 */
@Component({
  selector: 'app-generando-dialogo',
  templateUrl: './generando-dialogo.html',
  styleUrl: './generando-dialogo.css',
})
export class GenerandoDialogo implements OnDestroy {
  /** Minutos elegidos en la confirmación. */
  protected readonly minutos = inject<DatosGenerando>(DIALOG_DATA).minutos;

  /** El máximo de la barra, en segundos. */
  protected readonly totalSegundos = this.minutos * 60;

  /** Segundos enteros transcurridos desde que se abrió el diálogo. */
  protected readonly transcurrido = signal(0);

  protected readonly valorBarra = valorBarra;
  protected readonly formatoMinSeg = formatoMinSeg;

  private readonly inicio = Date.now();

  private readonly reloj = setInterval(
    () => this.transcurrido.set(Math.floor((Date.now() - this.inicio) / 1000)),
    1000,
  );

  ngOnDestroy(): void {
    clearInterval(this.reloj);
  }
}
