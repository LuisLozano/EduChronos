import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { Observable, Subject, of, throwError } from 'rxjs';
import { TutoriaDialogo } from './tutoria-dialogo';
import { TutoriaService } from '../../services/tutoria.service';
import { ProfesorService } from '../../services/profesor.service';
import { Grupo } from '../../models/grupo.model';
import { Profesor } from '../../models/profesor.model';
import { Tutoria } from '../../models/tutoria.model';

/**
 * Congela el diálogo de tutoría: la derivación de sus TRES estados a partir de las DOS
 * respuestas de red (1)-(6), la forma del desplegable (7) y las cuatro caras del
 * guardado (8)-(12). Secuencia propia desde (1) en este fichero (D-S101-num).
 *
 * <p><b>Los servicios van MOCKEADOS, no por `HttpTestingController`.</b> Mismo criterio
 * que `pdc-dialogo.spec.ts`: lo que este componente decide no es qué URL pide —eso ya
 * está congelado en `tutoria.service.spec.ts`— sino qué estado deriva de cada respuesta
 * y qué CUERPO compone al guardar. Además, un doble permite entregar un observable
 * PENDIENTE, que es el escenario de (2) y (3) y no se puede montar con un flush.
 *
 * <p>La app es ZONELESS: el DOM repinta en el frame siguiente. Todo caso que lea el DOM
 * tras una respuesta hace `await fixture.whenStable()` antes.
 *
 * <p><b>Los fixtures están elegidos para que el verde no pueda ser falso.</b> El grupo
 * tiene id 7 y no 1, así que un componente que pasara una constante caería en (1). El
 * tutor principal existente es el SEGUNDO profesor de la lista, así que su
 * `selectedIndex` es 2 —la opción «sin tutor» ocupa el 0— y un `<select>` que se
 * quedara en su primera opción no daría verde en (4). Y el `id` de cada profesor (11,
 * 12, 13) es DISTINTO de su código y no es prefijo de nada, para que (7) distinga un
 * `[value]="p.codigo"` de un `[value]="p.id"`.
 */

const GRUPO: Grupo = { id: 7, codigo: '3A', nivel: '3ESO', tipo: 'ORDINARIO' };

const PROFESORES: Profesor[] = [
  { id: 11, codigo: 'MAT1', nombreCompleto: 'Ana Matemáticas' },
  { id: 12, codigo: 'LEN2', nombreCompleto: 'Luis Lengua' },
  { id: 13, codigo: 'FIS3', nombreCompleto: 'Eva Física' },
];

/** El principal existente: el SEGUNDO de la lista. Ver la nota de cabecera. */
const PRINCIPAL: Tutoria = { profesor: 'LEN2', rol: 'TUTOR_PRINCIPAL' };
const CO_TUTOR: Tutoria = { profesor: 'FIS3', rol: 'CO_TUTOR' };

/** Un `HttpErrorResponse` como el que el servicio propaga sin transformar. */
function fallo(status: number, cuerpo: unknown = {}): Observable<never> {
  return throwError(() => new HttpErrorResponse({ status, statusText: 'x', error: cuerpo }));
}

