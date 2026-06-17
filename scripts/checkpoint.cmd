@echo off
setlocal

set "MESSAGE=%~1"
if "%MESSAGE%"=="" set "MESSAGE=progress checkpoint"

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0checkpoint.ps1" -Message "%MESSAGE%"
