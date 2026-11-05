package altn72.tpfilrouge.modele;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "incident")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_incident")
    private Long id;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_heure_ouverture", nullable = false)
    private LocalDateTime dateHeureOuverture;

    @Column(name = "statut", nullable = false, length = 50)
    private String statut;

    @Column(name = "date_heure_resolution")
    private LocalDateTime dateHeureResolution;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_equipement", nullable = false)
    private Equipement equipement;

    @ManyToOne
    @JoinColumn(name = "id_prestataire")
    private Prestataire prestataire;

    public Incident() {}

    public Incident(String description, LocalDateTime dateHeureOuverture, String statut, Equipement equipement) {
        this.description = description;
        this.dateHeureOuverture = dateHeureOuverture;
        this.statut = statut;
        this.equipement = equipement;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getDateHeureOuverture() { return dateHeureOuverture; }
    public void setDateHeureOuverture(LocalDateTime dateHeureOuverture) { this.dateHeureOuverture = dateHeureOuverture; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getDateHeureResolution() { return dateHeureResolution; }
    public void setDateHeureResolution(LocalDateTime dateHeureResolution) { this.dateHeureResolution = dateHeureResolution; }

    public Equipement getEquipement() { return equipement; }
    public void setEquipement(Equipement equipement) { this.equipement = equipement; }

    public Prestataire getPrestataire() { return prestataire; }
    public void setPrestataire(Prestataire prestataire) { this.prestataire = prestataire; }
}