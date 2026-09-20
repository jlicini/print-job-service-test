package com.adobe.printservice.service;

import com.adobe.printservice.config.JobWorkerProperties;
import com.adobe.printservice.exception.JobStateConflictException;
import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobAttemptResult;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Owns the complete worker lifecycle: claiming, rendering, retrying and recovering jobs.
 */
@Service
public class JobWorkerService {

    private final JobRepository jobRepository;
    private final int maxAttempts;
    private final Duration processingTimeout;
    private final Duration retryBackoff;

    public JobWorkerService(
            JobRepository jobRepository,
            JobWorkerProperties properties
    ) {
        this.jobRepository = jobRepository;
        this.maxAttempts = properties.getMaxAttempts();
        this.processingTimeout = properties.getProcessingTimeout();
        this.retryBackoff = properties.getRetryBackoff();
    }

    @Transactional
    public int recoverExpiredJobs(int limit) {
        Instant now = Instant.now();
        List<Job> jobs = jobRepository
                .findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                        JobStatus.PROCESSING,
                        now,
                        PageRequest.of(0, limit)
                );

        jobs.forEach(job -> {
            job.setUpdatedAt(now);

            if (job.getAttempts() >= maxAttempts) {
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage("Processing deadline expired");
                job.setScheduledAt(null);
            } else {
                job.setStatus(JobStatus.QUEUED);
                job.setErrorMessage(null);
                job.setScheduledAt(scheduledAt(now, job.getAttempts()));
            }
        });

        return jobs.size();
    }

    @Transactional
    public List<Job> claimNextJobs(int limit) {
        Instant now = Instant.now();
        List<Job> jobs = jobRepository.findByStatusAndScheduledAtLessThanEqualOrderByCreatedAtAsc(
                JobStatus.QUEUED,
                now,
                PageRequest.of(0, limit)
        );

        jobs.forEach(job -> {
            job.setStatus(JobStatus.PROCESSING);
            job.setAttempts(job.getAttempts() + 1);
            job.setErrorMessage(null);
            job.setScheduledAt(now.plus(processingTimeout));
            job.setUpdatedAt(now);
        });

        return jobs;
    }

    @Transactional
    public void completeAttempt(String jobId, int attempt, JobAttemptResult result) {
        Job job = findProcessingJob(jobId, attempt);
        Instant now = Instant.now();
        job.setUpdatedAt(now);

        if (result == JobAttemptResult.SUCCESS) {
            job.setStatus(JobStatus.DONE);
            job.setResultContent(result.message());
            job.setErrorMessage(null);
            job.setScheduledAt(null);
        } else if (job.getAttempts() >= maxAttempts) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(result.message());
            job.setScheduledAt(null);
        } else {
            job.setStatus(JobStatus.QUEUED);
            job.setErrorMessage(null);
            job.setScheduledAt(scheduledAt(now, job.getAttempts()));
        }
    }

    private Instant scheduledAt(Instant now, int attempts) {
        return now.plus(retryBackoff.multipliedBy(attempts));
    }

    private Job findProcessingJob(String jobId, int attempt) {
        return jobRepository.findByIdAndStatusAndAttempts(
                        jobId,
                        JobStatus.PROCESSING,
                        attempt
                )
                .orElseThrow(() -> JobStateConflictException.requiresStatus(
                        jobId,
                        JobStatus.PROCESSING
                ));
    }
}
