package com.itma.gestionProjet.services;

import com.itma.gestionProjet.dtos.EntenteDetailsDTO;
import com.itma.gestionProjet.entities.*;
import com.itma.gestionProjet.repositories.DatabasePapAgricoleRepository;
import com.itma.gestionProjet.repositories.DatabasePapPlaceAffaireRepository;
import com.itma.gestionProjet.repositories.EntenteRepository;
import com.itma.gestionProjet.services.imp.ProjectService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EntenteService {

    private final EntenteRepository ententeRepository;
    private final DatabasePapAgricoleRepository papAgricoleRepository;
    private final DatabasePapPlaceAffaireRepository papPlaceAffaireRepository;
    private final ProjectService projectService; // ← Ajoute cette ligne

//public Entente createEntente(Long papId, TypePap papType, Long projectId) {
//    List<Entente> ententesExistantes = ententeRepository.findByPapIdAndPapType(papId, papType);
//    if (!ententesExistantes.isEmpty()) {
//        throw new RuntimeException("Une entente existe déjà pour ce PAP");
//    }
//    Project project = projectService.getProjectById(projectId);
//    return ententeRepository.save(
//            Entente.builder()
//                    .papId(papId)
//                    .papType(papType)
//                    .project(project)
//                    .build()
//    );
//}

    public Entente createEntente(Long papId, TypePap papType, Long projectId) {
        List<Entente> ententesExistantes = ententeRepository.findByPapIdAndPapType(papId, papType);
        if (!ententesExistantes.isEmpty()) {
            throw new RuntimeException("Une entente existe déjà pour ce PAP");
        }

        Project project = projectService.getProjectById(projectId);

        // Récupérer le codePap selon le type de PAP
        String codePap = getCodePapByType(papId, papType);

        return ententeRepository.save(
                Entente.builder()
                        .papId(papId)
                        .papType(papType)
                        .codePap(codePap) // ← Ajouter le codePap ici
                        .project(project)
                        .build()
        );
    }

    private String getCodePapByType(Long papId, TypePap papType) {
        return switch (papType) {
            case PAPAGRICOLE -> papAgricoleRepository.findById(papId)
                    .map(DatabasePapAgricole::getCodePap)
                    .orElseThrow(() -> new RuntimeException("PAP agricole non trouvé avec l'ID: " + papId));
            case PAPPLACEAFFAIRE -> papPlaceAffaireRepository.findById(papId)
                    .map(DatabasePapPlaceAffaire::getCodePap)
                    .orElseThrow(() -> new RuntimeException("PAP place d'affaire non trouvé avec l'ID: " + papId));
            default -> throw new RuntimeException("Type de PAP non supporté: " + papType);
        };
    }

    @Transactional
    public Entente synchroniserEntente(Long ententeId) {
        Entente entente = ententeRepository.findById(ententeId)
                .orElseThrow(() -> new RuntimeException("Entente non trouvée"));
        entente.setDateSynchronisation(LocalDateTime.now());
        entente.setStatut(StatutEntente.SYNCHRONISEE);
        return ententeRepository.save(entente);
    }

    @Transactional
    public Entente finaliserEntente(Long ententeId) {
        Entente entente = ententeRepository.findById(ententeId)
                .orElseThrow(() -> new RuntimeException("Entente non trouvée"));
        if (entente.getStatut() != StatutEntente.SYNCHRONISEE) {
            throw new RuntimeException("L'entente doit être synchronisée avant finalisation");
        }
        entente.setDateFinalisation(LocalDateTime.now());
        entente.setStatut(StatutEntente.FINALISEE);
        return ententeRepository.save(entente);
    }

    public EntenteDetailsDTO getEntenteDetails(Long ententeId) {
        Entente entente = ententeRepository.findById(ententeId)
                .orElseThrow(() -> new RuntimeException("Entente non trouvée"));

        EntenteDetailsDTO dto = new EntenteDetailsDTO();
        mapEntenteToDto(entente, dto);

        if (entente.getPapType() == TypePap.PAPAGRICOLE) {
            populateFromAgricole(entente.getPapId(), dto);
        } else if (entente.getPapType() == TypePap.PAPPLACEAFFAIRE) {
            populateFromPlaceAffaire(entente.getPapId(), dto);
        }

        return dto;
    }

    private void mapEntenteToDto(Entente entente, EntenteDetailsDTO dto) {
        dto.setEntenteId(entente.getId());
        dto.setStatut(entente.getStatut());
        dto.setEtatProcessus(entente.getEtatProcessus());
        dto.setOptionPaiement(entente.getOptionPaiement());
        dto.setDateCreation(entente.getDateCreation());
        dto.setDateSynchronisation(entente.getDateSynchronisation());
        dto.setDateFinalisation(entente.getDateFinalisation());

        dto.setTypeFormation(entente.getTypeFormation());
        dto.setFormateur(entente.getFormateur());
        dto.setCommentairesSuivi(entente.getCommentairesSuivi());
        dto.setResultatSuivi(entente.getResultatSuivi());

        // État des 6 étapes
        dto.setCompensationEtablie(entente.getCompensationEtablie());
        dto.setPapInformee(entente.getPapInformee());
        dto.setAccordPapObtenu(entente.getAccordPapObtenu());
        dto.setPaiementEffectue(entente.getPaiementEffectue());
        dto.setFormationDonnee(entente.getFormationDonnee());
        dto.setSuiviEffectue(entente.getSuiviEffectue());

        dto.setModeInformation(entente.getModeInformation());
        dto.setDetailsInformation(entente.getDetailsInformation());
        dto.setPreuveAccord(entente.getPreuveAccord());
        dto.setReferencePaiement(entente.getReferencePaiement());
        dto.setDateEtablissementCompensation(entente.getDateEtablissementCompensation());

        dto.setDateFormation(entente.getDateFormation());
        dto.setDateInformationPap(entente.getDateInformationPap());
        dto.setDateSuivi(entente.getDateSuivi());
        dto.setDateFinalisation(entente.getDateFinalisation());
        dto.setDateCreation(entente.getDateCreation());
        dto.setDateSynchronisation(entente.getDateSynchronisation());
        dto.setDateAccordPap(entente.getDateAccordPap());
        dto.setDatePaiement(entente.getDatePaiement());

    }

    private void populateFromAgricole(Long papId, EntenteDetailsDTO dto) {
        papAgricoleRepository.findById(papId).ifPresent(pap -> {
            // Informations d'identification de base
            dto.setPapId(pap.getId());
            dto.setPapType("PAPAGRICOLE");
            dto.setCodePap(pap.getCodePap());
            dto.setPrenom(pap.getPrenom());
            dto.setNom(pap.getNom());
            dto.setSexe(pap.getSexe());
            dto.setCategorie(pap.getType());
            dto.setCommune(pap.getCommune());
            dto.setDepartement(pap.getDepartement());

            // Informations de contact
            dto.setNumeroTelephone(pap.getNumeroTelephone());

            // Informations sur les pertes et compensation
            dto.setPerteTotale(pap.getPerteTotale());
            dto.setFraisDeplacement(pap.getFraisDeplacement());
            dto.setAppuiRelocalisation(pap.getAppuieRelocalisation());
            dto.setPerteRevenue(pap.getPerteRevenue());
            dto.setPerteBatiment(pap.getPerteBatiment());
            dto.setPerteCloture(pap.getPerteCloture());
            dto.setPerteTotaleArbre(pap.getPerteTotaleArbre());
            dto.setPerteArbreJeune(pap.getPerteArbreJeune());
            dto.setPerteArbreAdulte(pap.getPerteArbreAdulte());
            dto.setPerteEquipement(pap.getPerteEquipement());
            dto.setPerteTerre(pap.getPerteTerre());

            // Options et détails de paiement
            dto.setOptionPaiement(pap.getOptionPaiement());

            // Informations géographiques et parcellaire
            dto.setPointGeometriques(pap.getPointGeometriques());
            dto.setSuperficie(pap.getSuperficie());
            dto.setCodeParcelle(pap.getCodeParcelle());

            // Description des pertes
            dto.setDescription(pap.getDescription());
            dto.setEvaluationPerte(pap.getEvaluationPerte());
            dto.setCaracteristiqueParcelle(pap.getCaracteristiqueParcelle());

            // Statut et vulnérabilité
            dto.setStatutPap(pap.getStatutPap());
            dto.setVulnerabilite(pap.getVulnerabilite());

            // Informations démographiques pertinentes
            dto.setAge(pap.getAge());
            dto.setActivitePrincipale(pap.getActivitePrincipale());
        });
    }

private void populateFromPlaceAffaire(Long papId, EntenteDetailsDTO dto) {
    papPlaceAffaireRepository.findById(papId).ifPresent(pap -> {
        // Informations d'identification de base
        dto.setPapId(pap.getId());
        dto.setPapType("PAPPLACEAFFAIRE");
        dto.setCodePap(pap.getCodePap());
        dto.setPrenom(pap.getPrenom());
        dto.setNom(pap.getNom());
        dto.setSexe(pap.getSexe());
        dto.setCategorie(pap.getType());
        dto.setCommune(pap.getCommune());
        dto.setDepartement(pap.getDepartement());

        // Informations de contact
        dto.setNumeroTelephone(pap.getNumeroTelephone());

        // Informations sur les pertes et compensation
        dto.setPerteTotale(pap.getPerteTotale());
        dto.setFraisDeplacement(pap.getFraisDeplacement());
        dto.setAppuiRelocalisation(pap.getAppuieRelocalisation());
        dto.setPerteRevenue(pap.getPerteRevenue());
        dto.setPerteBatiment(pap.getPerteBatiment());
        dto.setPerteLoyer(pap.getPerteLoyer());
        dto.setPerteCloture(pap.getPerteCloture());
        dto.setPerteTotaleArbre(pap.getPerteTotaleArbre());
        dto.setPerteArbreJeune(pap.getPerteArbreJeune());
        dto.setPerteArbreAdulte(pap.getPerteArbreAdulte());
        dto.setPerteEquipement(pap.getPerteEquipement());

        // Options et détails de paiement
        dto.setOptionPaiement(pap.getOptionPaiement());

        // Informations géographiques
        dto.setPointGeometriques(pap.getPointGeometriques());

        // Description des pertes
        dto.setDescription(pap.getDescription());
        dto.setEvaluationPerte(pap.getEvaluationPerte());

        // Statut et vulnérabilité
        dto.setStatutPap(pap.getStatutPap());
        dto.setVulnerabilite(pap.getVulnerabilite());
    });
}

    public Page<EntenteDetailsDTO> getEntentesByProject(Long projectId, Pageable pageable) {
        Page<Entente> ententesPage = ententeRepository.findByProjectId(projectId, pageable);
        return ententesPage.map(entente -> getEntenteDetails(entente.getId()));
    }

    public Page<EntenteDetailsDTO> getAllEntentes(Pageable pageable) {
        Page<Entente> ententesPage = ententeRepository.findAll(pageable);
        return ententesPage.map(entente -> getEntenteDetails(entente.getId()));
    }
    public EntenteDetailsDTO getEntenteByCodePap(String codePap) {
        Entente entente = ententeRepository.findByCodePap(codePap)
                .orElseThrow(() -> new RuntimeException("Aucune entente trouvée avec le code PAP: " + codePap));

        return getEntenteDetails(entente.getId());
    }






//    @Autowired
//    private DatabasePapAgricoleRepository papAgricoleRepository;
//
//    @Autowired
//    private DatabasePapPlaceAffaireRepository papPlaceAffaireRepository;

    public List<Entente> createEntentesForProject(Long projectId) {
        Project project = projectService.getProjectById(projectId);
        List<Entente> nouvellesEntentes = new ArrayList<>();
        // Traiter les PAPs agricoles avec pagination
        processPapsAgricoles(projectId, project, nouvellesEntentes);
        // Traiter les PAPs place d'affaire avec pagination
        processPapsPlaceAffaire(projectId, project, nouvellesEntentes);

        return ententeRepository.saveAll(nouvellesEntentes);
    }

    private void processPapsAgricoles(Long projectId, Project project, List<Entente> nouvellesEntentes) {
        int page = 0;
        int size = 200;
        boolean hasMore = true;

        while (hasMore) {
            Page<DatabasePapAgricole> papPage = papAgricoleRepository.findByProjectId(
                    projectId,
                    PageRequest.of(page, size, Sort.by("id"))
            );

            for (DatabasePapAgricole pap : papPage.getContent()) {
                createEntenteIfNotExists(pap.getId(), TypePap.PAPAGRICOLE, project, pap.getCodePap(), nouvellesEntentes);
            }

            hasMore = papPage.hasNext();
            page++;
        }
    }

    private void processPapsPlaceAffaire(Long projectId, Project project, List<Entente> nouvellesEntentes) {
        int page = 0;
        int size = 200;
        boolean hasMore = true;

        while (hasMore) {
            Page<DatabasePapPlaceAffaire> papPage = papPlaceAffaireRepository.findByProjectId(
                    projectId,
                    PageRequest.of(page, size, Sort.by("id"))
            );
            for (DatabasePapPlaceAffaire pap : papPage.getContent()) {
                createEntenteIfNotExists(pap.getId(), TypePap.PAPPLACEAFFAIRE, project, pap.getCodePap(), nouvellesEntentes);
            }
            hasMore = papPage.hasNext();
            page++;
        }
    }

    private void createEntenteIfNotExists(Long papId, TypePap papType, Project project, String codePap, List<Entente> nouvellesEntentes) {
        List<Entente> ententesExistantes = ententeRepository.findByPapIdAndPapType(papId, papType);

        if (ententesExistantes.isEmpty()) {
            Entente entente = Entente.builder()
                    .papId(papId)
                    .papType(papType)
                    .codePap(codePap)
                    .project(project)
                    .build();

            nouvellesEntentes.add(entente);
        }
    }








}
