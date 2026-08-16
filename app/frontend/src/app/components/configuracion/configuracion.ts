import { Component } from '@angular/core';

import { AsignaturaLista } from '../asignaturas/asignatura-lista';
import { AulaLista } from '../aulas/aula-lista';
import { GrupoLista } from '../grupos/grupo-lista';
import { SubgrupoLista } from '../subgrupos/subgrupo-lista';
import { ActividadLista } from '../actividades/actividad-lista';
import { Jornada } from '../jornada/jornada';
import { ProfesorLista } from '../profesores/profesor-lista';

/**
 * Sección de Configuración del centro (O-shell, poblada por O-catálogo). Sigue
 * siendo presentacional pura y SIN servicios: no carga ni escribe nada por sí
 * misma, solo compone las secciones de catálogo, y cada una habla con su propio
 * servicio. Por eso el CRUD de profesores va INLINE aquí y no como ruta hija:
 * no hay estado que anidar ni URL que enlazar, y evitarlo ahorra el
 * `<router-outlet>` y el array `children` que el proyecto no usa en ningún sitio
 * salvo el raíz.
 *
 * <p>{@link AulaLista}, {@link AsignaturaLista} y {@link GrupoLista} se montan igual,
 * como HERMANAS de {@link ProfesorLista}: eso es lo que valida el molde de S101 —una
 * sección de catálogo se añade aquí con una línea en `imports:` y una etiqueta en la
 * plantilla, sin tocar las anteriores—.
 *
 * <p>Las CUATRO entidades de O-catálogo son profesor, aula, asignatura y grupo
 * (plan de trabajo, S101: «primer Cambio de cuatro (profesor/aula/asignatura/grupo)»).
 * Con grupo montado esto va 4/4: O-catálogo queda COMPLETO y el párrafo de pendientes
 * desaparece de la plantilla. El texto anterior nombraba «currículo» como la cuarta:
 * era un arrastre de S101, y currículo es otra cosa en el plan (códigos y
 * compatibilidades POR currículo, D-F8.5-C3-b), no una sección de este catálogo.
 *
 * <p>{@link Jornada} (C-jornada M4) se monta igual —una línea en `imports:` y una
 * etiqueta— pese a NO ser una sección de catálogo: es un singleton sin lista ni alta,
 * de O-estructura. Va PRIMERA porque la malla horaria es el marco sobre el que se
 * apoya todo lo demás, y porque su badge de «propuesta · sin guardar» avisa de que el
 * centro no ha configurado la jornada antes de que el usuario baje a las entidades.
 * Que encaje aquí sin adaptar nada confirma que el patrón de composición de esta
 * sección no depende del molde CRUD.
 *
 * <p>{@link ActividadLista} (O-estructura) se monta ÚLTIMA y con el mismo gesto. Va
 * detrás de subgrupos porque una actividad referencia por código a asignaturas,
 * profesores, aulas y subgrupos: el orden de la página sigue al orden en que hay que
 * darlos de alta, y quien baje hasta aquí ya tiene arriba todo lo que el formulario le
 * va a pedir.
 */
@Component({
  selector: 'app-configuracion',
  imports: [
    Jornada,
    ProfesorLista,
    AulaLista,
    AsignaturaLista,
    GrupoLista,
    SubgrupoLista,
    ActividadLista,
  ],
  templateUrl: './configuracion.html',
  styleUrl: './configuracion.css',
})
export class Configuracion {}
