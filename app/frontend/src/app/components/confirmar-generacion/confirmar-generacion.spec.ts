import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';

import { ConfirmarGeneracion } from './confirmar-generacion';
import { AvisoPrevalidacion } from '../../models/prevalidacion.model';

/**
 * Spec PROPIO del diálogo (hueco 3): en `horario-view.spec` el `Dialog` va doblado,
 * así que el componente real —qué cierra cada botón y qué pinta— era cero-cobertura.
 * Aquí se monta el componente REAL con `DIALOG_DATA` inyectado y un `DialogRef`
 * ESPIADO por `useValue`: no hay overlay ni `Dialog.open`, solo el componente y su
 * ref, que es la frontera que el componente toca.
 *
 * <p>Los botones se pulsan por el DOM (`.confirmar`/`.cancelar`), no llamando a los
 * métodos protegidos: así se cubre también el cableado plantilla→método, no solo el
 * cuerpo del método.
 *
 * <p>Dos errores con textos DISTINTOS: (c) los asevera los DOS, porque con uno solo
 * "pinta el primero" y "pinta todos" serían indistinguibles.
 */

const ERROR_A: AvisoPrevalidacion = {
  severidad: 'ERROR',
  regla: 'DEMANDA_INSATISFACIBLE',
  entidadCodigo: 'MAT1',
  demanda: 31,
  disponible: 30,
  descripcion: 'MAT1 necesita 31 tramos y dispone de 30',
};

const ERROR_B: AvisoPrevalidacion = {
  severidad: 'ERROR',
  regla: 'AULA_INSUFICIENTE',
  entidadCodigo: 'GEO2',
  demanda: 12,
  disponible: 8,
  descripcion: 'GEO2 pide 12 aulas y solo hay 8',
};

describe('diálogo de confirmar generación', () => {
  let fixture: ComponentFixture<ConfirmarGeneracion>;
  let ref: { close: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    ref = { close: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [ConfirmarGeneracion],
      providers: [
        { provide: DialogRef, useValue: ref },
        { provide: DIALOG_DATA, useValue: [ERROR_A, ERROR_B] },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmarGeneracion);
    await fixture.whenStable();
  });

  /** El botón principal cierra con `true`: procede la generación. */
  it('(35) confirmar cierra la ref con true', () => {
    const boton = (fixture.nativeElement as HTMLElement).querySelector(
      'button.confirmar',
    ) as HTMLButtonElement;

    boton.click();

    expect(ref.close).toHaveBeenCalledTimes(1);
    expect(ref.close).toHaveBeenCalledWith(true);
  });

  /** El botón de cancelar cierra con `false`: aborta. Gemelo opuesto de (35). */
  it('(36) cancelar cierra la ref con false', () => {
    const boton = (fixture.nativeElement as HTMLElement).querySelector(
      'button.cancelar',
    ) as HTMLButtonElement;

    boton.click();

    expect(ref.close).toHaveBeenCalledTimes(1);
    expect(ref.close).toHaveBeenCalledWith(false);
  });

  /**
   * El cuerpo enumera el texto de CADA error de `DIALOG_DATA`. Se aseveran los DOS
   * (no solo el primero): una mutación que pintara `errores[0]` en vez de iterar
   * dejaría fuera el segundo y caería aquí.
   */
  it('(37) el cuerpo contiene la descripción de cada error recibido', () => {
    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(texto).toContain('MAT1 necesita 31 tramos y dispone de 30');
    expect(texto).toContain('GEO2 pide 12 aulas y solo hay 8');
  });
});
