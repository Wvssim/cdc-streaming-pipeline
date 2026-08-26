package ma.wvssim.ocr.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Pas de login ici : le token est emis par documents-api, ce service se contente de le valider. */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(String jwtSecret, long jwtTtlMinutes) {
}
