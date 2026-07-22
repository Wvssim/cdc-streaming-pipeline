# Architecture — cdc-streaming-pipeline

## Le projet en une phrase

Plateforme de dépôt et de traitement de documents, bâtie sur un **pipeline CDC event-driven** : PostgreSQL → Debezium → Kafka → 5 microservices consommateurs indépendants.

**PFE** — Wassim Lazim, EMSI Casablanca. Encadrant : Pr. Bekkali Mohamed (spécialité JEE).
**Durée** : 6 semaines. **État actuel** : S1 terminée (cahier des charges + maquette Figma). S2 en cours (infrastructure).

## Le flux nominal — défini par l'encadrant, NON NÉGOCIABLE

1. L'utilisateur dépose un document via l'interface **Angular**
2. Angular envoie le document à `documents-api` (**Spring Boot**)
3. L'API écrit les **métadonnées dans PostgreSQL** et le **fichier binaire dans MinIO**
4. **Debezium** détecte le nouvel enregistrement en lisant le WAL de PostgreSQL (CDC log-based)
5. Debezium publie un **événement dans Kafka** (topic `docs.public.documents`)
6. **Cinq services consomment cet événement en parallèle**, chacun avec son propre consumer group

## Architecture

```
Angular SPA
    │ upload
    ▼
documents-api ──── métadonnées ──▶ PostgreSQL (public.documents)
    │                                      │ WAL
    └──── fichier ──▶ MinIO                ▼
                        ▲            Debezium (Kafka Connect)
                        │                  │
                        │                  ▼
                        │           Kafka — topic documents
                        │                  │ fan-out
                        │      ┌───────────┼───────────┬───────────┬──────────┐
                        │      ▼           ▼           ▼           ▼          ▼
                        │  audit-    notification- blockchain-  ocr-      siem-
                        │  service     service      service    service   service
                        └────────────────────────────────────────┘ (lit le fichier)
                                          │
                                          ▼
                                       MailHog
```

**6 microservices Spring Boot** :

| Service | Rôle | Endpoint de lecture |
|---|---|---|
| `documents-api` | Producteur. Upload multipart → métadonnées Postgres + fichier MinIO | `GET /api/documents` |
| `audit-service` | Trace toutes les opérations (qui, quoi, quand) | `GET /api/audit` |
| `notification-service` | Envoie un e-mail à l'utilisateur (MailHog en dev) | `GET /api/notifications` |
| `blockchain-service` | Hash SHA-256 + chaînage `hash_n = SHA256(hash_doc ‖ hash_n-1)` | `GET /api/integrity` |
| `ocr-service` | Récupère le fichier depuis MinIO, extrait le texte (Tika / Tesseract) | `GET /api/ocr/{docId}` |
| `siem-service` | Moteur de règles → détection d'anomalies | `GET /api/alerts` |

Angular = frontend (pas un microservice). Debezium = connecteur **configuré**, pas codé. Kafka, PostgreSQL, MinIO, MailHog, Kafbat UI = infrastructure.

## Invariants — les règles qu'on ne casse jamais

Ces invariants sont aussi les points techniques à savoir défendre en soutenance : chacun porte une justification, pas seulement une règle.

