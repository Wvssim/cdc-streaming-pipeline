package ma.wvssim.documents;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@OpenAPIDefinition(info = @Info(
        title = "documents-api",
        version = "1.0",
        description = "Producteur : upload multipart, metadonnees PostgreSQL, fichier MinIO (Claim Check), login JWT"))
public class DocumentsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentsApiApplication.class, args);
    }
}
