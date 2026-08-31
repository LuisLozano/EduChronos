import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { Dialog } from '@angular/cdk/dialog';
import { AsignaturaLista } from './asignatura-lista';

/**
 * Congela el comportamiento de la lista. El foco de M3 (traducción de error) son
 * los casos (4)(5): que el 409 muestra el TEXTO DEL BACKEND cuando viaja, y el
 * DEGRADADO cuando no. El borrado se dispara con el diálogo espiado a `true`,
 * sin overlay real. Secuencia propia desde (1). Fuera de alcance por decisión,
 * no por imposibilidad: estilos, y el orden interno de `mensaje()` (cubierto en
 * el form, misma función).
 */
describe('AsignaturaLista', () => {
  let fixture: ComponentFixture<AsignaturaLista>;
  let http: HttpTestingController;
  let dialog: { open: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    dialog = { open: vi.fn() };
    TestBed.configureTestingModule({
      imports: [AsignaturaLista],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Dialog, useValue: dialog },
      ],
    });
    fixture = TestBed.createComponent(AsignaturaLista);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function flushLista(filas: unknown[] = []): void {
    fixture.detectChanges(); // dispara ngOnInit → cargar()
    http.expectOne('/api/asignaturas').flush(filas);
  }

  it('(1) carga la lista en init y la pinta', async () => {
    flushLista([
      { id: 7, codigo: 'Mat', nombreCompleto: 'Matemáticas' },
      { id: 8, codigo: 'LCL', nombreCompleto: 'Lengua Castellana y Literatura' },
    ]);
    await fixture.whenStable();
    const filas = fixture.nativeElement.querySelectorAll('tbody tr');
    // DOS filas, no una: con una sola, un `@for` roto que pintara solo el primer
    // elemento quedaría verde. El conteo y los dos textos miden que el bucle itera.
    expect(filas.length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('Matemáticas');
    expect(fixture.nativeElement.textContent).toContain('Lengua Castellana y Literatura');
  });

  it('(2) lista vacía muestra la invitación a crear la primera', async () => {
    flushLista([]);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.estado-lista__vacio')).toBeTruthy();
  });

  it('(3) error de carga cae al degradado con status', async () => {
    fixture.detectChanges();
    http.expectOne('/api/asignaturas').flush('', { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.estado-lista__error').textContent;
    expect(err).toContain('No se pudo cargar');
    expect(err).toContain('500');
  });

  it('(4) al borrar, un 409 CON message muestra el texto RICO del backend, no el degradado', async () => {
    flushLista([{ id: 7, codigo: 'Mat', nombreCompleto: 'Matemáticas' }]);
    await fixture.whenStable();
    // el diálogo de confirmación devuelve true
    dialog.open.mockReturnValue({ closed: { subscribe: (fn: (v: boolean) => void) => fn(true) } });

    (fixture.componentInstance as unknown as { borrar: (asig: unknown) => void }).borrar({
      id: 7, codigo: 'Mat', nombreCompleto: 'Matemáticas',
    });

    http.expectOne('/api/asignaturas/7').flush(
      { message: 'No se puede borrar: referenciada por 2 actividad(es), 1 plaza(s)' },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.estado-lista__error').textContent;
    expect(err).toContain('referenciada por 2 actividad(es)');
    // discriminante: NO cae al degradado. El `(409)` no es adorno: sin él, 'Mat' es
    // prefijo de 'Matemáticas' y el aserto dejaría de distinguir codigo de nombre.
    expect(err).not.toContain('No se pudo borrar la asignatura Mat (409)');
  });

  it('(5) al borrar, un 409 SIN message cae al degradado que dice qué pasó', async () => {
    flushLista([{ id: 7, codigo: 'Mat', nombreCompleto: 'Matemáticas' }]);
    await fixture.whenStable();
    dialog.open.mockReturnValue({ closed: { subscribe: (fn: (v: boolean) => void) => fn(true) } });

    (fixture.componentInstance as unknown as { borrar: (asig: unknown) => void }).borrar({
      id: 7, codigo: 'Mat', nombreCompleto: 'Matemáticas',
    });

    http.expectOne('/api/asignaturas/7').flush({}, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();
    const err = fixture.nativeElement.querySelector('.estado-lista__error').textContent;
    // el `(409)` corta el prefijo: con `nombreCompleto` sería '…Matemáticas (409)'
    // y este aserto caería, que es justo lo que debe discriminar.
    expect(err).toContain('No se pudo borrar la asignatura Mat (409)');
  });

  /**
   * La cabecera compartida está MONTADA y con SUS rótulos (S124). Los dos asertos
   * son distintos a propósito: el del `<h2>` caza un `titulo` mal cableado, y el
   * del botón caza el género —«Nueva asignatura» no se deriva de «Asignaturas»— que es lo
   * que el e2e localiza por texto exacto. Un `<app-cabecera-lista>` presente pero
   * sin inputs pasaría un `querySelector` a secas y falla estos dos.
   */
  it('(6) monta la cabecera compartida con su rótulo y su texto de alta', async () => {
    flushLista([]);
    await fixture.whenStable();
    const cabecera = fixture.nativeElement.querySelector('app-cabecera-lista');
    expect(cabecera).toBeTruthy();
    expect(cabecera.querySelector('.cabecera-lista__titulo').textContent).toContain('Asignaturas');
    expect(cabecera.querySelector('.cabecera-lista__nuevo').textContent.trim()).toBe('Nueva asignatura');
  });

  /** Teclea en la caja de la cabecera como una persona, y deja repintar. */
  async function buscar(texto: string): Promise<void> {
    const caja = fixture.nativeElement.querySelector('.cabecera-lista__busqueda');
    caja.value = texto;
    caja.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  /**
   * La búsqueda recorta la tabla y el contador lo dice (busca por el nombre completo, no solo por el código).
   * Los tres asertos van juntos a propósito: el conteo de filas mide el `@for`
   * sobre `visibles()`, el texto que SOBREVIVE y el que DESAPARECE miden que
   * recorta la correcta —no «una cualquiera»—, y el contador ata el cableado de
   * la cabecera, que recibe `coincidencias` por separado de `total`.
   */
  it('(7) al buscar se ve solo la fila que casa, y el contador dice «1 de 2»', async () => {
    flushLista([{ id: 7, codigo: 'Mat', nombreCompleto: 'Matemáticas' },
      { id: 8, codigo: 'LCL', nombreCompleto: 'Lengua Castellana y Literatura' }]);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(2);

    await buscar('lengua');

    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('LCL');
    expect(fixture.nativeElement.textContent).not.toContain('Mat');
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
  it('(8) una búsqueda sin resultados da su propio mensaje, no el de catálogo vacío', async () => {
    flushLista([{ id: 7, codigo: 'Mat', nombreCompleto: 'Matemáticas' },
      { id: 8, codigo: 'LCL', nombreCompleto: 'Lengua Castellana y Literatura' }]);
    await fixture.whenStable();

    await buscar('zzz');

    const sinResultados = fixture.nativeElement.querySelector('.estado-lista__sin-resultados');
    expect(sinResultados).toBeTruthy();
    expect(sinResultados.textContent).toContain('zzz');
    expect(fixture.nativeElement.querySelector('.estado-lista__vacio')).toBeNull();
    expect(fixture.nativeElement.querySelector('tbody tr')).toBeNull();
  });
});
