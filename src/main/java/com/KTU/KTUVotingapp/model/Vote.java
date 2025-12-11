package com.KTU.KTUVotingapp.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "votes",
    indexes = {
        @Index(name = "idx_voter_id", columnList = "voter_id"),
        @Index(name = "idx_category", columnList = "category"),
        @Index(name = "idx_candidate_id", columnList = "candidate_id"),
        @Index(name = "idx_voter_category", columnList = "voter_id, category")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_voter_category", columnNames = {"voter_id", "category"})
    }
)
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "voter_id", nullable = false)
    private Voter voter;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Vote() {
    }

    public Vote(Voter voter, Candidate candidate, Category category) {
        this.voter = voter;
        this.candidate = candidate;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public Voter getVoter() {
        return voter;
    }

    public void setVoter(Voter voter) {
        this.voter = voter;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
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
}


