-- ============================================================
--  02-consumer-schemas.sql
--  Un schéma par service consommateur (isolation logique, pas de database-per-service).
--  IMPORTANT : ces schémas ne sont JAMAIS captés par Debezium
--  (table.include.list = public.documents uniquement) — sinon boucle infinie.
-- ============================================================

CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS notif;
CREATE SCHEMA IF NOT EXISTS integrity;
CREATE SCHEMA IF NOT EXISTS ocr;
CREATE SCHEMA IF NOT EXISTS siem;

-- audit-service : trace de toutes les opérations
CREATE TABLE IF NOT EXISTS audit.audit_log (
    event_id     VARCHAR(255) PRIMARY KEY,   -- clé d'idempotence (dédup des rejeux)
    doc_id       BIGINT,
    action       VARCHAR(32),
    actor        VARCHAR(128),
    occurred_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- notification-service : e-mails envoyés (MailHog en dev)
CREATE TABLE IF NOT EXISTS notif.notifications (
    id         BIGSERIAL PRIMARY KEY,
    doc_id     BIGINT,
    recipient  VARCHAR(255),
    status     VARCHAR(32),
    sent_at    TIMESTAMPTZ
);

-- blockchain-service : registre d'intégrité append-only (chaîne de hash)
CREATE TABLE IF NOT EXISTS integrity.hash_chain (
    seq         BIGSERIAL PRIMARY KEY,
    doc_id      BIGINT,
    doc_hash    VARCHAR(64),   -- SHA-256 du document
    prev_hash   VARCHAR(64),   -- chain_hash du maillon précédent
    chain_hash  VARCHAR(64),   -- SHA256(doc_hash || prev_hash)
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ocr-service : texte extrait (Tika / Tesseract)
CREATE TABLE IF NOT EXISTS ocr.extracted_text (
    doc_id        BIGINT PRIMARY KEY,
    text          TEXT,
    engine        VARCHAR(32),
    extracted_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- siem-service : alertes du moteur de règles
CREATE TABLE IF NOT EXISTS siem.alerts (
    id         BIGSERIAL PRIMARY KEY,
    doc_id     BIGINT,
    rule       VARCHAR(64),
    severity   VARCHAR(16),
    detail     TEXT,
    raised_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
