package com.itma.gestionProjet.services;

import com.itma.gestionProjet.dtos.DatabasePapEconomiqueDto;
import com.itma.gestionProjet.entities.DatabasePapEconomique;
import com.itma.gestionProjet.requests.DatabasePapEconomiqueRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface DatabasePapEconomiqueService {
    List<DatabasePapEconomiqueDto> createPapEconomiques(List<DatabasePapEconomiqueRequest> requests);
    Page<DatabasePapEconomiqueDto> getAllPapEconomiques(int page, int size);
    Page<DatabasePapEconomiqueDto> getPapEconomiquesByProjectId(Long projectId, int page, int size);
    DatabasePapEconomiqueDto getPapEconomiqueById(Long id);
    DatabasePapEconomiqueDto updatePapEconomique(Long id, DatabasePapEconomiqueRequest request);
    void deletePapEconomique(Long id);

    DatabasePapEconomique getByCodePap(String codePap);
}

