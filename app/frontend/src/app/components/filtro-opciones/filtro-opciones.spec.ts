import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FiltroOpciones, filtrarOpciones } from './filtro-opciones';

/** Spec del filtro de selectores (S185, condición 5 de O-pre-demo). */
interface Op {
  codigo: string;
  nombre: string;
}
const op = (codigo: string, nombre = ''): Op => ({ codigo, nombre });
const texto = (o: Op): string => `${o.codigo} — ${o.nombre}`;
const codigo = (o: Op): string => o.codigo;

describe('filtrarOpciones', () => {
  const lista = [op('1ºA-ING'), op('4ºC-ING'), op('4ºC-FR'), op('2ºB-MAT')];

  it('con consulta vacía devuelve todas en su orden', () => {
    expect(filtrarOpciones(lista, texto, codigo, '', []).map(codigo)).toEqual([
      '1ºA-ING', '4ºC-ING', '4ºC-FR', '2ºB-MAT',
    ]);
  });

  it('devuelve las que casan, con la normalización de «4ºC»', () => {
    expect(filtrarOpciones(lista, texto, codigo, '4c', []).map(codigo)).toEqual(['4ºC-ING', '4ºC-FR']);
  });

  it('incluye la seleccionada que no casa, en su posición original', () => {
    expect(filtrarOpciones(lista, texto, codigo, '4c', ['1ºA-ING']).map(codigo)).toEqual([
      '1ºA-ING', '4ºC-ING', '4ºC-FR',
    ]);
  });

  it('filtra por el texto visible, no sólo por el código', () => {
    const tutores = [op('P01', 'Ana Ruiz'), op('P02', 'Luis Gil')];
    expect(filtrarOpciones(tutores, texto, codigo, 'ruiz', []).map(codigo)).toEqual(['P01']);
  });

  it('sin coincidencias ni seleccionadas devuelve una lista vacía', () => {
    expect(filtrarOpciones(lista, texto, codigo, 'zzz', [])).toEqual([]);
  });
});

describe('FiltroOpciones', () => {
  let fixture: ComponentFixture<FiltroOpciones<Op>>;

  /** G01, G02, …: «g1» casa sólo con G10 y G11. */
  const opciones = (n: number): Op[] =>
    Array.from({ length: n }, (_, i) => op(`G${String(i + 1).padStart(2, '0')}`));

  async function montar(n: number, seleccionados: string[] = []): Promise<void> {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({ imports: [FiltroOpciones] }).compileComponents();
    fixture = TestBed.createComponent(FiltroOpciones) as ComponentFixture<FiltroOpciones<Op>>;
    fixture.componentRef.setInput('opciones', opciones(n));
    fixture.componentRef.setInput('texto', texto);
    fixture.componentRef.setInput('codigo', codigo);
    fixture.componentRef.setInput('seleccionados', seleccionados);
    fixture.componentRef.setInput('etiqueta', 'grupos');
    await fixture.whenStable();
  }

  const campo = (): HTMLInputElement | null => fixture.nativeElement.querySelector('input');
  const recuento = (): string | undefined =>
    (fixture.nativeElement.querySelector('.filtro-opciones__recuento') as HTMLElement | null)?.textContent?.trim();
  const visibles = (): string[] => fixture.componentInstance.visibles().map(codigo);

  async function escribir(t: string): Promise<void> {
    const el = campo();
    expect(el).not.toBeNull();
    el!.value = t;
    el!.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  it('con 10 opciones no pinta el campo y deja todas visibles', async () => {
    await montar(10);
    expect(campo()).toBeNull();
    expect(visibles()).toHaveLength(10);
  });

  it('con 11 opciones pinta el campo, con su aria-label y el recuento completo', async () => {
    await montar(11);
    expect(campo()?.getAttribute('aria-label')).toBe('Filtrar grupos');
    expect(recuento()).toBe('11 de 11');
  });

  it('escribir filtra y el recuento dice cuántas se ven de cuántas hay', async () => {
    await montar(11);
    await escribir('g1');
    expect(visibles()).toEqual(['G10', 'G11']);
    expect(recuento()).toBe('2 de 11');
  });

  it('una seleccionada que no casa sigue visible', async () => {
    await montar(11, ['G03']);
    await escribir('g1');
    expect(visibles()).toEqual(['G03', 'G10', 'G11']);
  });

  it('si la lista baja a 10 o menos, la consulta escrita deja de aplicarse', async () => {
    await montar(11);
    await escribir('g1');
    fixture.componentRef.setInput('opciones', opciones(10));
    await fixture.whenStable();
    expect(campo()).toBeNull();
    expect(visibles()).toHaveLength(10);
  });

  it('limpiar() vacía la consulta y el campo', async () => {
    await montar(11);
    await escribir('g1');
    fixture.componentInstance.limpiar();
    await fixture.whenStable();
    expect(visibles()).toHaveLength(11);
    expect(campo()?.value).toBe('');
  });
});
