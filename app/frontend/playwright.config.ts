import { defineConfig, devices } from '@playwright/test';

/**
 * Andamiaje e2e (Playwright). Convive con la suite de unidad de vitest sin
 * tocarla: los specs de unidad viven bajo `src/` (los descubre el builder
 * `@angular/build:unit-test`) y los de e2e en `e2e/`, fuera de `src/` y fuera
 * de todo tsconfig del proyecto —`tsconfig.app.json` solo incluye los `.ts` de
 * `src/`, y `tsconfig.spec.json` solo los `.spec.ts` de `src/`—. Playwright
 * transpila sus propios ficheros, así que no necesita entrar en ninguno.
 *
 * Los dos servidores se levantan con `webServer`: Playwright espera a que
 * AMBAS `url` respondan antes de correr ningún test, así que el orden de
 * arranque es indiferente (el frontend sirve HTML sin backend; el proxy de
 * `/api` solo se ejerce en runtime, ya con :8080 arriba).
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 2 : 0,
  reporter: 'list',

  use: {
    baseURL: 'http://localhost:4200',
    trace: 'on-first-retry',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  webServer: [
    {
      // `mvn spring-boot:run` a secas desde la raíz FALLA: el pom de la raíz es
      // el agregador `educhronos-parent` (packaging pom) y el goal se ejecuta
      // primero sobre él → "Unable to find a suitable main class". De ahí el
      // `-pl app`, que lo dirige al único módulo arrancable.
      //
      // Sin perfil `seed`: no se activa ninguno, así que `SeedCatalogoRunner`
      // (@Profile("seed")) no corre y el catálogo queda vacío.
      //
      // BD limpia por construcción: `schema.sql` hace `drop table if exists` de
      // las 21 tablas y las recrea en CADA arranque de contexto
      // (spring.sql.init.mode=always). Con `-pl app` el working dir del proceso
      // es `app/`, así que la BD es `app/educhronos.db` y NO se toca la
      // `educhronos.db` de la raíz.
      command: 'mvn -pl app spring-boot:run',
      cwd: '../..',
      // GET que ya existe y responde 200 con el catálogo vacío (`[]`).
      url: 'http://localhost:8080/api/prevalidacion',
      // Arranque de Spring + carga de la JVM + compilación Maven: generoso.
      timeout: 120_000,
      reuseExistingServer: !process.env['CI'],
      stdout: 'pipe',
      stderr: 'pipe',
    },
    {
      command: 'npm start',
      cwd: '.',
      url: 'http://localhost:4200',
      timeout: 120_000,
      reuseExistingServer: !process.env['CI'],
      stdout: 'pipe',
      stderr: 'pipe',
    },
  ],
});
