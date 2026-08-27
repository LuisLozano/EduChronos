import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, Routes, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';

import { Configuracion } from './configuracion';
import { ProfesorLista } from '../profesores/profesor-lista';
import { JornadaService } from '../../services/jornada.service';
import { ProfesorService } from '../../services/profesor.service';
import { routes } from '../../app.routes';

/**
 * ENRUTADO, no composición. Lo que este fichero congela desde S123 (C-rutas-hijas) es el
 * criterio 2 de O-navegación: que Configuración es un índice más `<router-outlet>` con
 * una URL por destino, y que ese índice SE DERIVA de `app.routes.ts`.
 *
 * <p>QUÉ DEJÓ DE SER SU RESPONSABILIDAD. Los ocho casos anteriores aseveraban que la
 * sección MONTABA cada una de las ocho listas, porque las componía como hermanas en su
 * plantilla. Ya no las compone: las monta el router, y qué hace cada lista se mide en su
 * propio spec (`profesor-lista.spec.ts`, `jornada.spec.ts`…). Reponer aquí un caso por
 * destino recrearía justo el acoplamiento que este Cambio deshace: ocho razones para
 * tocar este fichero cada vez que se añade una sección.
 *
 * <p>LO QUE PROTEGE CADA CASO:
 * <ul>
 *   <li>(1) la entrada por defecto: `/configuracion` no es una página en blanco, redirige
 *       a `jornada`, que es la primera por el orden de alta;
 *   <li>(2) el índice pinta los OCHO rótulos de `data.titulo`, en el orden declarado, y
 *       cada uno enlaza a su segmento. Es el aserto que se pondría rojo si alguien
 *       reordenara `children` sin querer;
 *   <li>(3) y (4) LA DERIVACIÓN, que es la razón de ser de este spec. Ver abajo;
 *   <li>(5) navegar a un destino cambia la URL y lo monta en el outlet: el cableado
 *       router→outlet, una vez y con la configuración real;
 *   <li>(6) `routerLinkActive` marca la entrada del destino actual y SOLO esa.
 * </ul>
 *
 * <p><b>POR QUÉ EXISTEN (3) Y (4), QUE ES LO MENOS OBVIO.</b> El índice podría pintarse
 * con un array de destinos escrito a mano en el componente, y los casos (1), (2), (5) y
 * (6) seguirían VERDES con esa implementación: los rótulos coincidirían porque alguien
 * los habría copiado bien. Lo que ese diseño rompe no es el render de hoy, es la promesa
 * de que un noveno destino sea UNA entrada nueva en `app.routes.ts` y no dos ediciones
 * que se desincronizan. Por eso (3) configura en el TestBed un conjunto de rutas hijas
 * DISTINTO del real —tres destinos inventados, con rótulos que no existen en el
 * producto— y exige que el índice pinte ESE conjunto. Un array fijo pintaría los ocho de
 * siempre y (3) se pondría rojo mientras todos los demás siguen verdes. (4) cubre el
 * otro extremo de la misma derivación: la ruta de redirección, que no tiene `titulo`, no
 * debe generar entrada de índice.
 *
 * <p>SOBRE LA NOTA DE S101/S102 QUE ESTE SPEC HEREDA. El spec viejo razonaba que hacían
 * falta DOS asertos por caso —el tag y un texto del hijo— porque quitar un componente
 * del array `imports:` dejaría el tag en el DOM como elemento desconocido y el
 * `querySelector` seguiría verde. S102 lo corrigió por mutación: en esta versión de
 * Angular esa mutación NO llega al test, porque el compilador de plantillas la rechaza
 * con NG8001 y no hay build. Ese hecho SIGUE SIENDO CIERTO y sigue mandando aquí, solo
 * que sobre otras piezas: `Configuracion` importa hoy `RouterOutlet`, `RouterLink` y
 * `RouterLinkActive`, y quitar cualquiera de las tres rompe el build igual. Por eso este
 * spec no gasta ningún caso en vigilar el array `imports:`: esa mutación no es
 * observable desde un test, la caza el compilador. El doble aserto que aquella nota
 * justificaba ya no aplica, y no se conserva.
 *
 * <p>INSTRUMENTAL. `RouterTestingHarness` (`@angular/router/testing`, API pública en
 * 21.2) en vez de `TestBed.createComponent`: `Configuracion` inyecta `ActivatedRoute` y
 * lee su `routeConfig`, así que fuera de una navegación real no hay nada que leer —es la
 * causa del NG0201 con el que los ocho casos viejos murieron—. `provideRouter(routes)`
 * con las rutas REALES sigue el precedente de `app.spec.ts`. De los ocho dobles de
 * servicio del spec viejo quedan DOS, los de los únicos destinos que alguna navegación
 * de este fichero llega a montar; el resto sobra porque el router no los instancia.
 */
