import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { Observable, Subject, of, throwError } from 'rxjs';
import { DisponibilidadDialogo } from './disponibilidad-dialogo';
import { JornadaService } from '../../../services/jornada.service';
import { RestriccionHorariaService } from '../../../services/restriccion-horaria.service';
import { Profesor } from '../../../models/profesor.model';
import { JornadaDTO, TramoJornadaDTO } from '../../../models/jornada.model';
import { RestriccionHoraria } from '../../../models/restriccion-horaria.model';

/**
 * Diálogo de disponibilidad. Molde de `tutoria-dialogo.spec.ts`: servicios MOCKEADOS con
 * `vi.fn()`, `DialogRef` espiado y `DIALOG_DATA` con la entidad. La lógica de la rejilla
 * —derivación, pincel, conversión— la mide `rejilla-disponibilidad.spec.ts`; aquí se
 * mide el CABLEADO: que la plantilla pinte lo que dice el módulo, que los clics lleguen
 * al pincel con el objetivo correcto y que guardar mande lo que toca.
 *
 * <p>Se pulsa por el DOM, localizando cada botón por su `aria-label`, que es lo que oye
 * un lector de pantalla: así el mismo aserto mide la accesibilidad y el cableado.
 *
 * <p>El profesor tiene id 7 y no 1, como el grupo de la tutoría: una constante colada en
 * el componente caería en (1).
 */

const PROFESOR: Profesor = { id: 7, codigo: 'MAT1', nombreCompleto: 'Profesor de ejemplo' };

const DIAS_API = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES'];

/** El día tipo real del banco s137: seis lectivos y el recreo tras el tercero. */
const DIA_TIPO: ReadonlyArray<[string, string, number | null]> = [
  ['08:00', '09:00', 1],
  ['09:00', '10:00', 2],
  ['10:00', '11:00', 3],
  ['11:00', '11:30', null],
  ['11:30', '12:30', 4],
  ['12:30', '13:30', 5],
  ['13:30', '14:30', 6],
];

function jornada(persistida = true): JornadaDTO {
  let orden = 1;
  const tramos: TramoJornadaDTO[] = DIAS_API.flatMap((dia) =>
    DIA_TIPO.map(([horaInicio, horaFin, ordenEnDia]) => ({
      dia, horaInicio, horaFin, esLectivo: ordenEnDia !== null, orden: orden++, ordenEnDia,
    })));
  return { persistida, tramos };
}

/** Lo que devuelve el GET: una DURA con motivo el lunes a primera y una BLANDA el martes a cuarta. */
const PRECARGA: RestriccionHoraria[] = [
  { tipo: 'DURA', dia: 1, ordenEnDia: 1, motivo: 'guardia' },
  { tipo: 'BLANDA', dia: 2, ordenEnDia: 4, motivo: null },
];

function fallo(status: number, cuerpo: unknown = {}): Observable<never> {
  return throwError(() => new HttpErrorResponse({ status, statusText: 'x', error: cuerpo }));
}

