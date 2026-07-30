import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { Dialog } from '@angular/cdk/dialog';
import { GrupoLista } from './grupo-lista';

/**
 * Congela el comportamiento de la lista de grupos. Hereda del molde los cinco casos
 * (1)-(5) —render, vacío, error de carga y las dos caras del 409— y añade (6), que
 * fija las DOS columnas de esta tabla: Nivel se pinta, `tipo` se omite a propósito
 * (ver javadoc de `GrupoLista`). El borrado se dispara con el diálogo espiado a
 * `true`, sin overlay real. Secuencia propia desde (1). Fuera de alcance por decisión,
 * no por imposibilidad: estilos, y el orden interno de `mensaje()` (cubierto en el
 * form, misma función).
 *
 * <p>Los datos de prueba están elegidos para que los asertos DISCRIMINEN: el código
 * de un grupo ('1ESOA') tiene como PREFIJO el código de su nivel ('1ESO'), que es
 * justo la confusión posible al pintar la fila. Un aserto laxo no distinguiría una
 * columna de la otra.
 */
describe('GrupoLista', () => {
  let fixture: ComponentFixture<GrupoLista>;
  let http: HttpTestingController;
  let dialog: { open: ReturnType<typeof vi.fn> };

  const FILAS = [
    { id: 7, codigo: '1ESOA', nivel: '1ESO', tipo: 'ORDINARIO' },
    { id: 8, codigo: '2ESOB', nivel: '2ESO', tipo: 'ORDINARIO' },
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

  it("(6) la tabla pinta la columna Nivel y OMITE la columna tipo", async () => {
    flushLista(FILAS);
    await fixture.whenStable();
    const raiz = fixture.nativeElement as HTMLElement;

    const cabeceras = [...raiz.querySelectorAll('thead th')].map((th) => th.textContent!.trim());
    expect(cabeceras).toEqual(['Código', 'Nivel', '']);

    // Igualdad ESTRICTA de la celda, no `toContain`: '1ESO' está contenido en '1ESOA',
    // así que un `toContain('1ESO')` quedaría verde si la celda pintara el código.
    const celdas = raiz.querySelectorAll('tbody tr:first-child td');
    expect(celdas[0].textContent!.trim()).toBe('1ESOA');
    expect(celdas[1].textContent!.trim()).toBe('1ESO');

    // La omisión de `tipo` es una decisión, así que se vigila: si alguien añadiera la
    // columna, el valor constante aparecería en el DOM y este aserto lo cazaría.
    expect(raiz.textContent).not.toContain('ORDINARIO');
  });
});
