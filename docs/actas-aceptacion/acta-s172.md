# Acta de aceptación — S172

Corrida de la cadena de `docs/guion-aceptacion.md` en Windows limpio, para la condición 4 de
`O-aceptación`. Plantilla de §8 del guion.

## ACTA DE ACEPTACIÓN — Educhronos

- **Fecha:** 2026-09-25.
- **Ejecutor:** Luis (usuario), por pantalla.
- **Máquina (modelo / VM) y edición de Windows (versión y compilación):** máquina virtual VirtualBox de
  S163 (host Linux), Windows 11 Pro, versión 10.0.26200.9457.
- **Cuenta estándar sin administrador:** sí. Cuenta nueva `prueba2`, que no está en Administradores
  (`whoami /groups` sin S-1-5-32-544).
- **Java y Node ausentes antes de instalar:** sí. `where.exe java` y `where.exe node` no encuentran nada;
  sin `JAVA_HOME`; sin `C:\Program Files\Java` ni `C:\Program Files\nodejs`. Tampoco están `msvcp140`,
  `vcruntime140` ni `vcruntime140_1` en System32. Sin `%LOCALAPPDATA%\Educhronos` previo.
- **Commit aceptado:** `8d9a74a2200c0953c7a7da995af39d5f8b0d0b12`. Sin cambios de código desde
  `4c07553`; lo cubren las suites de S170.
- **sha256 del jar (-HuellaJar):** `7b65bd487489346c82e474fbcf17277dca7559dd029cfdd12e159ff8b2b0a57d`.
- **sha256 del zip del bundle:** `76e363d87c3b316361554ff089dce2e57df1380fef65230f914196566bf28566`, el
  mismo en la cuenta de administrador y en `prueba2` (certutil). Carpeta del app-image: 231.770.158 B
  (condición 2: CUMPLE).
- **Ids de horario:** generado en el paso 3 = 1; regenerado en 4d (final) = 2; otros, si se regeneró de
  más = ninguno en el curso archivado. En el curso nuevo, horario 1, generado FUERA del guion (ver
  paso 6).

| Paso | Resultado (PASA/FALLA) | Oráculo aplicado | Capturas | Observaciones |
|------|------------------------|------------------|----------|---------------|
| 1 Instalar | PASA | pantalla + carpeta de datos | 1a 1b | Sin diálogos de seguridad ni de credenciales; portada y carpeta de datos. |
| 2 Crear el centro | PASA con desviación de ejecución | listas + prevalidación + §7.2 | 2a–2h | El PDC 3ºA-Di se creó ANTES de asignar tutor a 3ºA, no heredó y se le puso P1 a mano; la acción 9 (herencia) no se ejerció en esta corrida (sí en el ensayo de Linux de S171). §7.2 pasa entero, con 3ºA-Di → P1 `TUTOR_PRINCIPAL`. P2 y P4 se dieron de alta como «Profesor Dos» y «Profesor Cuatro» en lugar de «Profesora Dos» y «Profesora Cuatro» (§4); §7.2 no mira los nombres. |
| 3 Generar | PASA | diagnóstico en navegador + §7.3 | 3a–3d (3b: discrepancia, ver tabla de capturas) | Horario 1 `OPTIMAL`, objetivo 0.0, 24 sesiones, 0 violaciones, en pantalla y en §7.3. |
| 4 Ajustar (a, b, c, d) | PASA | pantalla + diagnóstico + §7.3 | 4a-2 4b 4c 4d (4a-1 falta; 4a-2 y 4d: discrepancia, ver tabla de capturas) | 4a: LEN-3ºB #3 a V6, insignia solo tras F5 (defecto conocido). 4b: ING-3ºA del miércoles a L2, rechazo con la única línea `INDISPONIBILIDAD_PROFESOR — P3 en L2`. 4c: TEC-3ºB a M3, rechazo con la única línea `BLOQUE_IMPOSIBLE — TEC-3ºB #1 en M3`. 4d: una sola regeneración, horario 2 `OPTIMAL`, objetivo 1.0, 0 violaciones, única blanda con delta positivo la de LEN-3ºB #3 en V6; `sesion_bloqueada` 1. Los dos avisos desfasados de `D-vista-horario-estado-rancio`, como el guion declara. |
| 5 Exportar | PASA | §7.4 (siete rc=0) | 5a 5b | Cuatro descargas sin sufijo (sha256 en «Material»). §7.4 con los siete rc=0: CSV «OK: las tres vistas coinciden» (grupo 29/29, profesor 25/25, aula 25/25); PDF grupo 3ºA 14, 3ºA-Di 3, 3ºB 12, «TOTAL: halladas 29, FALTAN 0, SOBRAN 0»; PDF profesor P1 6, P2 6, P3 6, P4 7, «TOTAL: halladas 25, FALTAN 0, SOBRAN 0»; PDF aula A1 10, A2 8, LAB 7, «TOTAL: halladas 25, FALTAN 0, SOBRAN 0», «páginas vacías con leyenda: 0»; leyenda «páginas OK: 3 / 4 / 3   con fallo: 0». El CSV se abrió con el Bloc de notas porque la máquina no tiene Excel; la apertura en Excel se verificó en S156. |
| 6 Duplicar | FALLA en §7.5 tal como está escrito | pantalla + §7.5 | 6a–6e (6d-1: discrepancia, ver tabla de capturas) | El duplicado es correcto: 17 tablas de configuración sin diferencias, `curso-abierto` → `curso-2026-2027.db`, nombres `2026/2027\|0` y `2025/2026\|1`, rechazo 403 en el archivado visto en pantalla. Pero el curso nuevo tiene `horario_generado` 1 y `sesion` 24, donde el guion exige 0 y 0: el ejecutor pulsó «Generar horario» en el curso nuevo después de ver el mensaje «Este curso todavía no tiene horario. Créalo con «Generar horario».», que no quedó capturado (la captura 6d-1 muestra la rejilla generada). Cronología (D1): duplicado 16:06:19, apertura 16:08:18, generación 16:08:45, captura 16:08:49. Indicios de que el curso nació sin horario: el horario nuevo es el id 1, `OPTIMAL` con objetivo 0.0 y sin pines (el 2 del archivado tenía objetivo 1.0 y un pin), y `sesion_bloqueada` y `aula_bloqueada` están a 0. |