1. **Claim Check pattern** — le fichier binaire ne transite JAMAIS par Kafka ni par PostgreSQL. Postgres ne stocke que les métadonnées + la clé de stockage MinIO. Un message Kafka fait 1 Mo par défaut ; un PDF de 18 Mo casserait le broker. Le service qui a besoin du fichier (`ocr-service`) va le chercher avec la clé.
2. **Aucun appel HTTP direct entre microservices.** Les consommateurs ne connaissent QUE le topic Kafka. Ils ignorent l'existence de `documents-api` et l'existence les uns des autres. Si tu es tenté d'écrire un `RestTemplate` d'un service vers un autre, tu casses le sujet du PFE.
3. **Un consumer group distinct par service (fan-out).** C'est ce qui fait que les 5 services reçoivent TOUS le même événement, au lieu de se le partager. Conséquence directe : **extensibilité** — ajouter un 7ᵉ service, c'est ajouter un consumer group de plus sur le topic, sans toucher à l'existant (ni à l'API, ni aux autres services).
4. **Les consommateurs sont idempotents (at-least-once).** Un même événement peut arriver deux fois (redémarrage, rééquilibrage). Traiter deux fois ne doit pas produire deux lignes d'audit ni deux e-mails — déduplication par identifiant de document / d'événement.
5. **`REPLICA IDENTITY FULL` sur `public.documents`** — sans ça, les événements UPDATE/DELETE n'embarquent pas l'état « avant ».
6. **Debezium ne surveille QUE `public.documents`.** Les schémas des consommateurs ne sont jamais captés — sinon boucle infinie.
7. **Toute erreur de traitement part en Dead Letter Topic** (`DefaultErrorHandler` + `DeadLetterPublishingRecoverer`). Un message toxique ne doit jamais bloquer le pipeline.
8. **La chaîne de hash est append-only.** `blockchain-service` maintient un **registre d'intégrité** (`hash_n = SHA256(hash_doc ‖ hash_{n-1})`) : un maillon existant ne se réécrit jamais. C'est un registre d'intégrité, **PAS une blockchain** publique.
9. **L'événement naît du CDC, jamais d'un dual-write.** C'est le WAL de PostgreSQL (source unique de vérité) qui déclenche l'événement Kafka, jamais un code applicatif qui écrirait « en base puis dans Kafka ». L'événement n'existe donc que si la transaction a commité : le problème classique du **dual-write** est éliminé par construction.

