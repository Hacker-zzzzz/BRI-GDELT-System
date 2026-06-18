param(
    [Parameter(Mandatory = $true)]
    [string[]]$Dates,

    [string]$OutputDir = "D:\Temp\BRI-GDELT-System\input",

    [int]$MaxRetries = 3,

    [int]$ThrottleLimit = 8,

    [string]$MasterFileListUrl = "http://data.gdeltproject.org/gdeltv2/masterfilelist.txt",

    [string]$CacheDir = "D:\Temp\BRI-GDELT-System\cache",

    [int]$MasterListCacheHours = 12,

    [switch]$RefreshMasterList,

    [switch]$ListOnly
)

$ErrorActionPreference = "Stop"

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
New-Item -ItemType Directory -Force -Path $CacheDir | Out-Null

$datePrefixes = New-Object 'System.Collections.Generic.HashSet[string]'
$exactTimestamps = New-Object 'System.Collections.Generic.HashSet[string]'

foreach ($item in $Dates) {
    foreach ($value in ($item -split ",")) {
        $trimmed = $value.Trim()
        if ($trimmed.Length -eq 0) {
            continue
        }
        if ($trimmed -match "^\d{8}$") {
            [void][datetime]::ParseExact($trimmed, "yyyyMMdd", $null)
            [void]$datePrefixes.Add($trimmed)
        } elseif ($trimmed -match "^\d{14}$") {
            $instant = [datetime]::ParseExact($trimmed, "yyyyMMddHHmmss", $null)
            if ($instant.Second -ne 0 -or ($instant.Minute % 15) -ne 0) {
                throw "Invalid timestamp '$trimmed'. GDELT 2.0 event files use 15-minute slots, for example 20250601000000 or 20250601001500."
            }
            [void]$exactTimestamps.Add($trimmed)
        } else {
            throw "Invalid value '$trimmed'. Use YYYYMMDD for a day or YYYYMMDDHHMMSS for one 15-minute file."
        }
    }
}

if ($datePrefixes.Count -eq 0 -and $exactTimestamps.Count -eq 0) {
    throw "No dates provided. Example: scripts/download-gdelt-data.ps1 -Dates 20250601 or 20250601000000"
}

if ($ThrottleLimit -lt 1) {
    throw "ThrottleLimit must be at least 1."
}

function Read-MasterListCache {
    param([string]$Path)

    for ($attempt = 1; $attempt -le 5; $attempt++) {
        try {
            return Get-Content -LiteralPath $Path
        } catch {
            if ($attempt -eq 5) {
                throw
            }
            Start-Sleep -Milliseconds 500
        }
    }
}

$masterListCacheFile = Join-Path $CacheDir "gdeltv2-masterfilelist.txt"
$useCachedMasterList = $false
if (-not $RefreshMasterList -and (Test-Path -LiteralPath $masterListCacheFile)) {
    $cacheAge = (Get-Date) - (Get-Item -LiteralPath $masterListCacheFile).LastWriteTime
    $useCachedMasterList = $cacheAge.TotalHours -lt $MasterListCacheHours
}

if ($useCachedMasterList) {
    Write-Host "READ  cached master list $masterListCacheFile"
    $masterLines = Read-MasterListCache -Path $masterListCacheFile
} else {
    Write-Host "GET   $MasterFileListUrl"
    $tempMasterListCacheFile = "$masterListCacheFile.$PID.tmp"
    Invoke-WebRequest -Uri $MasterFileListUrl -OutFile $tempMasterListCacheFile -UseBasicParsing
    Move-Item -LiteralPath $tempMasterListCacheFile -Destination $masterListCacheFile -Force
    $masterLines = Read-MasterListCache -Path $masterListCacheFile
}

$downloadItems = New-Object System.Collections.Generic.List[object]
$seenFiles = New-Object 'System.Collections.Generic.HashSet[string]'

