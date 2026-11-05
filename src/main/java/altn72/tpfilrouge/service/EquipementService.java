package altn72.tpfilrouge.service;

import altn72.tpfilrouge.modele.Equipement;
import altn72.tpfilrouge.repository.EquipementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EquipementService {

    @Autowired
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

    public Equipement obtenirParId(Long id) {
        return equipementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Équipement introuvable avec l'ID : " + id));
    }
}