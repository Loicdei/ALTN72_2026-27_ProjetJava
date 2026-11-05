package altn72.tpfilrouge.service;

import altn72.tpfilrouge.modele.Incident;
import altn72.tpfilrouge.repository.IncidentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IncidentService {

    @Autowired
    private IncidentRepository incidentRepository;

    public List<Incident> obtenirIncidentsParEquipement(Long equipementId) {
        return incidentRepository.findByEquipementIdOrderByDateHeureOuvertureDesc(equipementId);
    }
}