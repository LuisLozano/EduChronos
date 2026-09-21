# =====================================================================
#  Educhronos - empaquetado, lado Windows.
#
#  Toma la carpeta de entrega que produce scripts/empaquetar-linux.sh y
#  construye el app-image con jpackage. No necesita codigo fuente, ni Maven,
#  ni Git, ni Java instalado: el JDK viaja en la entrega.
#
#    powershell -ExecutionPolicy Bypass -File .\empaquetar-windows.ps1 -HuellaJar <sha256>
#
#  Opciones:
#    -Base C:\ruta   donde trabajar (por defecto C:\DES\educhronos-build)
#    -HuellaJar H    OBLIGATORIA. La sha256 del jar, copiada de la consola del guion de
#                    Linux al terminar. NO se lee de esta carpeta a proposito: es lo unico
#                    que distingue una entrega al dia de una caducada.
#    -SinHumo        no arranca la aplicacion al terminar
#
#  Sin tildes a proposito: Windows PowerShell 5.1 lee los .ps1 en la
#  codificacion del sistema cuando no llevan BOM.
#
#  Los 14 modulos del runtime estan medidos, no supuestos (S152):
#    jdk.zipfs    lo exige el cargador nativo de OR-Tools; sin el, el solver
#                 muere con ProviderNotFoundException al primer POST.
#    java.desktop lo exige el enlazador de propiedades de Spring Boot
#                 (java.beans); sin el, la aplicacion ni arranca.
# =====================================================================
param(
    [string]$Base = "C:\DES\educhronos-build",
    [string]$HuellaJar,
    [switch]$SinHumo
)
$ErrorActionPreference = 'Continue'

$entrega = $PSScriptRoot
$jdkRaiz = "$Base\jdk"
$entrada = "$Base\entrada"
$dest    = "$Base\imagen"
$datos   = "$Base\humo"

$modulos = "java.base,java.compiler,java.desktop,java.instrument,java.management," +
           "java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss," +
           "java.sql.rowset,jdk.jfr,jdk.unsupported,jdk.zipfs"
$limite = 250000000

# Modo escritorio (S154): sin esta opcion el .exe arranca como un servidor mudo. Con ella
# hace lo que un programa de escritorio: comprueba que no haya otro Educhronos abierto, abre
# el navegador, pone el icono en la bandeja y escribe su log en la carpeta de datos.
#
# El literal TIENE que ser identico a la constante ModoEscritorio.PROPIEDAD del codigo Java
# (app/src/main/java/es/yaroki/educhronos/app/escritorio/ModoEscritorio.java). Si se renombra
# alli y no aqui, el bundle arranca en modo servidor SIN dar ningun error: sin navegador, sin
# bandeja y sin forma de cerrarlo. La comparacion la hace el paso 8a de S154 con grep.
$propiedadEscritorio = "educhronos.escritorio"

# -Base es el primer parametro POSICIONAL: si alguien encadena otra orden en la
# misma linea, PowerShell se la pasa aqui. Una ruta relativa se resolveria contra
# el directorio actual y el guion escribiria en un sitio inventado, en silencio
# (ocurrio en S152: "Copy-Item" llego como -Base). Se exige ruta absoluta.
if (-not [System.IO.Path]::IsPathRooted($Base)) {
    Write-Host "ABORTA: -Base tiene que ser una ruta absoluta."
    Write-Host ("  recibido: '{0}'" -f $Base)
    Write-Host "  ejemplo : -Base C:\DES\educhronos-build"
    Write-Host "  Si encadenaste otra orden detras del guion, ponla en una linea aparte."
    exit 2
}

# -HuellaJar tiene que llegar por un canal DISTINTO de la propia entrega: se copia de la
# consola de la maquina de Linux, no se lee de esta carpeta. SHA256SUMS viaja DENTRO de la
# entrega, asi que una entrega vieja pasa sus propias huellas sin que nada chille
# (D-entrega-caducada-indetectable, S154). Sin [Parameter(Mandatory)]: pediria el valor por
# teclado en vez de abortar, y un guion que espera tecleo no sirve en una tanda.
if ($HuellaJar -notmatch '^[0-9A-Fa-f]{64}$') {
    Write-Host "ABORTA: falta -HuellaJar o no es una sha256 (64 hexadecimales). Copiala de la orden"
    Write-Host "        que imprime el guion de Linux al terminar; NO la tomes de esta carpeta."
    exit 2
}
if (-not (Test-Path $Base)) { New-Item -ItemType Directory $Base -Force | Out-Null }

