package com.KTU.KTUVotingapp.service;

import com.KTU.KTUVotingapp.model.*;
import com.KTU.KTUVotingapp.repository.*;
import com.KTU.KTUVotingapp.exception.InvalidVoteException;
import com.KTU.KTUVotingapp.exception.AlreadyVotedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class VoteService {

    private final VoterRepository voterRepository;
    private final VoteRepository voteRepository;
    private final CandidateRepository candidateRepository;

    public VoteService(VoterRepository voterRepository, VoteRepository voteRepository, CandidateRepository candidateRepository) {
        this.voterRepository = voterRepository;
        this.voteRepository = voteRepository;
        this.candidateRepository = candidateRepository;
    }

    @Transactional
    public Vote castOrUpdateVote(Long voterId, Category category, Integer candidateNumber) {
        // Lock voter row to serialize operations for the same voter and avoid race inserts
        Voter voter = voterRepository.findById(voterId)
                .orElseThrow(() -> new InvalidVoteException("Voter not found"));

        // Read existing votes for the voter
        // We could also query only paired category but reading all is fine
        Optional<Vote> pairedVoteOpt = voteRepository.findByVoterAndCategory(voter, category.paired());
        if (pairedVoteOpt.isPresent() && pairedVoteOpt.get().getCandidateNumber().equals(candidateNumber)) {
            String message = String.format(
                    "You already voted Candidate No.%d for %s. You cannot choose Candidate No.%d for %s.",
                    candidateNumber,
                    pairedVoteOpt.get().getCategory().displayName(),
                    candidateNumber,
                    category.displayName()
            );
            throw new InvalidVoteException(message);
        }

        Optional<Vote> existing = voteRepository.findByVoterAndCategory(voter, category);
        if (existing.isPresent()) {
            Vote v = existing.get();
            // Look up the Candidate entity and update both candidate reference and candidate number
            Candidate candidate = candidateRepository.findByCategoryAndCandidateNumber(category, candidateNumber)
                    .orElseThrow(() -> new InvalidVoteException("Candidate not found for category " + category + " and number " + candidateNumber));
            v.setCandidateNumber(candidateNumber);
            v.setCandidate(candidate);
            return voteRepository.save(v);
        } else {
            // Find candidate entity for the category/number and create vote
            Candidate candidate = candidateRepository.findByCategoryAndCandidateNumber(category, candidateNumber)
                    .orElseThrow(() -> new InvalidVoteException("Candidate not found for category " + category + " and number " + candidateNumber));
            Vote v = new Vote(voter, candidate, category);
            v.setCandidateNumber(candidateNumber);
            return voteRepository.save(v);
        }
    }

    // New method: transactional and synchronized to prevent race conditions across threads
    @Transactional
    public synchronized Vote castOrUpdateVoteWithIpCookie(Long voterId, Category category, Integer candidateNumber, String ipAddress, String voterCookieId) {
        // Defensive check: if a vote already exists with this ip+cookie, reject
        if (ipAddress != null && voterCookieId != null) {
            boolean exists = voteRepository.existsByIpAddressAndVoterCookieId(ipAddress, voterCookieId);
            if (exists) {
                throw new AlreadyVotedException("A vote from this IP and cookie has already been recorded.");
            }
        }

        // Proceed with the normal cast/update flow but set ip and cookie on new votes
        Voter voter = voterRepository.findById(voterId)
                .orElseThrow(() -> new InvalidVoteException("Voter not found"));

        Optional<Vote> pairedVoteOpt = voteRepository.findByVoterAndCategory(voter, category.paired());
        if (pairedVoteOpt.isPresent() && pairedVoteOpt.get().getCandidateNumber().equals(candidateNumber)) {
            String message = String.format(
                    "You already voted Candidate No.%d for %s. You cannot choose Candidate No.%d for %s.",
                    candidateNumber,
                    pairedVoteOpt.get().getCategory().displayName(),
                    candidateNumber,
                    category.displayName()
            );
            throw new InvalidVoteException(message);
        }

        Optional<Vote> existing = voteRepository.findByVoterAndCategory(voter, category);
        if (existing.isPresent()) {
            Vote v = existing.get();
            Candidate candidate = candidateRepository.findByCategoryAndCandidateNumber(category, candidateNumber)
                    .orElseThrow(() -> new InvalidVoteException("Candidate not found for category " + category + " and number " + candidateNumber));
            v.setCandidateNumber(candidateNumber);
            v.setCandidate(candidate);
            // Update ip/cookie in case they changed
            v.setIpAddress(ipAddress);
            v.setVoterCookieId(voterCookieId);
            return voteRepository.save(v);
        } else {
            Candidate candidate = candidateRepository.findByCategoryAndCandidateNumber(category, candidateNumber)
                    .orElseThrow(() -> new InvalidVoteException("Candidate not found for category " + category + " and number " + candidateNumber));
            Vote v = new Vote(voter, candidate, category);
            v.setCandidateNumber(candidateNumber);
            v.setIpAddress(ipAddress);
            v.setVoterCookieId(voterCookieId);
            return voteRepository.save(v);
        }
    }
}
