package ma.wvssim.documents.domain;

/** Contenu et metadonnees necessaires a une reponse HTTP de telechargement. */
public record DownloadedDocument(String filename, String contentType, byte[] content) {
}
