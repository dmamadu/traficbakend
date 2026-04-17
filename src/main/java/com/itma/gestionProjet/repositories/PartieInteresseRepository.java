package com.itma.gestionProjet.repositories;

import com.itma.gestionProjet.entities.DatabasePapAgricole;
import com.itma.gestionProjet.entities.PartieInteresse;
import com.itma.gestionProjet.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PartieInteresseRepository extends JpaRepository<PartieInteresse, Long> {

    Optional<PartieInteresse> findByLibelle(String libelle);

    @EntityGraph(attributePaths = "contacts")
    List<PartieInteresse> findAll();


    @Query("SELECT p FROM PartieInteresse p LEFT JOIN FETCH p.contacts")
    List<PartieInteresse> findAllWithContacts();

    Page<PartieInteresse> findByProjectId(Long projectId, Pageable pageable);

    Optional<PartieInteresse> findBycourrielPrincipal(String courrielPrincipal);

    Optional<PartieInteresse> findByCourrielPrincipal(String courrielPrincipal);

    Page<PartieInteresse> findByCategorie(String categorie, Pageable pageable);
}
