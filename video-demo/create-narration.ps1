$ErrorActionPreference = "Stop"
py (Join-Path $PSScriptRoot "generate_narration.py")
if ($LASTEXITCODE -ne 0) { throw "La génération de la narration a échoué." }
