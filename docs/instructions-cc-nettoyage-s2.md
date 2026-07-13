# Instructions — nettoyage du repo et préparation de la S2

Bon inventaire. On exécute dans cet ordre. Ne code aucune logique métier : la S2 est une semaine d'infrastructure.

---

## Étape 0 — Couper les traces d'agent (à faire EN PREMIER)

1. Crée ou édite `.claude/settings.json` à la racine du projet avec :
   ```json
   { "includeCoAuthoredBy": false }
   ```
   Objectif : ne plus ajouter de trailer `Co-Authored-By` ni de mention "Generated with…" dans les messages de commit.

2. Ajoute au `.gitignore` :
   ```
   CLAUDE.md
   CLAUDE.local.md
   .claude/
   ```
   (Garde `.idea/` et `*.iml` ignorés, c'est correct.)

3. Vérifie l'historique existant et **montre-moi le résultat sans rien réécrire** :
   ```bash
   git log --format='%H %an %ae %s' | head -50
   git log --format='%B' | grep -in "claude\|co-authored\|generated with"
   ```
   Si des commits contiennent des trailers d'agent, liste-les-moi et attends ma décision avant toute réécriture d'historique.

---

## Étape 1 — Séparer le contenu (versionné) du contenant (ignoré)

Le `CLAUDE.md` actuel contient la source de vérité du projet. On la déplace dans un document versionné et lisible par un humain.

1. **Crée `docs/ARCHITECTURE.md`** en reprenant le contenu du `CLAUDE.md` actuel, avec ces adaptations :
   - Supprime l'encadré d'avertissement sur RetailPulse (transitoire, sans intérêt une fois le legacy purgé)
   - Renomme la section « Stack figée — ne pas proposer d'alternative » en **« Stack technique et justification des choix »** — même contenu, mais rédigé pour un lecteur humain
   - Renomme « Hors périmètre — ne PAS proposer, ne PAS implémenter » en **« Périmètre exclu (perspectives)** »
   - Supprime la section « Ce que le prof attend en soutenance ». Réintègre les notions techniques qu'elle contient (Claim Check, fan-out, at-least-once, DLT, dual-write, extensibilité) dans la section **Invariants**, où elles ont naturellement leur place
   - Corrige la référence : `docs/cahier-des-charges.tex` (LaTeX, pas `.md`) — sur ce point c'est le CLAUDE.md qui avait tort, le repo a raison
   - Le reste (flux nominal, architecture, invariants, structure du monorepo, modèle de données, conventions, commandes) est transféré tel quel

2. **Réduis `CLAUDE.md` à un pointeur** de quelques lignes :
   ```markdown
   # CLAUDE.md

   La source de vérité de ce projet est `docs/ARCHITECTURE.md`.
   Lis-la intégralement avant toute tâche.

   Règles de travail :
   - Ne jamais contredire les invariants de docs/ARCHITECTURE.md
   - Ne jamais proposer une technologie hors de la stack figée
   - Ne jamais implémenter un sujet listé dans « Périmètre exclu »
   - Annoncer le plan avant d'exécuter une tâche structurante
   - Le plan de travail est dans docs/plan-6-semaines.md
   ```

3. **`docs/ARCHITECTURE.md` doit être committé.** `CLAUDE.md` reste gitignoré.

---

## Étape 2 — Purger le legacy RetailPulse

1. **Avant de supprimer `analytics-consumer/`**, extrais et montre-moi :
   - la classe `@KafkaListener`
   - la logique de désérialisation de l'enveloppe Debezium (`before` / `after` / `op` / `ts_ms`)

   Ce sont les seuls éléments réutilisables : ils iront dans le futur module `common`. Garde-les de côté.

2. Supprime ensuite **tout** le reste de `analytics-consumer/` : entités `Vente`, repositories, services de vues analytiques, `pom.xml` du module.

3. **Renomme `docker/` en `infra/`.** La structure de référence est celle de `docs/ARCHITECTURE.md`, pas celle du disque. Ne modifie pas la doc pour coller au dossier — c'est l'inverse.

---

## Étape 3 — Reconstruire l'infrastructure (le cœur de la S2)

Dans `infra/`, produis les trois fichiers, conformes à `docs/ARCHITECTURE.md` :

1. **`docker-compose.yml`** — 6 containers :
   - `postgres` : PostgreSQL 17, `command: ["postgres", "-c", "wal_level=logical"]`, base `docdb`
   - `kafka` : Apache Kafka 4.1 en **mode KRaft** (aucun Zookeeper — si le compose actuel en contient un, il disparaît)
   - `connect` : Kafka Connect avec Debezium 3.5
   - `minio` : stockage objet, bucket `documents`, console sur `:9001`
   - `mailhog` : SMTP de dev, interface web sur `:8025`
   - `kafka-ui` : Kafbat UI sur `:8080`

   Supprime le second PostgreSQL (`analytics`) : il n'existe plus dans l'architecture cible.

2. **`init.sql`** :
   - Schéma `public` → table `documents` (`id`, `filename`, `content_type`, `size`, `storage_key`, `uploaded_by`, `uploaded_at`)
   - `ALTER TABLE documents REPLICA IDENTITY FULL;`
   - Les 5 schémas des consommateurs : `audit`, `notif`, `integrity`, `ocr`, `siem`, avec leurs tables (voir le modèle de données dans `docs/ARCHITECTURE.md`)

3. **`register-postgres.json`** — connecteur Debezium :
   - `plugin.name: pgoutput`
   - `topic.prefix: docs`
   - `table.include.list: public.documents` — **et rien d'autre.** Les schémas des consommateurs ne doivent jamais être captés, sous peine de boucle infinie.
   - `decimal.handling.mode: string`
   - `snapshot.mode: initial`

---

## Étape 4 — Le jalon S2

C'est le seul objectif de la semaine, et il passe **avant** la création du squelette Maven :

```bash
cd infra && docker compose up -d
docker compose ps                                  # les 6 containers en "running"

curl -X POST -H "Content-Type: application/json" \
  --data @register-postgres.json http://localhost:8083/connectors
curl http://localhost:8083/connectors/source-postgres-connector/status   # RUNNING

docker exec -it cdc-postgres psql -U cdc -d docdb \
  -c "INSERT INTO documents (filename, content_type, size, storage_key, uploaded_by) VALUES ('test.pdf','application/pdf',1024,'test-key','wassim');"
```

🎯 **Jalon atteint quand l'événement JSON apparaît dans Kafbat UI, topic `docs.public.documents`.**

Raison de cet ordre : Debezium et le WAL sont la brique la plus incertaine du projet. Si le CDC ne capte rien (`wal_level` mal réglé, plugin `pgoutput` absent, permissions de réplication manquantes), il faut le découvrir **avant** d'investir une journée dans une arborescence Maven qui ne sert à rien tant que le pipeline ne tourne pas. On dérisque, on ne décore pas.

**Le squelette Maven (parent pom + module `common`) et la CI GitHub Actions viennent APRÈS le jalon**, pas avant.

---

## Séquence de validation

Arrête-toi et montre-moi le résultat après **chaque étape**. Ne passe pas à la suivante sans mon feu vert.
