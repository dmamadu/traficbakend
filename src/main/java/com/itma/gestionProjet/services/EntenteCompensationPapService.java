package com.itma.gestionProjet.services;

import com.itma.gestionProjet.dtos.EntenteCompensationPapDto;
import com.itma.gestionProjet.requests.EntenteCompensationPapRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;


public interface EntenteCompensationPapService {
    Page<EntenteCompensationPapDto> getAllEntentes(Pageable pageable);


    Page<EntenteCompensationPapDto> getEntentesByProjectId(Long projectId, Pageable pageable);


    EntenteCompensationPapDto createEntente(EntenteCompensationPapRequest request);
    EntenteCompensationPapDto updateEntente(Long id, EntenteCompensationPapRequest request);

    void deleteEntente(Long id);

    EntenteCompensationPapDto getEntenteByCodePap(String codePap);

    EntenteCompensationPapDto getEntenteByCodePap(String codePap, Long projectId);


    // Nouvelle méthode pour la recherche globale
    Page<EntenteCompensationPapDto> searchGlobalEntenteCompensationPap(String searchTerm, Optional<Long> projectId, Pageable pageable);

}
