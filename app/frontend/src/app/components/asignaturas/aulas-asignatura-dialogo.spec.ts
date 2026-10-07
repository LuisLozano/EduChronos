import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { DialogRef, DIALOG_DATA } from '@angular/cdk/dialog';
import { Observable, Subject, of, throwError } from 'rxjs';
import { AulasAsignaturaDialogo } from './aulas-asignatura-dialogo';
import { AsignaturaAulaService } from '../../services/asignatura-aula.service';
import { AulaService } from '../../services/aula.service';
import { Asignatura } from '../../models/asignatura.model';
import { Aula } from '../../models/aula.model';
import { AsignaturaAula } from '../../models/asignatura-aula.model';

/**
 * Congela el diálogo de las aulas de una asignatura (S206): sus TRES estados a partir de las DOS
 * respuestas de red (1)-(6), añadir y quitar (7)(8), las caras del guardado (9)-(11), los roles
 * mezclados (12), el cierre al cancelar (13) y el filtro del selector (14). Molde de
 * `tutoria-dialogo.spec.ts`: servicios MOCKEADOS, porque lo que decide el componente es qué estado
 * deriva de cada respuesta y qué CUERPO compone, y un doble permite entregar un observable
 * PENDIENTE. La app es zoneless: todo caso que lea el DOM tras una respuesta hace `whenStable()`.
 *
 * <p>La asignatura tiene id 7 y no 1, para que (1) distinga el id de una constante. Las aulas
 * guardadas en (5) son la PRIMERA y la TERCERA del catálogo, para que (7) mida que el selector
 * quita justo esas y no las primeras N.
 */

const ASIGNATURA: Asignatura = { id: 7, codigo: 'ByG', nombreCompleto: 'Biología' };

function aula(id: number, codigo: string): Aula {
  return { id, codigo, tipo: 'ORDINARIA', capacidad: null, edificio: null, planta: null, sector: null };
}

const CATALOGO: Aula[] = [aula(31, 'LAB1'), aula(32, 'A13'), aula(33, 'LAB2')];

const EXCLUSIVAS: AsignaturaAula[] = [
  { aula: 'LAB1', rol: 'EXCLUSIVA' },
  { aula: 'LAB2', rol: 'EXCLUSIVA' },
];

function fallo(status: number, cuerpo: unknown = {}): Observable<never> {
  return throwError(() => new HttpErrorResponse({ status, statusText: 'x', error: cuerpo }));
}

