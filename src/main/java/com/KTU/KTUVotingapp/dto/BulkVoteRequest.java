package com.KTU.KTUVotingapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class BulkVoteRequest {

    private String cookieId;

    @NotBlank(message = "PIN is required")
    private String pin;

    private String ipAddress;

    @NotEmpty(message = "At least one vote is required")
    @Valid
    private List<VoteItem> votes;

    public BulkVoteRequest() {
    }

    public String getCookieId() {
        return cookieId;
    }

    public void setCookieId(String cookieId) {
        this.cookieId = cookieId;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
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
