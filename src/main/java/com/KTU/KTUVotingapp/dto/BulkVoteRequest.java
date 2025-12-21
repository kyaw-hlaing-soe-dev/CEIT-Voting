package com.KTU.KTUVotingapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class BulkVoteRequest {

    @NotEmpty(message = "At least one vote is required")
    @Valid
    private List<VoteItem> votes;

    public BulkVoteRequest() {
    }

    public List<VoteItem> getVotes() {
        return votes;
    }

    public void setVotes(List<VoteItem> votes) {
        this.votes = votes;
    }

    public static class VoteItem {
        @NotNull(message = "Category is required")
        private com.KTU.KTUVotingapp.model.Category category;

        @NotNull(message = "Candidate number is required")
        private Integer candidateNumber;

        public VoteItem() {
        }

        public com.KTU.KTUVotingapp.model.Category getCategory() {
            return category;
        }

        public void setCategory(com.KTU.KTUVotingapp.model.Category category) {
            this.category = category;
        }

        public Integer getCandidateNumber() {
            return candidateNumber;
        }

        public void setCandidateNumber(Integer candidateNumber) {
            this.candidateNumber = candidateNumber;
        }
    }
}
