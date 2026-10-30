package altn72.tpfilrouge.repository;

import altn72.tpfilrouge.modele.Equipement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EquipementRepository extends JpaRepository<Equipement, Long> {

    List<Equipement> findByStationId(Long stationId);

    Optional<Equipement> findByReference(String reference);
}