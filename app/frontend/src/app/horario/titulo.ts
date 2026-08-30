/**
 * Título de la vista de horario (D10). Función PURA y en la capa `horario/`, como
 * `filtrar` o `indiceViolaciones`: el contenedor orquesta señales, no formatea.
 *
 * <p>El problema que resuelve es de ANCHO, no de estética. El backend nombra cada
 * horario `"Horario " + Instant.now()` cuando el POST no manda nombre
 * (`GeneradorHorarioService:187-188`), y el frontend nunca lo manda
 * (`horario.service.ts:27` postea `{}`), así que el nombre real del horario 1 de la
 * BD demo son 39 caracteres con precisión de NANOSEGUNDO:
 * `Horario 2026-08-24T15:37:39.317184258Z`. D9 mete el título en la misma fila que
 * dos selectores y un botón, y eso ahí no cabe.
 *
 * <p>Arreglarlo en ORIGEN —mandar un nombre legible en el POST— sería una escritura
 * nueva, que el criterio 5 de O-navegación excluye. Queda como deuda de O-demo; aquí
 * se corrige la PINTURA, que es lo que este encargo puede tocar.
 *
 * <p>Degradado deliberado: si la fecha no parsea se devuelve el nombre TAL CUAL. Un
 * título feo se lee; un título vacío no dice qué horario estás mirando.
 */

/** Prefijo que el backend antepone al instante. Sin él, el nombre no es suyo. */
const PREFIJO = 'Horario ';

/**
 * Forma ISO mínima exigida antes de construir el `Date`. NO sobra sobre el
 * `Number.isNaN` posterior: `new Date('2026')` es una fecha VÁLIDA (1 de enero), así
 * que un nombre como `"Horario 2026"` pasaría el parseo y se pintaría como
 * `01/01/2026 00:00`, inventando un dato que nadie escribió. Con la `T` obligatoria,
 * eso cae al degradado y se muestra el nombre original.
 */
const ISO = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/;

function dosDigitos(n: number): string {
  return String(n).padStart(2, '0');
}

/**
 * Convierte el nombre crudo del horario en el título de la vista.
 *
 * - Sin nombre (o en blanco): `"Horario"` a secas, que es lo que se pinta mientras la
 *   proyección no ha llegado.
 * - Nombre del backend con instante parseable: `"Horario dd/mm/aaaa hh:mm"` en HORA
 *   LOCAL, porque quien mira el horario está en el centro, no en UTC.
 * - Cualquier otra cosa —nombre puesto a mano, instante ilegible—: el nombre tal cual.
 *
 * <p>El instante llega con nanosegundos y `Date` los trunca a milisegundos, que para
 * un título al minuto sobra de largo.
 */
export function tituloHorario(nombre: string | null | undefined): string {
  if (nombre === null || nombre === undefined || nombre.trim() === '') {
    return 'Horario';
  }
  if (!nombre.startsWith(PREFIJO)) {
    return nombre;
  }
  const instante = nombre.slice(PREFIJO.length).trim();
  if (!ISO.test(instante)) {
    return nombre;
  }
  const d = new Date(instante);
  if (Number.isNaN(d.getTime())) {
    return nombre;
  }
  const fecha = `${dosDigitos(d.getDate())}/${dosDigitos(d.getMonth() + 1)}/${d.getFullYear()}`;
  return `Horario ${fecha} ${dosDigitos(d.getHours())}:${dosDigitos(d.getMinutes())}`;
}
