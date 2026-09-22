import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Página de inicio del shell (O-shell). Presentacional puro: NO habla con
 * ningún servicio ni con el backend. Ofrece los dos accesos de primer nivel de
 * la aplicación —Configuración y Horario— como enlaces de router.
 *
 * <p>La ruta de Horario apunta a `/horario`, sin id: es la vista la que resuelve el
 * horario vigente del curso abierto (S161). Configuración enlaza al
 * placeholder de esta misma fase, que O-catálogo rellenará.
 */
@Component({
  selector: 'app-landing',
  imports: [RouterLink],
  templateUrl: './landing.html',
  styleUrl: './landing.css',
})
export class Landing {}
