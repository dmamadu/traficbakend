package com.itma.gestionProjet.services.imp;

import com.itma.gestionProjet.dtos.ProjectDTO;
import com.itma.gestionProjet.entities.*;
import com.itma.gestionProjet.repositories.*;
import com.itma.gestionProjet.requests.ProjectRequest;
import com.itma.gestionProjet.services.IProjectService;
import com.itma.gestionProjet.services.UserProjectRoleService;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProjectService implements IProjectService {


    @Autowired
    ModelMapper modelMapper;
    @Autowired
    ProjectRepository projectRepository;


    @Autowired
    private NormeProjectRepository normeProjectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProjectRoleService userProjectRoleService;
    @Override
    public Optional<Project> findProjectByName(String name) {
        return Optional.empty();
    }

    public ProjectDTO saveProject(ProjectRequest projectRequest, Long userId) {
        Project project = new Project();
        project.setLibelle(projectRequest.getLibelle());
        project.setDescription(projectRequest.getDescription());
        project.setStatus(projectRequest.getStatus());
        project.setDatedebut(projectRequest.getDatedebut());
        project.setDatefin(projectRequest.getDatefin());
        project.setImageUrl(projectRequest.getImageUrl());
        project.setColors(projectRequest.getColors());

        // Récupérer l'utilisateur principal par son ID (celui de l'URL)
        User mainUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Créer la liste des utilisateurs et ajouter l'utilisateur principal
        List<User> users = new ArrayList<>();
        users.add(mainUser);

        // Ajouter le projet à l'utilisateur principal
        mainUser.getProjects().add(project);

        // Gestion des utilisateurs supplémentaires du corps de la requête
        if (projectRequest.getUsers() != null && !projectRequest.getUsers().isEmpty()) {
            for (User userRequest : projectRequest.getUsers()) {
                User additionalUser = userRepository.findById(userRequest.getId())
                        .orElseThrow(() -> new IllegalArgumentException("User not found: " + userRequest.getId()));

                // Éviter les doublons
                if (!users.contains(additionalUser)) {
                    users.add(additionalUser);
                    additionalUser.getProjects().add(project);
                }
            }
        }

        project.setUsers(users);

        Project savedProject = projectRepository.save(project);

        // Affecte à chaque utilisateur du projet (principal + additionnels) une UserProjectRole
        // reprenant son rôle légal actuel : sans cela, resolveGrantedCodes() ne trouve aucune
        // affectation pour ce nouveau projet et l'utilisateur voit une sidebar vide dessus.
        for (User u : users) {
            if (u.getRoles() != null && !u.getRoles().isEmpty()) {
                userProjectRoleService.assign(u.getId(), savedProject.getId(), u.getRoles().get(0).getId());
            }
        }

        // Gestion des normes
        if (projectRequest.getNormes() != null && !projectRequest.getNormes().isEmpty()) {
            for (NormeProjet norme : projectRequest.getNormes()) {
                norme.setProject(savedProject);
                normeProjectRepository.save(norme);
            }
        }

        return convertEntityToDto(savedProject);
    }



    @Override
    public ProjectDTO updateProject(ProjectRequest projectRequest) {
        // Vérifiez si le projet existe
        Project existingProject = projectRepository.findById(projectRequest.getId())
                .orElseThrow(() -> new IllegalArgumentException("Project not found with id: " + projectRequest.getId()));

        // Mise à jour des champs principaux
        existingProject.setLibelle(projectRequest.getLibelle());
        existingProject.setDescription(projectRequest.getDescription());
        existingProject.setStatus(projectRequest.getStatus());
        existingProject.setDatedebut(projectRequest.getDatedebut());
        existingProject.setDatefin(projectRequest.getDatefin());
        existingProject.setImageUrl(projectRequest.getImageUrl());

        // Utilisateurs actuels du projet, capturés avant dissociation pour pouvoir retirer leur
        // affectation de rôle plus bas si elle n'est plus dans la liste mise à jour.
        List<User> previousUsers = existingProject.getUsers() != null
                ? new ArrayList<>(existingProject.getUsers())
                : new ArrayList<>();

        // Dissociation des utilisateurs existants
        for (User user : previousUsers) {
            user.getProjects().remove(existingProject);
        }

        // Mise à jour des utilisateurs
        List<User> updatedUsers = new ArrayList<>();
        if (projectRequest.getUsers() != null && !projectRequest.getUsers().isEmpty()) {
            updatedUsers = projectRequest.getUsers().stream()
                    .map(user -> userRepository.findById(user.getId())
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + user.getId())))
                    .collect(Collectors.toList());
            existingProject.setUsers(updatedUsers);
            for (User user : updatedUsers) {
                user.getProjects().add(existingProject);
            }
        }
        if (existingProject.getNormes() != null && !existingProject.getNormes().isEmpty()) {
            for (NormeProjet norme : existingProject.getNormes()) {
                norme.setProject(null);
                normeProjectRepository.delete(norme);
            }
        }
        if (projectRequest.getNormes() != null && !projectRequest.getNormes().isEmpty()) {
            for (NormeProjet norme : projectRequest.getNormes()) {
                norme.setProject(existingProject);
                normeProjectRepository.save(norme);
            }
        }
        Project updatedProject = projectRepository.save(existingProject);

        // Retire l'affectation de rôle des utilisateurs qui ne sont plus sur ce projet (sinon
        // resolveGrantedCodes() continue de leur accorder les permissions du projet après retrait),
        // et (ré)affecte celle des utilisateurs qui y restent ou arrivent selon leur rôle légal actuel.
        Set<Long> updatedUserIds = updatedUsers.stream().map(User::getId).collect(Collectors.toSet());
        for (User user : previousUsers) {
            if (!updatedUserIds.contains(user.getId())) {
                userProjectRoleService.removeAllForUserAndProject(user.getId(), updatedProject.getId());
            }
        }
        for (User user : updatedUsers) {
            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                userProjectRoleService.assign(user.getId(), updatedProject.getId(), user.getRoles().get(0).getId());
            }
        }

        return convertEntityToDto(updatedProject);
    }
    @Override
    public Optional<ProjectDTO> findProjectById(Long id) {
        Optional<Project> project = projectRepository.findById(id);
        if (project.isPresent()) {
            return project.map(this::convertEntityToDto);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public List<ProjectDTO> getAllProjects() {
        return projectRepository.findAllByOrderByIdDesc().stream()
                .map(this::convertEntityToDto)
                .collect(Collectors.toList());
    }
    @Override
    public void deleteProject(Project p) {

    }

    @Override
    public void deleteProjectById(Long id) {

        Project project = projectRepository.findById((long) Math.toIntExact(id)).orElseThrow(() -> new IllegalArgumentException("project not found with id " + id));
        List<NormeProjet> normeProjects = normeProjectRepository.findByProjectId(id);
        for (NormeProjet normeProject : normeProjects) {
            normeProjectRepository.delete(normeProject);
        }
        projectRepository.save(project);
        projectRepository.deleteById((long) Math.toIntExact(id));
    }

    @Override
    public ProjectDTO convertEntityToDto(Project p) {
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        return modelMapper.map(p, ProjectDTO.class);
    }

    @Override
    public Project convertDtoToEntity(Project p) {
        Project project = new Project();
        project = modelMapper.map(p, Project.class);
        return project;
    }
    @Override
    public Project getProjectById(Long projectId) {
        return projectRepository.findById(projectId).orElse(null);
    }
}
