# Acta de la generación sin suspensión de S238: `v0.5.0-rc.4` en la VM `Win11`

Cambio `C-sin-suspension`, condición 4 de `O-uso-centro`. Commits `b890c39` (mecanismo y tests) y `c2c157f` (JNA alineada con OR-Tools). Versión verificada: `v0.5.0-rc.4`, prerelease sobre `c2c157f` (zip `03d64d42…2754e`, 176 208 098 B).

## Qué hace
Mientras se genera un horario, la aplicación pide a Windows que no suspenda el equipo ni apague la pantalla (`PowerCreateRequest` y `PowerSetRequest`, tipos SYSTEM y DISPLAY, con el motivo «Educhronos está generando un horario») y lo retira al terminar: con horario, con error o con el rechazo de la prevalidación. Si la petición falla, deja un aviso en el log y la generación sigue. Fuera de Windows no hace nada.

## Evidencia
1. Tests: 10 nuevos en app (965 en total) y cinco mutantes, los cinco muertos (`s238/t1`).
2. Windows real en la CI: sonda en la rama experimental `experimento/s238-suspension` (run 39230125121, `windows-2025-vs2026`), con el JDK del producto y el runtime de 14 módulos. Con un control negativo previo, la petición aparece en `powercfg /requests` bajo DISPLAY y SYSTEM, desaparece al soltarla y desaparece si el proceso sale sin soltarla (`s238/m2`).
3. VM `Win11`, cuenta estándar `inst237`, desde `s237-instalador-hecho`, con `v0.5.0-rc.4` llegada por USB e instalada con `Instalar.cmd` encima de rc.2 (`s238/m4vm`):
   - durante una generación de 10 minutos, `powercfg /requests` muestra el motivo bajo DISPLAY y SYSTEM, del `Educhronos.exe` instalado;
   - al terminar, con horario, DISPLAY y SYSTEM dicen «Ninguna.»;
   - el log tiene la línea de petición y la de retirada de cada generación: 436 ms en un intento que rechazó la prevalidación de guardias y 605 831 ms en la generación con horario; ningún aviso;
   - JNA 5.14.0 se desempaqueta en el `%TEMP%` de la cuenta estándar sin problemas.
   Para generar se pusieron a 0 los tres mínimos de guardias del curso de prueba, como en el guion de aceptación. En Windows en español, `powercfg` traduce solo algunas cabeceras (`EJECUCIÓN:`), no DISPLAY ni SYSTEM. Lo que no recoge un transcript (el horario generado, los mínimos de guardias a 0 y el rechazo `GUARDIAS_INSUFICIENTES` del primer intento) consta en `m4vm/notas-usuario.txt`, según lo que contó el usuario en la conversación.
   - **Nota (S239, decisión CH):** la instantánea `s237-instalador-hecho` se borró el 19/10/2026 para liberar espacio en disco; la evidencia sigue en el material.

## Decisión y riesgo residual
Sin PC físico, la condición se da por cumplida con esta evidencia (decisión BW del usuario). No se ha observado una suspensión real impedida: eso depende de que Windows respete sus peticiones de energía, que es su comportamiento documentado; pedir también la pantalla cubre el modo de espera moderno. La condición 8 (uso real) aportará la confirmación: las dos líneas del log dirán si la protección estuvo activa en cada generación.

## Versión descartada
`v0.5.0-rc.3` (sobre `b890c39`) queda publicada como prerelease, pero descartada: declaraba JNA 5.19.1 y dejaba en 5.14.0 el `jna-platform` que trae OR-Tools. Se corrigió en `c2c157f`.

## Material
`/home/luis/educhronos-aceptacion/s238/` (t0, m2, c0, t1, t1bis, m4a, m4a2, m4b, m4b2, m4vm y cierre), cada carpeta con su MANIFIESTO.sha256.
