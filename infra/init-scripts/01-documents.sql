-- ============================================================
--  01-documents.sql
--  Schéma public : la table SOURCE, seule captée par Debezium.
--  Rejoué automatiquement au 1er boot par docker-entrypoint-initdb.d.
-- ============================================================

CREATE TABLE IF NOT EXISTS public.documents (
    id            BIGSERIAL     PRIMARY KEY,
    filename      VARCHAR(255)  NOT NULL,
    content_type  VARCHAR(128)  NOT NULL,
    size          BIGINT        NOT NULL CHECK (size >= 0),
    storage_key   VARCHAR(512)  NOT NULL,   -- clé de l'objet dans MinIO (Claim Check)
    uploaded_by   VARCHAR(128)  NOT NULL,
    uploaded_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- REPLICA IDENTITY FULL : indispensable pour que Debezium reçoive l'image complète
-- de la ligne "before" sur les UPDATE/DELETE (par défaut, seule la PK est loguée dans le WAL).
ALTER TABLE public.documents REPLICA IDENTITY FULL;
