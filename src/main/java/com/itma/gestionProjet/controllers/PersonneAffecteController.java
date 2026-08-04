package com.itma.gestionProjet.controllers;


import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.dtos.ApiResponse;
import com.itma.gestionProjet.dtos.PersonneAffecteDTO;
import com.itma.gestionProjet.entities.PersonneAffecte;
import com.itma.gestionProjet.entities.Project;
import com.itma.gestionProjet.repositories.ProjectRepository;
import com.itma.gestionProjet.services.ExcelService;
import com.itma.gestionProjet.services.imp.PersonneAffecteServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/personneAffectes")
public class PersonneAffecteController {

    @Autowired
    private PersonneAffecteServiceImpl personneAffecteService;

    @Autowired
    private ProjectRepository projectRepository;

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping
    public ResponseEntity<AApiResponse<PersonneAffecteDTO>> getPersonneAffectes(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int max,
            @RequestParam(required = false) Long projectId) {
        Page<PersonneAffecteDTO> personneAffectePage = personneAffecteService.getPersonneAffectes(offset, max);
        AApiResponse<PersonneAffecteDTO> response = new AApiResponse<>();
        response.setResponseCode(200);
        response.setData(personneAffectePage.getContent());
        response.setOffset(offset);
        response.setMax(max);
        response.setLength(personneAffectePage.getTotalElements());
        return ResponseEntity.ok().body(response);
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_VOIR')")
    @GetMapping("/{id}")
    public ResponseEntity<PersonneAffecte> getPersonneAffecteById(@PathVariable Long id, @RequestParam Long projectId) {
        Optional<PersonneAffecte> personneAffecte = personneAffecteService.getPersonneAffecteById(id);
        return personneAffecte.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_CREER')")
    @PostMapping
    public ResponseEntity<AApiResponse<PersonneAffecteDTO>> createPersonneAffecte(@RequestBody PersonneAffecteDTO personneAffecte, @RequestParam Long projectId) {
        AApiResponse<PersonneAffecteDTO> response = new AApiResponse<>();
        try {
            PersonneAffecteDTO savedPersonneAffecte = personneAffecteService.savePersonneAffecte(personneAffecte);
            response.setResponseCode(200);
            response.setMessage("Personne affecté créé avec succés");
            response.setData(Collections.singletonList(savedPersonneAffecte));
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            response.setResponseCode(500); // or any other appropriate error code
            response.setMessage("An error occurred while saving the PersonneAffecte: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_CREER')")
    @PostMapping("/importer")
    public ResponseEntity<ApiResponse<List<PersonneAffecteDTO>>> createPersonneAffectes(@RequestBody List<PersonneAffecteDTO> personneAffectes, @RequestParam Long projectId) {
        try {
            List<PersonneAffecteDTO> savedPersonneAffectes = personneAffecteService.savePersonneAffectes(personneAffectes);
            ApiResponse<List<PersonneAffecteDTO>> response = new ApiResponse<>(HttpStatus.CREATED.value(), "Personnes affectées importées avec succès", savedPersonneAffectes);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            ApiResponse<List<PersonneAffecteDTO>> errorResponse = new ApiResponse<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Erreur lors de la création des personnes affectées", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }



    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_MODIFIER')")
    @PutMapping("/{id}")

    public ResponseEntity<AApiResponse<PersonneAffecteDTO>> updatePersonneAffecte(@PathVariable Long id, @RequestBody PersonneAffecteDTO personneAffecteDetails, @RequestParam Long projectId) {
        AApiResponse<PersonneAffecteDTO> response = new AApiResponse<>();
        try {
            PersonneAffecteDTO updatedPersonneAffecte = personneAffecteService.updatePersonneAffecte(id, personneAffecteDetails);
            response.setResponseCode(200);
            response.setMessage("Personne affecté mis à jour avec succès");
            response.setData(Collections.singletonList(updatedPersonneAffecte));
            return ResponseEntity.ok().body(response);
        } catch (RuntimeException e) {
            response.setResponseCode(404);
            response.setMessage("Personne affecté non trouvée: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.setResponseCode(500);
            response.setMessage("An error occurred while updating the PersonneAffecte: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_SUPPRIMER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePersonneAffecte(@PathVariable Long id, @RequestParam Long projectId) {
        personneAffecteService.deletePersonneAffecte(id);
        return ResponseEntity.noContent().build();
    }


    @Autowired
    private ExcelService excelService;

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_IMPORTER')")
    @PostMapping("/import-personnes-affectees")
    public ResponseEntity<String> importPersonneAffecteData(@RequestParam("file") MultipartFile file, @RequestParam Long projectId) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please upload a file!");
        }
        try {
            excelService.importPersonneAffecteData(file);
            return ResponseEntity.status(HttpStatus.OK).body("File imported successfully!");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to import data: " + e.getMessage());
        }
    }



    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_MODIFIER')")
    @PutMapping("addImage/{id}")

    public ResponseEntity<AApiResponse<PersonneAffecteDTO>> addImageToPap(@PathVariable Long id, @RequestBody String url, @RequestParam Long projectId) {
        AApiResponse<PersonneAffecteDTO> response = new AApiResponse<>();
        try {
            PersonneAffecteDTO updatedPersonneAffecte = personneAffecteService.addImageToPap(id, url);
            response.setResponseCode(200);
            response.setMessage("Personne affecté mis à jour avec succès");
            response.setData(Collections.singletonList(updatedPersonneAffecte));
            return ResponseEntity.ok().body(response);
        } catch (RuntimeException e) {
            response.setResponseCode(404);
            response.setMessage("Personne affecté non trouvée: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.setResponseCode(500);
            response.setMessage("An error occurred while updating the PersonneAffecte: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_MODIFIER')")
    @PutMapping("addSignature/{id}")
    public ResponseEntity<AApiResponse<PersonneAffecteDTO>> addSignature(@PathVariable Long id, @RequestBody String url, @RequestParam Long projectId) {
        AApiResponse<PersonneAffecteDTO> response = new AApiResponse<>();
        try {
            PersonneAffecteDTO updatedPersonneAffecte = personneAffecteService.addSignatureToPap(id, url);
            response.setResponseCode(200);
            response.setMessage("Personne affecté mis à jour avec succès");
            response.setData(Collections.singletonList(updatedPersonneAffecte));
            return ResponseEntity.ok().body(response);
        } catch (RuntimeException e) {
            response.setResponseCode(404);
            response.setMessage("Personne affecté non trouvée: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.setResponseCode(500);
            response.setMessage("An error occurred while updating the PersonneAffecte: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'PAP_MODIFIER')")
    @PutMapping("dedommagerPap/{id}")
    public ResponseEntity<AApiResponse<PersonneAffecteDTO>> dedommagerPap(@PathVariable Long id, @RequestBody String url, @RequestParam Long projectId) {
        AApiResponse<PersonneAffecteDTO> response = new AApiResponse<>();
        try {
            PersonneAffecteDTO updatedPersonneAffecte = personneAffecteService.dedommagerPap(id, url);
            response.setResponseCode(200);
            response.setMessage("Personne affecté mis à jour avec succès");
            response.setData(Collections.singletonList(updatedPersonneAffecte));
            return ResponseEntity.ok().body(response);
        } catch (RuntimeException e) {
            response.setResponseCode(404);
            response.setMessage("Personne affecté non trouvée: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            response.setResponseCode(500);
            response.setMessage("An error occurred while updating the PersonneAffecte: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}