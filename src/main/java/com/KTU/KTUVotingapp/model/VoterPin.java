package com.KTU.KTUVotingapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "voter_pins",
    indexes = {
        @Index(name = "idx_voter_pins_pin", columnList = "pin"),
        @Index(name = "idx_voter_pins_used", columnList = "is_used")
    },
    uniqueConstraints = {@UniqueConstraint(name = "uk_voter_pins_pin", columnNames = "pin")}
)
public class VoterPin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // map to DB column 'pin' but expose property as pinCode in the Java model
    @Column(name = "pin", nullable = false, length = 7)
    private String pinCode;

    @Column(name = "is_used", nullable = false)
    private boolean isUsed = false;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public VoterPin() {}

    public VoterPin(String pinCode) {
        this.pinCode = pinCode;
    }

    public Long getId() {
        return id;
    }

    public String getPinCode() {
        return pinCode;
    }

    public void setPinCode(String pinCode) {
        this.pinCode = pinCode;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean used) {
        this.isUsed = used;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
