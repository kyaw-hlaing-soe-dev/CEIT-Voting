package com.KTU.KTUVotingapp.service;

import com.KTU.KTUVotingapp.dto.BulkVoteRequest;
import com.KTU.KTUVotingapp.dto.VoteRequest;
import com.KTU.KTUVotingapp.model.Candidate;
import com.KTU.KTUVotingapp.model.Category;
import com.KTU.KTUVotingapp.model.Vote;
import com.KTU.KTUVotingapp.model.Voter;
import com.KTU.KTUVotingapp.repository.CandidateRepository;
import com.KTU.KTUVotingapp.repository.VoteRepository;
import com.KTU.KTUVotingapp.repository.VoterRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class VotingService {

    private final VoterRepository voterRepository;
    private final VoteRepository voteRepository;
    private final CandidateRepository candidateRepository;

    public VotingService(VoterRepository voterRepository, VoteRepository voteRepository,
                        CandidateRepository candidateRepository) {
        this.voterRepository = voterRepository;
        this.voteRepository = voteRepository;
        this.candidateRepository = candidateRepository;
    }

    /**
     * Submit a single vote with pessimistic locking and transaction management.
     * Uses READ_COMMITTED isolation level for optimal performance with consistency.
     * Supports shared PINs - multiple devices can use the same PIN.
     *
     * Surgical change: remove explicit pessimistic locking calls to reduce long-lived DB locks
     * under high concurrency. Rely on DB unique constraints and DataIntegrityViolationException
     * to detect duplicates. This reduces transaction contention and moves locking responsibility
     * back to the DB (single responsibility) instead of the service.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    @CacheEvict(value = {"results", "candidates"}, allEntries = true)
    public void submitVote(VoteRequest request) {
        // NOTE: removed pessimistic locking reads to shorten transaction duration and avoid
        // heavy lock contention. Use non-locking checks and let DB constraints guard uniqueness.

        String ipAddress = request.getIpAddress();
        String fingerprint = request.getFingerprint();
        String hardwareHash = request.getHardwareHash();
        String screenInfo = request.getScreenInfo();

        // MULTI-FACTOR DEVICE CHECK: Check all device identifiers

        // Check 1: IP address
        if (ipAddress != null && !ipAddress.isBlank()) {
            if (voterRepository.existsByIpAddressAndHasVotedTrue(ipAddress)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This IP address has already been used to vote. Only one vote is allowed per IP.");
            }
        }

        // Check 2: Fingerprint
        if (fingerprint != null && !fingerprint.isBlank()) {
            if (voterRepository.existsByFingerprintAndHasVotedTrue(fingerprint)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This device fingerprint has already been used to vote.");
            }
        }

        // Check 3: Hardware hash (cross-browser)
        if (hardwareHash != null && !hardwareHash.isBlank()) {
            if (voterRepository.existsByHardwareHashAndHasVotedTrue(hardwareHash)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This device has already been used to vote (hardware signature detected).");
            }
        }

        // Check 4: Screen info + IP combination
        if (screenInfo != null && !screenInfo.isBlank() && ipAddress != null && !ipAddress.isBlank()) {
            Optional<Voter> screenMatch = voterRepository.findByScreenInfoAndIpAddressAndHasVoted(screenInfo, ipAddress);
            if (screenMatch.isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A device with the same screen configuration has already voted from this network.");
            }
        }

        // Step 1: Check device ID existence (non-locking quick check)
        Optional<Voter> existingDeviceVoter = voterRepository.findByDeviceId(request.getDeviceId());
        if (existingDeviceVoter.isPresent()) {
            Voter existingVoter = existingDeviceVoter.get();
            if (existingVoter.isHasVoted() || voteRepository.existsByVoter(existingVoter)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This device has already submitted a vote");
            }
        }

        // Step 2: Get or create voter by device ID (PIN can be shared)
        Voter voter = getOrCreateVoter(request.getPin(), request.getDeviceId(),
                                       request.getUserAgent(), request.getIpAddress(),
                                       request.getFingerprint(), request.getHardwareHash(),
                                       request.getScreenInfo());

        if (voter.isHasVoted() || voteRepository.existsByVoter(voter)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "This device has already submitted a vote");
        }

        // Step 3: Check if voter already voted in this category (non-locking existence check)
        if (voteRepository.existsByVoterAndCategory(voter, request.getCategory())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "You have already voted in this category");
        }

        // Step 4: Get candidate
        Candidate candidate = candidateRepository.findByCategoryAndCandidateNumber(
                request.getCategory(), request.getCandidateNumber())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Candidate not found for category " + request.getCategory() +
                    " and number " + request.getCandidateNumber()));

        // Step 5: Create and save vote (database constraint will prevent duplicates)
        try {
            Vote vote = new Vote(voter, candidate, request.getCategory());
            voteRepository.save(vote);

            // Atomically increment candidate vote count in DB to avoid lost updates
            candidateRepository.incrementVoteCount(candidate.getId());

            // Update voter status
            if (!voter.isHasVoted()) {
                voter.setHasVoted(true);
                voter.setVotedAt(LocalDateTime.now());
                voterRepository.save(voter);
            }
        } catch (DataIntegrityViolationException e) {
            // Database constraint violation - handle gracefully
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Duplicate vote detected. You may have already voted in this category.");
        }
    }

    /**
     * Submit multiple votes in a single transaction (bulk voting).
     * All votes are processed atomically - either all succeed or all fail.
     * Supports shared PINs - multiple devices can use the same PIN.
     *
     * Surgical change: same approach as above, avoid pessimistic locking reads.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    @CacheEvict(value = {"results", "candidates"}, allEntries = true)
    public void submitBulkVotes(BulkVoteRequest request) {
        String ipAddress = request.getIpAddress();
        String fingerprint = request.getFingerprint();
        String hardwareHash = request.getHardwareHash();
        String screenInfo = request.getScreenInfo();

        // MULTI-FACTOR DEVICE CHECK: Check all device identifiers
        // This prevents voting from different browsers on the same device

        // Check 1: IP address
        if (ipAddress != null && !ipAddress.isBlank()) {
            if (voterRepository.existsByIpAddressAndHasVotedTrue(ipAddress)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This IP address has already been used to vote. Only one vote is allowed per IP.");
            }
        }

        // Check 2: Fingerprint (browser-specific but helps detect same browser)
        if (fingerprint != null && !fingerprint.isBlank()) {
            if (voterRepository.existsByFingerprintAndHasVotedTrue(fingerprint)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This device fingerprint has already been used to vote.");
            }
        }

        // Check 3: Hardware hash (cross-browser device identification)
        if (hardwareHash != null && !hardwareHash.isBlank()) {
            if (voterRepository.existsByHardwareHashAndHasVotedTrue(hardwareHash)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This device has already been used to vote (hardware signature detected).");
            }
        }

        // Check 4: Screen info + IP combination (catches different browsers on same device/network)
        if (screenInfo != null && !screenInfo.isBlank() && ipAddress != null && !ipAddress.isBlank()) {
            Optional<Voter> screenMatch = voterRepository.findByScreenInfoAndIpAddressAndHasVoted(screenInfo, ipAddress);
            if (screenMatch.isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A device with the same screen configuration has already voted from this network.");
            }
        }

        // Step 1: Check device ID existence (non-locking)
        Optional<Voter> existingDeviceVoter = voterRepository.findByDeviceId(request.getDeviceId());
        if (existingDeviceVoter.isPresent()) {
            Voter existingVoter = existingDeviceVoter.get();
            if (existingVoter.isHasVoted() || voteRepository.existsByVoter(existingVoter)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This device has already submitted votes");
            }
        }

        // Step 2: Get or create voter by device ID (PIN can be shared)
        Voter voter = getOrCreateVoter(request.getPin(), request.getDeviceId(),
                                       request.getUserAgent(), request.getIpAddress(),
                                       request.getFingerprint(), request.getHardwareHash(),
                                       request.getScreenInfo());

        if (voter.isHasVoted() || voteRepository.existsByVoter(voter)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "This device has already submitted votes");
        }

        // Step 3: Validate all votes before processing
        for (BulkVoteRequest.VoteItem voteItem : request.getVotes()) {
            // Check if already voted in this category
            if (voteRepository.existsByVoterAndCategory(voter, voteItem.getCategory())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You have already voted in category: " + voteItem.getCategory());
            }

            // Validate candidate exists
            candidateRepository.findByCategoryAndCandidateNumber(
                    voteItem.getCategory(), voteItem.getCandidateNumber())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Candidate not found for category " + voteItem.getCategory() +
                        " and number " + voteItem.getCandidateNumber()));
        }

        // Step 4: Process all votes
        try {
            for (BulkVoteRequest.VoteItem voteItem : request.getVotes()) {
                Candidate candidate = candidateRepository.findByCategoryAndCandidateNumber(
                        voteItem.getCategory(), voteItem.getCandidateNumber())
                        .orElseThrow();

                Vote vote = new Vote(voter, candidate, voteItem.getCategory());
                voteRepository.save(vote);

                // Atomically increment candidate vote count in DB
                candidateRepository.incrementVoteCount(candidate.getId());
            }

            // Update voter status
            if (!voter.isHasVoted()) {
                voter.setHasVoted(true);
                voter.setVotedAt(LocalDateTime.now());
                voterRepository.save(voter);
            }
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Duplicate vote detected. Transaction rolled back.");
        }
    }

    /**
     * Get or create voter with multi-factor device identification.
     * Supports shared PINs - multiple devices can use the same PIN.
     * Device ID is unique, PIN can be shared across multiple devices.
     */
    private Voter getOrCreateVoter(String pin, String deviceId, String userAgent,
                                   String ipAddress, String fingerprint,
                                   String hardwareHash, String screenInfo) {
        // Find by device ID first (non-locking quick check)
        Optional<Voter> voterOpt = voterRepository.findByDeviceId(deviceId);

        if (voterOpt.isPresent()) {
            // Device already exists - update missing device identification fields
            Voter existingVoter = voterOpt.get();
            boolean updated = false;

            if (fingerprint != null && !fingerprint.isBlank() &&
                (existingVoter.getFingerprint() == null || existingVoter.getFingerprint().isBlank())) {
                existingVoter.setFingerprint(fingerprint);
                updated = true;
            }
            if (hardwareHash != null && !hardwareHash.isBlank() &&
                (existingVoter.getHardwareHash() == null || existingVoter.getHardwareHash().isBlank())) {
                existingVoter.setHardwareHash(hardwareHash);
                updated = true;
            }
            if (screenInfo != null && !screenInfo.isBlank() &&
                (existingVoter.getScreenInfo() == null || existingVoter.getScreenInfo().isBlank())) {
                existingVoter.setScreenInfo(screenInfo);
                updated = true;
            }

            if (updated) {
                voterRepository.save(existingVoter);
            }
            return existingVoter;
        }

        // Create new voter with all device identification info
        try {
            Voter newVoter = new Voter(pin, deviceId);
            newVoter.setUserAgent(userAgent);
            newVoter.setIpAddress(ipAddress);
            if (fingerprint != null && !fingerprint.isBlank()) {
                newVoter.setFingerprint(fingerprint);
            }
            if (hardwareHash != null && !hardwareHash.isBlank()) {
                newVoter.setHardwareHash(hardwareHash);
            }
            if (screenInfo != null && !screenInfo.isBlank()) {
                newVoter.setScreenInfo(screenInfo);
            }
            return voterRepository.save(newVoter);
        } catch (DataIntegrityViolationException e) {
            // Race condition - another thread created the voter with same device ID
            return voterRepository.findByDeviceId(deviceId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Failed to create or find voter"));
        }
    }

    public boolean hasVoted(String pin, Category category) {
        Optional<Voter> voterOpt = voterRepository.findByPin(pin);
        if (voterOpt.isEmpty()) {
            return false;
        }
        return voteRepository.existsByVoterAndCategory(voterOpt.get(), category);
    }

    public boolean deviceHasVoted(String deviceId) {
        Optional<Voter> voterOpt = voterRepository.findByDeviceId(deviceId);
        return voterOpt.isPresent() && voterOpt.get().isHasVoted();
    }

    /**
     * Check if an IP address has already been used to vote.
     */
    public boolean ipHasVoted(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }
        return voterRepository.existsByIpAddressAndHasVotedTrue(ipAddress);
    }
}