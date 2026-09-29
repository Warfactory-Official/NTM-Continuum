param(
    [string]$RunLabel = '20260922',
    [string[]]$Checks = @('checks/ClientSmoke.java'),
    [string[]]$Pairs = @('26.1-neoforge,26.1-fabric', '1.21.1-neoforge,1.21.1-fabric', '1.20.1-forge,1.20.1-fabric'),
    [switch]$NoBuild
)
$ErrorActionPreference = 'Stop'
# The CLI lives in the Golem Core checkout next to this repository (../Golem-Core/harness).
$Harness = Join-Path $PSScriptRoot '..\..\Golem-Core\harness\harness.cmd'
$results = @()
Push-Location $PSScriptRoot
try {
    foreach ($pair in $Pairs) {
        try {
            Write-Output "START clients $pair"
            $startup = @('up', '--nodes', $pair)
            if ($NoBuild) { $startup += '--no-build' }
            & $Harness @startup
            if ($LASTEXITCODE -ne 0) { throw "Client startup failed for $pair" }
            foreach ($check in $Checks) {
                & $Harness run $check
                $results += [pscustomobject]@{ nodes = $pair; check = $check; exitCode = $LASTEXITCODE }
            }
        } catch {
            Write-Output $_
            $results += [pscustomobject]@{ nodes = $pair; exitCode = 1; error = "$_" }
        } finally {
            & $Harness down
        }
        $results | ConvertTo-Json | Set-Content "out/client-matrix-$RunLabel.json"
    }
} finally { Pop-Location }
if (@($results | Where-Object exitCode -ne 0).Count -gt 0) { exit 1 }
