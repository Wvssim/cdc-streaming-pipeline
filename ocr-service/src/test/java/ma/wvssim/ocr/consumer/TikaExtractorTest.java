package ma.wvssim.ocr.consumer;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TikaExtractorTest {

    @Test
    void extraitLeTexteBrutDetecteAutomatiquement() {
        String contenu = "Rapport de stage - plateforme documentaire evenementielle";
        String texte = TikaExtractor.extract(contenu.getBytes(StandardCharsets.UTF_8));
        assertTrue(texte.contains("plateforme documentaire"), texte);
    }

    @Test
    void refuseUnContenuIllisible() {
        // meme du binaire aleatoire ne fait pas echouer Tika (il retombe sur octet-stream),
        // seul un flux qui casse vraiment le parsing doit lever une exception.
        assertThrows(IllegalStateException.class, () -> TikaExtractor.extract(null));
    }
}
