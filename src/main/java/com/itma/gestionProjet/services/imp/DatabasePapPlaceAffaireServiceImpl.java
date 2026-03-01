package com.itma.gestionProjet.services.imp;
import com.itma.gestionProjet.dtos.CategoryStats;
import com.itma.gestionProjet.dtos.DatabasePapPlaceAffaireRequestDTO;
import com.itma.gestionProjet.dtos.DatabasePapPlaceAffaireResponseDTO;
import com.itma.gestionProjet.entities.DatabasePapPlaceAffaire;
import com.itma.gestionProjet.entities.Project;
import com.itma.gestionProjet.helpers.StatsHelper;
import com.itma.gestionProjet.repositories.DatabasePapPlaceAffaireRepository;
import com.itma.gestionProjet.repositories.ProjectRepository;
import com.itma.gestionProjet.services.DatabasePapPlaceAffaireService;
import jakarta.persistence.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DatabasePapPlaceAffaireServiceImpl implements DatabasePapPlaceAffaireService {

    @Autowired
    private DatabasePapPlaceAffaireRepository repository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ModelMapper modelMapper;


    @Autowired
    private StatsHelper statsHelper;

    @Override
    public void createDatabasePapPlaceAffaire(List<DatabasePapPlaceAffaireRequestDTO> requestDTOs) {
        modelMapper.getConfiguration().setAmbiguityIgnored(true);

        List<DatabasePapPlaceAffaire> entities = requestDTOs.stream().map(dto -> {
            DatabasePapPlaceAffaire entity = convertDTOToEntity(dto);

            // Valeurs par défaut
            if (entity.getType() == null || entity.getType().isEmpty()) {
                entity.setType("PAPPLACEAFFAIRE");
            }
            if (entity.getStatutPap() == null || entity.getStatutPap().isEmpty()) {
                entity.setStatutPap("recense");
            }

            // Associer le projet
            if (dto.getProjectId() != null) {
                Project project = projectRepository.findById(dto.getProjectId())
                        .orElseThrow(() -> new RuntimeException("Project not found with ID: " + dto.getProjectId()));
                entity.setProject(project);
            }

            // Calculer la vulnérabilité et la perte totale
            entity.setVulnerabilite(determinerVulnerabilite(entity));
            if (entity.getPerteTotale() == null) {
                entity.setPerteTotale(calculatePerteTotale(entity));
            }

            return entity;
        }).collect(Collectors.toList());

        repository.saveAll(entities);
    }

    /**
     * Convertit le DTO en Entity avec gestion sécurisée des conversions String vers Long/Double
     */
    private DatabasePapPlaceAffaire convertDTOToEntity(DatabasePapPlaceAffaireRequestDTO dto) {
        DatabasePapPlaceAffaire entity = new DatabasePapPlaceAffaire();

        // Copie des champs String normaux
        entity.setCodePap(dto.getCodePap());
        entity.setCaracteristiquePlaceAffaire(dto.getCaracteristiquePlaceAffaire());
        entity.setNom(dto.getNom());
        entity.setPrenom(dto.getPrenom());
        entity.setCodePlaceAffaire(dto.getCodePlaceAffaire());
        entity.setCommune(dto.getCommune());
        entity.setDepartement(dto.getDepartement());
        entity.setNombrePlaceAffaire(dto.getNombrePlaceAffaire());
        entity.setSurnom(dto.getSurnom());
        entity.setSexe(dto.getSexe());
        entity.setExistePni(dto.getExistePni());
        entity.setTypePni(dto.getTypePni());
        entity.setNumeroPni(dto.getNumeroPni());
        entity.setNumeroTelephone(dto.getNumeroTelephone());
        entity.setPhotoPap(dto.getPhotoPap());
        entity.setPhotoPlaceAffaire(dto.getPhotoPlaceAffaire());
        entity.setPointGeometriques(dto.getPointGeometriques());
        entity.setNationalite(dto.getNationalite());
        entity.setEthnie(dto.getEthnie());
        entity.setLangueParlee(dto.getLangueParlee());
        entity.setSituationMatrimoniale(dto.getSituationMatrimoniale());
        entity.setNiveauEtude(dto.getNiveauEtude());
        entity.setReligion(dto.getReligion());
        entity.setActivitePrincipale(dto.getActivitePrincipale());
        entity.setMembreFoyer(dto.getMembreFoyer());
        entity.setMembreFoyerHandicape(dto.getMembreFoyerHandicape());
        entity.setInformationsEtendues(dto.getInformationsEtendues());
        entity.setEvaluationPerte(dto.getEvaluationPerte());
        entity.setStatutPap(dto.getStatutPap());
        entity.setVulnerabilite(dto.getVulnerabilite());
        entity.setPj1(dto.getPj1());
        entity.setPj2(dto.getPj2());
        entity.setPj3(dto.getPj3());
        entity.setPj4(dto.getPj4());
        entity.setPj5(dto.getPj5());
        entity.setInfosComplemenataires(dto.getInfosComplemenataires());
        entity.setDateNaissance(dto.getDateNaissance());
        entity.setRoleDansFoyer(dto.getRoleDansFoyer());
        entity.setOptionPaiement(dto.getOptionPaiement());
        entity.setTypeHandicape(dto.getTypeHandicape());
        entity.setDescription(dto.getDescription());
        entity.setVulne(dto.getVulne());
        entity.setSuptotale(dto.getSuptotale());
        entity.setPourcaffecte(dto.getPourcaffecte());

        // Conversion sécurisée de age (String vers Long)
        entity.setAge(parseLong(dto.getAge()));

        // Conversion sécurisée des champs Double
        entity.setPerteArbreJeune(parseDouble(dto.getPerteArbreJeune()));
        entity.setPerteArbreAdulte(parseDouble(dto.getPerteArbreAdulte()));
        entity.setPerteEquipement(parseDouble(dto.getPerteEquipement()));
        entity.setPerteRevenue(parseDouble(dto.getPerteRevenue()));
        entity.setAppuieRelocalisation(parseDouble(dto.getAppuieRelocalisation()));
        entity.setPerteTotale(parseDouble(dto.getPerteTotale()));
        entity.setFraisDeplacement(parseDouble(dto.getFraisDeplacement()));
        entity.setPerteBatiment(parseDouble(dto.getPerteBatiment()));
        entity.setPerteLoyer(parseDouble(dto.getPerteLoyer()));
        entity.setPerteCloture(parseDouble(dto.getPerteCloture()));
        entity.setPerteTotaleArbre(parseDouble(dto.getPerteTotaleArbre()));

        return entity;
    }

    /**
     * Convertit une String en Long de manière sécurisée
     * Retourne null si la valeur est null, vide, "à compléter" ou invalide
     */
    private Long parseLong(String value) {
        if (value == null || value.trim().isEmpty() ||
                value.equalsIgnoreCase("à compléter") ||
                value.equalsIgnoreCase("a completer")) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            System.err.println("Erreur de conversion Long pour la valeur: " + value);
            return null;
        }
    }

    /**
     * Convertit une String en Double de manière sécurisée
     * Retourne null si la valeur est null, vide, "à compléter" ou invalide
     */
    private Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty() ||
                value.equalsIgnoreCase("à compléter") ||
                value.equalsIgnoreCase("a completer")) {
            return null;
        }
        try {
            // Remplace la virgule par un point pour gérer le format français
            String normalizedValue = value.trim().replace(',', '.');
            return Double.parseDouble(normalizedValue);
        } catch (NumberFormatException e) {
            System.err.println("Erreur de conversion Double pour la valeur: " + value);
            return null;
        }
    }

    private Double calculatePerteTotale(DatabasePapPlaceAffaire entity) {
        Double perteArbreJeune = zeroIfNull(entity.getPerteArbreJeune());
        Double perteArbreAdulte = zeroIfNull(entity.getPerteArbreAdulte());
        Double perteEquipement = zeroIfNull(entity.getPerteEquipement());
        Double perteRevenue = zeroIfNull(entity.getPerteRevenue());
        Double perteBatiment = zeroIfNull(entity.getPerteBatiment());
        Double perteLoyer = zeroIfNull(entity.getPerteLoyer());
        Double perteCloture = zeroIfNull(entity.getPerteCloture());
        Double fraisDeplacement = zeroIfNull(entity.getFraisDeplacement());
        Double appuieRelocalisation = zeroIfNull(entity.getAppuieRelocalisation());
        return perteArbreJeune + perteArbreAdulte + perteEquipement + perteRevenue
                + perteBatiment + perteLoyer + perteCloture
                + fraisDeplacement - appuieRelocalisation;
    }

    private Double zeroIfNull(Double value) {
        return value != null ? value : 0.0;
    }

    private String determinerVulnerabilite(DatabasePapPlaceAffaire pap) {
        List<String> vulnerabilites = new ArrayList<>();

        // 1. Situation matrimoniale précaire
        if (pap.getSituationMatrimoniale() != null &&
                Arrays.asList("veuf", "veuve", "divorcé", "divorcée", "célibataire")
                        .contains(pap.getSituationMatrimoniale().toLowerCase())) {
            vulnerabilites.add("Situation matrimoniale précaire");
        }

        // 2. Ménage avec personne handicapée
        if (pap.getMembreFoyerHandicape() != null &&
                !pap.getMembreFoyerHandicape().isEmpty() &&
                !"non".equalsIgnoreCase(pap.getMembreFoyerHandicape())) {
            vulnerabilites.add("Ménage avec personne handicapée");
        }

        // 3. Mineur chef de ménage
        if (pap.getDateNaissance() != null || pap.getAge() != null) {
            int age;
            // Si l'âge est directement disponible, on l'utilise
            if (pap.getAge() != null) {
                age = Math.toIntExact(pap.getAge());
            }
            // Sinon on le calcule à partir de la date de naissance
            else {
                age = calculerAge(pap.getDateNaissance());
            }
            // Vérifications des vulnérabilités
            if (age < 18 && "chef de ménage".equalsIgnoreCase(pap.getRoleDansFoyer())) {
                vulnerabilites.add("Mineur chef de ménage");
            }
            // 4. Personne âgée sans soutien
            if (age >= 65 && (pap.getMembreFoyer() == null || pap.getMembreFoyer().isEmpty() || "seul".equalsIgnoreCase(pap.getMembreFoyer()))) {
                vulnerabilites.add("Personne âgée sans soutien");
            }
        }
        // 5. Ménage avec plusieurs personnes à charge
        if (pap.getMembreFoyer() != null && !pap.getMembreFoyer().isEmpty()) {
            try {
                int nbDependants = Integer.parseInt(pap.getMembreFoyer());
                // Supposons qu'une personne travaille si elle a une activité principale
                boolean aActivite = pap.getActivitePrincipale() != null && !pap.getActivitePrincipale().isEmpty();
                if (nbDependants >= 4 && aActivite) {
                    vulnerabilites.add("Ménage nombreux");
                }
            } catch (NumberFormatException e) {
                // Gérer le cas où membreFoyer n'est pas un nombre
            }
        }
        // 6. Analphabète
        if (pap.getNiveauEtude() != null &&
                (pap.getNiveauEtude().toLowerCase().contains("ne sait pas lire") ||
                        "analphabète".equalsIgnoreCase(pap.getNiveauEtude()))) {
            vulnerabilites.add("Analphabétisme");
        }
        return vulnerabilites.isEmpty() ? "Non vulnérable" : String.join(", ", vulnerabilites);
    }

    private int calculerAge(LocalDate dateNaissance) {
        return Period.between(dateNaissance, LocalDate.now()).getYears();
    }

    @Override
    public Map<String, Object> getVulnerabilityStats(Long projectId) {
        var pageRequest = PageRequest.of(0, 10000000);
        Page<DatabasePapPlaceAffaire> pagedPaps = repository.findByProjectId(projectId, pageRequest);
        List<DatabasePapPlaceAffaire> projectPaps = pagedPaps.getContent();
        // 1. Initialisation des structures
        // Vulnérabilités globales
        Map<String, Long> globalVulnerabilities = initVulnerabilityMap();

        // Sexes globaux
        Map<String, Long> globalGenders = initGenderMap(projectPaps.size());

        // Vulnérabilités par sexe (analyse croisée)
        Map<String, Map<String, Long>> vulnerabilitiesByGender = initCrossAnalysisMap();

        // Terminologie des sexes
        Set<String> maleTerms = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        maleTerms.addAll(Arrays.asList("m", "masculin", "homme", "male", "garçon", "h", "mâle"));

        Set<String> femaleTerms = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        femaleTerms.addAll(Arrays.asList("f", "féminin", "femme", "femelle", "female", "fille", "feminin"));

        // 2. Traitement des données
        for (DatabasePapPlaceAffaire pap : projectPaps) {
            String sexe = pap.getSexe();
            String normalizedSexe = sexe != null ? sexe.trim().toLowerCase() : "";

            // Détection du sexe
            String genderKey = getGenderKey(normalizedSexe, maleTerms, femaleTerms);
            globalGenders.put(genderKey, globalGenders.get(genderKey) + 1);

            // Traitement vulnérabilité
            String vuln = pap.getVulnerabilite();
            boolean isNonVulnerable = vuln == null || vuln.equals("Non vulnérable");

            if (isNonVulnerable) {
                globalVulnerabilities.put("Non vulnérable", globalVulnerabilities.get("Non vulnérable") + 1);
                vulnerabilitiesByGender.get("Non vulnérable").put(genderKey,
                        vulnerabilitiesByGender.get("Non vulnérable").get(genderKey) + 1);
                continue;
            }

            // Analyse par catégorie
            for (String category : globalVulnerabilities.keySet()) {
                if (vuln.contains(category)) {
                    globalVulnerabilities.put(category, globalVulnerabilities.get(category) + 1);
                    vulnerabilitiesByGender.get(category).put(genderKey,
                            vulnerabilitiesByGender.get(category).get(genderKey) + 1);
                }
            }
        }

        // 3. Construction du résultat
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("Vulnerabilites_globales", globalVulnerabilities);
        result.put("Sexes_globaux", globalGenders);
        result.put("Vulnerabilites_par_sexe", vulnerabilitiesByGender);

        return result;
    }

    // Méthodes utilitaires
    private Map<String, Long> initVulnerabilityMap() {
        Map<String, Long> map = new LinkedHashMap<>();
        map.put("Situation matrimoniale précaire", 0L);
        map.put("Ménage avec personne handicapée", 0L);
        map.put("Mineur chef de ménage", 0L);
        map.put("Personne âgée sans soutien", 0L);
        map.put("Ménage nombreux", 0L);
        map.put("Analphabétisme", 0L);
        map.put("Non vulnérable", 0L);
        return map;
    }

    private Map<String, Long> initGenderMap(long total) {
        Map<String, Long> map = new LinkedHashMap<>();
        map.put("Total", total);
        map.put("Hommes", 0L);
        map.put("Femmes", 0L);
        map.put("Autre", 0L);
        return map;
    }

    private Map<String, Map<String, Long>> initCrossAnalysisMap() {
        Map<String, Map<String, Long>> map = new LinkedHashMap<>();
        for (String category : initVulnerabilityMap().keySet()) {
            Map<String, Long> genderMap = new LinkedHashMap<>();
            genderMap.put("Hommes", 0L);
            genderMap.put("Femmes", 0L);
            genderMap.put("Autre", 0L);
            map.put(category, genderMap);
        }
        return map;
    }

    private String getGenderKey(String normalizedSexe, Set<String> maleTerms, Set<String> femaleTerms) {
        if (femaleTerms.contains(normalizedSexe)) return "Femmes";
        if (maleTerms.contains(normalizedSexe)) return "Hommes";
        return "Autre";
    }

    @Override
    public List<DatabasePapPlaceAffaireResponseDTO> getAllDatabasePapPlaceAffaire(int page, int size) {
        var pageRequest = PageRequest.of(page, size);
        var pageResult = repository.findAll(pageRequest);
        List<DatabasePapPlaceAffaireResponseDTO> data = pageResult.getContent().stream()
                .map(this::convertEntityToResponseDTO)
                .collect(Collectors.toList());
        return data;
    }

    @Override
    public List<DatabasePapPlaceAffaireResponseDTO> getDatabasePapPlaceAffaireByProjectId(Long projectId, int page, int size) {
        var pageRequest = PageRequest.of(page, size);
        var pageResult = repository.findByProjectId(projectId, pageRequest);
        List<DatabasePapPlaceAffaireResponseDTO> data = pageResult.getContent().stream()
                .map(this::convertEntityToResponseDTO)
                .collect(Collectors.toList());
        return data;
    }

    @Override
    public DatabasePapPlaceAffaireResponseDTO getDatabasePapPlaceAffaireById(Long id) {
        DatabasePapPlaceAffaire entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Entity not found"));
        return modelMapper.map(entity, DatabasePapPlaceAffaireResponseDTO.class);
    }

    @Override
    public void updateDatabasePapPlaceAffaire(Long id, DatabasePapPlaceAffaireRequestDTO requestDTO) {
        DatabasePapPlaceAffaire entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entity not found with ID: " + id));

        // Conversion manuelle du DTO vers l'entité
        DatabasePapPlaceAffaire updatedFields = convertDTOToEntity(requestDTO);

        // Copie des champs non-null
        if (updatedFields.getCodePap() != null) entity.setCodePap(updatedFields.getCodePap());
        if (updatedFields.getCaracteristiquePlaceAffaire() != null)
            entity.setCaracteristiquePlaceAffaire(updatedFields.getCaracteristiquePlaceAffaire());
        if (updatedFields.getNom() != null) entity.setNom(updatedFields.getNom());
        if (updatedFields.getPrenom() != null) entity.setPrenom(updatedFields.getPrenom());
        if (updatedFields.getCodePlaceAffaire() != null)
            entity.setCodePlaceAffaire(updatedFields.getCodePlaceAffaire());
        if (updatedFields.getCommune() != null) entity.setCommune(updatedFields.getCommune());
        if (updatedFields.getDepartement() != null) entity.setDepartement(updatedFields.getDepartement());
        if (updatedFields.getNombrePlaceAffaire() != null)
            entity.setNombrePlaceAffaire(updatedFields.getNombrePlaceAffaire());
        if (updatedFields.getSurnom() != null) entity.setSurnom(updatedFields.getSurnom());
        if (updatedFields.getSexe() != null) entity.setSexe(updatedFields.getSexe());
        if (updatedFields.getExistePni() != null) entity.setExistePni(updatedFields.getExistePni());
        if (updatedFields.getTypePni() != null) entity.setTypePni(updatedFields.getTypePni());
        if (updatedFields.getNumeroPni() != null) entity.setNumeroPni(updatedFields.getNumeroPni());
        if (updatedFields.getNumeroTelephone() != null) entity.setNumeroTelephone(updatedFields.getNumeroTelephone());
        if (updatedFields.getPhotoPap() != null) entity.setPhotoPap(updatedFields.getPhotoPap());
        if (updatedFields.getPhotoPlaceAffaire() != null)
            entity.setPhotoPlaceAffaire(updatedFields.getPhotoPlaceAffaire());
        if (updatedFields.getPointGeometriques() != null)
            entity.setPointGeometriques(updatedFields.getPointGeometriques());
        if (updatedFields.getNationalite() != null) entity.setNationalite(updatedFields.getNationalite());
        if (updatedFields.getEthnie() != null) entity.setEthnie(updatedFields.getEthnie());
        if (updatedFields.getLangueParlee() != null) entity.setLangueParlee(updatedFields.getLangueParlee());
        if (updatedFields.getSituationMatrimoniale() != null)
            entity.setSituationMatrimoniale(updatedFields.getSituationMatrimoniale());
        if (updatedFields.getNiveauEtude() != null) entity.setNiveauEtude(updatedFields.getNiveauEtude());
        if (updatedFields.getReligion() != null) entity.setReligion(updatedFields.getReligion());
        if (updatedFields.getActivitePrincipale() != null)
            entity.setActivitePrincipale(updatedFields.getActivitePrincipale());
        if (updatedFields.getMembreFoyer() != null) entity.setMembreFoyer(updatedFields.getMembreFoyer());
        if (updatedFields.getMembreFoyerHandicape() != null)
            entity.setMembreFoyerHandicape(updatedFields.getMembreFoyerHandicape());
        if (updatedFields.getInformationsEtendues() != null)
            entity.setInformationsEtendues(updatedFields.getInformationsEtendues());
        if (updatedFields.getEvaluationPerte() != null) entity.setEvaluationPerte(updatedFields.getEvaluationPerte());
        if (updatedFields.getStatutPap() != null) entity.setStatutPap(updatedFields.getStatutPap());
        if (updatedFields.getPj1() != null) entity.setPj1(updatedFields.getPj1());
        if (updatedFields.getPj2() != null) entity.setPj2(updatedFields.getPj2());
        if (updatedFields.getPj3() != null) entity.setPj3(updatedFields.getPj3());
        if (updatedFields.getPj4() != null) entity.setPj4(updatedFields.getPj4());
        if (updatedFields.getPj5() != null) entity.setPj5(updatedFields.getPj5());
        if (updatedFields.getInfosComplemenataires() != null)
            entity.setInfosComplemenataires(updatedFields.getInfosComplemenataires());
        if (updatedFields.getDateNaissance() != null) entity.setDateNaissance(updatedFields.getDateNaissance());
        if (updatedFields.getRoleDansFoyer() != null) entity.setRoleDansFoyer(updatedFields.getRoleDansFoyer());
        if (updatedFields.getOptionPaiement() != null) entity.setOptionPaiement(updatedFields.getOptionPaiement());
        if (updatedFields.getTypeHandicape() != null) entity.setTypeHandicape(updatedFields.getTypeHandicape());
        if (updatedFields.getDescription() != null) entity.setDescription(updatedFields.getDescription());
        if (updatedFields.getVulne() != null) entity.setVulne(updatedFields.getVulne());
        if (updatedFields.getSuptotale() != null) entity.setSuptotale(updatedFields.getSuptotale());
        if (updatedFields.getPourcaffecte() != null) entity.setPourcaffecte(updatedFields.getPourcaffecte());

        // Mise à jour des champs numériques
        if (updatedFields.getAge() != null) entity.setAge(updatedFields.getAge());
        if (updatedFields.getPerteArbreJeune() != null) entity.setPerteArbreJeune(updatedFields.getPerteArbreJeune());
        if (updatedFields.getPerteArbreAdulte() != null)
            entity.setPerteArbreAdulte(updatedFields.getPerteArbreAdulte());
        if (updatedFields.getPerteEquipement() != null) entity.setPerteEquipement(updatedFields.getPerteEquipement());
        if (updatedFields.getPerteRevenue() != null) entity.setPerteRevenue(updatedFields.getPerteRevenue());
        if (updatedFields.getAppuieRelocalisation() != null)
            entity.setAppuieRelocalisation(updatedFields.getAppuieRelocalisation());
        if (updatedFields.getFraisDeplacement() != null)
            entity.setFraisDeplacement(updatedFields.getFraisDeplacement());
        if (updatedFields.getPerteBatiment() != null) entity.setPerteBatiment(updatedFields.getPerteBatiment());
        if (updatedFields.getPerteLoyer() != null) entity.setPerteLoyer(updatedFields.getPerteLoyer());
        if (updatedFields.getPerteCloture() != null) entity.setPerteCloture(updatedFields.getPerteCloture());
        if (updatedFields.getPerteTotaleArbre() != null)
            entity.setPerteTotaleArbre(updatedFields.getPerteTotaleArbre());

        // Valeurs par défaut
        if (entity.getType() == null || entity.getType().isEmpty()) {
            entity.setType("PAPPLACEAFFAIRE");
        }
        if (entity.getStatutPap() == null || entity.getStatutPap().isEmpty()) {
            entity.setStatutPap("recense");
        }

        // Mise à jour du projet si nécessaire
        if (requestDTO.getProjectId() != null) {
            Project project = projectRepository.findById(requestDTO.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found with ID: " + requestDTO.getProjectId()));
            entity.setProject(project);
        }

        // Calcul des champs dérivés
        entity.setVulnerabilite(determinerVulnerabilite(entity));
        if (entity.getPerteTotale() == null) {
            entity.setPerteTotale(calculatePerteTotale(entity));
        }

        repository.save(entity);
    }

    public long getTotalCount() {
        return repository.count();
    }

    @Override
    public void deleteDatabasePapPlaceAffaire(Long id) {
        DatabasePapPlaceAffaire entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Entity not found"));
        repository.delete(entity);
    }

    private DatabasePapPlaceAffaireResponseDTO convertEntityToResponseDTO(DatabasePapPlaceAffaire entity) {
        DatabasePapPlaceAffaireResponseDTO dto = modelMapper.map(entity, DatabasePapPlaceAffaireResponseDTO.class);
        if (entity.getProject() != null) {
            dto.setProjectId((long) entity.getProject().getId());
        }
        return dto;
    }

    public DatabasePapPlaceAffaire getByCodePap(String codePap) {
        return repository.findByCodePap(codePap)
                .orElseThrow(() -> new EntityNotFoundException("DatabasePapPlaceAffaire avec codePap " + codePap + " introuvable"));
    }

    @Override
    public long getTotalCountByProjectId(Long projectId) {
        return repository.countByProjectId(projectId);
    }

    @Override
    public List<DatabasePapPlaceAffaireResponseDTO> searchGlobalDatabasePapPlaceAffaire(String searchTerm, Optional<Long> projectId, int page, int size) {
        Pageable pageRequest = PageRequest.of(page, size);
        Long projectIdValue = projectId.orElse(null);
        Page<DatabasePapPlaceAffaire> pageResult = repository.searchGlobal(searchTerm, projectIdValue, pageRequest);
        List<DatabasePapPlaceAffaireResponseDTO> data = pageResult.getContent().stream()
                .map(this::convertEntityToResponseDTO)
                .collect(Collectors.toList());
        return data;
    }

    @Override
    public long getTotalCountForSearch(String searchTerm, Optional<Long> projectId) {
        Long projectIdValue = projectId.orElse(null);
        return repository.countBySearchTermAndProjectId(searchTerm, projectIdValue);
    }

    @Override
    public Double calculateTotalPerte(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("L'ID du projet ne peut pas être null");
        }

        long totalItems = repository.countByProjectId(projectId);
        if (totalItems == 0) return 0.0;

        int pageSize = 1000;
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        double total = 0.0;

        for (int i = 0; i < totalPages; i++) {
            Page<DatabasePapPlaceAffaire> page = repository.findByProjectId(
                    projectId,
                    PageRequest.of(i, pageSize)
            );
            total += page.getContent().stream()
                    .filter(Objects::nonNull)
                    .mapToDouble(p -> p.getPerteTotale() != null ? p.getPerteTotale() : 0.0)
                    .sum();
        }

        return total;
    }

    // Vider tous les PAPs d'un projet
    public void deleteAllByProjectId(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID cannot be null");
        }
        repository.deleteAllByProjectId(projectId);
    }

    // Supprimer par une liste d'IDs
    public void deleteAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("IDs list cannot be null or empty");
        }
        repository.deleteAllByIdIn(ids);
    }

    @Override
    public CategoryStats getCategoryStats(Long projectId) {
        // 1. Récupérer les PAPs du projet
        List<DatabasePapPlaceAffaire> paps = repository
                .findByProjectId(projectId, PageRequest.of(0, 10_000_000))
                .getContent();

        // 2. Calculer la perte totale
        double totalPerte = paps.stream()
                .mapToDouble(p -> p.getPerteTotale() != null ? p.getPerteTotale() : 0.0)
                .sum();

        // 3. Déléguer le calcul des stats au StatsHelper
        return statsHelper.buildCategoryStats(
                paps,
                DatabasePapPlaceAffaire::getSexe,
                DatabasePapPlaceAffaire::getVulnerabilite,
                totalPerte
        );
    }

    // Méthode utilitaire pour vérifier l'existence des IDs
    public boolean existAllByIds(List<Long> ids) {
        return repository.countByIdIn(ids) == ids.size();
    }
}

