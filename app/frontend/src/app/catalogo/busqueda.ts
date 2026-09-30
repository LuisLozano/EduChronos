/**
 * Predicado de BÚSQUEDA por texto de las listas de catálogo (S124,
 * `C-listas-filtradas`). Lógica pura, hermana de `horario/pines.ts`: sin
 * componentes, sin servicios y SIN conocer entidades —recibe el texto de la fila
 * ya compuesto, y componerlo es de quien tenga el modelo delante—.
 *
 * <p><b>El módulo se llama BÚSQUEDA, y ese nombre está elegido, no heredado.</b>
 * La palabra que se evita aquí —la de «criba», «tamiz»— ya está tomada en
 * `grupos/`, donde nombra el condicional que decide qué botones lleva cada fila
 * según sea ordinario o PDC (`grupo-lista.ts:46-53` y su caso (14)). Dos cosas
 * distintas con el mismo nombre se confunden en la siguiente lectura, así que
 * esto se llama de otra manera a propósito.
 */

/**
 * Indicadores ordinales. Van APARTE de {@link NO_ALFANUMERICO} porque Unicode
 * los clasifica como LETRA (categoría `Lo`), así que la regla general no los
 * tocaría y «1ºESO» no se reduciría a «1ESO». Tampoco vale NFKD, que los
 * descompone a «o»/«a» y daría «1OESO».
 *
 * <p>Se quitan SOLO cuando la parte de la consulta no lleva ordinal (ver
 * {@link coincide}): «1B» sigue encontrando «1ºB» (S124), pero quien escribe
 * «1ºB» lo está distinguiendo y no debe ver «1B-A», porque en este centro «1ºB»
 * es ESO y «1B» es Bachillerato (S185, M4 del usuario).
 *
 * <p>El signo de grado «°» (U+00B0) se lee como «º» en fila y consulta: sale de
 * muchos teclados y de pegar desde una hoja de cálculo, y así el contrato del
 * caso (7 bis) vale en las dos direcciones también cuando el ordinal cuenta.
 */
const ORDINALES = /[ºª]/gu;
const GRADO = /\u00B0/gu; // «°» pegado desde hoja de cálculo en lugar de «º»: (7 bis)

const TIENE_ORDINAL = /[ºª\u00B0]/u; // sin /g: .test() con /g guarda lastIndex entre llamadas

/**
 * Todo lo que no sea letra o dígito: puntos, guiones, espacios, barras… y las
 * MARCAS DIACRÍTICAS que suelta el NFD, que son categoría `M` y por tanto ni
 * `\p{L}` ni `\p{N}`. Ese segundo cometido no es un efecto colateral: es la
 * mitad del borrado de tildes de {@link normaliza}. Un `\p{M}` explícito aquí
 * sería código inerte —se midió en S124: quitarlo no cambia ni un resultado—,
 * pero ampliar esta clase para dejar pasar algo (un espacio, un guion) DEVOLVERÍA
 * las tildes por la puerta de atrás.
 */
const NO_ALFANUMERICO = /[^\p{L}\p{N}]/gu;

/**
 * Forma canónica de un texto para comparar: sin tildes, sin puntuación, sin
 * espacios y en mayúsculas. «Ed. Física» → «EDFISICA», «1ºESO» → «1ESO»,
 * «Muñoz» → «MUNOZ». Con `conservarOrdinales`, «º» y «ª» se quedan —«1ºESO» →
 * «1ºESO»—: {@link NO_ALFANUMERICO} no los toca porque son letras para Unicode.
 * El «°» se convierte en «º» ANTES de todo lo demás, así que sigue la misma
 * regla: se quita sin el flag y se conserva, como «º», con él.
 *
 * <p>Las tildes se van en DOS PASOS ACOPLADOS, no en uno: el NFD separa la «Á»
 * en «A» + marca, y {@link NO_ALFANUMERICO} se lleva la marca. Quitar
 * cualquiera de los dos deja el acento en pie; el spec tiene un caso por cada
 * uno para que ninguno se pueda retirar en verde.
 *
 * <p>Es una reducción con PÉRDIDA, y esa pérdida es deliberada: «AmbSL» y
 * «ÁmbSL» —dos códigos que la base de datos distingue— colapsan los dos en
 * «AMBSL». Sirve para ENCONTRAR sin saber cómo se acentuó el dato; no sirve para
 * desambiguar duplicados, y nada debe usarla como identidad.
 */
export function normaliza(texto: string, conservarOrdinales = false): string {
  const base = texto.replace(GRADO, 'º').normalize('NFD').toUpperCase();
  return (conservarOrdinales ? base : base.replace(ORDINALES, '')).replace(NO_ALFANUMERICO, '');
}

/**
 * Si la fila casa con la consulta. La consulta se parte por espacios y CADA
 * parte ha de aparecer como subcadena del texto normalizado de la fila, en
 * cualquier orden: «mat 1eso» encuentra la fila que tenga las dos cosas, esté
 * como esté escrita y en el orden que sea.
 *
 * <p><b>El ordinal cuenta solo si la parte lo lleva.</b> Una parte sin «º»/«ª»/«°»
 * se compara con los ordinales quitados a los dos lados («1B» encuentra «1ºB»,
 * S124); una parte con ordinal se compara conservándolos a los dos lados («1ºB»
 * ya no encuentra «1B-A», S185). La decisión es POR PARTE: en «3ºA Di», «3ºA»
 * discrimina y «Di» no.
 *
 * <p>Consulta vacía —o solo espacios, o solo separadores como «...», que se
 * normalizan a nada— casa SIEMPRE: sin escribir nada se ve la lista entera.
 *
 * <p>Sin longitud mínima, sin límites de palabra y sin coincidencia exacta: la
 * subcadena arrastra a los hijos con el padre («3ºA» encuentra también
 * «3ºADi-Completo») y eso es lo que se quiere, no un efecto colateral.
 */
export function coincide(textoFila: string, consulta: string): boolean {
  const partes = consulta.split(/\s+/).filter((parte) => normaliza(parte).length > 0);
  return partes.every((parte) => {
    const conOrdinal = TIENE_ORDINAL.test(parte);
    return normaliza(textoFila, conOrdinal).includes(normaliza(parte, conOrdinal));
  });
}
