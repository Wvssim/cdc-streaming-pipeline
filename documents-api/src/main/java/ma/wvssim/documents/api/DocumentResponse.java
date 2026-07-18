package ma.wvssim.documents.api;

import ma.wvssim.documents.domain.Document;

import java.time.OffsetDateTime;

public record DocumentResponse(
        Long id,
        String filename,
        String contentType,
        Long size,
        String storageKey,
        String uploadedBy,
        OffsetDateTime uploadedAt
) {

    public static DocumentResponse from(Document d) {
        return new DocumentResponse(
                d.getId(),
                d.getFilename(),
                d.getContentType(),
                d.getSize(),
                d.getStorageKey(),
                d.getUploadedBy(),
                d.getUploadedAt());
    }
}
