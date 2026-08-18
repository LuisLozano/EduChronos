import { expect, test, type Locator, type Page } from '@playwright/test';

/**
 * Guion de aceptación completo por navegador: se crea el centro más pequeño que
 * el solver puede resolver y se comprueba que produce horario. Es el ÚNICO test
 * de este fichero y no debe crecer: R-e2e (gestion_proyecto.md §6) fija que la
 * suite de navegador cubre los ~6 eslabones del guion, uno por eslabón. Ramas de
 * error, validaciones y casos límite pertenecen a vitest o a la capa JVM, que las
 * miden más barato y con mejor diagnóstico.
 *
 * <p><b>El orden de creación es REQUISITO TÉCNICO, no preferencia.</b> Cada
 * diálogo puebla sus desplegables por red AL ABRIRSE (`nuevo()` monta el
 * formulario y este pide su catálogo), así que la dependencia tiene que estar
 * PERSISTIDA antes de abrir el diálogo que la ofrece: nivel antes que grupo,
 * grupo antes que subgrupo, y asignatura/aula/profesor/subgrupo antes que la
 * actividad. Reordenar los pasos no deja el test lento: lo deja con desplegables
 * vacíos y un 400 del backend.
 *
 * <p><b>El paso 1 (jornada) depende del aislamiento de la BD.</b> Con la BD
 * vacía `persistida()` es false y «Guardar jornada» hace el PUT directo, sin
 * abrir `ConfirmarReemplazo`. Eso lo garantiza `playwright.config.ts`, que borra
 * `app/educhronos-e2e.db` en cada corrida. Si alguien reutilizara una BD con
 * jornada guardada, este paso se volvería indeterminado —aparecería un diálogo
 * que el test no espera— y fallaría por una vía que no tiene nada que ver con lo
 * que mide.
 *
 * <p><b>Los botones de alta van por ROL, no por texto.</b> `name` es SUBSTRING
 * por defecto en Playwright 1.62 y el `<h2>` del diálogo repite literalmente el
 * mismo texto que el botón que lo abre («Nuevo nivel», «Nueva aula»…): un
 * `getByText` casaría dos elementos en cuanto el diálogo estuviera abierto.
 *
 * <p><b>«Guardar» se acota al overlay del CDK.</b> Los diálogos cuelgan de
 * `<body>` en `.cdk-overlay-container`, FUERA del árbol del componente que los
 * abre, así que un `page.getByRole` sin acotar los ve a la vez que la página.
 * Con `exact: true` además se separa de «Guardar jornada», que es substring del
 * mismo texto.
 *
 * <p><b>Los campos van por `formControlName`.</b> Es un atributo FUNCIONAL —lo
 * necesita el binding del formulario—, así que sobrevive a O-diseño; una clase
 * BEM es cosmética y puede no hacerlo. La excepción son los `<select multiple>`
 * (ver abajo) y la plaza de la actividad, donde `formControlName="asignatura"`
 * existe DOS veces —la asignatura de la actividad y la de la plaza—: ahí el
 * localizador se acota primero al `<fieldset class="actividad-form__plaza">`.
 *
 * <p><b>PUNTO DÉBIL CONOCIDO: los cuatro `<select multiple>`.</b> No tienen
 * `formControlName` —leen `(change)` → `selectedOptions` → `setValue`—, así que
 * hay que localizarlos por clase (`.subgrupo-form__multiple`,
 * `.actividad-form__profesores`, `.actividad-form__subgrupos`) y moverlos con
 * `selectOption()`, que SÍ dispara `change`. Cualquier alternativa que no lo
 * dispare (un `fill`, un `evaluate` que toque `selected` a mano) dejaría el
 * control del formulario VACÍO EN SILENCIO: la opción se vería marcada en el
 * navegador y el cuerpo del POST iría sin profesores ni subgrupos. Es la
 * fragilidad de este spec y está aquí escrita para que quien la rompa lo sepa.
 *
 * <p><b>La app es ZONELESS</b>: el DOM repinta en el frame siguiente al clic. Por
 * eso todo aserto es con reintento (`expect(...).toHaveCount` / `toBeVisible`) y
 * ninguno lee `locator.count()` a pelo, que capturaría el DOM de antes del
 * repintado.
 */

/** Timeout del conjunto: el solve por UI son 30 s y hay ocho altas antes. */
test.setTimeout(180_000);

/** El diálogo vivo. Es único: el recorrido cierra cada uno antes de abrir el siguiente. */
function dialogo(page: Page): Locator {
  return page.locator('.cdk-overlay-container cdk-dialog-container');
}

