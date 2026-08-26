package ma.wvssim.common.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void generatedTokenRoundTripsToTheSameSubject() {
        JwtService service = new JwtService(SECRET, Duration.ofMinutes(30));

        String token = service.generate("wassim");

        assertEquals("wassim", service.validateAndGetSubject(token));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService service = new JwtService(SECRET, Duration.ofMillis(1));
        String token = service.generate("wassim");

        assertThrows(ExpiredJwtException.class, () -> {
            Thread.sleep(10);
            service.validateAndGetSubject(token);
        });
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService issuer = new JwtService(SECRET, Duration.ofMinutes(30));
        JwtService verifier = new JwtService("fedcba9876543210fedcba9876543210fedcba98765432", Duration.ofMinutes(30));
        String token = issuer.generate("wassim");

        assertThrows(JwtException.class, () -> verifier.validateAndGetSubject(token));
    }
}
