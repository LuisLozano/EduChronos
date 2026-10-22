# Acta de la prueba de lectura de docs/tecnica (S247) — condición 4 de O-doc-tecnica

- **Documentos probados:** los nueve de `docs/tecnica/`, copiados a una carpeta aparte en cada pasada: los de `ce5f32e` en la primera, `c0e6338` en la segunda y `34a57f6` en la tercera.
- **Preguntas:** `/home/luis/educhronos-aceptacion/s247/f0/preguntas.txt`, sha256 `f4ea3b82d1f7ea4de583f3db11e06090b9700efee751ccec329cc8c16c29e14d`.
- **Hoja de soluciones:** `/home/luis/educhronos-aceptacion/s247/f0/soluciones.md` (versión 2), sha256 `6057717e44f780a086c1461d019750eac6a4bc7f4042078e7cf209a9180746b5`. Cada punto cita su fuente en el repo, sin usar `docs/tecnica/`.
- **Material:** `/home/luis/educhronos-aceptacion/s247/fd/`: un canario y una corrección por pasada, con las respuestas literales del agente.

## Las preguntas

Las fijó el usuario en S247 (decisiones DY y EB), antes de escribir `docs/tecnica/decisiones.md` y `docs/tecnica/documentos.md`.
Los siete documentos anteriores se escribieron en S245 y S246 sin ellas: el criterio pedía fijarlas antes, y no se hizo (decisión EC).

1. ¿Qué tecnologías se usan en el proyecto?
2. ¿Qué repositorio de código usa?
3. ¿En qué consiste el proyecto? ¿Qué hace y a qué público se dirige?
4. ¿Qué alcance tiene? Qué cubre y qué no.
5. ¿Cuantos módulos tiene y qué hacen cada uno?
6. ¿Existen herramientas externas como scripts o cualquier otro tipo de herramienta?
7. ¿En qué estadio de desarrollo se encuentra el proyecto?
8. ¿Hay errores conocidos?
9. Si tengo un error ¿Dónde puedo consultar el módulo implicado y el código afectado?
10. ¿Cómo se construye el proyecto?
11. ¿Hay manuales de uso e instalación?
12. ¿Por qué se decidió elegir la tecnología de desarrollo?
13. ¿Quien decide las reglas duras/blandas?
14. ¿Cómo se definió el proyecto: hay requisitos?
15. ¿Qué tipo de test existen y como se pasan?
16. ¿Cómo se distribuye?

## Cómo se corrige

Una respuesta acierta si recoge la sustancia de todos sus puntos esenciales y no afirma nada falso.
Las cifras de versión, los recuentos y los ejemplos son detalle: no se exigen, pero si la respuesta los da, tienen que ser correctos.
«No consta», o un punto esencial que falta, es fallo. Una corrección dudosa la decide el usuario.

En cada pasada, un subagente nuevo, de solo lectura, recibe las 16 preguntas y la orden de usar solo los ficheros de su carpeta y de decir «no consta» si la documentación no lo dice.
Antes, otro subagente nuevo, con la misma configuración, responde qué sabe de Educhronos sin leer nada: es el canario.
El canario está contaminado si dice algo del proyecto que no salga del nombre.

## Pasada 1 (`ce5f32e`)

- **Comprobación previa:** `~/.claude/CLAUDE.md` existe y no menciona Educhronos. El canario dice «No sé nada» y solo nombra el directorio de trabajo: limpio.
- **Resultado:** 3 de 16. Aciertan la 5, la 6 y la 15; fallan la 1, 2, 3, 4, 7, 8, 9, 10, 11, 12, 13, 14 y 16.
  La 5 y la 14 salieron dudosas, y el usuario decidió: la 5 acierta, porque «sin Spring» es detalle, y la 14 falla.
- **Lo que se añadió (`c0e6338`):**
  - `docs/tecnica/README.md`: qué es Educhronos y para quién (3), qué cubre y qué no (4), en qué punto está (7), los errores conocidos (8) y cómo se definió (14).
  - `docs/tecnica/mapa.md`: el repositorio y la rama main (2), y dónde mirar ante un fallo (9).
  - `docs/tecnica/arquitectura.md`: que el frontend es TypeScript (1) y quién fija las reglas duras y blandas (13).
  - `docs/tecnica/construir.md`: la versión con -Drevision (10).
  - `docs/tecnica/publicar.md`: la descarga y la comprobación de la huella (16), y que el manual trae la instalación y acaba en la guía de incidencias (11).
  - `docs/tecnica/decisiones.md`: la elección de la Fase 0 y los nativos de Windows (12), dentro de su tope de 18 000 B.

