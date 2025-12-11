package com.KTU.KTUVotingapp.repository;

import com.KTU.KTUVotingapp.model.Voter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoterRepository extends JpaRepository<Voter, Long> {

    Optional<Voter> findByPin(String pin);

    Optional<Voter> findByDeviceId(String deviceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voter v WHERE v.deviceId = :deviceId")
    Optional<Voter> findByDeviceIdWithLock(@Param("deviceId") String deviceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voter v WHERE v.pin = :pin")
    Optional<Voter> findByPinWithLock(@Param("pin") String pin);

    boolean existsByDeviceId(String deviceId);

    boolean existsByPin(String pin);
}


