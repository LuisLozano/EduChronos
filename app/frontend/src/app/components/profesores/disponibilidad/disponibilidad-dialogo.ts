import { Component, OnInit, inject, signal } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { JornadaService } from '../../../services/jornada.service';
import { RestriccionHorariaService } from '../../../services/restriccion-horaria.service';
import { Profesor } from '../../../models/profesor.model';
import { RestriccionHoraria } from '../../../models/restriccion-horaria.model';
import {
  Estado,
  EstadosCeldas,
  Objetivo,
  Rejilla,
  aRestricciones,
  aplicarPincel,
  claveCelda,
  construirRejilla,
  desdeRestricciones,
} from './rejilla-disponibilidad';

/**
 * En qué estado está la pantalla. UN valor, como {@code EstadoTutoria}.
 *
 * <p>`'sin-jornada'` NO es un error de carga: las dos peticiones han ido bien, pero la
 * jornada es la PROPUESTA sintetizada (`persistida:false`, `JornadaService.java:119-122`).
 * Sobre ella no se puede guardar nada: la tabla `tramo_semanal` está vacía y el `PUT`
 * rechazaría cada fila con «No existe tramo lectivo…» (`RestriccionHorariaService.java:194-196`).
 * Pintar la rejilla invitaría a marcar una tarde entera para perderla con un 400.
 */
export type EstadoDisponibilidad = 'cargando' | 'cargado' | 'sin-jornada' | 'error';

/** Rótulo de cada estado de celda, el mismo en el pincel, la leyenda y el `aria-label`. */
const ROTULOS: Readonly<Record<Estado, string>> = {
  DISPONIBLE: 'Disponible',
  BLANDA: 'Prefiere no',
  DURA: 'No puede',
};

/**
 * Diálogo de la DISPONIBILIDAD de un profesor: consulta y reemplazo de
 * `/api/profesores/{id}/restricciones-horarias` sobre la rejilla de la jornada
 * (C-rejilla-disponibilidad, S167). Molde de {@code TutoriaDialogo}: entidad directa en
 * el `DIALOG_DATA`, dos fuentes con un `forkJoin` que gatea el estado, `mensaje()`
 * copiado con texto propio y cierre con `true` sólo si hubo escritura.
 *
 * <p><b>El componente no sabe de rejillas.</b> Derivar filas y columnas, pintar con el
 * pincel y convertir a la lista del `PUT` es de `rejilla-disponibilidad.ts`, que es puro
 * y tiene su spec. Aquí sólo se guarda el estado y se enlaza la plantilla.
 *
 * <p><b>Los motivos sobreviven.</b> El `PUT` es un reemplazo total y el `motivo` no se
 * edita en esta pantalla, así que se guarda la lista ORIGINAL del `GET` y
 * {@link aRestricciones} la usa para reenviar el motivo de cada celda que conserve su
 * tipo, y las filas que no caigan en ninguna celda tal cual.
 *
 * <p>Las horas de las cabeceras de fila se pintan como las da la API. El riesgo de zona
 * horaria que llevó a {@code HorarioGrid} a no pintarlas (D8) es la deuda
 * D-hora-tramo-dependiente-de-zona, con sede en Fase 12: decisión cerrada de S167.
 */
@Component({
  selector: 'app-disponibilidad-dialogo',
  imports: [NgTemplateOutlet],
  templateUrl: './disponibilidad-dialogo.html',
  styleUrl: './disponibilidad-dialogo.css',
})
export class DisponibilidadDialogo implements OnInit {
  private readonly jornadas = inject(JornadaService);
  private readonly service = inject(RestriccionHorariaService);
  protected readonly ref = inject<DialogRef<boolean>>(DialogRef);

  /** El profesor cuya disponibilidad se edita. Siempre presente: lo pone quien abre. */
  protected readonly profesor = inject<Profesor>(DIALOG_DATA);

  protected readonly estado = signal<EstadoDisponibilidad>('cargando');
  protected readonly guardando = signal(false);
  /** Error de consulta o de escritura. Vacío = sin error. */
  protected readonly error = signal('');

  protected readonly rejilla = signal<Rejilla>({ columnas: [], filas: [] });
  protected readonly estados = signal<EstadosCeldas>(new Map());
  /** La lista del `GET`, intacta: la necesita {@link aRestricciones} al guardar. */
  private readonly original = signal<readonly RestriccionHoraria[]>([]);

  /** Estado que aplica el pincel. «No puede» por defecto: es el caso que más se marca. */
  protected readonly pincel = signal<Estado>('DURA');
  protected readonly opcionesPincel: readonly Estado[] = ['DISPONIBLE', 'BLANDA', 'DURA'];
  protected readonly rotulos = ROTULOS;

  /**
   * Pide la jornada y las restricciones, y no deriva nada hasta tener las DOS: sin
   * jornada no hay rejilla donde pintar las restricciones, y sin restricciones una
   * rejilla vacía afirmaría que el profesor está siempre disponible.
   */
  ngOnInit(): void {
    forkJoin({
      jornada: this.jornadas.obtener(),
      restricciones: this.service.obtener(this.profesor.id),
    }).subscribe({
      next: ({ jornada, restricciones }) => {
        if (!jornada.persistida) {
          this.estado.set('sin-jornada');
          return;
        }
        const rejilla = construirRejilla(jornada.tramos);
        const { estados, original } = desdeRestricciones(rejilla, restricciones);
        this.rejilla.set(rejilla);
        this.estados.set(estados);
        this.original.set(original);
        this.estado.set('cargado');
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(
          err, `No se pudo consultar la disponibilidad de ${this.profesor.nombreCompleto}`));
        this.estado.set('error');
      },
    });
  }

  /** Estado de una celda para la plantilla. */
  protected estadoDe(dia: number, ordenEnDia: number): Estado {
    return this.estados().get(claveCelda(dia, ordenEnDia))?.estado ?? 'DISPONIBLE';
  }

  protected pintar(objetivo: Objetivo): void {
    this.estados.set(aplicarPincel(this.estados(), this.pincel(), objetivo));
  }

  /**
   * Reemplaza las restricciones del profesor. En error PRESENTA el mensaje y NO cierra,
   * como la tutoría: el usuario tiene que poder leerlo y lo marcado no se pierde.
   */
  protected guardar(): void {
    this.guardando.set(true);
    this.error.set('');

    const cuerpo = aRestricciones(this.estados(), this.original());

    this.service.reemplazar(this.profesor.id, cuerpo).subscribe({
      next: () => this.ref.close(true),
      error: (err: HttpErrorResponse) => {
        this.error.set(this.mensaje(err, 'No se pudo guardar la disponibilidad'));
        this.guardando.set(false);
      },
    });
  }

  /** Salida sin escribir: el consumidor no recarga. */
  protected cerrar(): void {
    this.ref.close();
  }

  /**
   * Traduce error Http a texto de usuario. Mismo patrón que `TutoriaDialogo`, copiado con
   * texto propio (D-F8.6). El 403 de curso archivado trae `message` en su cuerpo escrito
   * a mano (`RechazoCursoDTO`), así que cae por la misma rama que un 400 del servicio.
   */
  private mensaje(err: HttpErrorResponse, degradado: string): string {
    const cuerpo = err?.error as { message?: string; error?: string } | undefined;
    return cuerpo?.message || cuerpo?.error || `${degradado} (${err?.status ?? 'error'}).`;
  }
}
