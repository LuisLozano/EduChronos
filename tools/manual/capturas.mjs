// Capturas del manual de uso, con comprobación de sus textos (S242, C-alcance-manual, O-manual).
//
// Uso: node tools/manual/capturas.mjs <JAR> <VERSION_ESPERADA> <carpeta de capturas> <manual.md>
//
// 1. Copia la base de partida a una carpeta de datos nueva y arranca el jar con java -jar y ese
//    XDG_DATA_HOME, sin -Deduchronos.escritorio: así no abre navegador ni bandeja (S242 F1.3).
// 2. Recorre las pantallas de las tareas del manual a 1366x768, guarda cada captura como
//    <nn>-<nombre>.png y acumula el texto visible de cada pantalla y diálogo. Cada paso espera a
//    un texto concreto, nunca a un tiempo fijo. Todo lo que escribe va a la copia.
// 3. Comprueba que cada cita «…» de <manual.md> aparece en ese texto, salvo las que van entre
//    <!-- sin-comprobar --> y <!-- /sin-comprobar --> (Windows, ventana de arranque, bandeja).
// 4. Autoprueba del comprobador con dos citas inventadas y una real.
// 5. Para la aplicación y borra la carpeta de datos de la copia, también si algo falla antes.
//
// Las capturas tapan los nombres completos del profesorado: la base de partida es la del centro
// y las capturas se versionan. El texto acumulado no se tapa: la comprobación no cambia.
//
// Códigos de salida: 0 todas las citas están; 1 falta alguna; 2 falla la autoprueba;
// 3 aborta antes de comprobar (argumentos, base, puerto, versión o un paso del recorrido).
import { createRequire } from 'node:module';
import { spawn, execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const RAIZ = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const require = createRequire(path.join(RAIZ, 'app', 'frontend', 'package.json'));
const { chromium } = require('playwright');

const BASE_PARTIDA = '/home/luis/educhronos-aceptacion/s241/f1/base-partida/base-partida-2025-2026.db';
const MD5_BASE = 'd867c2dec4a9ab01357f539d8b09f7e2';
const URL_APP = 'http://127.0.0.1:8080';
const PUERTO = 8080;
const ESPERA_ARRANQUE_MS = 90_000;
const ESPERA_PASO_MS = 30_000;
const VIEWPORT = { width: 1366, height: 768 };
const INVENTADAS = ['Botón inexistente 9.9.9', 'versión 9.9.9'];

class Aborta extends Error {}

const [jar, versionEsperada, carpetaCapturas, manual] = process.argv.slice(2);
if (!jar || !versionEsperada || !carpetaCapturas || !manual) {
  console.error('Uso: node tools/manual/capturas.mjs <JAR> <VERSION_ESPERADA> <carpeta de capturas> <manual.md>');
  process.exit(3);
}

// ---------------------------------------------------------------- citas del manual

/** Las citas «…» del manual con su línea, fuera de los tramos sin-comprobar. Admite anidadas. */
function citasDelManual(texto) {
  const sinComprobar = /<!--\s*sin-comprobar\s*-->[\s\S]*?<!--\s*\/sin-comprobar\s*-->/g;
  const limpio = texto.replace(sinComprobar, (m) => m.replace(/[^\n]/g, ' '));
  const citas = [];
  const abiertas = [];
  let linea = 1;
  for (let i = 0; i < limpio.length; i++) {
    const c = limpio[i];
    if (c === '\n') linea++;
    else if (c === '«') abiertas.push({ i, linea });
    else if (c === '»' && abiertas.length > 0) {
      const a = abiertas.pop();
      citas.push({ cita: limpio.slice(a.i + 1, i), linea: a.linea });
    }
  }
  return citas;
}

const normalizar = (s) => s.replace(/\s+/g, ' ').trim();

/** El comprobador: el MISMO para las citas del manual y para la autoprueba. */
function aparece(cita, textoAcumulado) {
  let buscada = normalizar(cita);
  if (buscada.endsWith('…')) buscada = buscada.slice(0, -1).trimEnd();
  return buscada.length > 0 && textoAcumulado.includes(buscada);
}

// ---------------------------------------------------------------- aplicación

function md5(fichero) {
  return createHash('md5').update(fs.readFileSync(fichero)).digest('hex');
}

/** PID que escucha en 127.0.0.1:8080, o null (ss -ltnp, como en S242 F1.6). */
function pidEnPuerto() {
  const salida = execFileSync('ss', ['-ltnpH', `sport = :${PUERTO}`], { encoding: 'utf8' });
  const m = salida.match(/pid=(\d+)/);
  return m ? Number(m[1]) : null;
}

function puertoOcupado() {
  return execFileSync('ss', ['-ltnH', `sport = :${PUERTO}`], { encoding: 'utf8' }).trim() !== '';
}

const pausa = (ms) => new Promise((r) => setTimeout(r, ms));

async function esperarVersion(proceso) {
  const fin = Date.now() + ESPERA_ARRANQUE_MS;
  while (Date.now() < fin) {
    if (proceso.exitCode !== null) throw new Aborta(`el jar terminó con código ${proceso.exitCode} antes de responder`);
    try {
      const r = await fetch(`${URL_APP}/api/version`);
      if (r.status === 200) return (await r.json()).version;
    } catch {
      // todavía no escucha
    }
    await pausa(500);
  }
  throw new Aborta(`/api/version no dio 200 en ${ESPERA_ARRANQUE_MS / 1000} s`);
}

async function parar(proceso) {
  const pid = pidEnPuerto();
  if (pid !== null) {
    process.kill(pid, 'SIGTERM');
    console.log(`Parada: SIGTERM al PID ${pid}, que escuchaba en 127.0.0.1:${PUERTO}.`);
  } else if (proceso && proceso.exitCode === null) {
    proceso.kill('SIGTERM');
    console.log(`Parada: nadie escuchaba en ${PUERTO}; SIGTERM al PID lanzado ${proceso.pid}.`);
  }
  const fin = Date.now() + 60_000;
  while (Date.now() < fin && (puertoOcupado() || (proceso && proceso.exitCode === null))) await pausa(250);
  if (puertoOcupado()) console.log(`AVISO: el puerto ${PUERTO} sigue ocupado tras 60 s.`);
}

// ---------------------------------------------------------------- recorrido

async function recorrer(page, nombresReales, capturas, textos) {
  let n = 0;

  const textoPagina = () => page.evaluate(() => document.body.innerText);

  async function esperar(presentes, ausentes = []) {
    try {
      await page.waitForFunction(
        ([p, a]) => {
          const t = document.body.innerText.replace(/\s+/g, ' ');
          return p.every((x) => t.includes(x)) && a.every((x) => !t.includes(x));
        },
        [presentes.map(normalizar), ausentes.map(normalizar)],
        { timeout: ESPERA_PASO_MS },
      );
    } catch {
      throw new Aborta(`no apareció ${JSON.stringify(presentes)}${ausentes.length ? ` ni se fue ${JSON.stringify(ausentes)}` : ''}`);
    }
  }

  /**
   * Tapa lo que enseña un nombre completo del profesorado pintando de gris el PROPIO elemento,
   * con el texto transparente. Ni la máscara de Playwright (cubre la caja entera y se sale de
   * una lista con barra de desplazamiento) ni una capa encima (los diálogos van en la capa
   * superior del navegador y quedan por delante): pintar el elemento lo recorta su contenedor.
   * Se deshace después de la captura; el texto no cambia.
   */
  async function taparNombres() {
    await page.evaluate((nombres) => {
      const tiene = (s) => nombres.some((x) => s.includes(x));
      const tapar = new Set();
      const recorrido = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
      let nodo;
      while ((nodo = recorrido.nextNode())) {
        if (!tiene(nodo.textContent)) continue;
        const padre = nodo.parentElement;
        if (padre?.tagName === 'OPTION') {
          const select = padre.closest('select');
          if (select && select.selectedOptions[0] === padre) tapar.add(select);
        } else if (padre) {
          tapar.add(padre);
        }
      }
      for (const e of document.querySelectorAll('input, textarea')) if (tiene(e.value)) tapar.add(e);
      for (const e of tapar) {
        e.setAttribute('data-estilo-original', e.getAttribute('style') ?? '');
        e.style.setProperty('background', '#b8bec6', 'important');
        e.style.setProperty('color', 'transparent', 'important');
        e.style.setProperty('text-shadow', 'none', 'important');
      }
    }, nombresReales);
  }

  async function destapar() {
    await page.evaluate(() => {
      for (const e of document.querySelectorAll('[data-estilo-original]')) {
        const original = e.getAttribute('data-estilo-original');
        if (original) e.setAttribute('style', original);
        else e.removeAttribute('style');
        e.removeAttribute('data-estilo-original');
      }
    });
  }

  /** Espera, acumula el texto y, si lleva nombre, captura. */
  async function paso(nombre, presentes, ausentes = []) {
    await esperar(presentes, ausentes);
    textos.push(await textoPagina());
    if (!nombre) return;
    n++;
    const fichero = `${String(n).padStart(2, '0')}-${nombre}.png`;
    await taparNombres();
    await page.screenshot({ path: path.join(carpetaCapturas, fichero) });
    await destapar();
    capturas.push(fichero);
    console.log(`  ${fichero}`);
  }

  const ir = (ruta) => page.goto(URL_APP + ruta);
  const boton = (nombre, ambito = page) => ambito.getByRole('button', { name: nombre, exact: true });
  const fila = (texto) => page.locator('tr').filter({ has: page.locator('td', { hasText: new RegExp(`^${texto}$`) }) });
  const buscar = (texto) => page.getByPlaceholder('Buscar…').fill(texto);

  // Portada y horario de 2025/2026.
  await ir('/');
  await paso('portada', [`versión ${versionEsperada}`, 'Elige por dónde empezar.']);
  await ir('/horario?vista=grupo&entidad=4ºC');
  // «otros hallazgos»: la pre-validación ha llegado (sin ella, «Generar horario» sale apagado).
  await paso('horario-grupo', ['Exportar CSV', 'PDF guardias', 'LAT', 'LEN4', 'otros hallazgos']);
  await page.locator('.instancia').filter({ has: page.locator('.asig', { hasText: /^LAT$/ }) }).first().locator('.candado').click();
  await paso('horario-candado', ['1 pines sin aplicar — regenerar', '🔒']);
  await page.locator('.instancia.pinada .candado').first().click();
  await paso(null, ['Exportar CSV'], ['pines sin aplicar']);
  await ir('/horario?vista=profesor&entidad=BYG1');
  await paso('horario-profesor', ['Exportar CSV', 'ByG', 'otros hallazgos']);
  await ir('/horario?vista=aula&entidad=A10');
  await paso('horario-aula', ['Exportar CSV', 'LEN4', 'otros hallazgos']);

  // Con horario generado, una clase no se puede guardar (S242 F1.5: el PUT da 409).
  await ir('/configuracion/actividades');
  await paso(null, ['Nueva actividad', 'N.º de plazas']);
  await buscar('Latín');
  await paso(null, ['Actividades de la asignatura Latín (LAT-4ESO)', 'LAT-4ºC']);
  await fila('LAT-4ºC').getByRole('button', { name: 'Editar', exact: true }).click();
  await paso(null, ['Editar actividad', 'Repeticiones por semana']);
  await boton('Guardar', page.locator('.actividad-form')).click();
  await paso(null, ['No se puede editar: referenciada por 3 sesion(es)']);
  // Guardar deja el diálogo desplazado al pie; el aviso está arriba.
  await page.locator('.actividad-form__conflicto').scrollIntoViewIfNeeded();
  await paso('actividad-con-horario', ['No se puede editar: referenciada por 3 sesion(es)']);
  await boton('Cancelar', page.locator('.actividad-form')).click();
  await paso(null, ['Nueva actividad'], ['Repeticiones por semana']);

  // Cursos: duplicar 2025/2026 como 2026/2027.
  await boton('Cursos…').click();
  await paso('cursos', ['Duplicar curso…', 'Abierto ahora', 'educhronos.db']);
  await boton('Duplicar curso…').click();
  await paso(null, ['Curso nuevo', 'El curso actual queda archivado, en solo lectura.']);
  const cursoNuevo = page.getByLabel('Curso nuevo');
  if ((await cursoNuevo.inputValue()) !== '2026/2027') await cursoNuevo.fill('2026/2027');
  await paso('duplicar-curso', ['Curso nuevo', 'Al terminar se abre el curso nuevo.']);
  await boton('Duplicar').click();
  await paso('curso-2026-2027', ['2026/2027'], ['Dos años consecutivos']);
  await boton('Cursos…').click();
  await paso('cursos-tras-duplicar', ['Archivado', 'curso-2026-2027.db', 'Abierto ahora']);
  await boton('Cancelar', page.locator('.cursos-dialogo')).click();
  await paso(null, ['2026/2027'], ['Abierto ahora']);

  // Configuración y profesores, ya en 2026/2027.
  await ir('/configuracion');
  await paso('configuracion', ['Define un día tipo. El horario se aplica igual de lunes a viernes.', 'Guardar jornada', 'Subgrupos']);
  await ir('/configuracion/profesores');
  // «Profesor/a 12 0 0» es la fila de BYG1 con sus horas: llegan después de la lista, por su cuenta.
  await paso('profesores', ['Nuevo profesor', 'Mínimo de profesores de guardia por tramo', 'BYG1', 'Profesor/a 12 0 0']);
  await boton('Nuevo profesor').click();
  await paso(null, ['Horas de clase declaradas', 'Guardias de recreo']);
  const formProfesor = page.locator('.profesor-form');
  await formProfesor.getByLabel('Código').fill('BYG4');
  await formProfesor.getByLabel('Nombre completo').fill('Profesora Nueva');
  await paso('profesor-nuevo', ['Nuevo profesor', 'Horas de clase declaradas']);
  await boton('Guardar', formProfesor).click();
  await paso(null, ['Profesora Nueva'], ['Horas de clase declaradas']);
  await buscar('BYG4');
  await fila('BYG4').getByRole('button', { name: 'Disponibilidad', exact: true }).click();
  await paso(null, ['Disponibilidad — Profesora Nueva', 'Pincel']);
  await page.locator('.disponibilidad-dialogo__cabecera', { hasText: 'Lunes' }).click();
  await paso('disponibilidad', ['Disponibilidad — Profesora Nueva', 'No puede', 'Prefiere no']);
  await boton('Cancelar', page.locator('.disponibilidad-dialogo')).click();
  await paso(null, ['Nuevo profesor'], ['Pincel']);

  // Tutoría de un grupo.
  await ir('/configuracion/grupos');
  await paso(null, ['Nuevo grupo', '4ºC']);
  await fila('4ºC').getByRole('button', { name: 'Tutoría', exact: true }).click();
  await paso('tutoria', ['Tutoría de 4ºC', 'Tutor principal']);
  await boton('Cancelar', page.locator('.tutoria-dialogo')).click();
  await paso(null, ['Nuevo grupo'], ['Tutor principal']);

  // Clases: buscar por grupo y por nombre de asignatura; la ficha de LAT-4ºC.
  await ir('/configuracion/actividades');
  await paso(null, ['Nueva actividad', 'N.º de plazas']);
  await buscar('4ºC');
  await paso('actividades-4C', ['Actividades del grupo 4ºC', 'LAT-4ºC']);
  await buscar('Latín');
  await paso('actividades-latin', ['Actividades de la asignatura Latín (LAT-4ESO)', 'LAT-4ºC']);
  await fila('LAT-4ºC').getByRole('button', { name: 'Editar', exact: true }).click();
  await paso('actividad-lat-4C', ['Editar actividad', 'Repeticiones por semana', 'Plazas']);
  await boton('Cancelar', page.locator('.actividad-form')).click();
  await paso(null, ['Nueva actividad'], ['Repeticiones por semana']);
  // S252: el alta de una clase pide primero el nivel (O-actividad-por-nivel, condición 4).
  await boton('Nueva actividad').click();
  await paso(null, ['Elige primero el nivel.', 'Mostrar todos los niveles']);
  await boton('Cancelar', page.locator('.actividad-form')).click();
  await paso(null, ['Nueva actividad'], ['Repeticiones por semana']);

  // Baja de BYG1: el intento, las clases a BYG4 y el borrado.
  await ir('/configuracion/profesores');
  await paso(null, ['Nuevo profesor', 'BYG1']);
  await buscar('BYG1');
  await fila('BYG1').getByRole('button', { name: 'Borrar', exact: true }).click();
  await paso(null, ['(BYG1)?']);
  await boton('Borrar', page.locator('.confirmar-borrado')).click();
  await paso('borrar-byg1-aviso', ['No se puede borrar: referenciada por 4 plaza(s).']);

  await ir('/configuracion/actividades');
  await paso(null, ['Nueva actividad', 'N.º de plazas']);
  await buscar('BYG1');
  await paso(null, ['Actividades del profesor BYG1', 'ByG-1ºA']);
  const codigos = await page.locator('.actividades__tabla tbody tr td:first-child').allInnerTexts();
  let primera = true;
  for (const codigo of codigos.map((c) => c.trim())) {
    await fila(codigo).getByRole('button', { name: 'Editar', exact: true }).click();
    await paso(null, ['Editar actividad', 'Elegidos (']);
    const profesores = page.locator('.actividad-form [data-lista="profesores"]');
    // La lista se repinta después del clic: se espera al DOM nuevo, no se cuenta en el acto.
    const elegido = (cod) => profesores.locator(`[data-lado="elegidos"] [data-codigo="${cod}"]`);
    await elegido('BYG1').click();
    await boton('Quitar ›', profesores).click();
    await elegido('BYG1').waitFor({ state: 'detached', timeout: ESPERA_PASO_MS });
    await profesores.locator('[data-lado="disponibles"] [data-codigo="BYG4"]').click();
    await boton('‹ Añadir', profesores).click();
    await elegido('BYG4').waitFor({ timeout: ESPERA_PASO_MS }).catch(() => {
      throw new Aborta(`BYG4 no quedó elegido en ${codigo}`);
    });
    await paso(primera ? 'reasignar-clase' : null, ['Editar actividad', 'Profesora Nueva']);
    primera = false;
    await boton('Guardar', page.locator('.actividad-form')).click();
    await paso(null, [`Actividad «${codigo}» guardada.`], ['Repeticiones por semana']);
  }
  await paso('reasignadas', ['Ningún resultado para «BYG1».']);

  await ir('/configuracion/profesores');
  await paso(null, ['Nuevo profesor', 'BYG1']);
  await buscar('BYG1');
  await fila('BYG1').getByRole('button', { name: 'Borrar', exact: true }).click();
  await paso(null, ['(BYG1)?']);
  await boton('Borrar', page.locator('.confirmar-borrado')).click();
  await paso('baja-byg1', ['Ningún resultado para «BYG1».'], ['(BYG1)?']);

  // Horario de 2026/2027: sin horario, pre-validación y confirmación (se cancela).
  await ir('/horario');
  await paso('horario-sin-generar', ['Este curso todavía no tiene horario. Créalo con «Generar horario».', 'errores', 'otros hallazgos']);
  const generar = page.locator('button.generar');
  await generar.waitFor({ state: 'visible' });
  await page.waitForFunction(() => !document.querySelector('button.generar')?.disabled, null, { timeout: ESPERA_PASO_MS });
  await generar.click();
  // «Tarda hasta 10 minutos.» lleva un enlace de datos: los textos fijos de la ventana están antes
  // de que se pinte, y esperar solo a ellos dejó fuera las partes dinámicas (1.ª vuelta de S242 F2.4).
  await paso('confirmar-generacion', ['Confirmar la generación', 'Tiempo de cálculo', 'Tarda hasta 10 minutos.', 'No se puede deshacer.']);
  await boton('Cancelar', page.locator('.confirmar-generacion')).click();
  await paso(null, ['Este curso todavía no tiene horario.'], ['Tiempo de cálculo']);
}

// ---------------------------------------------------------------- principal

async function principal() {
  if (!fs.existsSync(jar)) throw new Aborta(`no existe el jar ${jar}`);
  if (!fs.existsSync(manual)) throw new Aborta(`no existe el manual ${manual}`);
  const huella = md5(BASE_PARTIDA);
  if (huella !== MD5_BASE) throw new Aborta(`md5 de la base de partida ${huella}, se esperaba ${MD5_BASE}`);
  if (puertoOcupado()) throw new Aborta(`el puerto ${PUERTO} ya está ocupado: no se arranca nada`);
  fs.mkdirSync(carpetaCapturas, { recursive: true });

  const datos = fs.mkdtempSync(path.join(os.tmpdir(), 'educhronos-manual-'));
  const log = path.join(datos, 'arranque.log');
  let proceso = null;
  let rc = 3;
  try {
    fs.mkdirSync(path.join(datos, 'educhronos'));
    fs.copyFileSync(BASE_PARTIDA, path.join(datos, 'educhronos', 'educhronos.db'));
    console.log(`Copia de la base de partida (md5 ${huella}) en ${datos}/educhronos/educhronos.db`);
    const salida = fs.openSync(log, 'w');
    proceso = spawn('java', ['-jar', jar], {
      env: { ...process.env, XDG_DATA_HOME: datos },
      stdio: ['ignore', salida, salida],
    });
    const version = await esperarVersion(proceso);
    console.log(`GET /api/version → ${version}`);
    if (version !== versionEsperada) throw new Aborta(`la versión es ${version}, se esperaba ${versionEsperada}`);

    const nombresReales = (await (await fetch(`${URL_APP}/api/profesores`)).json()).map((p) => p.nombreCompleto);
    const capturas = [];
    const textos = [];
    const navegador = await chromium.launch();
    try {
      const page = await navegador.newPage({ viewport: VIEWPORT });
      console.log(`Capturas en ${carpetaCapturas}:`);
      await recorrer(page, nombresReales, capturas, textos);
    } finally {
      await navegador.close();
    }

    const acumulado = normalizar(textos.join('\n'));
    // Para diagnosticar una cita que no aparece: el texto acumulado, tal cual, en un fichero.
    if (process.env.CAPTURAS_TEXTO) fs.writeFileSync(process.env.CAPTURAS_TEXTO, textos.join('\n\n=====\n\n'));
    const citas = citasDelManual(fs.readFileSync(manual, 'utf8'));

    // Autoprueba, antes de dar el resultado.
    const fallosAutoprueba = [];
    for (const inventada of INVENTADAS)
      if (aparece(inventada, acumulado)) fallosAutoprueba.push(`da por presente la inventada «${inventada}»`);
    const real = `versión ${versionEsperada}`;
    if (!aparece(real, acumulado)) fallosAutoprueba.push(`da por ausente la real «${real}»`);
    if (fallosAutoprueba.length > 0) {
      for (const f of fallosAutoprueba) console.log(`AUTOPRUEBA FALLA: el comprobador ${f}`);
      rc = 2;
      return rc;
    }
    console.log(`Autoprueba: «${INVENTADAS.join('» y «')}» ausentes, «${real}» presente.`);

    const faltan = citas.filter((c) => !aparece(c.cita, acumulado));
    console.log(`Citas comprobadas: ${citas.length}; encontradas: ${citas.length - faltan.length}; no encontradas: ${faltan.length}.`);
    for (const c of faltan) console.log(`  NO ENCONTRADA (${path.basename(manual)}:${c.linea}): «${c.cita}»`);
    console.log(`Capturas escritas: ${capturas.length}.`);
    rc = faltan.length > 0 ? 1 : 0;
  } catch (e) {
    console.log(`ABORTA: ${e instanceof Aborta ? e.message : e.stack ?? e}`);
    if (fs.existsSync(log)) {
      console.log('Últimas líneas del arranque:');
      for (const l of fs.readFileSync(log, 'utf8').split('\n').slice(-15)) console.log(`  ${l}`);
    }
    rc = 3;
  } finally {
    await parar(proceso);
    fs.rmSync(datos, { recursive: true, force: true });
    console.log(`Carpeta de datos de la copia borrada: ${datos}`);
  }
  return rc;
}

const rc = await principal().catch((e) => {
  console.log(`ABORTA: ${e instanceof Aborta ? e.message : e.stack ?? e}`);
  return 3;
});
console.log(`rc=${rc}`);
process.exit(rc);
