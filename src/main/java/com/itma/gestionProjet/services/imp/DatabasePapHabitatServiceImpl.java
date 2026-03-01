//package com.itma.gestionProjet.services.imp;
//
//import com.itma.gestionProjet.dtos.DatabasePapHabitatRequestDTO;
//import com.itma.gestionProjet.dtos.DatabasePapHabitatResponseDTO;
//import com.itma.gestionProjet.entities.DatabasePapHabitat;
//import com.itma.gestionProjet.entities.Project;
//import com.itma.gestionProjet.repositories.DatabasePapHabitatRepository;
//import com.itma.gestionProjet.repositories.ProjectRepository;
//import com.itma.gestionProjet.services.DatabasePapHabitatService;
//import jakarta.persistence.EntityNotFoundException;
//import org.modelmapper.ModelMapper;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDate;
//import java.time.Period;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//public class DatabasePapHabitatServiceImpl implements DatabasePapHabitatService {
//
//    @Autowired
//    private DatabasePapHabitatRepository repository;
//
//    @Autowired
//    private ProjectRepository projectRepository;
//
//    @Autowired
//    private ModelMapper modelMapper;
//
//    @Override
//    public void create(List<DatabasePapHabitatRequestDTO> requestDTOs) {
//        modelMapper.getConfiguration().setAmbiguityIgnored(true);
//        modelMapper.typeMap(DatabasePapHabitatRequestDTO.class, DatabasePapHabitat.class)
//                .addMappings(mapper -> mapper.skip(DatabasePapHabitat::setId));
//
//        List<DatabasePapHabitat> entities = requestDTOs.stream().map(dto -> {
//            DatabasePapHabitat entity = modelMapper.map(dto, DatabasePapHabitat.class);
//
//            if (entity.getType() == null || entity.getType().isEmpty()) entity.setType("PAPHABITAT");
//
//            if (dto.getProjectId() != null) {
//                Project project = projectRepository.findById(dto.getProjectId())
//                        .orElseThrow(() -> new RuntimeException("Project not found with ID: " + dto.getProjectId()));
//                entity.setProject(project);
//            }
//
//            entity.setVulnerabilite(determinerVulnerabilite(entity));
//            if (entity.getPerteTotale() == null) entity.setPerteTotale(calculatePerteTotale(entity));
//
//            return entity;
//        }).collect(Collectors.toList());
//
//        repository.saveAll(entities);
//    }
//
//    private Double calculatePerteTotale(DatabasePapHabitat entity) {
//        // Initialiser toutes les valeurs à 0 si elles sont null
//        Double perteTerre = zeroIfNull(entity.getPerteTerre());
//        Double perteArbreJeune = zeroIfNull(entity.getPerteArbreJeune());
//        Double perteArbreAdulte = zeroIfNull(entity.getPerteArbreAdulte());
//        Double perteEquipement = zeroIfNull(entity.getPerteEquipement());
//        Double perteBatiment = zeroIfNull(entity.getPerteBatiment());
//
//        // Calcul spécifique pour l'agricole
//        return perteTerre + perteArbreJeune + perteArbreAdulte
//                + perteEquipement + perteBatiment;
//    }
//
//    private Double zeroIfNull(Double v) { return v != null ? v : 0.0; }
//
//    private String determinerVulnerabilite(DatabasePapHabitat pap) {
//        List<String> vulnerabilites = new ArrayList<>();
//
//        if (pap.getSituationMatrimoniale() != null &&
//                Arrays.asList("veuf", "veuve", "divorcé", "divorcée", "célibataire")
//                        .contains(pap.getSituationMatrimoniale().toLowerCase())) {
//            vulnerabilites.add("Situation matrimoniale précaire");
//        }
//
//        if (pap.getMembreFoyerHandicape() != null && !pap.getMembreFoyerHandicape().isEmpty() &&
//                !"non".equalsIgnoreCase(pap.getMembreFoyerHandicape())) {
//            vulnerabilites.add("Ménage avec personne handicapée");
//        }
//
//        int age = -1;
//        if (pap.getAge() != null) age = Math.toIntExact(pap.getAge());
//        else if (pap.getDateNaissance() != null) age = calculerAge(pap.getDateNaissance());
//
//        if (age >= 0) {
//            if (age < 18 && "chef de ménage".equalsIgnoreCase(pap.getRoleDansFoyer())) {
//                vulnerabilites.add("Mineur chef de ménage");
//            }
//            if (age >= 65 && (pap.getMembreFoyer() == null || pap.getMembreFoyer().isEmpty() || "seul".equalsIgnoreCase(pap.getMembreFoyer()))) {
//                vulnerabilites.add("Personne âgée sans soutien");
//            }
//        }
//
//        if (pap.getMembreFoyer() != null && !pap.getMembreFoyer().isEmpty()) {
//            try {
//                int nb = Integer.parseInt(pap.getMembreFoyer());
//                boolean aActivite = pap.getActivitePrincipale() != null && !pap.getActivitePrincipale().isEmpty();
//                if (nb >= 4 && aActivite) vulnerabilites.add("Ménage nombreux");
//            } catch (NumberFormatException ignored) {}
//        }
//
//        if (pap.getNiveauEtude() != null && (pap.getNiveauEtude().toLowerCase().contains("ne sait pas lire") || "analphabète".equalsIgnoreCase(pap.getNiveauEtude()))) {
//            vulnerabilites.add("Analphabétisme");
//        }
//
//        return vulnerabilites.isEmpty() ? "Non vulnérable" : String.join(", ", vulnerabilites);
//    }
//
//    private int calculerAge(LocalDate dn) { return Period.between(dn, LocalDate.now()).getYears(); }
//
//    @Override
//    public Map<String, Object> getVulnerabilityStats(Long projectId) {
//        var pageRequest = PageRequest.of(0, 10_000_000);
//        Page<DatabasePapHabitat> paged = repository.findByProjectId(projectId, pageRequest);
//        List<DatabasePapHabitat> paps = paged.getContent();
//
//        Map<String, Long> globalVulnerabilities = initVulnerabilityMap();
//        Map<String, Long> globalGenders = initGenderMap(paps.size());
//        Map<String, Map<String, Long>> vulnerabilitiesByGender = initCrossAnalysisMap();
//
//        Set<String> maleTerms = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
//        maleTerms.addAll(Arrays.asList("m","masculin","homme","male","garçon","h","mâle"));
//        Set<String> femaleTerms = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
//        femaleTerms.addAll(Arrays.asList("f","féminin","femme","femelle","female","fille","feminin"));
//
//        for (DatabasePapHabitat pap : paps) {
//            String sexe = pap.getSexe();
//            String normalizedSexe = sexe != null ? sexe.trim().toLowerCase() : "";
//            String genderKey = getGenderKey(normalizedSexe, maleTerms, femaleTerms);
//            globalGenders.put(genderKey, globalGenders.get(genderKey) + 1);
//
//            String vuln = pap.getVulnerabilite();
//            boolean isNon = vuln == null || vuln.equals("Non vulnérable");
//            if (isNon) {
//                globalVulnerabilities.put("Non vulnérable", globalVulnerabilities.get("Non vulnérable") + 1);
//                vulnerabilitiesByGender.get("Non vulnérable").put(genderKey, vulnerabilitiesByGender.get("Non vulnérable").get(genderKey) + 1);
//                continue;
//            }
//            for (String category : globalVulnerabilities.keySet()) {
//                if (vuln.contains(category)) {
//                    globalVulnerabilities.put(category, globalVulnerabilities.get(category) + 1);
//                    vulnerabilitiesByGender.get(category).put(genderKey, vulnerabilitiesByGender.get(category).get(genderKey) + 1);
//                }
//            }
//        }
//
//        Map<String, Object> result = new LinkedHashMap<>();
//        result.put("Vulnerabilites_globales", globalVulnerabilities);
//        result.put("Sexes_globaux", globalGenders);
//        result.put("Vulnerabilites_par_sexe", vulnerabilitiesByGender);
//        return result;
//    }
//
//    private Map<String, Long> initVulnerabilityMap() {
//        Map<String, Long> map = new LinkedHashMap<>();
//        map.put("Situation matrimoniale précaire", 0L);
//        map.put("Ménage avec personne handicapée", 0L);
//        map.put("Mineur chef de ménage", 0L);
//        map.put("Personne âgée sans soutien", 0L);
//        map.put("Ménage nombreux", 0L);
//        map.put("Analphabétisme", 0L);
//        map.put("Non vulnérable", 0L);
//        return map;
//    }
//
//    private Map<String, Long> initGenderMap(long total) {
//        Map<String, Long> map = new LinkedHashMap<>();
//        map.put("Total", total);
//        map.put("Hommes", 0L);
//        map.put("Femmes", 0L);
//        map.put("Autre", 0L);
//        return map;
//    }
//
//    private Map<String, Map<String, Long>> initCrossAnalysisMap() {
//        Map<String, Map<String, Long>> map = new LinkedHashMap<>();
//        for (String category : initVulnerabilityMap().keySet()) {
//            Map<String, Long> genderMap = new LinkedHashMap<>();
//            genderMap.put("Hommes", 0L);
//            genderMap.put("Femmes", 0L);
//            genderMap.put("Autre", 0L);
//            map.put(category, genderMap);
//        }
//        return map;
//    }
//
//    private String getGenderKey(String normalizedSexe, Set<String> maleTerms, Set<String> femaleTerms) {
//        if (femaleTerms.contains(normalizedSexe)) return "Femmes";
//        if (maleTerms.contains(normalizedSexe)) return "Hommes";
//        return "Autre";
//    }
//
//    @Override
//    public List<DatabasePapHabitatResponseDTO> getAll(int page, int size) {
//        Pageable pageRequest = PageRequest.of(page, size);
//        Page<DatabasePapHabitat> pageResult = repository.findAll(pageRequest);
//        return pageResult.getContent().stream().map(this::convertEntityToResponseDTO).collect(Collectors.toList());
//    }
//
//    @Override
//    public List<DatabasePapHabitatResponseDTO> getByProjectId(Long projectId, int page, int size) {
//        Pageable pageRequest = PageRequest.of(page, size);
//        Page<DatabasePapHabitat> pageResult = repository.findByProjectId(projectId, pageRequest);
//        return pageResult.getContent().stream().map(this::convertEntityToResponseDTO).collect(Collectors.toList());
//    }
//
//    @Override
//    public DatabasePapHabitatResponseDTO getById(Long id) {
//        DatabasePapHabitat entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Entity not found"));
//        return modelMapper.map(entity, DatabasePapHabitatResponseDTO.class);
//    }
//
//    @Override
//    public void update(Long id, DatabasePapHabitatRequestDTO requestDTO) {
//        DatabasePapHabitat entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Entity not found with ID: " + id));
//        modelMapper.getConfiguration().setAmbiguityIgnored(true);
//        modelMapper.typeMap(DatabasePapHabitatRequestDTO.class, DatabasePapHabitat.class).addMappings(m -> m.skip(DatabasePapHabitat::setId));
//        modelMapper.map(requestDTO, entity);
//
//        if (entity.getType() == null || entity.getType().isEmpty()) entity.setType("PAPHABITAT");
//        if (entity.getPerteTotale() == null) entity.setPerteTotale(calculatePerteTotale(entity));
//
//        if (requestDTO.getProjectId() != null) {
//            Project project = projectRepository.findById(requestDTO.getProjectId()).orElseThrow(() -> new RuntimeException("Project not found with ID: " + requestDTO.getProjectId()));
//            entity.setProject(project);
//        }
//
//        entity.setVulnerabilite(determinerVulnerabilite(entity));
//        repository.save(entity);
//    }
//
//    @Override
//    public void delete(Long id) {
//        DatabasePapHabitat entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Entity not found"));
//        repository.delete(entity);
//    }
//
//    private DatabasePapHabitatResponseDTO convertEntityToResponseDTO(DatabasePapHabitat entity) {
//        DatabasePapHabitatResponseDTO dto = modelMapper.map(entity, DatabasePapHabitatResponseDTO.class);
//        if (entity.getProject() != null) dto.setProjectId(entity.getProject().getId());
//        return dto;
//    }
//
//    @Override
//    public DatabasePapHabitatResponseDTO getByCodePap(String codePap) {
//        DatabasePapHabitat entity = repository.findByCodePap(codePap).orElseThrow(() -> new EntityNotFoundException("DatabasePapHabitat with codePap " + codePap + " not found"));
//        return modelMapper.map(entity, DatabasePapHabitatResponseDTO.class);
//    }
//
//    @Override
//    public long getTotalCount() { return repository.count(); }
//
//    @Override
//    public long getTotalCountByProjectId(Long projectId) { return repository.countByProjectId(projectId); }
//
//    @Override
//    public List<DatabasePapHabitatResponseDTO> searchGlobal(String searchTerm, Optional<Long> projectId, int page, int size) {
//        Pageable pageRequest = PageRequest.of(page, size);
//        Long projectIdValue = projectId.orElse(null);
//        Page<DatabasePapHabitat> pageResult = repository.searchGlobal(searchTerm, projectIdValue, pageRequest);
//        return pageResult.getContent().stream().map(this::convertEntityToResponseDTO).collect(Collectors.toList());
//    }
//
//    @Override
//    public long getTotalCountForSearch(String searchTerm, Optional<Long> projectId) {
//        Long projectIdValue = projectId.orElse(null);
//        return repository.countBySearchTermAndProjectId(searchTerm, projectIdValue);
//    }
//
//    @Override
//    public Double calculateTotalPerte(Long projectId) {
//        if (projectId == null) throw new IllegalArgumentException("Project ID cannot be null");
//        long totalItems = repository.countByProjectId(projectId);
//        if (totalItems == 0) return 0.0;
//        int pageSize = 1000;
//        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
//        double total = 0.0;
//        for (int i = 0; i < totalPages; i++) {
//            Page<DatabasePapHabitat> page = repository.findByProjectId(projectId, PageRequest.of(i, pageSize));
//            total += page.getContent().stream().filter(Objects::nonNull).mapToDouble(p -> p.getPerteTotale() != null ? p.getPerteTotale() : 0.0).sum();
//        }
//        return total;
//    }
//
//    @Override
//    public void deleteAllByProjectId(Long projectId) { repository.deleteAllByProjectId(projectId); }
//
//    @Override
//    public void deleteAllByIds(List<Long> ids) { repository.deleteAllByIdIn(ids); }
//}
//


