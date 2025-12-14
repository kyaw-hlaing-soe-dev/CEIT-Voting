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

    Optional<Voter> findByIpAddress(String ipAddress);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voter v WHERE v.deviceId = :deviceId")
    Optional<Voter> findByDeviceIdWithLock(@Param("deviceId") String deviceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Voter v WHERE v.pin = :pin")
    Optional<Voter> findByPinWithLock(@Param("pin") String pin);

    boolean existsByDeviceId(String deviceId);

    boolean existsByPin(String pin);

    boolean existsByIpAddressAndHasVotedTrue(String ipAddress);

    @Query("SELECT v FROM Voter v WHERE v.ipAddress = :ipAddress AND v.hasVoted = true")
    Optional<Voter> findByIpAddressAndHasVoted(@Param("ipAddress") String ipAddress);

    // Hardware hash methods for cross-browser device identification
    boolean existsByHardwareHashAndHasVotedTrue(String hardwareHash);

    @Query("SELECT v FROM Voter v WHERE v.hardwareHash = :hardwareHash AND v.hasVoted = true")
    Optional<Voter> findByHardwareHashAndHasVoted(@Param("hardwareHash") String hardwareHash);

    // Screen info methods (screen resolution is same across browsers on same device)
    @Query("SELECT v FROM Voter v WHERE v.screenInfo = :screenInfo AND v.ipAddress = :ipAddress AND v.hasVoted = true")
    Optional<Voter> findByScreenInfoAndIpAddressAndHasVoted(
        @Param("screenInfo") String screenInfo,
        @Param("ipAddress") String ipAddress);

    // Combined fingerprint check
    @Query("SELECT v FROM Voter v WHERE v.fingerprint = :fingerprint AND v.hasVoted = true")
    Optional<Voter> findByFingerprintAndHasVoted(@Param("fingerprint") String fingerprint);

    boolean existsByFingerprintAndHasVotedTrue(String fingerprint);
}


