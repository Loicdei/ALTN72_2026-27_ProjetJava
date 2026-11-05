package altn72.tpfilrouge.modele;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "evaluation")
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evaluation")
    private Long id;

    @Column(name = "note", nullable = false)
    private Integer note; // ex: 1 à 5 ou sur 20

    @Column(name = "commentaire")
    private String commentaire;

    @Column(name = "date_evaluation")
    private LocalDate dateEvaluation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_prestataire", nullable = false)
    private Prestataire prestataire;

    @OneToOne
    @JoinColumn(name = "id_incident")
    private Incident incident;

    public Evaluation() {}

    public Evaluation(Integer note, String commentaire, LocalDate dateEvaluation, Prestataire prestataire) {
        this.note = note;
        this.commentaire = commentaire;
        this.dateEvaluation = dateEvaluation;
        this.prestataire = prestataire;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getNote() { return note; }
    public void setNote(Integer note) { this.note = note; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public LocalDate getDateEvaluation() { return dateEvaluation; }
    public void setDateEvaluation(LocalDate dateEvaluation) { this.dateEvaluation = dateEvaluation; }

    public Prestataire getPrestataire() { return prestataire; }
    public void setPrestataire(Prestataire prestataire) { this.prestataire = prestataire; }

    public Incident getIncident() { return incident; }
    public void setIncident(Incident incident) { this.incident = incident; }
}