> Propriétés d'infrastructure qui en découlent, également défendables en soutenance : **snapshot initial puis streaming incrémental** (Debezium rejoue l'existant avant de suivre le WAL en continu) et **Kafka en mode KRaft, sans Zookeeper** (une brique d'infra en moins à opérer).

## Stack technique et justification des choix

| Brique | Version | Pourquoi ce choix |
|---|---|---|
| Java (Eclipse Temurin) | **21 LTS** | LTS, zéro friction avec l'écosystème |
| Maven multi-module | 3.9 | `pom.xml` = réflexe JEE, lisible par l'encadrant |
| Spring Boot | **4.x** | Baseline Jakarta EE 11 → du JEE moderne. Écosystème `spring-kafka` mature |
| Angular | dernière stable | Imposé par le brief de l'encadrant |
| Debezium (connecteur PostgreSQL, plugin `pgoutput`) | **3.5** | CDC log-based : lit le WAL, zéro impact sur les transactions source |
| Apache Kafka (mode **KRaft**, sans Zookeeper) | **4.1** | Log distribué → fan-out natif. KRaft = une brique de moins |
| PostgreSQL (`wal_level=logical`) | **17** | Base source, seule source de vérité |
| MinIO | latest | Stockage objet S3-compatible pour les fichiers (Claim Check) |
| MailHog | latest | SMTP de dev, interface web, zéro config |
| Kafbat UI | latest | Visualiser les topics et les messages en live (indispensable pour la démo) |
| Apache Tika + tess4j | — | Extraction de texte (PDF/DOCX puis images) |
| Testcontainers | — | Tests d'intégration avec Postgres + Kafka éphémères |
| Docker Compose | — | Toute l'infra en local |
| GitHub Actions | — | CI : `mvn verify` à chaque push |

## Structure du monorepo

```
cdc-streaming-pipeline/
├── infra/
│   ├── docker-compose.yml          # Postgres, Kafka (KRaft), Connect+Debezium, MinIO, MailHog, Kafbat UI
│   ├── init-scripts/               # rejoués au 1er boot par docker-entrypoint-initdb.d
│   │   ├── 01-documents.sql        # schéma public + table documents (REPLICA IDENTITY FULL)
│   │   └── 02-consumer-schemas.sql # schémas audit/notif/integrity/ocr/siem + leurs tables
│   ├── connectors/
│   │   └── postgres-source.json    # config du connecteur Debezium source
│   └── register-connector.ps1      # enregistre le connecteur via l'API REST de Kafka Connect
├── pom.xml                         # parent POM (dependencyManagement, modules)
├── common/                         # DTO de l'enveloppe Debezium (before/after/op/ts_ms), désérialisation
├── documents-api/
├── audit-service/
├── notification-service/
├── blockchain-service/
├── ocr-service/
├── siem-service/
├── frontend/                       # Angular
├── docs/
│   ├── ARCHITECTURE.md             # source de vérité (ce document)
│   ├── cahier-des-charges.tex      # cahier des charges (LaTeX)
│   ├── plan-6-semaines.md          # plan de réalisation détaillé
│   └── maquette/                   # export de la maquette Figma (référence contractuelle du front)
└── CLAUDE.md                       # pointeur vers docs/ARCHITECTURE.md (git-ignoré)
```

## Modèle de données — une instance PostgreSQL, un schéma par service

**Choix assumé** : pas de *database-per-service* (6 containers pour un gain nul à cette échelle). Un schéma par service donne l'isolation logique — aucun service ne lit le schéma d'un autre — et la migration vers database-per-service resterait triviale.

| Schéma | Propriétaire | Contenu |
|---|---|---|
| `public` | `documents-api` | `documents` (id, filename, content_type, size, storage_key, uploaded_by, uploaded_at) — **capté par Debezium** |
| `audit` | `audit-service` | `audit_log` (event_id, doc_id, action, actor, occurred_at) |
| `notif` | `notification-service` | `notifications` (doc_id, recipient, status, sent_at) |
| `integrity` | `blockchain-service` | `hash_chain` (seq, doc_id, doc_hash, prev_hash, chain_hash, created_at) |
| `ocr` | `ocr-service` | `extracted_text` (doc_id, text, engine, extracted_at) |
| `siem` | `siem-service` | `alerts` (doc_id, rule, severity, detail, raised_at) ; `deposits` (doc_id, actor, filename, uploaded_at) — historique interne alimenté par Kafka, pour la règle de fréquence |

Le fichier binaire vit dans MinIO, bucket `documents`, clé = `storage_key`.

## Conventions

- **Branches** : `feat/`, `fix/`, `chore/`, `docs/` + description courte (ex. `feat/s2-infra-debezium`)
- **Commits** : Conventional Commits, **en français** (`feat(audit): consomme les événements documents dans audit_log`)
- **Une milestone GitHub par semaine** (S2 → S6), une issue par tâche du plan
- **Chaque semaine se termine par un jalon vérifiable** — voir `docs/plan-6-semaines.md`

## Périmètre exclu (perspectives)

Ces sujets vont dans la section « perspectives » du rapport, jamais dans le code :

Schema Registry / Avro · Kafka Streams · Flink · Kubernetes · cluster Kafka multi-broker · sécurité SASL/SSL sur Kafka · blockchain publique (Ethereum, Hyperledger) · multi-tenant · Spring Cloud Gateway · Outbox pattern.

Le scope maîtrisé est une décision, pas une limite. Un PFE qui tourne à 100 % bat un PFE ambitieux à moitié fini.

## Commandes

```bash
# Infra — repartir sur des volumes propres puis démarrer
cd infra
docker compose down -v                  # sinon init-scripts/ ne se rejoue pas, le vieux slot persiste
docker compose up -d
docker compose ps                       # 6 containers "running" (+ createbuckets en Exited 0, normal)

# Enregistrer le connecteur Debezium (attendre ~40s que Connect démarre)
#   Windows : .\register-connector.ps1
#   sinon   : curl -X POST -H "Content-Type: application/json" \
#               --data @connectors/postgres-source.json http://localhost:8083/connectors
curl http://localhost:8083/connectors/source-postgres-connector/status   # doit être RUNNING

# Prouver que le CDC tourne (jalon S2)
docker exec -it cdc-postgres psql -U cdc -d docdb \
  -c "INSERT INTO documents (filename, content_type, size, storage_key, uploaded_by) VALUES ('test.pdf','application/pdf',1024,'test-key','wassim');"
# → l'événement doit apparaître dans Kafbat UI, topic docs.public.documents

# Build
mvn verify
```

**Interfaces web** : Kafbat UI `:8080` · Kafka Connect `:8083` · MinIO Console `:9001` · MailHog `:8025`
