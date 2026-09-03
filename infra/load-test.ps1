param(
    [ValidateRange(1, 1000)] [int]$Deposits = 30,
    [ValidateRange(10, 600)] [int]$DrainTimeoutSeconds = 120,
    [string]$ApiUrl = "http://localhost:8081",
    [string]$Username = "wassim",
    [string]$Password = "wassim2026"
)

$ErrorActionPreference = "Stop"
$targetPerMinute = 30
$tempDir = Join-Path ([System.IO.Path]::GetTempPath()) ("cdc-load-" + [guid]::NewGuid())
New-Item -ItemType Directory -Path $tempDir | Out-Null

try {
    $loginBody = @{ username = $Username; password = $Password } | ConvertTo-Json
    $login = Invoke-RestMethod -Method Post -Uri "$ApiUrl/api/auth/login" -ContentType "application/json" -Body $loginBody
    $headers = @{ Authorization = "Bearer $($login.token)" }

    $sample = Join-Path $tempDir "charge.txt"
    Set-Content -LiteralPath $sample -Value "Test de charge CDC - $(Get-Date -Format o)" -Encoding utf8

    $watch = [System.Diagnostics.Stopwatch]::StartNew()
    for ($i = 1; $i -le $Deposits; $i++) {
        $response = & curl.exe -sS -f -X POST "$ApiUrl/api/documents" `
            -H "Authorization: Bearer $($login.token)" `
            -F "file=@$sample;filename=charge-$i.txt" `
            -F "uploadedBy=load-test"
        if ($LASTEXITCODE -ne 0) { throw "Echec du depot $i" }
        $null = $response | ConvertFrom-Json
    }
    $watch.Stop()
    $rate = [math]::Round($Deposits * 60 / $watch.Elapsed.TotalSeconds, 1)

    $groups = @("audit-service", "notification-service", "blockchain-service", "siem-service", "ocr-service")
    $deadline = (Get-Date).AddSeconds($DrainTimeoutSeconds)
    $lag = [int]::MaxValue
    do {
        $lag = 0
        foreach ($group in $groups) {
            $rows = docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka:9092 --describe --group $group 2>$null
            foreach ($line in $rows) {
                if ($line -match '^\s*\S+\s+\d+\s+\d+\s+\d+\s+(\d+)\s+') { $lag += [int]$Matches[1] }
            }
        }
        if ($lag -gt 0) { Start-Sleep -Seconds 1 }
    } while ($lag -gt 0 -and (Get-Date) -lt $deadline)

    $result = [pscustomobject]@{
        Deposits = $Deposits
        UploadSeconds = [math]::Round($watch.Elapsed.TotalSeconds, 2)
        DepositsPerMinute = $rate
        TargetPerMinute = $targetPerMinute
        FinalAggregateLag = $lag
        Passed = ($rate -ge $targetPerMinute -and $lag -eq 0)
    }
    $result | Format-List
    if (-not $result.Passed) { throw "RNF-07 non respecte." }
} finally {
    Remove-Item -LiteralPath $tempDir -Recurse -Force -ErrorAction SilentlyContinue
}
