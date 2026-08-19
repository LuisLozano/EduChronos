import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Dialog, DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { Observable, Subject, of, throwError } from 'rxjs';
import { PdcDialogo } from './pdc-dialogo';
import { PdcService } from '../../services/pdc.service';
import { Grupo } from '../../models/grupo.model';

/**
 * Congela el diálogo del PDC: la derivación de sus CUATRO estados a partir de la
 * respuesta de `obtener` (1)-(5) y las dos escrituras con sus dos caras (6)-(9).
 * Secuencia propia desde (1).
 *
 * <p><b>`PdcService` va MOCKEADO, no por `HttpTestingController`.</b> Desviación
 * consciente del molde de `GrupoForm`: lo que este componente decide no es qué URL
 * pide —eso ya está congelado en `pdc.service.spec.ts`— sino qué estado deriva de cada
 * respuesta, y un doble permite entregar un observable PENDIENTE, que es el escenario
 * de (2) y no se puede montar con un flush.
 *
 * <p>La app es ZONELESS: el DOM repinta en el frame siguiente. Por eso todo caso que
 * lea el DOM tras una respuesta hace `await fixture.whenStable()` antes, igual que
 * `grupo-form.spec.ts`; leerlo en el mismo tick mediría el DOM de antes.
 *
 * <p>El padre tiene id 7, no 1: con 1, un componente que pasara una constante o el id
 * equivocado seguiría verde y (1) no mediría nada. Y su código ('3A') es PREFIJO del
 * código del PDC ('3ADI') a propósito, que es la confusión posible al pintar la ficha:
 * obliga a que los asertos de (4) discriminen uno de otro.
 */

const PADRE: Grupo = { id: 7, codigo: '3A', nivel: '3ESO', tipo: 'ORDINARIO' };
const PDC: Grupo = { id: 42, codigo: '3ADI', nivel: '3ESO', tipo: 'DIVERSIFICACION_PDC' };

/** Un `HttpErrorResponse` como el que el servicio propaga sin transformar. */
function fallo(status: number, cuerpo: unknown = {}): Observable<never> {
  return throwError(() => new HttpErrorResponse({ status, statusText: 'x', error: cuerpo }));
}

