import { Component } from '@angular/core';

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
 * <p>Las otras tres entidades de catálogo (aulas, grupos, currículo) se montarán
 * igual, como hermanas de {@link ProfesorLista}.
 */
@Component({
  selector: 'app-configuracion',
  imports: [ProfesorLista],
  templateUrl: './configuracion.html',
  styleUrl: './configuracion.css',
})
export class Configuracion {}
