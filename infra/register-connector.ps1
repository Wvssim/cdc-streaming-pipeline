# Enregistre le connecteur Debezium source aupres de Kafka Connect (http://localhost:8083)
# Usage : depuis infra/, lancer  .\register-connector.ps1
# Plus fiable qu'un curl brut sous Windows : gere le cas "deja enregistre" (409) et affiche le statut.

$ErrorActionPreference = "Stop"

$configPath = Join-Path $PSScriptRoot "connectors\postgres-source.json"
$config     = Get-Content -Raw -Path $configPath
$name       = ($config | ConvertFrom-Json).name

Write-Host "Enregistrement du connecteur '$name' aupres de Kafka Connect..."
try {
    Invoke-RestMethod -Uri "http://localhost:8083/connectors" -Method Post `
        -ContentType "application/json" -Body $config | Out-Null
    Write-Host "Connecteur cree."
}
catch {
    if ($_.Exception.Response -and $_.Exception.Response.StatusCode.value__ -eq 409) {
        Write-Host "Le connecteur existe deja (409) : mise a jour de sa config..."
        $cfg = ($config | ConvertFrom-Json).config | ConvertTo-Json -Depth 10
        Invoke-RestMethod -Uri "http://localhost:8083/connectors/$name/config" -Method Put `
            -ContentType "application/json" -Body $cfg | Out-Null
    }
    else { throw }
}

Start-Sleep -Seconds 3
Write-Host "`nStatut du connecteur (attendu : RUNNING pour le connecteur ET sa task) :"
Invoke-RestMethod -Uri "http://localhost:8083/connectors/$name/status" -Method Get |
    ConvertTo-Json -Depth 6
