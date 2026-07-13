# Enregistre le connecteur Debezium PostgreSQL aupres de Kafka Connect (localhost:8083)
$body = Get-Content -Raw -Path "$PSScriptRoot\connectors\postgres-source.json"
Invoke-RestMethod -Uri "http://localhost:8083/connectors" -Method Post -ContentType "application/json" -Body $body
