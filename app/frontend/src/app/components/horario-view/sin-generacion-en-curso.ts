import { CanDeactivateFn } from '@angular/router';

import type { HorarioView } from './horario-view';

/**
 * No se sale de la vista de horario mientras genera (S184, condición 3 de O-pre-demo).
 * Es la mitad del bloqueo que el diálogo modal no cubre: el modal tapa la pantalla y el
 * teclado, pero la navegación de la aplicación —un enlace de la cabecera, el «atrás» del
 * navegador— la decide el router, y es aquí donde se frena. Es la PRIMERA guarda de ruta
 * del proyecto; vive junto a la vista que protege porque no hay convención previa.
 */
export const sinGeneracionEnCurso: CanDeactivateFn<HorarioView> = (vista) =>
  !vista.generandoHorario();