# Nombre propio por corrida: si no, una segunda corrida borra la transcripcion
# de la primera, que es justo la que se quiere comparar.
$modo = if ($SinHumo) { "sinhumo" } else { "completo" }
$sello = Get-Date -Format "yyyyMMdd-HHmmss"
$transcripcion = "$Base\empaquetado-$modo-$sello.txt"
Start-Transcript -Path $transcripcion -Force | Out-Null
Write-Host ("Base: {0}" -f $Base)
Write-Host ("Transcripcion: {0}" -f $transcripcion)

function Terminar($codigo) { Stop-Transcript | Out-Null; exit $codigo }

Write-Host "=========================================================="
Write-Host " 1. VERIFICACION DE LA ENTREGA"
Write-Host "=========================================================="
Write-Host "entrega: $entrega"

$sumas = "$entrega\SHA256SUMS"
if (-not (Test-Path $sumas)) { Write-Host "ABORTA: falta SHA256SUMS."; Terminar 1 }

$esperado = @{}
foreach ($linea in Get-Content $sumas) {
    $t = $linea -split '\s+', 2
    if ($t.Count -eq 2) { $esperado[$t[1].Trim()] = $t[0].ToUpper() }
}
Write-Host ("entradas en SHA256SUMS: {0}" -f $esperado.Count)
if ($esperado.Count -ne 2) { Write-Host "ABORTA: se esperaban 2 entradas (jar y jdk)."; Terminar 1 }

$jarNombre = $null; $jdkZip = $null
foreach ($n in $esperado.Keys) {
    if ($n -like '*.jar') { $jarNombre = $n }
    if ($n -like '*.zip') { $jdkZip    = $n }
}
if (-not $jarNombre -or -not $jdkZip) { Write-Host "ABORTA: SHA256SUMS no nombra un .jar y un .zip."; Terminar 1 }

# El jar viaja junto a este guion; el JDK, en la carpeta jdk\ hermana.
$rutaJar = Join-Path $entrega $jarNombre
$rutaJdk = Join-Path (Split-Path $entrega -Parent) "jdk\$jdkZip"
Write-Host "jar: $rutaJar"
Write-Host "jdk: $rutaJdk"
if (-not (Test-Path $rutaJar)) { Write-Host "ABORTA: no esta el jar."; Terminar 1 }
if (-not (Test-Path $rutaJdk)) { Write-Host "ABORTA: no esta el zip del JDK (carpeta jdk\ hermana)."; Terminar 1 }

$hJar = (Get-FileHash $rutaJar -Algorithm SHA256).Hash
$hJdk = (Get-FileHash $rutaJdk -Algorithm SHA256).Hash
Write-Host ("jar esperado : {0}" -f $esperado[$jarNombre])
Write-Host ("jar medido   : {0}" -f $hJar)
Write-Host ("jdk esperado : {0}" -f $esperado[$jdkZip])
Write-Host ("jdk medido   : {0}" -f $hJdk)
if ($hJar -ne $esperado[$jarNombre] -or $hJdk -ne $esperado[$jdkZip]) {
    Write-Host "ABORTA: alguna huella no coincide. La copia no esta integra."
    Terminar 1
}
Write-Host "Las dos huellas coinciden."

# TERCERA comprobacion, y la unica que detecta una entrega CADUCADA: las dos de arriba solo
# dicen que esta copia esta integra respecto al SHA256SUMS que viaja con ella, y eso lo
# cumple igual una entrega de hace tres construcciones.
Write-Host ("jar -HuellaJar: {0}" -f $HuellaJar.ToUpper())
if ($hJar -ne $HuellaJar.ToUpper()) {
    Write-Host "ABORTA: el jar de esta carpeta no es el que construyo Linux (huella distinta de"
    Write-Host "        -HuellaJar). La entrega esta caducada o es de otra construccion."
    Terminar 1
}
Write-Host "El jar es el de la construccion de Linux."

