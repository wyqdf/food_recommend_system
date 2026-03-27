Set-StrictMode -Version Latest

$script:DockerUsable = $null

function Test-CommandAvailable {
    param([string]$Name)

    return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Get-NormalizedPort {
    param(
        [string]$Value,
        [int]$Default
    )

    $parsed = 0
    if ($Value -and [int]::TryParse($Value, [ref]$parsed) -and $parsed -gt 0) {
        return $parsed
    }

    return $Default
}

function New-LatestDevContext {
    param([string]$ScriptPath)

    $repoRoot = Split-Path -Parent $ScriptPath
    $tempDir = Join-Path $repoRoot 'temp'

    $backendPort = Get-NormalizedPort -Value $env:BACKEND_PORT -Default 8081
    $frontendPort = Get-NormalizedPort -Value $env:FRONTEND_PORT -Default 3000

    return [pscustomobject]@{
        RepoRoot        = $repoRoot
        BackendDir      = Join-Path $repoRoot 'backend\let-me-cook'
        FrontendDir     = Join-Path $repoRoot 'frontend'
        TempDir         = $tempDir
        BackendPidFile  = Join-Path $tempDir 'backend-dev.pid'
        FrontendPidFile = Join-Path $tempDir 'frontend-dev.pid'
        BackendLog      = Join-Path $tempDir 'backend-dev.log'
        BackendErrLog   = Join-Path $tempDir 'backend-dev.err.log'
        FrontendLog     = Join-Path $tempDir 'frontend-dev.log'
        FrontendErrLog  = Join-Path $tempDir 'frontend-dev.err.log'
        BackendPort     = $backendPort
        FrontendPort    = $frontendPort
        SearchEngine    = if ([string]::IsNullOrWhiteSpace($env:SEARCH_ENGINE)) { 'auto' } else { $env:SEARCH_ENGINE }
        OssEnabled      = if ([string]::IsNullOrWhiteSpace($env:ALIYUN_OSS_ENABLED)) { 'false' } else { $env:ALIYUN_OSS_ENABLED }
        DockerContainers = @('food-backend', 'food-frontend')
    }
}

function Ensure-Directory {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path $Path | Out-Null
    }
}

function Ensure-RequiredCommands {
    param([string[]]$Names)

    $missing = @()
    foreach ($name in $Names) {
        if (-not (Test-CommandAvailable -Name $name)) {
            $missing += $name
        }
    }

    if ($missing.Count -gt 0) {
        throw "Missing required command(s): $($missing -join ', ')"
    }
}

function Read-Pid {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return $null
    }

    $raw = Get-Content -LiteralPath $Path -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $raw) {
        return $null
    }

    $processId = 0
    if ([int]::TryParse($raw, [ref]$processId)) {
        return $processId
    }

    return $null
}

function Stop-ManagedProcess {
    param(
        [string]$PidFile,
        [string]$Name
    )

    $processId = Read-Pid -Path $PidFile
    if (-not $processId) {
        return
    }

    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if ($process) {
        Write-Host "Stopping $Name process $processId ..."
        Stop-Process -Id $processId -Force
        Start-Sleep -Seconds 2
    }

    Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
}

function Stop-RepoPortProcess {
    param(
        [string]$RepoRoot,
        [int]$Port,
        [string]$Name
    )

    $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if (-not $connections) {
        return
    }

    foreach ($connection in $connections) {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId = $($connection.OwningProcess)" -ErrorAction SilentlyContinue
        if (-not $process) {
            continue
        }

        $commandLine = $process.CommandLine
        if ($commandLine -and $commandLine.Contains($RepoRoot)) {
            Write-Host "Stopping $Name process on port $Port (PID $($connection.OwningProcess)) ..."
            Stop-Process -Id $connection.OwningProcess -Force
            Start-Sleep -Seconds 2
        }
    }
}

function Assert-PortFree {
    param(
        [int]$Port,
        [string]$Name
    )

    $connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if ($connections) {
        $ownerIds = ($connections | Select-Object -ExpandProperty OwningProcess -Unique) -join ', '
        throw "$Name port $Port is still occupied by process $ownerIds."
    }
}

function Test-DockerUsable {
    if ($null -ne $script:DockerUsable) {
        return $script:DockerUsable
    }

    if (-not (Test-CommandAvailable -Name 'docker')) {
        $script:DockerUsable = $false
        return $false
    }

    & docker ps -q *> $null
    $script:DockerUsable = ($LASTEXITCODE -eq 0)
    return $script:DockerUsable
}

