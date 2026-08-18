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
      // BD propia y limpia, garantizada por el `rm -f` de este command —NO por
      // el esquema—. Desde S109 `schema.sql` no demuele nada: son 21
      // `create table if not exists` y cero DROP (su propia cabecera lo
      // documenta), así que es idempotente pero conserva las filas que ya
      // hubiera. Lo único que deja la BD a cero es borrarla antes de arrancar.
      // El `*` del `rm` arrastra los `-journal`/`-wal`/`-shm` que pudiera haber
      // dejado una corrida abortada.
      //
      // Con `-pl app` el working dir del proceso es `app/`, así que la BD del
      // e2e es `app/educhronos-e2e.db`, separada de `app/educhronos.db` (la BD
      // de trabajo del desarrollador, que el e2e no debe tocar).
      command:
        'rm -f app/educhronos-e2e.db* && mvn -pl app spring-boot:run -Dspring-boot.run.arguments=--spring.datasource.url=jdbc:sqlite:educhronos-e2e.db',
      cwd: '../..',
      // GET que ya existe y responde 200 con el catálogo vacío (`[]`).
      url: 'http://localhost:8080/api/prevalidacion',
      // Arranque de Spring + carga de la JVM + compilación Maven: generoso.
      timeout: 120_000,
      // Nunca reutilizar: si se enganchara a un backend de desarrollo ya
      // escuchando en :8080, el e2e correría contra `app/educhronos.db` y el
      // aislamiento no serviría de nada. Consecuencia asumida: para correr el
      // e2e hay que tener parado el backend de dev, o Playwright aborta por
      // puerto ocupado.
      reuseExistingServer: false,
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
