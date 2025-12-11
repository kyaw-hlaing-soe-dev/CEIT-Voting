package com.KTU.KTUVotingapp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "candidates", indexes = {
    @Index(name = "idx_category_number", columnList = "category, candidate_number")
})
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Column(nullable = false, name = "candidate_number")
    private Integer candidateNumber;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String department;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "vote_count", nullable = false)
    private Long voteCount = 0L;

    public Candidate() {
    }

    public Candidate(Category category, Integer candidateNumber, String name, String department, String imageUrl) {
        this.category = category;
        this.candidateNumber = candidateNumber;
        this.name = name;
        this.department = department;
        this.imageUrl = imageUrl;
        this.voteCount = 0L;
    }

    public Long getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Integer getCandidateNumber() {
        return candidateNumber;
    }

    public void setCandidateNumber(Integer candidateNumber) {
        this.candidateNumber = candidateNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Long getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(Long voteCount) {
        this.voteCount = voteCount;
    }

    public void incrementVoteCount() {
        this.voteCount++;
    }
}

