package ma.wvssim.ocr.consumer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TesseractExtractorTest {

    // Pas de test d'extraction reelle ici : necessite le binaire natif Tesseract + tessdata,
    // absents de l'environnement CI. Voir README pour l'installation locale.

    @Test
    void refuseUnContenuQuiNestPasUneImage() {
        byte[] contenu = "pas une image".getBytes();
        assertThrows(IllegalStateException.class, () -> TesseractExtractor.extract(contenu, "tessdata", "fra+eng"));
    }

    @Test
    void refuseUnContenuNul() {
        assertThrows(IllegalStateException.class, () -> TesseractExtractor.extract(null, "tessdata", "fra+eng"));
    }
}
