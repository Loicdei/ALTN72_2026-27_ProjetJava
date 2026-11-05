package altn72.tpfilrouge.service;

import altn72.tpfilrouge.modele.Station;
import altn72.tpfilrouge.repository.StationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StationService {

    private final StationRepository stationRepository;

    public StationService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    public List<Station> obtenirToutesLesStations() {
        return stationRepository.findAll();
    }
}