describe('sección de configuración', () => {
  /** Rótulos y segmentos que `app.routes.ts` declara, en su orden de alta. */
  const DESTINOS: ReadonlyArray<readonly [string, string]> = [
    ['Jornada', 'jornada'],
    ['Profesores', 'profesores'],
    ['Aulas', 'aulas'],
    ['Asignaturas', 'asignaturas'],
    ['Niveles', 'niveles'],
    ['Grupos', 'grupos'],
    ['Subgrupos', 'subgrupos'],
    ['Actividades', 'actividades'],
  ];

  /** Los rótulos que pinta el índice, sin espacios de plantilla. */
  function rotulos(raiz: HTMLElement): string[] {
    return Array.from(raiz.querySelectorAll('.configuracion__destino')).map(
      (a) => a.textContent!.trim(),
    );
  }

  describe('sobre la configuración de rutas REAL', () => {
    beforeEach(() => {
      TestBed.configureTestingModule({
        providers: [
          provideRouter(routes),
          // Solo los dos destinos que las navegaciones de este fichero montan: la
          // jornada (a la que redirige `/configuracion`) y profesores (el destino de
          // (5) y (6)). Los otros seis no se instancian, y doblarlos sería atrezo.
          { provide: JornadaService, useValue: { obtener: () => of({ persistida: true, tramos: [] }) } },
          { provide: ProfesorService, useValue: { listar: () => of([]) } },
        ],
      });
    });

    it('(1) /configuracion redirige a /configuracion/jornada', async () => {
      await RouterTestingHarness.create('/configuracion');

      expect(TestBed.inject(Router).url).toBe('/configuracion/jornada');
    });

    it('(2) el índice pinta los ocho destinos, con su rótulo y en el orden de las rutas', async () => {
      const harness = await RouterTestingHarness.create('/configuracion');
      const raiz = harness.routeNativeElement!;

      expect(rotulos(raiz)).toEqual(DESTINOS.map(([titulo]) => titulo));

      // Y cada entrada enlaza a SU segmento: sin esto, ocho rótulos correctos podrían
      // apuntar todos al mismo sitio.
      const hrefs = Array.from(raiz.querySelectorAll('.configuracion__destino')).map((a) =>
        a.getAttribute('href'),
      );
      expect(hrefs).toEqual(DESTINOS.map(([, ruta]) => `/configuracion/${ruta}`));
    });

    it('(5) navegar a un destino cambia la URL y lo monta en el outlet', async () => {
      const harness = await RouterTestingHarness.create('/configuracion/jornada');
      expect(harness.routeNativeElement!.querySelector('app-profesor-lista')).toBeNull();

      await harness.navigateByUrl('/configuracion/profesores', Configuracion);

      // El destino REAL, no un doble: lo que se mide es que la configuración de
      // producción navega y activa el componente que declara, con su servicio doblado
      // para que el `ngOnInit` de la lista no salga a la red.
      expect(TestBed.inject(Router).url).toBe('/configuracion/profesores');
      expect(
        harness.routeNativeElement!.querySelector('.configuracion__panel app-profesor-lista'),
      ).not.toBeNull();
      expect(TestBed.inject(Router).routerState.root.firstChild?.firstChild?.component).toBe(
        ProfesorLista,
      );
    });

    it('(6) routerLinkActive marca la entrada del destino actual, y solo esa', async () => {
      const harness = await RouterTestingHarness.create('/configuracion/subgrupos');
      const raiz = harness.routeNativeElement!;

      const activos = Array.from(raiz.querySelectorAll('.configuracion__destino--activo')).map(
        (a) => a.textContent!.trim(),
      );

      expect(activos).toEqual(['Subgrupos']);
    });
  });

  /**
   * El conjunto de rutas de estos dos casos NO es el de producción a propósito: son tres
   * destinos inventados cuyos rótulos no existen en `app.routes.ts`. Si el índice
   * dejara de derivarse y volviera a un array escrito en el componente, aquí se verían
   * los ocho de siempre en vez de estos tres, y (3) caería.
   */
  describe('derivación del índice desde las rutas', () => {
    @Component({ selector: 'app-destino-alfa', template: 'alfa' })
    class DestinoAlfa {}

    @Component({ selector: 'app-destino-beta', template: 'beta' })
    class DestinoBeta {}

    @Component({ selector: 'app-destino-gamma', template: 'gamma' })
    class DestinoGamma {}

    /** Destinos de prueba, no listas reales: aquí no se mide ninguna lista. */
    const RUTAS_INVENTADAS: Routes = [
      {
        path: 'configuracion',
        component: Configuracion,
        children: [
          { path: '', redirectTo: 'alfa', pathMatch: 'full' },
          { path: 'alfa', component: DestinoAlfa, data: { titulo: 'Alfa' } },
          { path: 'beta', component: DestinoBeta, data: { titulo: 'Beta' } },
          { path: 'gamma', component: DestinoGamma, data: { titulo: 'Gamma' } },
        ],
      },
    ];

    beforeEach(() => {
      TestBed.configureTestingModule({ providers: [provideRouter(RUTAS_INVENTADAS)] });
    });

    it('(3) el índice pinta los destinos que declaran las rutas, no una lista propia', async () => {
      const harness = await RouterTestingHarness.create('/configuracion');
      const raiz = harness.routeNativeElement!;

      expect(rotulos(raiz)).toEqual(['Alfa', 'Beta', 'Gamma']);
      // Explícito, porque es la mutación que este caso existe para cazar: si el
      // componente volviera a un array fijo, aquí aparecerían los rótulos reales.
      expect(rotulos(raiz)).not.toContain('Jornada');
    });

    it('(4) la ruta de redirección por defecto no genera entrada de índice', async () => {
      const harness = await RouterTestingHarness.create('/configuracion');

      // Cuatro hijas configuradas, tres entradas: la de `path: ''` es la entrada por
      // defecto, no un destino, y no tiene `titulo` que pintar.
      expect(rotulos(harness.routeNativeElement!)).toHaveLength(3);
      expect(TestBed.inject(Router).url).toBe('/configuracion/alfa');
    });
  });
});
