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

  /**
   * Monta el diálogo con los avisos dados. Se extrae del `beforeEach` en S145
   * porque `data` ya no es siempre una lista con contenido: desde que la
   * confirmación se pide en TODA generación, la lista vacía es el caso NORMAL —el
   * catálogo del centro real pre-valida limpio— y tiene que poder montarse.
   */
  async function montar(avisos: AvisoPrevalidacion[]): Promise<void> {
    TestBed.resetTestingModule();
    ref = { close: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [ConfirmarGeneracion],
      providers: [
        { provide: DialogRef, useValue: ref },
        { provide: DIALOG_DATA, useValue: avisos },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmarGeneracion);
    await fixture.whenStable();
  }

  beforeEach(async () => {
    await montar([ERROR_A, ERROR_B]);
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

  /**
   * CON LISTA VACÍA el diálogo no queda mudo: enuncia los tres hechos del coste que
   * son la razón de pedir confirmación. Es el caso NORMAL desde S145 —el centro real
   * pre-valida limpio—, y hasta S144 esta plantilla lo habría servido con un título
   * que hablaba de la pre-validación, un párrafo que prometía hallazgos y una lista
   * vacía debajo.
   *
   * <p>Los TRES hechos van en el aserto por separado: cada uno es una razón distinta
   * para no pulsar sin pensar —tiempo, pérdida de trabajo, irreversibilidad— y con
   * uno solo, "dice algo" pasaría por "los dice todos".
   */
  it('(38) con la lista vacía, el diálogo enuncia el coste de la operación', async () => {
    await montar([]);

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(texto).toContain('diez minutos');
    expect(texto).toContain('Sustituye el horario en el que estás trabajando');
    expect(texto).toContain('No se puede deshacer');
  });

  /**
   * Con lista vacía NO se pinta el bloque de hallazgos —ni el párrafo que los
   * promete ni la lista— y el botón principal deja de decir «de todos modos», que
   * sin hallazgos no se referiría a nada. Los botones siguen operativos: el gesto no
   * depende de que haya avisos.
   */
  it('(39) con la lista vacía no hay bloque de hallazgos y el botón no dice «de todos modos»', async () => {
    await montar([]);

    const raiz = fixture.nativeElement as HTMLElement;
    expect(raiz.querySelector('.advertencia')).toBeNull();
    expect(raiz.querySelectorAll('.error-entrada').length).toBe(0);

    const confirmar = raiz.querySelector('button.confirmar') as HTMLButtonElement;
    expect(confirmar.textContent?.trim()).toBe('Generar horario');

    confirmar.click();
    expect(ref.close).toHaveBeenCalledWith(true);
  });

  /**
   * Contrapunto del (39) en el montaje CON errores: ahí el bloque sí está y el botón
   * sí dice «de todos modos». Sin este par, «nunca pinta el bloque» pasaría el (39).
   */
  it('(40) con errores, el bloque de hallazgos está y el botón avisa de que se genera igual', () => {
    const raiz = fixture.nativeElement as HTMLElement;

    expect(raiz.querySelector('.advertencia')).not.toBeNull();
    expect(raiz.querySelectorAll('.error-entrada').length).toBe(2);
    expect((raiz.querySelector('button.confirmar') as HTMLButtonElement).textContent?.trim()).toBe(
      'Generar de todos modos',
    );
  });
});
