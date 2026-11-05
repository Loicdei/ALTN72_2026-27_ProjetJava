package altn72.tpfilrouge.service;

import altn72.tpfilrouge.repository.IncidentRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class EquipementFragiliteService {

    private static final long SEUIL_INCIDENTS = 3;
    private static final Duration FENETRE = Duration.ofDays(30);

    private final IncidentRepository incidentRepository;
    private final Clock horloge;

    public EquipementFragiliteService(IncidentRepository incidentRepository, Clock horloge) {
        this.incidentRepository = incidentRepository;
        this.horloge = horloge;
    }

    public boolean estFragile(Long equipementId) {
        return obtenirEquipementsFragiles().contains(equipementId);
    }

    public Set<Long> obtenirEquipementsFragiles() {
        LocalDateTime debutFenetre = LocalDateTime.now(horloge).minus(FENETRE);
        return incidentRepository.trouverEquipementsAvecAuMoinsNIncidentsDepuis(debutFenetre, SEUIL_INCIDENTS);
    }
}
