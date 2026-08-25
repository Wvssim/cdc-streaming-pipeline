#!/usr/bin/env bash
# T6.2 : generateur de donnees pour la demo live.
# Depose des documents varies qui declenchent, de facon reproductible, les 3 regles siem :
#   1. frequence anormale  -> 6 depots par le meme acteur en quelques secondes (seuil = 5 / 10 min)
#   2. horaire inhabituel  -> uploaded_at force a 3h du matin (hors plage 7h-21h)
#   3. extension suspecte  -> depot d'un .exe
#
# Prerequis : infra up (`cd infra && docker compose up -d`), connecteur Debezium enregistre,
# documents-api demarre en local (port 8081 par defaut).
#
# Usage : ./infra/demo-seed.sh   (ou DOCUMENTS_API_URL=http://localhost:8081 ./infra/demo-seed.sh)

set -euo pipefail

API_URL="${DOCUMENTS_API_URL:-http://localhost:8081}"

TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

echo "rapport de demo genere le $(date)" > "$TMP_DIR/rapport.pdf"
echo "contenu factice d'executable" > "$TMP_DIR/script-suspect.exe"

extract_field() {
  # extract_field <json> <champ> -- lecture simple, pas de dependance a jq
  echo "$1" | grep -o "\"$2\":\"[^\"]*\"" | head -1 | cut -d'"' -f4
}

upload() {
  local file="$1" uploaded_by="$2"
  curl -sf -X POST "$API_URL/api/documents" \
    -F "file=@${file}" \
    -F "uploadedBy=${uploaded_by}"
}

if ! curl -sf -o /dev/null "$API_URL/api/documents"; then
  echo "documents-api injoignable sur $API_URL (est-il demarre ?)" >&2
  exit 1
fi

echo "== regle 1 : frequence anormale (6 depots par 'demo-frequence') =="
storage_key=""
content_type=""
for i in $(seq 1 6); do
  response=$(upload "$TMP_DIR/rapport.pdf" "demo-frequence")
  storage_key=$(extract_field "$response" "storageKey")
  content_type=$(extract_field "$response" "contentType")
  echo "  depot $i/6 -> storageKey=$storage_key"
done

echo "== regle 3 : extension suspecte (acteur 'demo-extension', fichier .exe) =="
upload "$TMP_DIR/script-suspect.exe" "demo-extension" >/dev/null
echo "  fichier .exe depose"

echo "== regle 2 : horaire inhabituel (reutilise un objet MinIO reel, uploaded_at force a 03h) =="
if [ -z "$storage_key" ]; then
  echo "abandon : aucun storage_key recupere sur les depots precedents" >&2
  exit 1
fi
docker exec -i cdc-postgres psql -U cdc -d docdb -v ON_ERROR_STOP=1 <<SQL
INSERT INTO public.documents (filename, content_type, size, storage_key, uploaded_by, uploaded_at)
VALUES ('rapport-nocturne.pdf', '${content_type:-application/pdf}', 1024, '${storage_key}', 'demo-nocturne',
        (CURRENT_DATE::text || ' 03:00:00+00')::timestamptz);
SQL
echo "  ligne inseree, uploaded_at = 03:00 (hors plage 7h-21h)"

echo
echo "generateur termine. Ouvrir Kafbat UI (localhost:8080) puis l'ecran Alertes SIEM du front pour verifier les 3 regles declenchees."
