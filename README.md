# cdc-streaming-pipeline

[![CI](https://github.com/Wvssim/cdc-streaming-pipeline/actions/workflows/ci.yml/badge.svg)](https://github.com/Wvssim/cdc-streaming-pipeline/actions/workflows/ci.yml)

Plateforme de dépôt et de traitement de documents, bâtie sur un **pipeline CDC event-driven** :
PostgreSQL → Debezium → Kafka → 5 microservices consommateurs indépendants (fan-out).

Projet réalisé dans le cadre d'un stage de fin d'année (filière DSI, 4ᵉ année) à EMSI Casablanca,
au sein de la société 6Solutions.

## Le principe

Quand un utilisateur dépose un document, ce simple dépôt déclenche automatiquement plusieurs
traitements en parallèle (audit, notification, intégrité, extraction de texte, sécurité), sans
qu'aucun service n'appelle un autre directement. Le mécanisme repose sur le **Change Data
Capture (CDC)** : au lieu que l'application prévienne chaque service, on lit le journal de
transactions de PostgreSQL — dès qu'une ligne est insérée, un événement est publié automatiquement.

```
Angular SPA ──upload──▶ documents-api ──métadonnées──▶ PostgreSQL ──WAL──▶ Debezium ──▶ Kafka
                             │                                                            │
                             └──fichier──▶ MinIO                          fan-out vers 5 consommateurs
                                            ▲                          (audit, notification, blockchain,
                                            └──lit le fichier── ocr-service         ocr, siem)
```

- **Claim Check** — le fichier binaire ne transite jamais par Kafka ni par la base ; il va dans
  MinIO, seule sa référence circule dans le pipeline.
- **Fan-out par consumer groups** — chaque service reçoit sa propre copie de chaque événement,
  indépendamment des autres.
- **Découplage total** — les consommateurs ne connaissent que le topic Kafka, jamais l'API ni
  les autres services.

Documentation complète : **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** (source de vérité
technique) et **[docs/cahier-des-charges.tex](docs/cahier-des-charges.tex)** (cahier des charges).
Plan de travail : **[docs/plan-6-semaines.md](docs/plan-6-semaines.md)**.

## Architecture

**6 microservices Spring Boot**, monorepo Maven multi-module :

| Service | Rôle | Port |
|---|---|---|
| `documents-api` | Upload multipart → métadonnées PostgreSQL + fichier MinIO | 8081 |
| `audit-service` | Trace de chaque opération (qui, quoi, quand) | 8082 |
| `notification-service` | E-mail de confirmation à l'utilisateur | 8084 |
| `blockchain-service` | Registre d'intégrité par chaîne de hash SHA-256 | 8085 |
| `ocr-service` | Extraction de texte (Apache Tika / Tesseract) | — |
| `siem-service` | Détection de comportements anormaux | 8086 |

Infrastructure : PostgreSQL 17 (CDC via `wal_level=logical`), Kafka 4.1 en mode KRaft,
Debezium 3.5 (connecteur, pas de code), MinIO, MailHog, Kafbat UI.

## Démarrage rapide

```bash
# 1. Infrastructure
cd infra
docker compose up -d     # Postgres, Kafka (KRaft), Connect+Debezium, MinIO, MailHog, Kafbat UI
docker compose ps        # 6 containers "running" (+ createbuckets en Exited 0, one-shot)

# 2. Build (nécessite un JDK 21 — stack figée)
cd ..
mvn -B verify

# 3. Lancer les services
java -jar documents-api/target/documents-api-0.0.1-SNAPSHOT.jar &
java -jar audit-service/target/audit-service-0.0.1-SNAPSHOT.jar &
java -jar notification-service/target/notification-service-0.0.1-SNAPSHOT.jar &
java -jar blockchain-service/target/blockchain-service-0.0.1-SNAPSHOT.jar &
java -jar siem-service/target/siem-service-0.0.1-SNAPSHOT.jar &

# 4. Déposer un document
curl -F "file=@monfichier.pdf" -F "uploadedBy=demo" http://localhost:8081/api/documents

# 5. Vérifier le fan-out : le même événement déclenche les 4 services en parallèle
curl http://localhost:8082/api/audit
curl http://localhost:8084/api/notifications
curl http://localhost:8085/api/integrity
curl http://localhost:8086/api/alerts
```

Enregistrement du connecteur Debezium : voir la section « Commandes » de
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Interfaces (dev)

| Service | URL |
|---|---|
| Kafbat UI (topics Kafka) | http://localhost:8080 |
| Kafka Connect (REST) | http://localhost:8083 |
| MinIO Console | http://localhost:9001 |
| MailHog | http://localhost:8025 |

## Avancement

- ✅ Infrastructure et socle CDC (Docker Compose, connecteur Debezium, CI)
- ✅ `documents-api` — upload, Claim Check MinIO
- ✅ `audit-service` — premier consommateur, jalon prouvé (upload → ligne d'audit automatique)
- ✅ `notification-service`, `blockchain-service`, `siem-service` — fan-out complet + Dead Letter Topic
  (1 upload → 4 services en parallèle, message invalide isolé en DLT)
- ⬜ `ocr-service` et frontend Angular
- ⬜ Tests d'intégration, sécurité, consolidation

## Build

```bash
mvn -B verify            # nécessite un JDK 21 (stack figée)
```
