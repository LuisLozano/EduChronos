# Acta del instalador de S237: `v0.5.0-rc.1` y `v0.5.0-rc.2` en la VM `Win11`

- **Objetivo:** `O-uso-centro`, condiciones 1, 2, 3 y 5, con `C-instalador`.
- **Versiones:** `v0.5.0-rc.1` y `v0.5.0-rc.2`, prerelease, tags anotados sobre `6296052`, construidas por `bundle` #15 y #14. Zips `Educhronos-win.zip` de 176 199 512 B (sha256 `edccf977…e71f`) y de 176 199 521 B (sha256 `fbcebedf…4329`), verificados en Linux y en Windows. Los dos llevan `Educhronos\Instalar.cmd` (sha256 `963532cf…cf9d`, el del repo) y difieren en el jar (`app-0.5.0-rc.1.jar` y `app-0.5.0-rc.2.jar`).
- **Máquina:** VM `Win11` (VirtualBox), restaurada desde `s235-actualizada`. Antes se tomó `s237-previa` y, al terminar, `s237-instalador-hecho`. El anfitrión, con inhibidor de suspensión.
- **Cuenta:** `inst237`, nueva, local y estándar: sin S-1-5-32-544 en su token. Creada por PowerShell desde la consola de `educhronos-admin`.
- **Transporte:** entrada por el USB físico ESD-USB (F:, carpeta `s237`: los dos zips con su `.sha256` y `base-partida\base-partida-s181.db`, md5 `ba0db782…`); salida de los transcripts por la carpeta compartida (E:).
- **Control de Edge (condición 5):** «Ofrecer traducir páginas que no están en un idioma que leo», activado; único idioma preferido, español (España).

## Resultado
- **Copia y extracción de rc.1:** el zip copiado del USB solo tiene el flujo `:$DATA` y su sha256 casa. «Extraer todo» deja la carpeta `Educhronos` con `Instalar.cmd`. La carpeta `base-partida` se copió a mano junto a `Instalar.cmd` (decisión BS).
- **Instalación de rc.1 (condiciones 1 y 3):** doble clic en `Instalar.cmd`, sin control de cuentas, SmartScreen ni contraseña. La ventana recorre [1/3] a [3/3], copia la base y espera una tecla. Deja `Educhronos.exe`, `app` y `runtime` en `%LOCALAPPDATA%\Programs\Educhronos`, el acceso directo del Escritorio apuntando a ese exe y, en la carpeta de datos, solo `educhronos.db`, con el md5 de la base.
- **Arranque de rc.1 (condición 5):** ventana de arranque, «versión 0.5.0-rc.1» bajo la marca, «Esquema preparado» de 0 a 8 en el log y ningún aviso de traducir (del icono de la barra de direcciones no consta). Hay dos procesos `Educhronos`: el lanzador y su JVM (S151).
- **Segundo curso:** duplicado como 2026/2027; el selector muestra los dos.
- **Con la aplicación abierta:** `Instalar.cmd` de rc.2 se detiene con «ABORTA: Educhronos esta abierto…» y el programa instalado sigue siendo rc.1.
- **Actualización a rc.2 (condición 2):** con la aplicación cerrada, la carpeta de datos tiene cinco ficheros (dos `.db`, `curso-abierto`, `educhronos.lock` y el log). `Instalar.cmd` de rc.2, con `base-partida` también en su carpeta, dice que la carpeta de datos ya tiene 2 ficheros `.db` y no copia la base, y espera una tecla. Queda solo `app-0.5.0-rc.2.jar`. Las sha256 de los cinco ficheros de datos son idénticas antes y después.
- **Arranque de rc.2:** «versión 0.5.0-rc.2», los dos cursos y ningún aviso de traducir; el log arranca desde `app-0.5.0-rc.2.jar`.

## Incidencias del procedimiento
- La comprobación de la cuenta se ejecutó primero en la consola elevada de `educhronos-admin` y sin el USB conectado a la VM; se repitió en `inst237`.
- El guion esperaba un proceso `Educhronos` y hay dos (el lanzador y su JVM, documentado desde S151).
- Al entrar por primera vez en `inst237`, creada con `-NoPassword`, Windows pidió fijar una contraseña; no consta cómo se resolvió.
- La ventana de la instalación de rc.1 se observó y no se transcribió; la de la actualización a rc.2 se transcribió entera. No consta si la ventana del intento con la aplicación abierta esperó una tecla.

## Material
`/home/luis/educhronos-aceptacion/s237/f2/` (`MANIFIESTO.sha256`, 32 ficheros; transcripts en `vm/`) y `/home/luis/educhronos-aceptacion/s237/f2t0/`.
