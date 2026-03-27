$ErrorActionPreference = 'Stop'

$commonPath = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) 'scripts\startup\LatestDev.Common.ps1'
. $commonPath

$context = New-LatestDevContext -ScriptPath $MyInvocation.MyCommand.Path

Ensure-Directory -Path $context.TempDir
Ensure-RequiredCommands -Names @('npm')
if (-not (Test-Path -LiteralPath (Join-Path $context.BackendDir 'mvnw.cmd'))) {
    Ensure-RequiredCommands -Names @('mvn')
}

Ensure-FrontendDependencies -Context $context
Stop-LatestDevRuntime -Context $context -IncludeDockerContainers

Assert-PortFree -Port $context.BackendPort -Name 'backend'
Assert-PortFree -Port $context.FrontendPort -Name 'frontend'

$backendCommand = Get-BackendLaunchCommand -Context $context
$frontendCommand = "npm run dev -- --host 127.0.0.1 --port $($context.FrontendPort) --strictPort"

Write-Host "Starting latest backend from source ..."
$backendProcess = Start-LoggedProcess `
    -WorkingDirectory $context.BackendDir `
    -Command $backendCommand `
    -StdOutPath $context.BackendLog `
    -StdErrPath $context.BackendErrLog `
    -PidFile $context.BackendPidFile `
    -ShellType 'cmd'

Write-Host "Starting latest frontend from source ..."
$frontendProcess = Start-LoggedProcess `
    -WorkingDirectory $context.FrontendDir `
    -Command $frontendCommand `
    -StdOutPath $context.FrontendLog `
    -StdErrPath $context.FrontendErrLog `
    -PidFile $context.FrontendPidFile

try {
    Wait-HttpOk -Url "http://127.0.0.1:$($context.BackendPort)/api/categories" -Name 'backend'
    Wait-HttpOk -Url "http://127.0.0.1:$($context.FrontendPort)" -Name 'frontend'
} catch {
    Show-LogTail -Path $context.BackendLog -Title 'backend log'
    Show-LogTail -Path $context.BackendErrLog -Title 'backend error log'
    Show-LogTail -Path $context.FrontendLog -Title 'frontend log'
    Show-LogTail -Path $context.FrontendErrLog -Title 'frontend error log'
    throw
}

$backendListenerPid = Get-ListenerProcessId -Port $context.BackendPort
if ($backendListenerPid) {
    Set-Content -LiteralPath $context.BackendPidFile -Value $backendListenerPid
}

$frontendListenerPid = Get-ListenerProcessId -Port $context.FrontendPort
if ($frontendListenerPid) {
    Set-Content -LiteralPath $context.FrontendPidFile -Value $frontendListenerPid
}

if (-not (Test-DockerUsable)) {
    Write-Host 'Docker is unavailable. Skipped container cleanup and kept source startup only.'
}

Write-Host ''
Write-Host "Latest source frontend: http://127.0.0.1:$($context.FrontendPort)"
Write-Host "Latest source backend:  http://127.0.0.1:$($context.BackendPort)"
Write-Host "Backend log:            $($context.BackendLog)"
Write-Host "Backend err log:        $($context.BackendErrLog)"
Write-Host "Frontend log:           $($context.FrontendLog)"
Write-Host "Frontend err log:       $($context.FrontendErrLog)"
Write-Host "Backend PID:            $(if ($backendListenerPid) { $backendListenerPid } else { $backendProcess.Id })"
Write-Host "Frontend PID:           $(if ($frontendListenerPid) { $frontendListenerPid } else { $frontendProcess.Id })"