describe('DisponibilidadDialogo', () => {
  let fixture: ComponentFixture<DisponibilidadDialogo>;
  let ref: { close: ReturnType<typeof vi.fn> };
  let jornadas: { obtener: ReturnType<typeof vi.fn> };
  let service: { obtener: ReturnType<typeof vi.fn>; reemplazar: ReturnType<typeof vi.fn> };

  async function montar(
    restricciones: Observable<RestriccionHoraria[]> = of(PRECARGA),
    laJornada: Observable<JornadaDTO> = of(jornada()),
  ): Promise<void> {
    ref = { close: vi.fn() };
    jornadas = { obtener: vi.fn().mockReturnValue(laJornada) };
    service = { obtener: vi.fn().mockReturnValue(restricciones), reemplazar: vi.fn() };
    TestBed.configureTestingModule({
      imports: [DisponibilidadDialogo],
      providers: [
        { provide: JornadaService, useValue: jornadas },
        { provide: RestriccionHorariaService, useValue: service },
        { provide: DialogRef, useValue: ref },
        { provide: DIALOG_DATA, useValue: PROFESOR },
      ],
    });
    fixture = TestBed.createComponent(DisponibilidadDialogo);
    fixture.detectChanges(); // dispara ngOnInit → forkJoin
    await fixture.whenStable();
  }

  const raiz = (): HTMLElement => fixture.nativeElement as HTMLElement;

  async function refrescar(): Promise<void> {
    fixture.detectChanges();
    await fixture.whenStable();
  }

  /** El botón cuyo `aria-label` EMPIEZA por el prefijo dado; revienta si no hay uno solo. */
  function boton(prefijo: string): HTMLButtonElement {
    const todos = [...raiz().querySelectorAll<HTMLButtonElement>('button[aria-label]')]
      .filter((b) => b.getAttribute('aria-label')!.startsWith(prefijo));
    if (todos.length !== 1) {
      throw new Error(`«${prefijo}»: ${todos.length} botones`);
    }
    return todos[0];
  }

  /** El `aria-label` de la celda de ese día y esa franja. */
  const celda = (dia: string, franja: string): string =>
    boton(`${dia} ${franja}:`).getAttribute('aria-label')!;

  /** Botón por su texto visible (pincel, Guardar, Cancelar). */
  function porTexto(texto: string): HTMLButtonElement {
    return [...raiz().querySelectorAll<HTMLButtonElement>('button')]
      .find((b) => b.textContent!.trim() === texto)!;
  }

  const guardarBoton = (): HTMLButtonElement =>
    raiz().querySelector<HTMLButtonElement>('.disponibilidad-dialogo__guardar')!;

  it('(1) al abrir pide la jornada y las restricciones con el id DEL PROFESOR', async () => {
    await montar();

    expect(jornadas.obtener).toHaveBeenCalledTimes(1);
    expect(service.obtener).toHaveBeenCalledWith(7);
    expect(service.obtener).toHaveBeenCalledTimes(1);
    expect(raiz().querySelector('.disponibilidad-dialogo__titulo')!.textContent)
      .toContain('Disponibilidad — Profesor de ejemplo');
  });

  it('(2) DISCRIMINANTE: con la jornada ya resuelta pero SIN restricciones sigue cargando', async () => {
    // Pintar la rejilla en cuanto llega la jornada afirmaría «disponible siempre» mientras
    // las restricciones aún viajan, y un Guardar a tiempo las BORRARÍA.
    await montar(new Subject<RestriccionHoraria[]>().asObservable());

    expect(raiz().querySelector('.disponibilidad-dialogo__cargando')).toBeTruthy();
    expect(raiz().querySelector('table')).toBeNull();
    expect(guardarBoton()).toBeNull();
  });

  it('(3) la rejilla pinta los estados precargados: 30 celdas, cada una con su estado', async () => {
    await montar();

    expect(raiz().querySelectorAll('.disponibilidad-dialogo__celda')).toHaveLength(30);
    expect(celda('Lunes', '08:00–09:00')).toBe('Lunes 08:00–09:00: No puede');
    expect(celda('Martes', '11:30–12:30')).toBe('Martes 11:30–12:30: Prefiere no');
    expect(celda('Miércoles', '08:00–09:00')).toBe('Miércoles 08:00–09:00: Disponible');
    // Y lo que se VE, no sólo lo que se oye: clase de estado y símbolo.
    const dura = boton('Lunes 08:00–09:00:');
    expect(dura.classList).toContain('disponibilidad-dialogo__celda--dura');
    expect(dura.querySelector('svg circle')).toBeTruthy();
    const blanda = boton('Martes 11:30–12:30:');
    expect(blanda.classList).toContain('disponibilidad-dialogo__celda--blanda');
    expect(blanda.querySelector('svg line')).toBeTruthy();
    expect(blanda.querySelector('svg circle')).toBeNull();
    expect(boton('Miércoles 08:00–09:00:').querySelector('svg')).toBeNull();
  });

  it('(4) el recreo es una fila separadora con sus horas y SIN botones', async () => {
    await montar();

    const recreo = raiz().querySelector('.disponibilidad-dialogo__recreo')!;
    expect(recreo.textContent).toContain('Recreo');
    expect(recreo.textContent).toContain('11:00–11:30');
    expect(recreo.querySelector('button')).toBeNull();
    // Y va en su sitio: la cuarta fila del cuerpo, tras el tercer lectivo.
    expect(raiz().querySelectorAll('tbody tr')[3]).toBe(recreo);
  });

  it('(5) las cabeceras de fila son las horas de la API, en orden', async () => {
    await montar();

    const cabeceras = [...raiz().querySelectorAll('tbody th[scope="row"]')]
      .map((th) => th.textContent!.trim());
    expect(cabeceras).toEqual([
      '08:00–09:00', '09:00–10:00', '10:00–11:00',
      '11:30–12:30', '12:30–13:30', '13:30–14:30',
    ]);
  });

  it('(6) el pincel: tres opciones, «No puede» activa por defecto, y el clic la cambia', async () => {
    await montar();

    const pulsada = (t: string): string | null => porTexto(t).getAttribute('aria-pressed');
    expect([pulsada('Disponible'), pulsada('Prefiere no'), pulsada('No puede')])
      .toEqual(['false', 'false', 'true']);

    porTexto('Prefiere no').click();
    await refrescar();

    expect([pulsada('Disponible'), pulsada('Prefiere no'), pulsada('No puede')])
      .toEqual(['false', 'true', 'false']);
  });

  it('(7) el clic en una CELDA le aplica el pincel a ella sola', async () => {
    await montar();

    boton('Miércoles 08:00–09:00:').click();
    await refrescar();

    expect(celda('Miércoles', '08:00–09:00')).toBe('Miércoles 08:00–09:00: No puede');
    expect(celda('Jueves', '08:00–09:00')).toBe('Jueves 08:00–09:00: Disponible');
    expect(celda('Miércoles', '09:00–10:00')).toBe('Miércoles 09:00–10:00: Disponible');
  });

  it('(8) el clic en la cabecera de un DÍA pinta sus seis tramos y ningún otro día', async () => {
    await montar();
    porTexto('Prefiere no').click();
    await refrescar();

    boton('Jueves, todos los tramos').click();
    await refrescar();

    for (const franja of ['08:00–09:00', '09:00–10:00', '10:00–11:00',
      '11:30–12:30', '12:30–13:30', '13:30–14:30']) {
      expect(celda('Jueves', franja)).toBe(`Jueves ${franja}: Prefiere no`);
    }
    expect(celda('Viernes', '08:00–09:00')).toBe('Viernes 08:00–09:00: Disponible');
    expect(celda('Lunes', '08:00–09:00')).toBe('Lunes 08:00–09:00: No puede');
  });

  it('(9) el clic en la cabecera de un TRAMO lo pinta en los cinco días; DISPONIBLE borra', async () => {
    await montar();
    porTexto('Disponible').click();
    await refrescar();

    boton('08:00–09:00, todos los días').click();
    await refrescar();

    // El lunes a primera era «No puede»: el pincel Disponible lo vacía.
    for (const dia of ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes']) {
      expect(celda(dia, '08:00–09:00')).toBe(`${dia} 08:00–09:00: Disponible`);
    }
    // Otro tramo no se toca.
    expect(celda('Martes', '11:30–12:30')).toBe('Martes 11:30–12:30: Prefiere no');
  });

  it('(10) guardar manda el cuerpo de aRestricciones, CON el motivo conservado, y cierra con true', async () => {
    await montar();
    service.reemplazar.mockReturnValue(of([]));

    boton('Miércoles 08:00–09:00:').click();
    await refrescar();
    guardarBoton().click();
    await refrescar();

    // El array ENTERO: el motivo 'guardia' de la fila que no se tocó tiene que viajar,
    // porque el PUT es reemplazo total y omitirlo lo borra.
    expect(service.reemplazar).toHaveBeenCalledWith(7, [
      { tipo: 'DURA', dia: 1, ordenEnDia: 1, motivo: 'guardia' },
      { tipo: 'BLANDA', dia: 2, ordenEnDia: 4, motivo: null },
      { tipo: 'DURA', dia: 3, ordenEnDia: 1, motivo: null },
    ]);
    expect(ref.close).toHaveBeenCalledWith(true);
  });

  it('(11) mientras guarda, Guardar queda deshabilitado', async () => {
    await montar();
    service.reemplazar.mockReturnValue(new Subject<RestriccionHoraria[]>().asObservable());

    expect(guardarBoton().disabled).toBe(false);
    guardarBoton().click();
    await refrescar();

    expect(guardarBoton().disabled).toBe(true);
    expect(ref.close).not.toHaveBeenCalled();
  });

  it('(12) un 400 con message lo muestra, NO cierra y deja reintentar', async () => {
    await montar();
    service.reemplazar.mockReturnValue(
      fallo(400, { error: 'Bad Request', message: 'No existe tramo lectivo con (dia=1, ordenEnDia=7)' }));

    guardarBoton().click();
    await refrescar();

    expect(ref.close).not.toHaveBeenCalled();
    const alerta = raiz().querySelector('[role="alert"]')!;
    expect(alerta.textContent!.trim()).toBe('No existe tramo lectivo con (dia=1, ordenEnDia=7)');
    // Sigue la rejilla, con lo marcado, y Guardar vuelve a estar disponible.
    expect(raiz().querySelector('table')).toBeTruthy();
    expect(guardarBoton().disabled).toBe(false);
  });

  it('(13) un 403 de curso archivado muestra el message de su RechazoCursoDTO', async () => {
    await montar();
    const texto = 'El curso 2024-2025 está archivado y es de solo lectura. '
      + 'Los cambios se hacen en el curso activo.';
    service.reemplazar.mockReturnValue(
      fallo(403, { causa: 'CURSO_SOLO_LECTURA', message: texto }));

    guardarBoton().click();
    await refrescar();

    expect(ref.close).not.toHaveBeenCalled();
    expect(raiz().querySelector('[role="alert"]')!.textContent!.trim()).toBe(texto);
  });

  it('(14) un error de carga muestra el degradado, sin rejilla y sin Guardar', async () => {
    await montar(fallo(500));

    const alerta = raiz().querySelector('[role="alert"]')!.textContent!;
    expect(alerta).toContain('No se pudo consultar la disponibilidad de Profesor de ejemplo');
    expect(alerta).toContain('500');
    expect(raiz().querySelector('table')).toBeNull();
    expect(guardarBoton()).toBeNull();
    expect(porTexto('Cerrar')).toBeTruthy();
  });

  it('(15) DISCRIMINANTE: jornada no guardada → aviso, sin rejilla y Guardar deshabilitado', async () => {
    // Con persistida:false la tabla de tramos está vacía y el PUT rechazaría cada fila.
    // Ofrecer la rejilla invitaría a marcar para perderlo todo con un 400.
    await montar(of(PRECARGA), of(jornada(false)));

    expect(raiz().querySelector('.disponibilidad-dialogo__aviso')!.textContent)
      .toContain('La jornada del centro todavía no está guardada');
    expect(raiz().querySelector('table')).toBeNull();
    expect(guardarBoton().disabled).toBe(true);
    guardarBoton().click();
    await refrescar();
    expect(service.reemplazar).not.toHaveBeenCalled();
  });

  it('(16) cancelar cierra SIN true y sin escribir nada', async () => {
    await montar();
    boton('Miércoles 08:00–09:00:').click();
    await refrescar();

    porTexto('Cancelar').click();

    expect(ref.close).toHaveBeenCalledWith();
    expect(ref.close).not.toHaveBeenCalledWith(true);
    expect(service.reemplazar).not.toHaveBeenCalled();
  });
});
