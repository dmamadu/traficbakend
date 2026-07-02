package com.itma.gestionProjet.services.imp;

import com.itma.gestionProjet.dtos.AApiResponse;
import com.itma.gestionProjet.dtos.PlainteDto;
import com.itma.gestionProjet.dtos.PlainteImportResultDto;
import com.itma.gestionProjet.entities.AuditEntry;
import com.itma.gestionProjet.entities.Plainte;
import com.itma.gestionProjet.repositories.AuditRepository;
import com.itma.gestionProjet.repositories.PlainteRepository;
import com.itma.gestionProjet.repositories.ProjectRepository;
import com.itma.gestionProjet.requests.PlainteRequest;
import com.itma.gestionProjet.services.PlainteService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlainteServiceImpl implements PlainteService {

    @Autowired
    private PlainteRepository plainteRepository;

    @Autowired
    private ProjectRepository projetRepository;

    @Autowired
    private AuditRepository auditRepository;

    @Override
    public PlainteDto createPlainte(PlainteRequest req) {
        projetRepository.findById(req.getProjectId())
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));
        Plainte p = new Plainte();
        mapRequestToEntity(req, p);
        return convertEntityToDto(plainteRepository.save(p));
    }

    @Override
    public PlainteDto updatePlainte(Long id, PlainteRequest req) {
        Plainte p = plainteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plainte non trouvée"));
        projetRepository.findById(req.getProjectId())
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));
        mapRequestToEntity(req, p);
        return convertEntityToDto(plainteRepository.save(p));
    }

    @Override
    public Page<PlainteDto> getAllPlaintes(Pageable pageable) {
        return plainteRepository.findAll(pageable).map(this::convertEntityToDto);
    }

    @Override
    public Page<PlainteDto> getPlaintesByProjectId(Long projectId, Pageable pageable) {
        return plainteRepository.findByProjectId(projectId, pageable).map(this::convertEntityToDto);
    }

    @Override
    public Page<PlainteDto> getByProject(Long projectId, Pageable pageable) {
        return getPlaintesByProjectId(projectId, pageable);
    }

    @Override
    public PlainteDto getPlainteById(Long id) {
        return convertEntityToDto(plainteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plainte non trouvée")));
    }

    @Override
    public void deletePlainte(Long id) {
        plainteRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("IDs list cannot be null or empty");
        }
        List<Plainte> plaintes = plainteRepository.findAllById(ids);
        String utilisateur = SecurityContextHolder.getContext().getAuthentication().getName();
        String details = String.format(
                "Suppression en masse de %d plainte(s): %s",
                plaintes.size(),
                plaintes.stream()
                        .map(p -> p.getNumeroReference() != null ? p.getNumeroReference() : "id:" + p.getId())
                        .collect(Collectors.joining(", "))
        );
        auditRepository.save(new AuditEntry("SUPPRESSION_PLAINTE_BATCH", details, utilisateur, LocalDateTime.now()));
        plainteRepository.deleteAllByIdIn(ids);
    }

    @Override
    public List<PlainteDto> createPlaintes(List<PlainteRequest> plainteRequests) {
        List<PlainteDto> result = new ArrayList<>();
        for (PlainteRequest req : plainteRequests) {
            try {
                result.add(createPlainte(req));
            } catch (Exception ignored) {}
        }
        return result;
    }

    @Override
    public AApiResponse<PlainteDto> getPlainteByCodePap(String codePap, int page, int size) {
        Page<Plainte> plaintesPage = plainteRepository.findByCodePap(codePap, PageRequest.of(page, size));
        AApiResponse<PlainteDto> response = new AApiResponse<>();
        if (!plaintesPage.isEmpty()) {
            response.setResponseCode(200);
            response.setData(plaintesPage.stream().map(this::convertEntityToDto).collect(Collectors.toList()));
            response.setMessage("Plaintes trouvées avec succès.");
            response.setLength(plaintesPage.getTotalElements());
        } else {
            response.setResponseCode(404);
            response.setData(new ArrayList<>());
            response.setMessage("Aucune plainte trouvée avec le codePap fourni.");
        }
        return response;
    }

    @Override
    public AApiResponse<PlainteDto> searchGlobalPlaintes(String searchTerm, Optional<Long> projectId, int page, int size) {
        try {
            Page<Plainte> pageResult = plainteRepository.searchGlobal(searchTerm, projectId, PageRequest.of(page, size));
            AApiResponse<PlainteDto> response = new AApiResponse<>();
            response.setResponseCode(200);
            response.setData(pageResult.getContent().stream().map(this::convertEntityToDto).collect(Collectors.toList()));
            response.setOffset(page);
            response.setLength(pageResult.getTotalElements());
            response.setMax(size);
            response.setMessage("Successfully retrieved data.");
            return response;
        } catch (Exception e) {
            AApiResponse<PlainteDto> errorResponse = new AApiResponse<>();
            errorResponse.setResponseCode(500);
            errorResponse.setMessage("Error retrieving data: " + e.getMessage());
            return errorResponse;
        }
    }

    @Override
    @Transactional
    public PlainteImportResultDto importerDepuisExcel(MultipartFile file, Long projectId) {
        int totalLignes = 0;
        int importees = 0;
        int doublons = 0;
        int erreurs = 0;
        List<String> erreurDetails = new ArrayList<>();
        List<Plainte> batch = new ArrayList<>();

        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;

                String numeroRef = getCellStringValue(row.getCell(1));
                if (numeroRef == null || numeroRef.isBlank()) continue;

                totalLignes++;

                if (plainteRepository.existsByNumeroReference(numeroRef)) {
                    doublons++;
                    continue;
                }

                try {
                    Plainte p = Plainte.builder()
                            .statut(getCellStringValue(row.getCell(0)))
                            .numeroReference(numeroRef)
                            .dateEnregistrement(getCellDateValue(row.getCell(2)))
                            .moisReception(getCellDateValue(row.getCell(3)))
                            .codePap(getCellStringValue(row.getCell(4)))
                            .nomPrenom(getCellStringValue(row.getCell(5)))
                            .mandataire(getCellStringValue(row.getCell(6)))
                            .sexe(normalizeSexe(getCellStringValue(row.getCell(7))))
                            .telephone(getCellStringValue(row.getCell(8)))
                            .perimetreGmp(getCellStringValue(row.getCell(9)))
                            .numeroParcelle(getCellStringValue(row.getCell(10)))
                            .typeCarteIdentite(getCellStringValue(row.getCell(11)))
                            .cin(getCellStringValue(row.getCell(12)))
                            .typePap(getCellStringValue(row.getCell(13)))
                            .villageQuartier(getCellStringValue(row.getCell(14)))
                            .plainteParZone(getCellStringValue(row.getCell(15)))
                            .categorisation(getCellStringValue(row.getCell(16)))
                            .objetPlainte(getCellStringValue(row.getCell(17)))
                            .niveauGravite(normalizeNiveauGravite(getCellStringValue(row.getCell(18))))
                            .descriptionPlainte(getCellStringValue(row.getCell(19)))
                            .facilitateur(getCellStringValue(row.getCell(20)))
                            .descriptionReglement(getCellStringValue(row.getCell(21)))
                            .observations(getCellStringValue(row.getCell(22)))
                            .communicationResolution1(getCellStringValue(row.getCell(23)))
                            .dateTraitementConsultant(getCellDateValue(row.getCell(24)))
                            .dateVisite(getCellDateValue(row.getCell(25)))
                            .communicationResolution2(getCellStringValue(row.getCell(26)))
                            .dateTraitementClm(getCellDateValue(row.getCell(27)))
                            .communicationResolution3(getCellStringValue(row.getCell(28)))
                            .dateTraitementCcd(getCellDateValue(row.getCell(29)))
                            .resolutionPlainte(getCellStringValue(row.getCell(30)))
                            .siNonExpliquez(getCellStringValue(row.getCell(31)))
                            .prochaineEtape(getCellStringValue(row.getCell(32)))
                            .dateCloture(getCellDateValue(row.getCell(33)))
                            .delaiResolution(getCellStringValue(row.getCell(34)))
                            .projectId(projectId)
                            .build();

                    batch.add(p);
                    importees++;

                    if (batch.size() == 100) {
                        plainteRepository.saveAll(batch);
                        batch.clear();
                    }
                } catch (Exception e) {
                    erreurs++;
                    erreurDetails.add("Ligne " + (rowIndex + 1) + " : " + e.getMessage());
                }
            }

            if (!batch.isEmpty()) {
                plainteRepository.saveAll(batch);
            }

        } catch (IOException e) {
            erreurDetails.add("Erreur lecture fichier : " + e.getMessage());
            erreurs++;
        }

        return new PlainteImportResultDto(totalLignes, importees, doublons, erreurs, erreurDetails);
    }

    private String normalizeSexe(String value) {
        if (value == null) return null;
        String upper = value.trim().toUpperCase(Locale.ROOT)
                .replace("É", "E").replace("È", "E").replace("Ê", "E");
        if (upper.contains("MASCULIN")) return "Masculin";
        if (upper.contains("FEMININ") || upper.contains("FÉMININ")) return "Feminin";
        return value;
    }

    private String normalizeNiveauGravite(String value) {
        if (value == null) return null;
        String upper = value.trim().toUpperCase(Locale.ROOT)
                .replace("É", "E").replace("È", "E").replace("Ê", "E").replace("Ë", "E");
        if (upper.contains("ELEV")) return "Elevé";
        return value;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell))
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                yield String.valueOf((long) cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCachedFormulaResultType() == CellType.STRING
                    ? cell.getStringCellValue().trim()
                    : String.valueOf((long) cell.getNumericCellValue());
            default -> null;
        };
    }

    private LocalDate getCellDateValue(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell))
                return cell.getLocalDateTimeCellValue().toLocalDate();
            if (cell.getCellType() == CellType.STRING) {
                String val = cell.getStringCellValue().trim();
                if (!val.isEmpty() && !val.equalsIgnoreCase("N/D") && !val.equalsIgnoreCase("NaT"))
                    return LocalDate.parse(val);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void mapRequestToEntity(PlainteRequest req, Plainte p) {
        p.setStatut(req.getStatut());
        p.setNumeroReference(req.getNumeroReference());
        p.setDateEnregistrement(req.getDateEnregistrement());
        p.setMoisReception(req.getMoisReception());
        p.setCodePap(req.getCodePap());
        p.setNomPrenom(req.getNomPrenom());
        p.setMandataire(req.getMandataire());
        p.setSexe(req.getSexe());
        p.setTelephone(req.getTelephone());
        p.setPerimetreGmp(req.getPerimetreGmp());
        p.setNumeroParcelle(req.getNumeroParcelle());
        p.setTypeCarteIdentite(req.getTypeCarteIdentite());
        p.setCin(req.getCin());
        p.setTypePap(req.getTypePap());
        p.setVillageQuartier(req.getVillageQuartier());
        p.setPlainteParZone(req.getPlainteParZone());
        p.setCategorisation(req.getCategorisation());
        p.setObjetPlainte(req.getObjetPlainte());
        p.setNiveauGravite(req.getNiveauGravite());
        p.setDescriptionPlainte(req.getDescriptionPlainte());
        p.setFacilitateur(req.getFacilitateur());
        p.setDescriptionReglement(req.getDescriptionReglement());
        p.setObservations(req.getObservations());
        p.setCommunicationResolution1(req.getCommunicationResolution1());
        p.setDateTraitementConsultant(req.getDateTraitementConsultant());
        p.setDateVisite(req.getDateVisite());
        p.setCommunicationResolution2(req.getCommunicationResolution2());
        p.setDateTraitementClm(req.getDateTraitementClm());
        p.setCommunicationResolution3(req.getCommunicationResolution3());
        p.setDateTraitementCcd(req.getDateTraitementCcd());
        p.setResolutionPlainte(req.getResolutionPlainte());
        p.setSiNonExpliquez(req.getSiNonExpliquez());
        p.setProchaineEtape(req.getProchaineEtape());
        p.setDateCloture(req.getDateCloture());
        p.setDelaiResolution(req.getDelaiResolution());
        p.setProjectId(req.getProjectId());
    }

    private PlainteDto convertEntityToDto(Plainte p) {
        PlainteDto dto = new PlainteDto();
        dto.setId(p.getId());
        dto.setStatut(p.getStatut());
        dto.setNumeroReference(p.getNumeroReference());
        dto.setDateEnregistrement(p.getDateEnregistrement());
        dto.setMoisReception(p.getMoisReception());
        dto.setCodePap(p.getCodePap());
        dto.setNomPrenom(p.getNomPrenom());
        dto.setMandataire(p.getMandataire());
        dto.setSexe(p.getSexe());
        dto.setTelephone(p.getTelephone());
        dto.setPerimetreGmp(p.getPerimetreGmp());
        dto.setNumeroParcelle(p.getNumeroParcelle());
        dto.setTypeCarteIdentite(p.getTypeCarteIdentite());
        dto.setCin(p.getCin());
        dto.setTypePap(p.getTypePap());
        dto.setVillageQuartier(p.getVillageQuartier());
        dto.setPlainteParZone(p.getPlainteParZone());
        dto.setCategorisation(p.getCategorisation());
        dto.setObjetPlainte(p.getObjetPlainte());
        dto.setNiveauGravite(p.getNiveauGravite());
        dto.setDescriptionPlainte(p.getDescriptionPlainte());
        dto.setFacilitateur(p.getFacilitateur());
        dto.setDescriptionReglement(p.getDescriptionReglement());
        dto.setObservations(p.getObservations());
        dto.setCommunicationResolution1(p.getCommunicationResolution1());
        dto.setDateTraitementConsultant(p.getDateTraitementConsultant());
        dto.setDateVisite(p.getDateVisite());
        dto.setCommunicationResolution2(p.getCommunicationResolution2());
        dto.setDateTraitementClm(p.getDateTraitementClm());
        dto.setCommunicationResolution3(p.getCommunicationResolution3());
        dto.setDateTraitementCcd(p.getDateTraitementCcd());
        dto.setResolutionPlainte(p.getResolutionPlainte());
        dto.setSiNonExpliquez(p.getSiNonExpliquez());
        dto.setProchaineEtape(p.getProchaineEtape());
        dto.setDateCloture(p.getDateCloture());
        dto.setDelaiResolution(p.getDelaiResolution());
        dto.setProjectId(p.getProjectId());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }
}