package com.itma.gestionProjet.services.imp;

import com.itma.gestionProjet.dtos.NormeProjetDTO;
import com.itma.gestionProjet.entities.NormeProjet;
import com.itma.gestionProjet.entities.Project;
import com.itma.gestionProjet.repositories.NormeProjectRepository;
import com.itma.gestionProjet.repositories.ProjectRepository;
import com.itma.gestionProjet.requests.NormeProjetRequest;
import com.itma.gestionProjet.services.INormeProjet;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NormeProjetService  implements INormeProjet {

    @Autowired
    NormeProjectRepository normeProjectRepository;

    @Autowired
    ProjectRepository projectRepository;
    @Override
    public Optional<com.itma.gestionProjet.entities.NormeProjet> findNormeProjetByName(String name) {
        return Optional.empty();
    }


    @Override
    public NormeProjet saveNormeProjet1(NormeProjet p,Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + p.getProject().getId()));
        p.setProject(project);
        return normeProjectRepository.save( p);
    }




    @Override
    public List<NormeProjet> saveNormeProjet(List<NormeProjet> normeProjets, Long projectId) {
        // Supprime tous les NormeProjet existants pour le projet spécifié
        normeProjectRepository.deleteAll(normeProjectRepository.findByProjectId(projectId));

        // Récupère le projet associé
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + projectId));

        // Associe le projet aux NormeProjets et enregistre-les
        for (NormeProjet p : normeProjets) {
            p.setProject(project);
        }

        // Enregistre tous les NormeProjets en une seule opération pour l'efficacité
        return normeProjectRepository.saveAll(normeProjets);
    }


    public void deleteExistingNormeProjets(Long projectId) {
        normeProjectRepository.deleteByProjectId(projectId);
    }



    public NormeProjet updatee(NormeProjet newNormeProjets, Long projectId) {
        // Supprimez les NormeProjet existantes associées au projet
        deleteExistingNormeProjets(projectId);

        // Ajoutez les nouvelles NormeProjet
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + newNormeProjets.getProject().getId()));
        newNormeProjets.setProject(project);
        return normeProjectRepository.save( newNormeProjets);
    }



    @Override
    public NormeProjetDTO updateNormeProjet(NormeProjetDTO p) {
        return null;
    }

    @Override
    public NormeProjetDTO getNormeProjetById(Long id) {
        return null;
    }

    @Override
    public List<NormeProjet> getAllNormeProjets() {
        return normeProjectRepository.findAll();
    }

    @Override
    public void deleteNormeProjet(com.itma.gestionProjet.entities.NormeProjet p) {

    }

    @Override
    public void deleteNormeProjetById(Long id) {
        normeProjectRepository.deleteById(id);
    }

    @Override
    public List<NormeProjet> findByProjectId(Long projectId) {
        return normeProjectRepository.findByProjectId(projectId);
    }

    @Override
    public NormeProjet updateSingleNorme(NormeProjet normeProjet) {
        NormeProjet existing = normeProjectRepository.findById((long) normeProjet.getId())
                .orElseThrow(() -> new EntityNotFoundException("NormeProjet not found with id: " + normeProjet.getId()));
        existing.setTitre(normeProjet.getTitre());
        existing.setDescription(normeProjet.getDescription());
        return normeProjectRepository.save(existing);
    }

    @Override
    public NormeProjetDTO convertEntityToDto(com.itma.gestionProjet.entities.NormeProjet p) {
        return null;
    }

    @Override
    public com.itma.gestionProjet.entities.NormeProjet convertDtoToEntity(NormeProjetRequest normeProjetRequest) {
        return null;
    }
}
