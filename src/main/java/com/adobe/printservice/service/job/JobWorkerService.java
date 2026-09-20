package com.adobe.printservice.service.job;

import com.adobe.printservice.config.JobWorkerProperties;
import com.adobe.printservice.event.JobStatusChangedEvent;
import com.adobe.printservice.exception.JobNotFoundException;
import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobAttemptResult;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    public JobWorkerService(
            JobRepository jobRepository,
            JobWorkerProperties properties,
            ApplicationEventPublisher eventPublisher
    ) {
        this.jobRepository = jobRepository;
        this.maxAttempts = properties.getMaxAttempts();
        this.processingTimeout = properties.getProcessingTimeout();
        this.retryBackoff = properties.getRetryBackoff();
        this.eventPublisher = eventPublisher;
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
            JobStatus previousStatus = job.getStatus();
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
            publishStatusChanged(previousStatus, job.getStatus(), false, false);
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
            int previousAttempts = job.getAttempts();
            JobStatus previousStatus = job.getStatus();
            job.setStatus(JobStatus.PROCESSING);
            job.setAttempts(previousAttempts + 1);
            job.setErrorMessage(null);
            job.setScheduledAt(now.plus(processingTimeout));
            job.setUpdatedAt(now);
            publishStatusChanged(previousStatus, job.getStatus(), true, previousAttempts == 1);
        });

        return jobs;
    }

    @Transactional
    public void completeAttempt(String jobId, int attempt, JobAttemptResult result) {
        Job job = findProcessingJob(jobId, attempt);
        JobStatus previousStatus = job.getStatus();
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
        publishStatusChanged(previousStatus, job.getStatus(), false, false);
    }

    private void publishStatusChanged(
            JobStatus previousStatus,
            JobStatus newStatus,
            boolean attemptStarted,
            boolean firstRetry
    ) {
        eventPublisher.publishEvent(new JobStatusChangedEvent(
                previousStatus,
                newStatus,
                attemptStarted,
                firstRetry
        ));
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
                .orElseThrow(() -> new JobNotFoundException(jobId));
    }
}
