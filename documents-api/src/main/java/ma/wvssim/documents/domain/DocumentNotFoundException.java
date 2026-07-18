package ma.wvssim.documents.domain;

public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(Long id) {
        super("document introuvable : " + id);
    }
}
