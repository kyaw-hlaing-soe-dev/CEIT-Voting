package com.KTU.KTUVotingapp.repository;

import com.KTU.KTUVotingapp.model.Category;
import com.KTU.KTUVotingapp.model.Vote;
import com.KTU.KTUVotingapp.model.Voter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    List<Vote> findByVoter(Voter voter);

    List<Vote> findByVoterAndCategory(Voter voter, Category category);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Vote v WHERE v.voter = :voter AND v.category = :category")
    Optional<Vote> findByVoterAndCategoryWithLock(@Param("voter") Voter voter, @Param("category") Category category);

    boolean existsByVoterAndCategory(Voter voter, Category category);

    @Query("SELECT COUNT(v) FROM Vote v WHERE v.category = :category")
    long countByCategory(@Param("category") Category category);

    @Query("SELECT COUNT(v) FROM Vote v WHERE v.candidate.id = :candidateId")
    long countByCandidateId(@Param("candidateId") Long candidateId);
}


