package ma.wvssim.documents.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Identifiants demo (utilisateur unique, pas de table users : hors perimetre du sujet). */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        String username,
        String passwordHash,
        String jwtSecret,
        long jwtTtlMinutes
) {
}
