import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PanelPrevalidacion } from './panel-prevalidacion';
import { AvisoPrevalidacion } from '../../models/prevalidacion.model';

/**
 * Panel PRESENTACIONAL: inputs por `componentRef.setInput`, SIN
 * `HttpTestingController` —el transporte es asunto de `prevalidacion.service.spec.ts`—.
 * Zoneless (sin zone.js): el render se espera con `await fixture.whenStable()`,
 * NUNCA con `detectChanges()`, misma disciplina que `horario-view.spec.ts`.
 */

/** Aviso mínimo: solo importan `severidad` y `regla` para estos asertos. */
function aviso(severidad: string, regla: string): AvisoPrevalidacion {
  return { severidad, regla, entidadCodigo: 'X', demanda: 0, disponible: 0, descripcion: 'd' };
}

describe('panel de pre-validación', () => {
  let fixture: ComponentFixture<PanelPrevalidacion>;
  let raiz: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PanelPrevalidacion],
    }).compileComponents();

    fixture = TestBed.createComponent(PanelPrevalidacion);
    raiz = fixture.nativeElement as HTMLElement;
  });

  /**
   * DOS MITADES, cuatro aserciones. La rama `null` se lee del VALOR POR DEFECTO
   * del input —SIN `setInput`—: es lo único que mata M2 (`input(null)` →
   * `input([])`), porque un `setInput('avisos', null)` explícito sobrescribiría el
   * default mutado y la mutación sobreviviría en verde. La rama `[]` sí se setea.
   *
   * <p>Cada mitad asevera PRESENCIA de su clase Y AUSENCIA de la hermana: son
   * ramas DISJUNTAS por DOM, y sin la mitad de ausencia M3 —fundir las dos ramas
   * en `@if (!avisos()?.length)` con UNA sola clase— quedaría verde (la clase que
   * conservara seguiría apareciendo). La ausencia de la OTRA es lo que discrimina.
   */
  it('(28) null pinta pendiente y no limpia; [] pinta limpia y no pendiente', async () => {
    // Rama null: por DEFECTO del input, sin setInput (imprescindible para matar M2).
    await fixture.whenStable();
    expect(raiz.querySelector('.prevalidacion-pendiente')).not.toBeNull();
    expect(raiz.querySelector('.prevalidacion-limpia')).toBeNull();

    // Rama []: ejecutado y vacío.
    fixture.componentRef.setInput('avisos', []);
    await fixture.whenStable();
    expect(raiz.querySelector('.prevalidacion-limpia')).not.toBeNull();
    expect(raiz.querySelector('.prevalidacion-pendiente')).toBeNull();
  });

  /**
   * Contadores de la cabecera. Fixture con UN ERROR y UN AVISO a propósito: el
   * conteo del no-ERROR (1) es DISTINTO de la longitud total (2), así que un
   * `.contador-avisos` que contara `avisos.length` (M4) marcaría 2 y el aserto lo
   * caza. Si los dos fueran AVISO, longitud y no-ERROR coincidirían y no
   * discriminaría nada.
   */
  it('(29) los contadores separan ERROR del resto (1 y 1)', async () => {
    fixture.componentRef.setInput('avisos', [aviso('ERROR', 'R1'), aviso('AVISO', 'R2')]);
    await fixture.whenStable();

    expect(raiz.querySelector('.contador-errores')!.textContent!.trim()).toBe('1');
    expect(raiz.querySelector('.contador-avisos')!.textContent!.trim()).toBe('1');
  });

  /**
   * Las DOS entradas en el DOM, y `.es-error` SOLO en la del ERROR. Se asevera
   * sobre LAS DOS —presencia en la ERROR, AUSENCIA en la AVISO—: M5 (aplicar
   * `.es-error` a toda `.aviso-entrada` sin condición) solo muere por la mitad
   * NEGATIVA, la del AVISO. El cuerpo va desplegado porque hay un ERROR (colapso
   * inicial abierto ante error), así que las dos entradas se pintan.
   */
  it('(30) es-error marca solo la entrada ERROR, no la AVISO', async () => {
    fixture.componentRef.setInput('avisos', [aviso('ERROR', 'R1'), aviso('AVISO', 'R2')]);
    await fixture.whenStable();

    const entradas = raiz.querySelectorAll('.aviso-entrada');
    expect(entradas.length).toBe(2);
    expect(entradas[0].classList.contains('es-error')).toBe(true);
    expect(entradas[1].classList.contains('es-error')).toBe(false);
  });
});
