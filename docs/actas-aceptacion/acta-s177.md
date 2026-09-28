# Acta de aceptación — S177

Corrida de la cadena de `docs/guion-aceptacion.md` sobre el bundle que construye la CI, para la condición 4
de `O-ci`, con la actualización de §5 bis. Plantilla de §8 del guion.

## ACTA DE ACEPTACIÓN — Educhronos

- **Fecha:** 2026-09-28.
- **Ejecutor:** Luis (usuario), por pantalla, guiado paso a paso por el arquitecto; las capturas se revisaron
  al cerrar cada paso.
- **Máquina (modelo / VM) y edición de Windows (versión y compilación):** máquina virtual VirtualBox de S163
  (host Linux), Windows 11 Pro, versión 10.0.26200.9457.
- **Cuenta estándar sin administrador:** sí. Cuenta nueva `prueba4` (`whoami /groups` sin S-1-5-32-544).
- **Java y Node ausentes antes de instalar:** sí. `where.exe java` y `where.exe node` sin resultado;
  `JAVA_HOME` vacío; sin `C:\Program Files\Java` ni `C:\Program Files\nodejs`; sin `msvcp140`,
  `vcruntime140` ni `vcruntime140_1` en System32; sin `%LOCALAPPDATA%\Educhronos` previo.
- **Tag y Release (tag y URL de la Release):** `v0.1.0`,
  https://github.com/LuisLozano/EduChronos/releases/tag/v0.1.0 — Release definitiva, publicada el
  2026-09-28 a las 07:14:14Z por la ejecución 36390351012 de `bundle` (runners `ubuntu-24.04` y
  `windows-2025-vs2026`).
- **Commit aceptado:** `a7846b69146fc64f57a14060da99eb00f569ea44` (notas de la Release); `tests` en verde
  sobre él (ejecución 36388998261).
- **sha256 del jar (extraído del zip; igual a la huella del log del job `linux`):**
  `90606e507e86b3a5a2f67d800eb0311f2eb2909920f1c68f55a226bb21317e9a`, igual también a la `HUELLA_JAR` del
  job `windows`; las dos leídas por el usuario en la web.
- **sha256 del zip del bundle:** `7afc5b8c97e2eb8f2484c612cfdab72cf694b01deb5cecb82fdf04131c9fb170`,
  176.078.736 B: el del asset `.sha256`, comprobado en Linux (`sha256sum -c`), en el pendrive y en Windows
  (`Get-FileHash`) en `prueba4` y en `prueba2`.
- **Transporte:** pendrive conectado a la máquina virtual por USB. En las dos cuentas el zip llega sin
  `Zone.Identifier` (sólo `:$DATA`).
- **Ids de horario:** generado en el paso 3 = 1; regenerado en 4d (final) = 2; ninguna regeneración de más.
  El curso nuevo, sin horario.

