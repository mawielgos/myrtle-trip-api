[CmdletBinding()]
param(
    [string]$Database = "myrtle_trip",
    [string]$HostName = "localhost",
    [int]$Port = 5432,
    [string]$Username = "myrtle_app",
    [string]$PgDumpPath,
    [string]$OutputPath
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$candidateDir = Join-Path $projectRoot "database\baseline-candidate"

if (-not (Test-Path $candidateDir)) {
    New-Item -ItemType Directory -Path $candidateDir -Force | Out-Null
}

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $OutputPath = Join-Path $candidateDir "${Database}_schema_${timestamp}.sql"
}

if ([string]::IsNullOrWhiteSpace($PgDumpPath)) {
    $command = Get-Command pg_dump -ErrorAction SilentlyContinue
    if ($null -eq $command) {
        throw @"
pg_dump was not found on PATH.

Either:
  1. Add your PostgreSQL bin directory to PATH, or
  2. Re-run with -PgDumpPath, for example:

     .\scripts\Export-CurrentSchemaForFlyway.ps1 `
       -PgDumpPath "C:\Program Files\PostgreSQL\17\bin\pg_dump.exe"
"@
    }
    $PgDumpPath = $command.Source
}

if (-not (Test-Path $PgDumpPath)) {
    throw "pg_dump was not found at '$PgDumpPath'."
}

$resolvedOutput = [System.IO.Path]::GetFullPath($OutputPath)

Write-Host "Exporting PostgreSQL schema only..."
Write-Host "  Database : $Database"
Write-Host "  Server   : ${HostName}:$Port"
Write-Host "  User     : $Username"
Write-Host "  Output   : $resolvedOutput"
Write-Host ""
Write-Host "pg_dump may prompt for the database password."
Write-Host "No table data will be exported."
Write-Host ""

$arguments = @(
    "--schema-only",
    "--no-owner",
    "--no-privileges",
    "--format=plain",
    "--encoding=UTF8",
    "--host=$HostName",
    "--port=$Port",
    "--username=$Username",
    "--file=$resolvedOutput",
    $Database
)

& $PgDumpPath @arguments

if ($LASTEXITCODE -ne 0) {
    throw "pg_dump failed with exit code $LASTEXITCODE."
}

if (-not (Test-Path $resolvedOutput)) {
    throw "pg_dump completed without creating '$resolvedOutput'."
}

$file = Get-Item $resolvedOutput
if ($file.Length -eq 0) {
    throw "The schema export file is empty: '$resolvedOutput'."
}

Write-Host ""
Write-Host "Schema export complete."
Write-Host "Do NOT move this file into src/main/resources/db/migration yet."
Write-Host "This is the raw candidate that will be reviewed and converted into V001__baseline.sql."
Write-Host ""
Write-Host $resolvedOutput
