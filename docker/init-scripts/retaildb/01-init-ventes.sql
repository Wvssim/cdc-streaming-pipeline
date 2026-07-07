CREATE TABLE ventes (
    id             BIGSERIAL PRIMARY KEY,
    magasin        VARCHAR(100)   NOT NULL,
    produit        VARCHAR(100)   NOT NULL,
    quantite       INTEGER        NOT NULL CHECK (quantite > 0),
    prix_unitaire  NUMERIC(10, 2) NOT NULL CHECK (prix_unitaire >= 0),
    montant_total  NUMERIC(10, 2) GENERATED ALWAYS AS (quantite * prix_unitaire) STORED,
    date_vente     TIMESTAMP      NOT NULL DEFAULT now()
);

-- REPLICA IDENTITY FULL : indispensable pour que Debezium reçoive l'image complete
-- de la ligne "before" sur les UPDATE/DELETE (par defaut, seule la PK est loguee dans le WAL).
ALTER TABLE ventes REPLICA IDENTITY FULL;
