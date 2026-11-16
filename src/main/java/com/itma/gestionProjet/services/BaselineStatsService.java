package com.itma.gestionProjet.services;

import com.itma.gestionProjet.dtos.BaselineStats;
import com.itma.gestionProjet.dtos.CategoryStats;
import com.itma.gestionProjet.entities.Entente;
import com.itma.gestionProjet.entities.EtatProcessusEntente;
import com.itma.gestionProjet.entities.TypePap;
import com.itma.gestionProjet.helpers.StatsHelper;
import com.itma.gestionProjet.repositories.DatabasePapAgricoleRepository;
import com.itma.gestionProjet.repositories.DatabasePapHabitatRepository;
import com.itma.gestionProjet.repositories.DatabasePapPlaceAffaireRepository;
import com.itma.gestionProjet.repositories.EntenteRepository;
import com.itma.gestionProjet.repositories.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Calculs du tableau de bord « BaseLine PAR ». Lecture seule. Voir {@link BaselineStats}. */
@Service
@RequiredArgsConstructor
public class BaselineStatsService {

    private static final PageRequest TOUT = PageRequest.of(0, 10_000_000);

    /** Étapes où la compensation est établie mais pas encore payée. */
    private static final Set<EtatProcessusEntente> ETATS_EN_ETUDE = EnumSet.of(
            EtatProcessusEntente.PAP_A_INFORMER,
            EtatProcessusEntente.ACCORD_A_OBTENIR,
            EtatProcessusEntente.PAIEMENT_A_EFFECTUER
    );

    private static final Pattern POINT = Pattern.compile("point\\s*\\(\\s*(-?\\d+(?:\\.\\d+)?)[\\s,]+(-?\\d+(?:\\.\\d+)?)\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAIRE = Pattern.compile("(-?\\d+(?:\\.\\d+)?)\\s+(-?\\d+(?:\\.\\d+)?)");

    private final StatsHelper statsHelper;
    private final ProjectRepository projectRepository;
    private final EntenteRepository ententeRepository;
    private final DatabasePapAgricoleRepository agricoleRepository;
    private final DatabasePapPlaceAffaireRepository placeAffaireRepository;
    private final DatabasePapHabitatRepository habitatRepository;

    /** Vue commune d'une PAP, quelle que soit sa catégorie. */
    record Pap(String categorie, TypePap typeEntente, Long id, String codePap, String prenom, String nom,
               String sexe, String vulnerabilite, String commune, String statutPap,
               String geometrie, double perteTotale) {}

    public BaselineStats getStats(Long projectId, String zone, String categorie) {
        List<Pap> toutes = chargerPaps(projectId);
        Map<String, Entente> ententes = new HashMap<>();
        for (Entente e : ententeRepository.findAllByProjectId(projectId)) {
            ententes.put(e.getPapType() + ":" + e.getPapId(), e);
        }

        List<Pap> paps = toutes.stream()
                .filter(p -> zone == null || zone.isBlank()
                        || (p.commune() != null && p.commune().trim().equalsIgnoreCase(zone.trim())))
                .filter(p -> categorie == null || categorie.isBlank() || categorie.equals(p.categorie()))
                .toList();

        BaselineStats s = new BaselineStats();
        s.setDateMaj(LocalDateTime.now());
        projectRepository.findById(projectId).ifPresent(p -> s.setStatutProjet(p.getStatus()));
        s.setZones(toutes.stream().map(Pap::commune).filter(c -> c != null && !c.isBlank())
                .map(String::trim).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList());
        s.setNbZones(paps.stream().map(Pap::commune).filter(c -> c != null && !c.isBlank())
                .map(c -> c.trim().toLowerCase()).distinct().count());

        s.setPlaceAffaire(stats(paps, MiseEnOeuvreStatsService.CAT_PLACE_AFFAIRE));
        s.setAgricole(stats(paps, MiseEnOeuvreStatsService.CAT_AGRICOLE));
        s.setHabitat(stats(paps, MiseEnOeuvreStatsService.CAT_HABITAT));
        s.setTotal(stats(paps, null));

        for (Pap p : paps) {
            Entente e = p.typeEntente() != null ? ententes.get(p.typeEntente() + ":" + p.id()) : null;
            String statut = statut(p, e);
            switch (statut) {
                case "terminee" -> s.getAvancement().setTerminee(s.getAvancement().getTerminee() + 1);
                case "en_etude" -> s.getAvancement().setEnEtude(s.getAvancement().getEnEtude() + 1);
                case "recense" -> s.getAvancement().setRecense(s.getAvancement().getRecense() + 1);
                default -> s.getAvancement().setInconnu(s.getAvancement().getInconnu() + 1);
            }
            double[] pos = position(p.geometrie());
            if (pos != null) {
                s.getPoints().add(new BaselineStats.PointPap(p.codePap(), p.prenom(), p.nom(), p.categorie(),
                        statut, p.commune(), pos[0], pos[1]));
            }
        }
        return s;
    }

    // ───────────────────────────── Règles ─────────────────────────────

    private CategoryStats stats(List<Pap> paps, String categorie) {
        List<Pap> liste = categorie == null ? paps
                : paps.stream().filter(p -> categorie.equals(p.categorie())).toList();
        double perte = liste.stream().mapToDouble(Pap::perteTotale).sum();
        return statsHelper.buildCategoryStats(liste, Pap::sexe, Pap::vulnerabilite, perte);
    }

    static String statut(Pap p, Entente e) {
        if (e != null && e.getEtatProcessus() != null) {
            if (MiseEnOeuvreStatsService.ETATS_PAYES.contains(e.getEtatProcessus())) return "terminee";
            if (ETATS_EN_ETUDE.contains(e.getEtatProcessus())) return "en_etude";
        }
        String sp = p.statutPap() == null ? "" : p.statutPap().trim().toLowerCase()
                .replace('é', 'e').replace(' ', '_');
        return switch (sp) {
            case "indemnisation_terminee", "terminee" -> "terminee";
            case "en_etude" -> "en_etude";
            case "recense" -> "recense";
            default -> "inconnu";
        };
    }

    /**
     * Position [lat, lng] d'une PAP à partir de pointGeometriques (WKT, longitude en premier) :
     * « POINT(lng lat) » (espace ou virgule) ou centre d'un POLYGON / MULTIPOLYGON (parcelles agricoles).
     */
    static double[] position(String wkt) {
        if (wkt == null || wkt.isBlank()) return null;
        Matcher m = POINT.matcher(wkt);
        if (m.find()) return valide(Double.parseDouble(m.group(2)), Double.parseDouble(m.group(1)));
        if (!wkt.toLowerCase().contains("polygon")) return null;
        Matcher pm = PAIRE.matcher(wkt);
        double sLat = 0, sLng = 0;
        int n = 0;
        while (pm.find()) {
            sLng += Double.parseDouble(pm.group(1));
            sLat += Double.parseDouble(pm.group(2));
            n++;
        }
        return n > 0 ? valide(sLat / n, sLng / n) : null;
    }

    private static double[] valide(double lat, double lng) {
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180 || (lat == 0 && lng == 0)) return null;
        return new double[]{lat, lng};
    }

