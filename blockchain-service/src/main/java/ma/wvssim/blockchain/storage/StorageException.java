package ma.wvssim.blockchain.storage;

/** Erreur lors de la lecture d'un fichier dans MinIO. */
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
