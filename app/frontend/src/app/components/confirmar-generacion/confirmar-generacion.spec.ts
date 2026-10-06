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
  async function montar(
    errores: AvisoPrevalidacion[],
    avisos: AvisoPrevalidacion[] = [],
  ): Promise<void> {
    TestBed.resetTestingModule();
    ref = { close: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [ConfirmarGeneracion],
      providers: [
        { provide: DialogRef, useValue: ref },
        // S203: `data` lleva errores y avisos ya repartidos (DatosConfirmarGeneracion).
        { provide: DIALOG_DATA, useValue: { errores, avisos } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmarGeneracion);
    await fixture.whenStable();
  }

  beforeEach(async () => {
    await montar([ERROR_A, ERROR_B]);
  });

  /**
   * El botón principal, sin tocar la elección, cierra con los 10 minutos por defecto
   * (S184; hasta entonces cerraba con `true`). Es el caso más común: quien no mira el
   * tiempo genera con el valor de siempre.
   */
  it('(35) confirmar sin tocar la elección cierra la ref con 10', () => {
    const boton = (fixture.nativeElement as HTMLElement).querySelector(
      'button.confirmar',
    ) as HTMLButtonElement;

    boton.click();

    expect(ref.close).toHaveBeenCalledTimes(1);
    expect(ref.close).toHaveBeenCalledWith(10);
  });

  /**
   * El botón de cancelar cierra SIN valor (S184; hasta entonces con `false`): lo mismo
   * que backdrop y Escape, así que el contenedor sólo tiene que distinguir número de no
   * número. Se asevera la llamada vacía, no sólo que no sea un número.
   */
  it('(36) cancelar cierra la ref sin valor', () => {
    const boton = (fixture.nativeElement as HTMLElement).querySelector(
      'button.cancelar',
    ) as HTMLButtonElement;

    boton.click();

    expect(ref.close).toHaveBeenCalledTimes(1);
    expect(ref.close.mock.calls[0]).toEqual([]);
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
   * uno solo, "dice algo" pasaría por "los dice todos". Desde S184 el tiempo es el de
   * la opción marcada, que al abrir es la de 10: se asevera la frase entera.
   */
  it('(38) con la lista vacía, el diálogo enuncia el coste de la operación', async () => {
    await montar([]);

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(texto).toContain('Tarda hasta 10 minutos.');
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
    expect(ref.close).toHaveBeenCalledWith(10);
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

  /**
   * S166 · la advertencia ya no promete que el servidor callará la causa: desde la
   * condición 6 el 422 de la pre-validación la trae y la vista la enseña. Se fija el
   * texto entero para que la afirmación falsa no pueda volver ni quedarse a medias.
   */
  it('(41) la advertencia anuncia el rechazo sin decir que el servidor callará la causa', () => {
    const advertencia = (fixture.nativeElement as HTMLElement).querySelector('.advertencia');

    expect(advertencia!.textContent?.replace(/\s+/g, ' ').trim()).toBe(
      'Además, el servidor rechazará esta generación. Estos hallazgos de severidad ERROR lo impedirán:',
    );
  });

  /**
   * S184 · las cuatro opciones EXACTAS, en su orden, con la de 10 marcada y sólo ella, y
   * la ayuda debajo. Con una sola opción comprobada, «ofrece 10» pasaría por «ofrece las
   * cuatro»; con sólo el `checked` de la primera, dos marcadas a la vez pasarían.
   */
  it('(42) ofrece 10, 20, 30 y 60 minutos, con 10 marcado al abrir', () => {
    const raiz = fixture.nativeElement as HTMLElement;
    const opciones = Array.from(raiz.querySelectorAll('.tiempo__opcion'));

    expect(raiz.querySelector('.tiempo legend')?.textContent?.trim()).toBe('Tiempo de cálculo');
    expect(opciones.map((o) => o.textContent?.trim())).toEqual([
      '10 minutos',
      '20 minutos',
      '30 minutos',
      '60 minutos',
    ]);
    expect(radios().map((r) => r.checked)).toEqual([true, false, false, false]);
    expect(radios().every((r) => r.name === 'minutos')).toBe(true);
    expect(raiz.querySelector('.tiempo__ayuda')?.textContent?.trim()).toBe(
      'Con más tiempo es más probable que salga horario, y suele salir mejor.',
    );
  });

  /**
   * S184 · elegir 30 cambia la consecuencia a «30 minutos» y confirmar cierra con 30.
   * Se elige por el DOM (click en el radio), como se pulsan los botones: así se cubre
   * también el cableado plantilla→señal.
   */
  it('(43) al elegir 30, el texto dice 30 minutos y confirmar cierra con 30', async () => {
    const raiz = fixture.nativeElement as HTMLElement;

    radios()[2].click();
    await fixture.whenStable();

    expect(raiz.querySelector('.consecuencias')?.textContent).toContain('Tarda hasta 30 minutos.');
    expect(raiz.querySelector('.consecuencias')?.textContent).not.toContain('10 minutos');

    (raiz.querySelector('button.confirmar') as HTMLButtonElement).click();

    expect(ref.close).toHaveBeenCalledTimes(1);
    expect(ref.close).toHaveBeenCalledWith(30);
  });

  /** Los radios del tiempo, en orden de pintado. */
  function radios(): HTMLInputElement[] {
    return Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLInputElement>(
        '.tiempo input[type="radio"]',
      ),
    );
  }

  // ─────────────────────────── S203 T3b: los AVISO llegan al diálogo

  const AVISO_CUADRE: AvisoPrevalidacion = {
    severidad: 'AVISO',
    regla: 'PROFESOR_HORAS_DESCUADRADAS',
    entidadCodigo: 'LEN1',
    demanda: 0,
    disponible: 5,
    descripcion: 'LEN1: 0 horas de clase configuradas y 5 declaradas.',
  };

  const raiz = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const rotulo = (): string => raiz().querySelector('.confirmar')!.textContent!.trim();

  it('(44) con avisos y sin errores: el bloque de avisos se ve y el botón no dice «de todos modos»', async () => {
    await montar([], [AVISO_CUADRE]);

    expect(raiz().querySelector('.avisos__titulo')!.textContent!.trim()).toBe(
      'Avisos (no impiden generar)',
    );
    const entrada = raiz().querySelector('.avisos .aviso-entrada')!;
    expect(entrada.querySelector('.entidad')!.textContent!.trim()).toBe('LEN1');
    expect(entrada.querySelector('.descripcion')!.textContent!.trim()).toBe(
      AVISO_CUADRE.descripcion,
    );
    expect(raiz().querySelector('.errores')).toBeNull();
    expect(rotulo()).toBe('Generar horario');
  });

  it('(45) con errores y avisos: dos bloques separados y el botón según los errores', async () => {
    await montar([ERROR_A], [AVISO_CUADRE]);

    const errores = raiz().querySelector('.errores')!.textContent!;
    const avisos = raiz().querySelector('.avisos')!.textContent!;
    expect(errores).toContain(ERROR_A.descripcion);
    expect(errores).not.toContain(AVISO_CUADRE.descripcion);
    expect(avisos).toContain(AVISO_CUADRE.descripcion);
    expect(avisos).not.toContain(ERROR_A.descripcion);
    expect(rotulo()).toBe('Generar de todos modos');
  });

  it('(46) sin avisos no hay bloque de avisos', async () => {
    await montar([ERROR_A], []);

    expect(raiz().querySelector('.avisos')).toBeNull();
    expect(raiz().querySelector('.avisos__titulo')).toBeNull();
    expect(raiz().textContent).not.toContain('no impiden generar');
  });
});
