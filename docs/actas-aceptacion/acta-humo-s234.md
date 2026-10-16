# Acta del humo de S234: `v0.4.0-rc.1` en la VM `Win11`

- **Objetivo:** `O-prueba-secretaria`, condición 1 (humo), con el procedimiento de la condición 2 en uso.
- **Versión:** `v0.4.0-rc.1`, prerelease, tag anotado sobre `971646f`. `app/` y `solver/` sin cambios hasta `cd8d11c`. Zip `Educhronos-win.zip`, 176 197 754 B, sha256 verificado en Linux y en Windows.
- **Máquina:** VM `Win11` (VirtualBox), restaurada desde `s187-demo-lista`. Antes se tomó `s234-previa` y, al terminar, `s234-humo-hecho`.
- **Cuenta:** `humo234`, nueva y estándar: sin S-1-5-32-544 en su token e integridad media, con control positivo en la consola elevada de `educhronos-admin`. Sin `java` ni `node`.
- **Transporte:** entrada por el USB físico ESD-USB (F:, carpeta `s234`); salida por la carpeta compartida (E:).
- **Bases:** de partida, `A-2025-2026.db` (md5 `ba0db782…`, esquema 0). Para generar, `B-2026-2027.db` (md5 `38b81f23…`, esquema 8), copiada a la carpeta de datos como `curso-humo-b.db`.

## Resultado
- **Instalación por el punto 8:** los pasos 1, 2 y 4, sin incidencias; el zip solo tiene el flujo `:$DATA`. Paso 3: ver «Defecto del procedimiento».
- **Ventana de arranque:** aparece a los 2-3 s con su texto y se cierra al abrirse el navegador, a los 22 s. Ningún diálogo de seguridad.
- **Segundo lanzamiento:** no sale la ventana, se abre una pestaña nueva y queda un solo icono.
- **Versión:** «versión 0.4.0-rc.1» bajo la marca y en el log.
- **Migración:** «Esquema preparado», de 0 a 8.
- **Duplicado:** 2025/2026 queda archivado (`educhronos.db`) y 2026/2027 activo (`curso-2026-2027.db`).
- **Generación:** con el tiempo por defecto (10 min), horario 4 FEASIBLE: objetivo 227, 830 sesiones, 589 s. El diagnóstico de la propia aplicación da 0 violaciones duras y 0 de guardia.
- **Descargas:** CSV y PDF de grupo, de profesor, de aula y de guardias. Las cinco pasan el oráculo de exportación, sin entradas que falten ni que sobren.

## Defecto del procedimiento
En el paso 3, PowerShell no acepta `"…en $datos: no…"` (`InvalidVariableReferenceWithDrive`). La guarda no se ejecuta y, si hubiera una base previa, el `Copy-Item` siguiente la sobrescribiría (no medido). En el humo no había base previa y el resultado fue el esperado. Corregido en `2a0e6fa` con `${datos}`. No hay `pwsh` en el anfitrión, así que la corrección no se ha pasado por el analizador de PowerShell: se verifica en la condición 3.

## Incidencias del entorno
- El anfitrión Linux se suspendió durante la espera del primer intento de generar (22:46) y hubo que reiniciarlo. La generación aún no había empezado. Para la prueba, el anfitrión debe estar configurado para no suspenderse.
- Tras el reinicio, Windows mostró una pantalla de actualización: la instantánea `s187-demo-lista` no tiene esas actualizaciones.
- En el primer arranque de la cuenta, Edge tapa la aplicación con sus pantallas de bienvenida.
- `Get-ChildItem` sobre E: falla por los `Zone.Identifier`, igual que en `acta-demo-bundle.md:184-186`. Los cinco ficheros llegaron con el mismo md5.

## Respuestas del usuario
1. «Tras el corte no salió pantalla de recuperación de Windows. Salió de actualización.»
2. «Confirmar la generación del horario no mostró avisos, o al menos ninguno importante que recuerde.»
3. «No vi celdas en rojo.»

## Material
`/home/luis/educhronos-aceptacion/s234/vm/` (`MANIFIESTO.sha256`, 36 ficheros) y `/home/luis/educhronos-aceptacion/s234/doc/`.
