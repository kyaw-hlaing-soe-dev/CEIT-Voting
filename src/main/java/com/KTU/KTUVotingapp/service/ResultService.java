package com.KTU.KTUVotingapp.service;

import com.KTU.KTUVotingapp.dto.ResultDTO;
import com.KTU.KTUVotingapp.model.Candidate;
import com.KTU.KTUVotingapp.model.Category;
import com.KTU.KTUVotingapp.repository.CandidateRepository;
import com.KTU.KTUVotingapp.repository.VoteRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ResultService {

    private final VoteRepository voteRepository;
    private final CandidateRepository candidateRepository;

    public ResultService(VoteRepository voteRepository, CandidateRepository candidateRepository) {
        this.voteRepository = voteRepository;
        this.candidateRepository = candidateRepository;
    }

    @Cacheable(value = "results", key = "#category")
    public ResultDTO getResultsByCategory(Category category) {
        List<Candidate> candidates = candidateRepository.findByCategoryOrderByCandidateNumber(category);
        long totalVotes = voteRepository.countByCategory(category);

        List<ResultDTO.CandidateResultDTO> candidateResults = candidates.stream()
                .map(candidate -> {
                    long voteCount = candidate.getVoteCount();
                    double percentage = totalVotes > 0 ? (voteCount * 100.0 / totalVotes) : 0.0;
                    
                    return new ResultDTO.CandidateResultDTO(
                            candidate.getId(),
                            candidate.getCandidateNumber(),
                            candidate.getName(),
                            candidate.getDepartment(),
                            candidate.getImageUrl(),
                            voteCount,
                            Math.round(percentage * 100.0) / 100.0 // Round to 2 decimal places
                    );
                })
                .collect(Collectors.toList());

        return new ResultDTO(category, totalVotes, candidateResults);
    }

    @Cacheable(value = "results", key = "'all'")
    public List<ResultDTO> getAllResults() {
        return List.of(
                getResultsByCategory(Category.KING),
                getResultsByCategory(Category.QUEEN),
                getResultsByCategory(Category.PRINCE),
                getResultsByCategory(Category.PRINCESS),
                getResultsByCategory(Category.COUPLE)
        );
    }
}