**Veredicto de la cadena: PASA CON SALVEDAD, POR DECISIÓN DEL USUARIO.**

Salvedades: (1) Paso 6: el oráculo §7.5 falla tal como está escrito (curso nuevo con
horario_generado 1 y sesion 24, por una generación hecha a mano tras ver el mensaje, que no
quedó capturado). (2) Evidencia incompleta (regla 4 del guion): 3b muestra 3ºA en lugar de 3ºB;
falta 4a-1; 4a-2 no muestra la celda tras F5; 4d no muestra la proyección ni el diagnóstico del
horario 2. Los resultados de los pasos 3 y 4 los confirma el recálculo de §7.3, y la insignia de
4a tras F5 se ve en la captura 4c. (3) Desviaciones de tecleo y orden en el paso 2, sin efecto
en §7.2.

El usuario da la corrida por válida: vio en pantalla el resultado esperado del paso 6 y el producto
no presentó ningún fallo. El arquitecto recomendó repetir la cadena entera con una cuenta nueva,
porque el oráculo §7.5 falla tal como está escrito, el resultado del paso 6 sólo consta por
testimonio y cuatro capturas faltan o no muestran lo que exigen.

## Observaciones

- Prueba de humo del empaquetado NO DISCRIMINANTE, como es de esperar (`docs/empaquetado.md` §6): sobre
  base vacía, `POST /api/horarios` da 422 `CONFIGURACION_INCOMPLETA` antes de tocar el solver.
- En §7.3 el horario 1 lleva `totales` ventanas 1 e indispBlanda 1, por el arrastre de 4a sobre ese
  horario; el guion no lo exige.
- En `datos/` hay además `educhronos.lock`, de 0 B.
- Se abrió Edge una vez con `prueba2` antes del paso 1 para saltar su asistente de primer uso.
- El tutor de un grupo no se propaga a un PDC ya existente (conocido desde S171, sin deuda).
- Avisos del build de la fase A: falta la versión de `maven-resources-plugin`, deprecación en
  `ProyeccionDtoContratoTest` y presupuesto inicial de Angular superado (589 de 550 kB). Sin comprobar
  si son anteriores.

