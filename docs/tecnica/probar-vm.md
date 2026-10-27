# Cómo se prueba una versión en la VM

Responde a esta pregunta: cómo se prueba una versión de Educhronos en la máquina virtual de Windows con el script del anfitrión, y qué queda para la persona.
Léelo antes de probar una versión nueva en la VM, o cuando la prueba dé un FALLO.

Describe el commit 21cd952fc89a.

La prueba la lleva `tools/prueba-vm/prueba_vm.py`, desde Linux, con VBoxManage.
Hace la parte que no es de pantalla: restaurar la VM, instalar la versión, medir y apagar.
A la persona le deja tres pasos: entrar en Windows, abrir Educhronos y salir desde la bandeja (`tools/prueba-vm/prueba_vm.py:2-7`).
Entre ellos, la persona mira en pantalla lo que pida la versión.
El criterio que cumple es el de O-prueba-vm, en §3 de `docs/gestion_proyecto.md`.
La última prueba hecha a mano, con 20 pasos de la persona, es la de `docs/actas-aceptacion/acta-version-s252.md`.

## Qué hace falta

- La VM de VirtualBox Win11, con la instantánea s254-cuenta-prueba: la cuenta estándar prueba-vm, que nunca ha iniciado sesión.
  Son los valores por defecto del script; se cambian con --vm, --instantanea y --cuenta (`tools/prueba-vm/prueba_vm.py:51-53`, `tools/prueba-vm/prueba_vm.py:892-895`).
- La VM apagada y sin cambios desde esa instantánea; el script lo comprueba antes de tocar nada.
- systemd-inhibit en el anfitrión: el script impide que se suspenda mientras dura la prueba (`tools/prueba-vm/prueba_vm.py:94-96`).
- El zip de la versión con su .sha256 al lado, en la forma «64 hex, dos espacios, nombre», y la base de partida con el principio de su md5, de 8 caracteres como mínimo (`tools/prueba-vm/prueba_vm.py:464-479`).
- El fichero de contraseñas de la cuenta: un fichero regular del usuario, con modo 600 (`tools/prueba-vm/prueba_vm.py:457-463`).
  Tiene que estar fuera del repo, de las carpetas compartidas de la VM y de la carpeta de material (`tools/prueba-vm/prueba_vm.py:377-384`).
  El script solo pasa su ruta a VBoxManage; nunca lo abre (`tools/prueba-vm/prueba_vm.py:20-21`).
- Una carpeta de material que todavía no existe, con ruta absoluta: la crea el script (`tools/prueba-vm/prueba_vm.py:455-456`, `tools/prueba-vm/prueba_vm.py:916-918`).

## La prueba, paso a paso

Son cuatro subcomandos, en este orden; preparar escribe estado.json en el material y los demás lo leen (`tools/prueba-vm/prueba_vm.py:9-16`).
Entre uno y otro, la persona hace su parte en la ventana de la VM.
preparar, sesion y abierta imprimen al final el paso humano siguiente y la orden que viene después.

```bash
python3 -B tools/prueba-vm/prueba_vm.py preparar --clave /ruta/clave.txt \
    --zip /ruta/Educhronos-win.zip --base /ruta/base-partida-2025-2026.db --md5-base d867c2de \
    --migracion-esperada 'versionInicial=0 versionFinal=9' --material /ruta/material
python3 -B tools/prueba-vm/prueba_vm.py sesion --material /ruta/material
python3 -B tools/prueba-vm/prueba_vm.py abierta --material /ruta/material
python3 -B tools/prueba-vm/prueba_vm.py cerrar --material /ruta/material
```

1. **preparar.** Corre la autoprueba y comprueba el anfitrión y la VM.
   Después restaura la instantánea, arranca la VM con su ventana y espera al invitado.
   Mide la cuenta, copia el zip y la base, comprueba sus sumas dentro de la VM, extrae, coloca la base e instala.
   Por último mide la base instalada y la carpeta de datos, e imprime H1.
2. **H1, la persona.** Entra con la cuenta, tecleando ella la contraseña, y pasa las pantallas del primer inicio de Windows hasta el escritorio.
3. **sesion.** Comprueba que la sesión de la consola es de la cuenta; imprime H2.
4. **H2, la persona.** Abre Educhronos con el acceso directo del escritorio.
   Si sale la bienvenida del navegador, la pasa como dice el anexo de `docs/manual/manual.md`.
