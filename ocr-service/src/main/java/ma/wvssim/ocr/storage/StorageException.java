package ma.wvssim.ocr.storage;

/** Erreur lors de la lecture d'un fichier dans MinIO. */
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
