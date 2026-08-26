package ma.wvssim.documents.api;

import ma.wvssim.common.security.JwtService;
import ma.wvssim.documents.security.SecurityProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SecurityProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(SecurityProperties properties, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        boolean valid = properties.username().equals(request.username())
                && passwordEncoder.matches(request.password(), properties.passwordHash());
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "identifiants invalides");
        }
        return new LoginResponse(jwtService.generate(request.username()));
    }
}
