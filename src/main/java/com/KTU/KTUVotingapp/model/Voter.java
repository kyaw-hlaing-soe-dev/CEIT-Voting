package com.KTU.KTUVotingapp.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "voters",
    indexes = {
        @Index(name = "idx_pin", columnList = "pin"),
        @Index(name = "idx_has_voted", columnList = "has_voted"),
        @Index(name = "idx_cookie_id", columnList = "cookie_id"),
        @Index(name = "idx_ip_address", columnList = "ip_address")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_cookie_id", columnNames = "cookie_id")
    }
)
public class Voter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 5)
    private String pin;

    @Column(name = "cookie_id", nullable = false, length = 255, unique = true)
    private String cookieId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "has_voted", nullable = false)
    private boolean hasVoted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "voted_at")
    private LocalDateTime votedAt;

    public Voter() {
    }

    public Voter(String pin, String cookieId) {
        this.pin = pin;
        this.cookieId = cookieId;
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

    public String getCookieId() {
        return cookieId;
    }

    public void setCookieId(String cookieId) {
        this.cookieId = cookieId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
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
