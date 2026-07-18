package ma.wvssim.documents;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DocumentsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentsApiApplication.class, args);
    }
}
