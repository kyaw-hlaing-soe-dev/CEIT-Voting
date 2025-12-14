package com.KTU.KTUVotingapp.repository;

import com.KTU.KTUVotingapp.model.AdminActionAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminActionAuditRepository extends JpaRepository<AdminActionAudit, Long> {
}

