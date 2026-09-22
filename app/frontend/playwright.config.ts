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
  // EN SERIE, y no por velocidad. Cambiar de curso cambia la base de TODO el backend
  // (`BaseConmutable`), así que dos specs en paralelo se pisan siempre.
  //
  // INVARIANTE DE ORDEN: duplicar archiva la base de arranque y no hay vía para
  // desarchivarla, así que `curso.spec.ts` debe correr DESPUÉS de todo spec que escriba.
  // Lo garantiza el orden alfabético de ficheros, que es el mecanismo que documenta
  // Playwright sin paralelismo. Si aparece un segundo spec que cambie de curso, esto deja
  // de bastar: hará falta un backend por spec (S162, D-e2e-aislamiento).
  fullyParallel: false,
  workers: 1,
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
      // BD propia y limpia, garantizada por el `rm -rf` de este command —NO por
      // el esquema—. Desde S109 `schema.sql` no demuele nada: son 22
      // `create table if not exists` y cero DROP (su propia cabecera lo
      // documenta), así que es idempotente pero conserva las filas que ya
      // hubiera. Lo único que deja la BD a cero es borrarla antes de arrancar.
      //
      // Carpeta propia, `app/target/e2e/`, con ruta ABSOLUTA (`$PWD` es la raíz
      // del repo, por el `cwd`), porque el directorio de trabajo de la JVM no está
      // fijado en el repo. Se borra entera en cada corrida, con los
      // `-journal`/`-wal`/`-shm` que pudiera haber dejado una corrida abortada.
      // Tiene que ser PROPIA porque el listado de cursos (`/api/cursos`) muestra la
      // carpeta de la base abierta: así no aparece `app/educhronos.db`, y los
      // `curso-*.db` que crea el duplicado desaparecen con ella (S162).
      //
      // La base del e2e es, pues, `app/target/e2e/educhronos-e2e.db`. La URL
      // explícita también la separa de la base de desarrollo: desde S153 un
      // arranque SIN argumento no usa `app/`,
      // sino la carpeta de datos del usuario (`$XDG_DATA_HOME/educhronos`, o
      // `~/.local/share/educhronos`). Por eso el e2e la pasa: no hereda dónde
      // resuelva la aplicación por defecto.
      command:
        'rm -rf "$PWD/app/target/e2e" && mkdir -p "$PWD/app/target/e2e" && mvn -pl app spring-boot:run -Dspring-boot.run.arguments=--spring.datasource.url=jdbc:sqlite:$PWD/app/target/e2e/educhronos-e2e.db',
      cwd: '../..',
      // GET que ya existe y responde 200 con el catálogo vacío (`[]`).
      url: 'http://localhost:8080/api/prevalidacion',
      // Arranque de Spring + carga de la JVM + compilación Maven: generoso.
      timeout: 120_000,
      // Nunca reutilizar: si se enganchara a un backend de desarrollo ya
      // escuchando en :8080, el e2e correría contra la base de ESE backend —hoy
      // la de la carpeta de datos del usuario— y el aislamiento no serviría de
      // nada. Consecuencia asumida: para correr el e2e hay que tener parado el
      // backend de dev, o Playwright aborta por puerto ocupado.
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
