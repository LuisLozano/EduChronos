import { ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { SelectorFiltrable } from '../components/selector-filtrable/selector-filtrable';

/**
 * Cómo eligen y leen los specs un `app-selector-filtrable` (S260, C-combobox, F3.1). Sustituye a
 * lo que hacían con el `<select>`: `value` + `change` para elegir, y `.options`, `.value` y
 * `.selectedIndex` para leer. `donde` es un selector CSS que casa con el HOST del selector.
 *
 * <p>Las opciones sólo están en el DOM con la lista abierta: {@link opcionesSelector} la abre con
 * ↓ si estaba cerrada y la vuelve a cerrar con Escape, que no cambia el valor.
 */
export interface OpcionLeida {
  /** El código de la opción ('' en la vacía): lo que era `option.value`. */
  valor: string;
  texto: string;
  elegida: boolean;
  deshabilitada: boolean;
}

const raiz = (fixture: ComponentFixture<unknown>): HTMLElement => fixture.nativeElement as HTMLElement;

export function campoSelector(fixture: ComponentFixture<unknown>, donde: string): HTMLInputElement | null {
  return raiz(fixture).querySelector<HTMLInputElement>(`${donde} input[role="combobox"]`);
}

/** El código elegido: lo que era `select.value`. */
export function valorSelector(fixture: ComponentFixture<unknown>, donde: string): string {
  return (fixture.debugElement.query(By.css(donde)).componentInstance as SelectorFiltrable<unknown>).valor();
}

/** Nombre accesible del campo: el texto de las `<label>` asociadas al input. */
export function nombreSelector(fixture: ComponentFixture<unknown>, donde: string): string {
  return Array.from(campoSelector(fixture, donde)!.labels ?? [])
    .map((l) => l.textContent!.trim())
    .join(' ');
}

async function tecla(fixture: ComponentFixture<unknown>, donde: string, key: string): Promise<void> {
  campoSelector(fixture, donde)!.dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true }));
  await fixture.whenStable();
}

/** Abre la lista con ↓ si está cerrada. Devuelve si la ha abierto. */
export async function abrirSelector(fixture: ComponentFixture<unknown>, donde: string): Promise<boolean> {
  const campo = campoSelector(fixture, donde)!;
  if (campo.getAttribute('aria-expanded') === 'true') {
    return false;
  }
  campo.focus();
  await tecla(fixture, donde, 'ArrowDown');
  return true;
}

/** Las opciones, en orden, con la vacía si la hay: lo que era `select.options`. */
export async function opcionesSelector(fixture: ComponentFixture<unknown>, donde: string): Promise<OpcionLeida[]> {
  const abierta = await abrirSelector(fixture, donde);
  const leidas = Array.from(raiz(fixture).querySelectorAll<HTMLElement>(`${donde} [role="option"]`)).map((o) => ({
    valor: o.getAttribute('data-valor') ?? '',
    texto: o.textContent!.trim(),
    elegida: o.getAttribute('aria-selected') === 'true',
    deshabilitada: o.getAttribute('aria-disabled') === 'true',
  }));
  if (abierta) {
    await tecla(fixture, donde, 'Escape');
  }
  return leidas;
}

/** Elige la opción de ese código con un clic: lo que era `value` + `change`. */
export async function elegirEnSelector(fixture: ComponentFixture<unknown>, donde: string, valor: string): Promise<void> {
  await abrirSelector(fixture, donde);
  const opcion = Array.from(raiz(fixture).querySelectorAll<HTMLElement>(`${donde} [role="option"]`)).find(
    (o) => (o.getAttribute('data-valor') ?? '') === valor,
  );
  if (opcion === undefined) {
    throw new Error(`No hay opción «${valor}» en ${donde}`);
  }
  opcion.click();
  await fixture.whenStable();
}

/** Escribe en el campo del selector, que es donde se filtra: lo que era escribir en el filtro aparte. */
export async function escribirEnSelector(fixture: ComponentFixture<unknown>, donde: string, texto: string): Promise<void> {
  const campo = campoSelector(fixture, donde)!;
  campo.focus();
  campo.value = texto;
  campo.dispatchEvent(new InputEvent('input', { bubbles: true }));
  await fixture.whenStable();
}