package com.itma.gestionProjet.services.imp;

import com.itma.gestionProjet.dtos.CategoryStats;
import com.itma.gestionProjet.dtos.DatabasePapHabitatRequestDTO;
import com.itma.gestionProjet.dtos.DatabasePapHabitatResponseDTO;
import com.itma.gestionProjet.entities.DatabasePapHabitat;
import com.itma.gestionProjet.entities.Project;
import com.itma.gestionProjet.helpers.StatsHelper;
import com.itma.gestionProjet.repositories.DatabasePapHabitatRepository;
import com.itma.gestionProjet.repositories.ProjectRepository;
import com.itma.gestionProjet.services.DatabasePapHabitatService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
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
public class DatabasePapHabitatServiceImpl implements DatabasePapHabitatService {

    @Autowired
    private DatabasePapHabitatRepository repository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private StatsHelper statsHelper;

    @Override
    public void create(List<DatabasePapHabitatRequestDTO> requestDTOs) {
        modelMapper.getConfiguration().setAmbiguityIgnored(true);
        modelMapper.typeMap(DatabasePapHabitatRequestDTO.class, DatabasePapHabitat.class)
                .addMappings(mapper -> mapper.skip(DatabasePapHabitat::setId));

        List<DatabasePapHabitat> entities = requestDTOs.stream().map(dto -> {
            DatabasePapHabitat entity = convertDTOToEntity(dto);

            if (entity.getType() == null || entity.getType().isEmpty()) entity.setType("PAPHABITAT");

            if (dto.getProjectId() != null) {
                Project project = projectRepository.findById(dto.getProjectId())
                        .orElseThrow(() -> new RuntimeException("Project not found with ID: " + dto.getProjectId()));
                entity.setProject(project);
            }

            entity.setVulnerabilite(determinerVulnerabilite(entity));
            if (entity.getPerteTotale() == null) entity.setPerteTotale(calculatePerteTotale(entity));

            return entity;
        }).collect(Collectors.toList());

        repository.saveAll(entities);
    }