describe('PdcDialogo', () => {
  let fixture: ComponentFixture<PdcDialogo>;
  let ref: { close: ReturnType<typeof vi.fn> };
  let dialog: { open: ReturnType<typeof vi.fn> };
  let service: {
    obtener: ReturnType<typeof vi.fn>;
    crear: ReturnType<typeof vi.fn>;
    borrar: ReturnType<typeof vi.fn>;
  };

  /** Monta el componente con la respuesta dada para el `obtener` del `ngOnInit`. */
  function montar(respuesta: Observable<Grupo>): void {
    ref = { close: vi.fn() };
    dialog = { open: vi.fn() };
    service = {
      obtener: vi.fn().mockReturnValue(respuesta),
      crear: vi.fn(),
      borrar: vi.fn(),
    };
    TestBed.configureTestingModule({
      imports: [PdcDialogo],
      providers: [
        { provide: PdcService, useValue: service },
        { provide: Dialog, useValue: dialog },
        { provide: DialogRef, useValue: ref },
        { provide: DIALOG_DATA, useValue: PADRE },
      ],
    });
    fixture = TestBed.createComponent(PdcDialogo);
    fixture.detectChanges(); // dispara ngOnInit → obtener()
  }

  /** Acceso a los miembros protegidos que los casos necesitan tocar. */
  function instancia(): {
    estado: () => string;
    form: { setValue: (v: unknown) => void };
    guardar: () => void;
    borrar: () => void;
  } {
    return fixture.componentInstance as unknown as {
      estado: () => string;
      form: { setValue: (v: unknown) => void };
      guardar: () => void;
      borrar: () => void;
    };
  }

  const raiz = (): HTMLElement => fixture.nativeElement as HTMLElement;

  it('(1) al abrir consulta el PDC con el id DEL PADRE', () => {
    montar(fallo(404));
    // Argumento exacto, no `toHaveBeenCalled()`: el error de uso de este sub-recurso es
    // pasarle el id del PDC en vez del id del padre, y los dos son `number`.
    expect(service.obtener).toHaveBeenCalledWith(7);
    expect(service.obtener).toHaveBeenCalledTimes(1);
  });

  it('(2) mientras la respuesta no llega el estado es cargando y no pinta ni alta ni ficha', () => {
    // Observable PENDIENTE: nunca emite, así que el componente se queda en el estado
    // inicial. Es el caso que protege el parpadeo —si alguien arranca el estado en
    // 'sin-pdc', aquí aparecería el formulario de alta de un grupo que quizá tiene PDC.
    montar(new Subject<Grupo>().asObservable());

    expect(instancia().estado()).toBe('cargando');
    expect(raiz().querySelector('.pdc-dialogo__form')).toBeNull();
    expect(raiz().querySelector('.pdc-dialogo__ficha')).toBeNull();
  });

  it('(3) un 404 lleva a sin-pdc y pinta el formulario de alta', async () => {
    montar(fallo(404));
    await fixture.whenStable();

    expect(instancia().estado()).toBe('sin-pdc');
    expect(raiz().querySelector('.pdc-dialogo__form')).toBeTruthy();
    expect(raiz().querySelector('input')).toBeTruthy();
  });

  it('(4) un 200 lleva a con-pdc y pinta el código del PDC', async () => {
    montar(of(PDC));
    await fixture.whenStable();

    expect(instancia().estado()).toBe('con-pdc');
    const ficha = raiz().querySelector('.pdc-dialogo__ficha')!;
    expect(ficha).toBeTruthy();
    // Igualdad ESTRICTA del valor: '3A' es prefijo de '3ADI', así que un `toContain`
    // quedaría verde si la ficha pintara el código del PADRE en vez del del PDC.
    const valores = [...ficha.querySelectorAll('.pdc-dialogo__valor')]
      .map((dd) => dd.textContent!.trim());
    expect(valores[0]).toBe('3ADI');
    // Y no ofrece el alta: el formulario es de la otra rama.
    expect(raiz().querySelector('.pdc-dialogo__form')).toBeNull();
  });

  it('(5) DISCRIMINANTE: un 500 lleva a error y NO ofrece el formulario de alta', async () => {
    montar(fallo(500));
    await fixture.whenStable();

    expect(instancia().estado()).toBe('error');
    // Lo que este caso protege: quien trate TODO error como "no hay PDC" —discriminando
    // por `err` en vez de por `err.status === 404`— pintaría aquí el alta, y ofrecerla
    // sin saber si el grupo ya tiene PDC lleva a un 400 que el usuario no entiende.
    expect(raiz().querySelector('.pdc-dialogo__form')).toBeNull();
    const err = raiz().querySelector('.pdc-dialogo__error-servidor')!.textContent!;
    expect(err).toContain('No se pudo consultar el PDC de 3A');
    expect(err).toContain('500');
  });

  it('(6) un alta correcta llama a crear con (idPadre, {codigo}) y cierra con true', async () => {
    montar(fallo(404));
    await fixture.whenStable();
    service.crear.mockReturnValue(of(PDC));

    instancia().form.setValue({ codigo: '3ADI' });
    instancia().guardar();
    await fixture.whenStable();

    // Los DOS argumentos, y el cuerpo por igualdad estricta: así cae tanto pasar el id
    // equivocado como colar en el cuerpo un `nivel` o un `tipo` que el backend no lee.
    expect(service.crear).toHaveBeenCalledWith(7, { codigo: '3ADI' });
    expect(ref.close).toHaveBeenCalledWith(true);
  });

  it('(7) un alta con 400 NO cierra el diálogo y pinta el mensaje del backend', async () => {
    montar(fallo(404));
    await fixture.whenStable();
    service.crear.mockReturnValue(
      fallo(400, { message: 'Ya existe un grupo con codigo 3ADI' }));

    instancia().form.setValue({ codigo: '3ADI' });
    instancia().guardar();
    await fixture.whenStable();

    // NO cierra: el código duplicado se arregla escribiendo otro, sin salir.
    expect(ref.close).not.toHaveBeenCalled();
    const err = raiz().querySelector('.pdc-dialogo__error-servidor')!.textContent!;
    expect(err).toContain('Ya existe un grupo con codigo 3ADI');
    expect(err).not.toContain('No se pudo crear el PDC (400)');
    // y sigue en la rama de alta, con el formulario disponible para reintentar
    expect(raiz().querySelector('.pdc-dialogo__form')).toBeTruthy();
  });

  it('(8) un borrado confirmado llama a borrar con el idPadre y cierra con true', async () => {
    montar(of(PDC));
    await fixture.whenStable();
    dialog.open.mockReturnValue({
      closed: { subscribe: (fn: (v: boolean) => void) => fn(true) },
    });
    service.borrar.mockReturnValue(of(undefined));

    instancia().borrar();
    await fixture.whenStable();

    expect(service.borrar).toHaveBeenCalledWith(7);
    expect(ref.close).toHaveBeenCalledWith(true);
  });

  it('(9) un borrado con 409 NO cierra el diálogo y pinta el mensaje', async () => {
    montar(of(PDC));
    await fixture.whenStable();
    dialog.open.mockReturnValue({
      closed: { subscribe: (fn: (v: boolean) => void) => fn(true) },
    });
    service.borrar.mockReturnValue(
      fallo(409, { message: 'No se puede borrar: referenciada por 1 plaza(s)' }));

    instancia().borrar();
    await fixture.whenStable();

    // El 409 es informativo: el usuario debe poder leerlo y salir por su pie.
    expect(ref.close).not.toHaveBeenCalled();
    const err = raiz().querySelector('.pdc-dialogo__error-servidor')!.textContent!;
    expect(err).toContain('referenciada por 1 plaza(s)');
  });
});
