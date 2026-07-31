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
});
