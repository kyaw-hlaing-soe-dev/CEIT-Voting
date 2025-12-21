package com.KTU.KTUVotingapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pin_sessions",
    indexes = {
        @Index(name = "idx_pin_sessions_token", columnList = "token"),
        @Index(name = "idx_pin_sessions_pin_id", columnList = "pin_id"),
        @Index(name = "idx_pin_sessions_expires_at", columnList = "expires_at")
    }
)
public class PinSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pin_id", nullable = false)
    private VoterPin pin;

    @Column(nullable = false, length = 64, unique = true)
    private String token;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    public PinSession() {}

    public PinSession(VoterPin pin, String token, LocalDateTime expiresAt) {
        this.pin = pin;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public VoterPin getPin() { return pin; }
    public void setPin(VoterPin pin) { this.pin = pin; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getConsumedAt() { return consumedAt; }
    public void setConsumedAt(LocalDateTime consumedAt) { this.consumedAt = consumedAt; }
}

