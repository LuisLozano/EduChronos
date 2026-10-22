# Artefactos generados

Responde a esta pregunta: qué ficheros del repo salen de un programa y no se editan a mano, y cómo se regenera cada uno.
Léelo cuando cambies el código del solver o un documento de gestión con índice.

Describe el commit 30b8d67a9b49.

Hay dos: la referencia de código del solver y los índices de tres documentos de gestión.
Los dos se regeneran con un guion del repo.
Si no ha cambiado lo que describen, los índices salen iguales y la referencia solo cambia su fecha y su commit.

## La referencia del solver

`docs/referencia-codigo-solver.md` es un índice de la API del solver: cada tipo, con sus firmas y los tipos del módulo que usa.
No explica nada; solo lista lo que el código declara.
La genera `tools/referencia-solver/generar_referencia.py`, que lee los ficheros .java de `solver/src/`, los de main y los de test (`tools/referencia-solver/generar_referencia.py:209`, `tools/referencia-solver/generar_referencia.py:213`).
Por eso la referencia lista también los tipos de los tests: 60 de main y 50 de test.
`docs/referencia-codigo-solver.md:632` es el primero de los de test.

### Cuándo

Se regenera cuando cambia algo en `solver/src/`, en main o en test.
Se corre después del commit de código, y el resultado va en un commit de documentación aparte.
Así quedó la versión vigente: el código en 4e8a45e y la referencia en f302edf, que cita 4e8a45e en su cabecera.

### La orden

Desde la raíz del repo:

```bash
python3 -I -B tools/referencia-solver/generar_referencia.py "$PWD" "$(date +%F)" "$(git rev-parse --short HEAD)" "$PWD/docs/referencia-codigo-solver.md"
```

Los argumentos son la raíz, la fecha, el commit y el fichero de salida.
La raíz y la salida tienen que ser rutas absolutas; si no, o si falta alguno, sale con 2 sin escribir nada.
El commit es el del código, por eso se corre justo después de su commit.

Sin cambios en `solver/src/`, regenerar solo cambia las líneas 5 y 6 de la referencia: la fecha y el commit.
Las escribe `tools/referencia-solver/generar_referencia.py:258`.

### El guion y su especificación

El guion manda.
Lo que pide está escrito en `docs/Prompt_generar_referencia_codigo.txt`, que es su especificación.
Se aparta de ella en un punto: el orden de las secciones de tests.
La especificación ordena los paquetes como domain, cpsat, io y cli (`docs/Prompt_generar_referencia_codigo.txt:11`).
El guion sigue ese orden en las secciones del código, y pone las de tests en orden alfabético: cli, cpsat, domain, io.
Así está en la referencia (`docs/referencia-codigo-solver.md:630`, `docs/referencia-codigo-solver.md:653`, `docs/referencia-codigo-solver.md:1034`, `docs/referencia-codigo-solver.md:1083`).

Sus tests están en `tools/referencia-solver/tests/test_generar_referencia.py` y se corren como cuenta `docs/tecnica/probar.md`.
Trabajan sobre un solver fabricado en una carpeta temporal, nunca sobre el real.

## Los índices de los documentos de gestión

Tres documentos de gestión llevan al principio un índice con cada encabezado y su línea.
Son `docs/gestion_proyecto.md`, `docs/plan_trabajo_horarios.md` y `docs/gestion-historico.md` (`scripts/regenerar-indice.py:19-20`).
Los números de línea caducan en cuanto alguien edita, así que el índice se regenera en cada cierre de sesión (`scripts/regenerar-indice.py:6-7`).

Desde la raíz del repo, sin argumentos:

```bash
python3 -B scripts/regenerar-indice.py
```

Con cualquier argumento no escribe nada y sale con 2 (`scripts/regenerar-indice.py:67-72`).
Por cada documento imprime cuántos encabezados y entradas tiene y cuántas entradas están descuadradas (`scripts/regenerar-indice.py:77-78`).
Una descuadrada es un fallo, y el guion sale con 1 (`scripts/regenerar-indice.py:86`).
Comprueba también que regenerar dos veces da lo mismo (`scripts/regenerar-indice.py:80-85`).