Write-Host ""
Write-Host "=========================================================="
Write-Host " 2. JDK PORTABLE"
Write-Host "=========================================================="
if (Test-Path $jdkRaiz) { Remove-Item $jdkRaiz -Recurse -Force }
New-Item -ItemType Directory $jdkRaiz -Force | Out-Null
Expand-Archive $rutaJdk -DestinationPath $jdkRaiz -Force
$jdkDir = Get-ChildItem $jdkRaiz -Directory | Select-Object -First 1
if (-not $jdkDir) { Write-Host "ABORTA: el zip del JDK no trajo ninguna carpeta."; Terminar 1 }
$jdk = $jdkDir.FullName
$jpackage = "$jdk\bin\jpackage.exe"
Write-Host "JDK: $jdk"
if (-not (Test-Path $jpackage)) { Write-Host "ABORTA: no hay jpackage.exe."; Terminar 1 }
& "$jdk\bin\java.exe" -version 2>&1 | ForEach-Object { Write-Host "    $_" }
$njmods = (Get-ChildItem "$jdk\jmods" -ErrorAction SilentlyContinue).Count
Write-Host ("jmods: {0}" -f $njmods)
if ($njmods -eq 0) { Write-Host "ABORTA: sin jmods, jpackage no puede montar el runtime."; Terminar 1 }

Write-Host ""
Write-Host "=========================================================="
Write-Host " 3. JPACKAGE"
Write-Host "=========================================================="
if (Test-Path $entrada) { Remove-Item $entrada -Recurse -Force }
New-Item -ItemType Directory $entrada -Force | Out-Null
Copy-Item $rutaJar $entrada
if (Test-Path $dest) { Remove-Item $dest -Recurse -Force }
New-Item -ItemType Directory $dest -Force | Out-Null

