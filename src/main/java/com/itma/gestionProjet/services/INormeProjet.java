package com.itma.gestionProjet.services;



import com.itma.gestionProjet.dtos.NormeProjetDTO;
import com.itma.gestionProjet.entities.NormeProjet;
import com.itma.gestionProjet.requests.NormeProjetRequest;

import java.util.List;
import java.util.Optional;

public interface INormeProjet {

    Optional<NormeProjet> findNormeProjetByName(String name);



    NormeProjet saveNormeProjet1(NormeProjet p,Long id);

    List<NormeProjet> saveNormeProjet(List<NormeProjet> normeProjets, Long projectId);

    NormeProjetDTO updateNormeProjet(NormeProjetDTO p);
    NormeProjetDTO getNormeProjetById(Long id);
    List<NormeProjet> getAllNormeProjets();
    void deleteNormeProjet(NormeProjet p);


    void deleteNormeProjetById(Long id);

    List<NormeProjet> findByProjectId(Long projectId);

    NormeProjet updateSingleNorme(NormeProjet normeProjet);

    NormeProjetDTO convertEntityToDto(NormeProjet p);

    NormeProjet convertDtoToEntity(NormeProjetRequest normeProjetRequest);
}
