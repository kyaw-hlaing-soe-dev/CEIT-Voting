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
        @UniqueConstraint(name = "uk_voter_category", columnNames = {"voter_id", "category"}),
        @UniqueConstraint(name = "uk_ip_cookie", columnNames = {"ip_address", "voter_cookie_id"})
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


    @Column(name = "candidate_number")
    private Integer candidateNumber;

    // Vote weight based on user role (1 for USER, 2 for ADMIN)
    @Column(name = "weight", nullable = false)
    private Integer weight = 1;

    // New fields for deduplication by IP and cookie
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "voter_cookie_id", length = 255)
    private String voterCookieId;

    public Vote() {
    }

    public Vote(Voter voter, Candidate candidate, Category category) {
        this.voter = voter;
        this.candidate = candidate;
        this.category = category;
        // ensure candidateNumber is populated when a Candidate is provided
        this.candidateNumber = candidate != null ? candidate.getCandidateNumber() : null;
        // Set weight based on voter's role
        this.weight = voter != null && voter.getUserRole() != null ? voter.getUserRole().getVoteWeight() : 1;
    }

    public Long getId() {
        return id;
    }

    public Voter getVoter() {
        return voter;
    }

    public void setVoter(Voter voter) {
        this.voter = voter;
        // Update weight when voter is set
        if (voter != null && voter.getUserRole() != null) {
            this.weight = voter.getUserRole().getVoteWeight();
        }
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

    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }

    // New getters/setters
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getVoterCookieId() { return voterCookieId; }
    public void setVoterCookieId(String voterCookieId) { this.voterCookieId = voterCookieId; }

}
