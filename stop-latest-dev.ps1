$ErrorActionPreference = 'Stop'

$commonPath = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) 'scripts\startup\LatestDev.Common.ps1'
. $commonPath

$context = New-LatestDevContext -ScriptPath $MyInvocation.MyCommand.Path

Stop-LatestDevRuntime -Context $context -IncludeDockerContainers

Write-Host "Latest source runtime stopped for ports $($context.BackendPort) and $($context.FrontendPort)."
