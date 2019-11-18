param (
    [string] $SourceRoot = "$PSScriptRoot/..",

    [string] $EclipseUrl = 'https://archive.eclipse.org/eclipse/downloads/drops/R-3.5.2-201002111343/eclipse-SDK-3.5.2-win32-x86_64.zip',
    [string] $EclipseFileHash = '4E9EDDD6DC4F69C8A02C105DCBAF1A2DEDED6BBED138557DBC598323BBAA0AD7',

    [string] $EGitUrl = 'https://archive.eclipse.org/egit/updates-2.1/org.eclipse.egit.repository-2.1.0.201209190230-r.zip',
    [string] $EGitFileHash = 'AF2D9B6D946B734D5BC232DCF58A5A40980CAE78DB5E12B66FCAF1A970535328',

    [string] $Downloads = "$SourceRoot/.build/downloads",
    [string] $EclipseRoot = "$SourceRoot/.build/eclipse"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if (!(Test-Path -Type Container $Downloads)) {
    New-Item -Type Directory $Downloads
}

function isCached($url, $requiredHash, $filePath) {
    if (!(Test-Path $filePath)) {
        return $false
    }

    $hash = (Get-FileHash $filePath -Algorithm SHA256).Hash
    $hash -eq $requiredHash
}

function downloadFile($url, $requiredHash, $filePath) {
    Write-Output "Downloading $url ($requiredHash)"

    # Check if the download is already cached:
    if (Test-Path $filePath) {
        Write-Output "File found, checking hash: $filePath"
        $hash = (Get-FileHash $filePath -Algorithm SHA256).Hash
        if ($hash -ne $requiredHash) {
            Write-Output "Invalid cached file hash: $requiredHash expected, but got $hash"
            Write-Output "Redownloading $filePath"
            Remove-Item $filePath
        }
    }

    if (!(Test-Path $filePath)) {
        Write-Output "Downloading $filePath..."
        Invoke-WebRequest -UseBasicParsing $url -OutFile $filePath
        Write-Output "Verifying hash for $filePath..."
        $hash = (Get-FileHash $filePath -Algorithm SHA256).Hash
        if ($hash -ne $requiredHash) {
            throw "Invalid downloaded file hash: $requiredHash expected, but got $hash"
        }
    }
}

# First, check if we have every file in cache. If we are, then we don't need to either redownload or reexpand the
# files.
$cacheOk = (isCached $EclipseUrl $EclipseFileHash "$Downloads/eclipse.zip") -and (isCached $EGitUrl $EGitFileHash "$Downloads/egit.zip")
if (!$cacheOk) {
    # downloadFile will reuse cache if it is valid.
    downloadFile $EclipseUrl $EclipseFileHash "$Downloads/eclipse.zip"
    downloadFile $EGitUrl $EGitFileHash "$Downloads/egit.zip"

    Write-Output "Cleaning $EclipseRoot..."
    Remove-Item -Force $EclipseRoot -ErrorAction Continue
}

if (!(Test-Path $EclipseRoot)) {
    Write-Output 'Unpacking Eclipse...'
    Expand-Archive "$Downloads/eclipse.zip" -DestinationPath $EclipseRoot

    Write-Output 'Unpacking EGit...'
    Expand-Archive "$Downloads/egit.zip" -DestinationPath "$EclipseRoot/eclipse"
}