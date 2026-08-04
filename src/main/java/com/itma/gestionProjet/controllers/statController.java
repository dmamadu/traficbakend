package com.itma.gestionProjet.controllers;

import com.itma.gestionProjet.dtos.AvancementStats;
import com.itma.gestionProjet.dtos.CategoryStats;
import com.itma.gestionProjet.dtos.CombinedStatsResponse;
import com.itma.gestionProjet.dtos.CriterionStats;
import com.itma.gestionProjet.dtos.DossiersStats;
import com.itma.gestionProjet.entities.EtatProcessusEntente;
import com.itma.gestionProjet.entities.StatutEntente;
import com.itma.gestionProjet.helpers.StatsHelper;
import com.itma.gestionProjet.repositories.EntenteRepository;
import com.itma.gestionProjet.services.DatabasePapAgricoleService;
import com.itma.gestionProjet.services.DatabasePapPlaceAffaireService;
import com.itma.gestionProjet.services.DatabasePapHabitatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stats")
public class statController {

    @Autowired
    private StatsHelper statsHelper;

    @Autowired
    private DatabasePapPlaceAffaireService placeAffaireService;

    @Autowired
    private DatabasePapAgricoleService agricoleService;

    @Autowired
    private DatabasePapHabitatService habitatService;

    @Autowired
    private EntenteRepository ententeRepository;

    // Alimente à la fois /dashboards/jobs (DASHBOARD_BASELINE) et /dashboards/miseEnOeuvre
    // (DASHBOARD_MISE_EN_OEUVRE) — accessible dès qu'on a le droit de voir l'un des deux.
    @PreAuthorize("@permissionChecker.has(#projectId, 'DASHBOARD_BASELINE_VOIR') or @permissionChecker.has(#projectId, 'DASHBOARD_MISE_EN_OEUVRE_VOIR')")
    @GetMapping("/combine")
    public CombinedStatsResponse getCombinedStats(@RequestParam(required = false) Long projectId) {

        CategoryStats paStats  = placeAffaireService.getCategoryStats(projectId);
        CategoryStats agStats  = agricoleService.getCategoryStats(projectId);
        CategoryStats habStats = habitatService.getCategoryStats(projectId);

        CategoryStats totalStats = buildTotal(paStats, agStats, habStats);

        CombinedStatsResponse response = new CombinedStatsResponse();
        response.setPlaceAffaireStats(paStats);
        response.setAgricoleStats(agStats);
        response.setHabitatStats(habStats);
        response.setTotalStats(totalStats);
        response.setSummary(statsHelper.buildSummary(paStats, agStats, habStats));
        response.setDossiersStats(buildDossiersStats(projectId));

        return response;
    }

    // Recalcule le champ `vulnerabilite` de tous les PAP existants (3 catégories) avec la logique
    // actuelle de determinerVulnerabilite(). À lancer une fois après un changement de cette logique
    // pour que les PAP déjà en base reflètent la nouvelle règle.
    // Opération de maintenance globale (toutes catégories, tous projets confondus) : aucun code de
    // permission du catalogue ne correspond à ce périmètre, réservée à Super Admin/Admin.
    @PreAuthorize("hasAnyAuthority('Super Admin', 'Admin')")
    @PostMapping("/recalculer-vulnerabilites")
    public Map<String, Long> recalculerVulnerabilites() {
        long paCount = placeAffaireService.recalculerVulnerabilites();
        long agCount = agricoleService.recalculerVulnerabilites();
        long habCount = habitatService.recalculerVulnerabilites();

        Map<String, Long> result = new LinkedHashMap<>();
        result.put("placeAffaire", paCount);
        result.put("agricole", agCount);
        result.put("habitat", habCount);
        return result;
    }

