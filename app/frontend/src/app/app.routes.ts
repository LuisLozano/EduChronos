import { Routes } from '@angular/router';

import { Landing } from './components/landing/landing';
import { Configuracion } from './components/configuracion/configuracion';
import { HorarioView } from './components/horario-view/horario-view';

export const routes: Routes = [
  { path: '', component: Landing },
  { path: 'configuracion', component: Configuracion },
  { path: 'horario/:id', component: HorarioView },
];