Write-Host "modulos (14, medidos en S152):"
Write-Host "    $modulos"
Write-Host ("java-options: -D{0}=true" -f $propiedadEscritorio)
$crono = [System.Diagnostics.Stopwatch]::StartNew()
& $jpackage --type app-image --name Educhronos `
    --input $entrada --main-jar $jarNombre `
    --java-options "-D$propiedadEscritorio=true" `
    --add-modules $modulos --dest $dest
$exitJp = $LASTEXITCODE
$crono.Stop()
Write-Host ("exit jpackage = {0}   segundos = {1:N1}" -f $exitJp, $crono.Elapsed.TotalSeconds)
if ($exitJp -ne 0) { Write-Host "ABORTA: jpackage fallo."; Terminar 1 }
Get-ChildItem "$dest\Educhronos" | Format-Table Mode, Length, Name -AutoSize

Write-Host ""
Write-Host "=========================================================="
Write-Host " 4. TAMANOS  (condicion 2, en bytes de 10^6)"
Write-Host "=========================================================="
$carpeta = (Get-ChildItem "$dest\Educhronos" -Recurse -File | Measure-Object Length -Sum).Sum
$runtime = (Get-ChildItem "$dest\Educhronos\runtime" -Recurse -File | Measure-Object Length -Sum).Sum
$appdir  = (Get-ChildItem "$dest\Educhronos\app" -Recurse -File | Measure-Object Length -Sum).Sum
Write-Host ("carpeta : {0,13} B   ({1:N1} MB)" -f $carpeta, ($carpeta/1000000))
Write-Host ("runtime : {0,13} B   ({1:N1} MB)" -f $runtime, ($runtime/1000000))
Write-Host ("app     : {0,13} B   ({1:N1} MB)" -f $appdir,  ($appdir/1000000))
Write-Host ("limite  : {0} B" -f $limite)
if ($carpeta -lt $limite) {
    Write-Host ("CUMPLE. Margen {0:N1} MB." -f (($limite-$carpeta)/1000000))
} else {
    Write-Host ("NO CUMPLE. Exceso {0:N1} MB." -f (($carpeta-$limite)/1000000))
    Write-Host "Palanca medida y no aplicada (S152): 60.869.367 B de nativos de"
    Write-Host "OR-Tools de otras plataformas, podables con <exclusions>."
}
Write-Host "Referencia S151 sin --add-modules: carpeta 291502931 B, runtime 134690396 B"

Write-Host ""
Write-Host "=========================================================="
Write-Host " 5. ZIP"
Write-Host "=========================================================="
$zip = "$Base\Educhronos-win.zip"
if (Test-Path $zip) { Remove-Item $zip -Force }
# Compress-Archive de PowerShell 5.1 tarda minutos con ~232 MB.
Add-Type -AssemblyName System.IO.Compression.FileSystem
$cronoZip = [System.Diagnostics.Stopwatch]::StartNew()
[System.IO.Compression.ZipFile]::CreateFromDirectory("$dest\Educhronos", $zip,
    [System.IO.Compression.CompressionLevel]::Optimal, $true)
$cronoZip.Stop()
if (-not (Test-Path $zip)) { Write-Host "ABORTA: no se creo el zip."; Terminar 1 }
Write-Host ("zip: {0}  ({1} B)  en {2:N1} s" -f $zip, (Get-Item $zip).Length, $cronoZip.Elapsed.TotalSeconds)
# La huella del zip se imprime AQUI y se repite en el resumen: es lo que se comprueba en el
# equipo de destino antes de extraer, y sale de la consola, no de dentro del zip.
$hZip = (Get-FileHash $zip -Algorithm SHA256).Hash
Write-Host ("zip sha256: {0}" -f $hZip)

if ($SinHumo) {
    Write-Host ""
    Write-Host "Prueba de humo omitida por -SinHumo."
    Write-Host ("App-image en {0}\Educhronos" -f $dest)
    Write-Host ("transcripcion: {0}" -f $transcripcion)
    Terminar 0
}

Write-Host ""
Write-Host "=========================================================="
Write-Host " 6. PRUEBA DE HUMO"
Write-Host "=========================================================="
Write-Host "Arranca sobre una base VACIA en una carpeta aparte. No prueba datos."

# AVISO: desde S154 el .exe arranca en MODO ESCRITORIO (--java-options
# -Deduchronos.escritorio=true). Durante el humo, por tanto, la aplicacion ABRE EL NAVEGADOR
# en http://127.0.0.1:8080 y PONE SU ICONO en la bandeja del sistema. Es lo esperado, no un
# efecto secundario que haya que corregir: el humo ejercita el .exe tal como lo recibe el
# usuario. Quien lance este guion vera aparecer una pestana; se cierra sola al pararse el
# proceso, unos segundos despues.
#
# Y un efecto que NO se puede evitar pasando la base por variable de entorno: el modo
# escritorio resuelve y CREA la carpeta de datos del usuario (%LOCALAPPDATA%\Educhronos)
# pase lo que pase, porque ahi viven el candado de instancia unica y el fichero de log. La
# BASE sigue siendo la temporal de $datos —que es lo que sostiene "no prueba datos"—, pero
# esa carpeta se toca. Si ya existia, el humo le anade su educhronos.log.
if (Test-Path $datos) { Remove-Item $datos -Recurse -Force }
New-Item -ItemType Directory $datos -Force | Out-Null

# GUARDA (S154): con el 8080 ya ocupado, este humo daria VERDE SIN PROBAR NADA de lo que
# acaba de construirse. El modo escritorio detecta al otro Educhronos por el candado, no
# arranca, abre el navegador y sale con 0; el Invoke-WebRequest de mas abajo recibiria su
# 200 de LA INSTANCIA DEL USUARIO, y el POST /api/horarios se ejecutaria contra SU base.
# Por eso se aborta, y no se avisa y se sigue.
$ocupado = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($ocupado) {
    Write-Host "ABORTA: el puerto 8080 ya esta ocupado ANTES de lanzar el humo."
    $ocupado | Select-Object LocalAddress, LocalPort, OwningProcess | Format-Table -AutoSize
    Write-Host "  Cierra ese programa (si es Educhronos, con Salir en el icono de la bandeja)"
    Write-Host "  y vuelve a lanzar. El app-image y el zip ya estan construidos."
    Terminar 1
}

# La URL se pasa EXPLICITA, y es lo que sostiene la frase de arriba. Desde S153 la
# aplicacion sin argumento NO crea la base en el directorio de trabajo: la resuelve en la
# carpeta de datos del usuario (%LOCALAPPDATA%\Educhronos), que PERSISTE entre corridas.
# Con la resolucion por defecto, el Remove-Item de $datos ya no garantizaria una base
# vacia (limpiaria una carpeta donde no vive la base) y el listado del final saldria vacio
# sin que nada fallase. Pasandola aqui, el humo prueba lo que dice que prueba: base vacia,
# en sitio conocido y borrado en cada corrida.
#
# Que la resolucion POR DEFECTO acierte no se verifica aqui: eso es el M4 de la condicion 7.
#
# PENDIENTE DE MEDIR EN WINDOWS (no se puede probar en Linux, este guion solo corre alli):
#   La ruta lleva barras invertidas dentro de una URL JDBC. El driver Xerial 3.53.2.0 no
#   normaliza nada: toma la subcadena que sigue a "jdbc:sqlite:" tal cual y se la pasa al
#   open nativo (solo trata aparte ":memory:", "file:" y ":resource:"). Verificado leyendo
#   el bytecode del driver, NO ejecutandolo en Windows. Si el open fallase con
#   SQLITE_CANTOPEN, la alternativa a probar es la misma ruta con barras normales:
#   ("$bdHumo" -replace '\\', '/').
# (El segundo riesgo que tuvo este bloque, el troceo de -ArgumentList por espacios, ya no
# aplica: la URL viaja por variable de entorno. La razon esta junto al Start-Process.)
$bdHumo = "$datos\educhronos-humo.db"
Write-Host ("base del humo: {0}" -f $bdHumo)

# Por VARIABLE DE ENTORNO y no por argumento. Start-Process -ArgumentList en PowerShell
# 5.1 une los elementos con espacios y NO los encomilla, asi que un -Base con espacios
# partiria "--spring.datasource.url=jdbc:sqlite:C:\Mis Cosas\humo\educhronos-humo.db" en
# dos argumentos y la URL llegaria truncada, en silencio y con el humo pareciendo correcto.
# Una variable de entorno no se parte. Spring Boot enlaza SPRING_DATASOURCE_URL a
# spring.datasource.url por binding relajado.
$env:SPRING_DATASOURCE_URL = "jdbc:sqlite:$bdHumo"
try {
    $proc = Start-Process -FilePath "$dest\Educhronos\Educhronos.exe" `
            -WorkingDirectory $datos -PassThru
} finally {
    # Se limpia en cuanto el proceso esta lanzado: lo hereda el hijo, y asi no queda
    # colgando en la sesion de PowerShell para lo que venga despues.
    Remove-Item Env:\SPRING_DATASOURCE_URL -ErrorAction SilentlyContinue
}
Write-Host ("PID lanzador = {0}" -f $proc.Id)