    @PreAuthorize("@permissionChecker.has(#projectId, 'DASHBOARD_MISE_EN_OEUVRE_VOIR')")
    @GetMapping("/avancement")
    public AvancementStats getAvancement(@RequestParam(required = false) Long projectId) {
        AvancementStats stats = new AvancementStats();
        if (projectId != null) {
            stats.setBrouillon(ententeRepository.countByProjectIdAndStatut(projectId, StatutEntente.BROUILLON));
            stats.setSynchronisee(ententeRepository.countByProjectIdAndStatut(projectId, StatutEntente.SYNCHRONISEE));
            stats.setFinalisee(ententeRepository.countByProjectIdAndStatut(projectId, StatutEntente.FINALISEE));
            stats.setCompensationAEtablir(ententeRepository.countByProjectIdAndEtatProcessus(projectId, EtatProcessusEntente.COMPENSATION_A_ETABLIR));
            stats.setPapAInformer(ententeRepository.countByProjectIdAndEtatProcessus(projectId, EtatProcessusEntente.PAP_A_INFORMER));
            stats.setAccordAObtenir(ententeRepository.countByProjectIdAndEtatProcessus(projectId, EtatProcessusEntente.ACCORD_A_OBTENIR));
            stats.setPaiementAEffectuer(ententeRepository.countByProjectIdAndEtatProcessus(projectId, EtatProcessusEntente.PAIEMENT_A_EFFECTUER));
            stats.setFormationADonner(ententeRepository.countByProjectIdAndEtatProcessus(projectId, EtatProcessusEntente.FORMATION_A_DONNER));
            stats.setSuiviAEffectuer(ententeRepository.countByProjectIdAndEtatProcessus(projectId, EtatProcessusEntente.SUIVI_A_EFFECTUER));
            stats.setProcessusTermine(ententeRepository.countByProjectIdAndEtatProcessus(projectId, EtatProcessusEntente.PROCESSUS_TERMINE));
        } else {
            stats.setBrouillon(ententeRepository.countByStatut(StatutEntente.BROUILLON));
            stats.setSynchronisee(ententeRepository.countByStatut(StatutEntente.SYNCHRONISEE));
            stats.setFinalisee(ententeRepository.countByStatut(StatutEntente.FINALISEE));
            stats.setCompensationAEtablir(ententeRepository.countByEtatProcessus(EtatProcessusEntente.COMPENSATION_A_ETABLIR));
            stats.setPapAInformer(ententeRepository.countByEtatProcessus(EtatProcessusEntente.PAP_A_INFORMER));
            stats.setAccordAObtenir(ententeRepository.countByEtatProcessus(EtatProcessusEntente.ACCORD_A_OBTENIR));
            stats.setPaiementAEffectuer(ententeRepository.countByEtatProcessus(EtatProcessusEntente.PAIEMENT_A_EFFECTUER));
            stats.setFormationADonner(ententeRepository.countByEtatProcessus(EtatProcessusEntente.FORMATION_A_DONNER));
            stats.setSuiviAEffectuer(ententeRepository.countByEtatProcessus(EtatProcessusEntente.SUIVI_A_EFFECTUER));
            stats.setProcessusTermine(ententeRepository.countByEtatProcessus(EtatProcessusEntente.PROCESSUS_TERMINE));
        }
        return stats;
    }

    private DossiersStats buildDossiersStats(Long projectId) {
        List<EtatProcessusEntente> etatsPayees = List.of(
            EtatProcessusEntente.FORMATION_A_DONNER,
            EtatProcessusEntente.SUIVI_A_EFFECTUER,
            EtatProcessusEntente.PROCESSUS_TERMINE
        );

        DossiersStats stats = new DossiersStats();
        if (projectId != null) {
            stats.setEntentesSignees(ententeRepository.countByProjectIdAndStatut(projectId, StatutEntente.FINALISEE));
            stats.setDossiersTransmis(ententeRepository.countByProjectIdAndEtatProcessus(projectId, EtatProcessusEntente.PAIEMENT_A_EFFECTUER));
            stats.setPapPayees(ententeRepository.countByProjectIdAndEtatProcessusIn(projectId, etatsPayees));
        } else {
            stats.setEntentesSignees(ententeRepository.countByStatut(StatutEntente.FINALISEE));
            stats.setDossiersTransmis(ententeRepository.countByEtatProcessus(EtatProcessusEntente.PAIEMENT_A_EFFECTUER));
            stats.setPapPayees(ententeRepository.countByEtatProcessusIn(etatsPayees));
        }
        return stats;
    }

    private CategoryStats buildTotal(CategoryStats... stats) {
        CategoryStats total = new CategoryStats();
        total.setTotal(Arrays.stream(stats).mapToLong(CategoryStats::getTotal).sum());
        total.setHommes(Arrays.stream(stats).mapToLong(CategoryStats::getHommes).sum());
        total.setFemmes(Arrays.stream(stats).mapToLong(CategoryStats::getFemmes).sum());
        total.setAutre(Arrays.stream(stats).mapToLong(CategoryStats::getAutre).sum());
        total.setTotalVulnerables(Arrays.stream(stats).mapToLong(CategoryStats::getTotalVulnerables).sum());
        total.setVulnerablesHommes(Arrays.stream(stats).mapToLong(CategoryStats::getVulnerablesHommes).sum());
        total.setVulnerablesFemmes(Arrays.stream(stats).mapToLong(CategoryStats::getVulnerablesFemmes).sum());
        total.setTotalPerte(Arrays.stream(stats).mapToDouble(CategoryStats::getTotalPerte).sum());
        long t = total.getTotal();
        total.setPercentHommes(t > 0 ? (total.getHommes() * 100.0 / t) : 0);
        total.setPercentFemmes(t > 0 ? (total.getFemmes() * 100.0 / t) : 0);
        Map<String, CriterionStats> criteresTotal = new LinkedHashMap<>();
        stats[0].getCriteresVulnerabilite().forEach((key, val) -> {
            long th = Arrays.stream(stats).mapToLong(s -> s.getCriteresVulnerabilite().get(key).getHommes()).sum();
            long tf = Arrays.stream(stats).mapToLong(s -> s.getCriteresVulnerabilite().get(key).getFemmes()).sum();
            long ta = Arrays.stream(stats).mapToLong(s -> s.getCriteresVulnerabilite().get(key).getAutre()).sum();
            criteresTotal.put(key, new CriterionStats(th + tf + ta, th, tf, ta));
        });
        total.setCriteresVulnerabilite(criteresTotal);
        return total;
    }
}
