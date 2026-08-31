#!/usr/bin/env bash
# Lance les 6 microservices en local, hors Docker (equivalent bash de run-services.ps1).
# Prerequis : infra up, connecteur Debezium enregistre, JDK 21, `mvn -B verify` deja passe.
#
# Usage, depuis la racine du repo :
#   ./infra/run-services.sh          # demarre les 6 services en arriere-plan
#   ./infra/run-services.sh stop     # tue les 6 services
#
# Si l'infra Postgres est publiee sur un autre port hote (docker-compose.5433.yml) :
#   CDC_PG_PORT=5433 ./infra/run-services.sh
#
# Logs : .run-logs/<service>.log (dossier git-ignore).

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SERVICES=(documents-api audit-service notification-service blockchain-service siem-service ocr-service)

# Stack figee sur Java 21 : utiliser JAVA_HOME si dispo (le `java` du PATH peut etre une autre version).
JAVA=java
[ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ] && JAVA="$JAVA_HOME/bin/java"

if [ "${1:-}" = "stop" ]; then
  pkill -f 'cdc-streaming-pipeline.*target.*-SNAPSHOT\.jar' && echo "services arretes" || echo "aucun service en cours"
  exit 0
fi

if [ -n "${CDC_PG_PORT:-}" ]; then
  export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:${CDC_PG_PORT}/docdb"
  echo "Postgres hote : $SPRING_DATASOURCE_URL"
fi

mkdir -p "$ROOT/.run-logs"
for s in "${SERVICES[@]}"; do
  jar="$ROOT/$s/target/$s-0.0.1-SNAPSHOT.jar"
  if [ ! -f "$jar" ]; then
    echo "$jar absent - lancer 'mvn -B verify' d'abord." >&2
    exit 1
  fi
  "$JAVA" -jar "$jar" > "$ROOT/.run-logs/$s.log" 2>&1 &
  echo "demarre : $s (pid $!)"
done

echo
echo "6 services lances. Logs : .run-logs/  |  Arret : ./infra/run-services.sh stop"
