$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Push-Location $repoRoot
try {
    mvn compile
    mvn exec:java "-Dexec.mainClass=edu.course.brigdelt.GdeltInputDataImportCheck"
} finally {
    Pop-Location
}
