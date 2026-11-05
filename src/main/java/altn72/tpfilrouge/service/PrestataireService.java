package altn72.tpfilrouge.service;

import altn72.tpfilrouge.exception.RessourceIntrouvableException;
import altn72.tpfilrouge.modele.NotePrestataire;
import altn72.tpfilrouge.modele.Prestataire;
import altn72.tpfilrouge.repository.PrestataireRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrestataireService {

    private final PrestataireRepository prestataireRepository;

    public PrestataireService(PrestataireRepository prestataireRepository) {
        this.prestataireRepository = prestataireRepository;
    }

    public List<Prestataire> obtenirTousLesPrestataires() {
        return prestataireRepository.findAll();
    }

    public List<NotePrestataire> obtenirNotesDesPrestataires() {
        return prestataireRepository.calculerNotesDesPrestataires();
    }

    public Prestataire obtenirParId(Long id) {
        return prestataireRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Le prestataire n° " + id + " n'existe pas."));
    }
}
