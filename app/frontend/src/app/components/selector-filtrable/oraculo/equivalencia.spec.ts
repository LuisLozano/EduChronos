import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FiltroOpciones } from '../../filtro-opciones/filtro-opciones';
import datos from './datos.json';
import esperado from './esperado.json';

/**
 * Oráculo de equivalencia del filtro de los selectores (S260, C-combobox, condición 2 de
 * O-combobox). `datos.json` son las listas que sirve la API sobre una copia migrada de la base de
 * partida (md5 d867c2de…f7e2), sin retocar; los grupos son el `entidadesDeVista` real de la vista
 * por grupo de su horario. `esperado.json` salió de ejecutar el FiltroOpciones de hoy sobre esos
 * datos y estas búsquedas (s260/c1/f0b/COMO.txt): las búsquedas son fijas y no se añaden ni se
 * quitan después.
 *
 * <p>Cada selector usa su lista y SU función de texto y de código, copiadas literal de lo que hoy
 * pasa a `<app-filtro-opciones>` (s260/t0/06-selectores.txt). S4, S5 y S6 usan la lista completa:
 * lo comparado es el filtro, no el recorte previo de cada formulario.
 */
interface Profesor {
  codigo: string;
  nombreCompleto: string;
}
interface Aula {
  codigo: string;
  tipo: string;
}
interface Asignatura {
  codigo: string;
  nombreCompleto: string | null;
}

interface Selector<T> {
  opciones: readonly T[];
  texto: (o: T) => string;
  codigo: (o: T) => string;
  busquedas: readonly string[];
}

/** Lo esperado de un selector: los códigos visibles, en orden, por búsqueda y con un elegido. */
interface Esperado {
  busquedas: Record<string, string[]>;
  elegido: { codigo: string; busqueda: string; visibles: string[] };
}

// horario-view.ts:87
const identidad = (e: string): string => e;
// grupo-form.ts:83, tutoria-dialogo.ts:94, aulas-asignatura-dialogo.ts:73, actividad-form.ts:313
const codigoDe = (x: { codigo: string }): string => x.codigo;
// tutoria-dialogo.ts:95
const textoTutor = (p: Profesor): string => `${p.codigo} — ${p.nombreCompleto}`;
// actividad-form.ts:320-321
const textoAsignatura = (a: Asignatura): string =>
  a.nombreCompleto?.trim() ? `${a.codigo} — ${a.nombreCompleto}` : a.codigo;
// actividad-form.ts:314
const textoAula = (a: Aula): string => `${a.codigo} (${a.tipo})`;

const BUSCAR_AULAS = ['Taller 4', 'taller', 'aula', 'fpb', 'zzz', ''];
const BUSCAR_ASIGNATURAS = ['LAT', 'Latín', 'latin', 'mús', '4º', 'zzz', ''];
/** La búsqueda del caso «elegido fuera de la búsqueda»; el elegido es la primera opción. */
const BUSCAR_CON_ELEGIDO = 'zzz';

const selector = <T>(s: Selector<T>): Selector<unknown> => s as unknown as Selector<unknown>;

const PROFESORES = datos.profesores as Profesor[];
const AULAS = datos.aulas as Aula[];
const ASIGNATURAS = datos.asignaturas as Asignatura[];

const SELECTORES: Record<string, Selector<unknown>> = {
  S1: selector({ opciones: datos.grupos, texto: identidad, codigo: identidad, busquedas: ['4ºC', '4º C', '1B', 'di', 'zzz', ''] }),
  S2: selector({
    opciones: PROFESORES,
    texto: textoTutor,
    codigo: codigoDe,
    busquedas: ['BYG1', 'byg', 'ORI1', 'ez', 'garcía', 'garcia', 'zzz', ''],
  }),
  S3: selector({ opciones: AULAS, texto: codigoDe, codigo: codigoDe, busquedas: BUSCAR_AULAS }),
  S4: selector({ opciones: AULAS, texto: codigoDe, codigo: codigoDe, busquedas: BUSCAR_AULAS }),
  S5: selector({ opciones: ASIGNATURAS, texto: textoAsignatura, codigo: codigoDe, busquedas: BUSCAR_ASIGNATURAS }),
  S6: selector({ opciones: ASIGNATURAS, texto: textoAsignatura, codigo: codigoDe, busquedas: BUSCAR_ASIGNATURAS }),
  S7: selector({
    opciones: AULAS,
    texto: textoAula,
    codigo: codigoDe,
    busquedas: ['Taller 4', 'taller', 'ordinaria', 'fpb', 'zzz', ''],
  }),
};

const ESPERADO = esperado as unknown as Record<string, Esperado>;

describe('oráculo de equivalencia: FiltroOpciones (hoy)', () => {
  let fixture: ComponentFixture<FiltroOpciones<unknown>>;

  /** Monta el filtro de hoy con un selector, escribe la búsqueda y devuelve los códigos visibles. */
  async function visibles(s: Selector<unknown>, busqueda: string, seleccionados: string[]): Promise<string[]> {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({ imports: [FiltroOpciones] }).compileComponents();
    fixture = TestBed.createComponent(FiltroOpciones) as ComponentFixture<FiltroOpciones<unknown>>;
    fixture.componentRef.setInput('opciones', s.opciones);
    fixture.componentRef.setInput('texto', s.texto);
    fixture.componentRef.setInput('codigo', s.codigo);
    fixture.componentRef.setInput('seleccionados', seleccionados);
    fixture.componentRef.setInput('etiqueta', 'opciones');
    await fixture.whenStable();
    // Con 10 opciones o menos el filtro de hoy no se pinta y no filtra: el oráculo no valdría.
    const campo = fixture.nativeElement.querySelector('input') as HTMLInputElement | null;
    expect(campo).not.toBeNull();
    campo!.value = busqueda;
    campo!.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    return fixture.componentInstance.visibles().map(s.codigo);
  }

  for (const [nombre, s] of Object.entries(SELECTORES)) {
    it(`${nombre}: el esperado tiene exactamente las búsquedas fijas`, () => {
      expect(Object.keys(ESPERADO[nombre]?.busquedas ?? {})).toEqual([...s.busquedas]);
    });

    for (const busqueda of s.busquedas) {
      it(`${nombre} «${busqueda}»`, async () => {
        expect(await visibles(s, busqueda, [])).toEqual(ESPERADO[nombre]?.busquedas[busqueda]);
      });
    }

    it(`${nombre}: el elegido sigue visible aunque la búsqueda no case`, async () => {
      const elegido = s.codigo(s.opciones[0]);
      expect(ESPERADO[nombre]?.elegido.codigo).toBe(elegido);
      expect(ESPERADO[nombre]?.elegido.busqueda).toBe(BUSCAR_CON_ELEGIDO);
      expect(await visibles(s, BUSCAR_CON_ELEGIDO, [elegido])).toEqual(ESPERADO[nombre]?.elegido.visibles);
    });
  }
});