## Pasada 2 (`c0e6338`)

- **Dos intentos que no valen.** En los dos, el agente leyó el volcado que el propio entorno guarda cuando una orden saca demasiado texto, fuera de su carpeta.
  Ese volcado solo contenía los nueve documentos, pero la regla de la pasada lo prohíbe. El usuario mandó añadir al prompt: «Lee los ficheros de uno en uno con la herramienta de lectura; no los vuelques juntos con cat» (enmienda E16).
  Sus canarios, los dos limpios.
- **Tercer intento, válido:** `~/.claude/CLAUDE.md` sin menciones; canario limpio, que solo nombra el directorio y supone «horarios escolares» por el nombre.
- **Resultado:** 14 de 16. Falla la 9: no nombra dónde están el código del solver, con su referencia, ni el del frontend. La 14 sale dudosa: omite «sobre medición», que el README ya dice.
- **Lo que se añadió (`34a57f6`):** en `docs/tecnica/mapa.md`, el párrafo de qué hacer ante un fallo nombra las secciones de la aplicación, del solver y del frontend, y la referencia `docs/referencia-codigo-solver.md`.

## Pasada 3 (`34a57f6`)

- **Comprobación previa:** `~/.claude/CLAUDE.md` sin menciones; canario limpio («No sé nada del proyecto Educhronos»).
- **Resultado:** 16 de 16.
- **Lo que se añadió:** nada.

## Salvedad

El contexto del subagente no se puede certificar: el canario pregunta qué sabe, y responde él.
La lista de ficheros abiertos también la declara el propio subagente.
Los canarios solo mostraron el directorio de trabajo, que trae el nombre del proyecto.

## Notas para la lectura del usuario

- `docs/tecnica/construir.md` pide Maven 3.6.0 o superior, por el plugin del frontend, y el `README.md` de la raíz dice 3.9 o superior. No se ha tocado ninguno de los dos (decisión E17).
- El agente de la pasada 2 lee una contradicción: `docs/tecnica/decisiones.md` dice que los tests de las herramientas de Python quedan fuera de la CI, y `docs/tecnica/probar.md`, que la suite del comprobador de rutas sí corre en ella. La frase de decisiones.md se refiere a las herramientas de datos del centro y no lo dice.
- El agente de la pasada 3 señala que `docs/tecnica/arquitectura.md` y `docs/tecnica/construir.md` citan rangos distintos de `app/pom.xml` para la copia del frontend al jar.

## Lectura del usuario

En su lectura, el usuario dejó tres notas. Con ellas se revisaron también las notas 2 y 3 de arriba.

- **Modelo de datos.** `docs/tecnica/arquitectura.md`, apartado nuevo «Dónde está el modelo de datos»: el esquema de una base nueva, las migraciones y la versión en PRAGMA user_version, las entidades Java, y que `docs/modelo_datos_fase1.md` es el diseño de la Fase 1, con partes que el propio fichero marca como apartadas o superadas.
- **Configuración.** `docs/tecnica/arquitectura.md`, apartado nuevo «La configuración»: qué fija cada clave de `app/src/main/resources/application.properties`, y que el fichero de los tests tapa al de producción.
  El usuario pidió decir que ningún test vigila las claves de producción. No es exacto: `app/src/test/java/es/yaroki/educhronos/app/web/MensajeDeErrorHttpTest.java` carga a mano el fichero de producción. El apartado lo dice como excepción.
- **Publicar.** `docs/tecnica/publicar.md`, apartado «Publicar una versión, paso a paso», en nueve pasos: la CI de main, el tag, el workflow, la Release, el contenido del zip, la base de partida y qué hace con ella `scripts/Instalar.cmd`. La base original no está en el repo; el paso 7 dice dónde la sitúa su acta.
- **Nota 2 (tests de Python).** `docs/tecnica/decisiones.md`, entrada de las herramientas de Python: los tests del comprobador de rutas corren en la CI y los de las otras tres herramientas, no.
- **Nota 3 (rangos de `app/pom.xml`).** No se cambia. Las dos citas señalan el mismo plugin, que no ha cambiado desde los commits que describen los dos documentos: la de `docs/tecnica/arquitectura.md` acaba en la carpeta de origen de la copia, y la de `docs/tecnica/construir.md`, al cerrar el plugin.
- **Nota 1 (Maven).** Se deja, decisión del usuario pendiente en su relectura.

Relectura del usuario: pendiente.
