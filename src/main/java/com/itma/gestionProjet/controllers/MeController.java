package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.ApiResponse;
import com.itma.gestionProjet.dtos.UserDTO;
import com.itma.gestionProjet.entities.AuditEntry;
import com.itma.gestionProjet.entities.Project;
import com.itma.gestionProjet.entities.Role;
import com.itma.gestionProjet.entities.User;
import com.itma.gestionProjet.entities.UserProjectRole;
import com.itma.gestionProjet.repositories.AuditRepository;
import com.itma.gestionProjet.repositories.UserProjectRoleRepository;
import com.itma.gestionProjet.repositories.UserRepository;
import com.itma.gestionProjet.services.imp.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Page « Mon profil » : l'utilisateur agit uniquement sur son propre compte.
 * L'utilisateur est toujours déterminé par le JWT, jamais par un identifiant transmis par le client.
 */
@RestController
@RequestMapping("/me")
public class MeController {

    private static final List<String> TYPES_CONNEXION = List.of("CONNEXION_REUSSIE", "CONNEXION_ECHEC");
    private static final int MAX_CONTACT = 30;
    private static final int MAX_LOCALITY = 100;
    private static final int MAX_IMAGE_URL = 500;

    private final UserRepository userRepository;
    private final UserService userService;
    private final UserProjectRoleRepository userProjectRoleRepository;
    private final AuditRepository auditRepository;

    public MeController(UserRepository userRepository,
                        UserService userService,
                        UserProjectRoleRepository userProjectRoleRepository,
                        AuditRepository auditRepository) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.userProjectRoleRepository = userProjectRoleRepository;
        this.auditRepository = auditRepository;
    }

    /** Champs modifiables par l'utilisateur lui-même. Tout autre champ envoyé est ignoré. */
    public record MeUpdateRequest(String contact, String locality, String imageUrl) {}

    public record ConnexionDTO(Long id, String type, String details, LocalDateTime date) {}

    public record ConnexionsPage(List<ConnexionDTO> items, long total, int page, int size) {}

    public record MonProjetDTO(Long id, String libelle, String description, String status,
                               String imageUrl, String colors, List<String> roles) {}

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<UserDTO> getMe() {
        return new ApiResponse<>(200, "Mon profil", userService.convertEntityToDto(currentUser()));
    }

    @PutMapping
    @Transactional
    public ApiResponse<UserDTO> updateMe(@RequestBody MeUpdateRequest request) {
        User user = currentUser();

        String contact = clean(request.contact(), MAX_CONTACT, "Le contact");
        String locality = clean(request.locality(), MAX_LOCALITY, "La localité");
        String imageUrl = clean(request.imageUrl(), MAX_IMAGE_URL, "La photo");

        user.setContact(contact);
        user.setLocality(locality);
        user.setImageUrl(imageUrl);
        userRepository.save(user);

        auditRepository.save(new AuditEntry(
                "PROFIL_MODIFIE",
                "Modification de son profil (contact, localité, photo)",
                user.getEmail(),
                LocalDateTime.now()
        ));
        return new ApiResponse<>(200, "Profil mis à jour.", userService.convertEntityToDto(user));
    }

    @GetMapping("/connexions")
    public ApiResponse<ConnexionsPage> getMesConnexions(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        String email = currentEmail();

        Page<AuditEntry> entries = auditRepository.findByUtilisateurIgnoreCaseAndTypeActionInOrderByDateActionDesc(
                email, TYPES_CONNEXION, PageRequest.of(safePage, safeSize));

        List<ConnexionDTO> items = entries.getContent().stream()
                .map(e -> new ConnexionDTO(e.getId(), e.getTypeAction(), e.getDetails(), e.getDateAction()))
                .toList();
        return new ApiResponse<>(200, "Historique des connexions",
                new ConnexionsPage(items, entries.getTotalElements(), safePage, safeSize));
    }

    @GetMapping("/projets")
    @Transactional(readOnly = true)
    public ApiResponse<List<MonProjetDTO>> getMesProjets() {
        User user = currentUser();

        // Rôles par projet (user_project_role) ; à défaut, les rôles globaux du compte.
        Map<Long, List<String>> rolesParProjet = new HashMap<>();
        for (UserProjectRole upr : userProjectRoleRepository.findByUserId(user.getId())) {
            if (upr.getProject() != null && upr.getRole() != null) {
                rolesParProjet.computeIfAbsent(upr.getProject().getId(), k -> new ArrayList<>())
                        .add(upr.getRole().getName());
            }
        }
        List<String> rolesGlobaux = user.getRoles() == null ? List.of()
                : user.getRoles().stream().map(Role::getName).filter(Objects::nonNull).toList();

        List<MonProjetDTO> projets = (user.getProjects() == null ? List.<Project>of() : user.getProjects()).stream()
                .sorted(Comparator.comparing(p -> Optional.ofNullable(p.getLibelle()).orElse(""),
                        String.CASE_INSENSITIVE_ORDER))
                .map(p -> new MonProjetDTO(
                        p.getId(), p.getLibelle(), p.getDescription(), p.getStatus(),
                        p.getImageUrl(), p.getColors(),
                        rolesParProjet.getOrDefault(p.getId(), rolesGlobaux).stream().distinct()
                                .collect(Collectors.toList())))
                .toList();
        return new ApiResponse<>(200, "Mes projets", projets);
    }

    private String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié");
        }
        return auth.getName();
    }

    private User currentUser() {
        return userRepository.findFirstByEmailIgnoreCase(currentEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
    }

    /** Supprime les espaces, transforme "" en null et refuse les valeurs trop longues (400). */
    private static String clean(String value, int maxLength, String label) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    label + " ne doit pas dépasser " + maxLength + " caractères.");
        }
        return trimmed;
    }
}
