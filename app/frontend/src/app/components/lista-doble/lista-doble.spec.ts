import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { ListaDoble } from './lista-doble';

/**
 * Spec de la lista doble (S230, C-lista-doble). Se monta dentro de un anfitrión con un
 * `FormControl`, que es como la usan los formularios: lo que se mide es la cadena completa
 * gesto → valor del control, y no los métodos sueltos.
 */
interface Op {
  codigo: string;
  nombre: string;
}
const op = (codigo: string, nombre = ''): Op => ({ codigo, nombre });

@Component({
  imports: [ReactiveFormsModule, ListaDoble],
  template: `
    <span id="rotulo">Grupos</span>
    <app-lista-doble
      [formControl]="control"
      nombre="grupos"
      etiqueta="grupos"
      rotuloId="rotulo"
      [opciones]="opciones()"
      [texto]="texto"
      [codigo]="codigo"
      [filas]="filas()"
    />
    @if (dos()) {
      <app-lista-doble
        [formControl]="otro"
        nombre="otra"
        etiqueta="otros"
        [opciones]="opciones()"
        [texto]="texto"
        [codigo]="codigo"
      />
    }
  `,
})
class Anfitrion {
  readonly control = new FormControl<string[]>([], { nonNullable: true });
  readonly otro = new FormControl<string[]>([], { nonNullable: true });
  readonly opciones = signal<Op[]>([]);
  readonly filas = signal(8);
  readonly dos = signal(false);
  readonly texto = (o: Op): string => (o.nombre ? `${o.codigo} — ${o.nombre}` : o.codigo);
  readonly codigo = (o: Op): string => o.codigo;
}

