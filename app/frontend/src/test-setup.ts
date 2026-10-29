/**
 * Setup global de vitest, declarado en `setupFiles` del target `test` de `angular.json`
 * (S262, `D-resize-observer-jsdom`).
 *
 * <p>jsdom no define `ResizeObserver`, así que el `afterNextRender` de la rejilla
 * lanzaba `ReferenceError` en cada fixture, en silencio: vitest solo enseña el
 * `stderr` cuando algo falla. Con el error, el disparador por `altoCelda` del
 * `afterRenderEffect` de D11 no lo ejercitaba ningún test.
 *
 * <p>El doble no mide nada ni despierta solo: en jsdom no hay tamaños que vigilar. Un
 * test lo despierta con {@link ResizeObserverDoble.disparar}.
 */
export class ResizeObserverDoble implements ResizeObserver {
  /** Los observadores con algún elemento observado; `disconnect` los saca. */
  private static readonly vivos = new Set<ResizeObserverDoble>();

  private readonly objetivos = new Set<Element>();

  constructor(private readonly callback: ResizeObserverCallback) {}

  observe(objetivo: Element): void {
    this.objetivos.add(objetivo);
    ResizeObserverDoble.vivos.add(this);
  }

  unobserve(objetivo: Element): void {
    this.objetivos.delete(objetivo);
  }

  disconnect(): void {
    this.objetivos.clear();
    ResizeObserverDoble.vivos.delete(this);
  }

  /**
   * Llama al callback de cada observador vivo que observa `objetivo`, sin entradas:
   * la rejilla no las lee, vuelve a medir el DOM. Devuelve cuántos ha llamado, para
   * que el test afirme que había alguno y un cero no pase por «sin efecto».
   */
  static disparar(objetivo: Element): number {
    let llamados = 0;
    for (const observador of [...ResizeObserverDoble.vivos]) {
      if (observador.objetivos.has(objetivo)) {
        observador.callback([], observador);
        llamados++;
      }
    }
    return llamados;
  }
}

globalThis.ResizeObserver = ResizeObserverDoble;
