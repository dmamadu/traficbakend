package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.*;
import com.itma.gestionProjet.entities.PartieInteresse;
import com.itma.gestionProjet.entities.User;
import com.itma.gestionProjet.exceptions.PartieInteresseNotFoundException;
import com.itma.gestionProjet.services.PartieInteresseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/partie-interesse")
public class PartieInteresseController {

    @Autowired
    private PartieInteresseService service;
    
    /*
    @GetMapping
    public ResponseEntity<AApiResponse<PartieInteresseResponseDTO>> getAll(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int max,
            @RequestParam(required = false) String categorieLibelle) {

        Pageable pageable = PageRequest.of(offset, max);
        Page<PartieInteresse> partieInteressePage;
            partieInteressePage = service.getPartieInteresses(pageable);

        AApiResponse<PartieInteresseResponseDTO> response = new AApiResponse<>();
        response.setResponseCode(200);

        // Convertir les entités PartieInteresse en DTO
        List<PartieInteresseResponseDTO> dtoList = partieInteressePage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        response.setData(dtoList);
        response.setOffset(offset);
        response.setMax(max);
        response.setLength(partieInteressePage.getTotalElements());

        return ResponseEntity.ok().body(response);
    }


     */


//    @GetMapping
//    public ResponseEntity<AApiResponse<PartieInteresseResponseDTO>> getAll(
//            @RequestParam(defaultValue = "0") int offset,
//            @RequestParam(defaultValue = "10") int max,
//            @RequestParam(required = false) String categorieLibelle,
//            @RequestParam(required = false) Long projectId) {
//
//        try {
//            Pageable pageable = PageRequest.of(offset, max);
//            Page<PartieInteresse> partieInteressePage;
//
//            // Vérifier si projectId est fourni
//            if (projectId != null) {
//                // Appeler la méthode pour récupérer les PartieInteresse par projectId
//                partieInteressePage = service.getPartieInteressesByProjectId(projectId, pageable);
//            } else {
//                // Appeler la méthode pour récupérer toutes les PartieInteresse
//                partieInteressePage = service.getPartieInteresses(pageable);
//            }
//
//            // Convertir les entités PartieInteresse en DTO
//            List<PartieInteresseResponseDTO> dtoList = partieInteressePage.getContent().stream()
//                    .map(this::convertToDTO) // Assurez-vous que cette méthode est définie
//                    .collect(Collectors.toList());
//
//            // Construire la réponse AApiResponse
//            AApiResponse<PartieInteresseResponseDTO> response = new AApiResponse<>();
//            response.setResponseCode(200);
//            response.setData(dtoList);
//            response.setOffset(offset);
//            response.setMax(max);
//            response.setLength(partieInteressePage.getTotalElements());
//            response.setMessage("Données récupérées avec succès.");
//
//            return ResponseEntity.ok().body(response);
//        } catch (Exception e) {
//            // Gestion des erreurs
//            AApiResponse<PartieInteresseResponseDTO> errorResponse = new AApiResponse<>();
//            errorResponse.setResponseCode(500);
//            errorResponse.setMessage("Erreur lors de la récupération des données : " + e.getMessage());
//            return ResponseEntity.status(500).body(errorResponse);
//        }
//    }


