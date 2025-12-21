package com.KTU.KTUVotingapp.service;

import com.KTU.KTUVotingapp.model.PinSession;
import com.KTU.KTUVotingapp.model.VoterPin;
import com.KTU.KTUVotingapp.repository.PinSessionRepository;
import com.KTU.KTUVotingapp.repository.VoterPinRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PinService {

    private final VoterPinRepository voterPinRepository;
    private final PinSessionRepository pinSessionRepository;
    private final SecureRandom random = new SecureRandom();

    @Value("${voting.admin-pin:}")
    private String adminPin;

    public PinService(VoterPinRepository voterPinRepository, PinSessionRepository pinSessionRepository) {
        this.voterPinRepository = voterPinRepository;
        this.pinSessionRepository = pinSessionRepository;
    }

    @Transactional
    public List<String> generatePins(int count) {
        if (count <= 0) return List.of();
        List<VoterPin> toSave = new ArrayList<>(count);
        while (toSave.size() < count) {
            String pin = randomPin();
            boolean collisionInBatch = toSave.stream().anyMatch(p -> p.getPinCode().equals(pin));
            if (collisionInBatch || (adminPin != null && adminPin.equals(pin)) || voterPinRepository.existsByPinCode(pin)) {
                continue; // skip collisions and admin pin
            }
            toSave.add(new VoterPin(pin));
        }
        voterPinRepository.saveAll(toSave);
        return toSave.stream().map(VoterPin::getPinCode).toList();
    }

    public Page<VoterPin> listPins(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return voterPinRepository.findAll(pageable);
    }

    @Transactional
    public String issueSessionToken(String pin, LocalDateTime expiresAt) {
        VoterPin voterPin = voterPinRepository.findByPinCodeForUpdate(pin)
                .orElseThrow(() -> new IllegalArgumentException("PIN not found"));
        if (voterPin.isUsed()) {
            throw new IllegalStateException("PIN already used");
        }
        String token = UUID.randomUUID().toString();
        PinSession session = new PinSession(voterPin, token, expiresAt);
        pinSessionRepository.save(session);
        return token;
    }

    @Transactional
    public VoterPin consumeToken(String token) {
        PinSession session = pinSessionRepository.findByTokenForUpdate(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Token expired");
        }
        if (session.getConsumedAt() != null) {
            throw new IllegalStateException("Token already consumed");
        }
        // Lock the associated VoterPin row to prevent race where two transactions consume the same underlying pin
        VoterPin pin = session.getPin();
        VoterPin lockedPin = voterPinRepository.findByPinCodeForUpdate(pin.getPinCode())
                .orElseThrow(() -> new IllegalStateException("Associated PIN not found"));
        if (lockedPin.isUsed()) {
            throw new IllegalStateException("PIN already used");
        }
        session.setConsumedAt(LocalDateTime.now());
        pinSessionRepository.save(session);
        return lockedPin;
    }

    @Transactional
    public void markPinUsed(VoterPin pin) {
        pin.setUsed(true);
        pin.setUsedAt(LocalDateTime.now());
        voterPinRepository.save(pin);
    }

    public void cleanupExpiredSessions() {
        pinSessionRepository.deleteExpired(LocalDateTime.now());
    }

    public List<VoterPin> listAllPins() {
        return voterPinRepository.findAll();
    }

    private String randomPin() {
        int num = random.nextInt(10_000_000); // 0 to 9,999,999
        return String.format("%07d", num);
    }
}
