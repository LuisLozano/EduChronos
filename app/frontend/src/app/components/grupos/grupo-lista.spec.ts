import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { Dialog } from '@angular/cdk/dialog';
import { GrupoLista } from './grupo-lista';
import { TutoriaDialogo } from './tutoria-dialogo';

/**
 * Congela el comportamiento de la lista de grupos. Hereda del molde los cinco casos
 * (1)-(5) —render, vacío, error de carga y las dos caras del 409—, fija en (6) las TRES
 * columnas de esta tabla, cubre en (7)-(12) el cableado del diálogo del PDC —qué
 * acciones ve cada fila según su `tipo`, con qué dato se abre el diálogo y cuándo
 * recarga la lista al cerrarse— y en (13)-(15) el del diálogo de TUTORÍA. Los diálogos
 * se disparan con `Dialog` espiado, sin overlay real. Secuencia propia desde (1). Fuera
 * de alcance por decisión, no por imposibilidad: estilos, y el orden interno de
 * `mensaje()` (cubierto en el form, misma función).
 *
 * <p><b>Los dos cableados no comparten regla, y (7)/(8) lo fijan.</b> Las acciones de
 * ordinario van dentro de un `@if` por tipo; «Tutoría» va fuera y se pinta en TODAS las
 * filas, porque un PDC hereda el tutor de su padre y tiene que poder editarlo. Por eso
 * los dos casos afirman la lista COMPLETA de botones de la fila y no la presencia
 * suelta de uno: es lo único que distingue «Tutoría se pinta siempre» de «el `@if` se
 * ha caído y ahora todo se pinta siempre».
 *
 * <p>Los datos de prueba están elegidos para que los asertos DISCRIMINEN: el código
 * de un grupo ('1ESOA') tiene como PREFIJO el código de su nivel ('1ESO'), que es
 * justo la confusión posible al pintar la fila. Un aserto laxo no distinguiría una
 * columna de la otra.
 *
 * <p>Por la misma razón los casos del PDC operan sobre la SEGUNDA fila y pulsando el
 * botón REAL del DOM, no llamando al método del componente: lo que puede romperse aquí
 * es que la plantilla pase la fila equivocada al `(click)`, y eso solo se mide clicando
 * una fila que no sea la primera.
 */