foreach ($line in $masterLines) {
    if ($line -notmatch "http://data\.gdeltproject\.org/gdeltv2/(\d{14})\.export\.CSV\.zip") {
        continue
    }

    $timestamp = $Matches[1]
    $datePrefix = $timestamp.Substring(0, 8)
    if (-not $exactTimestamps.Contains($timestamp) -and -not $datePrefixes.Contains($datePrefix)) {
        continue
    }

    $url = $Matches[0]
    $fileName = "$timestamp.export.CSV.zip"
    if (-not $seenFiles.Add($fileName)) {
        continue
    }

    $downloadItems.Add([pscustomobject]@{
        Timestamp = $timestamp
        FileName = $fileName
        Url = $url
        Target = Join-Path $OutputDir $fileName
    }) | Out-Null
}

$downloadItems = @($downloadItems | Sort-Object Timestamp)

Write-Host "FOUND $($downloadItems.Count) existing GDELT event files in master list."
if ($downloadItems.Count -eq 0) {
    Write-Warning "No matching files were found in the official master file list. Try another date range."
    exit 1
}

if ($ListOnly) {
    $downloadItems | Select-Object Timestamp, FileName, Url
    exit 0
}

$pendingItems = @($downloadItems | Where-Object {
        if (Test-Path -LiteralPath $_.Target) {
            Write-Host "SKIP  $($_.FileName) already exists at $($_.Target)"
            $false
        } else {
            $true
        }
    })

Write-Host "TODO  $($pendingItems.Count) files need downloading."
if ($pendingItems.Count -eq 0) {
    exit 0
}

$downloadScript = {
    param($Url, $Target, $FileName, $MaxRetries)

    for ($attempt = 1; $attempt -le $MaxRetries; $attempt++) {
        try {
            Invoke-WebRequest -Uri $Url -OutFile $Target -UseBasicParsing
            return [pscustomobject]@{
                Success = $true
                FileName = $FileName
                Message = "SAVED $Target"
            }
        } catch {
            if (Test-Path -LiteralPath $Target) {
                Remove-Item -LiteralPath $Target -Force
            }
            if ($attempt -lt $MaxRetries) {
                Start-Sleep -Seconds ([Math]::Min(10, $attempt * 2))
            } else {
                return [pscustomobject]@{
                    Success = $false
                    FileName = $FileName
                    Message = $_.Exception.Message
                }
            }
        }
    }
}

$jobs = @()
$failedFiles = @()
$completed = 0

foreach ($item in $pendingItems) {
    while (@($jobs | Where-Object { $_.State -eq "Running" }).Count -ge $ThrottleLimit) {
        $finished = Wait-Job -Job $jobs -Any
        $result = Receive-Job -Job $finished
        Remove-Job -Job $finished
        $jobs = @($jobs | Where-Object { $_.Id -ne $finished.Id })
        $completed++
        if ($result.Success) {
            Write-Host "DONE  [$completed/$($pendingItems.Count)] $($result.FileName)"
        } else {
            Write-Warning "FAILED [$completed/$($pendingItems.Count)] $($result.FileName) $($result.Message)"
            $failedFiles += $result.FileName
        }
    }

    Write-Host "QUEUE $($item.FileName)"
    $jobs += Start-Job -ScriptBlock $downloadScript -ArgumentList $item.Url, $item.Target, $item.FileName, $MaxRetries
}

while ($jobs.Count -gt 0) {
    $finished = Wait-Job -Job $jobs -Any
    $result = Receive-Job -Job $finished
    Remove-Job -Job $finished
    $jobs = @($jobs | Where-Object { $_.Id -ne $finished.Id })
    $completed++
    if ($result.Success) {
        Write-Host "DONE  [$completed/$($pendingItems.Count)] $($result.FileName)"
    } else {
        Write-Warning "FAILED [$completed/$($pendingItems.Count)] $($result.FileName) $($result.Message)"
        $failedFiles += $result.FileName
    }
}

if ($failedFiles.Count -gt 0) {
    Write-Warning "Some files failed after retries. You can rerun this script later; existing files will be skipped."
    $failedFiles | ForEach-Object { Write-Warning "  $_" }
    exit 1
}
