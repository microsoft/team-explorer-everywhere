param (
    [string] $SourceRoot = "$PSScriptRoot/..",
    [string] $EclipseRoot = "$SourceRoot/.appveyor/eclipse"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

Push-Location "$SourceRoot/build"
try {
    ant "-Ddir.machine.build-runtime=$EclipseRoot/eclipse"
    if (!$?) {
        throw "Ant exit code: $LASTEXITCODE"
    }
} finally {
    Pop-Location
}
