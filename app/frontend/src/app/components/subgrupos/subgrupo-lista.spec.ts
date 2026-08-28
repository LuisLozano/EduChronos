import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { Dialog } from '@angular/cdk/dialog';
import { SubgrupoLista } from './subgrupo-lista';

/**
 * Congela el comportamiento de la lista de subgrupos. Hereda del molde de grupo-lista
 * los casos de render, vacío, error de carga y las dos caras del 409, y del molde de
 * aula-lista el caso de recarga tras guardado (7) y el de con-qué-se-abre el diálogo
 * (8). Añade (6), que fija las dos columnas: Código y Grupos, esta última pintada como
 * `grupos.join(', ')`. Secuencia propia desde (1).
 *
 * <p>Los datos discriminan: el subgrupo de dos grupos ({1ºA, 1ºC}) mide que la celda
 * "Grupos" une TODA la población, no solo el primero; y el código del subgrupo NO
 * contiene a sus grupos, para que un aserto no confunda columna con columna.
 */
describe('SubgrupoLista', () => {
  let fixture: ComponentFixture<SubgrupoLista>;
  let http: HttpTestingController;
  let dialog: { open: ReturnType<typeof vi.fn> };

  interface Interna {
    nuevo: () => void;
    editar: (s: unknown) => void;
    borrar: (s: unknown) => void;
  }

  const FILAS = [
    { id: 5, codigo: 'CyR-Tec', grupos: ['1ºA'] },
    { id: 6, codigo: 'Agrup-AC', grupos: ['1ºA', '1ºC'] },
  ];

  beforeEach(() => {
    dialog = { open: vi.fn() };
    TestBed.configureTestingModule({
      imports: [SubgrupoLista],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Dialog, useValue: dialog },
      ],
    });
    fixture = TestBed.createComponent(SubgrupoLista);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function flushLista(filas: unknown[] = []): void {
    fixture.detectChanges(); // dispara ngOnInit → cargar()
    http.expectOne('/api/subgrupos').flush(filas);
  }

  /** Hace que el próximo diálogo abierto cierre con el valor dado. */
  function dialogoDevuelve(valor: boolean | undefined): void {
    dialog.open.mockReturnValue({
      closed: { subscribe: (fn: (v: boolean | undefined) => void) => fn(valor) },
    });
  }

  /** Dispara el borrado de una fila con el diálogo de confirmación aceptado. */
  function borrar(subgrupo: unknown): void {
    dialogoDevuelve(true);
    (fixture.componentInstance as unknown as Interna).borrar(subgrupo);
  }

  it('(1) carga la lista en init y la pinta', async () => {
    flushLista(FILAS);
    await fixture.whenStable();
    const filas = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(filas.length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('CyR-Tec');
    expect(fixture.nativeElement.textContent).toContain('Agrup-AC');
  });

  it('(2) lista vacía muestra la invitación a crear el primero', async () => {
    flushLista([]);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.subgrupos__vacio')).toBeTruthy();
  });

  it('(3) error de carga cae al degradado con status', async () => {
    fixture.detectChanges();
    http.expectOne('/api/subgrupos').flush('', { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.subgrupos__error').textContent;
    expect(err).toContain('No se pudo cargar');
    expect(err).toContain('500');
  });

  it('(4) al borrar, un 409 CON message muestra el texto RICO del backend, no el degradado', async () => {
    flushLista(FILAS);
    await fixture.whenStable();
    borrar(FILAS[0]);

    http.expectOne('/api/subgrupos/5').flush(
      { message: 'No se puede borrar: referenciada por 3 plaza(s)' },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.subgrupos__error').textContent;
    expect(err).toContain('referenciada por 3 plaza(s)');
    expect(err).not.toContain('No se pudo borrar el subgrupo CyR-Tec (409)');
  });

  it('(5) al borrar, un 409 SIN message cae al degradado que dice qué pasó', async () => {
    flushLista(FILAS);
    await fixture.whenStable();
    borrar(FILAS[0]);

    http.expectOne('/api/subgrupos/5').flush({}, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.subgrupos__error').textContent;
    expect(err).toContain('No se pudo borrar el subgrupo CyR-Tec (409)');
  });

  it('(6) la tabla pinta Código y la columna Grupos como lista unida por comas', async () => {
    flushLista(FILAS);
    await fixture.whenStable();
    const raiz = fixture.nativeElement as HTMLElement;

    const cabeceras = [...raiz.querySelectorAll('thead th')].map((th) => th.textContent!.trim());
    expect(cabeceras).toEqual(['Código', 'Grupos', '']);

    // Segunda fila: población de DOS grupos. Igualdad estricta de la celda para medir
    // que une toda la población ('1ºA, 1ºC'), no solo el primer código.
    const celdas = raiz.querySelectorAll('tbody tr:nth-child(2) td');
    expect(celdas[0].textContent!.trim()).toBe('Agrup-AC');
    expect(celdas[1].textContent!.trim()).toBe('1ºA, 1ºC');
  });

  it('(7) un guardado en el diálogo recarga la lista; un cierre sin guardar no', async () => {
    flushLista([]);
    await fixture.whenStable();
    const inst = fixture.componentInstance as unknown as Interna;

    dialogoDevuelve(true);
    inst.nuevo();
    http.expectOne('/api/subgrupos').flush([{ id: 5, codigo: 'CyR-Tec', grupos: ['1ºA'] }]); // recargó
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('CyR-Tec');

    // backdrop/Escape emiten undefined: no debe salir un segundo GET. El http.verify()
    // del afterEach lo pondría rojo si saliera.
    dialogoDevuelve(undefined);
    inst.nuevo();
  });

  it('(8) editar pasa el subgrupo al diálogo; nuevo pasa null', async () => {
    flushLista([FILAS[1]]);
    await fixture.whenStable();
    const inst = fixture.componentInstance as unknown as Interna;
    dialogoDevuelve(undefined); // se cancela: aquí solo interesa CON QUÉ se abrió

    inst.editar(FILAS[1]);
    expect(dialog.open.mock.calls.at(-1)?.[1]).toEqual({ data: FILAS[1] });

    inst.nuevo();
    expect(dialog.open.mock.calls.at(-1)?.[1]).toEqual({ data: null });
  });

  /**
   * La cabecera compartida está MONTADA y con SUS rótulos (S124). Los dos asertos
   * son distintos a propósito: el del `<h2>` caza un `titulo` mal cableado, y el
   * del botón caza el género —«Nuevo subgrupo» no se deriva de «Subgrupos»— que es lo
   * que el e2e localiza por texto exacto. Un `<app-cabecera-lista>` presente pero
   * sin inputs pasaría un `querySelector` a secas y falla estos dos.
   */
  it('(9) monta la cabecera compartida con su rótulo y su texto de alta', async () => {
    flushLista([]);
    await fixture.whenStable();
    const cabecera = fixture.nativeElement.querySelector('app-cabecera-lista');
    expect(cabecera).toBeTruthy();
    expect(cabecera.querySelector('.cabecera-lista__titulo').textContent).toContain('Subgrupos');
    expect(cabecera.querySelector('.cabecera-lista__nuevo').textContent.trim()).toBe('Nuevo subgrupo');
  });

  /** Teclea en la caja de la cabecera como una persona, y deja repintar. */
  async function buscar(texto: string): Promise<void> {
    const caja = fixture.nativeElement.querySelector('.cabecera-lista__busqueda');
    caja.value = texto;
    caja.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  /**
   * La búsqueda recorta la tabla y el contador lo dice (busca por el código del subgrupo).
   * Los tres asertos van juntos a propósito: el conteo de filas mide el `@for`
   * sobre `visibles()`, el texto que SOBREVIVE y el que DESAPARECE miden que
   * recorta la correcta —no «una cualquiera»—, y el contador ata el cableado de
   * la cabecera, que recibe `coincidencias` por separado de `total`.
   */
  it('(10) al buscar se ve solo la fila que casa, y el contador dice «1 de 2»', async () => {
    flushLista(FILAS);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(2);

    await buscar('agrup');

    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('Agrup-AC');
    expect(fixture.nativeElement.textContent).not.toContain('CyR-Tec');
    expect(
      fixture.nativeElement.querySelector('.cabecera-lista__contador').textContent.trim(),
    ).toBe('1 de 2');
  });

  /**
   * El SEGUNDO estado vacío, que no es el de catálogo sin filas. Se asevera que
   * aparece el mensaje nuevo CON el texto tecleado, que NO aparece el de
   * «no hay … todavía» —son dos mensajes distintos y confundirlos es el error
   * fácil— y que la tabla se va entera.
   */
  it('(11) una búsqueda sin resultados da su propio mensaje, no el de catálogo vacío', async () => {
    flushLista(FILAS);
    await fixture.whenStable();

    await buscar('zzz');

    const sinResultados = fixture.nativeElement.querySelector('.subgrupos__sin-resultados');
    expect(sinResultados).toBeTruthy();
    expect(sinResultados.textContent).toContain('zzz');
    expect(fixture.nativeElement.querySelector('.subgrupos__vacio')).toBeNull();
    expect(fixture.nativeElement.querySelector('tbody tr')).toBeNull();
  });
});
