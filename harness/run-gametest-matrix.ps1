param([string]$RunLabel = '20260922', [string[]]$Nodes = @('26.1-neoforge', '26.1-fabric', '1.21.1-neoforge', '1.21.1-fabric', '1.20.1-forge', '1.20.1-fabric'))
$ErrorActionPreference = 'Stop'
$outDir = Join-Path $PSScriptRoot 'out'
$results = @()
Push-Location (Join-Path $PSScriptRoot '..')
try {
    foreach ($node in $nodes) {
        $task = if ($node.EndsWith('-fabric')) { 'runGametest' } else { 'runGameTestServer' }
        $log = Join-Path $outDir "gametest-$node-$RunLabel.log"
        Write-Output "START $node $task"
        & .\gradlew.bat ":${node}:$task" --configure-on-demand --console=plain *> $log
        $taskExit = $LASTEXITCODE
        $passSummary = Select-String -LiteralPath $log -Pattern '(?i)All (\d+) required tests passed' | Select-Object -Last 1
        $passedTests = if ($passSummary) { [int]$passSummary.Matches[0].Groups[1].Value } else { 0 }
        # A successful process that omitted HBM registration is not a successful suite.
        if ($taskExit -eq 0 -and $passedTests -lt 717) { $taskExit = 1 }
        $result = [pscustomobject]@{ node = $node; exitCode = $taskExit; passedTests = $passedTests; log = $log }
        $results += $result
        $results | ConvertTo-Json | Set-Content (Join-Path $outDir "gametest-matrix-$RunLabel.json")
        Write-Output "END $node exit=$($result.exitCode) log=$log"
    }
} finally { Pop-Location }
if (@($results | Where-Object exitCode -ne 0).Count -gt 0) { exit 1 }