## Material (fuera del repo)

Carpeta de la sesión: `/home/luis/educhronos-aceptacion/s172/`.

- `app-0.1.0-SNAPSHOT.jar`: el jar entregado, sha256
  `7b65bd487489346c82e474fbcf17277dca7559dd029cfdd12e159ff8b2b0a57d`.
- `empaquetar-linux.log`: la construcción de la entrega (fase A).
- `datos/`: la carpeta de datos traída de Windows (`educhronos.db`, `curso-2026-2027.db`,
  `curso-abierto`, `educhronos.log`, `educhronos.lock`).
  - `educhronos.db`: `9db95190a832d0e264e0be3395ac3befccf3aea3175504416ba31d5af29e3de4`
  - `curso-2026-2027.db`: `062d17089f07ea284940aa03d8dc9a791944dde427c7149f80a3fce488680744`
- `descargas/`: las cuatro descargas del paso 5.
  - `horario-2.csv`: `31961a338caf4524f3b4187dfd0c856c48254eb16d4e73f1b6ef9805f8a1b4fc`
  - `horario-2-grupo.pdf`: `820e469f99ccbeba62b97d51dfd7f387fc920deeca4a72d6c07cc45dadffe6f8`
  - `horario-2-profesor.pdf`: `68d76ac05046d87255895d284ecd649bdaaa788b71dff178c4d28439eccf830d`
  - `horario-2-aula.pdf`: `8c14a506c96ca38dbf1506f17dab27e696e20e677e86f9668e135dfd23de9d60`
- `capturas/`: las 26 capturas de la tabla siguiente.
- `oraculos/`: §7.1 a §7.5, cada bloque en `7.N.sh` con su salida en `7.N.log`; las copias de las
  bases, `recalculo.log` y las respuestas `proy-1.json`, `proy-2.json`, `diag-1.json` y `diag-2.json`.

Transcripción de Windows: `empaquetado-completo-20260925-111010.txt`, en la carpeta compartida
`/home/luis/educhronos-vm/bundle/` (`E:\bundle` en la VM), junto al `Educhronos-win.zip` que se probó.

### Capturas

Identificadores de §5 sin fichero: **4a-1**. Ficheros que no casan con ningún identificador: ninguno.

