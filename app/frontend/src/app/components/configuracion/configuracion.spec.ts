import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { Configuracion } from './configuracion';
import { AsignaturaService } from '../../services/asignatura.service';
import { AulaService } from '../../services/aula.service';
import { GrupoService } from '../../services/grupo.service';
import { JornadaService } from '../../services/jornada.service';
import { ProfesorService } from '../../services/profesor.service';

/**
 * CABLEADO, no comportamiento. Lo que este fichero congela es que la sección de
 * Configuración MONTA de verdad cada CRUD de catálogo; qué hace cada uno se mide en
 * `profesor-lista.spec.ts` / `aula-lista.spec.ts`, que es donde vive su lógica.
 *
 * <p>Los servicios van DOBLADOS por `useValue`: cada lista pide la suya en su
 * `ngOnInit`, y sin doble esos GET saldrían a `HttpClient` de verdad. Los dobles
 * emiten lista vacía —el camino más corto a un render estable—; el contenido de las
 * tablas no se asevera aquí.
 *
 * <p>UN CASO POR SECCIÓN, no uno que las mire todas: así el rojo NOMBRA la sección
 * desmontada en vez de obligar a leer el aserto para saber cuál cayó.
 *
 * <p>POR QUÉ DOS ASERTOS Y NO SOLO EL `querySelector`. Son dos mutaciones
 * distintas y solo el segundo aserto caza la segunda:
 *
 * <ul>
 *   <li>quitar `<app-profesor-lista />` de `configuracion.html` → el elemento
 *       desaparece del DOM y caen LOS DOS;
 *   <li>quitar `ProfesorLista` del array `imports:` de `configuracion.ts` → Angular
 *       deja el tag en el DOM como ELEMENTO DESCONOCIDO, sin instanciar el
 *       componente. `querySelector('app-profesor-lista')` SIGUE devolviéndolo y ese
 *       aserto quedaría verde con el cableado roto. Solo el texto que pinta el hijo
 *       —el botón «Nuevo profesor», que únicamente existe si el componente se
 *       instanció— pone rojo esa mutación.
 * </ul>
 *
 * <p>El texto elegido es del HIJO, no de `configuracion.html`: un aserto sobre
 * «Configuración» o sobre el párrafo de pendientes mediría la plantilla propia y
 * no distinguiría el hijo montado del hijo ausente.
 */
describe('sección de configuración', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Configuracion],
      providers: [
        { provide: ProfesorService, useValue: { listar: () => of([]) } },
        { provide: AulaService, useValue: { listar: () => of([]) } },
        { provide: AsignaturaService, useValue: { listar: () => of([]) } },
        { provide: GrupoService, useValue: { listar: () => of([]) } },
        // Jornada no es un CRUD: su doble expone `obtener`, no `listar`. Malla vacía
        // y persistida=true (el badge de propuesta se mide en jornada.spec.ts).
        {
          provide: JornadaService,
          useValue: { obtener: () => of({ persistida: true, tramos: [] }) },
        },
      ],
    }).compileComponents();
  });

  it('(1) monta el CRUD de profesores dentro de la sección', async () => {
    const fixture = TestBed.createComponent(Configuracion);
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;

    expect(raiz.querySelector('app-profesor-lista')).not.toBeNull();
    expect(raiz.textContent).toContain('Nuevo profesor');
  });

  it('(2) monta el CRUD de aulas dentro de la sección', async () => {
    const fixture = TestBed.createComponent(Configuracion);
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;

    expect(raiz.querySelector('app-aula-lista')).not.toBeNull();
    // El segundo aserto mide que el hijo RENDERIZA, no solo que el tag está: el
    // botón «Nueva aula» solo existe si el componente se instanció y pintó.
    //
    // MATIZ sobre la nota de (1), verificado por mutación en S102: en esta versión
    // de Angular quitar `AulaLista` del array `imports:` NO deja el tag como
    // elemento desconocido con los tests verdes —el compilador de plantillas lo
    // rechaza con NG8001 y NO HAY BUILD—. Esa mutación cae antes de llegar aquí,
    // así que el aserto de texto se sostiene por la razón de arriba (el hijo pinta),
    // no por la que el spec de S101 le atribuía.
    expect(raiz.textContent).toContain('Nueva aula');
  });

  it('(3) monta el CRUD de asignaturas dentro de la sección', async () => {
    const fixture = TestBed.createComponent(Configuracion);
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;

    expect(raiz.querySelector('app-asignatura-lista')).not.toBeNull();
    // Los dos asertos valen aquí por lo que (2) ya razona, sin repetirlo: el texto
    // mide que el hijo RENDERIZA, y la mutación de `imports:` no llega a este spec
    // porque revienta antes en build (NG8001).
    expect(raiz.textContent).toContain('Nueva asignatura');
  });

  it('(4) monta el CRUD de grupos dentro de la sección', async () => {
    const fixture = TestBed.createComponent(Configuracion);
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;

    expect(raiz.querySelector('app-grupo-lista')).not.toBeNull();
    // Cuarta y última sección de O-catálogo. Solo se dobla `GrupoService`: el que
    // pide niveles es `GrupoForm`, que vive en un diálogo y no se instancia al
    // montar la sección.
    expect(raiz.textContent).toContain('Nuevo grupo');
  });

  it('(5) monta la jornada dentro de la sección', async () => {
    const fixture = TestBed.createComponent(Configuracion);
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;

    // NO es una sección de catálogo (singleton de O-estructura), pero se monta con el
    // mismo gesto que las cuatro anteriores: eso es lo que este caso congela.
    expect(raiz.querySelector('app-jornada')).not.toBeNull();
    expect(raiz.textContent).toContain('Guardar jornada');
  });
});
