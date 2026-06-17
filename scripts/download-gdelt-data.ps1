param(
    [Parameter(Mandatory = $true)]
    [string[]]$Dates,

    [string]$OutputDir = "D:\Temp\BRI-GDELT-System\input",

    [int]$MaxRetries = 3
)

$ErrorActionPreference = "Stop"

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$timestamps = @()
foreach ($item in $Dates) {
    foreach ($value in ($item -split ",")) {
        $trimmed = $value.Trim()
        if ($trimmed.Length -gt 0) {
            if ($trimmed -match "^\d{8}$") {
                $day = [datetime]::ParseExact($trimmed, "yyyyMMdd", $null)
                for ($slot = 0; $slot -lt 96; $slot++) {
                    $timestamps += $day.AddMinutes($slot * 15).ToString("yyyyMMddHHmmss")
                }
            } elseif ($trimmed -match "^\d{14}$") {
                $instant = [datetime]::ParseExact($trimmed, "yyyyMMddHHmmss", $null)
                if ($instant.Second -ne 0 -or ($instant.Minute % 15) -ne 0) {
                    throw "Invalid timestamp '$trimmed'. GDELT 2.0 event files use 15-minute slots, for example 20250601000000 or 20250601001500."
                }
                $timestamps += $trimmed
            } else {
                throw "Invalid value '$trimmed'. Use YYYYMMDD for a full day or YYYYMMDDHHMMSS for one 15-minute file."
            }
        }
    }
}

if ($timestamps.Count -eq 0) {
    throw "No dates provided. Example: scripts/download-gdelt-data.ps1 -Dates 20250601 or 20250601000000"
}

$failedFiles = @()

foreach ($timestamp in ($timestamps | Select-Object -Unique)) {
    $fileName = "$timestamp.export.CSV.zip"
    $target = Join-Path $OutputDir $fileName
    $url = "http://data.gdeltproject.org/gdeltv2/$fileName"

    if (Test-Path -LiteralPath $target) {
        Write-Host "SKIP  $fileName already exists at $target"
        continue
    }

    $downloaded = $false
    for ($attempt = 1; $attempt -le $MaxRetries; $attempt++) {
        Write-Host "GET   $url attempt $attempt/$MaxRetries"
        try {
            Invoke-WebRequest -Uri $url -OutFile $target
            Write-Host "SAVED $target"
            $downloaded = $true
            break
        } catch {
            if (Test-Path -LiteralPath $target) {
                Remove-Item -LiteralPath $target -Force
            }
            if ($attempt -lt $MaxRetries) {
                Start-Sleep -Seconds ([Math]::Min(10, $attempt * 2))
            } else {
                Write-Warning "FAILED $fileName $($_.Exception.Message)"
                $failedFiles += $fileName
            }
        }
    }
}

if ($failedFiles.Count -gt 0) {
    Write-Warning "Some files failed after retries. You can rerun this script later; existing files will be skipped."
    $failedFiles | ForEach-Object { Write-Warning "  $_" }
    exit 1
}
