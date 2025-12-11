package com.KTU.KTUVotingapp.dto;

import com.KTU.KTUVotingapp.model.Category;

public class CandidateDTO {

    private Long id;
    private Category category;
    private Integer candidateNumber;
    private String name;
    private String department;
    private String imageUrl;
    private Long voteCount;

    public CandidateDTO() {
    }

    public CandidateDTO(Long id, Category category, Integer candidateNumber, String name, 
                       String department, String imageUrl, Long voteCount) {
        this.id = id;
        this.category = category;
        this.candidateNumber = candidateNumber;
        this.name = name;
        this.department = department;
        this.imageUrl = imageUrl;
        this.voteCount = voteCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}