    /**
     * Convertit le DTO en Entity avec gestion sécurisée des conversions String vers Long/Double
     */
    private DatabasePapHabitat convertDTOToEntity(DatabasePapHabitatRequestDTO dto) {
        DatabasePapHabitat entity = new DatabasePapHabitat();

        // Copie des champs String normaux
        entity.setCodePap(dto.getCodePap());
        entity.setCodeParcelle(dto.getCodeParcelle());
        entity.setPrenom(dto.getPrenom());
        entity.setNom(dto.getNom());
        entity.setCaracteristiqueParcelle(dto.getCaracteristiqueParcelle());
        entity.setEvaluationPerte(dto.getEvaluationPerte());
        entity.setCommune(dto.getCommune());
        entity.setDepartement(dto.getDepartement());
        entity.setNombreParcelle(dto.getNombreParcelle());
        entity.setSurnom(dto.getSurnom());
        entity.setSexe(dto.getSexe());
        entity.setExistePni(dto.getExistePni());
        entity.setTypePni(dto.getTypePni());
        entity.setNumeroPni(dto.getNumeroPni());
        entity.setNumeroTelephone(dto.getNumeroTelephone());
        entity.setPhotoPap(dto.getPhotoPap());
        entity.setPointGeometriques(dto.getPointGeometriques());
        entity.setSuperficie(dto.getSuperficie());
        entity.setNationalite(dto.getNationalite());
        entity.setEthnie(dto.getEthnie());
        entity.setLangueParlee(dto.getLangueParlee());
        entity.setSituationMatrimoniale(dto.getSituationMatrimoniale());
        entity.setNiveauEtude(dto.getNiveauEtude());
        entity.setReligion(dto.getReligion());
        entity.setMembreFoyer(dto.getMembreFoyer());
        entity.setMembreFoyerHandicape(dto.getMembreFoyerHandicape());
        entity.setInformationsEtendues(dto.getInformationsEtendues());
        entity.setStatutPap(dto.getStatutPap());
        entity.setVulnerabilite(dto.getVulnerabilite());
        entity.setPj1(dto.getPj1());
        entity.setPj2(dto.getPj2());
        entity.setPj3(dto.getPj3());
        entity.setPj4(dto.getPj4());
        entity.setPj5(dto.getPj5());
        entity.setInfos_complemenataires(dto.getInfos_complemenataires());
        entity.setActivitePrincipale(dto.getActivitePrincipale());
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
        entity.setPerteTerre(parseDouble(dto.getPerteTerre()));
        entity.setPerteArbreJeune(parseDouble(dto.getPerteArbreJeune()));
        entity.setPerteArbreAdulte(parseDouble(dto.getPerteArbreAdulte()));
        entity.setPerteEquipement(parseDouble(dto.getPerteEquipement()));
        entity.setPerteTotale(parseDouble(dto.getPerteTotale()));
        entity.setPerteBatiment(parseDouble(dto.getPerteBatiment()));
        entity.setPerteTotaleArbre(parseDouble(dto.getPerteTotaleArbre()));
        entity.setPerteCloture(parseDouble(dto.getPerteCloture()));
        entity.setPerteRevenue(parseDouble(dto.getPerteRevenue()));
        entity.setAppuieRelocalisation(parseDouble(dto.getAppuieRelocalisation()));
        entity.setFraisDeplacement(parseDouble(dto.getFraisDeplacement()));

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

    private Double calculatePerteTotale(DatabasePapHabitat entity) {
        // Initialiser toutes les valeurs à 0 si elles sont null
        Double perteTerre = zeroIfNull(entity.getPerteTerre());
        Double perteArbreJeune = zeroIfNull(entity.getPerteArbreJeune());
        Double perteArbreAdulte = zeroIfNull(entity.getPerteArbreAdulte());
        Double perteEquipement = zeroIfNull(entity.getPerteEquipement());
        Double perteBatiment = zeroIfNull(entity.getPerteBatiment());

        // Calcul spécifique pour l'agricole
        return perteTerre + perteArbreJeune + perteArbreAdulte
                + perteEquipement + perteBatiment;
    }

    private Double zeroIfNull(Double v) { return v != null ? v : 0.0; }

    private String determinerVulnerabilite(DatabasePapHabitat pap) {
        List<String> vulnerabilites = new ArrayList<>();

        if (pap.getSituationMatrimoniale() != null &&
                Arrays.asList("veuf", "veuve", "divorcé", "divorcée", "célibataire")
                        .contains(pap.getSituationMatrimoniale().toLowerCase())) {
            vulnerabilites.add("Situation matrimoniale précaire");
        }

        if (pap.getMembreFoyerHandicape() != null && !pap.getMembreFoyerHandicape().isEmpty() &&
                !"non".equalsIgnoreCase(pap.getMembreFoyerHandicape())) {
            vulnerabilites.add("Ménage avec personne handicapée");
        }

        int age = -1;
        if (pap.getAge() != null) age = Math.toIntExact(pap.getAge());
        else if (pap.getDateNaissance() != null) age = calculerAge(pap.getDateNaissance());

        if (age >= 0) {
            if (age < 18 && "chef de ménage".equalsIgnoreCase(pap.getRoleDansFoyer())) {
                vulnerabilites.add("Mineur chef de ménage");
            }
            if (age >= 65 && (pap.getMembreFoyer() == null || pap.getMembreFoyer().isEmpty() || "seul".equalsIgnoreCase(pap.getMembreFoyer()))) {
                vulnerabilites.add("Personne âgée sans soutien");
            }
        }

        if (pap.getMembreFoyer() != null && !pap.getMembreFoyer().isEmpty()) {
            try {
                int nb = Integer.parseInt(pap.getMembreFoyer());
                boolean aActivite = pap.getActivitePrincipale() != null && !pap.getActivitePrincipale().isEmpty();
                if (nb >= 4 && aActivite) vulnerabilites.add("Ménage nombreux");
            } catch (NumberFormatException ignored) {}
        }

        if (pap.getNiveauEtude() != null && (pap.getNiveauEtude().toLowerCase().contains("ne sait pas lire") || "analphabète".equalsIgnoreCase(pap.getNiveauEtude()))) {
            vulnerabilites.add("Analphabétisme");
        }

        return vulnerabilites.isEmpty() ? "Non vulnérable" : String.join(", ", vulnerabilites);
    }

    private int calculerAge(LocalDate dn) { return Period.between(dn, LocalDate.now()).getYears(); }

    @Override
    public Map<String, Object> getVulnerabilityStats(Long projectId) {
        var pageRequest = PageRequest.of(0, 10_000_000);
        Page<DatabasePapHabitat> paged = repository.findByProjectId(projectId, pageRequest);
        List<DatabasePapHabitat> paps = paged.getContent();

        Map<String, Long> globalVulnerabilities = initVulnerabilityMap();
        Map<String, Long> globalGenders = initGenderMap(paps.size());
        Map<String, Map<String, Long>> vulnerabilitiesByGender = initCrossAnalysisMap();

        Set<String> maleTerms = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        maleTerms.addAll(Arrays.asList("m","masculin","homme","male","garçon","h","mâle"));
        Set<String> femaleTerms = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        femaleTerms.addAll(Arrays.asList("f","féminin","femme","femelle","female","fille","feminin"));

        for (DatabasePapHabitat pap : paps) {
            String sexe = pap.getSexe();
            String normalizedSexe = sexe != null ? sexe.trim().toLowerCase() : "";
            String genderKey = getGenderKey(normalizedSexe, maleTerms, femaleTerms);
            globalGenders.put(genderKey, globalGenders.get(genderKey) + 1);

            String vuln = pap.getVulnerabilite();
            boolean isNon = vuln == null || vuln.equals("Non vulnérable");
            if (isNon) {
                globalVulnerabilities.put("Non vulnérable", globalVulnerabilities.get("Non vulnérable") + 1);
                vulnerabilitiesByGender.get("Non vulnérable").put(genderKey, vulnerabilitiesByGender.get("Non vulnérable").get(genderKey) + 1);
                continue;
            }
            for (String category : globalVulnerabilities.keySet()) {
                if (vuln.contains(category)) {
                    globalVulnerabilities.put(category, globalVulnerabilities.get(category) + 1);
                    vulnerabilitiesByGender.get(category).put(genderKey, vulnerabilitiesByGender.get(category).get(genderKey) + 1);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("Vulnerabilites_globales", globalVulnerabilities);
        result.put("Sexes_globaux", globalGenders);
        result.put("Vulnerabilites_par_sexe", vulnerabilitiesByGender);
        return result;
    }

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
    public List<DatabasePapHabitatResponseDTO> getAll(int page, int size) {
        Pageable pageRequest = PageRequest.of(page, size);
        Page<DatabasePapHabitat> pageResult = repository.findAll(pageRequest);
        return pageResult.getContent().stream().map(this::convertEntityToResponseDTO).collect(Collectors.toList());
    }

    @Override
    public List<DatabasePapHabitatResponseDTO> getByProjectId(Long projectId, int page, int size) {
        Pageable pageRequest = PageRequest.of(page, size);
        Page<DatabasePapHabitat> pageResult = repository.findByProjectId(projectId, pageRequest);
        return pageResult.getContent().stream().map(this::convertEntityToResponseDTO).collect(Collectors.toList());
    }

    @Override
    public DatabasePapHabitatResponseDTO getById(Long id) {
        DatabasePapHabitat entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Entity not found"));
        return modelMapper.map(entity, DatabasePapHabitatResponseDTO.class);
    }

    @Override
    public void update(Long id, DatabasePapHabitatRequestDTO requestDTO) {
        DatabasePapHabitat entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Entity not found with ID: " + id));

        // Conversion manuelle du DTO vers l'entité
        DatabasePapHabitat updatedFields = convertDTOToEntity(requestDTO);

        // Copie des champs non-null
        if (updatedFields.getCodePap() != null) entity.setCodePap(updatedFields.getCodePap());
        if (updatedFields.getCodeParcelle() != null) entity.setCodeParcelle(updatedFields.getCodeParcelle());
        if (updatedFields.getPrenom() != null) entity.setPrenom(updatedFields.getPrenom());
        if (updatedFields.getNom() != null) entity.setNom(updatedFields.getNom());
        if (updatedFields.getCaracteristiqueParcelle() != null) entity.setCaracteristiqueParcelle(updatedFields.getCaracteristiqueParcelle());
        if (updatedFields.getEvaluationPerte() != null) entity.setEvaluationPerte(updatedFields.getEvaluationPerte());
        if (updatedFields.getCommune() != null) entity.setCommune(updatedFields.getCommune());
        if (updatedFields.getDepartement() != null) entity.setDepartement(updatedFields.getDepartement());
        if (updatedFields.getNombreParcelle() != null) entity.setNombreParcelle(updatedFields.getNombreParcelle());
        if (updatedFields.getSurnom() != null) entity.setSurnom(updatedFields.getSurnom());
        if (updatedFields.getSexe() != null) entity.setSexe(updatedFields.getSexe());
        if (updatedFields.getExistePni() != null) entity.setExistePni(updatedFields.getExistePni());
        if (updatedFields.getTypePni() != null) entity.setTypePni(updatedFields.getTypePni());
        if (updatedFields.getNumeroPni() != null) entity.setNumeroPni(updatedFields.getNumeroPni());
        if (updatedFields.getNumeroTelephone() != null) entity.setNumeroTelephone(updatedFields.getNumeroTelephone());
        if (updatedFields.getPhotoPap() != null) entity.setPhotoPap(updatedFields.getPhotoPap());
        if (updatedFields.getPointGeometriques() != null) entity.setPointGeometriques(updatedFields.getPointGeometriques());
        if (updatedFields.getSuperficie() != null) entity.setSuperficie(updatedFields.getSuperficie());
        if (updatedFields.getNationalite() != null) entity.setNationalite(updatedFields.getNationalite());
        if (updatedFields.getEthnie() != null) entity.setEthnie(updatedFields.getEthnie());
        if (updatedFields.getLangueParlee() != null) entity.setLangueParlee(updatedFields.getLangueParlee());
        if (updatedFields.getSituationMatrimoniale() != null) entity.setSituationMatrimoniale(updatedFields.getSituationMatrimoniale());
        if (updatedFields.getNiveauEtude() != null) entity.setNiveauEtude(updatedFields.getNiveauEtude());
        if (updatedFields.getReligion() != null) entity.setReligion(updatedFields.getReligion());
        if (updatedFields.getMembreFoyer() != null) entity.setMembreFoyer(updatedFields.getMembreFoyer());
        if (updatedFields.getMembreFoyerHandicape() != null) entity.setMembreFoyerHandicape(updatedFields.getMembreFoyerHandicape());
        if (updatedFields.getInformationsEtendues() != null) entity.setInformationsEtendues(updatedFields.getInformationsEtendues());
        if (updatedFields.getStatutPap() != null) entity.setStatutPap(updatedFields.getStatutPap());
        if (updatedFields.getPj1() != null) entity.setPj1(updatedFields.getPj1());
        if (updatedFields.getPj2() != null) entity.setPj2(updatedFields.getPj2());
        if (updatedFields.getPj3() != null) entity.setPj3(updatedFields.getPj3());
        if (updatedFields.getPj4() != null) entity.setPj4(updatedFields.getPj4());
        if (updatedFields.getPj5() != null) entity.setPj5(updatedFields.getPj5());
        if (updatedFields.getInfos_complemenataires() != null) entity.setInfos_complemenataires(updatedFields.getInfos_complemenataires());
        if (updatedFields.getActivitePrincipale() != null) entity.setActivitePrincipale(updatedFields.getActivitePrincipale());
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
        if (updatedFields.getPerteTerre() != null) entity.setPerteTerre(updatedFields.getPerteTerre());
        if (updatedFields.getPerteArbreJeune() != null) entity.setPerteArbreJeune(updatedFields.getPerteArbreJeune());
        if (updatedFields.getPerteArbreAdulte() != null) entity.setPerteArbreAdulte(updatedFields.getPerteArbreAdulte());
        if (updatedFields.getPerteEquipement() != null) entity.setPerteEquipement(updatedFields.getPerteEquipement());
        if (updatedFields.getPerteBatiment() != null) entity.setPerteBatiment(updatedFields.getPerteBatiment());
        if (updatedFields.getPerteTotaleArbre() != null) entity.setPerteTotaleArbre(updatedFields.getPerteTotaleArbre());
        if (updatedFields.getPerteCloture() != null) entity.setPerteCloture(updatedFields.getPerteCloture());
        if (updatedFields.getPerteRevenue() != null) entity.setPerteRevenue(updatedFields.getPerteRevenue());
        if (updatedFields.getAppuieRelocalisation() != null) entity.setAppuieRelocalisation(updatedFields.getAppuieRelocalisation());
        if (updatedFields.getFraisDeplacement() != null) entity.setFraisDeplacement(updatedFields.getFraisDeplacement());

        if (entity.getType() == null || entity.getType().isEmpty()) entity.setType("PAPHABITAT");
        if (entity.getPerteTotale() == null) entity.setPerteTotale(calculatePerteTotale(entity));

        if (requestDTO.getProjectId() != null) {
            Project project = projectRepository.findById(requestDTO.getProjectId()).orElseThrow(() -> new RuntimeException("Project not found with ID: " + requestDTO.getProjectId()));
            entity.setProject(project);
        }

        entity.setVulnerabilite(determinerVulnerabilite(entity));
        repository.save(entity);
    }

    @Override
    public void delete(Long id) {
        DatabasePapHabitat entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Entity not found"));
        repository.delete(entity);
    }

    private DatabasePapHabitatResponseDTO convertEntityToResponseDTO(DatabasePapHabitat entity) {
        DatabasePapHabitatResponseDTO dto = modelMapper.map(entity, DatabasePapHabitatResponseDTO.class);
        if (entity.getProject() != null) dto.setProjectId(entity.getProject().getId());
        return dto;
    }

    @Override
    public DatabasePapHabitatResponseDTO getByCodePap(String codePap) {
        DatabasePapHabitat entity = repository.findByCodePap(codePap).orElseThrow(() -> new EntityNotFoundException("DatabasePapHabitat with codePap " + codePap + " not found"));
        return modelMapper.map(entity, DatabasePapHabitatResponseDTO.class);
    }

    @Override
    public long getTotalCount() { return repository.count(); }

    @Override
    public long getTotalCountByProjectId(Long projectId) { return repository.countByProjectId(projectId); }

    @Override
    public List<DatabasePapHabitatResponseDTO> searchGlobal(String searchTerm, Optional<Long> projectId, int page, int size) {
        Pageable pageRequest = PageRequest.of(page, size);
        Long projectIdValue = projectId.orElse(null);
        Page<DatabasePapHabitat> pageResult = repository.searchGlobal(searchTerm, projectIdValue, pageRequest);
        return pageResult.getContent().stream().map(this::convertEntityToResponseDTO).collect(Collectors.toList());
    }

    @Override
    public long getTotalCountForSearch(String searchTerm, Optional<Long> projectId) {
        Long projectIdValue = projectId.orElse(null);
        return repository.countBySearchTermAndProjectId(searchTerm, projectIdValue);
    }

    @Override
    public Double calculateTotalPerte(Long projectId) {
        if (projectId == null) throw new IllegalArgumentException("Project ID cannot be null");
        long totalItems = repository.countByProjectId(projectId);
        if (totalItems == 0) return 0.0;
        int pageSize = 1000;
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        double total = 0.0;
        for (int i = 0; i < totalPages; i++) {
            Page<DatabasePapHabitat> page = repository.findByProjectId(projectId, PageRequest.of(i, pageSize));
            total += page.getContent().stream().filter(Objects::nonNull).mapToDouble(p -> p.getPerteTotale() != null ? p.getPerteTotale() : 0.0).sum();
        }
        return total;
    }

    @Override
    public void deleteAllByProjectId(Long projectId) { repository.deleteAllByProjectId(projectId); }

//    @Override
//    public void deleteAllByIds(List<Long> ids) { repository.deleteAllByIdIn(ids); }


    @Override
    @Transactional  // ⚠️ TRÈS IMPORTANT
    public void deleteAllByIds(List<Long> ids) {
        // Validation
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("IDs list cannot be null or empty");
        }

        // Vérifier que tous les IDs existent (optionnel mais recommandé)
        long existingCount = repository.countByIdIn(ids);
        if (existingCount != ids.size()) {
            throw new EntityNotFoundException(
                    String.format("Some IDs not found. Expected: %d, Found: %d",
                            ids.size(), existingCount)
            );
        }

        // Supprimer - Méthode 1 : JPA native (RECOMMANDÉ)
        repository.deleteAllById(ids);

        // OU Méthode 2 : Custom method
        // repository.deleteByIdIn(ids);
    }

    // Méthode utilitaire pour vérifier l'existence
    @Override
    public boolean existAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        return repository.countByIdIn(ids) == ids.size();
    }

    @Override
    public CategoryStats getCategoryStats(Long projectId) {
        List<DatabasePapHabitat> paps = repository
                .findByProjectId(projectId, PageRequest.of(0, 10_000_000))
                .getContent();

        double totalPerte = paps.stream()
                .mapToDouble(p -> p.getPerteTotale() != null ? p.getPerteTotale() : 0.0)
                .sum();

        return statsHelper.buildCategoryStats(
                paps,
                DatabasePapHabitat::getSexe,
                DatabasePapHabitat::getVulnerabilite,
                totalPerte
        );
    }
}