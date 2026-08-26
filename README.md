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
| `siem-service` | Détection de comportements anormaux | 8086 |
| `ocr-service` | Extraction de texte (Apache Tika ; Tesseract en option) | 8087 |

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

# 4. Se connecter (les endpoints /api/** exigent un JWT, sauf /api/auth/login — voir « Sécurité »)
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" -d '{"username":"wassim","password":"wassim2026"}' | jq -r .token)

# 5. Déposer un document
curl -H "Authorization: Bearer $TOKEN" -F "file=@monfichier.pdf" -F "uploadedBy=demo" http://localhost:8081/api/documents

# 6. Vérifier le fan-out : le même événement déclenche les 4 services en parallèle
curl -H "Authorization: Bearer $TOKEN" http://localhost:8082/api/audit
curl -H "Authorization: Bearer $TOKEN" http://localhost:8084/api/notifications
curl -H "Authorization: Bearer $TOKEN" http://localhost:8085/api/integrity
curl -H "Authorization: Bearer $TOKEN" http://localhost:8086/api/alerts
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

## Sécurité (JWT)

Les endpoints `/api/**` des 6 services exigent un JWT valide (`Authorization: Bearer <token>`),
sauf `POST /api/auth/login` — seul point d'entrée pour s'authentifier, exposé par `documents-api`.

- **Identifiants de démo** (utilisateur unique en dur, pas de table `users` — hors périmètre) :
  `wassim` / `wassim2026`.
- Chaque service valide le token lui-même avec le même secret partagé : pas de gateway central
  (cf. « Périmètre exclu » dans `docs/ARCHITECTURE.md`). Les 6 services tournent en local (pas de
  conteneurs Docker pour eux), donc la valeur par défaut codée dans chaque `application.yml`
  suffit pour la démo — rien à configurer.
- Variables d'environnement (à garder identiques sur les 6 services pour `JWT_SECRET`) :
  - `JWT_SECRET` — secret HMAC partagé (valeur de démo par défaut, à changer en prod).
  - `JWT_TTL_MINUTES` — durée de validité du token (défaut `480`, soit 8h).
  - `AUTH_USERNAME` / `AUTH_PASSWORD_HASH` (uniquement sur `documents-api`) — identifiant et hash
    bcrypt du mot de passe de démo.
- Le frontend Angular gère la connexion (écran `/login`), stocke le token et l'attache
  automatiquement aux appels vers les 6 APIs (interceptor HTTP) ; un guard de route redirige vers
  `/login` si non connecté.

## Avancement

- ✅ Infrastructure et socle CDC (Docker Compose, connecteur Debezium, CI)
- ✅ `documents-api` — upload, Claim Check MinIO
- ✅ `audit-service` — premier consommateur, jalon prouvé (upload → ligne d'audit automatique)
- ✅ `notification-service`, `blockchain-service`, `siem-service` — fan-out complet + Dead Letter Topic
  (1 upload → 4 services en parallèle, message invalide isolé en DLT)
- ✅ `ocr-service` — extraction de texte via Apache Tika (PDF/DOCX) ; dispatch Tesseract câblé
  pour les images (binaire natif requis pour l'activer, voir « OCR images » ci-dessous)
- ✅ Frontend Angular — design system + 7 écrans (Tableau de bord, Documents, Piste d'audit,
  Notifications, Alertes SIEM, Intégrité, Détail document) branchés sur les vraies APIs
- ✅ Tests d'intégration Testcontainers e2e, générateur de données de démo
- ✅ Sécurité JWT sur les 6 services + frontend Angular (voir « Sécurité » ci-dessus)
- ⬜ README/diagrammes finalisés, rapport PFE, répétition de la démo

### OCR images (Tesseract)

`ocr-service` route automatiquement les images (`content_type` commençant par `image/`) vers
Tesseract, les autres formats (PDF/DOCX) restant sur Tika. Tesseract (via `tess4j`) s'appuie sur
le binaire natif du même nom : sans lui installé sur la machine qui exécute `ocr-service`, un
dépôt d'image échouera au niveau de l'extraction (le document reste visible, sans texte OCR).

Pour l'activer :
1. Installer Tesseract OCR (ex. [UB Mannheim builds](https://github.com/UB-Mannheim/tesseract/wiki)
   sous Windows, `apt install tesseract-ocr tesseract-ocr-fra` sous Linux).
2. Repérer le dossier `tessdata` (contient `fra.traineddata`, `eng.traineddata`).
3. Définir `TESSERACT_DATAPATH` (chemin vers ce dossier) avant de lancer `ocr-service` ;
   `TESSERACT_LANGUAGES` (défaut `fra+eng`) si besoin d'une autre combinaison.

## Build

```bash
mvn -B verify            # nécessite un JDK 21 (stack figée)
```
