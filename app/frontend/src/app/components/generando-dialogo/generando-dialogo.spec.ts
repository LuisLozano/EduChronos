import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { Subject } from 'rxjs';

import { GenerandoDialogo, formatoMinSeg, valorBarra } from './generando-dialogo';

/**
 * Spec del diálogo de espera de la generación (S184, condición 3 de O-pre-demo).
 *
 * <p><b>Reloj falso, y SÓLO el del diálogo.</b> Es el primer spec del repo con
 * temporizadores falsos. Se falsean `setInterval`, `clearInterval` y `Date`, que son lo
 * que usa el componente, y NADA más: el planificador de Angular corre sobre
 * `setTimeout`, y falsearlo dejaría `whenStable()` esperando a un reloj que nadie avanza.
 * Por la misma razón `vi.getTimerCount()` cuenta aquí sólo intervalos: el del diálogo.
 */
describe('diálogo de espera de la generación', () => {
  let fixture: ComponentFixture<GenerandoDialogo>;

  /**
   * Doble de `DialogRef`: sólo `keydownEvents`, que es lo único que el diálogo toca
   * (S184 F3-bis). Emitir en él es lo que hace el CDK con cada `keydown` sobre `body`.
   */
  let teclas: Subject<KeyboardEvent>;

  async function montar(minutos: number): Promise<void> {
    TestBed.resetTestingModule();
    teclas = new Subject<KeyboardEvent>();
    await TestBed.configureTestingModule({
      imports: [GenerandoDialogo],
      providers: [
        { provide: DIALOG_DATA, useValue: { minutos } },
        { provide: DialogRef, useValue: { keydownEvents: teclas } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(GenerandoDialogo);
    await fixture.whenStable();
  }

  /** Avanza el reloj falso y deja que la vista repinte. */
  async function avanzar(ms: number): Promise<void> {
    vi.advanceTimersByTime(ms);
    await fixture.whenStable();
  }

  function raiz(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function barra(): HTMLProgressElement {
    return raiz().querySelector('progress.barra') as HTMLProgressElement;
  }

  function transcurrido(): string {
    return raiz().querySelector('.transcurrido')?.textContent?.replace(/\s+/g, ' ').trim() ?? '';
  }

  beforeEach(async () => {
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval', 'Date'] });
    await montar(10);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  /** Al abrir: el texto con los minutos elegidos, la barra vacía sobre el total y el reloj a cero. */
  it('(1) al abrir dice los minutos, la barra va de 0 sobre el total y el reloj marca 0:00', () => {
    expect(raiz().querySelector('h2')?.textContent?.trim()).toBe('Generando horario');
    expect(raiz().textContent).toContain(
      'Puede tardar hasta 10 minutos. Mientras tanto no se puede usar la aplicación.',
    );
    expect(barra().max).toBe(600);
    expect(barra().value).toBe(0);
    expect(transcurrido()).toBe('Transcurrido: 0:00 de 10:00');
    expect(raiz().querySelectorAll('button, a, input, [tabindex]').length).toBe(0);
  });

  /**
   * Con 30 minutos, el texto y el máximo son los de 30: lo que antes aseveraba el (43) de la
   * vista sobre su párrafo de espera vive aquí desde F3.
   */
  it('(2) con 30 minutos dice 30 y la barra tiene 1800 de máximo', async () => {
    await montar(30);

    expect(raiz().textContent).toContain('Puede tardar hasta 30 minutos.');
    expect(barra().max).toBe(1800);
    expect(transcurrido()).toBe('Transcurrido: 0:00 de 30:00');
  });

  /** A los 75 s, la barra y el texto avanzan juntos. */
  it('(3) tras 75 s la barra vale 75 y el texto dice 1:15 de 10:00', async () => {
    await avanzar(75_000);

    expect(barra().value).toBe(75);
    expect(transcurrido()).toBe('Transcurrido: 1:15 de 10:00');
  });

  /**
   * Pasado el total —cargar y guardar se comen unos segundos más—, la barra se queda en el
   * máximo y el texto pasa a «Terminando…». Se avanza a 630 s y no a más de 1200: por
   * debajo del doble del total, para que «sólo se dice pasado el doble» no pase.
   */
  it('(4) pasado el total la barra queda topada y dice «Terminando…»', async () => {
    await avanzar(630_000);

    expect(barra().value).toBe(600);
    expect(transcurrido()).toBe('Terminando…');
  });

  /** Al destruir el diálogo no queda ningún reloj vivo; antes, había exactamente uno. */
  it('(5) al destruirse limpia su intervalo', () => {
    expect(vi.getTimerCount()).toBe(1);

    fixture.destroy();

    expect(vi.getTimerCount()).toBe(0);
  });

  it('(6) valorBarra topa el transcurrido al total', () => {
    expect(valorBarra(0, 600)).toBe(0);
    expect(valorBarra(75, 600)).toBe(75);
    expect(valorBarra(600, 600)).toBe(600);
    expect(valorBarra(630, 600)).toBe(600);
  });

  it('(7) formatoMinSeg da m:ss sin partir en horas', () => {
    expect(formatoMinSeg(0)).toBe('0:00');
    expect(formatoMinSeg(75)).toBe('1:15');
    expect(formatoMinSeg(600)).toBe('10:00');
    expect(formatoMinSeg(3600)).toBe('60:00');
  });

  // --- S184 F3-bis · el Tab no sale del diálogo -----------------------------------

  /** Emite una tecla como la entrega el CDK y dice si alguien la anuló. */
  function pulsar(key: string, shiftKey = false): boolean {
    const evento = new KeyboardEvent('keydown', { key, shiftKey, cancelable: true });
    teclas.next(evento);
    return evento.defaultPrevented;
  }

  it('(8) el Tab se anula: el foco no sale del diálogo', () => {
    expect(pulsar('Tab')).toBe(true);
  });

  it('(9) el Shift+Tab también se anula', () => {
    expect(pulsar('Tab', true)).toBe(true);
  });

  /** El diálogo no se mete en las demás teclas: Escape ya lo anula `disableClose`. */
  it('(10) Enter y Escape no se tocan', () => {
    expect(pulsar('Enter')).toBe(false);
    expect(pulsar('Escape')).toBe(false);
  });

  /**
   * Destruido el diálogo, su suscripción está liberada: un Tab ya no se anula. Sin esto,
   * cada generación dejaría una escucha viva sobre un overlay muerto.
   */
  it('(11) al destruirse deja de anular el Tab', () => {
    fixture.destroy();

    expect(pulsar('Tab')).toBe(false);
  });
});
