package com.itma.gestionProjet.services;

import com.itma.gestionProjet.dtos.DossierEtapeDto;
import com.itma.gestionProjet.dtos.MiseEnOeuvreStats;
import com.itma.gestionProjet.entities.Entente;
import com.itma.gestionProjet.entities.EtatProcessusEntente;
import com.itma.gestionProjet.entities.StatutEntente;
import com.itma.gestionProjet.entities.TypePap;
import com.itma.gestionProjet.repositories.DatabasePapAgricoleRepository;
import com.itma.gestionProjet.repositories.DatabasePapHabitatRepository;
import com.itma.gestionProjet.repositories.DatabasePapPlaceAffaireRepository;
import com.itma.gestionProjet.repositories.EntenteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Calculs du tableau de bord « Mise en œuvre du PAR ». Lecture seule.
 * Voir {@link MiseEnOeuvreStats} pour les définitions (bases PAP / dossiers, effet de la période).
 */
@Service
@RequiredArgsConstructor
public class MiseEnOeuvreStatsService {

    static final String CAT_PLACE_AFFAIRE = "placeAffaire";
    static final String CAT_AGRICOLE = "agricole";
    static final String CAT_HABITAT = "habitat";

    /** Étapes à partir desquelles le paiement est effectué (même règle que /stats/combine). */
    static final Set<EtatProcessusEntente> ETATS_PAYES = EnumSet.of(
            EtatProcessusEntente.FORMATION_A_DONNER,
            EtatProcessusEntente.SUIVI_A_EFFECTUER,
            EtatProcessusEntente.PROCESSUS_TERMINE
    );

    static final int SEUIL_RETARD_ACCORD_JOURS = 30;

    private static final PageRequest TOUT = PageRequest.of(0, 10_000_000);

    private final EntenteRepository ententeRepository;
    private final DatabasePapAgricoleRepository agricoleRepository;
    private final DatabasePapPlaceAffaireRepository placeAffaireRepository;
    private final DatabasePapHabitatRepository habitatRepository;

    /** Vue commune d'une PAP, quelle que soit sa catégorie. */
    record PapInfo(String categorie, Long id, String codePap, String prenom, String nom,
                   String commune, boolean vulnerable, double perteTotale) {}

    /** Une entente rattachée à sa PAP (pap peut être null si la PAP a été supprimée). */
    record Dossier(Entente entente, PapInfo pap) {}

    // ───────────────────────────── API ─────────────────────────────