/**
 * Abre un diálogo de alta y devuelve su contenedor, ya listo para escribir.
 *
 * <p><b>La espera del FOCO no es decorativa: sin ella el spec es inestable, y está
 * medido.</b> `cdk-dialog-container` entra en el DOM en cuanto el CDK lo crea, pero
 * el contenido del portal —el componente Angular y, con él, los
 * `ControlValueAccessor` de sus campos— se cabla un tick DESPUÉS. Si un `fill`
 * cae en esa ventana, el valor llega al DOM y acto seguido el `writeValue('')` de
 * la directiva al registrarse lo BORRA: el campo queda vacío y `touched`, el
 * `required` bloquea el submit y el diálogo no cierra. Se observó dos veces, en
 * pasos distintos en cada corrida —siempre el PRIMER campo tras abrir, nunca los
 * siguientes—, que es la firma de una carrera y no de un selector equivocado.
 *
 * <p>El foco sirve de barrera porque el CDK lo atrapa (`autoFocus: 'first-tabbable'`)
 * cuando el contenido ya está adjunto, es decir, estrictamente después de que las
 * directivas de formulario estén cableadas. Esperar a `:focus` dentro del diálogo
 * es esperar a que escribir sea seguro. Un `waitForTimeout` haría lo mismo peor: a
 * ojo y sin decir de qué depende.
 */
async function abrir(page: Page, boton: string): Promise<Locator> {
  await page.getByRole('button', { name: boton, exact: true }).click();
  await expect(dialogo(page)).toHaveCount(1);
  await expect(dialogo(page).locator(':focus')).toHaveCount(1);
  return dialogo(page);
}

/**
 * Guarda y espera al CIERRE. El cierre es el acuse de recibo del 2xx: si el
 * backend rechaza, el formulario se queda abierto pintando su
 * `*-form__error-servidor` y este aserto falla AQUÍ, señalando el alta culpable,
 * en vez de dejar que el fallo aflore tres pasos después como un desplegable vacío.
 */
async function guardar(page: Page): Promise<void> {
  await dialogo(page).getByRole('button', { name: 'Guardar', exact: true }).click();
  await expect(dialogo(page)).toHaveCount(0);
}

