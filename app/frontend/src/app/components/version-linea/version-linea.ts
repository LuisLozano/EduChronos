import { Component, OnInit, inject, signal } from '@angular/core';

import { VersionService } from '../../services/version.service';

/** Lo que se enseña cuando el GET falla: se dice que no se sabe, no se inventa una versión. */
const NO_DISPONIBLE = 'versión no disponible';

/**
 * La versión de la aplicación en la barra, junto a la marca (C-version-y-rastro, condición 2,
 * S193). Es lo que cierra `D-version-invisible`: una incidencia del profesor tiene que poder
 * decir qué versión tiene delante.
 *
 * <p>Patrón de {@code CursoBarra}: una sola petición al iniciarse y ninguna después. El valor
 * se pinta TAL CUAL llega, sin ramas: `0.0.0-dev` o `desconocida` son tan informativos como
 * una versión de tag, y esconderlos haría creer que la línea no existe.
 *
 * <p><b>Un fallo del GET no rompe la barra.</b> Se enseña «versión no disponible»: la versión
 * es información, no un requisito para usar el programa.
 */
@Component({
  selector: 'app-version-linea',
  templateUrl: './version-linea.html',
  styleUrl: './version-linea.css',
})
export class VersionLinea implements OnInit {
  private readonly service = inject(VersionService);

  /** Lo que se lee en la barra; vacío mientras carga. */
  protected readonly texto = signal('');

  ngOnInit(): void {
    this.service.obtener().subscribe({
      next: (dto) => this.texto.set(`versión ${dto.version}`),
      error: () => this.texto.set(NO_DISPONIBLE),
    });
  }
}
