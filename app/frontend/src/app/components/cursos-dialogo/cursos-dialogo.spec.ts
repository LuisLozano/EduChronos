import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';

import { CursosDialogo } from './cursos-dialogo';
import { RecargaPagina } from '../../services/recarga-pagina';
import { CursoDTO, CursoListadoDTO } from '../../models/curso.model';

/**
 * Spec de {@link CursosDialogo}: la lista, abrir otro curso y crear el siguiente (O-curso,
 * S160, C-selector-curso fase B). Secuencia propia del fichero desde (1).
 *
 * <p><b>{@link RecargaPagina} se sustituye por un contador, y ese es el punto entero de
 * varios casos.</b> El servicio real llama a {@code window.location.assign}, que en el
 * runner tiraría la página de pruebas; el doble además permite aseverar lo que de verdad
 * importa: que se recarga en el camino de éxito y que NO se recarga en el de error. Con un
 * fallo al abrir, la aplicación sigue en el curso de antes —el backend lo garantiza— y
 * recargar borraría el mensaje que explica por qué no se cambió.
 *
 * <p>El {@code DialogRef} es un doble que sólo cuenta los cierres: este diálogo no cierra
 * nunca en el camino de éxito —recarga—, y comprobar que NO cierra es parte del contrato.
 */
describe('CursosDialogo', () => {
  let http: HttpTestingController;
  let recargas: number;
  let cierres: number;

  const ABIERTO_ACTIVO: CursoDTO = {
    nombre: '2026/2027',
    archivado: false,
    propuestaSiguiente: '2027/2028',
  };

  const ABIERTO_ARCHIVADO: CursoDTO = {
    nombre: '2025/2026',
    archivado: true,
    propuestaSiguiente: '2026/2027',
  };

  const SIN_NOMBRE: CursoDTO = {
    nombre: null,
    archivado: false,
    propuestaSiguiente: null,
  };

  function fila(
    fichero: string,
    nombre: string | null,
    archivado: boolean,
    abierto: boolean,
  ): CursoListadoDTO {
    return { fichero, nombre, archivado, abierto };
  }

  function montarCon(curso: CursoDTO) {
    TestBed.configureTestingModule({
      imports: [CursosDialogo],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: DIALOG_DATA, useValue: curso },
        { provide: DialogRef, useValue: { close: () => (cierres += 1) } },
        { provide: RecargaPagina, useValue: { recargarEnInicio: () => (recargas += 1) } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(CursosDialogo);
    fixture.detectChanges();
    return fixture;
  }

  /** Monta el diálogo y responde el GET de la lista. */
  async function montar(curso: CursoDTO, lista: CursoListadoDTO[]) {
    const fixture = montarCon(curso);
    http.expectOne('/api/cursos').flush(lista);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  function html(fixture: { nativeElement: unknown }): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  /** Pasa al formulario de duplicar pulsando el botón, no llamando al método. */
  async function irADuplicar(fixture: { nativeElement: unknown; detectChanges: () => void; whenStable: () => Promise<unknown> }) {
    (html(fixture).querySelector('.cursos-dialogo__duplicar') as HTMLButtonElement).click();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  beforeEach(() => {
    recargas = 0;
    cierres = 0;
    TestBed.resetTestingModule();
  });

  afterEach(() => http.verify());

  // ───────────────────────────────────────────────────────────────────────────────── lista

  it('(1) la fila abierta no ofrece «Abrir» y las demás sí', async () => {
    const fixture = await montar(ABIERTO_ACTIVO, [
      fila('curso-2026-2027.db', '2026/2027', false, true),
      fila('educhronos.db', '2025/2026', true, false),
    ]);
    const filas = Array.from(html(fixture).querySelectorAll('.cursos-dialogo__fila'));

    expect(filas.length).toBe(2);
    // La abierta: texto «Abierto ahora» y NINGÚN botón. Un botón «Abrir» sobre el curso que
    // ya está abierto invitaría a un cambio que no cambia nada y que tira el pool.
    expect(filas[0].textContent).toContain('Abierto ahora');
    expect(filas[0].querySelector('.cursos-dialogo__abrir')).toBeNull();
    // Y su estado, en texto.
    expect(filas[0].textContent).toContain('Activo');

    expect(filas[1].querySelector('.cursos-dialogo__abrir')).not.toBeNull();
    expect(filas[1].textContent).toContain('Archivado');
    expect(filas[1].textContent).toContain('educhronos.db');
  });

  it('(2) un curso sin nombre se lista como «Sin nombre» con su fichero', async () => {
    const fixture = await montar(SIN_NOMBRE, [
      fila('educhronos.db', null, false, true),
      fila('curso-2019-2020.db', null, false, false),
    ]);
    const filas = Array.from(html(fixture).querySelectorAll('.cursos-dialogo__fila'));

    expect(filas[1].textContent).toContain('Sin nombre');
    expect(filas[1].textContent).toContain('curso-2019-2020.db');
  });

  // ────────────────────────────────────────────────────────────────────────────────── abrir

  it('(3) abrir con éxito manda el fichero y RECARGA', async () => {
    const fixture = await montar(ABIERTO_ACTIVO, [
      fila('curso-2026-2027.db', '2026/2027', false, true),
      fila('educhronos.db', '2025/2026', true, false),
    ]);
    (html(fixture).querySelector('.cursos-dialogo__abrir') as HTMLButtonElement).click();

    const req = http.expectOne('/api/cursos/abrir');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ fichero: 'educhronos.db' });
    req.flush(ABIERTO_ARCHIVADO);
    await fixture.whenStable();

    expect(recargas).toBe(1);
    // No se cierra: la recarga se lleva el diálogo por delante. Cerrar además dejaría un
    // parpadeo con la pantalla vieja detrás.
    expect(cierres).toBe(0);
  });

  it('(4) abrir con error enseña el mensaje del servidor y NO recarga', async () => {
    const fixture = await montar(ABIERTO_ACTIVO, [
      fila('curso-2026-2027.db', '2026/2027', false, true),
      fila('roto.db', null, false, false),
    ]);
    (html(fixture).querySelector('.cursos-dialogo__abrir') as HTMLButtonElement).click();

    http.expectOne('/api/cursos/abrir').flush(
      { causa: 'CURSO_NO_ABRE', message: 'No se ha podido abrir roto.db. Se sigue en el curso anterior.' },
      { status: 500, statusText: 'Server Error' },
    );
    await fixture.whenStable();
    fixture.detectChanges();

    expect(recargas).toBe(0);
    expect(cierres).toBe(0);
    const error = html(fixture).querySelector('.cursos-dialogo__error');
    expect(error?.textContent).toContain('Se sigue en el curso anterior');
    // El rol es lo que hace que un lector de pantalla lo anuncie sin que nadie navegue.
    expect(error?.getAttribute('role')).toBe('alert');
    // Y la lista sigue ahí: se puede reintentar con otro curso sin volver a abrir nada.
    expect(html(fixture).querySelectorAll('.cursos-dialogo__fila').length).toBe(2);
  });

  // ──────────────────────────────────────────────────────── habilitación de «Duplicar curso…»

  it('(5) con el curso abierto ACTIVO, duplicar está habilitado y sin motivo que explicar', async () => {
    const fixture = await montar(ABIERTO_ACTIVO, [
      fila('curso-2026-2027.db', '2026/2027', false, true),
      fila('educhronos.db', '2025/2026', true, false),
    ]);

    const boton = html(fixture).querySelector('.cursos-dialogo__duplicar') as HTMLButtonElement;
    expect(boton.disabled).toBe(false);
    expect(html(fixture).querySelector('.cursos-dialogo__motivo')).toBeNull();
  });

  it('(6) con el abierto ARCHIVADO y un activo en la lista, deshabilitado Y con el motivo a la vista', async () => {
    const fixture = await montar(ABIERTO_ARCHIVADO, [
      fila('educhronos.db', '2025/2026', true, true),
      fila('curso-2026-2027.db', '2026/2027', false, false),
    ]);

    const boton = html(fixture).querySelector('.cursos-dialogo__duplicar') as HTMLButtonElement;
    expect(boton.disabled).toBe(true);
    // El motivo VISIBLE es la mitad del contrato: un botón apagado sin explicación deja al
    // usuario probando clics.
    expect(html(fixture).querySelector('.cursos-dialogo__motivo')?.textContent).toContain(
      'Abre el curso activo para duplicarlo',
    );
  });

  it('(7) requisito (b): con el abierto archivado y NINGÚN activo, habilitado', async () => {
    // La salida del callejón: un centro que archivó su único curso tiene que poder crear
    // uno. Es la misma regla que el backend calcula en sinNingunCursoActivo().
    const fixture = await montar(ABIERTO_ARCHIVADO, [
      fila('educhronos.db', '2025/2026', true, true),
      fila('curso-2024-2025.db', '2024/2025', true, false),
    ]);

    const boton = html(fixture).querySelector('.cursos-dialogo__duplicar') as HTMLButtonElement;
    expect(boton.disabled).toBe(false);
    expect(html(fixture).querySelector('.cursos-dialogo__motivo')).toBeNull();
  });

  // ─────────────────────────────────────────────────────────────────────────────── duplicar

  it('(8) el campo «Curso nuevo» llega prefijado con la propuesta', async () => {
    const fixture = await montar(ABIERTO_ACTIVO, [
      fila('curso-2026-2027.db', '2026/2027', false, true),
    ]);
    await irADuplicar(fixture);

    const campo = html(fixture).querySelector('#curso-nuevo') as HTMLInputElement;
    expect(campo.value).toBe('2027/2028');
    // NINGÚN placeholder, y es una corrección medida con el arquitecto (S160, M4 de la fase
    // B): un ejemplo en gris dentro del campo se lee como un valor ya puesto. Lo único que
    // hay dentro del campo es la propuesta, que sí es un valor. La forma se dice debajo.
    expect(campo.getAttribute('placeholder')).toBeNull();
    // Y el del curso actual NO se pinta: la base abierta sí trae nombre.
    expect(html(fixture).querySelector('#curso-actual')).toBeNull();
  });

  it('(9) el campo del curso actual sale SOLO si el abierto no tiene nombre, y su valor viaja', async () => {
    const fixture = await montar(SIN_NOMBRE, [fila('educhronos.db', null, false, true)]);
    await irADuplicar(fixture);

    const nuevo = html(fixture).querySelector('#curso-nuevo') as HTMLInputElement;
    const actual = html(fixture).querySelector('#curso-actual') as HTMLInputElement;
    expect(actual).not.toBeNull();
    expect(actual.getAttribute('placeholder')).toBeNull();
    // Sin propuesta de la que partir: el campo nace vacío, no con una inventada.
    expect(nuevo.value).toBe('');

    nuevo.value = '2026/2027';
    nuevo.dispatchEvent(new Event('input'));
    actual.value = '2025/2026';
    actual.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    (html(fixture).querySelector('.cursos-dialogo__crear') as HTMLButtonElement).click();

    const req = http.expectOne('/api/cursos');
    expect(req.request.body).toEqual({
      nombreNuevo: '2026/2027',
      nombreActual: '2025/2026',
    });
    req.flush({ nombre: '2026/2027', fichero: 'curso-2026-2027.db' });
    await fixture.whenStable();
    expect(recargas).toBe(1);
  });

  it('(10) una forma inválida no llega a salir por la red', async () => {
    const fixture = await montar(ABIERTO_ACTIVO, [
      fila('curso-2026-2027.db', '2026/2027', false, true),
    ]);
    await irADuplicar(fixture);

    const campo = html(fixture).querySelector('#curso-nuevo') as HTMLInputElement;
    campo.value = '2026-2027';
    campo.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    (html(fixture).querySelector('.cursos-dialogo__crear') as HTMLButtonElement).click();

    // Ni una petición: el afterEach de http.verify() lo confirmaría igual, pero el aserto
    // explícito dice POR QUÉ no la hay.
    http.expectNone('/api/cursos');
    expect(recargas).toBe(0);
  });

  it('(11) duplicar con éxito RECARGA', async () => {
    const fixture = await montar(ABIERTO_ACTIVO, [
      fila('curso-2026-2027.db', '2026/2027', false, true),
    ]);
    await irADuplicar(fixture);
    (html(fixture).querySelector('.cursos-dialogo__crear') as HTMLButtonElement).click();

    const req = http.expectOne('/api/cursos');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ nombreNuevo: '2027/2028' });
    req.flush({ nombre: '2027/2028', fichero: 'curso-2027-2028.db' });
    await fixture.whenStable();

    expect(recargas).toBe(1);
    expect(cierres).toBe(0);
  });

  it('(12) duplicar con CURSO_YA_EXISTE enseña el mensaje, no recarga y CONSERVA lo escrito', async () => {
    const fixture = await montar(ABIERTO_ACTIVO, [
      fila('curso-2026-2027.db', '2026/2027', false, true),
    ]);
    await irADuplicar(fixture);

    const campo = html(fixture).querySelector('#curso-nuevo') as HTMLInputElement;
    campo.value = '2026/2027';
    campo.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    (html(fixture).querySelector('.cursos-dialogo__crear') as HTMLButtonElement).click();

    http.expectOne('/api/cursos').flush(
      { causa: 'CURSO_YA_EXISTE', message: 'Ya existe un curso 2026/2027 en la carpeta de datos.' },
      { status: 409, statusText: 'Conflict' },
    );
    await fixture.whenStable();
    fixture.detectChanges();

    expect(recargas).toBe(0);
    expect(cierres).toBe(0);
    expect(html(fixture).querySelector('.cursos-dialogo__error')?.textContent).toContain(
      'Ya existe un curso 2026/2027',
    );
    // Lo tecleado sigue ahí: el caso corriente es una errata, y borrarlo sería castigarla.
    expect(
      (html(fixture).querySelector('#curso-nuevo') as HTMLInputElement).value,
    ).toBe('2026/2027');
  });

  it('(13) «Volver» regresa a la lista sin escribir nada', async () => {
    const fixture = await montar(ABIERTO_ACTIVO, [
      fila('curso-2026-2027.db', '2026/2027', false, true),
    ]);
    await irADuplicar(fixture);
    expect(html(fixture).querySelector('#curso-nuevo')).not.toBeNull();

    (html(fixture).querySelector('.cursos-dialogo__cerrar') as HTMLButtonElement).click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(html(fixture).querySelector('#curso-nuevo')).toBeNull();
    expect(html(fixture).querySelectorAll('.cursos-dialogo__fila').length).toBe(1);
    expect(recargas).toBe(0);
    expect(cierres).toBe(0);
  });

  it('(14) si el GET de la lista falla, el diálogo lo dice y no ofrece duplicar', async () => {
    const fixture = montarCon(ABIERTO_ACTIVO);
    http.expectOne('/api/cursos').flush(
      { causa: 'X', message: 'la base no responde' },
      { status: 500, statusText: 'Server Error' },
    );
    await fixture.whenStable();
    fixture.detectChanges();

    expect(html(fixture).querySelector('.cursos-dialogo__error')?.textContent).toContain(
      'la base no responde',
    );
    // Sin lista no se puede decidir el requisito (b), así que no se ofrece el botón: mejor
    // no ofrecerlo que ofrecerlo mal.
    expect(html(fixture).querySelector('.cursos-dialogo__duplicar')).toBeNull();
  });
});