    @GetMapping
    public ResponseEntity<AApiResponse<PartieInteresseResponseDTO>> getAll(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int max,
            @RequestParam(required = false) String categorieLibelle,
            @RequestParam(required = false) Long projectId) {

        try {
            Pageable pageable = PageRequest.of(offset, max);
            Page<PartieInteresse> partieInteressePage;

            // 🔥 PRIORITÉ 1 : Filtrer par projet
            if (projectId != null) {
                partieInteressePage = service.getPartieInteressesByProjectId(projectId, pageable);
            }

            // 🔥 PRIORITÉ 2 : Filtrer par catégorie (CE QUI MANQUE ACTUELLEMENT)
            else if (categorieLibelle != null && !categorieLibelle.isEmpty()) {
                partieInteressePage = service.getByCategorieLibelle(categorieLibelle, pageable);
            }

            // 🔥 PAR DÉFAUT : Tout récupérer
            else {
                partieInteressePage = service.getPartieInteresses(pageable);
            }

            List<PartieInteresseResponseDTO> dtoList = partieInteressePage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            AApiResponse<PartieInteresseResponseDTO> response = new AApiResponse<>();
            response.setResponseCode(200);
            response.setData(dtoList);
            response.setOffset(offset);
            response.setMax(max);
            response.setLength(partieInteressePage.getTotalElements());
            response.setMessage("Données récupérées avec succès.");

            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            AApiResponse<PartieInteresseResponseDTO> errorResponse = new AApiResponse<>();
            errorResponse.setResponseCode(500);
            errorResponse.setMessage("Erreur : " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }


    @GetMapping("/{id}")
    public Optional<PartieInteresse> getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<AApiResponse<PartieInteresseResponseDTO>> create(@RequestBody PartieInteresseResponseDTO partieInteresseDTO) {
        AApiResponse<PartieInteresseResponseDTO> response = new AApiResponse<>();
        try {
            PartieInteresse savedPartieInteresse = service.save(partieInteresseDTO);
            PartieInteresseResponseDTO dto = this.convertToDTO(savedPartieInteresse);
            response.setResponseCode(200);
            response.setMessage("Partie intéressée créée avec succès");
            response.setData(Collections.singletonList(dto)); // Encapsulez le DTO dans une liste
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            response.setResponseCode(500);
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }



    @PutMapping("/{id}")
    public ResponseEntity<AApiResponse<PartieInteresseResponseDTO>> update(
            @PathVariable Long id,
            @RequestBody PartieInteresseResponseDTO partieInteresse) {
        AApiResponse<PartieInteresseResponseDTO> response = new AApiResponse<>();
        try {
            PartieInteresse updatedPip = service.update(id, partieInteresse);
            PartieInteresseResponseDTO dto = this.convertToDTO(updatedPip);
            response.setResponseCode(200);
            response.setMessage("Partie intéressée mise à jour avec succès");
            response.setData(Collections.singletonList(dto));  // Assurez-vous que setData accepte un objet unique
            return ResponseEntity.ok().body(response);
        } catch (PartieInteresseNotFoundException e) {
            response.setResponseCode(404);
            response.setMessage("Partie intéressée non trouvée : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.setResponseCode(500);
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    /*
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.deleteById(id);
    }
     */

    @DeleteMapping("/{id}")
    public ResponseEntity<AApiResponse<Void>> delete(@PathVariable Long id) {
        AApiResponse<Void> response = new AApiResponse<>();
        try {
            service.deleteById(id);
            response.setResponseCode(200);
            response.setMessage("Partie intéressée supprimée avec succès");
            return ResponseEntity.ok().body(response);
        } catch (PartieInteresseNotFoundException e) {
            response.setResponseCode(404);
            response.setMessage("Partie intéressée non trouvée : " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.setResponseCode(500);
            response.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }




    private PartieInteresseResponseDTO convertToDTO(PartieInteresse partieInteresse) {
        PartieInteresseResponseDTO responseDTO = new PartieInteresseResponseDTO();
        responseDTO.setId(partieInteresse.getId());
        responseDTO.setAdresse(partieInteresse.getAdresse());
         responseDTO.setCourrielPrincipal(partieInteresse.getCourrielPrincipal());
        responseDTO.setLibelle(partieInteresse.getLibelle());
        responseDTO.setCategorie(partieInteresse.getCategorie());
        responseDTO.setLocalisation(partieInteresse.getLocalisation());
//        responseDTO.setNormes(partieInteresse.getNormes());
        responseDTO.setStatut(partieInteresse.getStatut());
        responseDTO.setProject_id(partieInteresse.getProject().getId());

        // Mapper les utilisateurs (contacts) vers ContactDTO
        List<ContactDTO> contactDTOs = partieInteresse.getContacts().stream()
                .map(this::toContactDTO)  // Utilisation de la méthode de mappage interne
                .collect(Collectors.toList());
        responseDTO.setContacts(contactDTOs);

        return responseDTO;
    }


    private ContactDTO toContactDTO(User user) {
        if (user == null) {
            return null;
        }

        ContactDTO contactDTO = new ContactDTO();
        contactDTO.setNomContactPrincipal(user.getLastname());
        contactDTO.setPrenomContactPrincipal(user.getFirstname());
        contactDTO.setEmailContactPrincipal(user.getEmail());
        contactDTO.setAdresseContactPrincipal(user.getLocality());
        contactDTO.setTelephoneContactPrincipal(user.getContact());
        contactDTO.setSexeContactPrincipal(user.getSexe());

        return contactDTO;
    }


}