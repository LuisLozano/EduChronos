import { Routes } from '@angular/router';

import { HorarioView } from './components/horario-view/horario-view';

export const routes: Routes = [
  { path: '', redirectTo: 'horario/1', pathMatch: 'full' },
  { path: 'horario/:id', component: HorarioView },
];
