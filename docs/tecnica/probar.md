# Cómo se prueba

Responde a esta pregunta: qué suites de pruebas tiene Educhronos, cómo se corre cada una y cuántos tests da.
Léelo antes de dar por bueno un cambio, o cuando la CI falle y haya que reproducirlo en local.

Describe el commit 30b8d67a9b49.

Cada bloque de órdenes se corre desde la raíz del repo, en una shell nueva.
Si un bloque necesita otra carpeta, empieza entrando en ella.
Las órdenes de Java, Angular y extremo a extremo son las de la CI, en `.github/workflows/tests.yml`.
Los requisitos de Java, Maven, Node y npm están en `docs/tecnica/construir.md`.

## Java

Corre los tests del solver y de la aplicación (`.github/workflows/tests.yml:23`):

```bash
mvn -B test
```

Da 162 tests en el solver y 965 en la aplicación.
Los tests pesados del solver, marcados con la etiqueta escala, quedan fuera por defecto.
Se corren aparte con el perfil escala, mvn test -Pescala, y tardan unos diez minutos (`solver/pom.xml:71-95`).

## Angular

Los tests de unidad del frontend usan vitest a través de ng test.
La CI los corre en `app/frontend/` (`.github/workflows/tests.yml:31`), primero npm ci y después el test (`.github/workflows/tests.yml:39`, `.github/workflows/tests.yml:41`):

```bash
cd app/frontend
npm ci
npm test -- --watch=false
```

Da 853 tests en 69 ficheros.

## Extremo a extremo

Los tests de extremo a extremo usan Playwright con Chromium contra la aplicación entera.
La CI instala antes el solver en el repositorio local de Maven y compila la aplicación (`.github/workflows/tests.yml:57-73`):

```bash
mvn -B -pl solver -am install -DskipTests
mvn -B -pl app test-compile
cd app/frontend
npm ci
npx playwright install chromium
npm run e2e
```

Da 3 tests.

La CI añade la opción --with-deps al instalar el navegador (`.github/workflows/tests.yml:70`).
En local se usa la orden sin esa opción; así se midió en la máquina de desarrollo.

Playwright levanta dos servidores: el backend en el puerto 8080 y el de Angular en el 4200.
Los dos puertos tienen que estar libres.
El backend no se reutiliza nunca; el de Angular sí, fuera de la CI (`app/frontend/playwright.config.ts:80-85`, `app/frontend/playwright.config.ts:94`).
Así, un backend de desarrollo que esté escuchando hace fallar el arranque, en vez de prestar su base.
El backend del e2e usa una base propia, que borra y crea en cada corrida dentro de la carpeta target de la aplicación (`app/frontend/playwright.config.ts:59-74`).
No toca la carpeta de datos del usuario.

## Python

Las herramientas de `tools/` tienen sus tests con unittest, en una carpeta tests al lado de cada una.
Solo la de `tools/doc-tecnica/` corre en la CI (`.github/workflows/tests.yml:93`); las demás se corren en local.
Todas se lanzan igual, desde la raíz.

El comprobador de rutas de esta documentación, 32 tests:

```bash
python3 -B -m unittest discover -s tools/doc-tecnica/tests -p 'test_*.py'
```

La carga de datos de un centro, 284 tests:

```bash
python3 -B -m unittest discover -s tools/carga-centro/tests -p 'test_*.py'
```

La base de partida, 9 tests:

```bash
python3 -B -m unittest discover -s tools/base-partida/tests -p 'test_*.py'
```

El generador de la referencia del solver, 12 tests:

```bash
python3 -B -m unittest discover -s tools/referencia-solver/tests -p 'test_*.py'
```

Dos de estas suites piden un programa del sistema.
La de carga-centro necesita pdftotext, de poppler, en el PATH (`tools/carga-centro/tests/test_extraer_horario.py:9`).
La del comprobador de rutas necesita git: fabrica repos temporales (`tools/doc-tecnica/tests/test_comprobar_rutas.py:36-38`).

## Rutas de la documentación técnica

El comprobador de `tools/doc-tecnica/comprobar_rutas.py` revisa las rutas de `docs/tecnica/`.
Antes de revisar se prueba a sí mismo, y sale con 0 si no hay fallos.
La CI lo pasa en cada push a main (`.github/workflows/tests.yml:4-5`, `.github/workflows/tests.yml:95`); en local, con la raíz del repo como argumento:

```bash
python3 -B tools/doc-tecnica/comprobar_rutas.py --raiz "$PWD"
```

## Conteo de este commit

| Suite | Tests |
|---|---|
| Java, solver | 162 |
| Java, aplicación | 965 |
| Angular | 853, en 69 ficheros |
| Extremo a extremo | 3 |
| Python, comprobador de rutas | 32 |
| Python, carga de un centro | 284 |
| Python, base de partida | 9 |
| Python, referencia del solver | 12 |

## Fuera de las suites

Tres pruebas no entran en ninguna suite.
Los tests pesados del solver, con el perfil escala, como se dijo arriba.
`tools/manual/capturas.mjs`, que hace las capturas del manual de uso y comprueba sus textos contra la aplicación en marcha.
Y la autoprueba del instalador, que solo corre en Windows; está en `docs/tecnica/publicar.md`.
