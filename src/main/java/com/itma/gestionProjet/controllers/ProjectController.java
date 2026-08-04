package com.itma.gestionProjet.controllers;


import com.itma.gestionProjet.dtos.ApiResponse;
import com.itma.gestionProjet.dtos.ProjectDTO;
import com.itma.gestionProjet.dtos.UserDTO;
import com.itma.gestionProjet.entities.Project;
import com.itma.gestionProjet.entities.Role;
import com.itma.gestionProjet.entities.User;
import com.itma.gestionProjet.events.RegistrationCompleteEvent;
import com.itma.gestionProjet.exceptions.EmailAlreadyExistsException;
import com.itma.gestionProjet.exceptions.ProjectAlreadyExistException;
import com.itma.gestionProjet.repositories.ProjectRepository;
import com.itma.gestionProjet.repositories.UserRepository;
import com.itma.gestionProjet.requests.ProjectRequest;
import com.itma.gestionProjet.requests.UserRequest;
import com.itma.gestionProjet.services.imp.ProjectService;
import com.itma.gestionProjet.services.imp.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@Slf4j
@RequestMapping("/projects")
@Validated
public class ProjectController {
    @Autowired
    ProjectService projectService;

    @Autowired
    private ProjectRepository projectRepository;
    @PreAuthorize("@permissionChecker.has('PROJETS_VOIR')")
    @RequestMapping(path = "/all", method = RequestMethod.GET)
    public ApiResponse<List<ProjectDTO>> getUsers() {
        List<ProjectDTO> projects = projectService.getAllProjects();
        return new ApiResponse<>(HttpStatus.OK.value(), "Liste des projets récupérée avec succès", projects);
    }


//    @RequestMapping(path = "/createProject", method = RequestMethod.POST)
//    public ApiResponse<User> createProject(@Valid @RequestBody ProjectRequest projectRequest) {
//        Optional<Project> projectOptional= projectRepository.findByLibelle(projectRequest.getLibelle());
//        if(projectOptional.isPresent()){
//            throw  new ProjectAlreadyExistException("Un projet avec ce nom existe déja!!!");
//        }
//        else {
//            ProjectDTO projectDTO = projectService.saveProject(projectRequest);
//            return   new ApiResponse<>(HttpStatus.OK.value(), "Projet Créé avec succées crée avec succés",projectDTO);
//        }
//    }
@PreAuthorize("@permissionChecker.has('PROJETS_CREER')")
@RequestMapping(path = "/createProject/{userId}", method = RequestMethod.POST)
public ApiResponse<User> createProject(
        @PathVariable Long userId,
        @Valid @RequestBody ProjectRequest projectRequest) {

    Optional<Project> projectOptional = projectRepository.findByLibelle(projectRequest.getLibelle());
    if(projectOptional.isPresent()){
        throw new ProjectAlreadyExistException("Un projet avec ce nom existe déjà !!!");
    }
    else {
        ProjectDTO projectDTO = projectService.saveProject(projectRequest, userId);
        return new ApiResponse<>(HttpStatus.OK.value(), "Projet créé avec succès", projectDTO);
    }
}

    @PreAuthorize("@permissionChecker.has('PROJETS_MODIFIER')")
    @PostMapping("/updateProject")
    public ApiResponse<Project> updateProject(@RequestBody ProjectRequest projectRequest) {
        try {
            ProjectDTO projectDTO = projectService.updateProject(projectRequest);
            return new ApiResponse<>(HttpStatus.OK.value(), "Projet mis à jour avec succès", projectDTO);
        } catch (Exception e) {
            // Log l'erreur
           // logger.error("Erreur lors de la mise à jour du projet : " + e.getMessage(), e);

            // Retourne une réponse d'erreur avec le message approprié
            return new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erreur lors de la mise à jour du projet : " + e.getMessage(), null);
        }
    }


    @PreAuthorize("@permissionChecker.has('PROJETS_VOIR')")
    @GetMapping("/{id}")
    public ApiResponse<ProjectDTO> getProjectById(@PathVariable Long id) {
        Optional<ProjectDTO> projectDTO = projectService.findProjectById(id);
        if (projectDTO.isPresent()) {
            return new ApiResponse<> (HttpStatus.OK.value(), "",projectDTO.get());
        } else {
            return   new ApiResponse<>(HttpStatus.NO_CONTENT.value(), "",ResponseEntity.notFound().build());
        }
    }


    @PreAuthorize("@permissionChecker.has('PROJETS_SUPPRIMER')")
    @DeleteMapping("/delete/{id}")
    public ApiResponse<?> deleteProject(@PathVariable Long id) throws Exception {
        try {
            projectService.deleteProjectById(id);
            return new  ApiResponse<>(HttpStatus.OK.value(),"Project deleted successfully.",null);
        } catch (Exception e) {
            throw  new Exception( "An error occurred while deleting the user."+e);
        }
    }

}
