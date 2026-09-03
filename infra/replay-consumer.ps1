param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("audit-service", "notification-service", "blockchain-service", "siem-service", "ocr-service")]
    [string]$Group,
    [string]$Topic = "docs.public.documents"
)

$ErrorActionPreference = "Stop"

if ($Topic -notmatch '^[a-zA-Z0-9._-]+$') {
    throw "Nom de topic invalide : $Topic"
}

$running = docker ps --filter "name=^/kafka$" --format "{{.Names}}"
if ($running -ne "kafka") {
    throw "Le conteneur Kafka n'est pas actif. Lancez d'abord : cd infra; docker compose up -d"
}

Write-Host "Rejeu complet du groupe '$Group' sur '$Topic'."
Write-Host "Le service correspondant doit etre arrete pendant la remise a zero des offsets."

$output = docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh `
    --bootstrap-server kafka:9092 `
    --group $Group `
    --topic $Topic `
    --reset-offsets `
    --to-earliest `
    --execute 2>&1

if ($LASTEXITCODE -ne 0) {
    throw ($output -join [Environment]::NewLine)
}

$text = $output -join [Environment]::NewLine
Write-Host $text
if ($text -match "active members|cannot reset|Assignments can only be reset") {
    throw "Remise a zero refusee : arretez le service '$Group', puis relancez cette commande."
}

Write-Host "Offsets remis au debut. Redemarrez '$Group' : tous les evenements seront relus."
