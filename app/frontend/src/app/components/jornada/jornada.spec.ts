import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { Dialog } from '@angular/cdk/dialog';

import { Jornada } from './jornada';
import { JornadaDTO, TramoJornadaDTO } from '../../models/jornada.model';

/**
 * Congela el comportamiento de la pantalla de jornada. No hereda del molde de
 * `aula-lista.spec.ts` los casos de lista/alta/borrado —aquí no hay ninguna de las tres
 * cosas— y sí su instrumental: `Dialog` doblado con `vi.fn()`, `HttpTestingController`,
 * `http.verify()` en el `afterEach` como red que caza las peticiones que NO debían
 * salir. Secuencia propia desde (1).
 *
 * <p>Los casos que miden lo que este Cambio estrena:
 * <ul>
 *   <li>(1) del GET llegan 35 tramos y la rejilla pinta SOLO los 7 del primer día;
 *   <li>(3) el cuerpo del PUT es el día tipo RECORTADO: tres campos por tramo, sin
 *       `dia`/`orden`/`ordenEnDia`. Es el aserto que sostiene la asimetría del contrato;
 *   <li>(4) sobre malla guardada el PUT se confirma antes, y cancelar no llama al
 *       backend (lo comprueba el `http.verify()`);
 *   <li>(7) el 409 no es un error de formulario: aviso propio, solo lectura y Guardar
 *       deshabilitado, frente al 400 de (6) que deja la pantalla editable.
 * </ul>
 */
