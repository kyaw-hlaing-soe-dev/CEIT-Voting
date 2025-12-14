package com.KTU.KTUVotingapp.repository;

import com.KTU.KTUVotingapp.model.Candidate;
import com.KTU.KTUVotingapp.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    List<Candidate> findByCategory(Category category);

    Optional<Candidate> findByCategoryAndCandidateNumber(Category category, Integer candidateNumber);

    @Query("SELECT c FROM Candidate c WHERE c.category = :category ORDER BY c.candidateNumber")
    List<Candidate> findByCategoryOrderByCandidateNumber(@Param("category") Category category);

    // Atomic DB-side increment to avoid lost updates under concurrency.
    @Modifying
    @Transactional
    @Query("UPDATE Candidate c SET c.voteCount = c.voteCount + 1 WHERE c.id = :id")
    int incrementVoteCount(@Param("id") Long id);

    // Existence check for candidateNumber across a set of categories
    boolean existsByCategoryInAndCandidateNumber(Iterable<Category> categories, Integer candidateNumber);

    // Existence check across categories while excluding a candidate id (useful for updates)
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Candidate c WHERE c.category IN :cats AND c.candidateNumber = :number AND (:excludeId IS NULL OR c.id <> :excludeId)")
    boolean existsByCategoryInAndCandidateNumberExcludingId(@Param("cats") Iterable<Category> cats, @Param("number") Integer number, @Param("excludeId") Long excludeId);

    // New: reset all vote counts to zero
    @Modifying
    @Transactional
    @Query("UPDATE Candidate c SET c.voteCount = 0")
    int resetAllVoteCounts();

    // New: reset vote counts for a single category
    @Modifying
    @Transactional
    @Query("UPDATE Candidate c SET c.voteCount = 0 WHERE c.category = :category")
    int resetVoteCountsByCategory(@Param("category") Category category);
}