describe('TutoriaDialogo', () => {
  let fixture: ComponentFixture<TutoriaDialogo>;
  let ref: { close: ReturnType<typeof vi.fn> };
  let service: {
    obtener: ReturnType<typeof vi.fn>;
    reemplazar: ReturnType<typeof vi.fn>;
  };
  let profesores: { listar: ReturnType<typeof vi.fn> };

  /**
   * Monta el componente con las dos respuestas del `ngOnInit`. Los dos parámetros son
   * observables y no valores para poder entregar uno PENDIENTE y el otro resuelto, que
   * es lo que miden (2) y (3).
   */
  function montar(
    tutoria: Observable<Tutoria[]>,
    listado: Observable<Profesor[]> = of(PROFESORES),
  ): void {
    ref = { close: vi.fn() };
    service = {
      obtener: vi.fn().mockReturnValue(tutoria),
      reemplazar: vi.fn(),
    };
    profesores = { listar: vi.fn().mockReturnValue(listado) };
    TestBed.configureTestingModule({
      imports: [TutoriaDialogo],
      providers: [
        { provide: TutoriaService, useValue: service },
        { provide: ProfesorService, useValue: profesores },
        { provide: DialogRef, useValue: ref },
        { provide: DIALOG_DATA, useValue: GRUPO },
      ],
    });
    fixture = TestBed.createComponent(TutoriaDialogo);
    fixture.detectChanges(); // dispara ngOnInit → forkJoin
  }

  /** Acceso a los miembros protegidos que los casos necesitan tocar. */
  function instancia(): {
    estado: () => string;
    form: { setValue: (v: unknown) => void };
    guardar: () => void;
    cerrar: () => void;
  } {
    return fixture.componentInstance as unknown as {
      estado: () => string;
      form: { setValue: (v: unknown) => void };
      guardar: () => void;
      cerrar: () => void;
    };
  }

  const raiz = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const select = (): HTMLSelectElement | null => raiz().querySelector('select');

  it('(1) al abrir pide la tutoría con el id DEL GRUPO y lista el profesorado', () => {
    montar(of([]));
    // Argumento exacto, no `toHaveBeenCalled()`: el error de uso de este sub-recurso es
    // pasar el id de otra entidad, y todos son `number`.
    expect(service.obtener).toHaveBeenCalledWith(7);
    expect(service.obtener).toHaveBeenCalledTimes(1);
    expect(profesores.listar).toHaveBeenCalledTimes(1);
  });

  it('(2) mientras la TUTORÍA no llega el estado es cargando y no pinta el formulario', () => {
    montar(new Subject<Tutoria[]>().asObservable());

    expect(instancia().estado()).toBe('cargando');
    expect(raiz().querySelector('.tutoria-dialogo__form')).toBeNull();
    expect(select()).toBeNull();
  });

  it('(3) DISCRIMINANTE: con la tutoría ya resuelta pero SIN profesorado sigue cargando', async () => {
    // Este es el caso que protege el gating de las DOS fuentes. Quien pinte el
    // formulario en cuanto llega la tutoría —sin esperar a los profesores— dejaría al
    // usuario un desplegable con la opción «sin tutor» y nada más, afirmando que el
    // grupo no tiene tutor mientras las opciones aún viajan.
    montar(of([PRINCIPAL]), new Subject<Profesor[]>().asObservable());
    await fixture.whenStable();

    expect(instancia().estado()).toBe('cargando');
    expect(raiz().querySelector('.tutoria-dialogo__form')).toBeNull();
    expect(select()).toBeNull();
  });

  it('(4) con tutor principal existente pinta el formulario y lo deja PRESELECCIONADO', async () => {
    montar(of([PRINCIPAL]));
    await fixture.whenStable();

    expect(instancia().estado()).toBe('cargado');
    expect(raiz().querySelector('.tutoria-dialogo__form')).toBeTruthy();
    // 'LEN2' es el SEGUNDO profesor, así que su índice es 2 con la opción «sin tutor»
    // en el 0: con el primero, un select que se quedara en su opción inicial daría
    // verde falso.
    expect(select()!.value).toBe('LEN2');
    expect(select()!.selectedIndex).toBe(2);
  });

  it('(5) DISCRIMINANTE: una tutoría VACÍA es "cargado" con el select en "", no un error', async () => {
    // El GET responde 200 con [] cuando el grupo no tiene tutoría. Quien derive el
    // vacío de un `err.status === 404` —copiando `PdcDialogo` sin leer el contrato—
    // mandaría este caso al estado 'error' y el usuario no podría asignar tutor a
    // ningún grupo que aún no lo tenga, que es el caso MÁS COMÚN de esta pantalla.
    montar(of([]));
    await fixture.whenStable();

    expect(instancia().estado()).toBe('cargado');
    expect(raiz().querySelector('.tutoria-dialogo__form')).toBeTruthy();
    expect(select()!.value).toBe('');
    expect(select()!.selectedIndex).toBe(0);
    // y no hay bloque de co-tutores que pintar
    expect(raiz().querySelector('.tutoria-dialogo__cotutores')).toBeNull();
  });

  it('(6) un 500 en la carga lleva a error y NO ofrece el formulario', async () => {
    montar(fallo(500));
    await fixture.whenStable();

    expect(instancia().estado()).toBe('error');
    expect(raiz().querySelector('.tutoria-dialogo__form')).toBeNull();
    const err = raiz().querySelector('.tutoria-dialogo__error-servidor')!.textContent!;
    expect(err).toContain('No se pudo consultar la tutoría de 3A');
    expect(err).toContain('500');
  });

  it('(7) el desplegable usa el CÓDIGO del profesor como value, nunca su id', async () => {
    montar(of([]));
    await fixture.whenStable();

    const opciones = [...select()!.options];
    // Los profesores MÁS la opción «sin tutor»: TRES profesores y no uno, porque con
    // uno solo un `@for` roto que pintara únicamente el primero quedaría verde.
    expect(opciones.length).toBe(PROFESORES.length + 1);
    expect(opciones[0].value).toBe('');
    expect(opciones[0].textContent!.trim()).toBe('— sin tutor —');
    // El value es el código. Si alguien pone `[value]="p.id"` esto pasa a ser '11', y
    // ese texto viajaría al backend como código de profesor y moriría con un 404.
    expect(opciones[1].value).toBe('MAT1');
    expect(opciones[2].value).toBe('LEN2');
    // Red explícita contra el id: ningún value puede ser el id serializado.
    expect(opciones.map((o) => o.value)).not.toContain('11');
  });

  it('(8) guardar con un principal nuevo manda el PUT con ese principal y cierra con true', async () => {
    montar(of([]));
    await fixture.whenStable();
    service.reemplazar.mockReturnValue(of([{ profesor: 'MAT1', rol: 'TUTOR_PRINCIPAL' }]));

    instancia().form.setValue({ principal: 'MAT1' });
    instancia().guardar();
    await fixture.whenStable();

    expect(service.reemplazar).toHaveBeenCalledWith(7, [
      { profesor: 'MAT1', rol: 'TUTOR_PRINCIPAL' },
    ]);
    expect(ref.close).toHaveBeenCalledWith(true);
  });

  it('(9) DISCRIMINANTE: cambiar el principal CONSERVA los co-tutores en el cuerpo', async () => {
    // El PUT es un reemplazo total: lo que no se reenvía se borra. Quien mande solo
    // `[principal]` borraría el co-tutor sin decírselo a nadie, y el usuario no lo
    // vería hasta volver a abrir el diálogo. La igualdad es sobre el ARRAY ENTERO, no
    // sobre su longitud: así cae también quien reenvíe el co-tutor con el rol cambiado.
    montar(of([PRINCIPAL, CO_TUTOR]));
    await fixture.whenStable();
    service.reemplazar.mockReturnValue(of([]));

    instancia().form.setValue({ principal: 'MAT1' });
    instancia().guardar();
    await fixture.whenStable();

    expect(service.reemplazar).toHaveBeenCalledWith(7, [
      { profesor: 'MAT1', rol: 'TUTOR_PRINCIPAL' },
      { profesor: 'FIS3', rol: 'CO_TUTOR' },
    ]);
  });

  it('(10) quitar el principal habiendo co-tutores manda SOLO los co-tutores', async () => {
    // La otra mitad de (9): sin principal el cuerpo no queda vacío, porque los
    // co-tutores siguen ahí. Un `[]` aquí borraría de paso una tutoría que esta
    // pantalla ni siquiera ofrece editar.
    montar(of([PRINCIPAL, CO_TUTOR]));
    await fixture.whenStable();
    service.reemplazar.mockReturnValue(of([]));

    instancia().form.setValue({ principal: '' });
    instancia().guardar();
    await fixture.whenStable();

    expect(service.reemplazar).toHaveBeenCalledWith(7, [
      { profesor: 'FIS3', rol: 'CO_TUTOR' },
    ]);
  });

  it('(11) quitar el tutor sin co-tutores manda [] : es como se borra la tutoría', async () => {
    // No hay DELETE en el sub-recurso, así que este PUT con lista vacía es la única
    // forma de dejar el grupo sin tutoría.
    montar(of([PRINCIPAL]));
    await fixture.whenStable();
    service.reemplazar.mockReturnValue(of([]));

    instancia().form.setValue({ principal: '' });
    instancia().guardar();
    await fixture.whenStable();

    expect(service.reemplazar).toHaveBeenCalledWith(7, []);
    expect(ref.close).toHaveBeenCalledWith(true);
  });

  it('(12) un 404 EN ESCRITURA pinta el mensaje del backend y NO cierra el diálogo', async () => {
    // El 404 de escritura es contraintuitivo —un código de profesor inexistente da 404,
    // no 400—, y si el componente lo tratara como "no encontrado, nada que hacer" el
    // usuario se quedaría con el diálogo cerrado sin saber qué pasó.
    montar(of([]));
    await fixture.whenStable();
    service.reemplazar.mockReturnValue(
      fallo(404, { message: 'No existe profesor con codigo MAT9' }));

    instancia().form.setValue({ principal: 'MAT1' });
    instancia().guardar();
    await fixture.whenStable();

    expect(ref.close).not.toHaveBeenCalled();
    const err = raiz().querySelector('.tutoria-dialogo__error-servidor')!.textContent!;
    expect(err).toContain('No existe profesor con codigo MAT9');
    expect(err).not.toContain('No se pudo guardar la tutoría (404)');
    // y sigue en el formulario, disponible para reintentar
    expect(raiz().querySelector('.tutoria-dialogo__form')).toBeTruthy();
  });

  it('(13) cancelar cierra SIN true y sin escribir nada', async () => {
    // Caso añadido tras una mutación SUPERVIVIENTE en la campaña de M3/S114: con
    // `cerrar()` cambiado a `close(true)` los doce casos anteriores seguían verdes.
    // El contrato de cierre es asimétrico y solo `true` significa "hubo escritura":
    // el consumidor recarga con `=== true` estricto, así que un cancelar que cerrara
    // con `true` provocaría una recarga fantasma y, peor, mentiría sobre si se
    // escribió. El aserto es sobre el ARGUMENTO, no sobre que se haya llamado.
    montar(of([PRINCIPAL]));
    await fixture.whenStable();

    instancia().cerrar();

    expect(ref.close).toHaveBeenCalledWith();
    expect(ref.close).not.toHaveBeenCalledWith(true);
    expect(service.reemplazar).not.toHaveBeenCalled();
  });
});
