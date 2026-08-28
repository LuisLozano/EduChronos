import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CabeceraLista } from './cabecera-lista';

/**
 * Cabecera PRESENTACIONAL: los cuatro inputs son `required`, así que entran por
 * `componentRef.setInput` y no hay valor por defecto que probar. Sin
 * `HttpTestingController` ni `Dialog`: esta cabecera no carga nada ni abre nada.
 * Zoneless: el render se espera con `await fixture.whenStable()`, nunca con
 * `detectChanges()`, misma disciplina que `panel-prevalidacion.spec.ts`.
 */
describe('CabeceraLista', () => {
  let fixture: ComponentFixture<CabeceraLista>;
  let raiz: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CabeceraLista],
    }).compileComponents();

    fixture = TestBed.createComponent(CabeceraLista);
    raiz = fixture.nativeElement as HTMLElement;
    montar();
  });

  /** Estado de partida: contador visible, para que cada caso solo mueva lo suyo. */
  function montar(): void {
    fixture.componentRef.setInput('titulo', 'Aulas');
    fixture.componentRef.setInput('total', 42);
    fixture.componentRef.setInput('coincidencias', 42);
    fixture.componentRef.setInput('mostrarContador', true);
    fixture.componentRef.setInput('rotuloAlta', 'Nueva aula');
  }

  /** El elemento del componente, que es quien lleva la clase que lo pega. */
  function host(): HTMLElement {
    return fixture.debugElement.nativeElement as HTMLElement;
  }

  /** Texto del contador, ya recortado. `null` si no se pinta. */
  function contador(): string | null {
    return raiz.querySelector('.cabecera-lista__contador')?.textContent?.trim() ?? null;
  }

  /** Teclea en la caja como lo haría una persona: valor + evento `input`. */
  async function teclear(texto: string): Promise<void> {
    const caja = raiz.querySelector('.cabecera-lista__busqueda') as HTMLInputElement;
    caja.value = texto;
    caja.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  /**
   * Rótulo y texto del botón son DISTINTOS a propósito («Aulas» / «Nueva aula»):
   * una cabecera que derivara el botón del título —o al revés— pintaría el mismo
   * texto en los dos sitios y estos dos asertos lo cazan por separado. El del
   * botón es además el que protege al e2e, que lo localiza por texto exacto.
   */
  it('(1) pinta el rótulo en el título y el texto de alta en el botón', async () => {
    await fixture.whenStable();
    expect(raiz.querySelector('.cabecera-lista__titulo')?.textContent).toContain('Aulas');
    const boton = raiz.querySelector('.cabecera-lista__nuevo') as HTMLButtonElement;
    expect(boton.textContent?.trim()).toBe('Nueva aula');
  });

  /**
   * El contador pinta el VALOR de `total`, no la presencia del input: con 42 un
   * `{{ 0 }}` o un conteo inventado cae. Se comprueba dentro del `<span>` propio,
   * no en el texto del bloque, para que no lo satisfaga el rótulo ni el botón.
   */
  it('(2) con mostrarContador=true pinta el contador con el total', async () => {
    await fixture.whenStable();
    expect(contador()).toBe('42');
  });

  /**
   * La rama de ocultación es la que separa «cargando» de «catálogo vacío». Se
   * asevera la AUSENCIA del `<span>`, y además que el título SIGUE ahí: una
   * cabecera que se borrara entera con `mostrarContador=false` pasaría el primer
   * aserto y falla el segundo.
   */
  it('(3) con mostrarContador=false no pinta contador, pero sí el título', async () => {
    fixture.componentRef.setInput('mostrarContador', false);
    await fixture.whenStable();
    expect(raiz.querySelector('.cabecera-lista__contador')).toBeNull();
    expect(raiz.querySelector('.cabecera-lista__titulo')?.textContent).toContain('Aulas');
  });

  /**
   * El clic emite, y emite UNA vez: el contador de emisiones mata un `(click)`
   * duplicado o un handler colgado también del `<div>` contenedor.
   */
  it('(4) pulsar el botón emite alta exactamente una vez', async () => {
    await fixture.whenStable();
    let emisiones = 0;
    fixture.componentInstance.alta.subscribe(() => emisiones++);

    (raiz.querySelector('.cabecera-lista__nuevo') as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(emisiones).toBe(1);
  });

  /**
   * Los DOS estados del contador en un solo caso, con `total` y `coincidencias`
   * DISTINTOS a propósito (42 y 7): un contador que pintara siempre `total`
   * marcaría «42», y uno que pintara siempre `coincidencias` marcaría «7 de 7».
   * Con los dos números iguales ninguna de las dos mutaciones se vería.
   */
  it('(5) el contador pasa de «N» a «n de N» al escribir en la caja', async () => {
    fixture.componentRef.setInput('coincidencias', 7);
    await fixture.whenStable();
    expect(contador()).toBe('42');

    await teclear('mat');
    expect(contador()).toBe('7 de 42');
  });

  /**
   * Escribir solo espacios NO es buscar: no descarta ninguna fila, así que
   * anunciar «42 de 42» sería ruido. Es lo que mata quitar el `trim()`, y por eso
   * `coincidencias` vale aquí lo mismo que `total` —como valdría de verdad—: si
   * fueran distintos, el aserto caería por el número y no por la rama.
   */
  it('(6) escribir solo espacios se trata como no haber escrito nada', async () => {
    await teclear('   ');
    expect(contador()).toBe('42');
  });

  /**
   * El `model` publica hacia FUERA lo tecleado: sin esto la lista nunca se
   * enteraría de la consulta y la búsqueda entera sería decorativa.
   */
  it('(7) teclear actualiza el model de búsqueda', async () => {
    await teclear('3ºA');
    expect(fixture.componentInstance.busqueda()).toBe('3ºA');
  });

  /**
   * La clase que la pega va en el HOST, no en el div interior. No es un detalle
   * de estilo: sobre el div, el `position: sticky` queda encerrado en una caja de
   * su misma altura y se va con el scroll —medido en el navegador en S124—.
   * Este aserto es lo único que impide devolverla al div sin enterarse.
   */
  it('(8) la clase que la fija va en el elemento del componente, no en el div', async () => {
    await fixture.whenStable();
    expect(host().classList.contains('cabecera-lista-fija')).toBe(true);
    expect(
      raiz.querySelector('.cabecera-lista')?.classList.contains('cabecera-lista-fija'),
    ).toBe(false);
  });
});
