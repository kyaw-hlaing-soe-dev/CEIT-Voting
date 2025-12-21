package com.KTU.KTUVotingapp.repository;

import com.KTU.KTUVotingapp.model.VoterPin;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoterPinRepository extends JpaRepository<VoterPin, Long> {

    boolean existsByPinCode(String pinCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM VoterPin p WHERE p.pinCode = :pinCode")
    Optional<VoterPin> findByPinCodeForUpdate(@Param("pinCode") String pinCode);

    Optional<VoterPin> findByPinCode(String pinCode);

    // backward-compatible methods using column name (pin) may still exist in DB; prefer pinCode methods

    Page<VoterPin> findAll(Pageable pageable);
}
