package com.KTU.KTUVotingapp.service;

import com.KTU.KTUVotingapp.dto.CandidateDTO;
import com.KTU.KTUVotingapp.model.Candidate;
import com.KTU.KTUVotingapp.model.Category;
import com.KTU.KTUVotingapp.repository.CandidateRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CandidateService {

    private final CandidateRepository candidateRepository;

    public CandidateService(CandidateRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
    }

    @Cacheable(value = "candidates", key = "#category")
    public List<CandidateDTO> getCandidatesByCategory(Category category) {
        List<Candidate> candidates = candidateRepository.findByCategoryOrderByCandidateNumber(category);
        return candidates.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<Candidate> findByCategoryAndNumber(Category category, Integer candidateNumber) {
        return candidateRepository.findByCategoryAndCandidateNumber(category, candidateNumber);
    }

    public Candidate getCandidateById(Long id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found with id: " + id));
    }

    private CandidateDTO toDTO(Candidate candidate) {
        return new CandidateDTO(
                candidate.getId(),
                candidate.getCategory(),
                candidate.getCandidateNumber(),
                candidate.getName(),
                candidate.getDepartment(),
                candidate.getImageUrl(),
                candidate.getVoteCount()
        );
    }
}

