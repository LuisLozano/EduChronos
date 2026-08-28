import { coincide, normaliza } from './busqueda';

/**
 * Lógica PURA: sin TestBed, sin fixture, sin HTTP. Molde de `horario/pines.spec.ts`.
 *
 * <p>Los casos negativos NO son adorno. Sin ellos, media suite pasaría con un
 * `coincide()` que devolviera `true` siempre —o con `some()` en lugar de
 * `every()`—: lo que discrimina es la fila que NO debe casar.
 */
describe('búsqueda de las listas de catálogo', () => {
  describe('normaliza', () => {
    /** Los tres ejemplos normativos del contrato, cada uno con su pérdida propia. */
    it('(1) quita tildes, puntuación y ordinales, y sube a mayúsculas', () => {
      expect(normaliza('Ed. Física')).toBe('EDFISICA');
      expect(normaliza('1ºESO')).toBe('1ESO');
      expect(normaliza('Muñoz')).toBe('MUNOZ');
    });

    /**
     * COLAPSO DELIBERADO. «AmbSL» y «ÁmbSL» son dos códigos que la base de datos
     * distingue —conviven en el catálogo real— y aquí quedan indistinguibles. Es
     * el precio de encontrar sin saber cómo se acentuó el dato, y el aserto lo
     * congela para que el día que alguien quiera usar `normaliza()` como
     * identidad se encuentre con este caso escrito.
     */
    it('(2) «AmbSL» y «ÁmbSL» colapsan al mismo normalizado, a propósito', () => {
      expect(normaliza('ÁmbSL')).toBe(normaliza('AmbSL'));
      expect(normaliza('ÁmbSL')).toBe('AMBSL');
    });

    /**
     * Las tildes caen por DOS pasos acoplados —el NFD las separa de la vocal, el
     * barrido de no-alfanuméricos se lleva la marca suelta— y este caso ancla el
     * PRIMERO: sin `normalize('NFD')` la «Á» sigue siendo un único carácter de
     * categoría letra, sobrevive al barrido entero y el aserto cae. El caso (1) no
     * lo cubre por sí solo: se cumpliría con solo uno de los dos pasos si el otro
     * bastara, y no basta ninguno.
     */
    it('(2 bis) la vocal acentuada precompuesta queda igual que la lisa', () => {
      expect(normaliza('MATEMÁTICAS')).toBe('MATEMATICAS');
      expect(normaliza('Á')).toBe(normaliza('A'));
    });
  });

  describe('coincide', () => {
    /**
     * Las tres formas de «no he escrito nada»: vacío, solo espacios, y solo
     * separadores —«...» se normaliza a la cadena vacía y se descarta—.
     * Sin escribir, se ve TODO. Estos tres son los que mata un `coincide()` que
     * devuelva `false` con consulta vacía.
     */
    it('(3) consulta vacía, en blanco o solo separadores casa siempre', () => {
      expect(coincide('MAT', '')).toBe(true);
      expect(coincide('MAT', '   ')).toBe(true);
      expect(coincide('MAT', '...')).toBe(true);
    });

    /** Caja: la consulta va en minúsculas y el dato en mayúsculas. */
    it('(4) «matematicas» casa «MATEMÁTICAS» pese a caja y tilde', () => {
      expect(coincide('MATEMÁTICAS', 'matematicas')).toBe(true);
    });

    /**
     * El espacio de la consulta parte en dos partes que el dato NO separa: «1» y
     * «ESO» están pegados en «1ºESO», y el ordinal de por medio tampoco estorba.
     */
    it('(5) «1 ESO» casa «1ºESO»', () => {
      expect(coincide('1ºESO', '1 ESO')).toBe(true);
    });

    /** Un dato escrito de dos maneras, una sola consulta para las dos. */
    it('(6) «Ed Fisica» casa «Ed.Física» y «Ed. Fisica»', () => {
      expect(coincide('Ed.Física', 'Ed Fisica')).toBe(true);
      expect(coincide('Ed. Fisica', 'Ed Fisica')).toBe(true);
    });

    /**
     * EL GRADO POR EL ORDINAL. «1°ESO» con U+00B0 (signo de grado, el que sale de
     * muchos teclados y de pegar desde una hoja de cálculo) encuentra «1ºESO» con
     * U+00BA (indicador ordinal masculino, lo que hay en la base). Son caracteres
     * DISTINTOS y ninguna normalización Unicode los une.
     *
     * <p>Hoy pasa por {@link NO_ALFANUMERICO} —el grado es categoría `So`, símbolo,
     * no letra— y NO por `ORDINALES`, que solo lista «º» y «ª». O sea: funciona por
     * una rama distinta de la que uno creería al leer el código. Este caso es lo
     * único que lo protege; sin él, endurecer el barrido para conservar símbolos
     * rompería el pegado desde hoja de cálculo en silencio.
     */
    it('(7 bis) «1°ESO» con signo de grado casa «1ºESO» con ordinal', () => {
      expect(normaliza('1\u00B0ESO')).toBe('1ESO');
      expect(coincide('1\u00BAESO', '1\u00B0ESO')).toBe(true);
      expect(coincide('1\u00B0ESO', '1\u00BAESO')).toBe(true);
    });

    /** Ordinal en la consulta, guion en el dato, y sobra cola por la derecha. */
    it('(7) «3ºA Di» casa «3ºADi-Completo»', () => {
      expect(coincide('3ºADi-Completo', '3ºA Di')).toBe(true);
    });

    /** La eñe de la consulta ausente, la del dato presente. */
    it('(8) «Munoz» casa «Muñoz»', () => {
      expect(coincide('Muñoz', 'Munoz')).toBe(true);
    });

    /**
     * PADRE CON HIJOS, deliberado. Sin límites de palabra, «3ºA» arrastra a
     * «3ºADi-Completo»: quien busca el grupo ve también sus subgrupos. La segunda
     * mitad es la que documenta que NO es un descuido —está aseverada, no
     * tolerada—, y la tercera acota el arrastre: llega hasta donde llega el
     * prefijo, no a cualquier fila que empiece por 3.
     */
    it('(9) «3ºA» casa tanto el padre como el hijo, y no a un vecino cualquiera', () => {
      expect(coincide('3ºA', '3ºA')).toBe(true);
      expect(coincide('3ºADi-Completo', '3ºA')).toBe(true);
      expect(coincide('3ºB', '3ºA')).toBe(false);
    });

    /**
     * DISCRIMINANTE de `every` frente a `some`. Las dos primeras filas llevan las
     * dos partes en ORDEN CONTRARIO —la conjunción no depende del orden—, y las
     * dos últimas llevan SOLO UNA: son las que un `some()` daría por buenas. Sin
     * esas dos negativas, este caso no probaría nada que no probara ya el (4).
     */
    it('(10) «mat 1eso» exige LAS DOS partes, en cualquier orden', () => {
      expect(coincide('MAT-1ºESO', 'mat 1eso')).toBe(true);
      expect(coincide('1ºESO Matemáticas', 'mat 1eso')).toBe(true);
      expect(coincide('MAT-2ºESO', 'mat 1eso')).toBe(false);
      expect(coincide('1ºESO Lengua', 'mat 1eso')).toBe(false);
    });

    /** Lo que no está, no está: el predicado sabe decir que no. */
    it('(11) «zzz» no casa nada', () => {
      expect(coincide('MATEMÁTICAS', 'zzz')).toBe(false);
      expect(coincide('3ºADi-Completo', 'zzz')).toBe(false);
      expect(coincide('', 'zzz')).toBe(false);
    });
  });
});