    public MiseEnOeuvreStats getStats(Long projectId, String zone, LocalDate debut, LocalDate fin) {
        List<PapInfo> toutesPaps = chargerPaps(projectId);
        Map<String, PapInfo> papsParCle = indexer(toutesPaps);
        List<Dossier> tousDossiers = chargerDossiers(projectId, papsParCle);

        List<PapInfo> paps = toutesPaps.stream().filter(p -> dansZone(p, zone)).toList();
        List<Dossier> dossiers = tousDossiers.stream().filter(d -> dansZone(d.pap(), zone)).toList();

        MiseEnOeuvreStats s = new MiseEnOeuvreStats();
        s.setDateMaj(LocalDateTime.now());
        s.setZones(toutesPaps.stream()
                .map(PapInfo::commune)
                .filter(c -> c != null && !c.isBlank())
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList());

        s.setTotalPap(paps.size());
        s.setTotalVulnerables(paps.stream().filter(PapInfo::vulnerable).count());
        s.setTotalDossiers(dossiers.size());
        s.setVulnerablesDossiers(dossiers.stream().filter(this::estVulnerable).count());

        // Paiements (filtrés par période sur datePaiement)
        List<Dossier> payes = dossiers.stream()
                .filter(d -> estPaye(d.entente()))
                .filter(d -> dansPeriode(d.entente().getDatePaiement(), debut, fin))
                .toList();
        double budget = paps.stream().mapToDouble(PapInfo::perteTotale).sum();
        double verse = payes.stream().mapToDouble(this::montant).sum();
        s.setBudgetPrevu(budget);
        s.setMontantVerse(verse);
        s.setResteAPayer(Math.max(0, budget - verse));
        s.setTauxDecaissement(budget > 0 ? verse * 100.0 / budget : 0);
        s.setPapPayees(payes.size());
        s.setVulnerablesPayees(payes.stream().filter(this::estVulnerable).count());

        // Étapes (photo à l'instant T)
        Map<EtatProcessusEntente, Long> parEtape = dossiers.stream()
                .collect(Collectors.groupingBy(d -> etat(d.entente()), Collectors.counting()));
        for (EtatProcessusEntente e : EtatProcessusEntente.values()) {
            s.getEtapes().add(new MiseEnOeuvreStats.Etape(e.name(), libelle(e), parEtape.getOrDefault(e, 0L)));
        }
        s.setDossiersTransmis(dossiers.size() - parEtape.getOrDefault(EtatProcessusEntente.COMPENSATION_A_ETABLIR, 0L));

        // Statuts des ententes
        Map<StatutEntente, Long> parStatut = dossiers.stream()
                .filter(d -> d.entente().getStatut() != null)
                .collect(Collectors.groupingBy(d -> d.entente().getStatut(), Collectors.counting()));
        s.getStatuts().setBrouillon(parStatut.getOrDefault(StatutEntente.BROUILLON, 0L));
        s.getStatuts().setSynchronisee(parStatut.getOrDefault(StatutEntente.SYNCHRONISEE, 0L));
        s.getStatuts().setFinalisee(parStatut.getOrDefault(StatutEntente.FINALISEE, 0L));
        s.setEntentesFinalisees(dossiers.stream()
                .filter(d -> d.entente().getStatut() == StatutEntente.FINALISEE)
                .filter(d -> dansPeriode(d.entente().getDateFinalisation(), debut, fin))
                .count());

        // Paiements par catégorie
        for (String cat : List.of(CAT_PLACE_AFFAIRE, CAT_AGRICOLE, CAT_HABITAT)) {
            s.getCategories().add(new MiseEnOeuvreStats.Categorie(
                    cat,
                    libelleCategorie(cat),
                    paps.stream().filter(p -> cat.equals(p.categorie())).count(),
                    dossiers.stream().filter(d -> cat.equals(categorie(d))).count(),
                    payes.stream().filter(d -> cat.equals(categorie(d))).count(),
                    paps.stream().filter(p -> cat.equals(p.categorie())).mapToDouble(PapInfo::perteTotale).sum(),
                    payes.stream().filter(d -> cat.equals(categorie(d))).mapToDouble(this::montant).sum()
            ));
        }

        // Points d'attention (photo à l'instant T)
        LocalDateTime maintenant = LocalDateTime.now();
        s.getAlertes().setAccordsEnRetard(dossiers.stream()
                .filter(d -> etat(d.entente()) == EtatProcessusEntente.ACCORD_A_OBTENIR)
                .filter(d -> {
                    Long j = joursDansEtape(d.entente(), maintenant);
                    return j != null && j > SEUIL_RETARD_ACCORD_JOURS;
                })
                .count());
        s.getAlertes().setVulnerablesNonPayees(dossiers.stream()
                .filter(this::estVulnerable)
                .filter(d -> !estPaye(d.entente()))
                .count());
        s.getAlertes().setEntentesAValider(parStatut.getOrDefault(StatutEntente.SYNCHRONISEE, 0L));

        return s;
    }