test('crea un centro mínimo por la UI y el solver produce horario', async ({ page }) => {
  await page.goto('/configuracion');

  // 1. Jornada: se guarda la propuesta por defecto TAL CUAL. El badge «Propuesta ·
  //    sin guardar» solo se pinta con `persistida()` false, así que su desaparición
  //    es la prueba de que el PUT llegó —y de que no se abrió ConfirmarReemplazo—.
  await expect(page.locator('.jornada__badge')).toBeVisible();
  await page.getByRole('button', { name: 'Guardar jornada', exact: true }).click();
  await expect(page.locator('.jornada__badge')).toHaveCount(0);

  // 2. Nivel. El input de orden nace VACÍO (el de duración/repeticiones de la
  //    actividad no: ver paso 8), así que hay que teclearlo o el `required` bloquea.
  const nivel = await abrir(page, 'Nuevo nivel');
  await nivel.locator('[formControlName="codigo"]').fill('1ESO');
  await nivel.locator('[formControlName="orden"]').fill('1');
  await guardar(page);

  // 3. Grupo. `selectOption` espera a que la opción EXISTA, que es justo la espera
  //    que necesita el desplegable poblado por red al abrirse el diálogo.
  const grupo = await abrir(page, 'Nuevo grupo');
  await grupo.locator('[formControlName="codigo"]').fill('1ESOA');
  await grupo.locator('[formControlName="nivel"]').selectOption('1ESO');
  await guardar(page);

  // 4. Subgrupo. Primer `<select multiple>` sin formControlName: por clase.
  const subgrupo = await abrir(page, 'Nuevo subgrupo');
  await subgrupo.locator('[formControlName="codigo"]').fill('1ESOA-TODO');
  await subgrupo.locator('.subgrupo-form__multiple').selectOption(['1ESOA']);
  await guardar(page);

  // 5. Profesor.
  const profesor = await abrir(page, 'Nuevo profesor');
  await profesor.locator('[formControlName="codigo"]').fill('MAT1');
  await profesor.locator('[formControlName="nombreCompleto"]').fill('Profesor de Matemáticas');
  await guardar(page);

  // 6. Asignatura.
  const asignatura = await abrir(page, 'Nueva asignatura');
  await asignatura.locator('[formControlName="codigo"]').fill('MAT');
  await asignatura.locator('[formControlName="nombreCompleto"]').fill('Matemáticas');
  await guardar(page);

  // 7. Aula. El tipo arranca en '' (opción disabled «— elige un tipo —»), así que
  //    hay que seleccionarlo. ORDINARIA es compatible con una asignatura sin
  //    requisito de aula, que es lo que I3 exige para que el solver tenga solución.
  const aula = await abrir(page, 'Nueva aula');
  await aula.locator('[formControlName="codigo"]').fill('A1');
  await aula.locator('[formControlName="tipo"]').selectOption('ORDINARIA');
  await guardar(page);

  // 8. Actividad, la única alta con estructura anidada.
  const actividad = await abrir(page, 'Nueva actividad');
  await actividad.locator('[formControlName="codigo"]').fill('MAT-1ESOA');
  // La asignatura DE LA ACTIVIDAD no se toca: ya vale '' («varias, una por plaza»),
  // que es lo que corresponde cuando cada plaza declara la suya.
  // `duracionTramos` tampoco: ya vale 1.
  await actividad.locator('[formControlName="repeticionesPorSemana"]').fill('3');
  // El value es el literal del enum del modelo (`PATRONES_TEMPORALES`), no un
  // adjetivo: 'DISTRIBUIDA', nunca 'distribuido'.
  await actividad.locator('[formControlName="patronTemporal"]').selectOption('DISTRIBUIDA');

  // La plaza acota los localizadores: `formControlName="asignatura"` vive tanto en
  // la actividad como en cada plaza, y sin este `fieldset` de por medio el
  // localizador casaría dos elementos.
  const plaza = actividad.locator('.actividad-form__plaza').first();
  await plaza.locator('[formControlName="asignatura"]').selectOption('MAT');
  // `modoAula` no se toca: la fábrica de plazas nace en 'FIJA', así que el select de
  // aula fija YA está renderizado y no hay que esperar a ningún repintado del XOR.
  await plaza.locator('[formControlName="aulaFija"]').selectOption('A1');
  await plaza.locator('.actividad-form__profesores').selectOption(['MAT1']);
  await plaza.locator('.actividad-form__subgrupos').selectOption(['1ESOA-TODO']);
  await guardar(page);

  // 9. Al horario por el enlace del header. En /configuracion es ÚNICO; desde la
  //    landing sería ambiguo, porque allí «Horario» aparece además como tarjeta de
  //    la página (es el motivo por el que humo.spec.ts no lo usa como ancla).
  //    El destino es /horario/1, clavado en `app.html`.
  await page.getByRole('link', { name: 'Horario', exact: true }).click();
  await expect(page).toHaveURL(/\/horario\/1$/);

  // PRECONDICIÓN antes de generar: si el centro está mal montado se sabe AQUÍ, sin
  // gastar los 30 s del solve en un fallo que ya estaba decidido.
  await expect(page.locator('.prevalidacion-limpia')).toBeVisible();
  const generar = page.getByRole('button', { name: 'Generar horario', exact: true });
  await expect(generar).toBeEnabled();

  await generar.click();

  // ASERTO PRINCIPAL. Son 3 instancias porque `repeticionesPorSemana` es 3 —tres
  // ActividadInstancia— y hay UNA plaza. El timeout cubre el solve por UI: el POST
  // va con cuerpo `{}`, así que el backend aplica los defaults de
  // `GeneradorHorarioService`, 30 s de límite. El código de la actividad
  // (MAT-1ESOA) NO se pinta en la rejilla: por eso el ancla es `.instancia` y no su
  // texto.
  await expect(page.locator('.instancia')).toHaveCount(3, { timeout: 45_000 });

  // El diálogo de confirmación NO debe haberse abierto: la pre-validación devolvió
  // [] y esa es su única condición de apertura. Si aparece, el centro está mal
  // montado y el test debe morir aquí, no atravesarlo confirmando a ciegas.
  await expect(page.locator('.confirmar-generacion')).toHaveCount(0);

  // Contenido de la primera instancia: los cuatro campos que la rejilla pinta por
  // sesión. Sin esto, tres celdas vacías contarían igual que tres sesiones reales.
  const primera = page.locator('.instancia').first();
  await expect(primera.locator('.asig')).toHaveText('MAT');
  await expect(primera.locator('.prof')).toHaveText('MAT1');
  await expect(primera.locator('.aula')).toHaveText('A1');
  await expect(primera.locator('.grupos')).toHaveText('1ESOA');
});
