package altn72.tpfilrouge.modele;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "incident")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_incident")
    private Long id;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "date_declaration", nullable = false)
    private LocalDate dateDeclaration;

    @Column(name = "statut", nullable = false)
    private String statut; // ex: DECLARED, IN_PROGRESS, RESOLVED

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_equipement", nullable = false)
    private Equipement equipement;

    @ManyToOne
    @JoinColumn(name = "id_prestataire")
    private Prestataire prestataire;

    public Incident() {}

    public Incident(String description, LocalDate dateDeclaration, String statut, Equipement equipement) {
        this.description = description;
        this.dateDeclaration = dateDeclaration;
        this.statut = statut;
        this.equipement = equipement;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDateDeclaration() { return dateDeclaration; }
    public void setDateDeclaration(LocalDate dateDeclaration) { this.dateDeclaration = dateDeclaration; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public Equipement getEquipement() { return equipement; }
    public void setEquipement(Equipement equipement) { this.equipement = equipement; }

    public Prestataire getPrestataire() { return prestataire; }
    public void setPrestataire(Prestataire prestataire) { this.prestataire = prestataire; }
}