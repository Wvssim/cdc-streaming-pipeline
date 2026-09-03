package ma.wvssim.ocr.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TesseractExtractorTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "TESSDATA_PATH", matches = ".+")
    void extraitReellementDuTexteAvecLeBinaireNatif() throws Exception {
        BufferedImage image = new BufferedImage(600, 150, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 54));
        graphics.drawString("CDC PIPELINE", 55, 95);
        graphics.dispose();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);

        String extracted = TesseractExtractor.extract(
                output.toByteArray(), System.getenv("TESSDATA_PATH"), "eng");

        assertThat(extracted.toUpperCase()).contains("CDC", "PIPELINE");
    }

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
