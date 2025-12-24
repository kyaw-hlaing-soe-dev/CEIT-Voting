package com.KTU.KTUVotingapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.KTU.KTUVotingapp.model.Category;

public class VoteRequest {

    private String cookieId;

    @NotBlank(message = "PIN is required")
    private String pin;

    @NotNull(message = "Category is required")
    private Category category;

    @NotNull(message = "Candidate number is required")
    private Integer candidateNumber;

    private String ipAddress;

    public VoteRequest() {
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

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
