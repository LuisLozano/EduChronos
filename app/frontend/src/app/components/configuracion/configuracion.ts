import { Component } from '@angular/core';

/**
 * Sección de Configuración del centro (O-shell). Placeholder deliberado: el
 * shell necesita una ruta a la que navegar y desde la que volver para cumplir
 * su criterio de terminado, pero el contenido real —los formularios CRUD de
 * catálogo— es trabajo de O-catálogo, que rellenará este componente.
 *
 * <p>Presentacional puro, sin servicios. No representa deuda: es la sede donde
 * O-catálogo montará los formularios, no un cuerpo que haya que rehacer.
 */
@Component({
  selector: 'app-configuracion',
  templateUrl: './configuracion.html',
  styleUrl: './configuracion.css',
})
export class Configuracion {}
