import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';

import type { HorarioView } from './horario-view';
import { sinGeneracionEnCurso } from './sin-generacion-en-curso';

/**
 * Spec propio de la guarda (S184): es una función pura sobre la vista, así que se prueba
 * con un doble que sólo contesta `generandoHorario()`. Que la vista conteste bien lo mide
 * su propio spec.
 */
describe('guarda sinGeneracionEnCurso', () => {
  function decide(generando: boolean): unknown {
    const vista = { generandoHorario: () => generando } as unknown as HorarioView;
    return sinGeneracionEnCurso(
      vista,
      {} as ActivatedRouteSnapshot,
      {} as RouterStateSnapshot,
      {} as RouterStateSnapshot,
    );
  }

  it('(1) con una generación en vuelo, no deja salir', () => {
    expect(decide(true)).toBe(false);
  });

  it('(2) sin generación, deja salir', () => {
    expect(decide(false)).toBe(true);
  });
});
