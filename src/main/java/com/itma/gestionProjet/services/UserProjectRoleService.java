package com.itma.gestionProjet.services;

import com.itma.gestionProjet.dtos.UserProjectRoleDTO;
import com.itma.gestionProjet.entities.Project;
import com.itma.gestionProjet.entities.Role;
import com.itma.gestionProjet.entities.User;
import com.itma.gestionProjet.entities.UserProjectRole;
import com.itma.gestionProjet.repositories.ProjectRepository;
import com.itma.gestionProjet.repositories.RoleRepository;
import com.itma.gestionProjet.repositories.UserProjectRoleRepository;
import com.itma.gestionProjet.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Gère l'affectation d'un rôle à un utilisateur, par projet (ou globalement si projectId est null).
 * Voir GESTION_PERMISSIONS_DOC.md §3.4.
 */
@Service
public class UserProjectRoleService {

    @Autowired
    private UserProjectRoleRepository userProjectRoleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private RoleRepository roleRepository;

    public List<UserProjectRoleDTO> listByUser(Long userId) {
        return userProjectRoleRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Affecte un rôle à un utilisateur pour un projet donné (ou globalement si projectId est null).
     * Si une affectation existe déjà pour ce couple (utilisateur, projet), son rôle est remplacé —
     * un utilisateur n'a qu'un seul rôle actif par projet.
     */
    public UserProjectRoleDTO assign(Long userId, Long projectId, Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + userId));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Rôle introuvable : " + roleId));
        Project project = projectId == null ? null : projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable : " + projectId));

        UserProjectRole assignment = projectId == null
                ? userProjectRoleRepository.findByUserIdAndProjectIdIsNull(userId).stream().findFirst().orElse(null)
                : userProjectRoleRepository.findByUserIdAndProjectId(userId, projectId).stream().findFirst().orElse(null);

        if (assignment == null) {
            assignment = new UserProjectRole();
            assignment.setUser(user);
            assignment.setProject(project);
        }
        assignment.setRole(role);
        return toDto(userProjectRoleRepository.save(assignment));
    }

    public void remove(Long id) {
        if (!userProjectRoleRepository.existsById(id)) {
            throw new RuntimeException("Affectation introuvable : " + id);
        }
        userProjectRoleRepository.deleteById(id);
    }

    private UserProjectRoleDTO toDto(UserProjectRole upr) {
        UserProjectRoleDTO dto = new UserProjectRoleDTO();
        dto.setId(upr.getId());
        dto.setUserId(upr.getUser().getId());
        if (upr.getProject() != null) {
            dto.setProjectId(upr.getProject().getId());
            dto.setProjectLibelle(upr.getProject().getLibelle());
        }
        dto.setRoleId(upr.getRole().getId());
        dto.setRoleName(upr.getRole().getName());
        return dto;
    }
}
