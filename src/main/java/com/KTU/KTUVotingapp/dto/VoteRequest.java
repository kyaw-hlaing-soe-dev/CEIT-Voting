package com.KTU.KTUVotingapp.dto;

import jakarta.validation.constraints.NotNull;
import com.KTU.KTUVotingapp.model.Category;

public class VoteRequest {

    @NotNull(message = "Category is required")
    private Category category;

    @NotNull(message = "Candidate number is required")
    private Integer candidateNumber;

    public VoteRequest() {
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
}
