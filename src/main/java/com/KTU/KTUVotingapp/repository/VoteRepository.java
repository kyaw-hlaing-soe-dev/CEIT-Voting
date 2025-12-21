package com.KTU.KTUVotingapp.repository;

import com.KTU.KTUVotingapp.model.Category;
import com.KTU.KTUVotingapp.model.Vote;
import com.KTU.KTUVotingapp.model.VoterPin;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    List<Vote> findByVoterPin(VoterPin voterPin);

    Optional<Vote> findByVoterPinAndCategory(VoterPin voterPin, Category category);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Vote v WHERE v.voterPin = :voterPin AND v.category = :category")
    Optional<Vote> findByVoterPinAndCategoryWithLock(@Param("voterPin") VoterPin voterPin, @Param("category") Category category);

    boolean existsByVoterPinAndCategory(VoterPin voterPin, Category category);

    boolean existsByVoterPin(VoterPin voterPin);

    @Query("SELECT COUNT(v) FROM Vote v WHERE v.category = :category")
    long countByCategory(@Param("category") Category category);

    @Transactional
    @Query("DELETE FROM Vote v WHERE v.category = :category")
    int deleteByCategory(@Param("category") Category category);

    @Query("SELECT COUNT(v) FROM Vote v WHERE v.candidate.id = :candidateId")
    long countByCandidateId(@Param("candidateId") Long candidateId);

    @Query("SELECT COUNT(DISTINCT v.voterPin.id) FROM Vote v")
    long countDistinctVoters();
}
