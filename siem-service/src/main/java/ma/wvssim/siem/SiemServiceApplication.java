package ma.wvssim.siem;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@OpenAPIDefinition(info = @Info(
        title = "siem-service",
        version = "1.0",
        description = "Moteur de regles : detection d'anomalies sur les depots de documents"))
public class SiemServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SiemServiceApplication.class, args);
    }
}
