package ma.wvssim.ocr.consumer;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;

import java.io.ByteArrayInputStream;

/** Extraction de texte via Apache Tika (detection automatique du format : PDF, DOCX, texte...). */
public final class TikaExtractor {

    private TikaExtractor() {
    }

    public static String extract(byte[] content) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(content)) {
            BodyContentHandler handler = new BodyContentHandler(-1); // -1 = pas de limite de taille
            new AutoDetectParser().parse(in, handler, new Metadata(), new ParseContext());
            return handler.toString();
        } catch (Exception e) {
            throw new IllegalStateException("extraction Tika impossible : " + e.getMessage(), e);
        }
    }
}
