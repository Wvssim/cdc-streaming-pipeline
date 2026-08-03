package ma.wvssim.ocr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OcrServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OcrServiceApplication.class, args);
    }
}
