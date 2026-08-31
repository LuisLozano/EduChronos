import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { Dialog } from '@angular/cdk/dialog';
import { AulaLista } from './aula-lista';
import { Aula } from '../../models/aula.model';

/**
 * Congela el comportamiento de la lista de aulas. Hereda del molde de
 * `profesor-lista.spec.ts` los casos de carga (1)(2)(3) y de traducción del 409
 * (5)(6) —texto del backend cuando viaja, degradado cuando no—, y añade los que la
 * entidad trae de nuevo:
 *
 * <ul>
 *   <li>(4) los opcionales en null se pintan como «—», no como celda vacía ni «null»;
 *   <li>(7) un guardado en el diálogo RECARGA la lista, y un cierre sin guardar no;
 *   <li>(8) el diálogo se abre con el aula al editar y con null al crear;
 *   <li>(9) la lista NO reordena: respeta el orden que da el servidor, que ya ordena
 *       por código en `AulaService.listar()`.
 * </ul>
 *
 * El borrado se dispara con el diálogo espiado a `true`, sin overlay real. Secuencia
 * propia desde (1). Fuera de alcance por decisión: estilos, y el orden interno de
 * `mensaje()` (cubierto en el form, misma función).
 */
describe('AulaLista', () => {
  let fixture: ComponentFixture<AulaLista>;
  let http: HttpTestingController;
  let dialog: { open: ReturnType<typeof vi.fn> };

  /** Métodos `protected` que los casos necesitan invocar. */
  type Interna = { borrar: (a: Aula) => void; nueva: () => void; editar: (a: Aula) => void };

  const aulaCompleta: Aula = {
    id: 7,
    codigo: 'A12',
    tipo: 'INFORMATICA',
    capacidad: 30,
    edificio: 'Norte',
    planta: 1,
    sector: 'B',
  };

  beforeEach(() => {
    dialog = { open: vi.fn() };
    TestBed.configureTestingModule({
      imports: [AulaLista],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Dialog, useValue: dialog },
      ],
    });
    fixture = TestBed.createComponent(AulaLista);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function flushLista(filas: unknown[] = []): void {
    fixture.detectChanges(); // dispara ngOnInit → cargar()
    http.expectOne('/api/aulas').flush(filas);
  }

  /** Hace que el próximo diálogo abierto cierre con el valor dado. */
  function dialogoDevuelve(valor: boolean | undefined): void {
    dialog.open.mockReturnValue({
      closed: { subscribe: (fn: (v: boolean | undefined) => void) => fn(valor) },
    });
  }

  function textoError(): string {
    return fixture.nativeElement.querySelector('.estado-lista__error').textContent;
  }

  it('(1) carga la lista en init y la pinta', async () => {
    flushLista([aulaCompleta]);
    await fixture.whenStable();
    const filas = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(filas.length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('A12');
    expect(fixture.nativeElement.textContent).toContain('INFORMATICA');
  });

  it('(2) lista vacía muestra la invitación a crear la primera', async () => {
    flushLista([]);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.estado-lista__vacio')).toBeTruthy();
  });

  it('(3) error de carga cae al degradado con status', async () => {
    fixture.detectChanges();
    http.expectOne('/api/aulas').flush('', { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    expect(textoError()).toContain('No se pudo cargar');
    expect(textoError()).toContain('500');
  });

  it('(4) los opcionales en null se pintan como guion, no vacíos', async () => {
    flushLista([
      { id: 8, codigo: 'B01', tipo: 'ORDINARIA', capacidad: null, edificio: null, planta: null, sector: null },
    ]);
    await fixture.whenStable();
    const celdas = [...fixture.nativeElement.querySelectorAll('tbody tr td')]
      .map((td: HTMLElement) => td.textContent?.trim());
    // codigo, tipo, y luego los cuatro opcionales; la 7ª celda son las acciones.
    expect(celdas.slice(0, 6)).toEqual(['B01', 'ORDINARIA', '—', '—', '—', '—']);
  });

  it('(5) al borrar, un 409 CON message muestra el texto RICO del backend, no el degradado', async () => {
    flushLista([aulaCompleta]);
    await fixture.whenStable();
    dialogoDevuelve(true); // el diálogo de confirmación confirma

    (fixture.componentInstance as unknown as Interna).borrar(aulaCompleta);

    http.expectOne('/api/aulas/7').flush(
      { message: 'No se puede borrar: referenciada por 2 plaza(s), 1 sesion(es)' },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();
    expect(textoError()).toContain('referenciada por 2 plaza(s)');
    // discriminante: NO cae al degradado
    expect(textoError()).not.toContain('No se pudo borrar el aula A12 (409)');
  });

  it('(6) al borrar, un 409 SIN message cae al degradado que dice qué pasó', async () => {
    flushLista([aulaCompleta]);
    await fixture.whenStable();
    dialogoDevuelve(true);

    (fixture.componentInstance as unknown as Interna).borrar(aulaCompleta);

    http.expectOne('/api/aulas/7').flush({}, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();
    expect(textoError()).toContain('No se pudo borrar el aula A12');
    expect(textoError()).toContain('409');
  });

  it('(7) un guardado en el diálogo recarga la lista; un cierre sin guardar no', async () => {
    flushLista([]);
    await fixture.whenStable();
    const inst = fixture.componentInstance as unknown as Interna;

    dialogoDevuelve(true);
    inst.nueva();
    http.expectOne('/api/aulas').flush([aulaCompleta]); // recargó
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('A12');

    // backdrop/Escape emiten undefined: no debe salir un segundo GET, y el
    // http.verify() del afterEach lo pondría rojo si saliera.
    dialogoDevuelve(undefined);
    inst.nueva();
  });

  it('(8) editar pasa el aula al diálogo; nueva pasa null', async () => {
    flushLista([aulaCompleta]);
    await fixture.whenStable();
    const inst = fixture.componentInstance as unknown as Interna;
    dialogoDevuelve(undefined); // se cancela: aquí solo interesa CON QUÉ se abrió

    inst.editar(aulaCompleta);
    expect(dialog.open.mock.calls.at(-1)?.[1]).toEqual({ data: aulaCompleta });

    inst.nueva();
    expect(dialog.open.mock.calls.at(-1)?.[1]).toEqual({ data: null });
  });

  it('(9) no reordena: respeta el orden que da el servidor', async () => {
    // Llegan DESordenadas a propósito. Si la lista ordenase por código en el
    // cliente, saldrían A12/B01/C99 y este aserto caería.
    flushLista([
      { ...aulaCompleta, id: 3, codigo: 'C99' },
      { ...aulaCompleta, id: 1, codigo: 'A12' },
      { ...aulaCompleta, id: 2, codigo: 'B01' },
    ]);
    await fixture.whenStable();
    const codigos = [...fixture.nativeElement.querySelectorAll('tbody tr td:first-child')]
      .map((td: HTMLElement) => td.textContent?.trim());
    expect(codigos).toEqual(['C99', 'A12', 'B01']);
  });

  /**
   * La cabecera compartida está MONTADA y con SUS rótulos (S124). Los dos asertos
   * son distintos a propósito: el del `<h2>` caza un `titulo` mal cableado, y el
   * del botón caza el género —«Nueva aula» no se deriva de «Aulas»— que es lo
   * que el e2e localiza por texto exacto. Un `<app-cabecera-lista>` presente pero
   * sin inputs pasaría un `querySelector` a secas y falla estos dos.
   */
  it('(10) monta la cabecera compartida con su rótulo y su texto de alta', async () => {
    flushLista([]);
    await fixture.whenStable();
    const cabecera = fixture.nativeElement.querySelector('app-cabecera-lista');
    expect(cabecera).toBeTruthy();
    expect(cabecera.querySelector('.cabecera-lista__titulo').textContent).toContain('Aulas');
    expect(cabecera.querySelector('.cabecera-lista__nuevo').textContent.trim()).toBe('Nueva aula');
  });

  /** Teclea en la caja de la cabecera como una persona, y deja repintar. */
  async function buscar(texto: string): Promise<void> {
    const caja = fixture.nativeElement.querySelector('.cabecera-lista__busqueda');
    caja.value = texto;
    caja.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  /**
   * La búsqueda recorta la tabla y el contador lo dice (busca por una columna del fondo de la fila: el edificio).
   * Los tres asertos van juntos a propósito: el conteo de filas mide el `@for`
   * sobre `visibles()`, el texto que SOBREVIVE y el que DESAPARECE miden que
   * recorta la correcta —no «una cualquiera»—, y el contador ata el cableado de
   * la cabecera, que recibe `coincidencias` por separado de `total`.
   */
  it('(11) al buscar se ve solo la fila que casa, y el contador dice «1 de 2»', async () => {
    flushLista([aulaCompleta, { ...aulaCompleta, id: 8, codigo: 'B01', edificio: 'Sur' }]);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(2);

    await buscar('sur');

    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('B01');
    expect(fixture.nativeElement.textContent).not.toContain('A12');
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
  it('(12) una búsqueda sin resultados da su propio mensaje, no el de catálogo vacío', async () => {
    flushLista([aulaCompleta, { ...aulaCompleta, id: 8, codigo: 'B01', edificio: 'Sur' }]);
    await fixture.whenStable();

    await buscar('zzz');

    const sinResultados = fixture.nativeElement.querySelector('.estado-lista__sin-resultados');
    expect(sinResultados).toBeTruthy();
    expect(sinResultados.textContent).toContain('zzz');
    expect(fixture.nativeElement.querySelector('.estado-lista__vacio')).toBeNull();
    expect(fixture.nativeElement.querySelector('tbody tr')).toBeNull();
  });

  /**
   * Centinela del `textoVacio` que viaja por `input`, como el (11) de
   * `nivel-lista.spec.ts` y por el mismo motivo: el caso (3) de
   * `estado-lista.spec.ts` prueba que el componente pinta lo que recibe, no que
   * la lista mande lo correcto. Aulas discrimina por el GÉNERO —«Crea la primera
   * con «Nueva aula»», femenino—: cinco de las siete listas dicen «el primero»,
   * así que un texto copiado de la lista de al lado, o derivado del rótulo, cae
   * aquí. Cadena entera con `toBe`, que es lo que hace visible la concordancia.
   */
  it('(13) con el catálogo vacío pinta el texto propio de aulas, en femenino', async () => {
    flushLista([]);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('.estado-lista__vacio')?.textContent?.trim()).toBe(
      'No hay aulas todavía. Crea la primera con «Nueva aula».',
    );
  });
});
