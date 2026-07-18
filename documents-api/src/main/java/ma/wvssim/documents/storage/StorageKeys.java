package ma.wvssim.documents.storage;

import java.util.UUID;

/** Fabrique de cles d'objet MinIO. Logique pure, testable sans infrastructure. */
public final class StorageKeys {

    private StorageKeys() {
    }

    /**
     * Genere une cle unique tout en conservant le nom de fichier d'origine (lisibilite),
     * en neutralisant les separateurs de chemin.
     */
    public static String forUpload(String originalFilename) {
        String safe = (originalFilename == null || originalFilename.isBlank())
                ? "fichier"
                : originalFilename.replace('/', '_').replace('\\', '_');
        return UUID.randomUUID() + "-" + safe;
    }
}
