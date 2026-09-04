package com.adobe.printservice.repository;

import com.adobe.printservice.dto.JobStatusCountDTO;
import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, String> {
    List<Job> findByStatus(JobStatus status);

    @Query("SELECT j.status, COUNT(j) FROM Job j GROUP BY j.status")
    List<JobStatusCountDTO> countJobsByStatus();

    long countByAttemptsGreaterThan(int attempts);

    @Query("SELECT SUM(j.attempts) FROM Job j")
    Long sumAttempts();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Job> findByStatusOrderByCreatedAtAsc(JobStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Job> findByIdAndStatus(String id, JobStatus status);
}