    /** Dossiers d'une étape, du plus ancien dans l'étape au plus récent. */
    public Page<DossierEtapeDto> getDossiers(Long projectId, EtatProcessusEntente etape, String zone, int page, int size) {
        Map<String, PapInfo> papsParCle = indexer(chargerPaps(projectId));
        LocalDateTime maintenant = LocalDateTime.now();

        List<DossierEtapeDto> lignes = chargerDossiers(projectId, papsParCle).stream()
                .filter(d -> etat(d.entente()) == etape)
                .filter(d -> dansZone(d.pap(), zone))
                .map(d -> {
                    PapInfo p = d.pap();
                    return new DossierEtapeDto(
                            d.entente().getId(),
                            p != null && p.codePap() != null ? p.codePap() : d.entente().getCodePap(),
                            p != null ? p.prenom() : null,
                            p != null ? p.nom() : null,
                            categorie(d),
                            estVulnerable(d),
                            p != null ? p.commune() : null,
                            p != null ? p.perteTotale() : null,
                            joursDansEtape(d.entente(), maintenant));
                })
                .sorted(Comparator.comparing(DossierEtapeDto::getJoursEtape,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int taille = Math.max(1, Math.min(size, 200));
        int from = Math.min(Math.max(page, 0) * taille, lignes.size());
        int to = Math.min(from + taille, lignes.size());
        return new PageImpl<>(lignes.subList(from, to), PageRequest.of(Math.max(page, 0), taille), lignes.size());
    }

    // ───────────────────────────── Chargement ─────────────────────────────

    private List<PapInfo> chargerPaps(Long projectId) {
        List<PapInfo> paps = new ArrayList<>();
        placeAffaireRepository.findByProjectId(projectId, TOUT).forEach(p -> paps.add(new PapInfo(
                CAT_PLACE_AFFAIRE, p.getId(), p.getCodePap(), p.getPrenom(), p.getNom(), p.getCommune(),
                estVulnerable(p.getVulnerabilite()), valeur(p.getPerteTotale()))));
        agricoleRepository.findByProjectId(projectId, TOUT).forEach(p -> paps.add(new PapInfo(
                CAT_AGRICOLE, p.getId(), p.getCodePap(), p.getPrenom(), p.getNom(), p.getCommune(),
                estVulnerable(p.getVulnerabilite()), valeur(p.getPerteTotale()))));
        habitatRepository.findByProjectId(projectId, TOUT).forEach(p -> paps.add(new PapInfo(
                CAT_HABITAT, p.getId(), p.getCodePap(), p.getPrenom(), p.getNom(), p.getCommune(),
                estVulnerable(p.getVulnerabilite()), valeur(p.getPerteTotale()))));
        return paps;
    }

    /** PAP agricoles et places d'affaires indexées par « TYPE:id », la clé utilisée par Entente. */
    private Map<String, PapInfo> indexer(List<PapInfo> paps) {
        Map<String, PapInfo> index = new HashMap<>();
        for (PapInfo p : paps) {
            if (CAT_AGRICOLE.equals(p.categorie())) index.put(cle(TypePap.PAPAGRICOLE, p.id()), p);
            else if (CAT_PLACE_AFFAIRE.equals(p.categorie())) index.put(cle(TypePap.PAPPLACEAFFAIRE, p.id()), p);
        }
        return index;
    }

    private List<Dossier> chargerDossiers(Long projectId, Map<String, PapInfo> papsParCle) {
        return ententeRepository.findAllByProjectId(projectId).stream()
                .map(e -> new Dossier(e, papsParCle.get(cle(e.getPapType(), e.getPapId()))))
                .toList();
    }

    // ───────────────────────────── Règles ─────────────────────────────

    /** Même règle que StatsHelper.buildCategoryStats, pour rester cohérent avec la Baseline. */
    static boolean estVulnerable(String vulnerabilite) {
        return vulnerabilite != null && !vulnerabilite.trim().equalsIgnoreCase("Non vulnérable");
    }

    private boolean estVulnerable(Dossier d) {
        return d.pap() != null && d.pap().vulnerable();
    }

    private static boolean estPaye(Entente e) {
        return ETATS_PAYES.contains(etat(e));
    }

    private static EtatProcessusEntente etat(Entente e) {
        return e.getEtatProcessus() != null ? e.getEtatProcessus() : EtatProcessusEntente.COMPENSATION_A_ETABLIR;
    }

    /** Date d'entrée dans l'étape actuelle = date de validation de l'étape précédente. */
    static LocalDateTime dateEntreeEtape(Entente e) {
        LocalDateTime d = switch (etat(e)) {
            case COMPENSATION_A_ETABLIR -> e.getDateCreation();
            case PAP_A_INFORMER -> e.getDateEtablissementCompensation();
            case ACCORD_A_OBTENIR -> e.getDateInformationPap();
            case PAIEMENT_A_EFFECTUER -> e.getDateAccordPap();
            case FORMATION_A_DONNER -> e.getDatePaiement();
            case SUIVI_A_EFFECTUER -> e.getDateFormation();
            case PROCESSUS_TERMINE -> e.getDateSuivi();
        };
        return d != null ? d : e.getDateCreation();
    }

    static Long joursDansEtape(Entente e, LocalDateTime maintenant) {
        LocalDateTime entree = dateEntreeEtape(e);
        return entree != null ? ChronoUnit.DAYS.between(entree, maintenant) : null;
    }

    static boolean dansPeriode(LocalDateTime date, LocalDate debut, LocalDate fin) {
        if (debut == null && fin == null) return true;
        if (date == null) return false;
        LocalDate jour = date.toLocalDate();
        return (debut == null || !jour.isBefore(debut)) && (fin == null || !jour.isAfter(fin));
    }

    private static boolean dansZone(PapInfo p, String zone) {
        if (zone == null || zone.isBlank()) return true;
        return p != null && p.commune() != null && p.commune().trim().equalsIgnoreCase(zone.trim());
    }

    private double montant(Dossier d) {
        return d.pap() != null ? d.pap().perteTotale() : 0;
    }

    private static String categorie(Dossier d) {
        if (d.pap() != null) return d.pap().categorie();
        return d.entente().getPapType() == TypePap.PAPAGRICOLE ? CAT_AGRICOLE : CAT_PLACE_AFFAIRE;
    }

    private static String cle(TypePap type, Long id) {
        return type + ":" + id;
    }

    private static double valeur(Double d) {
        return d != null ? d : 0;
    }

    static String libelle(EtatProcessusEntente e) {
        return switch (e) {
            case COMPENSATION_A_ETABLIR -> "Compensation à établir";
            case PAP_A_INFORMER -> "PAP à informer";
            case ACCORD_A_OBTENIR -> "Accord à obtenir";
            case PAIEMENT_A_EFFECTUER -> "Paiement à effectuer";
            case FORMATION_A_DONNER -> "Formation à donner";
            case SUIVI_A_EFFECTUER -> "Suivi à effectuer";
            case PROCESSUS_TERMINE -> "Processus terminé";
        };
    }

    static String libelleCategorie(String cat) {
        return switch (cat) {
            case CAT_PLACE_AFFAIRE -> "Place affaire / Économique";
            case CAT_AGRICOLE -> "Agricole / Exploitant";
            default -> "Habitat";
        };
    }
}
