package com.itma.gestionProjet.services;

import com.itma.gestionProjet.dtos.DatabasePapAgricoleResponseDTO;
import com.itma.gestionProjet.dtos.PartieInteresseDTO;
import com.itma.gestionProjet.dtos.PartieInteresseResponseDTO;
import com.itma.gestionProjet.entities.CategoriePartieInteresse;
import com.itma.gestionProjet.entities.PartieInteresse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PartieInteresseService {
    List<PartieInteresse> findAll();

    Page<PartieInteresse> getPartieInteressesByProjectId(Long projectId, Pageable pageable);
    Optional<PartieInteresse> findById(Long id);
    PartieInteresse save(PartieInteresseResponseDTO partieInteresse);
    void deleteById(Long id);

    Page<PartieInteresse> getPartieInteresses(Pageable pageable);

    PartieInteresse  update(Long id, PartieInteresseResponseDTO partieInteresse);

    Page<PartieInteresse> getByCategorieLibelle(String categorieLibelle, Pageable pageable);
}