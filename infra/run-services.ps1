# Lance (ou arrete) les 6 microservices en local, hors Docker.
# Prerequis : infra up (cd infra ; docker compose up -d), connecteur Debezium enregistre,
#             JDK 21, et `mvn -B verify` passe (les .jar existent dans */target/).
#
# Usage, depuis la racine du repo :
#   .\infra\run-services.ps1                  # demarre les 6 services en arriere-plan
#   .\infra\run-services.ps1 -Stop            # arrete les 6 services
#   .\infra\run-services.ps1 -PgPort 5433     # si l'infra Postgres est publiee sur 5433

param([switch]$Stop, [int]$PgPort = 0)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent

# La stack est figee sur Java 21. Si un JDK 21 est sous JAVA_HOME, l'utiliser explicitement :
# le `java` du PATH peut etre une autre version (UnsupportedClassVersionError sinon).
$java = "java"
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    $java = Join-Path $env:JAVA_HOME "bin\java.exe"
}

$services = @(
    "documents-api",
    "audit-service",
    "notification-service",
    "blockchain-service",
    "siem-service",
    "ocr-service"
)

if ($Stop) {
    Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
        Where-Object { $_.CommandLine -match 'cdc-streaming-pipeline.+target.+-SNAPSHOT\.jar' } |
        ForEach-Object {
            Write-Host "arret pid $($_.ProcessId)"
            Stop-Process -Id $_.ProcessId -Force
        }
    return
}

if ($PgPort -gt 0) {
    $env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:$PgPort/docdb"
    Write-Host "Postgres hote : $($env:SPRING_DATASOURCE_URL)"
}

foreach ($s in $services) {
    $jar = Join-Path $root "$s\target\$s-0.0.1-SNAPSHOT.jar"
    if (-not (Test-Path $jar)) {
        Write-Warning "$jar absent - lancer 'mvn -B verify' d'abord. Service ignore."
        continue
    }
    Start-Process -FilePath $java -ArgumentList "-jar", "`"$jar`"" -WindowStyle Hidden
    Write-Host "demarre : $s"
}

Write-Host ""
Write-Host "6 services lances en arriere-plan. Verifier : curl http://localhost:8081/api/documents (401 attendu sans token = up)."
Write-Host "Arret : .\infra\run-services.ps1 -Stop"
