package altn72.tpfilrouge.repository;

import altn72.tpfilrouge.modele.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    Optional<Evaluation> findByIncidentId(Long incidentId);
}