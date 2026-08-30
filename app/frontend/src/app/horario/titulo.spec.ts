import { tituloHorario } from './titulo';

/**
 * El nombre REAL del horario 1 de `app/educhronos-demo-m4.db`, leído de la BD. No es
 * un ejemplo inventado: es el dato que la vista tiene que pintar, y los 39 caracteres
 * que motivan D10.
 */
const NOMBRE_DEMO = 'Horario 2026-08-24T15:37:39.317184258Z';

/**
 * Título esperado para un instante, derivado del MISMO `Date` que usa el navegador.
 * Escrito así y no con una cadena literal porque el formateo es en HORA LOCAL: un
 * literal ataría la suite a la zona de esta máquina y caería en cualquier CI en UTC.
 * La forma exacta —barras, dos puntos, orden— la fija el caso (3), que sí es literal
 * y es zona-independiente por construcción.
 */
function esperado(iso: string): string {
  const d = new Date(iso);
  const p = (n: number) => String(n).padStart(2, '0');
  return `Horario ${p(d.getDate())}/${p(d.getMonth() + 1)}/${d.getFullYear()} ${p(d.getHours())}:${p(d.getMinutes())}`;
}

describe('título de la vista de horario (D10)', () => {
  it('(1) el nombre real de la BD demo se acorta a fecha y hora al minuto', () => {
    const titulo = tituloHorario(NOMBRE_DEMO);

    expect(titulo).toBe(esperado('2026-08-24T15:37:39.317184258Z'));
    // DISCRIMINANTE del encargo: lo que se pinta cabe en una fila con dos selectores
    // y un botón, y el nombre crudo (39 caracteres) no.
    expect(titulo.length).toBeLessThan(NOMBRE_DEMO.length);
    expect(titulo).not.toContain('T');
    expect(titulo).not.toContain('Z');
  });

  /**
   * Forma exacta y relleno a dos dígitos, sin atar la suite a ninguna zona: la fecha
   * se construye por COMPONENTES LOCALES y se pasa a la función como el ISO
   * equivalente, así que el resultado local esperado es fijo en cualquier máquina.
   * Todos los campos son de un dígito a propósito: sin `padStart` esto diría
   * `5/1/2026 3:07` y caería.
   */
  it('(2) formato dd/mm/aaaa hh:mm con ceros a la izquierda', () => {
    const local = new Date(2026, 0, 5, 3, 7, 0);

    expect(tituloHorario(`Horario ${local.toISOString()}`)).toBe('Horario 05/01/2026 03:07');
  });

  it('(3) sin proyección todavía: "Horario" a secas', () => {
    expect(tituloHorario(null)).toBe('Horario');
    expect(tituloHorario(undefined)).toBe('Horario');
    expect(tituloHorario('   ')).toBe('Horario');
  });

  /**
   * El degradado. Un nombre que alguien puso a mano no se toca: prefiero un título
   * feo a un título vacío, y desde luego a uno inventado.
   */
  it('(4) nombre no reconocible: se devuelve tal cual', () => {
    expect(tituloHorario('Propuesta de septiembre')).toBe('Propuesta de septiembre');
    expect(tituloHorario('Horario de la tarde')).toBe('Horario de la tarde');
    expect(tituloHorario('Horario 2026-13-45T99:99:99Z')).toBe('Horario 2026-13-45T99:99:99Z');
  });

  /**
   * `new Date('2026')` es una fecha VÁLIDA (1 de enero). Sin la exigencia de forma ISO
   * completa, este nombre se pintaría como `Horario 01/01/2026 00:00` y la vista
   * estaría afirmando una hora de generación que nadie escribió.
   */
  it('(5) un año suelto NO se convierte en una fecha inventada', () => {
    expect(tituloHorario('Horario 2026')).toBe('Horario 2026');
  });
});