function Stop-DockerContainerIfRunning {
    param([string]$Name)

    if (-not (Test-DockerUsable)) {
        return
    }

    $containerId = & docker ps -q --filter "name=^/${Name}$" 2>$null
    if ($LASTEXITCODE -ne 0) {
        return
    }

    if ($containerId) {
        Write-Host "Stopping Docker container $Name ..."
        & docker stop $Name | Out-Null
    }
}

function Stop-LatestDevRuntime {
    param(
        [pscustomobject]$Context,
        [switch]$IncludeDockerContainers
    )

    if ($IncludeDockerContainers) {
        foreach ($containerName in $Context.DockerContainers) {
            Stop-DockerContainerIfRunning -Name $containerName
        }
    }

    Stop-ManagedProcess -PidFile $Context.BackendPidFile -Name 'backend'
    Stop-ManagedProcess -PidFile $Context.FrontendPidFile -Name 'frontend'
    Stop-RepoPortProcess -RepoRoot $Context.RepoRoot -Port $Context.BackendPort -Name 'backend'
    Stop-RepoPortProcess -RepoRoot $Context.RepoRoot -Port $Context.FrontendPort -Name 'frontend'
}

function Ensure-FrontendDependencies {
    param([pscustomobject]$Context)

    $viteCmd = Join-Path $Context.FrontendDir 'node_modules\.bin\vite.cmd'
    $viteShell = Join-Path $Context.FrontendDir 'node_modules\.bin\vite'
    if ((Test-Path -LiteralPath $viteCmd) -or (Test-Path -LiteralPath $viteShell)) {
        return
    }

    Write-Host 'Frontend dependencies are missing. Running npm install ...'
    Push-Location $Context.FrontendDir
    try {
        & npm install
        if ($LASTEXITCODE -ne 0) {
            throw 'npm install failed.'
        }
    } finally {
        Pop-Location
    }
}

function Start-LoggedProcess {
    param(
        [string]$WorkingDirectory,
        [string]$Command,
        [string]$StdOutPath,
        [string]$StdErrPath,
        [string]$PidFile,
        [ValidateSet('pwsh', 'cmd')]
        [string]$ShellType = 'pwsh'
    )

    if ($ShellType -eq 'cmd') {
        $shell = (Get-Command cmd.exe).Source
        $argumentList = @('/d', '/c', $Command)
    } else {
        $shell = (Get-Command pwsh -ErrorAction SilentlyContinue).Source
        if (-not $shell) {
            $shell = (Get-Command powershell.exe).Source
        }
        $encodedCommand = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($Command))
        $argumentList = @('-NoProfile', '-EncodedCommand', $encodedCommand)
    }

    if (Test-Path -LiteralPath $StdOutPath) {
        Remove-Item -LiteralPath $StdOutPath -Force
    }
    if (Test-Path -LiteralPath $StdErrPath) {
        Remove-Item -LiteralPath $StdErrPath -Force
    }

    $process = Start-Process `
        -FilePath $shell `
        -ArgumentList $argumentList `
        -WorkingDirectory $WorkingDirectory `
        -RedirectStandardOutput $StdOutPath `
        -RedirectStandardError $StdErrPath `
        -PassThru

    Set-Content -LiteralPath $PidFile -Value $process.Id
    return $process
}

function Wait-HttpOk {
    param(
        [string]$Url,
        [string]$Name,
        [int]$Retries = 60,
        [int]$DelaySeconds = 2
    )

    for ($i = 0; $i -lt $Retries; $i++) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 5
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                Write-Host "$Name is ready: $Url"
                return
            }
        } catch {
        }
        Start-Sleep -Seconds $DelaySeconds
    }

    throw "$Name did not become ready: $Url"
}

function Get-ListenerProcessId {
    param([int]$Port)

    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($connection) {
        return $connection.OwningProcess
    }

    return $null
}

function Show-LogTail {
    param(
        [string]$Path,
        [string]$Title
    )

    if (Test-Path -LiteralPath $Path) {
        Write-Host ''
        Write-Host "===== $Title ====="
        Get-Content -LiteralPath $Path -Tail 80
    }
}

function Get-BackendLaunchCommand {
    param([pscustomobject]$Context)

    $mavenCommand = if (Test-Path -LiteralPath (Join-Path $Context.BackendDir 'mvnw.cmd')) { '.\mvnw.cmd' } else { 'mvn' }
    return @(
        "set ALIYUN_OSS_ENABLED=$($Context.OssEnabled)",
        "set SEARCH_ENGINE=$($Context.SearchEngine)",
        "set SERVER_PORT=$($Context.BackendPort)",
        "$mavenCommand -Dmaven.test.skip=true spring-boot:run"
    ) -join '&& '
}