describe('ListaDoble', () => {
  let fixture: ComponentFixture<Anfitrion>;
  let anfitrion: Anfitrion;

  /** Catálogo en un orden que NO es el de código: así se distingue el orden de la vista del
   *  del valor (enmienda 2). */
  const TRES = [op('1ºC'), op('1ºA'), op('1ºB')];
  /** Doce: con más de diez se pinta el filtro. «g1» casa con G10, G11 y G12. */
  const DOCE = Array.from({ length: 12 }, (_, i) => op(`G${String(i + 1).padStart(2, '0')}`));

  async function montar(opciones: Op[], valor: string[] = [], dos = false): Promise<void> {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({ imports: [Anfitrion] }).compileComponents();
    fixture = TestBed.createComponent(Anfitrion);
    anfitrion = fixture.componentInstance;
    anfitrion.opciones.set(opciones);
    anfitrion.control.setValue(valor);
    anfitrion.dos.set(dos);
    await fixture.whenStable();
  }

  const raiz = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const lista = (): HTMLElement => raiz().querySelector<HTMLElement>('[data-lista="grupos"]')!;
  const filas = (lado: 'elegidos' | 'disponibles'): HTMLElement[] => [
    ...lista().querySelectorAll<HTMLElement>(`[data-lado="${lado}"] li[data-codigo]`),
  ];
  const codigos = (lado: 'elegidos' | 'disponibles'): string[] => filas(lado).map((f) => f.dataset['codigo']!);
  const fila = (lado: 'elegidos' | 'disponibles', codigo: string): HTMLElement =>
    lista().querySelector<HTMLElement>(`[data-lado="${lado}"] li[data-codigo="${codigo}"]`)!;
  const titulo = (lado: 'elegidos' | 'disponibles'): string =>
    lista().querySelector(`[data-titulo="${lado}"]`)!.textContent!.trim();
  const anadir = (): HTMLButtonElement => lista().querySelector<HTMLButtonElement>('.lista-doble__anadir')!;
  const quitar = (): HTMLButtonElement => lista().querySelector<HTMLButtonElement>('.lista-doble__quitar')!;
  const filtro = (): HTMLInputElement | null => lista().querySelector<HTMLInputElement>('.filtro-opciones__campo');

  async function clic(el: HTMLElement): Promise<void> {
    el.click();
    await fixture.whenStable();
  }
  async function dobleClic(el: HTMLElement): Promise<void> {
    el.dispatchEvent(new MouseEvent('dblclick', { bubbles: true }));
    await fixture.whenStable();
  }
  async function tecla(el: HTMLElement, key: string): Promise<void> {
    el.dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true }));
    await fixture.whenStable();
  }
  async function escribir(consulta: string): Promise<void> {
    filtro()!.value = consulta;
    filtro()!.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  it('L1 pinta Elegidos y Disponibles ordenados por código, con su recuento', async () => {
    await montar(TRES, ['1ºB']);
    expect(codigos('elegidos')).toEqual(['1ºB']);
    expect(codigos('disponibles')).toEqual(['1ºA', '1ºC']);
    expect(titulo('elegidos')).toBe('Elegidos (1)');
    expect(titulo('disponibles')).toBe('Disponibles (2)');
  });

  it('L2 un clic marca sin mover; «‹ Añadir» pasa lo marcado y avisa al control', async () => {
    await montar(TRES);
    await clic(fila('disponibles', '1ºB'));
    expect(fila('disponibles', '1ºB').getAttribute('aria-selected')).toBe('true');
    expect(anfitrion.control.value).toEqual([]);
    expect(anfitrion.control.touched).toBe(false);

    await clic(anadir());
    expect(codigos('elegidos')).toEqual(['1ºB']);
    expect(codigos('disponibles')).toEqual(['1ºA', '1ºC']);
    expect(anfitrion.control.value).toEqual(['1ºB']);
    expect(anfitrion.control.touched).toBe(true);
  });

  it('L3 «Quitar ›» devuelve lo marcado de Elegidos a Disponibles', async () => {
    await montar(TRES, ['1ºA', '1ºB']);
    await clic(fila('elegidos', '1ºA'));
    await clic(quitar());
    expect(codigos('elegidos')).toEqual(['1ºB']);
    expect(codigos('disponibles')).toEqual(['1ºA', '1ºC']);
    expect(anfitrion.control.value).toEqual(['1ºB']);
  });

  it('L4 los botones están apagados sin nada marcado, y un segundo clic desmarca', async () => {
    await montar(TRES, ['1ºA']);
    expect(anadir().disabled).toBe(true);
    expect(quitar().disabled).toBe(true);
    await clic(fila('disponibles', '1ºC'));
    expect(anadir().disabled).toBe(false);
    expect(quitar().disabled).toBe(true);
    await clic(fila('disponibles', '1ºC'));
    expect(anadir().disabled).toBe(true);
  });

  it('L5 doble clic e Intro pasan la fila directamente; Espacio solo la marca', async () => {
    await montar(TRES);
    await dobleClic(fila('disponibles', '1ºC'));
    expect(anfitrion.control.value).toEqual(['1ºC']);

    await tecla(fila('disponibles', '1ºA'), ' ');
    expect(fila('disponibles', '1ºA').getAttribute('aria-selected')).toBe('true');
    expect(anfitrion.control.value).toEqual(['1ºC']);

    await tecla(fila('elegidos', '1ºC'), 'Enter');
    expect(anfitrion.control.value).toEqual([]);
    expect(codigos('disponibles')).toEqual(['1ºA', '1ºB', '1ºC']);
  });

  it('L6 el valor va en el orden del catálogo, no en el de la vista (enmienda 2)', async () => {
    await montar(TRES);
    await clic(fila('disponibles', '1ºA'));
    await clic(fila('disponibles', '1ºC'));
    await clic(anadir());
    // La vista ordena por código; el valor, como el catálogo (1ºC antes que 1ºA).
    expect(codigos('elegidos')).toEqual(['1ºA', '1ºC']);
    expect(anfitrion.control.value).toEqual(['1ºC', '1ºA']);
  });

  it('L7 un código que no está en el catálogo se conserva hasta el primer cambio', async () => {
    await montar(TRES, ['1ºA', 'FANTASMA']);
    expect(titulo('elegidos')).toBe('Elegidos (1)');
    expect(anfitrion.control.value).toEqual(['1ºA', 'FANTASMA']);
    await dobleClic(fila('disponibles', '1ºB'));
    expect(anfitrion.control.value).toEqual(['1ºA', '1ºB']);
  });

  it('L8 un setValue posterior repinta las listas y borra lo marcado', async () => {
    await montar(TRES);
    await clic(fila('disponibles', '1ºA'));
    anfitrion.control.setValue(['1ºC']);
    await fixture.whenStable();
    expect(codigos('elegidos')).toEqual(['1ºC']);
    expect(lista().querySelectorAll('[aria-selected="true"]').length).toBe(0);
    expect(anadir().disabled).toBe(true);
  });

  it('L9 con diez opciones o menos no hay filtro; con más, sí', async () => {
    await montar(DOCE.slice(0, 10));
    expect(filtro()).toBeNull();
    await montar(DOCE);
    expect(filtro()).not.toBeNull();
  });

  it('L10 el filtro recorta solo Disponibles, por el texto visible, y no oculta lo marcado', async () => {
    await montar(DOCE, ['G02']);
    await clic(fila('disponibles', 'G05'));
    await escribir('g1');
    expect(codigos('disponibles')).toEqual(['G05', 'G10', 'G11', 'G12']);
    expect(codigos('elegidos')).toEqual(['G02']);
    expect(lista().querySelector('.filtro-opciones__recuento')!.textContent!.trim()).toBe('4 de 11');

    await escribir('zzz');
    expect(codigos('disponibles')).toEqual(['G05']);
  });

  it('L11 sin coincidencias ni marcadas lo dice; sin disponibles, también', async () => {
    await montar(DOCE);
    await escribir('zzz');
    expect(lista().querySelector('[data-lado="disponibles"] .lista-doble__vacia')!.textContent!.trim())
      .toBe('Nada casa con el filtro.');
    await montar(TRES, ['1ºA', '1ºB', '1ºC']);
    expect(lista().querySelector('[data-lado="disponibles"] .lista-doble__vacia')!.textContent!.trim())
      .toBe('No quedan disponibles.');
    await montar(TRES);
    expect(lista().querySelector('[data-lado="elegidos"] .lista-doble__vacia')!.textContent!.trim())
      .toBe('Ningún elemento elegido.');
  });

  it('L12 con el control deshabilitado queda BLOQUEADA: nada se mueve y se ve', async () => {
    await montar(DOCE, ['G01']);
    anfitrion.control.disable();
    await fixture.whenStable();

    expect(lista().classList).toContain('lista-doble--bloqueada');
    expect(lista().getAttribute('aria-disabled')).toBe('true');
    expect(lista().querySelectorAll('button').length).toBe(0);
    expect(filtro()).toBeNull();
    expect(fila('elegidos', 'G01').hasAttribute('tabindex')).toBe(false);

    await dobleClic(fila('elegidos', 'G01'));
    await clic(fila('disponibles', 'G02'));
    await tecla(fila('disponibles', 'G03'), 'Enter');
    expect(codigos('elegidos')).toEqual(['G01']);
    expect(lista().querySelectorAll('[aria-selected="true"]').length).toBe(0);
    expect(anfitrion.control.value).toEqual(['G01']);
  });

  it('L13 DISCRIMINANTE de L12: habilitada, las filas tienen foco y hay botones', async () => {
    await montar(DOCE, ['G01']);
    expect(lista().classList).not.toContain('lista-doble--bloqueada');
    expect(lista().hasAttribute('aria-disabled')).toBe(false);
    expect(lista().querySelectorAll('button').length).toBe(2);
    expect(fila('elegidos', 'G01').getAttribute('tabindex')).toBe('0');
  });

  it('L14 la altura en filas llega a la hoja de estilos', async () => {
    await montar(TRES);
    const caja = (): HTMLElement => lista();
    expect(caja().style.getPropertyValue('--lista-doble-filas')).toBe('8');
    anfitrion.filas.set(5);
    await fixture.whenStable();
    expect(caja().style.getPropertyValue('--lista-doble-filas')).toBe('5');
  });

  it('L15 ids únicos con dos listas en pantalla, label for del filtro y listas nombradas', async () => {
    await montar(DOCE, [], true);
    const ids = [...raiz().querySelectorAll('[id]')].map((e) => e.id);
    expect(new Set(ids).size).toBe(ids.length);

    const campo = filtro()!;
    const etiqueta = lista().querySelector<HTMLLabelElement>(`label[for="${campo.id}"]`);
    expect(etiqueta!.textContent!.trim()).toBe('Filtrar grupos');

    for (const ul of lista().querySelectorAll('ul')) {
      const partes = ul.getAttribute('aria-labelledby')!.split(' ');
      expect(partes[0]).toBe('rotulo');
      for (const id of partes) {
        expect(raiz().querySelector(`#${id}`)).not.toBeNull();
      }
    }
  });
});
