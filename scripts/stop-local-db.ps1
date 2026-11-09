param(
    [string]$PgBin = 'C:\Program Files\PostgreSQL\18\bin'
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$dataDir = Join-Path $projectRoot '.postgres\data'
$pgCtl = Join-Path $PgBin 'pg_ctl.exe'

if (-not (Test-Path (Join-Path $dataDir 'PG_VERSION'))) {
    throw "Cluster não encontrado em '$dataDir'. Execute scripts/setup-local-db.ps1 primeiro."
}

& $pgCtl -D $dataDir status *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host "O cluster PostgreSQL já está parado."
    exit 0
}

& $pgCtl -D $dataDir -m fast -w stop
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao parar o cluster. Consulte $dataDir."
}

Write-Host "Cluster PostgreSQL parado."
