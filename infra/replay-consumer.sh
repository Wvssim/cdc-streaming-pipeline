#!/usr/bin/env bash
set -euo pipefail

GROUP="${1:-}"
TOPIC="${2:-docs.public.documents}"
case "$GROUP" in
  audit-service|notification-service|blockchain-service|siem-service|ocr-service) ;;
  *) echo "usage: $0 {audit-service|notification-service|blockchain-service|siem-service|ocr-service} [topic]" >&2; exit 2 ;;
esac
[[ "$TOPIC" =~ ^[a-zA-Z0-9._-]+$ ]] || { echo "nom de topic invalide" >&2; exit 2; }

docker exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server kafka:9092 --group "$GROUP" --topic "$TOPIC" \
  --reset-offsets --to-earliest --execute

echo "Offsets remis au debut. Redemarrez $GROUP pour relire tous les evenements."
