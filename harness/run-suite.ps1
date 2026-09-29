param([string]$Nodes = '26.1-neoforge,26.1-fabric')
$ErrorActionPreference = 'Stop'
# The CLI lives in the Golem Core checkout next to this repository (../Golem-Core/harness).
$Harness = Join-Path $PSScriptRoot '..\..\Golem-Core\harness\harness.cmd'
Push-Location $PSScriptRoot
try {
    & $Harness up --nodes $Nodes --no-sodium
    if ($LASTEXITCODE -ne 0) { throw "Client startup failed for $Nodes" }
    $failed = @()
    foreach ($scenario in (Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'scenarios') -Filter '*.java' | Sort-Object Name)) {
        & $Harness run $scenario.Name
        if ($LASTEXITCODE -ne 0) { $failed += $scenario.Name }
    }
    Write-Output "SUITE nodes=$Nodes failed=$($failed -join ',')"
    if ($failed.Count -gt 0) { exit 1 }
} finally {
    & $Harness down
    Pop-Location
}
