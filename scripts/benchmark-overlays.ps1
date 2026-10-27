param([string]$JavaHome = '')
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
if (!$JavaHome) { $JavaHome=Join-Path $root '.local/jdk/jdk-21.0.12.1+1' }
$output=Join-Path $root '.local/overlay-benchmark'
New-Item -ItemType Directory -Force $output | Out-Null
& (Join-Path $JavaHome 'bin/javac.exe') -d $output (Join-Path $root 'minecraft/fabric-1.21.11/src/main/java/gg/hera/features/OverlayPolicy.java') (Join-Path $root 'minecraft/fabric-1.21.11/tools/ClipBenchmark.java')
if ($LASTEXITCODE) { throw 'Benchmark compilation failed' }
foreach ($trial in 1..3) {
    Write-Output "Trial $trial (isolated clipping CPU/allocation only)"
    foreach ($mode in @('before','after')) {
        & (Join-Path $JavaHome 'bin/java.exe') -Xms256m -Xmx256m -cp $output ClipBenchmark $mode
        if ($LASTEXITCODE) { throw 'Benchmark failed' }
    }
}