| Paso | Resultado (PASA/FALLA) | Oráculo aplicado | Capturas | Observaciones |
|------|------------------------|------------------|----------|---------------|
| 1 Instalar | PASA | pantalla + carpeta de datos | 1a 1b | Sin ningún diálogo de seguridad ni de credenciales. La carpeta de datos tiene `educhronos.db`, `educhronos.log` y además `educhronos.lock`, el de la instancia única (S154); el guion dice «sus dos ficheros». |
| 2 Crear el centro | PASA, con desviación de ejecución corregida antes de cualquier oráculo | listas + prevalidación + §7.2 | 2a–2h, 2g-antes, 2h-antes, 2g-1 a 2g-8 (2g-5a, 2g-5b, 2g-6a, 2g-6b, 2g-6c) | Al revisar 2g se vieron tres actividades mal tecleadas: ACM-3ºA-Di con 1 repetición, ING-3ºA con asignatura ING y el código `REVAL-3ºAB`. Se corrigieron con «Editar» antes de la prevalidación, y las ocho se verificaron campo a campo. Las plazas de RELVAL conservan `REVAL-3ºAB-P1/P2`: el código de plaza es estable por diseño (`ActividadService`, Javadoc de la edición) y ningún oráculo de §7 lo mira (medido en §7 y en `scripts/`). §7.2 pasa entero. |
| 3 Generar | PASA | diagnóstico en navegador + §7.3 | 3a–3d | Horario 1 `OPTIMAL`, objetivo 0.0, 24 sesiones, 0 violaciones, en pantalla y en §7.3. TEC-3ºB en martes 1–2, sin cruzar el recreo. |
| 4 Ajustar (a, b, c, d) | PASA | pantalla + diagnóstico + §7.3 | 4a-1 4a-2 4a-2d 4b 4c 4d-1 4d-2 4d-3 | 4a: LEN-3ºB #2 de J4 a V6, sin insignia antes de F5 y con ella después (defecto conocido); `INDISPONIBILIDAD_BLANDA` en V6 con delta 1. 4b: ING-3ºA a L2, rechazo con la única línea `INDISPONIBILIDAD_PROFESOR — P3 en L2`. 4c: TEC-3ºB a M3, rechazo con la única línea `BLOQUE_IMPOSIBLE — TEC-3ºB #1 en M3`. 4d: una sola regeneración, horario 2 `OPTIMAL`, objetivo 1.0, totales 0/0/1 y única blanda con delta positivo la de V6; `sesion_bloqueada` 1. Los dos avisos desfasados de `D-vista-horario-estado-rancio`, como el guion declara. |
| 5 Exportar | PASA | §7.4 (siete rc=0) | 5a 5b | Cuatro descargas sin sufijo. CSV 29/29, 25/25, 25/25; PDF grupo 29, profesor 25 y aula 25, «FALTAN 0, SOBRAN 0»; leyendas 3/4/3 sin fallo. El CSV se abrió con el Bloc de notas porque la máquina no tiene Excel (Excel, verificado en S156); lleva `REVAL-3ºAB-P1/P2` en la columna Plaza. |
| 6 Duplicar | PASA | pantalla + §7.5 | 6a 6b 6c 6d-1 6d-2 6e | No se generó en ningún curso. 17 tablas de configuración sin diferencias; curso nuevo con `horario_generado`, `sesion`, `sesion_bloqueada` y `aula_bloqueada` a 0; `2026/2027\|0` y `2025/2026\|1`; puntero → `curso-2026-2027.db`; rechazo de solo lectura visto en pantalla. |
| 5 bis Actualizar (cuenta `prueba2`) | PASA | §7.6 + huella del jar instalado | 7a 7b 7c-1 7c-2 7d | Instalación de S172 en `C:\Users\prueba2\Documents\Educhronos-win\Educhronos`, sustituida por la de `v0.1.0`. Los mismos cursos y el mismo abierto antes y después; los `.db` de `antes` y `despues`, idénticos al byte; el curso activo genera el horario 2 sin violaciones y el archivado no cambia. Que corría el programa nuevo lo prueba 7d: el jar instalado es `90606e50…17e9a` (el de S172 era `7b65bd48…`). |

**Veredicto de la cadena: PASA.** **Veredicto de la actualización (§5 bis): PASA.**

Sin salvedades. La desviación del paso 2 es de tecleo, se detectó al revisar sus capturas y se corrigió por la
interfaz antes de aplicar ningún oráculo; todos los oráculos pasan tal como están escritos.

## Intento anulado (cuenta `prueba3`)

Primer intento, anulado antes de que el producto llegara a ejecutarse. El zip se copió a Windows desde la
carpeta compartida de VirtualBox (`E:`, `\\VBoxSvr`), por indicación del arquitecto y en contra de la guía,
que dice USB. Llegó con `Zone.Identifier` y `ZoneId=3`; al extraerlo con el Explorador la marca pasó a
`Educhronos.exe` (`ReferrerUrl` = el zip), y SmartScreen detuvo el arranque con «Windows protegió su PC»; se
pulsó «No ejecutar». La corrida se repitió desde el paso 1 en otra cuenta nueva, `prueba4`, con el zip llegado
por USB y comprobado sin marca. Las mediciones constan en el chat de S177; `prueba3` conserva su estado.

## Observaciones

- La aplicación no muestra su versión, y el zip y `Educhronos.exe` tampoco la llevan: la identidad la dan el
  tag y el sha256 de la Release (`D-version-invisible`, mejora futura).
