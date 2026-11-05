package altn72.tpfilrouge.repository;

import altn72.tpfilrouge.modele.NotePrestataire;
import altn72.tpfilrouge.modele.Prestataire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PrestataireRepository extends JpaRepository<Prestataire, Long> {

    // Les jointures externes conservent les prestataires qui n'ont encore reçu aucune évaluation.
    @Query("""
            select new altn72.tpfilrouge.modele.NotePrestataire(p, avg(e.note), count(e))
            from Prestataire p
            left join Incident i on i.prestataire = p
            left join Evaluation e on e.incident = i
            group by p
            order by p.nom
            """)
    List<NotePrestataire> calculerNotesDesPrestataires();
}