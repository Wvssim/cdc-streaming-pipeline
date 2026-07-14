# cdc-streaming-pipeline

[![CI](https://github.com/Wvssim/cdc-streaming-pipeline/actions/workflows/ci.yml/badge.svg)](https://github.com/Wvssim/cdc-streaming-pipeline/actions/workflows/ci.yml)

Plateforme documentaire événementielle — pipeline **CDC** : PostgreSQL → Debezium → Kafka → 5 microservices consommateurs indépendants (fan-out).

- Documentation de référence : **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**
- Plan de travail (6 semaines) : **[docs/plan-6-semaines.md](docs/plan-6-semaines.md)**

## Démarrage rapide (infra)

```bash
cd infra
docker compose up -d     # Postgres, Kafka (KRaft), Connect+Debezium, MinIO, MailHog, Kafbat UI
docker compose ps        # 6 containers "running" (+ createbuckets en Exited 0, one-shot)
```

Enregistrement du connecteur Debezium et test du pipeline CDC (jalon S2) : voir la section
« Commandes » de [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Interfaces (dev)

| Service | URL |
|---|---|
| Kafbat UI | http://localhost:8080 |
| Kafka Connect (REST) | http://localhost:8083 |
| MinIO Console | http://localhost:9001 |
| MailHog | http://localhost:8025 |

## Build

```bash
mvn -B verify            # nécessite un JDK 21 (stack figée)
```
