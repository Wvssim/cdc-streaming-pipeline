# Diagrammes — cdc-streaming-pipeline

Diagrammes de référence du projet. La source de vérité reste [`ARCHITECTURE.md`](ARCHITECTURE.md) ;
ce fichier ne fait que la mettre en images. Les blocs ` ```mermaid ` sont rendus directement par
GitHub (aucun outil à installer).

Sommaire :

1. [Architecture / flux des composants](#1-architecture--flux-des-composants)
2. [Séquence — dépôt d'un document et fan-out](#2-séquence--dépôt-dun-document-et-fan-out)
3. [Séquence — message toxique et Dead Letter Topic](#3-séquence--message-toxique-et-dead-letter-topic)
4. [Séquence — authentification JWT](#4-séquence--authentification-jwt)
5. [Modèle de données — un schéma par service](#5-modèle-de-données--un-schéma-par-service)

---

## 1. Architecture / flux des composants

Le fichier binaire ne transite jamais par Kafka ni par PostgreSQL (**Claim Check**). L'événement
naît du WAL de PostgreSQL via Debezium (**CDC**), jamais d'un `write` applicatif vers Kafka
(pas de *dual-write*). Chaque service consomme le topic avec **son propre consumer group** (fan-out).

```mermaid
flowchart TD
    U([Utilisateur]) --> NG[Angular SPA<br/>:4200]

    NG -- "POST /api/documents<br/>(multipart + Bearer JWT)" --> API[documents-api<br/>:8081]
    NG -- "POST /api/auth/login" --> API
    NG -- "GET /api/... (Bearer JWT)" --> CONS

    API -- "métadonnées" --> PG[("PostgreSQL 17<br/>public.documents<br/>wal_level=logical")]
    API -- "fichier binaire<br/>(clé = storage_key)" --> S3[(MinIO<br/>bucket documents)]

    PG -- "WAL<br/>slot logique pgoutput" --> DBZ[Debezium 3.5<br/>Kafka Connect :8083]
    DBZ -- "publie l'événement CDC" --> K{{"Kafka 4.1 KRaft<br/>topic docs.public.documents"}}

    K -- "fan-out<br/>1 consumer group / service" --> CONS

    subgraph CONS [5 microservices consommateurs]
        direction TB
        AUD[audit-service :8082]
        NOT[notification-service :8084]
        BC[blockchain-service :8085]
        OCR[ocr-service :8087]
        SIEM[siem-service :8086]
    end

    BC -- "lit le fichier" --> S3
    OCR -- "lit le fichier" --> S3
    NOT -- "e-mail" --> MH[MailHog :8025]

    AUD --> PGA[(schéma audit)]
    NOT --> PGN[(schéma notif)]
    BC --> PGI[(schéma integrity)]
    OCR --> PGO[(schéma ocr)]
    SIEM --> PGS[(schéma siem)]

    K -. "message non traitable" .-> DLT{{"docs.public.documents-dlt"}}

    KUI[Kafbat UI :8080] -. "observe topics + offsets" .-> K
```

> Debezium ne surveille **que** `public.documents` (`table.include.list`) : les schémas des
> consommateurs ne sont jamais captés, sinon boucle infinie.

---

## 2. Séquence — dépôt d'un document et fan-out

Chemin nominal : un `POST` HTTP finit par déclencher 5 traitements, **sans aucun appel direct**
entre services.

```mermaid
sequenceDiagram
    actor U as Utilisateur
    participant NG as Angular
    participant API as documents-api
    participant S3 as MinIO
    participant PG as PostgreSQL
    participant DBZ as Debezium
    participant K as Kafka (docs.public.documents)
    participant AUD as audit-service
    participant NOT as notification-service
    participant BC as blockchain-service
    participant OCR as ocr-service
    participant SIEM as siem-service
    participant MH as MailHog

    U->>NG: dépose un fichier
    NG->>API: POST /api/documents (multipart, Bearer JWT)
    API->>S3: PUT objet (clé = storage_key)
    API->>PG: INSERT public.documents (métadonnées + storage_key)
    PG-->>API: COMMIT
    API-->>NG: 201 { id, storageKey }

    Note over PG,DBZ: le COMMIT est écrit dans le WAL
    PG-->>DBZ: WAL (slot logique pgoutput)
    DBZ->>K: événement CDC { op:"c", after:{...} }

    par fan-out — un consumer group par service
        K-->>AUD: événement
        AUD->>PG: INSERT audit.audit_log (dédup sur event_id)
    and
        K-->>NOT: événement
        NOT->>MH: envoi e-mail
        NOT->>PG: INSERT notif.notifications
    and
        K-->>BC: événement
        BC->>S3: GET objet
        BC->>PG: INSERT integrity.hash_chain (hash_n = SHA256(hash_doc ‖ hash_n-1))
    and
        K-->>OCR: événement
        OCR->>S3: GET objet
        OCR->>PG: INSERT ocr.extracted_text (Tika / Tesseract)
    and
        K-->>SIEM: événement
        SIEM->>PG: INSERT siem.deposits
        SIEM->>PG: règles → INSERT siem.alerts si anomalie
    end
