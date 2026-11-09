param(
    [int]$Port = 5433,
    [string]$PgBin = 'C:\Program Files\PostgreSQL\18\bin'
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

if (-not (Test-Path (Join-Path $projectRoot '.env'))) {
    throw "Arquivo .env não encontrado. Crie-o a partir de .env.example antes de executar."
}

& (Join-Path $PSScriptRoot 'start-local-db.ps1') -Port $Port -PgBin $PgBin

Write-Host ""
Write-Host "Iniciando a API DeskFlow..."
& mvn spring-boot:run
exit $LASTEXITCODE
