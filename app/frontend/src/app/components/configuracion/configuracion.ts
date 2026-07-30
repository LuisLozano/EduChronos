import { Component } from '@angular/core';

import { AsignaturaLista } from '../asignaturas/asignatura-lista';
import { AulaLista } from '../aulas/aula-lista';
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
 * <p>{@link AulaLista} y {@link AsignaturaLista} se montan igual, como HERMANAS de
 * {@link ProfesorLista}: eso es lo que valida el molde de S101 —una sección de catálogo
 * se añade aquí con una línea en `imports:` y una etiqueta en la plantilla, sin tocar
 * las anteriores—.
 *
 * <p>Las CUATRO entidades de O-catálogo son profesor, aula, asignatura y grupo
 * (plan de trabajo, S101: «primer Cambio de cuatro (profesor/aula/asignatura/grupo)»).
 * Con asignatura montada esto va 3/4 y falta solo GRUPO, que entrará del mismo modo.
 * El texto anterior nombraba «currículo» como la cuarta: era un arrastre de S101, y
 * currículo es otra cosa en el plan (códigos y compatibilidades POR currículo,
 * D-F8.5-C3-b), no una sección de este catálogo.
 */
@Component({
  selector: 'app-configuracion',
  imports: [ProfesorLista, AulaLista, AsignaturaLista],
  templateUrl: './configuracion.html',
  styleUrl: './configuracion.css',
})
export class Configuracion {}