```

> Consommation **at-least-once** : un même événement peut être livré deux fois. Chaque service
> est idempotent (déduplication par `doc_id` / `event_id`).

---

## 3. Séquence — message toxique et Dead Letter Topic

Un message que le consommateur n'arrive pas à traiter ne doit **jamais** bloquer le pipeline
(invariant 7).

```mermaid
sequenceDiagram
    participant K as Kafka (docs.public.documents)
    participant C as consumer (ex. audit-service)
    participant EH as DefaultErrorHandler
    participant DLT as Kafka (docs.public.documents-dlt)

    K-->>C: message n (illisible / traitement en erreur)
    C->>EH: exception
    loop retries + back-off
        EH->>C: nouvelle tentative
        C-->>EH: échoue encore
    end
    EH->>DLT: DeadLetterPublishingRecoverer republie le message brut
    Note over C,K: l'offset avance
    K-->>C: message n+1 (traité normalement)
```

---

## 4. Séquence — authentification JWT

`documents-api` est le **seul** émetteur de token. Les 6 services valident le JWT eux-mêmes avec
le même secret partagé (`JWT_SECRET`) : pas de gateway centrale (invariant 10, cohérent avec
l'exclusion de Spring Cloud Gateway).

```mermaid
sequenceDiagram
    actor U as Utilisateur
    participant NG as Angular
    participant API as documents-api
    participant SVC as un service consommateur

    U->>NG: identifiants (wassim / wassim2026)
    NG->>API: POST /api/auth/login
    API->>API: vérifie le mot de passe (bcrypt)
    API-->>NG: 200 { token } (HS256, TTL 480 min)
    NG->>NG: stocke le token

    Note over NG: l'interceptor HTTP ajoute l'en-tête<br/>Authorization Bearer sur les appels /api/**

    NG->>SVC: GET /api/audit (Bearer)
    SVC->>SVC: JwtAuthFilter valide la signature<br/>avec le secret partagé
    alt token valide
        SVC-->>NG: 200 données
    else absent / invalide / expiré
        SVC-->>NG: 401
        NG->>NG: guard de route → redirige vers /login
    end
```

---

## 5. Modèle de données — un schéma par service

Une seule instance PostgreSQL, un schéma par service (isolation logique, pas de
*database-per-service*). Les liens `doc_id` sont **logiques** : ils transitent par Kafka, il n'y a
aucune clé étrangère inter-schémas.

```mermaid
erDiagram
    documents {
        bigserial id PK
        varchar   filename
        varchar   content_type
        bigint    size
        varchar   storage_key "clé de l'objet MinIO"
        varchar   uploaded_by
        timestamptz uploaded_at
    }
    audit_log {
        varchar   event_id PK "clé d'idempotence"
        bigint    doc_id
        varchar   action
        varchar   actor
        timestamptz occurred_at
    }
    notifications {
        bigserial id PK
        bigint    doc_id
        varchar   recipient
        varchar   status
        timestamptz sent_at
    }
    hash_chain {
        bigserial seq PK
        bigint    doc_id
        varchar   doc_hash   "SHA-256 du document"
        varchar   prev_hash  "chain_hash du maillon précédent"
        varchar   chain_hash "SHA256(doc_hash ‖ prev_hash)"
        timestamptz created_at
    }
    extracted_text {
        bigint    doc_id PK
        text      text
        varchar   engine "tika | tesseract"
        timestamptz extracted_at
    }
    alerts {
        bigserial id PK
        bigint    doc_id
        varchar   rule
        varchar   severity
        text      detail
        timestamptz raised_at
    }
    deposits {
        bigint    doc_id PK
        varchar   actor
        varchar   filename
        timestamptz uploaded_at
    }

    documents ||..o{ audit_log      : "doc_id (via Kafka)"
    documents ||..o{ notifications   : "doc_id (via Kafka)"
    documents ||..o{ hash_chain      : "doc_id (via Kafka)"
    documents ||..o| extracted_text  : "doc_id (via Kafka)"
    documents ||..o{ alerts          : "doc_id (via Kafka)"
    documents ||..o| deposits        : "doc_id (via Kafka)"
```

| Schéma | Propriétaire | Table(s) |
|---|---|---|
| `public` | `documents-api` | `documents` — **seule table captée par Debezium** |
| `audit` | `audit-service` | `audit_log` |
| `notif` | `notification-service` | `notifications` |
| `integrity` | `blockchain-service` | `hash_chain` (append-only) |
| `ocr` | `ocr-service` | `extracted_text` |
| `siem` | `siem-service` | `alerts`, `deposits` (historique interne alimenté par Kafka) |
