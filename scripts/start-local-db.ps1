param(
    [int]$Port = 5433,
    [string]$PgBin = 'C:\Program Files\PostgreSQL\18\bin'
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$dataDir = Join-Path $projectRoot '.postgres\data'
$logFile = Join-Path $projectRoot '.postgres\postgres.log'
$pgCtl = Join-Path $PgBin 'pg_ctl.exe'

if (-not (Test-Path (Join-Path $dataDir 'PG_VERSION'))) {
    throw "Cluster não encontrado em '$dataDir'. Execute scripts/setup-local-db.ps1 primeiro."
}

& $pgCtl -D $dataDir status *> $null
if ($LASTEXITCODE -eq 0) {
    Write-Host "O cluster PostgreSQL já está em execução na porta $Port."
    exit 0
}

& $pgCtl -D $dataDir -l $logFile -o "-p $Port" -w start
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao iniciar o cluster. Consulte $logFile."
}

Write-Host "Cluster PostgreSQL iniciado na porta $Port."
