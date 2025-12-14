package com.KTU.KTUVotingapp.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "voters", 
    indexes = {
        @Index(name = "idx_pin", columnList = "pin"),
        @Index(name = "idx_has_voted", columnList = "has_voted"),
        @Index(name = "idx_device_id", columnList = "device_id"),
        @Index(name = "idx_ip_address", columnList = "ip_address"),
        @Index(name = "idx_hardware_hash", columnList = "hardware_hash")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_device_id", columnNames = "device_id")
    }
)
public class Voter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 5)
    private String pin;

    @Column(name = "device_id", nullable = false, length = 255, unique = true)
    private String deviceId;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "fingerprint", length = 512)
    private String fingerprint;

    @Column(name = "hardware_hash", length = 128)
    private String hardwareHash;

    @Column(name = "screen_info", length = 100)
    private String screenInfo;

    @Column(name = "has_voted", nullable = false)
    private boolean hasVoted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "voted_at")
    private LocalDateTime votedAt;

    public Voter() {
    }

    public Voter(String pin, String deviceId) {
        this.pin = pin;
        this.deviceId = deviceId;
    }

    public Long getId() {
        return id;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
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

    public boolean isHasVoted() {
        return hasVoted;
    }

    public void setHasVoted(boolean hasVoted) {
        this.hasVoted = hasVoted;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getVotedAt() {
        return votedAt;
    }

    public void setVotedAt(LocalDateTime votedAt) {
        this.votedAt = votedAt;
    }
}
