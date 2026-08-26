package ma.wvssim.documents.api;

import ma.wvssim.common.security.JwtService;
import ma.wvssim.documents.security.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthControllerTest {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private static final SecurityProperties PROPERTIES = new SecurityProperties(
            "wassim", ENCODER.encode("wassim2026"), "test-secret-at-least-32-bytes-long-please", 60);

    private final AuthController controller = new AuthController(
            PROPERTIES, ENCODER, new JwtService(PROPERTIES.jwtSecret(), Duration.ofMinutes(PROPERTIES.jwtTtlMinutes())));

    @Test
    void correctCredentialsReturnAUsableToken() {
        LoginResponse response = controller.login(new LoginRequest("wassim", "wassim2026"));

        assertNotNull(response.token());
        JwtService verifier = new JwtService(PROPERTIES.jwtSecret(), Duration.ofMinutes(60));
        assertEquals("wassim", verifier.validateAndGetSubject(response.token()));
    }

    @Test
    void wrongPasswordIsRejected() {
        assertThrows(ResponseStatusException.class, () -> controller.login(new LoginRequest("wassim", "mauvais-mdp")));
    }

    @Test
    void unknownUsernameIsRejected() {
        assertThrows(ResponseStatusException.class, () -> controller.login(new LoginRequest("inconnu", "wassim2026")));
    }
}