| Fichero | Id | mtime | sha256 | Comprobación |
|---|---|---|---|---|
| 1a.png | 1a | 2026-09-25 11:22:23 | `db75d5ac63113b55af9587026a4ef0d14a454d40c2cbb5d52c2f632dbe229bd0` | casa |
| 1b.png | 1b | 2026-09-25 11:23:41 | `bb80bb2ca93a81df7a3604013568eb3119d0dcf327f1ddea336a3e8ceb64fc00` | casa (además de `educhronos.db` y `educhronos.log` se ve `educhronos.lock`) |
| 2a.png | 2a | 2026-09-25 11:25:03 | `cd9644a49fd2998e51907b41fce0d5d0da7839ec9899af7f0e60120376e469f1` | casa |
| 2b.png | 2b | 2026-09-25 11:30:21 | `d50bcde9bdb7ca99a7e333f774887df035a955af5c36d8c04e51a44ee86bcac0` | casa |
| 2c.png | 2c | 2026-09-25 11:33:21 | `a3d3af301ad2e8a505d63230a3b5bf87a3df30f470c53d94886c9d607c83531c` | casa (P1 puesto a mano, ver paso 2) |
| 2d.png | 2d | 2026-09-25 11:55:50 | `1f312d548586703d17f914494862da3be7e791f121d179a9374b6bea4cdd7ac9` | casa |
| 2e.png | 2e | 2026-09-25 11:56:32 | `26fb4d1260c8ee44af16fc56301021e14653d0a5f714c0abe9d8d72e84fb7952` | casa |
| 2f.png | 2f | 2026-09-25 11:57:20 | `3d91abdf90c65cfb5a7ea3d069d06bca5aef92457df1a086e0aa36f96a4f3800` | casa |
| 2g.png | 2g | 2026-09-25 14:18:41 | `0f5fef2d9d28559870584d75924e45dd8d1b6f20711f7216ae8ccea3e3e3ba32` | casa |
| 2h.png | 2h | 2026-09-25 14:19:04 | `916f8f26edb7debb08c4efa454b5d3d441398e4a7f6c3227f5f522b2249f06b2` | casa |
| 3a.png | 3a | 2026-09-25 14:23:08 | `536170959c5e56677d4e2d1f8990567a6766715e7df60a38e84718b0007adf09` | casa |
| 3b.png | 3b | 2026-09-25 14:27:00 | `626df3a7d06129c364070eac8d75675d251db549eeab546ab746f7f6ddf4e603` | discrepancia: muestra la rejilla de 3ºA, no la de 3ºB, y no se ve TEC. TEC-3ºB en dos tramos seguidos del horario 1 se ve en 4c (martes, tramos 1 y 2) |
| 3c.png | 3c | 2026-09-25 14:24:32 | `adb75f17bc135abacc489831983a87b21091d6e834248c569e683fbfec13bd16` | casa |
| 3d.png | 3d | 2026-09-25 14:26:01 | `f8e4eae34c69db2d6ef154b4a25364217d246556058a3b5c3e7f6f77546d8262` | casa |
| 4a-2.png | 4a-2 | 2026-09-25 14:33:56 | `fb1dbd3a940689e7fba77ac07ef165bf8a7160e0f5a98003bb29e444780b1ad9` | discrepancia: muestra solo el diagnóstico con la penalización, no la celda tras F5 con la insignia. La insignia de LEN en V6 se ve en 4c (vista de 3ºB) |
| 4b.png | 4b | 2026-09-25 14:37:02 | `83a9c28bfe856bd4269872b79a03f2d9b87755438cc62bbce496fbe847e24acb` | casa (`INDISPONIBILIDAD_PROFESOR — P3 en L2`) |
| 4c.png | 4c | 2026-09-25 15:03:12 | `9858a4710981a3344c8707bc1a97ce9430dd6000697520a835b647eb08227aca` | casa (`BLOQUE_IMPOSIBLE — TEC-3ºB #1 en M3`) |
| 4d.png | 4d | 2026-09-25 15:18:18 | `7a7c5aea47a0856df2b41b1af317dff4a2f5a0e65d439ff841ada7b3eb13da09` | discrepancia: muestra la rejilla del horario 2 con LEN fijada en V6 y los dos avisos desfasados, pero no la proyección ni el diagnóstico del horario final (cubiertos por §7.3) |
| 5a.png | 5a | 2026-09-25 15:20:35 | `d50e6d5673d0ce80d6ad04a05990f472c2212ea14397d57ebe774c6ef659b19a` | casa (Bloc de notas, ver paso 5) |
| 5b.png | 5b | 2026-09-25 15:24:55 | `b5d5eaf5e373116c205b3e6567e983fdc7cf75982fcb67df3ca19f73ea6c2860` | casa |
| 6a.png | 6a | 2026-09-25 16:06:01 | `b7e0ebb842e187e0a45edacddf07acbc5a5a36c12d22a6622fb81acb956360b2` | casa |
| 6b.png | 6b | 2026-09-25 16:06:33 | `0b8fb0a2967159ed0fd32682eb566a18f619b9c080dcfa7818c4ed23fa6fb8cc` | casa |
| 6c.png | 6c | 2026-09-25 16:07:35 | `cae53517987bd409f89889e66685a194c9b2450b2508f0e39a61a324a5bac769` | casa |
| 6d-1.png | 6d-1 | 2026-09-25 16:08:49 | `de5280c7a3cf3cb1e0576cd557fc25f3590d03b7144fb157983acef1d08bfe6a` | discrepancia: muestra la rejilla del horario 1 del curso nuevo, generado a las 16:08, en lugar de «Este curso todavía no tiene horario. Créalo con «Generar horario».» (ver paso 6) |
| 6d-2.png | 6d-2 | 2026-09-25 16:09:16 | `1310a4da7b5e1f24ae36e8f7e79f37854a8410bd9a8234efca5fed688f793048` | casa |
| 6e.png | 6e | 2026-09-25 16:13:51 | `302b2a50e4733dbf6053abaf6f6c620fc5d7e97f1c7fa11cebed8b0a558c6499` | casa |

Las mtime son las de la copia en Linux, en hora local (+02:00), conservadas con `cp -a` desde la
carpeta compartida.
