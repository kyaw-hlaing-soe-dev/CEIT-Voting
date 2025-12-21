package com.KTU.KTUVotingapp.service;

import com.KTU.KTUVotingapp.dto.BulkVoteRequest;
import com.KTU.KTUVotingapp.dto.VoteRequest;
import com.KTU.KTUVotingapp.model.Candidate;
import com.KTU.KTUVotingapp.model.Category;
import com.KTU.KTUVotingapp.model.Vote;
import com.KTU.KTUVotingapp.model.VoterPin;
import com.KTU.KTUVotingapp.repository.CandidateRepository;
import com.KTU.KTUVotingapp.repository.VoteRepository;
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

@Service
public class VotingService {

    private final VoteRepository voteRepository;
    private final CandidateRepository candidateRepository;
    private final PinService pinService;

    public VotingService(VoteRepository voteRepository,
                         CandidateRepository candidateRepository,
                         PinService pinService) {
        this.voteRepository = voteRepository;
        this.candidateRepository = candidateRepository;
        this.pinService = pinService;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    @CacheEvict(value = {"results", "candidates"}, allEntries = true)
    public void submitVote(String token, VoteRequest request) {
        VoterPin pin = pinService.consumeToken(token);
        enforceNotUsed(pin);
        enforceNotVotedCategory(pin, request.getCategory());
        Candidate candidate = candidateRepository.findByCategoryAndCandidateNumber(
                request.getCategory(), request.getCandidateNumber())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Candidate not found for category " + request.getCategory() +
                    " and number " + request.getCandidateNumber()));
        try {
            Vote vote = new Vote(pin, candidate, request.getCategory());
            voteRepository.save(vote);
            candidateRepository.incrementVoteCount(candidate.getId());
            pinService.markPinUsed(pin);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Duplicate vote detected. You may have already voted in this category.");
        }
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    @CacheEvict(value = {"results", "candidates"}, allEntries = true)
    public void submitBulkVotes(String token, BulkVoteRequest request) {
        VoterPin pin = pinService.consumeToken(token);
        enforceNotUsed(pin);
        validatePairedSelections(request.getVotes());
        // Validate all candidates first
        request.getVotes().forEach(v -> candidateRepository.findByCategoryAndCandidateNumber(
                v.getCategory(), v.getCandidateNumber())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Candidate not found for category " + v.getCategory() +
                    " and number " + v.getCandidateNumber())));

        try {
            request.getVotes().forEach(v -> {
                enforceNotVotedCategory(pin, v.getCategory());
                Candidate candidate = candidateRepository.findByCategoryAndCandidateNumber(
                        v.getCategory(), v.getCandidateNumber()).orElseThrow();
                Vote vote = new Vote(pin, candidate, v.getCategory());
                voteRepository.save(vote);
                candidateRepository.incrementVoteCount(candidate.getId());
            });
            pinService.markPinUsed(pin);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Duplicate vote detected. Transaction rolled back.");
        }
    }

    private void enforceNotUsed(VoterPin pin) {
        if (pin.isUsed()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PIN already used");
        }
    }

    private void enforceNotVotedCategory(VoterPin pin, Category category) {
        if (voteRepository.existsByVoterPinAndCategory(pin, category)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "You have already voted in this category");
        }
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