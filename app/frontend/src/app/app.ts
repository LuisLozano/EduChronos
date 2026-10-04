import { Component, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';

import { CursoBarra } from './components/curso-barra/curso-barra';
import { VersionLinea } from './components/version-linea/version-linea';

/**
 * El shell: barra de aplicación y salida del router.
 *
 * <p>Desde S160 la barra monta {@link CursoBarra} en el hueco que S127 reservó, y con ella
 * entra la PRIMERA llamada HTTP del shell: hasta ahora el shell no hablaba con el backend y
 * sus specs no necesitaban un `HttpClient`. Los de `app.spec.ts` lo proveen desde entonces.
 *
 * <p>Desde S193 la barra monta también {@link VersionLinea} junto a la marca: segunda llamada
 * del shell, `GET /api/version`.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CursoBarra, VersionLinea],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('educhronos-ui');
}
