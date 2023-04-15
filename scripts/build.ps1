param (
    [string] $SourceRoot = "$PSScriptRoot/..",
    [string] $EclipseRoot = "$SourceRoot/.build/eclipse"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Write-Output 'Java version identification'
java -version

Push-Location "$SourceRoot/build"
try {
    ant "-Ddir.machine.build-runtime=$EclipseRoot/eclipse"
    if (!$?) {
        throw "Ant exit code: $LASTEXITCODE"
    }
} finally {
    Pop-Location
}
