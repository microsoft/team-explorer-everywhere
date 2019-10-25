param (
    [string] $SourceRoot = "$PSScriptRoot/..",

    [string] $EclipseUrl = 'https://archive.eclipse.org/eclipse/downloads/drops/R-3.5.2-201002111343/eclipse-SDK-3.5.2-win32-x86_64.zip',
    [string] $EclipseFileHash = '4E9EDDD6DC4F69C8A02C105DCBAF1A2DEDED6BBED138557DBC598323BBAA0AD7',

    [string] $Downloads = "$SourceRoot/.appveyor/downloads",
    [string] $EclipseRoot = "$SourceRoot/.appveyor/eclipse"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if (!(Test-Path -Type Container $Downloads)) {
    New-Item -Type Directory $Downloads
}

$fileName = $EclipseUrl.Split('/') | Select-Object -Last 1
$eclipseFilePath = "$Downloads/$fileName"

# Check if the download is already cached:
if (Test-Path $eclipseFilePath) {
    Write-Output "File found, checking hash: $eclipseFilePath"
    $hash = (Get-FileHash $eclipseFilePath -Algorithm SHA256).Hash
    if ($hash -ne $EclipseFileHash) {
        Write-Output "Invalid cached file hash: $EclipseFileHash expected, but got $hash"
        Write-Output "Redownloading $eclipseFilePath"
        Remove-Item $eclipseFilePath
    }
}

if (!(Test-Path $eclipseFilePath)) {
    Write-Output "Downloading Eclipse..."
    Invoke-WebRequest -UseBasicParsing $EclipseUrl -OutFile $eclipseFilePath
    Write-Output "Verifying hash..."
    $hash = (Get-FileHash $eclipseFilePath -Algorithm SHA256).Hash
    if ($hash -ne $EclipseFileHash) {
        throw "Invalid downloaded file hash: $EclipseFileHash expected, but got $hash"
    }

    Remove-Item -Recurse $EclipseRoot -ErrorAction SilentlyContinue
}

if (!(Test-Path $EclipseRoot)) {
    Write-Output "Unpacking Eclipse..."
    Expand-Archive $eclipseFilePath -DestinationPath $EclipseRoot

}
