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
import java.util.*;
import java.util.stream.Collectors;

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

    private Iterable<Category> pairedCategories(Category category) {
        if (category == null) return List.of();
        return switch (category) {
            case KING -> List.of(Category.PRINCE);
            case PRINCE -> List.of(Category.KING);
            case QUEEN -> List.of(Category.PRINCESS);
            case PRINCESS -> List.of(Category.QUEEN);
            default -> List.of();
        };
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
        String ipAddress = request.getIpAddress();
        String fingerprint = request.getFingerprint();
        String hardwareHash = request.getHardwareHash();
        String screenInfo = request.getScreenInfo();

        // Device checks (unchanged)
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

        Optional<Voter> existingDeviceVoter = voterRepository.findByDeviceId(request.getDeviceId());
        if (existingDeviceVoter.isPresent()) {
            Voter existingVoter = existingDeviceVoter.get();
            if (existingVoter.isHasVoted() || voteRepository.existsByVoter(existingVoter)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This device has already submitted a vote");
            }
        }

        Voter voter = getOrCreateVoter(request.getPin(), request.getDeviceId(),
                                       request.getUserAgent(), request.getIpAddress(),
                                       request.getFingerprint(), request.getHardwareHash(),
                                       request.getScreenInfo());

        if (voter.isHasVoted() || voteRepository.existsByVoter(voter)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "This device has already submitted a vote");
        }

        // Check if voter already voted in this category
        if (voteRepository.existsByVoterAndCategory(voter, request.getCategory())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "You have already voted in this category");
        }

        // Validate candidate exists
        Candidate candidate = candidateRepository.findByCategoryAndCandidateNumber(
                request.getCategory(), request.getCandidateNumber())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Candidate not found for category " + request.getCategory() +
                    " and number " + request.getCandidateNumber()));

        // CRITICAL SECTION: prevent race conditions by locking existing votes for the voter
        // Acquire pessimistic write lock on voter's votes (join fetch candidate) to inspect candidate numbers safely
        List<Vote> lockedVotes = voteRepository.findByVoterWithLock(voter);

        // Business rule: disallow same candidateNumber across paired categories
        Iterable<Category> paired = pairedCategories(request.getCategory());
        if (voteRepository.existsByVoterAndCandidate_CandidateNumberAndCategoryIn(voter, request.getCandidateNumber(), paired)) {
            // Find the conflicting category for message clarity
            Optional<Vote> conflict = lockedVotes.stream()
                    .filter(v -> paired.iterator() != null && containsCategory(paired, v.getCategory()) && v.getCandidate().getCandidateNumber().equals(request.getCandidateNumber()))
                    .findFirst();
            String conflictCategory = conflict.map(v -> v.getCategory().name()).orElse("paired category");
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                String.format("You already voted Candidate No.%d for %s. You cannot choose Candidate No.%d for %s.",
                    request.getCandidateNumber(), conflictCategory, request.getCandidateNumber(), request.getCategory()));
        }

        // Create vote and persist
        try {
            Vote vote = new Vote(voter, candidate, request.getCategory());
            voteRepository.save(vote);
            candidateRepository.incrementVoteCount(candidate.getId());

            if (!voter.isHasVoted()) {
                voter.setHasVoted(true);
                voter.setVotedAt(LocalDateTime.now());
                voterRepository.save(voter);
            }
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Duplicate vote detected. You may have already voted in this category.");
        }
    }

    private boolean containsCategory(Iterable<Category> cats, Category c) {
        for (Category x : cats) if (x == c) return true;
        return false;
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

        Optional<Voter> existingDeviceVoter = voterRepository.findByDeviceId(request.getDeviceId());
        if (existingDeviceVoter.isPresent()) {
            Voter existingVoter = existingDeviceVoter.get();
            if (existingVoter.isHasVoted() || voteRepository.existsByVoter(existingVoter)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This device has already submitted votes");
            }
        }

        Voter voter = getOrCreateVoter(request.getPin(), request.getDeviceId(),
                                       request.getUserAgent(), request.getIpAddress(),
                                       request.getFingerprint(), request.getHardwareHash(),
                                       request.getScreenInfo());

        if (voter.isHasVoted() || voteRepository.existsByVoter(voter)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "This device has already submitted votes");
        }

        // Lock existing votes to inspect candidate numbers for conflicts
        List<Vote> lockedVotes = voteRepository.findByVoterWithLock(voter);

        // Build map of existing candidateNumbers by category
        Map<Category, Integer> existingNumbers = new HashMap<>();
        for (Vote v : lockedVotes) existingNumbers.put(v.getCategory(), v.getCandidate().getCandidateNumber());

        // Convert incoming list to map and validate duplicates inside the bulk request
        Map<Category, Integer> incoming = new HashMap<>();
        for (BulkVoteRequest.VoteItem vi : request.getVotes()) {
            if (incoming.containsKey(vi.getCategory())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate category in request: " + vi.getCategory());
            }
            incoming.put(vi.getCategory(), vi.getCandidateNumber());
        }

        // Validate cross-category same-number rule between pairs and with existing votes
        for (Map.Entry<Category, Integer> entry : incoming.entrySet()) {
            Category cat = entry.getKey();
            Integer num = entry.getValue();
            Iterable<Category> paired = pairedCategories(cat);

            // Check against existing votes in paired categories
            if (voteRepository.existsByVoterAndCandidate_CandidateNumberAndCategoryIn(voter, num, paired)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("You already voted Candidate No.%d in a paired category. You cannot choose Candidate No.%d for %s.", num, num, cat));
            }

            // Check against other incoming choices (e.g., user attempted KING #1 and PRINCE #1 in same bulk)
            for (Category p : (Collection<Category>) paired) {
                if (incoming.containsKey(p) && Objects.equals(incoming.get(p), num)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                        String.format("Invalid selection: Candidate No.%d selected for both %s and %s.", num, cat, p));
                }
            }
        }

        // Validate candidate existence for each incoming vote
        for (BulkVoteRequest.VoteItem vi : request.getVotes()) {
            candidateRepository.findByCategoryAndCandidateNumber(vi.getCategory(), vi.getCandidateNumber())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Candidate not found for category " + vi.getCategory() + " and number " + vi.getCandidateNumber()));
        }

        // Process and persist votes
        try {
            for (BulkVoteRequest.VoteItem vi : request.getVotes()) {
                Candidate candidate = candidateRepository.findByCategoryAndCandidateNumber(vi.getCategory(), vi.getCandidateNumber()).orElseThrow();
                Vote vote = new Vote(voter, candidate, vi.getCategory());
                voteRepository.save(vote);
                candidateRepository.incrementVoteCount(candidate.getId());
            }

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
                                   String ipAddress, String fingerprint, String hardwareHash,
                                   String screenInfo) {
        Optional<Voter> voterOpt = voterRepository.findByDeviceId(deviceId);

        if (voterOpt.isPresent()) {
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