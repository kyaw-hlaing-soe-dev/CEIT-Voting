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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    @CacheEvict(value = {"results", "candidates"}, allEntries = true)
    public void submitVote(VoteRequest request) {
        String ipAddress = request.getIpAddress();
        String cookieId = request.getCookieId();

        if (cookieId != null && !cookieId.isBlank() && voterRepository.existsByCookieId(cookieId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This browser has already voted.");
        }

        if (ipAddress != null && !ipAddress.isBlank() && voterRepository.existsByIpAddressAndHasVotedTrue(ipAddress)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This network/IP has already been used to vote.");
        }

        Voter voter = getOrCreateVoter(request.getPin(), cookieId, ipAddress);

        if (voter.isHasVoted() || voteRepository.existsByVoterAndCategory(voter, request.getCategory())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already voted in this category.");
        }

        enforcePairedConstraintWithExistingVote(voter, request.getCategory(), request.getCandidateNumber());

        Candidate candidate = candidateRepository.findByCategoryAndCandidateNumber(
                request.getCategory(), request.getCandidateNumber())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Candidate not found for category " + request.getCategory() +
                    " and number " + request.getCandidateNumber()));

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

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    @CacheEvict(value = {"results", "candidates"}, allEntries = true)
    public void submitBulkVotes(BulkVoteRequest request) {
        String ipAddress = request.getIpAddress();
        String cookieId = request.getCookieId();

        if (cookieId != null && !cookieId.isBlank() && voterRepository.existsByCookieId(cookieId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This browser has already voted.");
        }

        if (ipAddress != null && !ipAddress.isBlank() && voterRepository.existsByIpAddressAndHasVotedTrue(ipAddress)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This network/IP has already been used to vote.");
        }

        Voter voter = getOrCreateVoter(request.getPin(), cookieId, ipAddress);

        if (voter.isHasVoted() || voteRepository.existsByVoter(voter)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This browser has already submitted votes.");
        }

        validatePairedSelections(request.getVotes());

        for (BulkVoteRequest.VoteItem voteItem : request.getVotes()) {
            if (voteRepository.existsByVoterAndCategory(voter, voteItem.getCategory())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You have already voted in category: " + voteItem.getCategory());
            }

            enforcePairedConstraintWithExistingVote(voter, voteItem.getCategory(), voteItem.getCandidateNumber());

            candidateRepository.findByCategoryAndCandidateNumber(
                    voteItem.getCategory(), voteItem.getCandidateNumber())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Candidate not found for category " + voteItem.getCategory() +
                        " and number " + voteItem.getCandidateNumber()));
        }

        try {
            for (BulkVoteRequest.VoteItem voteItem : request.getVotes()) {
                Candidate candidate = candidateRepository.findByCategoryAndCandidateNumber(
                        voteItem.getCategory(), voteItem.getCandidateNumber())
                        .orElseThrow();

                enforcePairedConstraintWithExistingVote(voter, voteItem.getCategory(), voteItem.getCandidateNumber());

                Vote vote = new Vote(voter, candidate, voteItem.getCategory());
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

    private Voter getOrCreateVoter(String pin, String cookieId, String ipAddress) {
        Optional<Voter> voterOpt = cookieId == null ? Optional.empty() : voterRepository.findByCookieId(cookieId);

        if (voterOpt.isPresent()) {
            Voter existing = voterOpt.get();
            if (ipAddress != null && !ipAddress.isBlank() && (existing.getIpAddress() == null || existing.getIpAddress().isBlank())) {
                existing.setIpAddress(ipAddress);
                voterRepository.save(existing);
            }
            return existing;
        }

        String idToUse = (cookieId == null || cookieId.isBlank()) ? "cookie-" + java.util.UUID.randomUUID() : cookieId;

        try {
            Voter newVoter = new Voter(pin, idToUse);
            newVoter.setIpAddress(ipAddress);
            return voterRepository.save(newVoter);
        } catch (DataIntegrityViolationException e) {
            return voterRepository.findByCookieId(idToUse)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Failed to create or find voter"));
        }
    }

    public boolean hasVoted(String pin, Category category) {
        return voterRepository.findByPin(pin)
                .map(voter -> voteRepository.existsByVoterAndCategory(voter, category))
                .orElse(false);
    }

    public boolean deviceHasVoted(String cookieId) {
        Optional<Voter> voterOpt = voterRepository.findByCookieId(cookieId);
        return voterOpt.isPresent() && voterOpt.get().isHasVoted();
    }

    public boolean ipHasVoted(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }
        return voterRepository.existsByIpAddressAndHasVotedTrue(ipAddress);
    }

    private void enforcePairedConstraintWithExistingVote(Voter voter, Category attemptedCategory, Integer candidateNumber) {
        if (candidateNumber == null) {
            return;
        }
        Category pairedCategory = attemptedCategory.paired();
        if (pairedCategory == attemptedCategory) {
            return;
        }
        voteRepository.findByVoterAndCategory(voter, pairedCategory)
                .filter(existing -> existing.getCandidateNumber() != null
                        && existing.getCandidateNumber().equals(candidateNumber))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        buildPairingErrorMessage(existing.getCategory(), attemptedCategory, candidateNumber));
                });
    }

    private void validatePairedSelections(List<BulkVoteRequest.VoteItem> votes) {
        if (votes == null || votes.isEmpty()) {
            return;
        }
        Map<Category, Integer> selections = new EnumMap<>(Category.class);
        for (BulkVoteRequest.VoteItem voteItem : votes) {
            Category category = voteItem.getCategory();
            Integer candidateNumber = voteItem.getCandidateNumber();
            if (category == null || candidateNumber == null) {
                continue;
            }
            Category pairedCategory = category.paired();
            if (pairedCategory != category) {
                Integer pairedSelection = selections.get(pairedCategory);
                if (pairedSelection != null && pairedSelection.equals(candidateNumber)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        buildPairingErrorMessage(pairedCategory, category, candidateNumber));
                }
            }
            selections.put(category, candidateNumber);
        }
    }

    private String buildPairingErrorMessage(Category existingCategory, Category attemptedCategory, Integer candidateNumber) {
        return String.format(
                "You already voted Candidate No.%d for %s. You cannot choose Candidate No.%d for %s.",
                candidateNumber,
                existingCategory.displayName(),
                candidateNumber,
                attemptedCategory.displayName()
        );
    }
}

