package com.KTU.KTUVotingapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class BulkVoteRequest {

    // Device ID may be omitted; server will derive a stable ID if missing
    private String deviceId;

    @NotBlank(message = "PIN is required")
    private String pin;

    // For device fingerprinting and auditing
    private String userAgent;
    private String ipAddress;
    private String fingerprint;
    private String hardwareHash;
    private String screenInfo;

    @NotEmpty(message = "At least one vote is required")
    @Valid
    private List<VoteItem> votes;

    public BulkVoteRequest() {
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getHardwareHash() {
        return hardwareHash;
    }

    public void setHardwareHash(String hardwareHash) {
        this.hardwareHash = hardwareHash;
    }

    public String getScreenInfo() {
        return screenInfo;
    }

    public void setScreenInfo(String screenInfo) {
        this.screenInfo = screenInfo;
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
