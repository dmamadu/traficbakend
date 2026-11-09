package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.dtos.LoginDTO;
import com.itma.gestionProjet.entities.User;
import com.itma.gestionProjet.entities.VerificationToken;
import com.itma.gestionProjet.repositories.AuditRepository;
import com.itma.gestionProjet.repositories.VerificationTokenRepository;
import com.itma.gestionProjet.services.imp.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Login avec mauvais identifiants et verifyEmail avec jeton inconnu :
 * ces deux cas renvoyaient une erreur 500.
 */
@ExtendWith(MockitoExtension.class)
class UserControllerAuthErrorsTest {

    @Mock private ApplicationEventPublisher publisher;
    @Mock private VerificationTokenRepository tokenRepository;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private AuditRepository auditRepository;
    @Mock private UserService userService;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController(publisher, tokenRepository);
        ReflectionTestUtils.setField(controller, "authenticationManager", authenticationManager);
        ReflectionTestUtils.setField(controller, "auditRepository", auditRepository);
        ReflectionTestUtils.setField(controller, "userService", userService);
    }

    private static LoginDTO login(String email) {
        LoginDTO dto = new LoginDTO();
        dto.setEmail(email);
        dto.setPassword("mauvais");
        return dto;
    }

    @Test
    void login_mauvaisIdentifiants_renvoie401EtTraceLEchec() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        ResponseEntity<?> response = controller.login(login("inconnu@example.invalid"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        AApiResponse<?> body = (AApiResponse<?>) response.getBody();
        assertThat(body.getResponseCode()).isEqualTo(401);
        assertThat(body.getMessage()).isEqualTo("Email ou mot de passe incorrect.");
        verify(auditRepository).save(any());
    }

    @Test
    void login_compteDesactive_renvoie401AvecMessageDedie() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("User is disabled"));

        ResponseEntity<?> response = controller.login(login("desactive@example.invalid"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(((AApiResponse<?>) response.getBody()).getMessage()).isEqualTo("Ce compte n'est pas activé.");
    }

    @Test
    void verifyEmail_jetonInconnu_renvoie400() {
        when(tokenRepository.findByToken("bidon")).thenReturn(null);

        ResponseEntity<String> response = controller.verifyEmail("bidon");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Invalid verification token");
        verifyNoInteractions(userService);
    }

    @Test
    void verifyEmail_compteDejaActive_renvoie200() {
        User user = new User();
        user.setEnabled(true);
        VerificationToken token = new VerificationToken();
        token.setUser(user);
        when(tokenRepository.findByToken("ok")).thenReturn(token);

        ResponseEntity<String> response = controller.verifyEmail("ok");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("already been verified");
    }

    @Test
    void verifyEmail_jetonExpire_renvoie400AvecLeMotif() {
        User user = new User();
        user.setEnabled(false);
        VerificationToken token = new VerificationToken();
        token.setUser(user);
        when(tokenRepository.findByToken("expire")).thenReturn(token);
        when(userService.validateToken("expire")).thenReturn("Token already expired");

        ResponseEntity<String> response = controller.verifyEmail("expire");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Token already expired");
    }
}
