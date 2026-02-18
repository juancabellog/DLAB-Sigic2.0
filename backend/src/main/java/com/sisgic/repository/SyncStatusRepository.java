package com.sisgic.repository;

import com.sisgic.entity.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SyncStatusRepository extends JpaRepository<SyncStatus, String> {
    Optional<SyncStatus> findByJobKey(String jobKey);
}
