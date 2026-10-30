package altn72.tpfilrouge.modele;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "evaluation")
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "note", nullable = false)
    private Integer note;

    @Column(name = "commentaire", length = 500)
    private String commentaire;

    @Column(name = "date_evaluation", nullable = false)
    private LocalDate dateEvaluation;

    @OneToOne(optional = false)
    @JoinColumn(name = "incident_id", nullable = false, unique = true)
    private Incident incident;

    public Evaluation() {
    }

    public Evaluation(Integer note, String commentaire, LocalDate dateEvaluation, Incident incident) {
        this.note = note;
        this.commentaire = commentaire;
        this.dateEvaluation = dateEvaluation;
        this.incident = incident;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getNote() {
        return note;
    }

    public void setNote(Integer note) {
        this.note = note;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public LocalDate getDateEvaluation() {
        return dateEvaluation;
    }

    public void setDateEvaluation(LocalDate dateEvaluation) {
        this.dateEvaluation = dateEvaluation;
    }

    public Incident getIncident() {
        return incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
    }
}