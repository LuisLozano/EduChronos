import { expect, test } from '@playwright/test';

/**
 * Humo del andamiaje e2e: la app se sirve y la landing (ruta '') se renderiza.
 * No crea entidades ni dispara el solver; no depende del contenido de la BD.
 *
 * El aserto va sobre el párrafo de intro de `landing.html` ("Elige por dónde
 * empezar."). Es texto REAL de la plantilla y es el único ancla EXCLUSIVA de la
 * landing: los otros dos rótulos de la página, "Configuración" y "Horario",
 * aparecen TAMBIÉN como enlaces del header del shell (`app.html`), así que un
 * `getByRole('link', { name: 'Configuración' })` casaría con dos elementos y
 * violaría el modo estricto —y, peor, pasaría aunque el router no hubiera
 * montado la landing, porque el header lo pinta el shell—.
 */
test('la landing carga y muestra su intro', async ({ page }) => {
  await page.goto('/');

  await expect(page.getByText('Elige por dónde empezar.')).toBeVisible();
});