$listo = $false
for ($i = 1; $i -le 45; $i++) {
    Start-Sleep -Seconds 2
    try {
        $r = Invoke-WebRequest -Uri "http://localhost:8080/api/jornada" -UseBasicParsing -TimeoutSec 5
        if ($r.StatusCode -eq 200) { $listo = $true; Write-Host ("arrancado en ~{0} s" -f ($i*2)); break }
    } catch { }
}
Write-Host ("LISTO = {0}" -f $listo)
if ($listo) {
    $cuerpo = (Invoke-WebRequest -Uri "http://localhost:8080/api/jornada" -UseBasicParsing).Content
    if ($cuerpo.Length -gt 160) { Write-Host $cuerpo.Substring(0,160) } else { Write-Host $cuerpo }
}

$solver = "NO EJECUTADO"
if ($listo) {
    $solver = "NO DISCRIMINANTE"
    Write-Host "--- POST /api/horarios (maxSegundos 5): ejercita el cargador nativo de OR-Tools"
    $codigo = 0
    $cuerpoPost = ""
    try {
        $p = Invoke-WebRequest -Uri "http://localhost:8080/api/horarios" -Method POST `
             -ContentType "application/json" -Body '{"maxSegundos":5}' `
             -UseBasicParsing -TimeoutSec 120
        $codigo = [int]$p.StatusCode
        $cuerpoPost = $p.Content
    } catch {
        if ($_.Exception.Response) {
            $codigo = [int]$_.Exception.Response.StatusCode
            $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $cuerpoPost = $sr.ReadToEnd()
            $sr.Close()
        } else {
            $cuerpoPost = $_.Exception.Message
        }
    }
    Write-Host ("  http = {0}" -f $codigo)
    if ($cuerpoPost.Length -gt 300) { Write-Host ("  " + $cuerpoPost.Substring(0,300)) }
    else { Write-Host ("  " + $cuerpoPost) }
    if ($codigo -eq 500) {
        Write-Host "  FALLO: un 500 aqui es tipicamente un runtime al que le falta un"
        Write-Host "  modulo. Con jdk.zipfs ausente, el cargador nativo de OR-Tools muere"
        Write-Host "  con ProviderNotFoundException (medido en S152)."
        $solver = "FALLO"
    } elseif ($codigo -eq 503) {
        Write-Host "  OK: el solver corrio y agoto el presupuesto, luego el nativo cargo."
        $solver = "OK"
    } else {
        Write-Host "  NO DISCRIMINANTE: sobre base vacia el rechazo puede llegar ANTES de"
        Write-Host "  tocar el solver, asi que esto NO prueba que el nativo cargue. Eso lo"
        Write-Host "  cierra la condicion 3, con el banco."
    }
}

