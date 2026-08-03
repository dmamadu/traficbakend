package com.itma.gestionProjet.security;

import com.itma.gestionProjet.entities.Permission;
import com.itma.gestionProjet.entities.UserProjectRole;
import com.itma.gestionProjet.repositories.UserProjectRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Point d'entrée unique des vérifications de permission, appelé depuis les contrôleurs via
 * @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_SUPPRIMER')").
 *
 * Ne fige jamais les permissions dans le JWT : elles sont résolues à chaque appel à partir de
 * UserProjectRole, pour rester correctes si l'utilisateur change de projet actif en cours de session.
 *
 * Voir GESTION_PERMISSIONS_DOC.md §5.
 */
@Component("permissionChecker")
public class PermissionChecker {

    private static final String SUPER_ADMIN = "Super Admin";

    @Autowired
    private UserProjectRoleRepository userProjectRoleRepository;

    /**
     * @param projectId projet actif, ou null pour une action non scopée à un projet (ex. gestion des rôles)
     * @param code      code de permission requis, ex. "PAP_SUPPRIMER"
     */
    public boolean has(Long projectId, String code) {
        Set<String> granted = resolveGrantedCodes(projectId);
        return granted == null || granted.contains(code);
    }

    /** Variante pour les actions qui ne sont jamais scopées à un projet (ex. /roles/**). */
    public boolean has(String code) {
        return has(null, code);
    }

    /**
     * Résout les codes de permission accordés à l'utilisateur courant pour le projet donné
     * (utilisé aussi par {@code GET /permissions/effective} pour le frontend).
     *
     * @return null si Super Admin (sentinel "toutes les permissions"), sinon l'ensemble des codes accordés
     */
    public Set<String> resolveGrantedCodes(Long projectId) {
        UserRegistrationDetails userDetails = currentUserDetails();
        if (userDetails == null) {
            return Set.of();
        }
        if (userDetails.getRoleNames().contains(SUPER_ADMIN)) {
            return null;
        }

        Long userId = userDetails.getUser().getId();
        List<UserProjectRole> assignments = Stream.concat(
                        projectId != null
                                ? userProjectRoleRepository.findByUserIdAndProjectId(userId, projectId).stream()
                                : Stream.empty(),
                        userProjectRoleRepository.findByUserIdAndProjectIdIsNull(userId).stream()
                )
                .toList();

        return assignments.stream()
                .map(UserProjectRole::getRole)
                .filter(role -> role != null && role.getPermissions() != null)
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toSet());
    }

    private UserRegistrationDetails currentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object principal = auth.getPrincipal();
        return principal instanceof UserRegistrationDetails details ? details : null;
    }
}
