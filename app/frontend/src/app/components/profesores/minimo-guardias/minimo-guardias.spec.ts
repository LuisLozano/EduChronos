import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
  TestRequest,
} from '@angular/common/http/testing';
import { MinimoGuardias } from './minimo-guardias';

/**
 * Congela el mínimo de profesores de guardia por tramo (S212, C-dato-guardias, E8): el GET al
 * montar pinta lo que responde, Guardar hace UN PUT con el cuerpo exacto, un valor no válido
 * deshabilita Guardar y no sale petición, y los errores se ven. Los valores (5 del GET, 6 del
 * PUT) son distintos del 4 por defecto para que ninguno pueda salir de un literal.
 */
describe('MinimoGuardias', () => {
  let fixture: ComponentFixture<MinimoGuardias>;
  let http: HttpTestingController;

  /** Monta el componente y devuelve su GET de arranque, sin responder. */
  function montarSinResponder(): TestRequest {
    TestBed.configureTestingModule({
      imports: [MinimoGuardias],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(MinimoGuardias);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges(); // ngOnInit → GET
    const req = http.expectOne('/api/configuracion-guardias');
    expect(req.request.method).toBe('GET');
    return req;
  }

  /** Monta el componente y responde el GET de arranque. */
  async function montar(respuesta: object = { minimoPorTramo: 5 }): Promise<void> {
    montarSinResponder().flush(respuesta);
    await fixture.whenStable();
  }

  afterEach(() => http.verify());

  const raiz = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const input = (): HTMLInputElement => raiz().querySelector('.minimo-guardias__input') as HTMLInputElement;
  const boton = (): HTMLButtonElement => raiz().querySelector('.minimo-guardias__guardar') as HTMLButtonElement;

  /** Escribe en el campo como el usuario: valor y evento `input`. */
  async function teclear(valor: string): Promise<void> {
    input().value = valor;
    input().dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  it('(1) pinta la etiqueta y el valor que responde el GET de arranque', async () => {
    await montar({ minimoPorTramo: 5 });

    expect(raiz().textContent).toContain('Mínimo de profesores de guardia por tramo');
    expect(input().value).toBe('5');
    expect(boton().disabled).toBe(false);
  });

  it('(2) poner 6 y Guardar hace un PUT con el cuerpo exacto { minimoPorTramo: 6 }', async () => {
    await montar();
    await teclear('6');
    boton().click();

    const req = http.expectOne('/api/configuracion-guardias');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ minimoPorTramo: 6 });
    req.flush({ minimoPorTramo: 6 });
    await fixture.whenStable();
    expect(input().value).toBe('6');
  });

  it('(3) con −1 Guardar está deshabilitado, avisa y no sale ningún PUT', async () => {
    await montar();
    await teclear('-1');

    expect(boton().disabled).toBe(true);
    boton().click();
    http.expectNone('/api/configuracion-guardias');
    expect(raiz().querySelector('.minimo-guardias__error-campo')!.textContent!.trim()).toBe(
      'Tiene que ser un número entero, 0 o más.',
    );
  });

  it('(4) un PUT que responde 400 muestra el texto del backend', async () => {
    await montar();
    await teclear('6');
    boton().click();

    http
      .expectOne('/api/configuracion-guardias')
      .flush({ message: 'minimoPorTramo es obligatorio' }, { status: 400, statusText: 'Bad Request' });
    await fixture.whenStable();
    expect(raiz().querySelector('[role="alert"]')!.textContent!.trim()).toBe('minimoPorTramo es obligatorio');
  });

  it('(5) si falla el GET de arranque, sale el degradado y no se puede guardar', async () => {
    montarSinResponder().flush('', { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();

    expect(raiz().querySelector('[role="alert"]')!.textContent!.trim()).toBe(
      'No se pudo cargar el mínimo de guardias (500).',
    );
    expect(boton().disabled).toBe(true);
  });
});
