import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

/** Una entrada del índice, derivada de una ruta hija. Solo rótulo y segmento. */
interface Destino {
  readonly ruta: string;
  readonly titulo: string;
}

/**
 * Sección de Configuración del centro (O-shell, poblada por O-catálogo). Sigue siendo
 * presentacional pura y SIN servicios de dominio: no carga ni escribe nada por sí misma;
 * cada destino habla con su propio servicio. Lo que cambia en S123 (C-rutas-hijas) es
 * CÓMO se llega a cada uno: un índice vertical más `<router-outlet>` con ocho rutas
 * hijas, en vez de las ocho listas compuestas como hermanas en una sola pantalla.
 *
 * <p>LA RAZÓN VIEJA ERA BUENA, Y SE APOYABA EN DOS HECHOS QUE YA NO SE DAN. Mientras las
 * ocho listas cabían en una pantalla, anidar rutas no compraba nada: no había estado que
 * anidar —este componente era la clase vacía que sigue siendo casi— ni URL que enlazar,
 * porque el destino de un enlace habría sido siempre la misma página, y evitarlo ahorraba
 * el `<router-outlet>` y el array `children` que el proyecto no usaba en ningún sitio
 * salvo el raíz. Con un centro de juguete eso era cierto.
 *
 * <p>DEJA DE SERLO POR DOS MEDIDAS, no por gusto. La primera es el TAMAÑO del centro
 * real: 334 subgrupos y 208 actividades (`docs/diseno-navegacion.md`). Ocho listas de ese
 * tamaño apiladas en un documento no son una pantalla, son un scroll en el que las
 * actividades quedan a varias pantallas de la jornada y no hay forma de ir a una sección
 * salvo desplazarse hasta ella. La segunda es la exigencia de O-navegación: una URL
 * ENLAZABLE por destino (`/configuracion/subgrupos`), que es la que hace que el botón
 * Atrás funcione, que un enlace pueda mandarse, y que recargar deje al usuario donde
 * estaba. Ninguna de las dos se resuelve componiendo hermanas, y las dos son exactamente
 * lo que `children` resuelve.
 *
 * <p>El coste que la razón vieja temía no se ha materializado: no había estado compartido
 * entre las ocho —cada lista carga el suyo en su `ngOnInit`—, así que montar por ruta no
 * pierde nada. Y gana un refresco que antes faltaba: dar de alta un PDC desde grupos
 * dejaba rancia la lista de subgrupos, porque nadie la avisaba; ahora entrar en subgrupos
 * la remonta y la recarga.
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
 *
 * <p>La sección monta además {@link NivelLista} desde S111, y eso NO amplía O-catálogo a
 * cinco entidades: el CRUD de niveles es un Cambio de O-estructura, no de O-catálogo, que
 * quedó cerrado en S106 con su censo de cuatro. Se monta aquí, y no en otra pantalla,
 * porque comparte forma (molde plano de catálogo) y porque el orden de alta manda: un
 * grupo necesita un nivel existente, así que la lista de niveles va antes que la de
 * grupos. El motivo de que exista es que sin niveles creables por UI no hay grupos, ni
 * subgrupos, ni población para las plazas del currículo.
 *
 * <p>Los dos párrafos anteriores sobreviven al Cambio sin tocarse porque su argumento no
 * era sobre la plantilla sino sobre el ORDEN, y el orden sigue mandando: es el del índice
 * y el del array `children` de `app.routes.ts`, que lo declara una sola vez.
 */
@Component({
  selector: 'app-configuracion',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './configuracion.html',
  styleUrl: './configuracion.css',
})
export class Configuracion {
  private readonly ruta = inject(ActivatedRoute);

  /**
   * El índice, DERIVADO de las rutas hijas de esta misma ruta: rótulo de `data.titulo`
   * y segmento de `path`. No hay array de destinos que mantener en paralelo, así que un
   * destino nuevo se añade en `app.routes.ts` y aparece aquí solo.
   *
   * <p>Se lee de `routeConfig` y no de `children` observables porque la configuración es
   * estática: no cambia mientras la vista vive, y un `signal` sugeriría lo contrario.
   * El filtro descarta la redirección `path: ''` → `jornada`, que no es un destino sino
   * la entrada por defecto: no tiene `titulo` y no debe pintar entrada de índice.
   */
  protected readonly destinos: readonly Destino[] = (this.ruta.routeConfig?.children ?? [])
    .filter((hija) => !!hija.path && typeof hija.data?.['titulo'] === 'string')
    .map((hija) => ({ ruta: hija.path!, titulo: hija.data!['titulo'] as string }));
}
