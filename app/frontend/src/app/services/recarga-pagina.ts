import { Injectable } from '@angular/core';

/**
 * Recarga la aplicación entera en el inicio (O-curso, S160, C-selector-curso).
 *
 * <p><b>Por qué una recarga completa y no una navegación de Angular.</b> Cambiar de curso
 * cambia la BASE DE DATOS que hay debajo: el catálogo, la jornada, los horarios y sus ids
 * son otros. Todo el estado del cliente vive en signals de componente —medido en el M2 de
 * S160: cero `BehaviorSubject`, cero signals en los diecisiete servicios, cero
 * `localStorage`—, así que no hay caché que invalidar; lo que sí queda es lo PINTADO en ese
 * instante y las rutas con id, como el `/horario/N` que la vista resolvió en la base vieja,
 * que tras el cambio apunta a un horario distinto o a ninguno. Un `router.navigate` dejaría
 * viva la pantalla anterior y sus datos; una recarga en `/` no deja nada.
 *
 * <p><b>Por qué es un servicio y no `window.location.assign` a pelo.</b> Para que los specs
 * puedan comprobar que se llama —y que NO se llama cuando la operación falla— sin recargar
 * el runner de pruebas, que es lo que haría inservible al fichero entero. Es el mínimo
 * envoltorio que hace observable un efecto del navegador; no abstrae nada más.
 */
@Injectable({ providedIn: 'root' })
export class RecargaPagina {
  /**
   * Va al inicio recargando. `assign` y no `reload`: tras el cambio hay que SALIR de donde
   * se estuviera —una ruta con id de la base vieja— y no repetirla.
   */
  recargarEnInicio(): void {
    window.location.assign('/');
  }
}
