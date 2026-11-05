package altn72.tpfilrouge.repository;

import altn72.tpfilrouge.modele.Equipement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface EquipementRepository extends JpaRepository<Equipement, Long> {

    List<Equipement> findByStationId(Long stationId);

    boolean existsByReference(String reference);

    @Query("select distinct e.type from Equipement e order by e.type")
    List<String> trouverTypes();

}