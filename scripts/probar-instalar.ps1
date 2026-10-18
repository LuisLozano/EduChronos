# =====================================================================
# Educhronos - probar-instalar.ps1 (S237, C-instalador). PowerShell 5.1.
#
# Prueba scripts/Instalar.cmd en 8 casos. Cada caso monta, en una carpeta temporal nueva que
# se borra al acabar, un origen falso (Educhronos.exe es una copia de PING.EXE; app\ y
# runtime\ llevan un fichero marca) y desvia LOCALAPPDATA a otra carpeta temporal.
# El Escritorio NO se puede desviar: Instalar.cmd lo pide a [Environment]::GetFolderPath.
# Por eso el script no corre si este usuario ya tiene Educhronos.lnk en el Escritorio, y
# borra el que crea al acabar cada caso.
#
#   powershell -ExecutionPolicy Bypass -File .\scripts\probar-instalar.ps1 [-Autoprueba] [-Instalador <ruta>]
#
# Sin -Autoprueba: corre los 8 casos, una linea por caso, y sale con 0 solo si pasan todos.
# Con -Autoprueba: corre los 8 casos sobre el original y despues sobre siete mutantes; cada
# mutante tiene que hacer fallar al menos un caso. Sale con 0 solo si el original pasa entero
# y ningun mutante sobrevive.
# El pause de Instalar.cmd no se prueba aqui (la entrada viene de un fichero vacio): se ve
# en la VM.
# =====================================================================
param(
    [switch]$Autoprueba,
    [string]$Instalador = ''
)

$ErrorActionPreference = 'Stop'

function Contar-LfSueltos([byte[]]$datos) {
    $n = 0
    for ($i = 0; $i -lt $datos.Length; $i++) {
        if ($datos[$i] -eq 10 -and ($i -eq 0 -or $datos[$i - 1] -ne 13)) { $n++ }
    }
    return $n
}