describe('GrupoLista', () => {
  let fixture: ComponentFixture<GrupoLista>;
  let http: HttpTestingController;
  let dialog: { open: ReturnType<typeof vi.fn> };

  const FILAS = [
    { id: 7, codigo: '1ESOA', nivel: '1ESO', tipo: 'ORDINARIO' },
    { id: 8, codigo: '2ESOB', nivel: '2ESO', tipo: 'ORDINARIO' },
  ];

  /** Un ordinario y su PDC, que es lo que devuelve hoy `GET /api/grupos` sin filtrar. */
  const FILAS_MIXTAS = [
    { id: 7, codigo: '1ESOA', nivel: '1ESO', tipo: 'ORDINARIO' },
    { id: 9, codigo: '1ESOADI', nivel: '1ESO', tipo: 'DIVERSIFICACION_PDC' },
  ];

  beforeEach(() => {
    dialog = { open: vi.fn() };
    TestBed.configureTestingModule({
      imports: [GrupoLista],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Dialog, useValue: dialog },
      ],
    });
    fixture = TestBed.createComponent(GrupoLista);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function flushLista(filas: unknown[] = []): void {
    fixture.detectChanges(); // dispara ngOnInit → cargar()
    http.expectOne('/api/grupos').flush(filas);
  }

  /** Dispara el borrado de una fila con el diálogo de confirmación aceptado. */
  function borrar(grupo: unknown): void {
    dialog.open.mockReturnValue({ closed: { subscribe: (fn: (v: boolean) => void) => fn(true) } });
    (fixture.componentInstance as unknown as { borrar: (g: unknown) => void }).borrar(grupo);
  }

  /** Los botones de acción de una fila (0-indexada), tal como los pinta la plantilla. */
  function acciones(fila: number): HTMLButtonElement[] {
    const raiz = fixture.nativeElement as HTMLElement;
    return [
      ...raiz.querySelectorAll<HTMLButtonElement>(
        `tbody tr:nth-child(${fila + 1}) .grupos__acciones button`),
    ];
  }

  /**
   * Pulsa el botón «PDC» de una fila con el diálogo espiado para cerrarse con el valor
   * dado. Va por el DOM y no por el método: mide también que el `(click)` de esa fila
   * pase SU grupo.
   */
  function pulsarPdc(fila: number, cierraCon: boolean | undefined): void {
    dialog.open.mockReturnValue({
      closed: { subscribe: (fn: (v: boolean | undefined) => void) => fn(cierraCon) },
    });
    const boton = acciones(fila).find((b) => b.textContent!.trim() === 'PDC')!;
    boton.click();
  }

  /**
   * Pulsa el botón «Tutoría» de una fila. El diálogo espiado se cierra con el valor dado
   * —`true` por defecto, que es lo que emite tras escribir—: el componente NO se suscribe
   * a `closed`, y devolver el caso más comprometido es lo que permite a (15) medir que
   * aun así no recarga.
   */
  function pulsarTutoria(fila: number, cierraCon: boolean | undefined = true): void {
    dialog.open.mockReturnValue({
      closed: { subscribe: (fn: (v: boolean | undefined) => void) => fn(cierraCon) },
    });
    const boton = acciones(fila).find((b) => b.textContent!.trim() === 'Tutoría')!;
    boton.click();
  }

  it('(1) carga la lista en init y la pinta', async () => {
    flushLista(FILAS);
    await fixture.whenStable();
    const filas = fixture.nativeElement.querySelectorAll('tbody tr');
    // DOS filas, no una: con una sola, un `@for` roto que pintara solo el primer
    // elemento quedaría verde. El conteo y los dos textos miden que el bucle itera.
    expect(filas.length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('1ESOA');
    expect(fixture.nativeElement.textContent).toContain('2ESOB');
  });

  it('(2) lista vacía muestra la invitación a crear el primero', async () => {
    flushLista([]);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.grupos__vacio')).toBeTruthy();
  });

  it('(3) error de carga cae al degradado con status', async () => {
    fixture.detectChanges();
    http.expectOne('/api/grupos').flush('', { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.grupos__error').textContent;
    expect(err).toContain('No se pudo cargar');
    expect(err).toContain('500');
  });

  it('(4) al borrar, un 409 CON message muestra el texto RICO del backend, no el degradado', async () => {
    flushLista(FILAS);
    await fixture.whenStable();
    borrar(FILAS[0]);

    http.expectOne('/api/grupos/7').flush(
      { message: 'No se puede borrar: referenciada por 2 subgrupo(s), 1 grupo(s) hijo(s)' },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.grupos__error').textContent;
    expect(err).toContain('referenciada por 2 subgrupo(s)');
    // discriminante: NO cae al degradado. El `(409)` no es adorno: sin él, '1ESO' es
    // prefijo de '1ESOA' y el aserto dejaría de distinguir nivel de código.
    expect(err).not.toContain('No se pudo borrar el grupo 1ESOA (409)');
  });

  it('(5) al borrar, un 409 SIN message cae al degradado que dice qué pasó', async () => {
    flushLista(FILAS);
    await fixture.whenStable();
    borrar(FILAS[0]);

    http.expectOne('/api/grupos/7').flush({}, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.grupos__error').textContent;
    // el `(409)` corta el prefijo: si el degradado nombrara el NIVEL en vez del código
    // sería '…el grupo 1ESO (409)' y este aserto caería, que es lo que debe discriminar.
    expect(err).toContain('No se pudo borrar el grupo 1ESOA (409)');
  });

  it('(6) la tabla pinta las tres columnas de datos: Código, Nivel y Tipo', async () => {
    flushLista(FILAS);
    await fixture.whenStable();
    const raiz = fixture.nativeElement as HTMLElement;

    const cabeceras = [...raiz.querySelectorAll('thead th')].map((th) => th.textContent!.trim());
    // La última, vacía, es la de acciones.
    expect(cabeceras).toEqual(['Código', 'Nivel', 'Tipo', '']);

    // Igualdad ESTRICTA de la celda, no `toContain`: '1ESO' está contenido en '1ESOA',
    // así que un `toContain('1ESO')` quedaría verde si la celda pintara el código.
    const celdas = raiz.querySelectorAll('tbody tr:first-child td');
    expect(celdas[0].textContent!.trim()).toBe('1ESOA');
    expect(celdas[1].textContent!.trim()).toBe('1ESO');
    expect(celdas[2].textContent!.trim()).toBe('Ordinario');

    // La constante CRUDA no aparece: la celda pinta la etiqueta legible. Distingue
    // mayúsculas, así que 'Ordinario' pasa y 'ORDINARIO' no.
    expect(raiz.textContent).not.toContain('ORDINARIO');
  });

  it('(7) una fila ORDINARIO ofrece las tres acciones de ordinario MÁS la de tutoría', async () => {
    flushLista(FILAS_MIXTAS);
    await fixture.whenStable();

    const textos = acciones(0).map((b) => b.textContent!.trim());
    expect(textos).toEqual(['Editar', 'Borrar', 'PDC', 'Tutoría']);
  });

  it('(8) DISCRIMINANTE: una fila DIVERSIFICACION_PDC ofrece SOLO la de tutoría', async () => {
    flushLista(FILAS_MIXTAS);
    await fixture.whenStable();

    // Igualdad de la lista COMPLETA, que mide las dos reglas a la vez: Editar, Borrar y
    // PDC acabarían aquí en un error que el usuario no puede resolver (400/409/400) y
    // siguen ocultas —si alguien quita el @if, aparecen y este aserto cae—, mientras que
    // «Tutoría» SÍ se pinta, porque este PDC pudo heredar el tutor de su padre.
    expect(acciones(1).map((b) => b.textContent!.trim())).toEqual(['Tutoría']);
    // La fila SIGUE pintándose: lo que se oculta son tres acciones, no el grupo.
    expect(fixture.nativeElement.querySelectorAll('tbody tr')).toHaveLength(2);
    expect(fixture.nativeElement.textContent).toContain('1ESOADI');
  });

  it('(9) pulsar PDC abre el diálogo con el Grupo DE ESA FILA', async () => {
    flushLista(FILAS);
    await fixture.whenStable();

    // La SEGUNDA fila: con la primera, una plantilla que pasara siempre el primer
    // elemento del `@for` quedaría verde.
    pulsarPdc(1, undefined);

    expect(dialog.open).toHaveBeenCalledTimes(1);
    // El objeto COMPLETO, no solo el id: así cae tanto pasar la fila equivocada como
    // pasar `null`, un id suelto o un objeto recortado que al diálogo le falte.
    expect(dialog.open.mock.calls[0][1]).toEqual({ data: FILAS[1] });
  });

  it('(10) si el diálogo del PDC cierra con true, la lista recarga', async () => {
    flushLista(FILAS);
    await fixture.whenStable();

    pulsarPdc(0, true);

    // La recarga es un segundo GET: el alta o el borrado de un PDC cambia la tabla.
    http.expectOne('/api/grupos').flush(FILAS_MIXTAS);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('1ESOADI');
  });

  it('(11) si el diálogo del PDC cierra sin escritura, NO recarga', async () => {
    flushLista(FILAS);
    await fixture.whenStable();

    // LOS DOS valores de salida sin escritura, y hacen falta los dos: `PdcDialogo`
    // emite `false` por su botón de cerrar y `undefined` por backdrop o Escape. Con
    // solo `undefined` este caso NO discrimina un `cambiado !== undefined` en el
    // componente —`undefined !== undefined` tampoco recarga—, y esa relajación pasaría
    // entera; medido en la campaña de mutación, no supuesto.
    pulsarPdc(0, false);
    http.expectNone('/api/grupos');

    pulsarPdc(0, undefined);
    http.expectNone('/api/grupos');

    // Dos aperturas, cero recargas: el diálogo se abrió de verdad las dos veces, así
    // que el `expectNone` mide la rama de cierre y no un botón que no hizo nada.
    expect(dialog.open).toHaveBeenCalledTimes(2);
  });

  it('(12) la columna Tipo traduce la constante a etiqueta legible', async () => {
    flushLista(FILAS_MIXTAS);
    await fixture.whenStable();
    const raiz = fixture.nativeElement as HTMLElement;

    const tipos = [...raiz.querySelectorAll('.grupos__tipo')].map((td) => td.textContent!.trim());
    // Igualdad de la lista completa: fija las DOS traducciones y su orden por fila.
    expect(tipos).toEqual(['Ordinario', 'PDC']);
    expect(raiz.textContent).not.toContain('DIVERSIFICACION_PDC');
  });

  it('(13) pulsar Tutoría abre TutoriaDialogo con el Grupo DE ESA FILA', async () => {
    flushLista(FILAS);
    await fixture.whenStable();

    // La SEGUNDA fila, mismo criterio que (9): con la primera, una plantilla que pasara
    // siempre el primer elemento del `@for` quedaría verde.
    pulsarTutoria(1);

    expect(dialog.open).toHaveBeenCalledTimes(1);
    // El COMPONENTE, no solo el data: es lo que distingue este botón del de PDC, que
    // vive en la misma celda y se abre igual. Sin este aserto, cablear «Tutoría» al
    // diálogo equivocado pasaría entero.
    expect(dialog.open.mock.calls[0][0]).toBe(TutoriaDialogo);
    // Y la entidad DIRECTA, no envuelta: `{ data: grupo }`, nunca `{ data: { grupo } }`.
    // El objeto completo por igualdad, así cae también pasar la fila equivocada.
    expect(dialog.open.mock.calls[0][1]).toEqual({ data: FILAS[1] });
  });

  it('(14) DISCRIMINANTE: en una fila PDC el botón también abre el diálogo con ESE PDC', async () => {
    flushLista(FILAS_MIXTAS);
    await fixture.whenStable();

    // El caso que justifica sacar el botón del `@if`: un PDC hereda el TUTOR_PRINCIPAL
    // de su padre al crearse, así que es justo la fila que necesita poder editarlo. Si
    // alguien mete «Tutoría» dentro del filtro por tipo, aquí no hay botón que pulsar y
    // el helper revienta al clicar sobre `undefined`.
    pulsarTutoria(1);

    expect(dialog.open).toHaveBeenCalledTimes(1);
    expect(dialog.open.mock.calls[0][0]).toBe(TutoriaDialogo);
    // El PDC, no su padre: los dos están en la tabla y comparten prefijo de código
    // ('1ESOA' es prefijo de '1ESOADI'), que es la confusión posible.
    expect(dialog.open.mock.calls[0][1]).toEqual({ data: FILAS_MIXTAS[1] });
  });

  it('(15) al cerrar el diálogo de tutoría la lista NO recarga, ni siquiera con true', async () => {
    flushLista(FILAS);
    await fixture.whenStable();

    // `true` es el caso comprometido: es lo que `TutoriaDialogo` emite tras escribir, y
    // es lo que hace recargar en los otros dos cableados de esta misma clase. Aquí no
    // debe: la tabla no pinta ningún dato de tutoría, así que el GET no cambiaría un
    // solo píxel. Quien copie el `.closed.subscribe(...)` de `pdc()` al cablear esto
    // añade una petición por cada guardado y este `expectNone` lo caza.
    pulsarTutoria(0, true);
    http.expectNone('/api/grupos');

    // El diálogo se abrió de verdad: el expectNone mide la ausencia de recarga, no un
    // botón que no hiciera nada.
    expect(dialog.open).toHaveBeenCalledTimes(1);
  });
});
