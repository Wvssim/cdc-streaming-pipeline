package ma.wvssim.documents.api;

import ma.wvssim.common.security.JwtService;
import ma.wvssim.documents.security.SecurityProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    /**
     * Retourne directement un 401 (pas de throw) : un ResponseStatusException declenche
     * sendError -> forward /error, qui repasse par la chaine Spring Security et se fait
     * bloquer en 403 vide par anyRequest().authenticated() (verifie en conditions reelles).
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        boolean valid = properties.username().equals(request.username())
                && passwordEncoder.matches(request.password(), properties.passwordHash());
        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new LoginResponse(jwtService.generate(request.username())));
    }
}
