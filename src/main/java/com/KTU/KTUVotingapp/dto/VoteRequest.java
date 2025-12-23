package com.KTU.KTUVotingapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.KTU.KTUVotingapp.model.Category;

public class VoteRequest {

    // Device ID can be supplied by client but will be derived server-side if missing
    private String deviceId;

    @NotBlank(message = "PIN is required")
    private String pin;

    @NotNull(message = "Category is required")
    private Category category;

    @NotNull(message = "Candidate number is required")
    private Integer candidateNumber;

    // For device fingerprinting and auditing
    private String userAgent;
    private String ipAddress;
    private String fingerprint;
    private String hardwareHash;
    private String screenInfo;

    public VoteRequest() {
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
}
