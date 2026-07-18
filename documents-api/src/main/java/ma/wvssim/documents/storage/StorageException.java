package ma.wvssim.documents.storage;

/** Echec lors du depot ou de la lecture d'un fichier dans le stockage objet. */
public class StorageException extends RuntimeException {

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
