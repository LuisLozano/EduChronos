import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { SelectorFiltrable } from './selector-filtrable';

/**
 * Spec del selector con filtro dentro (S260, C-combobox, F1.3). Un caso por punto del contrato
 * (F1.2) y por cada parte de la condición 3 de O-combobox. Cada aserto lee lo que ve quien lo
 * usa: las opciones del DOM, el valor del control, lo que recibió onChange (valueChanges), el
 * texto del campo, `aria-expanded` y el nombre accesible del campo.
 */
interface Op {
  codigo: string;
  nombre: string;
}
const op = (codigo: string, nombre = ''): Op => ({ codigo, nombre });
/** G01…G12; «g1» casa sólo con G10, G11 y G12. */
const doce = (): Op[] => Array.from({ length: 12 }, (_, i) => op(`G${String(i + 1).padStart(2, '0')}`));

@Component({
  imports: [SelectorFiltrable, ReactiveFormsModule],
  template: `
    <label for="sf-prueba">Aula del grupo</label>
    <app-selector-filtrable
      idEntrada="sf-prueba"
      etiqueta="aulas"
      [opciones]="opciones()"
      [texto]="texto"
      [codigo]="codigo"
      [opcionVacia]="opcionVacia()"
      [vaciaElegible]="vaciaElegible()"
      [formControl]="control"
    />
    <button type="button">fuera</button>
  `,
})
class ConFormulario {
  readonly opciones = signal<Op[]>(doce());
  readonly opcionVacia = signal<string | null>(null);
  readonly vaciaElegible = signal(true);
  readonly texto = (o: Op): string => (o.nombre ? `${o.codigo} — ${o.nombre}` : o.codigo);
  readonly codigo = (o: Op): string => o.codigo;
  readonly control = new FormControl('', { nonNullable: true });
}

/** Sin formulario, como la vista de horario: `[(valor)]`. */
@Component({
  imports: [SelectorFiltrable],
  template: `
    <label>
      grupo
      <app-selector-filtrable
        etiqueta="grupos"
        [opciones]="opciones"
        [texto]="identidad"
        [codigo]="identidad"
        [(valor)]="elegida"
        (valorChange)="avisos.push($event)"
      />
    </label>
  `,
})
class SinFormulario {
  readonly opciones = ['1ºA', '1ºB', '4ºC'];
  readonly identidad = (e: string): string => e;
  readonly elegida = signal('1ºA');
  readonly avisos: string[] = [];
}

/** Dentro de un formulario con su botón de enviar (EN-12). */
@Component({
  imports: [SelectorFiltrable, ReactiveFormsModule],
  template: `
    <form [formGroup]="form" (ngSubmit)="enviar()">
      <app-selector-filtrable etiqueta="aulas" [opciones]="opciones" [texto]="codigo" [codigo]="codigo" formControlName="aula" />
      <button type="submit">Guardar</button>
    </form>
  `,
})
class EnFormulario {
  readonly opciones = doce();
  readonly codigo = (o: Op): string => o.codigo;
  readonly form = new FormGroup({ aula: new FormControl('G03', { nonNullable: true }) });
  readonly enviar = vi.fn();
}

