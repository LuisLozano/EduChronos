# Acta de C-base-partida (S241) — condición 6 de O-uso-centro

- **Código:** e1ad229 feat: script de base de partida con el curso nombrado, con test (C-base-partida, S241).
- **Origen:** `partida-original.db` (`/home/luis/educhronos-aceptacion/s239/d3/trabajo/`), md5 citada en el acta de S239.
- **Resultado:** `/home/luis/educhronos-aceptacion/s241/f1/base-partida/base-partida-2025-2026.db`. Es la base que viaja en `Educhronos\base-partida\`, sola.

## Corrida del script
```
origen  : /home/luis/educhronos-aceptacion/s239/d3/trabajo/partida-original.db
md5     : ba0db78259de85c819fe507cbf0c2496
destino : /home/luis/educhronos-aceptacion/s241/f1/base-partida/base-partida-2025-2026.db
md5     : d867c2dec4a9ab01357f539d8b09f7e2
curso   : 2025/2026, archivado 0, user_version 0, 22 tablas
HECHO
```
Huella de la base entregable (`md5sum`): `d867c2dec4a9ab01357f539d8b09f7e2  /home/luis/educhronos-aceptacion/s241/f1/base-partida/base-partida-2025-2026.db`

## Suite y mutantes
- `tools/base-partida/tests`: Ran 9 tests in 0.013s.
- Mutantes sobre copias: m1 cazado (sin la guarda de fila existente): 1 casos rojos; m2 cazado (archivado 1): 4 casos rojos; m3 cazado (años no consecutivos aceptados): 1 casos rojos.
- Cómo cae cada mutante: m1 por `IntegrityError` (sin la guarda, el `insert` choca con la clave primaria de `curso`), m2 por la `RuntimeError` de `verificar` antes de los asertos, m3 por el aserto de `test_1`. Los tres mueren; solo m3 lo caza un aserto del spec.
- Suite de `tools/carga-centro/tests` con el mismo runner: Ran 284 tests in 2.248s.

## Verificación con la aplicación (Linux, sobre una copia)
- Arranque: `python3 -B /home/luis/desarrollo/educhronos/scripts/arrancar-backend.py arrancar --base /home/luis/educhronos-aceptacion/s241/f1/verificacion/educhronos.db --registro /home/luis/educhronos-aceptacion/s241/f1/verificacion`
- `GET /api/curso`: `{"nombre":"2025/2026","archivado":false,"propuestaSiguiente":"2026/2027"}`
- Log: `2026-10-20T19:57:08.439+02:00  INFO 695631 --- [           main] e.y.e.app.config.PreparadorEsquema       : Esquema preparado ruta=/home/luis/educhronos-aceptacion/s241/f1/verificacion/educhronos.db versionInicial=0 versionFinal=8 baseVacia=false`
- La copia, tras cerrar: `user_version 8 · curso [(1, '2025/2026', 0)]` (la fila sobrevive a las migraciones 1 a 8).
- La barra pinta `curso.nombre` (`curso-barra.ts:71`, spec `curso-barra.spec.ts`); la barra real, en la sección siguiente.

## Decisiones
Del arquitecto: script en `tools/base-partida/` (precedente `tools/carga-centro/`); copia de bytes y no `VACUUM INTO`; sin migrar (lo hace la aplicación al abrir); rechazo si ya hay fila (CJ); la base no se versiona ni entra en el zip: viaja en el USB y la copia quien entrega (BS).

## Verificación en la VM (F3, `s241/f3`)
- Cuenta estándar nueva `inst241` sobre `s239-prueba-hecha`, sin restaurar instantánea y sin instantánea al terminar (CQ).
- Zip de `v0.5.0-rc.4` descargado de la Release y verificado en el host y en la VM: `03d64d42e48368019a194444d53f3e00aa26434b08e64b22f7458ee24fc2754e`.
- Base copiada a mano en `base-partida\` (BS), md5 antes de instalar `d867c2dec4a9ab01357f539d8b09f7e2` y `educhronos.db` instalada `d867c2dec4a9ab01357f539d8b09f7e2`.
- `Instalar.cmd` desde PowerShell con transcripción (`vm/instalar.txt`): copia la base y termina bien.
- Barra: Barra de Educhronos en la VM (cuenta inst241, 2026-10-20 ~21:10), transcrita por el arquitecto de la captura … «2025/2026», «versión 0.5.0-rc.4», sin «Solo lectura» (`barra.txt`).
- Log: `C:\Users\inst241\AppData\Local\Educhronos\educhronos.log:14:2026-10-20T21:03:39.737+02:00  INFO 6032 --- [main] e.y.e.app.config.PreparadorEsquema       : Esquema preparado 
ruta=C:\Users\inst241\AppData\Local\Educhronos\educhronos.db versionInicial=0 versionFinal=8 baseVacia=false`.
- Carpeta de datos al cerrar: `educhronos.db`, `educhronos.lock` y `educhronos.log`, sin `curso-abierto` (`vm/datos-despues.txt`).
- Lectura: los `.txt` de PowerShell van en UTF-16LE; la primera corrida de `grep` dio falsos «NO casa» (`oraculo-falso-grep-utf16.txt`); el oráculo bueno, sobre copias en UTF-8 (`oraculo.txt`).