# GetTempPath puede traer componentes 8.3 (RUNNER~1) y el TargetPath de un .lnk vuelve con
# nombres largos: las rutas que se comparan pasan antes por aqui.
function Ruta-Larga([string]$ruta) {
    $partes = $ruta.TrimEnd('\').Split('\')
    $actual = $partes[0] + '\'
    for ($k = 1; $k -lt $partes.Length; $k++) {
        $hallados = [System.IO.Directory]::GetFileSystemEntries($actual, $partes[$k])
        if ($hallados.Length -ne 1) { return $ruta.TrimEnd('\') }
        $actual = $hallados[0]
    }
    return $actual
}

function Huella([string]$ruta) { return (Get-FileHash -LiteralPath $ruta -Algorithm SHA256).Hash }

function Escribir-Texto([string]$ruta, [string]$texto) {
    [System.IO.File]::WriteAllText($ruta, $texto, (New-Object System.Text.ASCIIEncoding))
}

# ---------------------------------------------------------------------
# Guardas previas: no se corre nada si alguna falla.
# ---------------------------------------------------------------------
if (-not $Instalador) { $Instalador = Join-Path $PSScriptRoot 'Instalar.cmd' }
if (-not (Test-Path -LiteralPath $Instalador -PathType Leaf)) { Write-Host "ABORTA: no existe $Instalador"; exit 1 }
$Instalador = (Resolve-Path -LiteralPath $Instalador).ProviderPath
$bytesOriginal = [System.IO.File]::ReadAllBytes($Instalador)
$sueltos = Contar-LfSueltos $bytesOriginal
if ($sueltos -ne 0) { Write-Host ("ABORTA: {0} tiene {1} saltos de linea sin CR." -f $Instalador, $sueltos); exit 1 }

$escritorio = [Environment]::GetFolderPath('Desktop')
if (-not $escritorio) { Write-Host "ABORTA: este usuario no tiene carpeta de Escritorio; Instalar.cmd no podria crear el acceso directo."; exit 1 }
$lnk = Join-Path $escritorio 'Educhronos.lnk'
if (Test-Path -LiteralPath $lnk) { Write-Host "ABORTA: ya existe $lnk; no se pisa el acceso directo de este usuario."; exit 1 }

$ping = Join-Path $env:SystemRoot 'System32\PING.EXE'
if (-not (Test-Path -LiteralPath $ping)) { Write-Host "ABORTA: no existe $ping"; exit 1 }
$temporal = Ruta-Larga ([System.IO.Path]::GetTempPath())
$agotados = New-Object System.Collections.ArrayList

Write-Host "Instalar.cmd : $Instalador"
Write-Host "Escritorio   : $escritorio"
Write-Host "Temporal     : $temporal"

# ---------------------------------------------------------------------
# Montaje
# ---------------------------------------------------------------------
function Nueva-Raiz {
    $raiz = Join-Path $temporal ('s237-instalar-' + [guid]::NewGuid().ToString('N').Substring(0, 12))
    New-Item -ItemType Directory -Path $raiz | Out-Null
    return $raiz
}

function Programa([string]$raiz) { return (Join-Path $raiz 'local\Programs\Educhronos') }
function Datos([string]$raiz) { return (Join-Path $raiz 'local\Educhronos') }

# Origen falso en <raiz>\origen\Educhronos, como la carpeta extraida del zip.
function Montar([string]$raiz, [string]$cmd, [bool]$conExe) {
    $origen = Join-Path $raiz 'origen\Educhronos'
    New-Item -ItemType Directory -Path (Join-Path $origen 'app') -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $origen 'runtime') -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $raiz 'local') -Force | Out-Null
    if ($conExe) { Copy-Item -LiteralPath $ping -Destination (Join-Path $origen 'Educhronos.exe') }
    Escribir-Texto (Join-Path $origen 'app\marca-app.txt') "marca app s237`r`n"
    Escribir-Texto (Join-Path $origen 'runtime\marca-runtime.txt') "marca runtime s237`r`n"
    Copy-Item -LiteralPath $cmd -Destination (Join-Path $origen 'Instalar.cmd')
    [System.IO.File]::WriteAllBytes((Join-Path $raiz 'entrada-vacia.txt'), [byte[]]@())
    return $origen
}

function Poner-Base([string]$origen, [string]$nombre, [string]$contenido) {
    $carpeta = Join-Path $origen 'base-partida'
    New-Item -ItemType Directory -Path $carpeta -Force | Out-Null
    $ruta = Join-Path $carpeta $nombre
    Escribir-Texto $ruta $contenido
    return $ruta
}

function Poner-EnDatos([string]$raiz, [string]$nombre, [string]$contenido) {
    $datos = Datos $raiz
    New-Item -ItemType Directory -Path $datos -Force | Out-Null
    $ruta = Join-Path $datos $nombre
    Escribir-Texto $ruta $contenido
    return $ruta
}

# Lanza Instalar.cmd del origen con LOCALAPPDATA desviado y la entrada desde un fichero
# vacio. Devuelve su rc, o $null si agota el plazo (60 s).
function Instalar([string]$raiz, [string]$origen, [string]$etiqueta) {
    $salida = Join-Path $raiz ('salida-' + $etiqueta + '.txt')
    $errores = Join-Path $raiz ('errores-' + $etiqueta + '.txt')
    $argumentos = '/c "' + (Join-Path $origen 'Instalar.cmd') + '"'
    $anterior = $env:LOCALAPPDATA
    $env:LOCALAPPDATA = Join-Path $raiz 'local'
    try {
        $proc = Start-Process -FilePath 'cmd.exe' -ArgumentList $argumentos -NoNewWindow -PassThru `
            -RedirectStandardInput (Join-Path $raiz 'entrada-vacia.txt') `
            -RedirectStandardOutput $salida -RedirectStandardError $errores
    } finally {
        $env:LOCALAPPDATA = $anterior
    }
    # Sin tocar Handle antes de que termine, PowerShell 5.1 deja ExitCode vacio.
    $null = $proc.Handle
    if (-not $proc.WaitForExit(60000)) {
        try { Stop-Process -Id $proc.Id -Force } catch { Write-Host ("    aviso: no se pudo parar cmd.exe {0}: {1}" -f $proc.Id, $_.Exception.Message) }
        return $null
    }
    $proc.WaitForExit()
    return $proc.ExitCode
}

function Rc-Distinto($rc, [int]$esperado) {
    if ($null -eq $rc) { return 'agota el tiempo (pause espera?)' }
    if ($rc -ne $esperado) { return ("rc {0}, se esperaba {1}" -f $rc, $esperado) }
    return ''
}

# Lista de ficheros de una carpeta con la sha256 de cada uno.
function Foto([string]$carpeta) {
    $lineas = Get-ChildItem -LiteralPath $carpeta -Recurse -File -Force | Sort-Object FullName |
        ForEach-Object { $_.FullName.Substring($carpeta.Length) + ' ' + (Huella $_.FullName) }
    return ($lineas -join "`n")
}

# ---------------------------------------------------------------------
# Casos. Cada uno devuelve '' si pasa, o el motivo del fallo.
# ---------------------------------------------------------------------
# 1. Limpia, sin base -> rc 0; programa, .lnk al exe y datos sin ficheros.
function Caso1([string]$raiz, [string]$cmd) {
    $origen = Montar $raiz $cmd $true
    $m = Rc-Distinto (Instalar $raiz $origen 'c1') 0
    if ($m) { return $m }
    $prog = Programa $raiz
    foreach ($f in @('Educhronos.exe', 'app\marca-app.txt', 'runtime\marca-runtime.txt')) {
        if (-not (Test-Path -LiteralPath (Join-Path $prog $f))) { return "falta Programs\Educhronos\$f" }
    }
    if (-not (Test-Path -LiteralPath $lnk)) { return 'no hay Educhronos.lnk en el Escritorio' }
    $destinoLnk = (New-Object -ComObject WScript.Shell).CreateShortcut($lnk).TargetPath
    $exe = Join-Path $prog 'Educhronos.exe'
    if ((Ruta-Larga $destinoLnk) -ne (Ruta-Larga $exe)) { return "el .lnk apunta a '$destinoLnk', no a '$exe'" }
    $datos = Datos $raiz
    if ((Test-Path -LiteralPath $datos) -and @(Get-ChildItem -LiteralPath $datos -Recurse -File -Force).Count -ne 0) { return 'la carpeta de datos tiene ficheros' }
    return ''
}

# 2. Limpia, con base-partida\una.db -> rc 0; datos tiene solo educhronos.db = una.db.
function Caso2([string]$raiz, [string]$cmd) {
    $origen = Montar $raiz $cmd $true
    $base = Poner-Base $origen 'una.db' "base de partida una`r`n"
    $m = Rc-Distinto (Instalar $raiz $origen 'c2') 0
    if ($m) { return $m }
    $datos = Datos $raiz
    if (-not (Test-Path -LiteralPath $datos)) { return 'no existe la carpeta de datos' }
    $ficheros = @(Get-ChildItem -LiteralPath $datos -Force)
    if ($ficheros.Count -ne 1 -or $ficheros[0].Name -ne 'educhronos.db') {
        return ('datos tiene: ' + (($ficheros | ForEach-Object { $_.Name }) -join ', '))
    }
    if ((Huella $ficheros[0].FullName) -ne (Huella $base)) { return 'educhronos.db no tiene la sha256 de una.db' }
    return ''
}

# 3. Datos con centinela.db y base -> rc 0; centinela intacto y sin educhronos.db.
function Caso3([string]$raiz, [string]$cmd) {
    $origen = Montar $raiz $cmd $true
    $null = Poner-Base $origen 'una.db' "base de partida una`r`n"
    $centinela = Poner-EnDatos $raiz 'centinela.db' "centinela s237`r`n"
    $h = Huella $centinela
    $m = Rc-Distinto (Instalar $raiz $origen 'c3') 0
    if ($m) { return $m }
    if (-not (Test-Path -LiteralPath $centinela)) { return 'centinela.db ha desaparecido' }
    if ((Huella $centinela) -ne $h) { return 'centinela.db ha cambiado' }
    if (Test-Path -LiteralPath (Join-Path (Datos $raiz) 'educhronos.db')) { return 'existe educhronos.db' }
    return ''
}

# 4. Datos con curso-abierto, sin .db, y base -> rc 0; sin educhronos.db.
function Caso4([string]$raiz, [string]$cmd) {
    $origen = Montar $raiz $cmd $true
    $null = Poner-Base $origen 'una.db' "base de partida una`r`n"
    $null = Poner-EnDatos $raiz 'curso-abierto' "otro-curso.db`r`n"
    $m = Rc-Distinto (Instalar $raiz $origen 'c4') 0
    if ($m) { return $m }
    if (Test-Path -LiteralPath (Join-Path (Datos $raiz) 'educhronos.db')) { return 'existe educhronos.db' }
    return ''
}

# 5. Dos .db en base-partida -> rc 0; sin educhronos.db.
function Caso5([string]$raiz, [string]$cmd) {
    $origen = Montar $raiz $cmd $true
    $null = Poner-Base $origen 'una.db' "base de partida una`r`n"
    $null = Poner-Base $origen 'otra.db' "base de partida otra`r`n"
    $m = Rc-Distinto (Instalar $raiz $origen 'c5') 0
    if ($m) { return $m }
    if (Test-Path -LiteralPath (Join-Path (Datos $raiz) 'educhronos.db')) { return 'existe educhronos.db' }
    return ''
}

# 6. Reinstalar encima con app\viejo.txt y datos\centinela.db -> rc 0; viejo.txt fuera y
#    centinela intacto.
function Caso6([string]$raiz, [string]$cmd) {
    $origen = Montar $raiz $cmd $true
    $m = Rc-Distinto (Instalar $raiz $origen 'c6-previa') 0
    if ($m) { return "instalacion previa: $m" }
    $viejo = Join-Path (Programa $raiz) 'app\viejo.txt'
    Escribir-Texto $viejo "viejo s237`r`n"
    $centinela = Poner-EnDatos $raiz 'centinela.db' "centinela s237`r`n"
    $h = Huella $centinela
    $m = Rc-Distinto (Instalar $raiz $origen 'c6') 0
    if ($m) { return $m }
    if (Test-Path -LiteralPath $viejo) { return 'app\viejo.txt sigue en Programs\Educhronos' }
    if (-not (Test-Path -LiteralPath $centinela)) { return 'centinela.db ha desaparecido' }
    if ((Huella $centinela) -ne $h) { return 'centinela.db ha cambiado' }
    return ''
}

# 7. Con el Educhronos.exe instalado en marcha -> rc 6; Programs\Educhronos identico.
function Caso7([string]$raiz, [string]$cmd) {
    $origen = Montar $raiz $cmd $true
    $m = Rc-Distinto (Instalar $raiz $origen 'c7-previa') 0
    if ($m) { return "instalacion previa: $m" }
    $prog = Programa $raiz
    $falso = Start-Process -FilePath (Join-Path $prog 'Educhronos.exe') -ArgumentList '-n 120 127.0.0.1' -WindowStyle Hidden -PassThru
    $idFalso = $falso.Id
    try {
        Start-Sleep -Seconds 1
        if ($falso.HasExited) { return 'el Educhronos.exe falso no sigue en marcha' }
        $antes = Foto $prog
        $m = Rc-Distinto (Instalar $raiz $origen 'c7') 6
        if ($m) { return $m }
        $despues = Foto $prog
        if ($despues -cne $antes) { return 'Programs\Educhronos ha cambiado' }
        return ''
    } finally {
        # Norma 5: por el PID guardado al lanzarlo.
        try { Stop-Process -Id $idFalso -Force } catch { Write-Host ("    aviso: no se pudo parar el falso {0}: {1}" -f $idFalso, $_.Exception.Message) }
        $null = $falso.WaitForExit(10000)
    }
}

# 8. Sin Educhronos.exe en el origen -> rc 2.
function Caso8([string]$raiz, [string]$cmd) {
    $origen = Montar $raiz $cmd $false
    return (Rc-Distinto (Instalar $raiz $origen 'c8') 2)
}

function Correr-Caso([int]$numero, [string]$cmd, [string]$etiqueta, [bool]$verSalidas) {
    $raiz = Nueva-Raiz
    $motivo = ''
    try {
        switch ($numero) {
            1 { $motivo = Caso1 $raiz $cmd }
            2 { $motivo = Caso2 $raiz $cmd }
            3 { $motivo = Caso3 $raiz $cmd }
            4 { $motivo = Caso4 $raiz $cmd }
            5 { $motivo = Caso5 $raiz $cmd }
            6 { $motivo = Caso6 $raiz $cmd }
            7 { $motivo = Caso7 $raiz $cmd }
            8 { $motivo = Caso8 $raiz $cmd }
        }
    } catch {
        $motivo = 'excepcion: ' + $_.Exception.Message
    } finally {
        if ($motivo -and $verSalidas) {
            Get-ChildItem -LiteralPath $raiz -Filter 'salida-*.txt' | ForEach-Object {
                Write-Host ("    --- {0}" -f $_.Name)
                Get-Content -LiteralPath $_.FullName | ForEach-Object { Write-Host ("    {0}" -f $_) }
            }
        }
        if (Test-Path -LiteralPath $lnk) { Remove-Item -LiteralPath $lnk -Force }
        try { Remove-Item -LiteralPath $raiz -Recurse -Force } catch { Write-Host ("    aviso: no se pudo borrar {0}: {1}" -f $raiz, $_.Exception.Message) }
    }
    if ($motivo -like 'agota el tiempo*') { $null = $agotados.Add(("{0}, caso {1}" -f $etiqueta, $numero)) }
    return [pscustomobject]@{ Caso = $numero; Ok = (-not $motivo); Motivo = $motivo }
}

function Correr-Casos([string]$cmd, [string]$etiqueta, [bool]$imprimir) {
    $resultados = @()
    foreach ($numero in 1..8) {
        $r = Correr-Caso $numero $cmd $etiqueta $imprimir
        if ($imprimir) {
            if ($r.Ok) { Write-Host ("caso {0}: OK" -f $numero) } else { Write-Host ("caso {0}: FALLO - {1}" -f $numero, $r.Motivo) }
        }
        $resultados += $r
    }
    return $resultados
}

function Informar-Agotados {
    if ($agotados.Count -gt 0) {
        Write-Host ""
        Write-Host "Agotaron el tiempo (pause espera?):"
        foreach ($a in $agotados) { Write-Host "  $a" }
    }
}

# ---------------------------------------------------------------------
# Mutantes (C2). Cada sustitucion es una linea entera del original; m6, la que empieza
# por el prefijo.
# ---------------------------------------------------------------------
$mutantes = @(
    @{ Nombre = 'm1'; Busca = 'if not "%NDATOS%"=="0" goto datos_con_base';            Pone = 'rem m1'; Prefijo = $false },
    @{ Nombre = 'm2'; Busca = 'if exist "%DATOS%\curso-abierto" goto datos_con_curso'; Pone = 'rem m2'; Prefijo = $false },
    @{ Nombre = 'm3'; Busca = 'if not errorlevel 1 goto abierto';                       Pone = 'rem m3'; Prefijo = $false },
    @{ Nombre = 'm4'; Busca = 'if exist "%DESTINO%\app\" rmdir /s /q "%DESTINO%\app"';  Pone = 'rem m4'; Prefijo = $false },
    @{ Nombre = 'm5'; Busca = 'if not "%NBASE%"=="1" goto varias_bases';                Pone = 'rem m5'; Prefijo = $false },
    @{ Nombre = 'm6'; Busca = 'powershell -NoProfile';                                  Pone = 'rem m6'; Prefijo = $true },
    @{ Nombre = 'm7'; Busca = 'copy "%BASE%" "%DATOS%\educhronos.db" >nul';             Pone = 'copy "%BASE%" "%DATOS%\" >nul'; Prefijo = $false }
)

if (-not $Autoprueba) {
    Write-Host ""
    $resultados = Correr-Casos $Instalador 'original' $true
    Informar-Agotados
    $fallos = @($resultados | Where-Object { -not $_.Ok }).Count
    Write-Host ""
    Write-Host ("{0} de 8 casos pasan." -f (8 - $fallos))
    if ($fallos -ne 0) { exit 1 }
    exit 0
}

# Antes de correr nada: cada sustitucion casa exactamente una vez en el original.
$lineasOriginal = [System.Text.Encoding]::ASCII.GetString($bytesOriginal) -split "`r`n"
$indices = @{}
foreach ($mutante in $mutantes) {
    $casan = @()
    for ($k = 0; $k -lt $lineasOriginal.Length; $k++) {
        $l = $lineasOriginal[$k]
        if (($mutante.Prefijo -and $l.StartsWith($mutante.Busca)) -or ((-not $mutante.Prefijo) -and ($l -ceq $mutante.Busca))) { $casan += $k }
    }
    if ($casan.Count -ne 1) {
        Write-Host ("ABORTA: la sustitucion de {0} casa {1} veces en el original; se esperaba 1." -f $mutante.Nombre, $casan.Count)
        exit 1
    }
    $indices[$mutante.Nombre] = $casan[0]
}
Write-Host "Las 7 sustituciones casan una vez cada una."

Write-Host ""
Write-Host "== Original =="
$resultados = Correr-Casos $Instalador 'original' $true
$fallos = @($resultados | Where-Object { -not $_.Ok }).Count
if ($fallos -ne 0) {
    Informar-Agotados
    Write-Host ""
    Write-Host ("FALLO: el original no pasa {0} de 8 casos; los mutantes no se corren." -f $fallos)
    exit 1
}

$carpetaMutantes = Nueva-Raiz
$tabla = @()
$vivos = 0
try {
    foreach ($mutante in $mutantes) {
        $lineas = [string[]]$lineasOriginal.Clone()
        $lineas[$indices[$mutante.Nombre]] = $mutante.Pone
        $carpeta = Join-Path $carpetaMutantes $mutante.Nombre
        New-Item -ItemType Directory -Path $carpeta | Out-Null
        $mutado = Join-Path $carpeta 'Instalar.cmd'
        Escribir-Texto $mutado ($lineas -join "`r`n")
        if ((Contar-LfSueltos ([System.IO.File]::ReadAllBytes($mutado))) -ne 0) { Write-Host ("ABORTA: el mutante {0} ha quedado con LF sin CR." -f $mutante.Nombre); exit 1 }
        Write-Host ("== Mutante {0} (linea {1}) ==" -f $mutante.Nombre, ($indices[$mutante.Nombre] + 1))
        $res = Correr-Casos $mutado $mutante.Nombre $false
        $marcas = ($res | ForEach-Object { if ($_.Ok) { '.' } else { 'x' } }) -join '  '
        $caen = @($res | Where-Object { -not $_.Ok } | ForEach-Object { $_.Caso })
        if ($caen.Count -eq 0) { $veredicto = 'SOBREVIVE'; $vivos++ } else { $veredicto = 'muere en ' + ($caen -join ',') }
        $tabla += ("{0}        {1}   {2}" -f $mutante.Nombre, $marcas, $veredicto)
    }
} finally {
    try { Remove-Item -LiteralPath $carpetaMutantes -Recurse -Force } catch { Write-Host ("aviso: no se pudo borrar {0}: {1}" -f $carpetaMutantes, $_.Exception.Message) }
}

Write-Host ""
Write-Host "mutante   c1 c2 c3 c4 c5 c6 c7 c8   (x = el caso falla)"
foreach ($fila in $tabla) { Write-Host $fila }
Informar-Agotados
Write-Host ""
if ($vivos -ne 0) {
    Write-Host ("FALLO: {0} de 7 mutantes sobreviven." -f $vivos)
    exit 1
}
Write-Host "OK: el original pasa los 8 casos y los 7 mutantes mueren."
exit 0
