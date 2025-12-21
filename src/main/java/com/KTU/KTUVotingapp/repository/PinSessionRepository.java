package com.KTU.KTUVotingapp.repository;

import com.KTU.KTUVotingapp.model.PinSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PinSessionRepository extends JpaRepository<PinSession, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PinSession s WHERE s.token = :token")
    Optional<PinSession> findByTokenForUpdate(@Param("token") String token);

    Optional<PinSession> findByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM PinSession s WHERE s.expiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now);
}

