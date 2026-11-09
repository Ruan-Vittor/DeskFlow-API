param(
    [int]$Port = 5433,
    [string]$PgBin = 'C:\Program Files\PostgreSQL\18\bin'
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$dataDir = Join-Path $projectRoot '.postgres\data'
$logFile = Join-Path $projectRoot '.postgres\postgres.log'
$envFile = Join-Path $projectRoot '.env'
$pgCtl = Join-Path $PgBin 'pg_ctl.exe'
$initdb = Join-Path $PgBin 'initdb.exe'
$createdb = Join-Path $PgBin 'createdb.exe'
$psql = Join-Path $PgBin 'psql.exe'

foreach ($command in @($pgCtl, $initdb, $createdb, $psql)) {
    if (-not (Test-Path $command)) {
        throw "Executável não encontrado: $command. Ajuste o parâmetro -PgBin."
    }
}

if (-not (Test-Path $envFile)) {
    throw "Arquivo .env não encontrado. Crie-o a partir de .env.example antes do setup."
}

$settings = @{}
Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
        $settings[$matches[1].Trim()] = $matches[2].Trim()
    }
}

$dbPassword = $settings['DB_PASSWORD']
if (-not $dbPassword) {
    throw "Defina DB_PASSWORD no arquivo .env antes do setup."
}

New-Item -ItemType Directory -Force -Path (Split-Path $dataDir) | Out-Null
New-Item -ItemType Directory -Force -Path (Split-Path $logFile) | Out-Null

if (-not (Test-Path (Join-Path $dataDir 'PG_VERSION'))) {
    Write-Host "Criando cluster PostgreSQL em $dataDir..."
    & $initdb --auth=trust --username=deskflow --encoding=UTF8 --locale=C -D $dataDir
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao executar initdb."
    }
} else {
    Write-Host "Cluster PostgreSQL já inicializado em $dataDir."
}

& $pgCtl -D $dataDir status *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Iniciando cluster na porta $Port..."
    & $pgCtl -D $dataDir -l $logFile -o "-p $Port" -w start
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao iniciar o cluster. Consulte $logFile."
    }
} else {
    Write-Host "Cluster já está em execução."
}

$previousPassword = $env:PGPASSWORD
try {
    $env:PGPASSWORD = $dbPassword

    $databaseExists = (& $psql -h localhost -p $Port -U deskflow -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = 'deskflow'" | Out-String).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao consultar os bancos existentes."
    }

    if ($databaseExists -ne '1') {
        Write-Host "Criando banco 'deskflow'..."
        & $createdb -h localhost -p $Port -U deskflow deskflow
        if ($LASTEXITCODE -ne 0) {
            throw "Falha ao criar o banco 'deskflow'."
        }
    }

    Write-Host "Definindo senha do usuário 'deskflow'..."
    $escapedPassword = $dbPassword.Replace("'", "''")
    & $psql -h localhost -p $Port -U deskflow -d postgres -c "ALTER USER deskflow WITH PASSWORD '$escapedPassword';"
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao definir a senha do usuário 'deskflow'."
    }

    Write-Host "Restringindo autenticação local para scram-sha-256..."
    $hbaFile = Join-Path $dataDir 'pg_hba.conf'
    $hbaContent = Get-Content $hbaFile -Raw
    $hbaContent = $hbaContent -replace '\btrust\b', 'scram-sha-256'
    Set-Content -Path $hbaFile -Value $hbaContent -NoNewline

    & $pgCtl -D $dataDir reload | Out-Null

    Write-Host "Validando conexão autenticada..."
    & $psql -h localhost -p $Port -U deskflow -d deskflow -tAc "SELECT current_database(), current_user;"
    if ($LASTEXITCODE -ne 0) {
        throw "Falha na validação da conexão autenticada."
    }
} finally {
    if ($null -eq $previousPassword) {
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    } else {
        $env:PGPASSWORD = $previousPassword
    }
}

Write-Host ""
Write-Host "Cluster local pronto na porta $Port."
Write-Host "Banco: deskflow | Usuário: deskflow"
Write-Host "Use scripts/start-local-db.ps1 e scripts/stop-local-db.ps1 para controlar o cluster."
