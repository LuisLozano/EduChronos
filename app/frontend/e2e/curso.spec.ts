import { expect, test, type Locator, type Page } from '@playwright/test';

// Condición 8 de O-curso (S162): duplicar, cambiar de curso y ver rechazada una
// escritura sobre el curso archivado. No genera horario (decisión C, S158).
//
// Parte de la base del e2e, que no tiene nombre de curso: el diálogo pide el
// nombre del actual junto al del nuevo (condición 6).
//
// ORDEN: este spec archiva la base de arranque y no la deja como la encontró.
// Debe correr después de todo spec que escriba; ver playwright.config.ts.
//
// Sin reintentos: un segundo intento encontraría la base ya archivada y el
// curso 2031/2032 ya creado, y fallaría por una causa que no es la real.
test.describe.configure({ retries: 0 });

const ACTUAL = '2030/2031';
const NUEVO = '2031/2032';
const FICHERO_ARRANQUE = 'educhronos-e2e.db';

async function abrirCursos(page: Page): Promise<void> {
  const boton = page.getByRole('button', { name: 'Cursos…', exact: true });
  await expect(boton).toBeEnabled();
  await boton.click();
}

// Alta de nivel: calcada de centro-minimo.spec.ts (`irA`, `abrir` y el paso 2). Llega
// a Niveles por el índice de Configuración, espera el FOCO dentro del diálogo antes de
// escribir —sin esa espera el primer `fill` puede borrarlo el `writeValue` de la
// directiva— y acota campos y «Guardar» al overlay del CDK. No espera el cierre: en el
// curso archivado el diálogo tiene que quedarse abierto, y eso lo asevera quien llama.

/** El diálogo vivo, como en centro-minimo.spec.ts. */
function dialogo(page: Page): Locator {
  return page.locator('.cdk-overlay-container cdk-dialog-container');
}

async function intentarAltaNivel(page: Page, codigo: string, orden: string): Promise<void> {
  await page.goto('/configuracion');
  await expect(page).toHaveURL(/\/configuracion\/jornada$/);
  await page.getByRole('link', { name: 'Niveles', exact: true }).click();
  await expect(page).toHaveURL(/\/configuracion\/niveles$/);
  await expect(page.locator('.configuracion__panel app-nivel-lista')).toHaveCount(1);

  await page.getByRole('button', { name: 'Nuevo nivel', exact: true }).click();
  await expect(dialogo(page)).toHaveCount(1);
  await expect(dialogo(page).locator(':focus')).toHaveCount(1);

  await dialogo(page).locator('[formControlName="codigo"]').fill(codigo);
  await dialogo(page).locator('[formControlName="orden"]').fill(orden);
  await dialogo(page).getByRole('button', { name: 'Guardar', exact: true }).click();
}

test('duplicar, cambiar de curso y ver rechazada la escritura en el archivado', async ({ page }) => {
  test.setTimeout(90_000);
  const nombre = page.locator('.curso-barra__nombre');
  const marca = page.locator('.curso-barra__marca');

  // 1. Duplicar desde una base sin nombre.
  await page.goto('/');
  await abrirCursos(page);
  await page.getByRole('button', { name: 'Duplicar curso…', exact: true }).click();
  const actual = page.getByLabel('Nombre del curso actual', { exact: true });
  const nuevo = page.getByLabel('Curso nuevo', { exact: true });
  await actual.fill(ACTUAL);
  await nuevo.fill(NUEVO);
  // El formulario se monta al cambiar de modo: se comprueba que lo tecleado sigue ahí
  // antes de enviar, por la misma carrera de `writeValue` que documenta centro-minimo.
  await expect(actual).toHaveValue(ACTUAL);
  await expect(nuevo).toHaveValue(NUEVO);
  await page.getByRole('button', { name: 'Duplicar', exact: true }).click();

  // Duplicar abre el curso nuevo, que está activo.
  await expect(nombre).toContainText(NUEVO);
  await expect(marca).not.toBeVisible();

  // 2. Control positivo: en el curso activo el alta SE GUARDA. Sin esto, un
  //    formulario roto daría el mismo error que la guarda y el paso 4 pasaría
  //    por una razón equivocada. El cierre es el acuse del 2xx, como en centro-minimo.
  await intentarAltaNivel(page, 'E2EA', '1');
  await expect(dialogo(page)).toHaveCount(0);
  await expect(page.getByText('E2EA', { exact: true })).toBeVisible();

  // 3. Cambiar al archivado: la fila se localiza por su fichero, porque todos
  //    los botones de fila se llaman «Abrir».
  await abrirCursos(page);
  await page
    .locator('li.cursos-dialogo__fila', { hasText: FICHERO_ARRANQUE })
    .getByRole('button', { name: 'Abrir', exact: true })
    .click();
  await expect(nombre).toContainText(ACTUAL);
  await expect(marca).toBeVisible();
  await expect(marca).toHaveText('Solo lectura');

  // 4. La escritura se rechaza, el mensaje de la guarda llega a la interfaz y
  //    el diálogo sigue abierto.
  await intentarAltaNivel(page, 'E2EB', '2');
  await expect(dialogo(page).locator('.nivel-form__error-servidor')).toContainText(
    'está archivado y es de solo lectura',
  );
  await expect(dialogo(page).locator('[formControlName="codigo"]')).toBeVisible();
});
