import { Routes } from '@angular/router';

import { Landing } from './components/landing/landing';
import { Configuracion } from './components/configuracion/configuracion';
import { HorarioView } from './components/horario-view/horario-view';

import { Jornada } from './components/jornada/jornada';
import { ProfesorLista } from './components/profesores/profesor-lista';
import { AulaLista } from './components/aulas/aula-lista';
import { AsignaturaLista } from './components/asignaturas/asignatura-lista';
import { NivelLista } from './components/niveles/nivel-lista';
import { GrupoLista } from './components/grupos/grupo-lista';
import { SubgrupoLista } from './components/subgrupos/subgrupo-lista';
import { ActividadLista } from './components/actividades/actividad-lista';

/**
 * Rutas de la aplicación. Importación ESTÁTICA en los ocho destinos hijos, igual que
 * en las tres rutas de primer nivel: `loadComponent` encaja con `D-bundle-presupuesto`
 * pero su beneficio hay que medirlo antes de prometerlo, y no se ha medido (D12, S122).
 *
 * <p>El RÓTULO de cada destino vive en su `data.titulo` y no en un array aparte: el
 * índice de {@link Configuracion} se deriva de esta misma configuración, así que un
 * noveno destino es UNA entrada aquí y no dos ediciones que se pueden desincronizar.
 *
 * <p>El ORDEN es el de alta, heredado del que la plantilla única argumentaba: la
 * jornada primero porque es el marco sobre el que se apoya todo, y las actividades al
 * final porque referencian por código a las cuatro entidades anteriores. Ver el javadoc
 * de `configuracion.ts`.
 */
export const routes: Routes = [
  { path: '', component: Landing },
  {
    path: 'configuracion',
    component: Configuracion,
    children: [
      { path: '', redirectTo: 'jornada', pathMatch: 'full' },
      { path: 'jornada', component: Jornada, data: { titulo: 'Jornada' } },
      { path: 'profesores', component: ProfesorLista, data: { titulo: 'Profesores' } },
      { path: 'aulas', component: AulaLista, data: { titulo: 'Aulas' } },
      { path: 'asignaturas', component: AsignaturaLista, data: { titulo: 'Asignaturas' } },
      { path: 'niveles', component: NivelLista, data: { titulo: 'Niveles' } },
      { path: 'grupos', component: GrupoLista, data: { titulo: 'Grupos' } },
      { path: 'subgrupos', component: SubgrupoLista, data: { titulo: 'Subgrupos' } },
      { path: 'actividades', component: ActividadLista, data: { titulo: 'Actividades' } },
    ],
  },
  { path: 'horario/:id', component: HorarioView },
];
