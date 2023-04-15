param (
    [string] $SourceRoot = "$PSScriptRoot/..",
    [string] $EclipseRoot = "$SourceRoot/.build/eclipse"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Push-Location "$SourceRoot/build"
try {
    ant "-Ddir.machine.build-runtime=$EclipseRoot/eclipse" -v
    if (!$?) {
        throw "Ant exit code: $LASTEXITCODE"
    }
} finally {
    Pop-Location
}
