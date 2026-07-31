import { TestBed, ComponentFixture } from '@angular/core/testing';
import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';

import { ConfirmarReemplazo } from './confirmar-reemplazo';

/**
 * Congela el diálogo de confirmación del reemplazo de jornada. Calco del molde de
 * `confirmar-borrado.spec.ts`: `DialogRef` doblado con `vi.fn()`, sin overlay real.
 * Secuencia propia desde (1).
 *
 * Lo discriminante es (3): cancelar cierra SIN valor, no con `false`. Quien abre el
 * diálogo compara `=== true`, así que un `close(false)` funcionaría igual hoy pero
 * rompería la equivalencia con backdrop/Escape, que emiten `undefined`.
 */
describe('ConfirmarReemplazo', () => {
  let fixture: ComponentFixture<ConfirmarReemplazo>;
  let ref: { close: ReturnType<typeof vi.fn> };

  /** Métodos `protected` que los casos necesitan invocar. */
  type Interna = { confirmar: () => void; cancelar: () => void };

  const lineas = ['Se sustituirá la jornada guardada.', 'Solo si no hay dependientes.'];

  beforeEach(() => {
    ref = { close: vi.fn() };
    TestBed.configureTestingModule({
      imports: [ConfirmarReemplazo],
      providers: [
        { provide: DialogRef, useValue: ref },
        { provide: DIALOG_DATA, useValue: lineas },
      ],
    });
    fixture = TestBed.createComponent(ConfirmarReemplazo);
    fixture.detectChanges();
  });

  it('(1) enumera las líneas que le pasa quien lo abre', () => {
    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Se sustituirá la jornada guardada.');
    expect(texto).toContain('Solo si no hay dependientes.');
  });

  it('(2) confirmar cierra con true', () => {
    (fixture.componentInstance as unknown as Interna).confirmar();
    expect(ref.close).toHaveBeenCalledWith(true);
  });

  it('(3) cancelar cierra sin valor, como el backdrop y Escape', () => {
    (fixture.componentInstance as unknown as Interna).cancelar();
    expect(ref.close).toHaveBeenCalledWith();
  });
});
