package ma.wvssim.ocr.consumer;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

/** Extraction de texte sur images (JPEG, PNG, ...) via Tesseract (binaire natif, wrapper JNA). */
public final class TesseractExtractor {

    private TesseractExtractor() {
    }

    public static String extract(byte[] content, String dataPath, String languages) {
        BufferedImage image = readImage(content);
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(dataPath);
        tesseract.setLanguage(languages);
        try {
            return tesseract.doOCR(image);
        } catch (TesseractException e) {
            throw new IllegalStateException("extraction Tesseract impossible : " + e.getMessage(), e);
        }
    }

    private static BufferedImage readImage(byte[] content) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(content)) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new IllegalArgumentException("format d'image non reconnu");
            }
            return image;
        } catch (Exception e) {
            throw new IllegalStateException("lecture de l'image impossible : " + e.getMessage(), e);
        }
    }
}
