package com.itma.gestionProjet.services.imp;

import com.itma.gestionProjet.dtos.DatabasePapAgricoleRequestDTO;
import com.itma.gestionProjet.dtos.DatabasePapEconomiqueDto;
import com.itma.gestionProjet.entities.DatabasePapAgricole;
import com.itma.gestionProjet.entities.DatabasePapEconomique;
import com.itma.gestionProjet.entities.Project;
import com.itma.gestionProjet.repositories.DatabasePapEconomiqueRepository;
import com.itma.gestionProjet.repositories.ProjectRepository;
import com.itma.gestionProjet.requests.DatabasePapEconomiqueRequest;
import com.itma.gestionProjet.services.DatabasePapEconomiqueService;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DatabasePapEconomiqueServiceImpl implements DatabasePapEconomiqueService {

    private final DatabasePapEconomiqueRepository repository;
    private final ProjectRepository projectRepository;
    private final ModelMapper modelMapper;
    public DatabasePapEconomiqueServiceImpl(DatabasePapEconomiqueRepository repository, ProjectRepository projectRepository, ModelMapper modelMapper) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public List<DatabasePapEconomiqueDto> createPapEconomiques(List<DatabasePapEconomiqueRequest> requests) {
        List<DatabasePapEconomique> papEconomiques = new ArrayList<>();

        for (DatabasePapEconomiqueRequest request : requests) {
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found"));
            DatabasePapEconomique papEconomique = modelMapper.map(request, DatabasePapEconomique.class);
            if (papEconomique.getType() == null || papEconomique.getType().isEmpty()) {
                papEconomique.setType("PAPECONOMIQUE");
            }
            if (papEconomique.getStatutPap() == null || papEconomique.getStatutPap().isEmpty()) {
                papEconomique.setType("recense");
            }
            papEconomique.setProject(project);
            papEconomiques.add(papEconomique);
        }
        List<DatabasePapEconomique> savedEntities = repository.saveAll(papEconomiques);
        return savedEntities.stream()
                .map(entity -> modelMapper.map(entity, DatabasePapEconomiqueDto.class))
                .collect(Collectors.toList());
    }


    @Override
    public Page<DatabasePapEconomiqueDto> getAllPapEconomiques(int page, int size) {
        Page<DatabasePapEconomique> papEconomiques = repository.findAll(PageRequest.of(page, size));
        return papEconomiques.map(entity -> modelMapper.map(entity, DatabasePapEconomiqueDto.class));
    }

    @Override
    public Page<DatabasePapEconomiqueDto> getPapEconomiquesByProjectId(Long projectId, int page, int size) {
        Page<DatabasePapEconomique> papEconomiques = repository.findByProjectId(projectId, PageRequest.of(page, size));
        return papEconomiques.map(entity -> modelMapper.map(entity, DatabasePapEconomiqueDto.class));
    }

    @Override
    public DatabasePapEconomiqueDto getPapEconomiqueById(Long id) {
        DatabasePapEconomique papEconomique = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("PapEconomique not found"));
        return modelMapper.map(papEconomique, DatabasePapEconomiqueDto.class);
    }

    @Override
    public DatabasePapEconomiqueDto updatePapEconomique(Long id, DatabasePapEconomiqueRequest request) {
        DatabasePapEconomique papEconomique = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("PapEconomique not found"));
        modelMapper.typeMap(DatabasePapEconomiqueRequest.class, DatabasePapEconomique.class)
                .addMappings(mapper -> mapper.skip(DatabasePapEconomique::setId));
        modelMapper.map(request, papEconomique);
        if (request.getProjectId() != null) {
            Project project = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found"));
            papEconomique.setProject(project);
        }
        if (request.getType() != null && !request.getType().isEmpty()) {
            papEconomique.setType(request.getType());
        } else {
            papEconomique.setType("PAPECONOMIQUE");
        }
        DatabasePapEconomique updatedEntity = repository.save(papEconomique);
        return modelMapper.map(updatedEntity, DatabasePapEconomiqueDto.class);
    }


    @Override
    public void deletePapEconomique(Long id) {
        DatabasePapEconomique papEconomique = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("PapEconomique not found"));
        repository.delete(papEconomique);
    }


    public DatabasePapEconomique getByCodePap(String codePap) {
        return repository.findByCodePap(codePap)
                .orElseThrow(() -> new EntityNotFoundException("DatabasePapEconomique avec codePap " + codePap + " introuvable"));
    }
}
