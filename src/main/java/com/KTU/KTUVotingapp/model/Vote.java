package com.KTU.KTUVotingapp.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "votes",
    indexes = {
        @Index(name = "idx_pin_id", columnList = "pin_id"),
        @Index(name = "idx_category", columnList = "category"),
        @Index(name = "idx_candidate_id", columnList = "candidate_id"),
        @Index(name = "idx_pin_category", columnList = "pin_id, category")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_pin_category", columnNames = {"pin_id", "category"})
    }
)
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "pin_id", nullable = true)
    private VoterPin voterPin;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();


    @Column(name = "candidate_number")
    private Integer candidateNumber;

    public Vote() {
    }

    public Vote(VoterPin voterPin, Candidate candidate, Category category) {
        this.voterPin = voterPin;
        this.candidate = candidate;
        this.category = category;
        // ensure candidateNumber is populated when a Candidate is provided
        this.candidateNumber = candidate != null ? candidate.getCandidateNumber() : null;
    }

    public Long getId() {
        return id;
    }

    public VoterPin getVoterPin() {
        return voterPin;
    }

    public void setVoterPin(VoterPin voterPin) {
        this.voterPin = voterPin;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
        if (candidate != null) {
            this.candidateNumber = candidate.getCandidateNumber();
        }
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Integer getCandidateNumber() { return candidateNumber; }
    public void setCandidateNumber(Integer candidateNumber) { this.candidateNumber = candidateNumber; }


}