Write-Host "--- procesos:"
Get-CimInstance Win32_Process -Filter "Name='Educhronos.exe'" |
    Select-Object ProcessId, ParentProcessId, @{n='MB';e={[int]($_.WorkingSetSize/1MB)}} |
    Format-Table -AutoSize
# Debe aparecer educhronos-humo.db, la base que se paso por argumento. Ya NO sirve como
# comprobacion de "donde nace la base por defecto": eso lo decide el post-procesador de
# S153 y se verifica en el M4.
Write-Host "--- ficheros en la carpeta del humo:"
Get-ChildItem $datos | Format-Table Length, Name -AutoSize
Write-Host "--- escucha en el 8080:"
Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue |
    Select-Object LocalAddress, LocalPort, OwningProcess | Format-Table -AutoSize

# Se mata SOLO EL LANZADOR, y el lanzador arrastra a la JVM hija. MEDIDO en Windows en
# S154. Antes se mataban primero las hijas y luego el lanzador; sobra, y ademas invierte el
# orden natural: matar la hija primero deja un instante en el que el lanzador sigue vivo sin
# nada que lanzar. La comprobacion de que no queda NINGUN Educhronos.exe es la que dice si
# esta suposicion se cumple el dia que deje de cumplirse.
Write-Host "--- parada (se mata el lanzador; arrastra a la JVM hija):"
Write-Host ("  matando lanzador {0}" -f $proc.Id)
Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 4
$vivos = Get-Process -Name Educhronos -ErrorAction SilentlyContinue
if ($vivos) {
    Write-Host "  FALLO: QUEDAN PROCESOS Educhronos vivos tras matar al lanzador."
    Write-Host "  (El 8080 ocupado se descarto antes de lanzar, asi que son suyos.)"
    $vivos | Format-Table Id, ProcessName -AutoSize
} else {
    Write-Host "  no queda ningun proceso Educhronos"
}
$puerto = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($puerto) { Write-Host "  FALLO: PUERTO 8080 OCUPADO tras la parada"; $puerto | Format-Table -AutoSize }
else { Write-Host "  puerto 8080 libre" }

Write-Host ""
Write-Host "=========================================================="
Write-Host " RESUMEN"
Write-Host "=========================================================="
Write-Host ("carpeta     : {0} B ({1:N1} MB)" -f $carpeta, ($carpeta/1000000))
Write-Host ("condicion 2 : {0}" -f $(if ($carpeta -lt $limite) { "CUMPLE" } else { "NO CUMPLE" }))
Write-Host ("arranque    : {0}" -f $(if ($listo) { "OK" } else { "FALLO" }))
Write-Host ("solver      : {0}" -f $solver)
Write-Host ("app-image   : {0}\Educhronos" -f $dest)
Write-Host ("zip         : {0}" -f $zip)
Write-Host ("zip sha256  : {0}" -f $hZip)
Write-Host ("jar sha256  : {0}" -f $HuellaJar.ToUpper())
Write-Host ("transcripcion: {0}" -f $transcripcion)
if (-not $listo -or $solver -eq "FALLO") { Terminar 1 }
Terminar 0
