package com.KTU.KTUVotingapp.service;

import com.KTU.KTUVotingapp.dto.CandidateDTO;
import com.KTU.KTUVotingapp.dto.CandidateForm;
import com.KTU.KTUVotingapp.exception.RankingConflictException;
import com.KTU.KTUVotingapp.model.AdminActionAudit;
import com.KTU.KTUVotingapp.model.Candidate;
import com.KTU.KTUVotingapp.model.Category;
import com.KTU.KTUVotingapp.repository.AdminActionAuditRepository;
import com.KTU.KTUVotingapp.repository.CandidateRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final AdminActionAuditRepository auditRepository;

    public CandidateService(CandidateRepository candidateRepository, AdminActionAuditRepository auditRepository) {
        this.candidateRepository = candidateRepository;
        this.auditRepository = auditRepository;
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

    // Business rule: paired categories that cannot share the same candidateNumber
    // KING <-> PRINCE, QUEEN <-> PRINCESS
    private Iterable<Category> conflictingPair(Category category) {
        if (category == null) return List.of();
        return switch (category) {
            case KING -> List.of(Category.PRINCE);
            case PRINCE -> List.of(Category.KING);
            case QUEEN -> List.of(Category.PRINCESS);
            case PRINCESS -> List.of(Category.QUEEN);
            default -> List.of();
        };
    }

    // Public helper to check if a rank is taken in the paired categories (optionally excluding a candidate id)
    public boolean isRankTakenInPaired(Category category, Integer candidateNumber, Long excludeId) {
        if (category == null || candidateNumber == null) return false;
        Iterable<Category> pairs = conflictingPair(category);
        if (excludeId == null) {
            return candidateRepository.existsByCategoryInAndCandidateNumber(pairs, candidateNumber);
        } else {
            return candidateRepository.existsByCategoryInAndCandidateNumberExcludingId(pairs, candidateNumber, excludeId);
        }
    }

    // Validate before creating a candidate - prevents obvious conflicts
    public void validateRankingConflictForCreate(Category category, Integer candidateNumber) {
        if (candidateNumber == null || category == null) return;
        Iterable<Category> pairs = conflictingPair(category);
        if (candidateRepository.existsByCategoryInAndCandidateNumber(pairs, candidateNumber)) {
            throw new RankingConflictException("Rank " + candidateNumber + " is already taken in the paired category for " + category);
        }
    }

    // Transactional save with SERIALIZABLE isolation to avoid race conditions when assigning same rank concurrently
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Candidate createCandidateTransactional(Candidate candidate) {
        // Validate again inside transaction to prevent race
        validateRankingConflictForCreate(candidate.getCategory(), candidate.getCandidateNumber());
        return candidateRepository.save(candidate);
    }

    // Update candidate - handle editing existing selections: if candidateNumber or category changed, validate conflicts
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Candidate updateCandidateTransactional(Long id, CandidateForm form, java.util.function.Consumer<Candidate> imageUpdater) {
        Candidate existing = getCandidateById(id);
        Category newCategory = form.getCategory() != null ? form.getCategory() : existing.getCategory();
        Integer newNumber = form.getCandidateNumber() != null ? form.getCandidateNumber() : existing.getCandidateNumber();

        boolean categoryChanged = !newCategory.equals(existing.getCategory());
        boolean numberChanged = !newNumber.equals(existing.getCandidateNumber());

        if (categoryChanged || numberChanged) {
            Iterable<Category> conflicting = conflictingPair(newCategory);
            if (candidateRepository.existsByCategoryInAndCandidateNumberExcludingId(conflicting, newNumber, id)) {
                throw new RankingConflictException("Rank " + newNumber + " is already taken in the paired category for " + newCategory);
            }
        }

        if (form.getName() != null) existing.setName(form.getName());
        if (form.getDepartment() != null) existing.setDepartment(form.getDepartment());
        if (form.getCandidateNumber() != null) existing.setCandidateNumber(form.getCandidateNumber());
        if (form.getCategory() != null) existing.setCategory(form.getCategory());
        if (form.getVoteCount() != null) existing.setVoteCount(form.getVoteCount());

        // imageUpdater is a lambda to optionally update imageUrl if a file was stored
        if (imageUpdater != null) imageUpdater.accept(existing);

        return candidateRepository.save(existing);
    }

    /**
     * Reset all candidate vote counts to zero in a separate transaction.
     * Uses REQUIRES_NEW to ensure the update runs atomically and independently
     * of any surrounding transaction. Evicts caches after update.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(value = {"results", "candidates"}, allEntries = true)
    public int resetAllVotes() {
        return resetAllVotes(null);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(value = {"results", "candidates"}, allEntries = true)
    public int resetAllVotes(String performedBy) {
        int updated = candidateRepository.resetAllVoteCounts();
        // Record audit
        String details = "Reset all candidate vote counts to 0. Rows affected: " + updated;
        auditRepository.save(new AdminActionAudit("RESET_ALL_VOTES", details, performedBy));
        return updated;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(value = {"results", "candidates"}, allEntries = true)
    public int resetVotesByCategory(Category category, String performedBy) {
        int updated = candidateRepository.resetVoteCountsByCategory(category);
        String details = "Reset votes for category " + category + ". Rows affected: " + updated;
        auditRepository.save(new AdminActionAudit("RESET_VOTES_BY_CATEGORY", details, performedBy));
        return updated;
    }
}