describe('AulasAsignaturaDialogo', () => {
  let fixture: ComponentFixture<AulasAsignaturaDialogo>;
  let ref: { close: ReturnType<typeof vi.fn> };
  let service: { listar: ReturnType<typeof vi.fn>; reemplazar: ReturnType<typeof vi.fn> };
  let aulas: { listar: ReturnType<typeof vi.fn> };

  function montar(
    guardadas: Observable<AsignaturaAula[]>,
    catalogo: Observable<Aula[]> = of(CATALOGO),
  ): void {
    ref = { close: vi.fn() };
    service = { listar: vi.fn().mockReturnValue(guardadas), reemplazar: vi.fn() };
    aulas = { listar: vi.fn().mockReturnValue(catalogo) };
    TestBed.configureTestingModule({
      imports: [AulasAsignaturaDialogo],
      providers: [
        { provide: AsignaturaAulaService, useValue: service },
        { provide: AulaService, useValue: aulas },
        { provide: DialogRef, useValue: ref },
        { provide: DIALOG_DATA, useValue: ASIGNATURA },
      ],
    });
    fixture = TestBed.createComponent(AulasAsignaturaDialogo);
    fixture.detectChanges(); // dispara ngOnInit → forkJoin
  }

  function instancia(): { estado: () => string; guardar: () => void; cancelar: () => void } {
    return fixture.componentInstance as unknown as {
      estado: () => string;
      guardar: () => void;
      cancelar: () => void;
    };
  }

  const raiz = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const filas = (): string[] =>
    Array.from(raiz().querySelectorAll('.aulas-asignatura__fila .dato')).map((e) => e.textContent!.trim());
  const radio = (rol: 'exclusiva' | 'preferida'): HTMLInputElement =>
    raiz().querySelector(`.aulas-asignatura__${rol}`) as HTMLInputElement;
  const selector = (): HTMLSelectElement => raiz().querySelector('.aulas-asignatura__nueva') as HTMLSelectElement;
  const botonGuardar = (): HTMLButtonElement =>
    raiz().querySelector('.aulas-asignatura__guardar') as HTMLButtonElement;

  async function elegirYAnadir(codigo: string): Promise<void> {
    selector().value = codigo;
    selector().dispatchEvent(new Event('change'));
    (raiz().querySelector('.aulas-asignatura__anadir') as HTMLButtonElement).click();
    await fixture.whenStable();
  }

  it('(1) al abrir pide las aulas con el id DE LA ASIGNATURA y el catálogo de aulas', () => {
    montar(of([]));
    expect(service.listar).toHaveBeenCalledWith(7);
    expect(service.listar).toHaveBeenCalledTimes(1);
    expect(aulas.listar).toHaveBeenCalledTimes(1);
  });

  it('(2) mientras el GET no llega el estado es cargando y no hay formulario', () => {
    montar(new Subject<AsignaturaAula[]>().asObservable());
    expect(instancia().estado()).toBe('cargando');
    expect(raiz().querySelector('.aulas-asignatura__form')).toBeNull();
  });

  it('(3) DISCRIMINANTE: con las aulas ya resueltas pero SIN catálogo sigue cargando', async () => {
    montar(of(EXCLUSIVAS), new Subject<Aula[]>().asObservable());
    await fixture.whenStable();
    expect(instancia().estado()).toBe('cargando');
    expect(raiz().querySelector('.aulas-asignatura__form')).toBeNull();
  });

  it('(4) sin aulas guardadas: listo, lista vacía y la marca por defecto es PREFERIDA', async () => {
    montar(of([]));
    await fixture.whenStable();
    expect(instancia().estado()).toBe('listo');
    expect(filas()).toEqual([]);
    expect(raiz().querySelector('.aulas-asignatura__vacia')).toBeTruthy();
    expect(radio('preferida').checked).toBe(true);
    expect(radio('exclusiva').checked).toBe(false);
  });

  it('(5) con dos EXCLUSIVA guardadas las pinta y deja marcada «Solo puede ir a estas aulas»', async () => {
    montar(of(EXCLUSIVAS));
    await fixture.whenStable();
    expect(filas()).toEqual(['LAB1', 'LAB2']);
    expect(radio('exclusiva').checked).toBe(true);
    expect(radio('preferida').checked).toBe(false);
    expect(botonGuardar().disabled).toBe(false);
  });

  it('(6) un 500 en la carga lleva a error, con su mensaje, y NO ofrece el formulario', async () => {
    montar(fallo(500));
    await fixture.whenStable();
    expect(instancia().estado()).toBe('error');
    expect(raiz().querySelector('.aulas-asignatura__form')).toBeNull();
    const err = raiz().querySelector('.aulas-asignatura__error-servidor')!.textContent!;
    expect(err).toContain('No se pudieron consultar las aulas de ByG');
    expect(err).toContain('500');
  });

  it('(7) añadir: el selector ofrece solo las que no están y lo añadido sale de él', async () => {
    montar(of(EXCLUSIVAS));
    await fixture.whenStable();
    const ofrecidas = (): string[] => Array.from(selector().options).map((o) => o.value).filter((v) => v !== '');
    expect(ofrecidas()).toEqual(['A13']);

    await elegirYAnadir('A13');

    expect(filas()).toEqual(['LAB1', 'LAB2', 'A13']);
    expect(ofrecidas()).toEqual([]);
    expect(selector().value).toBe('');
  });

  it('(8) quitar saca el aula de la lista y vuelve a ofrecerla', async () => {
    montar(of(EXCLUSIVAS));
    await fixture.whenStable();
    (raiz().querySelectorAll('.aulas-asignatura__quitar')[0] as HTMLButtonElement).click();
    await fixture.whenStable();

    expect(filas()).toEqual(['LAB2']);
    expect(Array.from(selector().options).map((o) => o.value)).toContain('LAB1');
  });

  it('(9) guardar manda TODAS las filas con la marca elegida y cierra con true', async () => {
    montar(of(EXCLUSIVAS));
    await fixture.whenStable();
    service.reemplazar.mockReturnValue(of([]));
    await elegirYAnadir('A13');
    radio('preferida').click();
    await fixture.whenStable();

    botonGuardar().click();
    await fixture.whenStable();

    expect(service.reemplazar).toHaveBeenCalledWith(7, [
      { aula: 'LAB1', rol: 'PREFERIDA' },
      { aula: 'LAB2', rol: 'PREFERIDA' },
      { aula: 'A13', rol: 'PREFERIDA' },
    ]);
    expect(ref.close).toHaveBeenCalledWith(true);
  });

  it('(10) quitar todas y guardar manda [] y cierra con true: es como se borran', async () => {
    montar(of([{ aula: 'LAB1', rol: 'EXCLUSIVA' }]));
    await fixture.whenStable();
    service.reemplazar.mockReturnValue(of([]));
    (raiz().querySelector('.aulas-asignatura__quitar') as HTMLButtonElement).click();
    await fixture.whenStable();

    botonGuardar().click();
    await fixture.whenStable();

    expect(service.reemplazar).toHaveBeenCalledWith(7, []);
    expect(ref.close).toHaveBeenCalledWith(true);
  });

  it.each([
    [400, 'la asignatura ByG no puede tener a la vez aulas EXCLUSIVA y PREFERIDA'],
    [404, 'No existe asignatura con id 7'],
    [403, 'El curso 2025/2026 está archivado y es de solo lectura.'],
  ])('(11) un %i al guardar pinta el mensaje del backend y NO cierra', async (status, mensaje) => {
    montar(of(EXCLUSIVAS));
    await fixture.whenStable();
    service.reemplazar.mockReturnValue(fallo(status, { message: mensaje }));

    botonGuardar().click();
    await fixture.whenStable();

    expect(ref.close).not.toHaveBeenCalled();
    const err = raiz().querySelector('.aulas-asignatura__error-servidor')!.textContent!;
    expect(err).toContain(mensaje);
    expect(err).not.toContain(`(${status})`);
    expect(raiz().querySelector('.aulas-asignatura__form')).toBeTruthy();
  });

  it('(12) roles mezclados en el GET: lo dice, no ofrece guardar hasta elegir marca, y entonces la aplica a todas', async () => {
    montar(of([
      { aula: 'LAB1', rol: 'EXCLUSIVA' },
      { aula: 'A13', rol: 'PREFERIDA' },
    ]));
    await fixture.whenStable();
    expect(raiz().querySelector('.aulas-asignatura__aviso')!.textContent).toContain('las dos marcas');
    expect(radio('exclusiva').checked).toBe(false);
    expect(radio('preferida').checked).toBe(false);
    expect(botonGuardar().disabled).toBe(true);
    instancia().guardar();
    expect(service.reemplazar).not.toHaveBeenCalled();

    service.reemplazar.mockReturnValue(of([]));
    radio('exclusiva').click();
    await fixture.whenStable();
    expect(raiz().querySelector('.aulas-asignatura__aviso')).toBeNull();
    expect(botonGuardar().disabled).toBe(false);
    botonGuardar().click();
    await fixture.whenStable();

    expect(service.reemplazar).toHaveBeenCalledWith(7, [
      { aula: 'LAB1', rol: 'EXCLUSIVA' },
      { aula: 'A13', rol: 'EXCLUSIVA' },
    ]);
  });

  it('(13) cancelar cierra con false, nunca con true, y sin escribir nada', async () => {
    montar(of(EXCLUSIVAS));
    await fixture.whenStable();
    (raiz().querySelector('.aulas-asignatura__cancelar') as HTMLButtonElement).click();

    expect(ref.close).toHaveBeenCalledWith(false);
    expect(ref.close).not.toHaveBeenCalledWith(true);
    expect(service.reemplazar).not.toHaveBeenCalled();
  });

  it('(14) con más de diez aulas el selector de añadir tiene filtro, y filtra', async () => {
    const doce = Array.from({ length: 12 }, (_, i) => aula(100 + i, `A${String(i + 1).padStart(2, '0')}`));
    montar(of([]), of(doce));
    await fixture.whenStable();
    const campo = raiz().querySelector<HTMLInputElement>('input[aria-label="Filtrar aulas"]');
    expect(campo).not.toBeNull();
    campo!.value = 'A1';
    campo!.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    expect(Array.from(selector().options).map((o) => o.value).filter((v) => v !== ''))
      .toEqual(['A10', 'A11', 'A12']);
  });
});
