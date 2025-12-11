package com.KTU.KTUVotingapp.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "voters", 
    indexes = {
        @Index(name = "idx_pin", columnList = "pin"),
        @Index(name = "idx_has_voted", columnList = "has_voted"),
        @Index(name = "idx_device_id", columnList = "device_id")
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


