package altn72.tpfilrouge.modele;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "date_heure_ouverture", nullable = false)
    private LocalDateTime dateHeureOuverture;

    @Column(name = "date_heure_resolution")
    private LocalDateTime dateHeureResolution;

    @Column(name = "statut", nullable = false)
    private String statut;

    @ManyToOne(optional = false)
    @JoinColumn(name = "equipement_id", nullable = false)
    private Equipement equipement;

    @ManyToOne
    @JoinColumn(name = "prestataire_id")
    private Prestataire prestataire;

    public Incident() {
    }

    public Incident(String description, LocalDateTime dateHeureOuverture, String statut, Equipement equipement) {
        this.description = description;
        this.dateHeureOuverture = dateHeureOuverture;
        this.statut = statut;
        this.equipement = equipement;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateHeureOuverture() {
        return dateHeureOuverture;
    }

    public void setDateHeureOuverture(LocalDateTime dateHeureOuverture) {
        this.dateHeureOuverture = dateHeureOuverture;
    }

    public LocalDateTime getDateHeureResolution() {
        return dateHeureResolution;
    }

    public void setDateHeureResolution(LocalDateTime dateHeureResolution) {
        this.dateHeureResolution = dateHeureResolution;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Equipement getEquipement() {
        return equipement;
    }

    public void setEquipement(Equipement equipement) {
        this.equipement = equipement;
    }

    public Prestataire getPrestataire() {
        return prestataire;
    }

    public void setPrestataire(Prestataire prestataire) {
        this.prestataire = prestataire;
    }
}