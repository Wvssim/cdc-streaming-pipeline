package ma.wvssim.siem.consumer;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Seuils du moteur de regles, lies au prefixe {@code siem.*} de application.yml. */
@ConfigurationProperties(prefix = "siem")
public record SiemRulesProperties(
        int maxDeposits,
        int windowMinutes,
        int businessHoursStart,
        int businessHoursEnd,
        List<String> suspiciousExtensions
) {
}
