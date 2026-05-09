# CLAUDE — Adaptation Backend Spring Boot : Module Plainte (Invodis)

## Contexte du projet
Je travaille sur le projet **Invodis** (Spring Boot backend).
J'ai déjà un module plainte fonctionnel. Je dois l'adapter pour correspondre exactement à la structure d'un fichier Excel de plaintes provenant de Konni (35 colonnes).

---

## ÉTAPE 1 — Mettre à jour l'entité `Plainte`

Remplace/adapte mon entité `Plainte` existante pour qu'elle corresponde exactement à cette structure. Utilise Jakarta JPA + Lombok.

```java
@Entity
@Table(name = "plainte")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plainte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Identification ──────────────────────────────────
    @Column(name = "statut")
    private String statut;

    @Column(name = "numero_reference", unique = true)
    private String numeroReference;

    @Column(name = "date_enregistrement")
    private LocalDate dateEnregistrement;

    @Column(name = "mois_reception")
    private LocalDate moisReception;

    // ── PAP (Personne Affectée par le Projet) ───────────
    @Column(name = "code_pap")
    private String codePap;

    @Column(name = "nom_prenom")
    private String nomPrenom;

    @Column(name = "mandataire")
    private String mandataire;

    @Column(name = "sexe")
    private String sexe;

    @Column(name = "telephone")
    private String telephone;

    @Column(name = "perimetre_gmp")
    private String perimetreGmp;

    @Column(name = "numero_parcelle")
    private String numeroParcelle;

    @Column(name = "type_carte_identite")
    private String typeCarteIdentite;

    @Column(name = "cin")
    private String cin;

    @Column(name = "type_pap")
    private String typePap;

    @Column(name = "village_quartier")
    private String villageQuartier;

    @Column(name = "plainte_par_zone")
    private String plainteParZone;

    // ── Plainte ─────────────────────────────────────────
    @Column(name = "categorisation")
    private String categorisation;

    @Column(name = "objet_plainte", columnDefinition = "TEXT")
    private String objetPlainte;

    @Column(name = "niveau_gravite")
    private String niveauGravite;

    @Column(name = "description_plainte", columnDefinition = "TEXT")
    private String descriptionPlainte;

    @Column(name = "facilitateur")
    private String facilitateur;

    // ── Résolution ──────────────────────────────────────
    @Column(name = "description_reglement", columnDefinition = "TEXT")
    private String descriptionReglement;

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    @Column(name = "communication_resolution1", columnDefinition = "TEXT")
    private String communicationResolution1;

    @Column(name = "date_traitement_consultant")
    private LocalDate dateTraitementConsultant;

    @Column(name = "date_visite")
    private LocalDate dateVisite;

    @Column(name = "communication_resolution2", columnDefinition = "TEXT")
    private String communicationResolution2;

    @Column(name = "date_traitement_clm")
    private LocalDate dateTraitementClm;

    @Column(name = "communication_resolution3", columnDefinition = "TEXT")
    private String communicationResolution3;

    @Column(name = "date_traitement_ccd")
    private LocalDate dateTraitementCcd;

    @Column(name = "resolution_plainte", columnDefinition = "TEXT")
    private String resolutionPlainte;

    @Column(name = "si_non_expliquez", columnDefinition = "TEXT")
    private String siNonExpliquez;

    @Column(name = "prochaine_etape", columnDefinition = "TEXT")
    private String prochaineEtape;

    @Column(name = "date_cloture")
    private LocalDate dateCloture;

    @Column(name = "delai_resolution")
    private String delaiResolution;

    // ── Projet (lien FK comme les autres entités Invodis) ─
    @Column(name = "project_id")
    private Long projectId;

    // ── Audit ───────────────────────────────────────────
    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

---

## ÉTAPE 2 — Mettre à jour `PlainteRepository`

```java
public interface PlainteRepository extends JpaRepository<Plainte, Long> {
    boolean existsByNumeroReference(String numeroReference);
    Page<Plainte> findByProjectId(Long projectId, Pageable pageable);
    List<Plainte> findByProjectIdAndStatut(Long projectId, String statut);
    long countByProjectId(Long projectId);
}
```

---

## ÉTAPE 3 — DTO de résultat d'import

Crée ce record (ou classe si Java < 16) :

```java
public record PlainteImportResultDto(
    int totalLignes,
    int importees,
    int doublons,
    int erreurs,
    List<String> erreurDetails
) {}
```

---

## ÉTAPE 4 — Adapter la méthode `importerDepuisExcel` dans `PlainteService`

Adapte mon service existant. La logique d'import doit :

1. Lire le fichier `.xlsx` avec Apache POI (`XSSFWorkbook`)
2. Prendre la **première feuille** (index 0 = "BD PLAINTES PAR KONNI")
3. Ignorer la ligne 0 (en-tête)
4. Ignorer les lignes où la cellule `numeroReference` (colonne 1) est vide
5. Pour chaque ligne valide :
    - Mapper les **35 colonnes** (index 0 à 34) vers l'entité `Plainte`
    - Vérifier doublon via `existsByNumeroReference` → compter dans `doublons`
    - Affecter le `projectId` passé en paramètre
    - Normaliser le `sexe` (MASCULIN/Masculin → "Masculin", FEMININ/feminin → "Feminin")
    - Normaliser `niveauGravite` (ELEVE/Eléve → "Elevé")
6. Sauvegarder par batch de 100 (`saveAll`)
7. Retourner `PlainteImportResultDto`

**Mapping colonnes exact (0-based) :**
```
0  → statut
1  → numeroReference
2  → dateEnregistrement       (LocalDate)
3  → moisReception            (LocalDate)
4  → codePap
5  → nomPrenom
6  → mandataire
7  → sexe
8  → telephone
9  → perimetreGmp
10 → numeroParcelle
11 → typeCarteIdentite
12 → cin
13 → typePap
14 → villageQuartier
15 → plainteParZone
16 → categorisation
17 → objetPlainte
18 → niveauGravite
19 → descriptionPlainte
20 → facilitateur
21 → descriptionReglement
22 → observations
23 → communicationResolution1
24 → dateTraitementConsultant (LocalDate)
25 → dateVisite               (LocalDate)
26 → communicationResolution2
27 → dateTraitementClm        (LocalDate)
28 → communicationResolution3
29 → dateTraitementCcd        (LocalDate)
30 → resolutionPlainte
31 → siNonExpliquez
32 → prochaineEtape
33 → dateCloture              (LocalDate)
34 → delaiResolution
```

**Méthodes utilitaires à inclure :**

```java
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
```

---

## ÉTAPE 5 — Adapter `PlainteController`

Adapte le controller existant pour ajouter/modifier :

```java
// Import Excel
@PostMapping("/import")
public ResponseEntity<?> importerExcel(
        @RequestParam("file") MultipartFile file,
        @RequestParam("projectId") Long projectId) {
    if (!Objects.requireNonNull(file.getOriginalFilename()).endsWith(".xlsx")) {
        return ResponseEntity.badRequest().body("Seuls les fichiers .xlsx sont acceptés");
    }
    PlainteImportResultDto result = plainteService.importerDepuisExcel(file, projectId);
    return ResponseEntity.ok(result);
}

// Liste paginée par projet
@GetMapping
public ResponseEntity<?> getByProject(
        @RequestParam Long projectId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(plainteService.getByProject(projectId, Pageable.ofSize(size).withPage(page)));
}
```

---

## Notes importantes
- Conserver la structure de réponse existante d'Invodis (`responseCode`, `message`, `data`)
- Conserver les annotations de sécurité déjà en place (`@PreAuthorize` si utilisé)
- Ne pas casser les endpoints existants, seulement les étendre/adapter