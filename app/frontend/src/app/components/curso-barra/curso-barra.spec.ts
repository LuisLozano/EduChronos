import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { Dialog } from '@angular/cdk/dialog';

import { CursoBarra } from './curso-barra';
import { CursosDialogo } from '../cursos-dialogo/cursos-dialogo';
import { CursoDTO } from '../../models/curso.model';

/**
 * Spec de {@link CursoBarra}: qué se lee en la barra y qué se le pasa al selector
 * (O-curso, S160, C-selector-curso fase B). Secuencia propia del fichero desde (1).
 *
 * <p>El {@code Dialog} del CDK se sustituye por un doble que SÓLO registra la llamada: lo
 * que este fichero fija es que la barra abre el diálogo correcto con el curso que ella misma
 * está enseñando, no lo que el diálogo haga después —eso es `cursos-dialogo.spec`—. Abrir el
 * diálogo de verdad metería aquí su GET de la lista y sus estados, y el caso pasaría a medir
 * dos componentes a la vez.
 */
describe('CursoBarra', () => {
  let http: HttpTestingController;
  let abiertos: { componente: unknown; config: { data?: unknown } }[];

  const CURSO_ACTIVO: CursoDTO = {
    nombre: '2026/2027',
    archivado: false,
    propuestaSiguiente: '2027/2028',
  };

  beforeEach(async () => {
    abiertos = [];
    await TestBed.configureTestingModule({
      imports: [CursoBarra],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: Dialog,
          useValue: {
            open: (componente: unknown, config: { data?: unknown }) => {
              abiertos.push({ componente, config });
              return { closed: { subscribe: () => undefined } };
            },
          },
        },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  /** Monta la barra y responde su GET con lo que se le diga. */
  async function montar(respuesta: CursoDTO) {
    const fixture = TestBed.createComponent(CursoBarra);
    fixture.detectChanges();
    http.expectOne('/api/curso').flush(respuesta);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  it('(1) enseña el nombre del curso abierto', async () => {
    const fixture = await montar(CURSO_ACTIVO);
    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(texto).toContain('2026/2027');
  });

  it('(2) una base sin nombre de curso se lee «Curso sin nombre», no un hueco', async () => {
    // Condición 6: una base anterior a S159. El hueco vacío no distinguiría «aún no ha
    // llegado» de «no tiene nombre», que son dos cosas distintas para el usuario.
    const fixture = await montar({
      nombre: null,
      archivado: false,
      propuestaSiguiente: null,
    });
    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(texto).toContain('Curso sin nombre');
  });

  it('(3) con el curso archivado sale la marca de solo lectura, EN TEXTO y explicada', async () => {
    const fixture = await montar({
      nombre: '2025/2026',
      archivado: true,
      propuestaSiguiente: '2026/2027',
    });
    const marca = (fixture.nativeElement as HTMLElement).querySelector('.curso-barra__marca');

    expect(marca?.textContent).toContain('Solo lectura');
    // La severidad no viaja sólo por color (regla de styles.css): el par title +
    // aria-label es lo que hace que el dato llegue a quien no distingue el ámbar y a quien
    // usa un lector de pantalla. Se comprueban LOS DOS: con sólo el title, el lector calla.
    expect(marca?.getAttribute('title')).toContain('archivado');
    expect(marca?.getAttribute('aria-label')).toContain('archivado');
    expect(marca?.getAttribute('title')).toContain('2025/2026');
  });

  it('(4) con el curso activo NO hay marca de solo lectura', async () => {
    const fixture = await montar(CURSO_ACTIVO);

    expect(
      (fixture.nativeElement as HTMLElement).querySelector('.curso-barra__marca'),
    ).toBeNull();
  });

  it('(5) si el GET falla la barra no se rompe: lo dice y deja el motivo en el title', async () => {
    const fixture = TestBed.createComponent(CursoBarra);
    fixture.detectChanges();
    http.expectOne('/api/curso').flush(
      { causa: 'X', message: 'la base no responde' },
      { status: 500, statusText: 'Server Error' },
    );
    await fixture.whenStable();
    fixture.detectChanges();

    const elemento = fixture.nativeElement as HTMLElement;
    expect(elemento.textContent).toContain('Curso: no disponible');
    expect(elemento.querySelector('.curso-barra__nombre')?.getAttribute('title')).toContain(
      'la base no responde',
    );
    // El botón sigue ahí —la barra no se desmonta—, pero apagado: sin curso no hay nada
    // que pasarle al diálogo.
    const boton = elemento.querySelector('.curso-barra__boton') as HTMLButtonElement;
    expect(boton).not.toBeNull();
    expect(boton.disabled).toBe(true);
  });

  it('(6) «Cursos…» abre CursosDialogo con el CursoDTO que la barra está enseñando', async () => {
    const fixture = await montar(CURSO_ACTIVO);
    const boton = (fixture.nativeElement as HTMLElement).querySelector(
      '.curso-barra__boton',
    ) as HTMLButtonElement;

    expect(boton.textContent).toContain('Cursos');
    boton.click();

    expect(abiertos.length).toBe(1);
    expect(abiertos[0].componente).toBe(CursosDialogo);
    // El MISMO objeto, no uno equivalente: si el diálogo pidiera su propio GET, la barra y
    // él podrían acabar hablando de cursos distintos.
    expect(abiertos[0].config.data).toEqual(CURSO_ACTIVO);
  });
});
