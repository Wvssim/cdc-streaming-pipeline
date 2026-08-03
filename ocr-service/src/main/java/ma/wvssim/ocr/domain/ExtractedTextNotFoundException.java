package ma.wvssim.ocr.domain;

public class ExtractedTextNotFoundException extends RuntimeException {

    public ExtractedTextNotFoundException(Long docId) {
        super("texte extrait introuvable pour le document : " + docId);
    }
}
