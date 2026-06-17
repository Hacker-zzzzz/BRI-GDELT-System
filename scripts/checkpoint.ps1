param(
    [string]$Message = "progress checkpoint"
)

$ErrorActionPreference = "Stop"

$repoRoot = git rev-parse --show-toplevel 2>$null
if (-not $repoRoot) {
    throw "Current directory is not a Git repository. Run git init first."
}

Set-Location $repoRoot

$changes = git status --porcelain
if (-not $changes) {
    Write-Host "No file changes detected. Checkpoint commit was not created."
    exit 0
}

$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$commitMessage = "checkpoint: $Message ($timestamp)"

git add -A
git commit -m $commitMessage

Write-Host "Created checkpoint commit: $commitMessage"
