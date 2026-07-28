import { TestBed, ComponentFixture } from '@angular/core/testing';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { ConfirmarBorrado } from './confirmar-borrado';

/**
 * Congela el diálogo genérico de confirmación: pinta las líneas, y los dos
 * botones cierran con `true` (Borrar) o sin valor (Cancelar). Monta el componente
 * real con DialogRef y DIALOG_DATA por useValue, sin overlay (molde de
 * confirmar-generacion.spec). Secuencia propia desde (1).
 */
describe('ConfirmarBorrado', () => {
  let fixture: ComponentFixture<ConfirmarBorrado>;
  let ref: { close: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    ref = { close: vi.fn() };
    TestBed.configureTestingModule({
      imports: [ConfirmarBorrado],
      providers: [
        { provide: DialogRef, useValue: ref },
        { provide: DIALOG_DATA, useValue: ['¿Borrar a Ana Ruiz (MAT8)?'] },
      ],
    });
    fixture = TestBed.createComponent(ConfirmarBorrado);
    fixture.detectChanges();
  });

  it('(1) pinta las líneas recibidas por DIALOG_DATA', () => {
    expect(fixture.nativeElement.textContent).toContain('¿Borrar a Ana Ruiz (MAT8)?');
  });

  it('(2) Borrar cierra con true', () => {
    fixture.nativeElement.querySelector('.confirmar-borrado__confirmar').click();
    expect(ref.close).toHaveBeenCalledWith(true);
  });

  it('(3) Cancelar cierra sin valor (no confirma)', () => {
    fixture.nativeElement.querySelector('.confirmar-borrado__cancelar').click();
    expect(ref.close).toHaveBeenCalledWith();
  });
});
