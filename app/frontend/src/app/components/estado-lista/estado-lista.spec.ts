import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EstadoLista } from './estado-lista';

/**
 * Componente PRESENTACIONAL: los seis inputs son `required`, así que entran por
 * `componentRef.setInput`. Sin `HttpTestingController` ni `Dialog`. Zoneless: el
 * render se espera con `await fixture.whenStable()`, misma disciplina que
 * `cabecera-lista.spec.ts`.
 *
 * <p>Los recuentos van con números DISTINTOS a propósito (42 y 7) siempre que la
 * rama no exija otra cosa: con `total` y `coincidencias` iguales, un mutante que
 * intercambiara los dos inputs no se vería en ningún caso.
 */
describe('EstadoLista', () => {
  let fixture: ComponentFixture<EstadoLista>;
  let raiz: HTMLElement;

  const VACIO = 'No hay profesores todavía. Crea el primero con «Nuevo profesor».';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EstadoLista],
    }).compileComponents();

    fixture = TestBed.createComponent(EstadoLista);
    raiz = fixture.nativeElement as HTMLElement;
    montar();
  });

  /** Estado de partida: cargado, con filas y sin error. Ninguna rama encendida. */
  function montar(): void {
    fixture.componentRef.setInput('cargando', false);
    fixture.componentRef.setInput('error', null);
    fixture.componentRef.setInput('total', 42);
    fixture.componentRef.setInput('coincidencias', 42);
    fixture.componentRef.setInput('busqueda', '');
    fixture.componentRef.setInput('textoVacio', VACIO);
  }

  /** Texto de la rama pintada, recortado. `null` si esa rama no está. */
  function texto(clase: string): string | null {
    return raiz.querySelector(`.estado-lista__${clase}`)?.textContent?.trim() ?? null;
  }

  /** Cuántos párrafos pinta el componente en total. Cero es un estado legítimo. */
  function parrafos(): number {
    return raiz.querySelectorAll('p').length;
  }

  /**
   * El hueco donde el padre pinta la tabla. Es el caso que impide escribir un
   * componente que siempre pinte algo: con datos y sin consulta NO hay estado.
   */
  it('(1) con filas, sin consulta y sin error no pinta ninguna rama', async () => {
    await fixture.whenStable();
    expect(parrafos()).toBe(0);
  });

  /**
   * `cargando` gana a todo lo demás. Se comprueba con `total` en 0 a la vez: un
   * orden invertido en el `computed` pintaría el vacío durante la carga, que es
   * la mentira que la cadena existe para evitar.
   */
  it('(2) cargando gana al catálogo vacío', async () => {
    fixture.componentRef.setInput('cargando', true);
    fixture.componentRef.setInput('total', 0);
    fixture.componentRef.setInput('coincidencias', 0);
    await fixture.whenStable();
    expect(texto('cargando')).toBe('Cargando…');
    expect(texto('vacio')).toBeNull();
  });

  /**
   * El vacío pinta el TEXTO recibido, no uno derivado: un componente que
   * construyera la frase a partir de un rótulo fallaría el `toBe` exacto. Es lo
   * que permite que Niveles añada su segunda frase sin ser una excepción.
   */
  it('(3) con el catálogo vacío pinta el texto recibido, literal', async () => {
    fixture.componentRef.setInput('total', 0);
    fixture.componentRef.setInput('coincidencias', 0);
    await fixture.whenStable();
    expect(texto('vacio')).toBe(VACIO);
  });

  /**
   * D-vacio-miente-con-error, EL CASO CENTRAL. Fallo de carga: no hay filas
   * porque no llegaron, no porque el catálogo esté vacío. Se aseveran las dos
   * mitades —el error SÍ, el vacío NO— porque un componente que no pintara
   * ninguna de las dos también satisfaría la segunda.
   */
  it('(4) con el catálogo vacío Y error pinta el error y NO el vacío', async () => {
    fixture.componentRef.setInput('total', 0);
    fixture.componentRef.setInput('coincidencias', 0);
    fixture.componentRef.setInput('error', 'No se pudo cargar la lista.');
    await fixture.whenStable();
    expect(texto('error')).toBe('No se pudo cargar la lista.');
    expect(texto('vacio')).toBeNull();
  });

  /**
   * La trampa del arreglo escrito. Con `total === 0 && !error()` puesto tal cual
   * en un `@else if`, el caso (4) cae al hermano siguiente —`coincidencias === 0`
   * también es cierto— y pinta «Ningún resultado para «»». Este aserto es lo
   * único que distingue el arreglo correcto del que solo mueve la mentira de
   * sitio, y por eso va como caso propio y no como una línea del (4).
   */
  it('(5) con el catálogo vacío Y error tampoco pinta «sin resultados»', async () => {
    fixture.componentRef.setInput('total', 0);
    fixture.componentRef.setInput('coincidencias', 0);
    fixture.componentRef.setInput('error', 'No se pudo cargar la lista.');
    await fixture.whenStable();
    expect(texto('sin-resultados')).toBeNull();
    expect(parrafos()).toBe(1);
  });

  /**
   * LA REVERSIÓN DE S124, protegida. Un 409 de borrado deja error CON filas
   * cargadas: el error se pinta y ninguna rama de recuento se enciende, para que
   * la lista siga mostrando su tabla. Un componente que apagara el recuento con
   * el error en general seguiría pasando; lo que este caso vigila es que no
   * apague nada MÁS que eso. La tabla vive en el padre, así que aquí se asevera
   * su complemento: un solo párrafo, el del error.
   */
  it('(6) con filas cargadas y error de borrado pinta solo el error', async () => {
    fixture.componentRef.setInput('error', 'No se puede borrar: referenciado por 3 actividad(es).');
    await fixture.whenStable();
    expect(texto('error')).toContain('No se puede borrar');
    expect(parrafos()).toBe(1);
  });

  /**
   * «Sin resultados» cita la consulta, y los dos recuentos son distintos (42 y
   * 0): un componente que mirara `total` en vez de `coincidencias` no encendería
   * esta rama en absoluto.
   */
  it('(7) con consulta sin coincidencias cita el texto buscado', async () => {
    fixture.componentRef.setInput('coincidencias', 0);
    fixture.componentRef.setInput('busqueda', 'zzz');
    await fixture.whenStable();
    expect(texto('sin-resultados')).toBe('Ningún resultado para «zzz».');
    expect(texto('vacio')).toBeNull();
  });

  /**
   * El `role="alert"` viaja con el mensaje: sin él un lector de pantalla no
   * anuncia el fallo, y es un atributo, no texto, así que ningún aserto sobre
   * contenido lo cubre.
   */
  it('(8) el error se anuncia con role="alert"', async () => {
    fixture.componentRef.setInput('error', 'Vaya.');
    await fixture.whenStable();
    expect(raiz.querySelector('.estado-lista__error')?.getAttribute('role')).toBe('alert');
  });

  /**
   * El componente REACCIONA: pasar de cargando a cargado con el catálogo vacío
   * cambia de rama. Un `computed` sustituido por un valor calculado una vez en el
   * constructor pasaría los ocho casos anteriores y caería aquí.
   */
  it('(9) al terminar la carga con catálogo vacío cambia de rama', async () => {
    fixture.componentRef.setInput('cargando', true);
    fixture.componentRef.setInput('total', 0);
    fixture.componentRef.setInput('coincidencias', 0);
    await fixture.whenStable();
    expect(texto('cargando')).toBe('Cargando…');

    fixture.componentRef.setInput('cargando', false);
    await fixture.whenStable();
    expect(texto('cargando')).toBeNull();
    expect(texto('vacio')).toBe(VACIO);
  });
});
