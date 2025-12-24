package com.KTU.KTUVotingapp.repository;

import com.KTU.KTUVotingapp.model.Voter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface VoterRepository extends JpaRepository<Voter, Long> {

    Optional<Voter> findByPin(String pin);

    Optional<Voter> findByCookieId(String cookieId);

    Optional<Voter> findByIpAddress(String ipAddress);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voter v WHERE v.cookieId = :cookieId")
    Optional<Voter> findByCookieIdWithLock(@Param("cookieId") String cookieId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voter v WHERE v.pin = :pin")
    Optional<Voter> findByPinWithLock(@Param("pin") String pin);

    boolean existsByCookieId(String cookieId);

    boolean existsByPin(String pin);

    boolean existsByIpAddressAndHasVotedTrue(String ipAddress);

    @Query("SELECT v FROM Voter v WHERE v.ipAddress = :ipAddress AND v.hasVoted = true")
    Optional<Voter> findByIpAddressAndHasVoted(@Param("ipAddress") String ipAddress);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voter v WHERE v.id = :id")
    Optional<Voter> findByIdWithLock(@Param("id") Long id);

    long countByHasVotedTrue();

    // Reset all voters to not voted and clear votedAt (used by admin reset actions)
    @Modifying
    @Transactional
    @Query("UPDATE Voter v SET v.hasVoted = false, v.votedAt = NULL")
    int resetAllHasVoted();
}