    private List<Pap> chargerPaps(Long projectId) {
        List<Pap> paps = new ArrayList<>();
        placeAffaireRepository.findByProjectId(projectId, TOUT).forEach(p -> paps.add(new Pap(
                MiseEnOeuvreStatsService.CAT_PLACE_AFFAIRE, TypePap.PAPPLACEAFFAIRE, p.getId(), p.getCodePap(),
                p.getPrenom(), p.getNom(), p.getSexe(), p.getVulnerabilite(), p.getCommune(), p.getStatutPap(),
                p.getPointGeometriques(), valeur(p.getPerteTotale()))));
        agricoleRepository.findByProjectId(projectId, TOUT).forEach(p -> paps.add(new Pap(
                MiseEnOeuvreStatsService.CAT_AGRICOLE, TypePap.PAPAGRICOLE, p.getId(), p.getCodePap(),
                p.getPrenom(), p.getNom(), p.getSexe(), p.getVulnerabilite(), p.getCommune(), p.getStatutPap(),
                p.getPointGeometriques(), valeur(p.getPerteTotale()))));
        habitatRepository.findByProjectId(projectId, TOUT).forEach(p -> paps.add(new Pap(
                MiseEnOeuvreStatsService.CAT_HABITAT, null, p.getId(), p.getCodePap(),
                p.getPrenom(), p.getNom(), p.getSexe(), p.getVulnerabilite(), p.getCommune(), p.getStatutPap(),
                p.getPointGeometriques(), valeur(p.getPerteTotale()))));
        return paps;
    }

    private static double valeur(Double d) {
        return d != null ? d : 0;
    }
}
