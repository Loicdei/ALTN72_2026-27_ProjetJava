package altn72.tpfilrouge.modele;

import jakarta.persistence.*;

@Entity
@Table(name = "equipement")
public class Equipement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_equipement")
    private Long id;

    @Column(name = "reference", nullable = false, unique = true)
    private String reference;

    @Column(name = "libelle", nullable = false)
    private String libelle;

    @Column(name = "type_equipement", nullable = false)
    private String type;

    @Column(name = "etat_fonctionnement", nullable = false)
    private String etatFonctionnement;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_station", nullable = false)
    private Station station;

    public Equipement() {}

    public Equipement(String reference, String libelle, String type, String etatFonctionnement, Station station) {
        this.reference = reference;
        this.libelle = libelle;
        this.type = type;
        this.etatFonctionnement = etatFonctionnement;
        this.station = station;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getEtatFonctionnement() { return etatFonctionnement; }
    public void setEtatFonctionnement(String etatFonctionnement) { this.etatFonctionnement = etatFonctionnement; }

    public Station getStation() { return station; }
    public void setStation(Station station) { this.station = station; }
}