describe('Jornada', () => {
  let fixture: ComponentFixture<Jornada>;
  let http: HttpTestingController;
  let dialog: { open: ReturnType<typeof vi.fn> };

  /** Métodos `protected` que los casos necesitan invocar. */
  type Interna = { guardar: () => void; recargar: () => void };

  /** Las 7 filas del día tipo de referencia: 3 lectivos, recreo, 3 lectivos. */
  const DIA_TIPO: ReadonlyArray<[string, string, boolean]> = [
    ['08:00', '09:00', true],
    ['09:00', '10:00', true],
    ['10:00', '11:00', true],
    ['11:00', '11:30', false],
    ['11:30', '12:30', true],
    ['12:30', '13:30', true],
    ['13:30', '14:30', true],
  ];

  /** La SEMANA como la devuelve el backend: 35 tramos, orden global continuo 1..35. */
  function malla(persistida: boolean): JornadaDTO {
    const dias = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES'];
    const tramos: TramoJornadaDTO[] = [];
    let orden = 1;
    for (const dia of dias) {
      let ordenEnDia = 0;
      for (const [horaInicio, horaFin, esLectivo] of DIA_TIPO) {
        tramos.push({
          dia,
          horaInicio,
          horaFin,
          esLectivo,
          orden: orden++,
          ordenEnDia: esLectivo ? ++ordenEnDia : null,
        });
      }
    }
    return { persistida, tramos };
  }

  beforeEach(() => {
    dialog = { open: vi.fn() };
    TestBed.configureTestingModule({
      imports: [Jornada],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Dialog, useValue: dialog },
      ],
    });
    fixture = TestBed.createComponent(Jornada);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  /** Dispara `ngOnInit` y responde el GET con la malla dada. */
  function flushJornada(dto: JornadaDTO): void {
    fixture.detectChanges();
    http.expectOne('/api/jornada').flush(dto);
  }

  /** Hace que el próximo diálogo abierto cierre con el valor dado. */
  function dialogoDevuelve(valor: boolean | undefined): void {
    dialog.open.mockReturnValue({
      closed: { subscribe: (fn: (v: boolean | undefined) => void) => fn(valor) },
    });
  }

  function filas(): HTMLElement[] {
    return [...fixture.nativeElement.querySelectorAll('tbody tr')];
  }

  function inputs(): HTMLInputElement[] {
    return [...fixture.nativeElement.querySelectorAll('.jornada__input')];
  }

  function botonGuardar(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('.jornada__guardar');
  }

  /** Escribe en un input de hora y propaga el evento al control reactivo. */
  function escribir(indice: number, valor: string): void {
    const input = inputs()[indice];
    input.value = valor;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function instancia(): Interna {
    return fixture.componentInstance as unknown as Interna;
  }

  it('(1) carga en init y pinta SOLO el primer día, no los 35 tramos', async () => {
    flushJornada(malla(true));
    await fixture.whenStable();

    // Discriminante: si el componente volcara los 35 tramos del GET saldrían 35 filas.
    expect(filas().length).toBe(7);
    const horasInicio = inputs()
      .filter((_, i) => i % 2 === 0)
      .map((i) => i.value);
    expect(horasInicio).toEqual([
      '08:00',
      '09:00',
      '10:00',
      '11:00',
      '11:30',
      '12:30',
      '13:30',
    ]);
    // La fila de recreo se distingue visualmente y lleva su toggle marcado.
    expect(filas()[3].classList).toContain('jornada__fila--recreo');
    expect(filas()[0].classList).not.toContain('jornada__fila--recreo');
  });

  it('(2) con persistida=false muestra el badge de propuesta', async () => {
    flushJornada(malla(false));
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.jornada__badge')).toBeTruthy();
    expect(fixture.nativeElement.textContent).toContain('Propuesta · sin guardar');
  });

  it('(2b) con persistida=true no hay badge', async () => {
    flushJornada(malla(true));
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.jornada__badge')).toBeNull();
  });

  it('(3) guardar una propuesta manda el día tipo RECORTADO, sin confirmación', async () => {
    flushJornada(malla(false));
    await fixture.whenStable();

    escribir(0, '08:15'); // primera horaInicio
    instancia().guardar();

    const req = http.expectOne('/api/jornada');
    expect(req.request.method).toBe('PUT');

    const cuerpo = req.request.body as { tramos: Record<string, unknown>[] };
    // Siete tramos: el DÍA TIPO, no la semana.
    expect(cuerpo.tramos.length).toBe(7);
    // Tres campos y solo tres: dia, orden y ordenEnDia NO viajan. Este aserto es el
    // que caza un `aRequest()` que reenviara lo recibido del GET.
    expect(Object.keys(cuerpo.tramos[0]).sort()).toEqual([
      'esLectivo',
      'horaFin',
      'horaInicio',
    ]);
    expect(cuerpo.tramos[0]).toEqual({
      horaInicio: '08:15',
      horaFin: '09:00',
      esLectivo: true,
    });
    // El toggle «Recreo» se invierte a esLectivo al salir.
    expect(cuerpo.tramos[3]).toEqual({
      horaInicio: '11:00',
      horaFin: '11:30',
      esLectivo: false,
    });
    // Una propuesta no destruye nada: no se confirma.
    expect(dialog.open).not.toHaveBeenCalled();

    req.flush(malla(true));
  });

  it('(4) sobre malla guardada confirma antes; cancelar no llama al backend', async () => {
    flushJornada(malla(true));
    await fixture.whenStable();

    dialogoDevuelve(undefined); // backdrop/Escape/cancelar
    instancia().guardar();

    expect(dialog.open).toHaveBeenCalled();
    // No se asevera "no hubo PUT" con un expectNone: el http.verify() del afterEach
    // pone rojo cualquier petición no consumida, que es la misma red y no depende de
    // que este caso recuerde comprobarlo.
  });

  it('(4b) sobre malla guardada, confirmar sí lanza el PUT', async () => {
    flushJornada(malla(true));
    await fixture.whenStable();

    dialogoDevuelve(true);
    instancia().guardar();

    const req = http.expectOne('/api/jornada');
    expect(req.request.method).toBe('PUT');
    req.flush(malla(true));
  });

  it('(5) una hora mal formada no llega al backend y marca los campos', async () => {
    flushJornada(malla(false));
    await fixture.whenStable();

    escribir(0, '8am');
    instancia().guardar();

    // Sin expectOne: si saliera un PUT, el http.verify() del afterEach lo cazaría.
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Usa el formato HH:mm.');
  });

  it('(6) un 400 muestra el mensaje inline y deja la pantalla editable', async () => {
    flushJornada(malla(false));
    await fixture.whenStable();

    instancia().guardar();
    http.expectOne('/api/jornada').flush(
      { message: 'el maximo es 6 tramos lectivos por dia; el dia tipo tiene 7' },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();

    const texto = fixture.nativeElement.querySelector('.jornada__error').textContent;
    expect(texto).toContain('el maximo es 6 tramos lectivos por dia');
    // Discriminante frente a (7): el 400 NO pone la pantalla en solo lectura.
    expect(fixture.nativeElement.querySelector('.jornada__conflicto')).toBeNull();
    expect(botonGuardar().disabled).toBe(false);
    expect(inputs()[0].disabled).toBe(false);
  });

  it('(7) un 409 avisa con el desglose, pasa a solo lectura y apaga Guardar', async () => {
    flushJornada(malla(false));
    await fixture.whenStable();

    instancia().guardar();
    http.expectOne('/api/jornada').flush(
      {
        message:
          'No se puede borrar: referenciada por 3 sesiones de horario, 2 restricciones horarias',
      },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();

    const aviso = fixture.nativeElement.querySelector('.jornada__conflicto');
    expect(aviso).toBeTruthy();
    // Nombra QUÉ y CUÁNTO (lo compone el backend) y añade QUÉ HACER.
    expect(aviso.textContent).toContain('3 sesiones de horario');
    expect(aviso.textContent).toContain('2 restricciones horarias');
    expect(aviso.textContent).toContain('antes de reconfigurar la jornada');
    // Solo lectura: ni el botón ni los campos aceptan nada.
    expect(botonGuardar().disabled).toBe(true);
    expect(inputs()[0].disabled).toBe(true);
    // Y no cae al camino del 400.
    expect(fixture.nativeElement.querySelector('.jornada__error')).toBeNull();
  });

  it('(8) tras un 409, recargar y guardar con éxito sale de solo lectura', async () => {
    flushJornada(malla(false));
    await fixture.whenStable();

    instancia().guardar();
    http.expectOne('/api/jornada').flush({}, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();
    expect(botonGuardar().disabled).toBe(true);

    // Única salida del modo solo lectura: releer del servidor.
    instancia().recargar();
    http.expectOne('/api/jornada').flush(malla(false));
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.jornada__conflicto')).toBeNull();
    expect(inputs()[0].disabled).toBe(false);

    // Y ahora el guardado va bien: persistida pasa a true (desaparece el badge).
    instancia().guardar();
    http.expectOne('/api/jornada').flush(malla(true));
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('.jornada__badge')).toBeNull();
    expect(botonGuardar().disabled).toBe(false);
    expect(filas().length).toBe(7);
  });

  it('(9) un error de carga cae al degradado con status', async () => {
    fixture.detectChanges();
    http.expectOne('/api/jornada').flush('', { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    const texto = fixture.nativeElement.querySelector('.jornada__error').textContent;
    expect(texto).toContain('No se pudo cargar la jornada');
    expect(texto).toContain('500');
  });
});