- La prueba de actualización no ejercita migración: `schema.sql` es idéntico en `8d9a74a` y en `a7846b6`
  (`D-esquema-sin-version`).
- El zip usa `\` como separador de rutas; el Explorador de Windows lo extrae bien.
- En `prueba2`, `curso-abierto` cambió de fecha pero no de contenido al abrir el archivado y volver al activo.
- En `prueba2` el navegador abrió su pestaña de novedades la primera vez; no es una pestaña de la aplicación.

## Material (fuera del repo)

- `/home/luis/educhronos-aceptacion/s177/`: `Educhronos-win.zip` y su `.sha256` descargados de la Release,
  y el jar extraído del zip.
- `…/s177/prueba4/`: `datos/`, `descargas/`, `capturas/`, `oraculos/` (7.1 a 7.5) y `MANIFIESTO.sha256`.
- `…/s177/prueba2/`: `antes/`, `despues/`, `final/`, `oraculos/7.6.log` y `MANIFIESTO.sha256`.
- En la carpeta compartida con la VM, `/home/luis/educhronos-vm/v0.1.0/`.

### Capturas

Identificadas por su sha256, tomado de `prueba4/MANIFIESTO.sha256`.

| Fichero | sha256 |
|---|---|
| capturas/1a.png | `177dbf9aef0233c1dd346e1090c2a9a2d71976de2c6d2159039464836ed4a68b` |
| capturas/1b.png | `4fb25da54f7e6b1df799e1edc90481eb924fdc04e98deecab74c5a1a4874d0a1` |
| capturas/2a.png | `ded1fa7fd22b9f5aa9c90cd2f5f28f4f34537db98dd18d9bee7371dee908ef92` |
| capturas/2b.png | `a753d850e903d44b6769497213a4bc271adf79660dd08b43b21e6fe214bcef43` |
| capturas/2c.png | `7b0583245b900a96ddaeffc087b8a66047653b57cbedfb678f065daa643d347a` |
| capturas/2d.png | `92f2df8a3c3a4a7bc11db45ff705de67d048a29ba52858b99139a9b59fffd56c` |
| capturas/2e.png | `25f9a38ebeaf7242b5f0035daa758801c588c656d80c91fda8369cbd6ad6c1a3` |
| capturas/2f.png | `d3f6236c8b5eca6fc7adb77fdc29733b4d9e1730d9219e935e09f0ce807b62fc` |
| capturas/2g-1.png | `7b7a4bff0f7b5dfe99ee69422508ed9c85f3495f8113e0ce80a22681a52b0627` |
| capturas/2g-2.png | `f4b155ce6ddb39c76f67c7f8e3fda25c9193d2e3579c4a61c9b9cd098d56a93b` |
| capturas/2g-3.png | `5920f7cd4dd6bc6fd836b79efb3ee4eeaa287f75699aeff752bee99149748b5f` |
| capturas/2g-4.png | `95f5ece3cd49a5ea58d6dd044c2e5cf6b186bb00fab9960fb83d95acd6edec18` |
| capturas/2g-5a.png | `2ca3b80016e34bc805b2fe0e69a28b2a0892fc312a218194f715798ee8d1c3a5` |
| capturas/2g-5b.png | `0e01382811fe3248e96d774be509cfed0491cc8b66b67b44d06febf37e30dc8d` |
| capturas/2g-6a.png | `9405a669791ebc8e6ec0580e5c422d727d4f54c30bfe01db5dd44f32f90966dd` |
| capturas/2g-6b.png | `bf2b9d59c22984cd3bcfdf08f7e933b1d1447c4e32ecfd7161813eb2045b9352` |
| capturas/2g-6c.png | `b2b269e06fd901c0a65bdfce48a40870a5ae339657c8feb70b684ec9f0f8dfde` |
| capturas/2g-7.png | `a367555eab7e0e3fb20eba071d20fa47abc07e88803a96256ea690836ba618bc` |
| capturas/2g-8.png | `6f7fca373697f95dcc1a5b06ffff628cbb8bfd298ecc47f9abc737525b9713bf` |
| capturas/2g-antes.png | `6aaa63c300d4defe11dd410cc99562afbf7bba0e285c58c3f8dc3d4ee9d5ad1e` |
| capturas/2g.png | `eee56c08156dcd3ef377aeaa57e20709ac1821fec8a3dcde0fa9af3459d4fa36` |
| capturas/2h-antes.png | `a6b394ec756d3bb8ce9a35c8f7fde1f34129a9237162ede917901d1dee9b46dc` |
| capturas/2h.png | `dfe9755575fe980cb08d5bfb211f0598283e66e86317fe5ec0c37a27b133c08c` |
| capturas/3a.png | `03ccce596ebb52e6799afd0f621146752db2aaa1557d4806944d8eb0b83e9a94` |
| capturas/3b.png | `61c7c6e868908de0e48a3b6cddbe34aa5bfcb98b69a5ff3ea58a0c61a424ffb9` |
| capturas/3c.png | `c25584e0c63196c7a67f6836d01ff6fd6cbb816e4c5a877478894a38dad565dd` |
| capturas/3d.png | `3493cd8918276d4a78e63c93e994f541eb5c0c78dc85626285e8fa206a3cb6f2` |
| capturas/4a-1.png | `a79cbba93f52b28bae20d80ca0a5bfce498b19576bd6c0635c521cc76ffe7832` |
| capturas/4a-2d.png | `ec93a6592383e2c1827319d7edc63feb82ed2a97a62a48ff71ca7c2e467fd413` |
| capturas/4a-2.png | `5af3bf51db5427d26b2fbec78373f0548de01f92d729b11aa9c9ae34a8b1cd65` |
| capturas/4b.png | `6705ebfa0a06153f3cfc0f3e7694809920196583949115b10b5e7b1eeaddb771` |
| capturas/4c.png | `76bfb5855a0a5b717d12da5f142fd38ed782c9d9eed9088926a27de1b99f9932` |
| capturas/4d-1.png | `fcc53c85ea146edee3f9d0f6a9d0298fa27679c581f587434a927b3c5a8a4c8c` |
| capturas/4d-2.png | `34e62ef762d486b9f106ce1c3922d626fb3ab3114bd88e8b2c38a2167131efd7` |
| capturas/4d-3.png | `1f773a980cc52061fd486aca361d22dceed81053483fe9098b41eb27748cae82` |
| capturas/5a.png | `008b42e2fae05b1c65f4599cc09ae921a29d3e7e397e672894cdab2b04c1f026` |
| capturas/5b.png | `a11997c954753090fb203802822b0a41c87d03ab360557c6722eae01650a9263` |
| capturas/6a.png | `e8640bcb09c941628de69039ffdef4439bf6ff1a9e4378ab9e3c05ee52d0c080` |
| capturas/6b.png | `7e281d8b6fcea0be04eb689deede71b6fe1a88db7536c356f354a950738911fd` |
| capturas/6c.png | `9e6443a9454fa93b83449c462310ee4401977d66cede06e4928928c3fdf1b634` |
| capturas/6d-1.png | `79edd189d3b78935f233053e4cabecbcf343e6fb9d0b16ed443196d744230bae` |
| capturas/6d-2.png | `9480c6724912bdf715b6570237531033c4d40ff3366caa3aa77a1a06338240a5` |
| capturas/6e.png | `7e23f31b11d5d1f70560ad9336e9aa9292c2fe82c61de591e8a0ebbe2e3a4dd9` |
| capturas/7a.png | `29402b641b4919e30525b910370237db8b937c2e797d82ce59237ded4ac4a726` |
| capturas/7b.png | `af8e06f5a15297bcd1e0119818dbba26c7f2d6b1ef4d64f8a69949a7cea90bbb` |
| capturas/7c-1.png | `2faac75ba3d6bf0cc82d5e17147266244caf61f6ab8f1e8969726d8ee6b550e7` |
| capturas/7c-2.png | `9efb425437b5a723642c024215099ff7ab4e1528bc1c80f83d41bde3e1522439` |
| capturas/7d.png | `72a649569097a224542d038e68173eb0c4f34ef232926d77630387d0e99a2c0f` |
| capturas/Thumbs.db:encryptable | `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855` |
