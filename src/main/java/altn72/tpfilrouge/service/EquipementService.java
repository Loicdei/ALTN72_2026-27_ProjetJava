package altn72.tpfilrouge.service;

import altn72.tpfilrouge.modele.Equipement;
import altn72.tpfilrouge.repository.EquipementRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EquipementService {

    private final EquipementRepository equipementRepository;

    public EquipementService(EquipementRepository equipementRepository) {
        this.equipementRepository = equipementRepository;
    }

    public List<Equipement> obtenirEquipementsParStation(Long stationId) {
        return equipementRepository.findByStationId(stationId);
    }

    public List<Equipement> obtenirTousLesEquipements() {
        return equipementRepository.findAll();
    }
}