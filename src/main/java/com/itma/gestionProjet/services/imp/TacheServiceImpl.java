package com.itma.gestionProjet.services.imp;

import com.itma.gestionProjet.dtos.ConsultantResponse;
import com.itma.gestionProjet.dtos.TacheDTO;
import com.itma.gestionProjet.dtos.TacheResponseDTO;
import com.itma.gestionProjet.dtos.UserDTO;
import com.itma.gestionProjet.entities.Project;
import com.itma.gestionProjet.entities.Tache;
import com.itma.gestionProjet.entities.TacheStatut;
import com.itma.gestionProjet.entities.User;
import com.itma.gestionProjet.exceptions.TacheNotFoundException;
import com.itma.gestionProjet.repositories.ProjectRepository;
import com.itma.gestionProjet.repositories.TacheRepository;
import com.itma.gestionProjet.repositories.TacheSpecifications;
import com.itma.gestionProjet.services.ITacheService;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TacheServiceImpl implements ITacheService {

    @Autowired
    private TacheRepository tacheRepository;
/*
    @Override
    public Tache createTache(Tache tache) {
        return tacheRepository.save(tache);
    }


 */

    @Autowired
    private ProjectRepository projectRepository;

    @Override
    public Tache createTache(Tache tache,Long projectId) {
        // Validation des données de base
        if (tache == null) {
            throw new IllegalArgumentException("La tâche ne peut pas être nulle");
        }
        validateProgression(tache.getProgression());
        validateStatut(tache.getStatut());
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé avec l'ID: " + projectId));
        tache.setProject(project);
        return tacheRepository.save(tache);
    }

    private void validateProgression(Integer progression) {
        if (progression != null && (progression < 0 || progression > 100)) {
            throw new IllegalArgumentException("La progression doit être comprise entre 0 et 100");
        }
    }

    private void validateStatut(String statut) {
        if (statut != null && !TacheStatut.isValid(statut)) {
            throw new IllegalArgumentException(
                    "Statut invalide : " + statut + ". Valeurs autorisées : " + TacheStatut.allowedValues());
        }
    }

    private Tache findTacheInProject(Long id, Long projectId) {
        Tache tache = tacheRepository.findById(id)
                .orElseThrow(() -> new TacheNotFoundException("Tâche non trouvée avec l'ID: " + id));
        if (tache.getProject() == null || !tache.getProject().getId().equals(projectId)) {
            throw new TacheNotFoundException("Tâche non trouvée avec l'ID: " + id);
        }
        return tache;
    }

    /*
    @Override
    public Tache updateTache(Long id, Tache tache) {
        tache.setId(id);
        return tacheRepository.save(tache);
    }

     */

    @Override
    @Transactional
    public TacheDTO updateTache(Long id, Tache updatedTache, Long projectId) {
        validateProgression(updatedTache.getProgression());
        validateStatut(updatedTache.getStatut());
        Tache existingTache = findTacheInProject(id, projectId);
        existingTache.setLibelle(updatedTache.getLibelle());
        existingTache.setDescription(updatedTache.getDescription());
        existingTache.setDateDebut(updatedTache.getDateDebut());
        existingTache.setDateFin(updatedTache.getDateFin());
        existingTache.setObservation(updatedTache.getObservation());
        if (updatedTache.getProgression() != null) {
            existingTache.setProgression(updatedTache.getProgression());
        }
        // Une tâche à 100% est considérée comme complétée, quel que soit le statut envoyé
        existingTache.setStatut(
                existingTache.getProgression() != null && existingTache.getProgression() == 100
                        ? "complete"
                        : updatedTache.getStatut()
        );
        if (updatedTache.getUtilisateurs() != null) {
            existingTache.setUtilisateurs(updatedTache.getUtilisateurs());
        }
        return convertEntityToDto(existingTache);
    }

    @Override
    public void deleteTache(Long id, Long projectId) {
        Tache tache = findTacheInProject(id, projectId);
        tacheRepository.delete(tache);
    }

    @Override
    public List<Tache> getAllTaches() {
        return tacheRepository.findAll();
    }

    @Override
    public Tache getTacheById(Long id, Long projectId) {
        return findTacheInProject(id, projectId);
    }

    @Override
    public Page<TacheDTO> getAllTaches(PageRequest pageRequest, String search, String statut) {
        Specification<Tache> spec = Specification.where(TacheSpecifications.matchesSearch(search))
                .and(TacheSpecifications.hasStatut(statut));
        Page<Tache> tachePage = tacheRepository.findAll(spec, pageRequest);
        List<TacheDTO> tacheDTOs = tachePage.stream()
                .map(this::convertEntityToDto)
                .collect(Collectors.toList());
        return new PageImpl<>(tacheDTOs, pageRequest, tachePage.getTotalElements());
    }


    @Override
    public Page<TacheDTO> getTachesByProjectId(Long projectId, PageRequest pageRequest, String search, String statut) {
        Specification<Tache> spec = Specification.where(TacheSpecifications.hasProjectId(projectId))
                .and(TacheSpecifications.matchesSearch(search))
                .and(TacheSpecifications.hasStatut(statut));
        Page<Tache> tachePage = tacheRepository.findAll(spec, pageRequest);
        List<TacheDTO> tacheDTOs = tachePage.stream()
                .map(this::convertEntityToDto)
                .collect(Collectors.toList());
        return new PageImpl<>(tacheDTOs, pageRequest, tachePage.getTotalElements());
    }

    private TacheDTO convertEntityToDto(Tache tache) {
        TacheDTO dto = new TacheDTO();
        dto.setId(tache.getId());
        dto.setLibelle(tache.getLibelle());
        dto.setDescription(tache.getDescription());
        dto.setDateDebut(tache.getDateDebut());
        dto.setDateFin(tache.getDateFin());
        dto.setStatut(tache.getStatut());
        dto.setProgression(tache.getProgression());
        Set<ConsultantResponse> consultantDTOs = tache.getUtilisateurs().stream()
                .map(this::convertConsultantToDto)
                .collect(Collectors.toSet());
        dto.setUtilisateurs(consultantDTOs);
        return dto;
    }

    private ConsultantResponse convertConsultantToDto(User consultant) {
        ConsultantResponse consultantDTO = new ConsultantResponse();
        consultantDTO.setId(Math.toIntExact(consultant.getId()));
        consultantDTO.setLastname(consultant.getLastname());
        consultantDTO.setFirstname(consultant.getFirstname());
        consultantDTO.setEmail(consultant.getEmail());
        consultantDTO.setContact(consultant.getContact());
        consultantDTO.setLocality(consultant.getLocality());
        consultantDTO.setEnabled(consultant.getEnabled());
        return consultantDTO;
    }



    public Page<Tache> getTachesByUserId(Long userId, Long projectId, Pageable pageable) {
        return tacheRepository.findTachesByUserIdAndProjectId(userId, projectId, pageable);
    }

    public TacheResponseDTO mapToDTO(Tache tache) {
        TacheResponseDTO dto = new TacheResponseDTO();
        dto.setId(tache.getId());
        dto.setLibelle(tache.getLibelle());
        dto.setDescription(tache.getDescription());
        dto.setDateDebut(tache.getDateDebut());
        dto.setDateFin(tache.getDateFin());
        dto.setStatut(tache.getStatut());
        dto.setProgression(tache.getProgression());
        return dto;
    }

}
