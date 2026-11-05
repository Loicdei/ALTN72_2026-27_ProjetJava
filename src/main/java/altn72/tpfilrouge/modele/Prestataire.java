package altn72.tpfilrouge.modele;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "prestataire")
public class Prestataire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prestataire")
    private Long id;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "specialite")
    private String specialite;

    @Column(name = "telephone")
    private String telephone;

    @Column(name = "email")
    private String email;

    @OneToMany(mappedBy = "prestataire")
    private List<Evaluation> evaluations;

    public Prestataire() {}

    public Prestataire(String nom, String specialite, String telephone, String email) {
        this.nom = nom;
        this.specialite = specialite;
        this.telephone = telephone;
        this.email = email;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public List<Evaluation> getEvaluations() { return evaluations; }
    public void setEvaluations(List<Evaluation> evaluations) { this.evaluations = evaluations; }
}