5. **abierta.** Lee en el log la línea de migración y mira los procesos de la aplicación; imprime H3.
6. **La persona.** Hace los pasos de mirar que pida la versión, de uno en uno, y después H3: «Salir» en el menú de la bandeja.
7. **cerrar.** Comprueba que el proceso ha terminado, apaga, restaura la instantánea, compara y suelta el inhibidor.
   Escribe el informe final y el manifiesto del material.

El criterio de O-prueba-vm pide que, entre los pasos 2, 4 y 6, la persona haga como mucho 10.
Las pantallas del primer inicio de Windows y la bienvenida del navegador cuentan dentro de su paso, como se contaron a mano.

## Qué deja y qué comprueba

En la carpeta de material quedan estado.json, informe.txt, la carpeta ordenes y MANIFIESTO.sha256.
En ordenes está cada llamada a VBoxManage, con su orden, su salida, su código y su duración, y cada lectura del .vbox.
informe.txt tiene una línea por comprobación, en un orden fijo, con OK, FALLO o DATO (`tools/prueba-vm/prueba_vm.py:98-103`).
DATO es una cifra que se anota sin juzgarla, o una comprobación que no llegó a medirse.

Las comprobaciones, agrupadas:

- **La VM antes y después.** vm-sin-cambios, regla-detecta-cambio, vm-restaurada e instantaneas.
  La VM no se restaura si ha cambiado desde la instantánea, y al final se exige que la lista de instantáneas sea la misma.
- **La clave.** clave-fuera.
- **El arranque.** arranque, invitado y cuenta: la cuenta no está en el grupo de administradores y tiene nivel de integridad medio.
- **La instalación.** transporte, suma-zip, suma-base, extraer, datos-previos, instalar-rc, instalar-lineas, md5-instalada, carpeta-datos, acceso-directo y exe.
- **La aplicación.** sesion-cuenta, migracion, procesos-abierta y proceso-terminado.
- **El inhibidor.** inhibidor.

El estado de la VM se juzga con su fichero .vbox, cuya ruta da CfgFile en showvminfo, y no con la hora que da showvminfo (`tools/prueba-vm/prueba_vm.py:88-90`).
Tras una restauración, showvminfo sigue dando la hora de la restauración mientras viva el proceso de VirtualBox que la hizo; el .vbox ya tiene la de la instantánea.
Del .vbox se miran la instantánea actual, la hora del último cambio de estado, al segundo, y que el estado no esté modificado (`tools/prueba-vm/prueba_vm.py:277-286`).

## Si algo falla

Los códigos de salida están en `tools/prueba-vm/prueba_vm.py:32-33`: 0 todo OK, 1 fallo inesperado, 2 rechazo sin tocar la VM, 3 alguna comprobación da FALLO y 4 la autoprueba no pasa.

- **Con 2,** el script no ha tocado la VM; el mensaje dice por qué.
  Una carpeta de material que ya existe, por ejemplo, no se toca.
- **Con un FALLO y la VM encendida,** la VM se queda como está y el mensaje pide ejecutar cerrar (`tools/prueba-vm/prueba_vm.py:17-18`, `tools/prueba-vm/prueba_vm.py:934-935`).
  cerrar solo apaga y restaura si preparar llegó a restaurar.
- **Con 4,** la autoprueba ha encontrado un veredicto que no distingue su defecto; no se da ninguna cifra.

La autoprueba inyecta un defecto por capacidad en las mismas funciones de veredicto que la corrida real (`tools/prueba-vm/prueba_vm.py:22-24`).
Se puede correr sola, sin tocar la VM:

```bash
python3 -B tools/prueba-vm/prueba_vm.py --solo-autoprueba
```

## Lo que el script no hace

No juzga las pantallas, y no pulsa teclas ni mueve el ratón en la VM: eso sigue siendo de la persona.
No toma ni borra instantáneas; solo restaura la que se le da.
Una versión que cambie `scripts/empaquetar-windows.ps1`, Instalar.cmd o la sección 2 de `docs/manual/manual.md` devuelve a la persona el transporte, la extracción y la instalación, que se prueban a mano como en `docs/actas-aceptacion/acta-version-s252.md`.

## Sus tests

Los tests usan una VM y un anfitrión falsos, y nunca llaman a VBoxManage: el ejecutor real se niega a correr si los tests ponen la variable EDUCHRONOS_PRUEBA_VM_SIN_VM (`tools/prueba-vm/prueba_vm.py:21-22`, `tools/prueba-vm/tests/test_prueba_vm.py:1`).
Se lanzan como las demás suites de Python, desde la raíz:

```bash
python3 -B -m unittest discover -s tools/prueba-vm/tests -p 'test_*.py'
```

Dan 55 tests.
