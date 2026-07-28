import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Página de inicio del shell (O-shell). Presentacional puro: NO habla con
 * ningún servicio ni con el backend. Ofrece los dos accesos de primer nivel de
 * la aplicación —Configuración y Horario— como enlaces de router.
 *
 * <p>La ruta de Horario apunta a `horario/1` por convención de arranque (el
 * primer horario sembrado); cuando O-catálogo/O-demo creen horarios reales, el
 * destino se derivará del centro activo, no será fijo. Configuración enlaza al
 * placeholder de esta misma fase, que O-catálogo rellenará.
 */
@Component({
  selector: 'app-landing',
  imports: [RouterLink],
  templateUrl: './landing.html',
  styleUrl: './landing.css',
})
export class Landing {}
