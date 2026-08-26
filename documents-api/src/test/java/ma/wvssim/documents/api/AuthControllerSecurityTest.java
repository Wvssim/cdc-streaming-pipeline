package ma.wvssim.documents.api;

import ma.wvssim.documents.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Passe par la VRAIE chaine Spring Security (contrairement a AuthControllerTest qui appelle
 * le controleur directement) : c'est ce niveau qui avait masque le bug ou un mauvais mot de
 * passe renvoyait 403 vide au lieu de 401 (ResponseStatusException -> forward /error -> repasse
 * par anyRequest().authenticated(), bloque faute de token). Verifie en conditions reelles avant
 * le fix, ce test le fige pour ne pas qu'il revienne.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void wrongPasswordReturns401NotAGeneric403() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"wassim\",\"password\":\"mauvais-mdp\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownUsernameReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"inconnu\",\"password\":\"wassim2026\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void correctCredentialsReturn200WithAToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"wassim\",\"password\":\"wassim2026\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isForbidden());
    }
}
