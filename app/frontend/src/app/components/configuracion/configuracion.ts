import { Component } from '@angular/core';

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
 * <p>{@link AulaLista} se monta igual, como HERMANA de {@link ProfesorLista}: eso es
 * lo que valida el molde de S101 —una sección de catálogo se añade aquí con una
 * línea en `imports:` y una etiqueta en la plantilla, sin tocar la anterior—. Las dos
 * que faltan (grupos, currículo) entrarán del mismo modo.
 */
@Component({
  selector: 'app-configuracion',
  imports: [ProfesorLista, AulaLista],
  templateUrl: './configuracion.html',
  styleUrl: './configuracion.css',
})
export class Configuracion {}
