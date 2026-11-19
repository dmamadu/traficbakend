package com.itma.gestionProjet.services.imp;

import com.itma.gestionProjet.dtos.EntenteCompensationPapDto;
import com.itma.gestionProjet.entities.DatabasePapAgricole;
import com.itma.gestionProjet.entities.DatabasePapPlaceAffaire;
import com.itma.gestionProjet.entities.EntenteCompensationPap;
import com.itma.gestionProjet.entities.Project;
import com.itma.gestionProjet.repositories.DatabasePapAgricoleRepository;
import com.itma.gestionProjet.repositories.DatabasePapPlaceAffaireRepository;
import com.itma.gestionProjet.repositories.EntenteCompensationPapRepository;
import com.itma.gestionProjet.repositories.ProjectRepository;
import com.itma.gestionProjet.requests.EntenteCompensationPapRequest;
import com.itma.gestionProjet.services.EntenteCompensationPapService;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class EntenteCompensationPapServiceImpl implements EntenteCompensationPapService {

    @Autowired
    private EntenteCompensationPapRepository repository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DatabasePapAgricoleRepository agricoleRepository;

    @Autowired
    private DatabasePapPlaceAffaireRepository placeAffaireRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public Page<EntenteCompensationPapDto> getAllEntentes(Pageable pageable) {
        Page<EntenteCompensationPap> ententes = repository.findAll(pageable);
        return ententes.map(this::convertEntityToDto);
    }

    @Override
    public Page<EntenteCompensationPapDto> getEntentesByProjectId(Long projectId, Pageable pageable) {
        Page<EntenteCompensationPap> ententesPage = repository.findByProjectId(projectId, pageable);
        return ententesPage.map(entente -> modelMapper.map(entente, EntenteCompensationPapDto.class));
    }

    @Override
    public EntenteCompensationPapDto createEntente(EntenteCompensationPapRequest request) {
        if (repository.existsByCodePap(request.getCodePap())) {
            throw new RuntimeException("Une entente avec le codePap " + request.getCodePap() + " existe déjà.");
        }

        modelMapper.getConfiguration().setAmbiguityIgnored(true);
        // Ignorer le mapping de l'ID pour éviter que l'ID soit mappé à partir de la requête.
        modelMapper.typeMap(EntenteCompensationPapRequest.class, EntenteCompensationPap.class)
                .addMappings(mapper -> mapper.skip(EntenteCompensationPap::setId));

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        DatabasePapAgricole databasePapAgricole = null;
        if (request.getDatabasePapAgricoleId() != null) {
            databasePapAgricole = agricoleRepository.findById(request.getDatabasePapAgricoleId())
                    .orElseThrow(() -> new EntityNotFoundException("DatabasePapAgricole not found"));
        }

        DatabasePapPlaceAffaire databasePapPlaceAffaire = null;
        if (request.getDatabasePapPlaceAffaireId() != null) {
            databasePapPlaceAffaire = placeAffaireRepository.findById(request.getDatabasePapPlaceAffaireId())
                    .orElseThrow(() -> new EntityNotFoundException("DatabasePapPlaceAffaire not found"));
        }

        EntenteCompensationPap entente = modelMapper.map(request, EntenteCompensationPap.class);
        entente.setProject(project);
        entente.setDatabasePapAgricole(databasePapAgricole);
        entente.setDatabasePapPlaceAffaire(databasePapPlaceAffaire);

        // Map additional fields if necessary
        return convertEntityToDto(repository.save(entente));
    }

    @Override
    public EntenteCompensationPapDto updateEntente(Long id, EntenteCompensationPapRequest request) {
        EntenteCompensationPap existingEntente = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entente not found"));

        modelMapper.map(request, existingEntente);
        // Map additional fields if necessary
        return convertEntityToDto(repository.save(existingEntente));
    }

    private EntenteCompensationPapDto convertEntityToDto(EntenteCompensationPap entity) {
        return modelMapper.map(entity, EntenteCompensationPapDto.class);
    }

    @Override
    public void deleteEntente(Long id) {
        EntenteCompensationPap entente = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entente not found with ID: " + id));
        repository.delete(entente);
    }

    @Override
    public EntenteCompensationPapDto getEntenteByCodePap(String codePap) {
        EntenteCompensationPap entente = repository.findByCodePap(codePap)
                .orElseThrow(() -> new RuntimeException("Entente not found with codePap: " + codePap));
        return modelMapper.map(entente, EntenteCompensationPapDto.class);
    }

    @Override
    public EntenteCompensationPapDto getEntenteByCodePap(String codePap, Long projectId) {
        EntenteCompensationPap entente = repository.findFirstByCodePapAndProjectIdOrderByIdDesc(codePap, projectId)
                .orElseThrow(() -> new RuntimeException("Entente not found with codePap: " + codePap));
        return modelMapper.map(entente, EntenteCompensationPapDto.class);
    }

    @Override
    public Page<EntenteCompensationPapDto> searchGlobalEntenteCompensationPap(String searchTerm, Optional<Long> projectId, Pageable pageable) {
        Page<EntenteCompensationPap> pageResult = repository.searchGlobal(searchTerm,projectId, pageable);
        return pageResult.map(this::convertEntityToDto);
    }
}

