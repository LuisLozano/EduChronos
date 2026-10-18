@echo off
rem =====================================================================
rem  Educhronos - Instalar.cmd (S237).
rem
rem  Se lanza con doble clic desde la carpeta Educhronos extraida del zip, donde estan
rem  Educhronos.exe, app\ y runtime\. No pide permisos de administrador.
rem   0. Si Educhronos esta abierto, se detiene sin tocar nada.
rem   1. Copia Educhronos.exe, app\ y runtime\ a %LOCALAPPDATA%\Programs\Educhronos,
rem      reemplazando app\ y runtime\ si ya estaban.
rem   2. Crea Educhronos.lnk en el Escritorio del usuario.
rem   3. Si junto a este fichero hay una carpeta base-partida con un unico .db, y la carpeta
rem      de datos %LOCALAPPDATA%\Educhronos no tiene ningun .db ni el fichero curso-abierto,
rem      lo copia alli como educhronos.db.
rem      Fuera de ese caso no escribe ni borra nada en la carpeta de datos.
rem  Toda salida espera una tecla, para que la ventana del doble clic se pueda leer.
rem  Sin tildes: cmd lee el fichero en la pagina de codigos de la consola.
rem =====================================================================
setlocal EnableExtensions
set "ORIGEN=%~dp0"
if "%LOCALAPPDATA%"=="" goto sin_localappdata
set "DESTINO=%LOCALAPPDATA%\Programs\Educhronos"
set "DATOS=%LOCALAPPDATA%\Educhronos"
if not exist "%ORIGEN%Educhronos.exe" goto sin_origen
if not exist "%ORIGEN%app\" goto sin_origen
if not exist "%ORIGEN%runtime\" goto sin_origen
tasklist /FI "IMAGENAME eq Educhronos.exe" /NH | find /i "Educhronos.exe" >nul
if not errorlevel 1 goto abierto

echo [1/3] Programa en %DESTINO%
if not exist "%DESTINO%\" mkdir "%DESTINO%"
if not exist "%DESTINO%\" goto fallo_programa
if exist "%DESTINO%\app\" rmdir /s /q "%DESTINO%\app"
if exist "%DESTINO%\runtime\" rmdir /s /q "%DESTINO%\runtime"
if exist "%DESTINO%\app\" goto en_uso
if exist "%DESTINO%\runtime\" goto en_uso
copy /y "%ORIGEN%Educhronos.exe" "%DESTINO%\Educhronos.exe" >nul
if errorlevel 1 goto en_uso
xcopy "%ORIGEN%app" "%DESTINO%\app\" /e /i /q /y >nul
if errorlevel 1 goto fallo_programa
xcopy "%ORIGEN%runtime" "%DESTINO%\runtime\" /e /i /q /y >nul
if errorlevel 1 goto fallo_programa

echo [2/3] Acceso directo en el Escritorio
powershell -NoProfile -ExecutionPolicy Bypass -Command "$l = Join-Path ([Environment]::GetFolderPath('Desktop')) 'Educhronos.lnk'; $s = (New-Object -ComObject WScript.Shell).CreateShortcut($l); $s.TargetPath = Join-Path $env:DESTINO 'Educhronos.exe'; $s.WorkingDirectory = $env:DESTINO; $s.Save(); if (-not (Test-Path -LiteralPath $l)) { exit 1 }"
if errorlevel 1 goto fallo_acceso

echo [3/3] Base de partida
set "NBASE=0"
set "BASE="
if exist "%ORIGEN%base-partida\" for %%F in ("%ORIGEN%base-partida\*.db") do call :contar_base "%%~fF"
if "%NBASE%"=="0" goto sin_base
if not "%NBASE%"=="1" goto varias_bases
set "NDATOS=0"
if exist "%DATOS%\" for %%F in ("%DATOS%\*.db") do set /a NDATOS+=1
if not "%NDATOS%"=="0" goto datos_con_base
if exist "%DATOS%\curso-abierto" goto datos_con_curso
if not exist "%DATOS%\" mkdir "%DATOS%"
copy "%BASE%" "%DATOS%\educhronos.db" >nul
if errorlevel 1 goto fallo_base
echo     Base de partida copiada a %DATOS%\educhronos.db
goto fin

:contar_base
set /a NBASE+=1
set "BASE=%~1"
goto :eof

:sin_base
echo     No hay base de partida junto a Instalar.cmd: la carpeta de datos no se toca.
goto fin
:varias_bases
echo     Hay %NBASE% ficheros .db en base-partida: no se copia ninguno.
goto fin
:datos_con_base
echo     La carpeta de datos ya tiene %NDATOS% ficheros .db: no se copia la base de partida.
goto fin
:datos_con_curso
echo     La carpeta de datos ya tiene el fichero curso-abierto: no se copia la base de partida.
goto fin

:sin_localappdata
echo ABORTA: falta la variable LOCALAPPDATA.
set "RC=2"
goto salir
:sin_origen
echo ABORTA: Instalar.cmd tiene que estar junto a Educhronos.exe, app y runtime.
echo Extrae antes el zip con "Extraer todo" y lanza Instalar.cmd desde la carpeta extraida.
set "RC=2"
goto salir
:abierto
echo ABORTA: Educhronos esta abierto. Cierralo desde el icono de la bandeja (Salir) y vuelve a lanzar Instalar.cmd.
set "RC=6"
goto salir
:en_uso
echo ABORTA: no se pudo reemplazar el programa en %DESTINO%. Cierra Educhronos desde el icono de la bandeja y vuelve a lanzar Instalar.cmd.
set "RC=3"
goto salir
:fallo_programa
echo ABORTA: fallo al copiar el programa a %DESTINO%.
set "RC=3"
goto salir
:fallo_acceso
echo ABORTA: no se pudo crear el acceso directo en el Escritorio.
set "RC=4"
goto salir
:fallo_base
echo ABORTA: no se pudo copiar la base de partida.
set "RC=5"
goto salir

:fin
echo Educhronos instalado en %DESTINO%.
echo Abre Educhronos con el acceso directo del Escritorio.
set "RC=0"
goto salir

:salir
echo.
pause
exit /b %RC%
