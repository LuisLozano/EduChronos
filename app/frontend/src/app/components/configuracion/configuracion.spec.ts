import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { Configuracion } from './configuracion';
import { ProfesorService } from '../../services/profesor.service';

/**
 * CABLEADO, no comportamiento. Lo que este fichero congela es que la sección de
 * Configuración MONTA de verdad el CRUD de profesores; qué hace ese CRUD se mide
 * en `profesor-lista.spec.ts`, que es donde vive su lógica.
 *
 * <p>`ProfesorService` va DOBLADO por `useValue`: `ProfesorLista` pide la lista en
 * su `ngOnInit`, y sin doble ese GET saldría a `HttpClient` de verdad. El doble
 * emite lista vacía —el camino más corto a un render estable—; el contenido de la
 * tabla no se asevera aquí.
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
      providers: [{ provide: ProfesorService, useValue: { listar: () => of([]) } }],
    }).compileComponents();
  });

  it('(1) monta el CRUD de profesores dentro de la sección', async () => {
    const fixture = TestBed.createComponent(Configuracion);
    await fixture.whenStable();

    const raiz = fixture.nativeElement as HTMLElement;

    expect(raiz.querySelector('app-profesor-lista')).not.toBeNull();
    expect(raiz.textContent).toContain('Nuevo profesor');
  });
});
