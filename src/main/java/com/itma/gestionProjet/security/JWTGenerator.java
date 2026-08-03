package com.itma.gestionProjet.security;


import java.util.Date;

import com.itma.gestionProjet.entities.Project;
import com.itma.gestionProjet.entities.Role;
import com.itma.gestionProjet.entities.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.List;
import java.util.stream.Collectors;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.SignatureAlgorithm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;


@Slf4j
@Component
public class JWTGenerator {
    private final SecretKey key;

    public JWTGenerator(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }
    // Génération du token JWT
    public String generateToken(Authentication authentication) {
        // Récupération de l'utilisateur authentifié
        UserRegistrationDetails userDetails = (UserRegistrationDetails) authentication.getPrincipal();
        User user = userDetails.getUser(); // Utilise le getter pour accéder à l'objet User

        // Récupération du nom d'utilisateur
        String username = user.getEmail(); // Utilisation de l'email comme identifiant

        // Récupération des rôles de l'utilisateur
        List<String> roles = user.getRoles().stream()
                .map(Role::getName) // Adapte selon ton modèle (Role::getName si nécessaire)
                .collect(Collectors.toList());

        // Récupération des projets de l'utilisateur
        List<Long> projectIds = user.getProjects().stream()
                .map(Project::getId)
                .collect(Collectors.toList());

        // Dates d'émission et d'expiration du token
        Date currentDate = new Date();
        Date expireDate = new Date(currentDate.getTime() + SecurityConstants.JWT_EXPIRATION);
        // Génération du token avec les rôles et les projets en claims
        return Jwts.builder()
                .setSubject(username)  // Nom d'utilisateur
                .claim("roles", roles) // Ajout des rôles
                .claim("projectIds", projectIds) // Ajout des projets
                .setIssuedAt(currentDate)
                .setExpiration(expireDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
    // Récupération du nom d'utilisateur à partir du token
    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    // Validation du token
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception ex) {
            log.error("Validation JWT échouée : {} - {}", ex.getClass().getSimpleName(), ex.getMessage());
            return false;
        }
    }
}
