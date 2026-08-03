package ma.wvssim.ocr.api;

import ma.wvssim.ocr.domain.ExtractedText;

import java.time.OffsetDateTime;

public record OcrResponse(
        Long docId,
        String text,
        String engine,
        OffsetDateTime extractedAt
) {

    public static OcrResponse from(ExtractedText e) {
        return new OcrResponse(e.getDocId(), e.getText(), e.getEngine(), e.getExtractedAt());
    }
}
