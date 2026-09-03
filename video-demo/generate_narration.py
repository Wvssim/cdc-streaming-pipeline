import asyncio
from pathlib import Path

import edge_tts


# The source is intentionally ASCII-only. French characters use Unicode escapes,
# so neither PowerShell nor the Windows command line can corrupt them.
SEGMENTS = (
    ("01-intro", "Cette d\u00e9monstration pr\u00e9sente une plateforme documentaire compl\u00e8te. Son architecture relie une interface web, plusieurs microservices, une base de donn\u00e9es, un bus d'\u00e9v\u00e9nements et un stockage de fichiers."),
    ("02-upload", "Apr\u00e8s une connexion s\u00e9curis\u00e9e, l'utilisateur d\u00e9pose un document depuis l'interface. Le fichier et ses informations sont enregistr\u00e9s, puis sa fiche d\u00e9taill\u00e9e devient imm\u00e9diatement accessible."),
    ("03-pipeline", "Le connecteur d\u00e9tecte la transaction et la publie dans le flux. Les services d'audit, de notification, d'int\u00e9grit\u00e9, de reconnaissance de texte et de s\u00e9curit\u00e9 travaillent ensuite en parall\u00e8le."),
    ("04-features", "L'application rassemble la piste d'audit, les notifications, la cha\u00eene de hachage et les alertes de s\u00e9curit\u00e9. Le fichier original reste t\u00e9l\u00e9chargeable depuis sa fiche."),
    ("05-apis", "La documentation interactive pr\u00e9sente clairement toutes les op\u00e9rations disponibles : d\u00e9p\u00f4t, consultation, t\u00e9l\u00e9chargement, renommage et suppression. L'outil de messagerie confirme aussi l'envoi des notifications."),
    ("06-kafka", "L'interface de supervision permet d'observer le flux de changements et les groupes de consommateurs. Chaque service conserve sa propre position de lecture et peut rejouer les \u00e9v\u00e9nements."),
    ("07-storage", "Les fichiers volumineux restent dans le stockage objet. Seule leur r\u00e9f\u00e9rence circule dans le flux, ce qui rend les \u00e9changes plus l\u00e9gers et plus efficaces."),
    ("08-results", "La validation finale confirme cinquante-cinq tests r\u00e9ussis, la reconnaissance de texte, un rejeu sans doublon et pr\u00e8s de quatre cents d\u00e9p\u00f4ts trait\u00e9s par minute, sans retard r\u00e9siduel."),
)


async def main() -> None:
    output = Path(__file__).parent / "video-demo" / "output" / "narration-natural"
    output.mkdir(parents=True, exist_ok=True)
    for name, text in SEGMENTS:
        target = output / f"{name}.mp3"
        speech = edge_tts.Communicate(
            text=text,
            voice="fr-FR-VivienneMultilingualNeural",
            rate="-2%",
            pitch="-2Hz",
        )
        await speech.save(str(target))
        print(target)


if __name__ == "__main__":
    asyncio.run(main())
