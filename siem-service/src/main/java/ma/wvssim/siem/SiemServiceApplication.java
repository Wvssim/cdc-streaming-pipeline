package ma.wvssim.siem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SiemServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SiemServiceApplication.class, args);
    }
}