describe('SelectorFiltrable', () => {
  let fixture: ComponentFixture<ConFormulario>;
  let recibidos: string[];

  const raiz = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const campo = (): HTMLInputElement => raiz().querySelector('input')!;
  const anfitrion = (): ConFormulario => fixture.componentInstance;
  /** Textos de las opciones que hay en el DOM, en orden. */
  const opciones = (): string[] =>
    Array.from(raiz().querySelectorAll('[role="option"]')).map((o) => o.textContent!.trim());
  const expandida = (): string | null => campo().getAttribute('aria-expanded');
  const activa = (): string | undefined => {
    const id = campo().getAttribute('aria-activedescendant');
    return id ? raiz().querySelector(`[id="${id}"]`)?.textContent?.trim() : undefined;
  };

  async function montar(ajustes: (a: ConFormulario) => void = () => {}): Promise<void> {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({ imports: [ConFormulario] }).compileComponents();
    fixture = TestBed.createComponent(ConFormulario);
    ajustes(fixture.componentInstance);
    recibidos = [];
    fixture.componentInstance.control.valueChanges.subscribe((v) => recibidos.push(v));
    await fixture.whenStable();
  }

  async function tecla(key: string): Promise<void> {
    campo().dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true }));
    await fixture.whenStable();
  }

  async function escribir(texto: string): Promise<void> {
    campo().focus();
    campo().value = texto;
    campo().dispatchEvent(new InputEvent('input', { bubbles: true }));
    await fixture.whenStable();
  }

  async function clic(texto: string): Promise<void> {
    const el = Array.from(raiz().querySelectorAll<HTMLElement>('[role="option"]')).find(
      (o) => o.textContent!.trim() === texto,
    );
    expect(el).toBeDefined();
    el!.click();
    await fixture.whenStable();
  }

  /** Elige con el teclado: ↓ abre, ↓ hasta la opción, Intro. */
  async function elegirConTeclado(texto: string): Promise<void> {
    campo().focus();
    await tecla('ArrowDown');
    for (let i = 0; i < 20 && activa() !== texto; i++) {
      await tecla('ArrowDown');
    }
    expect(activa()).toBe(texto);
    await tecla('Enter');
  }

  describe('cerrado', () => {
    it('muestra el texto de lo elegido, sin lista en el DOM y con aria-expanded="false"', async () => {
      await montar((a) => {
        a.opciones.set([op('A1', 'Aula 1'), op('A2', 'Aula 2')]);
        a.control.setValue('A2');
      });
      expect(campo().value).toBe('A2 — Aula 2');
      expect(expandida()).toBe('false');
      expect(opciones()).toEqual([]);
    });

    it('writeValue cambia el texto del campo (el formulario manda)', async () => {
      await montar((a) => a.control.setValue('G03'));
      anfitrion().control.setValue('G07');
      await fixture.whenStable();
      expect(campo().value).toBe('G07');
      expect(recibidos).toEqual(['G07']);
    });

    it('writeValue del MISMO valor con algo escrito devuelve el texto de lo elegido', async () => {
      await montar((a) => a.control.setValue('G03'));
      await escribir('g1');
      expect(campo().value).toBe('g1');
      anfitrion().control.setValue('G03');
      await fixture.whenStable();
      expect(campo().value).toBe('G03');
    });

    it('no hay campo de búsqueda aparte: un único input, el del combobox', async () => {
      await montar();
      expect(raiz().querySelectorAll('input')).toHaveLength(1);
      expect(campo().getAttribute('role')).toBe('combobox');
      expect(raiz().querySelector('app-filtro-opciones, input[type="search"]')).toBeNull();
    });
  });

  describe('abrir y filtrar', () => {
    it('↓ con la lista cerrada la abre con TODAS las opciones', async () => {
      await montar((a) => a.control.setValue('G03'));
      campo().focus();
      await tecla('ArrowDown');
      expect(expandida()).toBe('true');
      expect(opciones()).toEqual(doce().map((o) => o.codigo));
    });

    it('escribir abre la lista y la acorta con la búsqueda, sin tocar el valor', async () => {
      await montar();
      await escribir('g1');
      expect(expandida()).toBe('true');
      expect(opciones()).toEqual(['G10', 'G11', 'G12']);
      expect(anfitrion().control.value).toBe('');
      expect(recibidos).toEqual([]);
    });

    it('la elegida sigue en la lista aunque la búsqueda no case con ella, en su sitio', async () => {
      await montar((a) => a.control.setValue('G03'));
      await escribir('g1');
      expect(opciones()).toEqual(['G03', 'G10', 'G11', 'G12']);
    });

    it('filtra también con diez opciones o menos (DA1)', async () => {
      await montar((a) => a.opciones.set([op('A1'), op('B1'), op('B2')]));
      await escribir('b');
      expect(opciones()).toEqual(['B1', 'B2']);
    });

    it('filtra por el texto visible, con la búsqueda de coincide() (nombre sin tildes)', async () => {
      await montar((a) => a.opciones.set([op('P01', 'Ana García'), op('P02', 'Luis Gil'), op('P03', 'Eva Ruiz')]));
      await escribir('garcia');
      expect(opciones()).toEqual(['P01 — Ana García']);
    });

    it('limpiar() cierra, vacía la búsqueda y devuelve el texto de lo elegido (EN-17)', async () => {
      await montar((a) => a.control.setValue('G03'));
      await escribir('g1');
      expect(opciones()).toEqual(['G03', 'G10', 'G11', 'G12']);
      fixture.debugElement.query(By.directive(SelectorFiltrable)).componentInstance.limpiar();
      await fixture.whenStable();
      expect(expandida()).toBe('false');
      expect(campo().value).toBe('G03');
      await tecla('ArrowDown');
      expect(opciones()).toHaveLength(12);
    });

    it('abrir sin escribir tras cerrar enseña todas: la búsqueda anterior no se queda', async () => {
      await montar();
      await escribir('g1');
      await tecla('Escape');
      await tecla('ArrowDown');
      expect(opciones()).toHaveLength(12);
    });
  });

  describe('flechas, Intro y clic', () => {
    it('las flechas recorren las opciones y NO cambian el valor (selección explícita, DA5)', async () => {
      await montar((a) => a.control.setValue('G03'));
      campo().focus();
      await tecla('ArrowDown');
      expect(activa()).toBe('G03');
      await tecla('ArrowDown');
      expect(activa()).toBe('G04');
      await tecla('ArrowDown');
      expect(activa()).toBe('G05');
      await tecla('ArrowUp');
      expect(activa()).toBe('G04');
      expect(anfitrion().control.value).toBe('G03');
      expect(recibidos).toEqual([]);
      expect(raiz().querySelector('[aria-selected="true"]')?.textContent?.trim()).toBe('G03');
    });

    it('Intro elige la activa: fija el valor, avisa a onChange, pone su texto y cierra', async () => {
      await montar((a) => a.control.setValue('G03'));
      await elegirConTeclado('G05');
      expect(anfitrion().control.value).toBe('G05');
      expect(recibidos).toEqual(['G05']);
      expect(campo().value).toBe('G05');
      expect(expandida()).toBe('false');
      expect(opciones()).toEqual([]);
    });

    it('Intro tras filtrar elige entre las visibles', async () => {
      await montar();
      await escribir('g1');
      await tecla('ArrowDown');
      await tecla('ArrowDown');
      const elegida = activa();
      expect(['G10', 'G11', 'G12']).toContain(elegida);
      await tecla('Enter');
      expect(anfitrion().control.value).toBe(elegida);
      expect(campo().value).toBe(elegida);
      expect(expandida()).toBe('false');
    });

    it('Intro sobre la que ya estaba elegida no la quita: el valor se queda y se cierra', async () => {
      await montar((a) => a.control.setValue('G03'));
      campo().focus();
      await tecla('ArrowDown');
      expect(activa()).toBe('G03');
      await tecla('Enter');
      expect(anfitrion().control.value).toBe('G03');
      expect(recibidos).toEqual([]);
      expect(campo().value).toBe('G03');
      expect(expandida()).toBe('false');
    });

    it('clic en una opción la elige, avisa a onChange y cierra', async () => {
      await montar((a) => a.control.setValue('G03'));
      await escribir('g1');
      await clic('G11');
      expect(anfitrion().control.value).toBe('G11');
      expect(recibidos).toEqual(['G11']);
      expect(campo().value).toBe('G11');
      expect(expandida()).toBe('false');
    });
  });

  describe('cerrar sin elegir', () => {
    it('Escape cierra, devuelve el texto de lo elegido y no cambia el valor', async () => {
      await montar((a) => a.control.setValue('G03'));
      await escribir('g1');
      await tecla('ArrowDown');
      await tecla('Escape');
      expect(expandida()).toBe('false');
      expect(opciones()).toEqual([]);
      expect(campo().value).toBe('G03');
      expect(anfitrion().control.value).toBe('G03');
      expect(recibidos).toEqual([]);
    });

    it('perder el foco sin elegir hace lo mismo que Escape', async () => {
      await montar((a) => a.control.setValue('G03'));
      await escribir('g1');
      raiz().querySelector('button')!.focus();
      campo().dispatchEvent(new FocusEvent('focusout', { bubbles: true }));
      await new Promise((r) => setTimeout(r));
      await fixture.whenStable();
      expect(expandida()).toBe('false');
      expect(campo().value).toBe('G03');
      expect(anfitrion().control.value).toBe('G03');
      expect(recibidos).toEqual([]);
      expect(anfitrion().control.touched).toBe(true);
    });
  });

  describe('opción vacía', () => {
    it('va primera, no se filtra, y se puede elegir: el valor pasa a \'\'', async () => {
      await montar((a) => {
        a.opcionVacia.set('— sin aula —');
        a.control.setValue('G03');
      });
      await escribir('g1');
      expect(opciones()).toEqual(['— sin aula —', 'G03', 'G10', 'G11', 'G12']);
      await clic('— sin aula —');
      expect(anfitrion().control.value).toBe('');
      expect(recibidos).toEqual(['']);
      expect(campo().value).toBe('— sin aula —');
    });

    it('con vaciaElegible=false se ve, deshabilitada, y elegirla no cambia nada', async () => {
      await montar((a) => {
        a.opcionVacia.set('— elige —');
        a.vaciaElegible.set(false);
        a.control.setValue('G03');
      });
      campo().focus();
      await tecla('ArrowDown');
      const vacia = raiz().querySelector('[role="option"]')!;
      expect(vacia.textContent!.trim()).toBe('— elige —');
      expect(vacia.getAttribute('aria-disabled')).toBe('true');
      await clic('— elige —');
      expect(anfitrion().control.value).toBe('G03');
      expect(recibidos).toEqual([]);
    });

    it('con el valor \'\' el campo muestra el texto de la vacía', async () => {
      await montar((a) => a.opcionVacia.set('— sin tutor —'));
      expect(campo().value).toBe('— sin tutor —');
    });
  });

  describe('nombre accesible y estado', () => {
    it('el nombre accesible del campo sale de su <label for>, con un id propio', async () => {
      await montar();
      expect(campo().id).toBe('sf-prueba');
      expect(Array.from(campo().labels ?? []).map((l) => l.textContent!.trim())).toEqual(['Aula del grupo']);
    });

    it('disable() del control deshabilita el campo', async () => {
      await montar();
      anfitrion().control.disable();
      await fixture.whenStable();
      expect(campo().disabled).toBe(true);
    });
  });

  describe('Intro no envía el formulario (EN-12)', () => {
    let f: ComponentFixture<EnFormulario>;
    const entrada = (): HTMLInputElement => (f.nativeElement as HTMLElement).querySelector('input')!;

    async function montarEnFormulario(): Promise<void> {
      TestBed.resetTestingModule();
      await TestBed.configureTestingModule({ imports: [EnFormulario] }).compileComponents();
      f = TestBed.createComponent(EnFormulario);
      await f.whenStable();
    }

    async function pulsar(key: string): Promise<KeyboardEvent> {
      const evento = new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true });
      entrada().dispatchEvent(evento);
      await f.whenStable();
      return evento;
    }

    /**
     * Intro como lo trata el navegador: si nadie cancela el keydown, el formulario se envía
     * (envío implícito de un campo de texto). jsdom no lo hace con un evento sintético, así que
     * se hace aquí; sin esto el caso pasaría aunque el campo no protegiera nada.
     */
    async function introComoNavegador(): Promise<KeyboardEvent> {
      const evento = await pulsar('Enter');
      if (!evento.defaultPrevented) {
        (f.nativeElement as HTMLElement).querySelector('form')!.requestSubmit();
        await f.whenStable();
      }
      return evento;
    }

    it('(a) con la lista cerrada, Intro no envía', async () => {
      await montarEnFormulario();
      entrada().focus();
      expect(entrada().getAttribute('aria-expanded')).toBe('false');
      const evento = await introComoNavegador();
      expect(evento.defaultPrevented).toBe(true);
      expect(f.componentInstance.enviar).not.toHaveBeenCalled();
      expect(f.componentInstance.form.value).toEqual({ aula: 'G03' });
    });

    it('(b) con la lista abierta, flecha e Intro eligen y no envían', async () => {
      await montarEnFormulario();
      entrada().focus();
      await pulsar('ArrowDown');
      await pulsar('ArrowDown');
      const evento = await introComoNavegador();
      expect(evento.defaultPrevented).toBe(true);
      expect(f.componentInstance.form.value).toEqual({ aula: 'G04' });
      expect(entrada().getAttribute('aria-expanded')).toBe('false');
      expect(f.componentInstance.enviar).not.toHaveBeenCalled();
    });

    it('control: el botón de enviar sí envía (el espía y el formulario funcionan)', async () => {
      await montarEnFormulario();
      ((f.nativeElement as HTMLElement).querySelector('button[type="submit"]') as HTMLButtonElement).click();
      await f.whenStable();
      expect(f.componentInstance.enviar).toHaveBeenCalledTimes(1);
    });
  });

  describe('sin formulario: [(valor)]', () => {
    it('muestra el valor de quien lo usa y, al elegir, lo devuelve por valorChange', async () => {
      TestBed.resetTestingModule();
      await TestBed.configureTestingModule({ imports: [SinFormulario] }).compileComponents();
      const f = TestBed.createComponent(SinFormulario);
      await f.whenStable();
      const entrada = (f.nativeElement as HTMLElement).querySelector('input')!;
      expect(entrada.value).toBe('1ºA');
      expect(Array.from(entrada.labels ?? []).map((l) => l.textContent!.trim())).toEqual(['grupo']);

      entrada.focus();
      entrada.value = '4';
      entrada.dispatchEvent(new InputEvent('input', { bubbles: true }));
      await f.whenStable();
      const cuarto = Array.from((f.nativeElement as HTMLElement).querySelectorAll<HTMLElement>('[role="option"]')).find(
        (o) => o.textContent!.trim() === '4ºC',
      )!;
      cuarto.click();
      await f.whenStable();
      expect(f.componentInstance.elegida()).toBe('4ºC');
      expect(f.componentInstance.avisos).toEqual(['4ºC']);
      expect(entrada.value).toBe('4ºC');

      f.componentInstance.elegida.set('1ºB');
      await f.whenStable();
      expect(entrada.value).toBe('1ºB');
    });
  });
});
