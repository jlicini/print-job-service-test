package com.adobe.printservice.service;

import com.adobe.printservice.config.JobWorkerProperties;
import com.adobe.printservice.exception.JobNotFoundException;
import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobAttemptResult;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import com.adobe.printservice.service.job.JobWorkerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobWorkerServiceTest {

    private static final int MAX_ATTEMPTS = 3;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private JobWorkerService jobWorkerService;
    private Job queuedJob;
    private Job processingJob;

    @BeforeEach
    void setUp() {
        JobWorkerProperties properties = new JobWorkerProperties();
        properties.setMaxAttempts(MAX_ATTEMPTS);
        properties.setProcessingTimeout(Duration.ofMinutes(10));
        properties.setRetryBackoff(Duration.ofMinutes(10));

        jobWorkerService = new JobWorkerService(
                jobRepository,
                properties,
                eventPublisher
        );

        queuedJob = new Job();
        queuedJob.setStatus(JobStatus.QUEUED);
        queuedJob.setErrorMessage("Previous error");

        processingJob = new Job();
        processingJob.setStatus(JobStatus.PROCESSING);
        processingJob.setAttempts(1);
    }

    @Test
    void claimNextJobs_queuedJob_returnsProcessingJob() {
        when(jobRepository.findByStatusAndScheduledAtLessThanEqualOrderByCreatedAtAsc(
                eq(JobStatus.QUEUED),
                any(Instant.class),
                eq(PageRequest.of(0, 1))
        )).thenReturn(List.of(queuedJob));

        List<Job> claimedJobs = jobWorkerService.claimNextJobs(1);

        assertEquals(1, claimedJobs.size());
        assertEquals(JobStatus.PROCESSING, queuedJob.getStatus());
        assertEquals(1, queuedJob.getAttempts());
        assertNull(queuedJob.getErrorMessage());
        assertNotNull(queuedJob.getScheduledAt());
        assertEquals(
                Duration.ofMinutes(10),
                Duration.between(queuedJob.getUpdatedAt(), queuedJob.getScheduledAt())
        );
        verify(jobRepository).findByStatusAndScheduledAtLessThanEqualOrderByCreatedAtAsc(
                eq(JobStatus.QUEUED),
                any(Instant.class),
                eq(PageRequest.of(0, 1))
        );
    }

    @Test
    void completeAttempt_successfulRender_marksJobAsDone() {
        when(jobRepository.findByIdAndStatusAndAttempts(
                processingJob.getId(),
                JobStatus.PROCESSING,
                processingJob.getAttempts()
        ))
                .thenReturn(Optional.of(processingJob));

        jobWorkerService.completeAttempt(
                processingJob.getId(),
                processingJob.getAttempts(),
                JobAttemptResult.SUCCESS
        );

        assertEquals(JobStatus.DONE, processingJob.getStatus());
        assertEquals(JobAttemptResult.SUCCESS.message(), processingJob.getResultContent());
        assertNull(processingJob.getErrorMessage());
        assertNull(processingJob.getScheduledAt());
    }

    @Test
    void completeAttempt_transientFailure_returnsJobToQueue() {
        when(jobRepository.findByIdAndStatusAndAttempts(
                processingJob.getId(),
                JobStatus.PROCESSING,
                processingJob.getAttempts()
        ))
                .thenReturn(Optional.of(processingJob));

        jobWorkerService.completeAttempt(
                processingJob.getId(),
                processingJob.getAttempts(),
                JobAttemptResult.TRANSIENT_FAILURE
        );

        assertEquals(JobStatus.QUEUED, processingJob.getStatus());
        assertNull(processingJob.getErrorMessage());
        assertEquals(
                Duration.ofMinutes(10),
                Duration.between(processingJob.getUpdatedAt(), processingJob.getScheduledAt())
        );
    }

    @Test
    void completeAttempt_secondFailure_schedulesTwentyMinuteBackoff() {
        processingJob.setAttempts(2);
        when(jobRepository.findByIdAndStatusAndAttempts(
                processingJob.getId(),
                JobStatus.PROCESSING,
                processingJob.getAttempts()
        )).thenReturn(Optional.of(processingJob));

        jobWorkerService.completeAttempt(
                processingJob.getId(),
                processingJob.getAttempts(),
                JobAttemptResult.TRANSIENT_FAILURE
        );

        assertEquals(JobStatus.QUEUED, processingJob.getStatus());
        assertEquals(
                Duration.ofMinutes(20),
                Duration.between(processingJob.getUpdatedAt(), processingJob.getScheduledAt())
        );
    }

    @Test
    void completeAttempt_maxAttemptsReached_marksJobAsFailed() {
        processingJob.setAttempts(MAX_ATTEMPTS);
        when(jobRepository.findByIdAndStatusAndAttempts(
                processingJob.getId(),
                JobStatus.PROCESSING,
                processingJob.getAttempts()
        ))
                .thenReturn(Optional.of(processingJob));

        jobWorkerService.completeAttempt(
                processingJob.getId(),
                processingJob.getAttempts(),
                JobAttemptResult.TRANSIENT_FAILURE
        );

        assertEquals(JobStatus.FAILED, processingJob.getStatus());
        assertEquals(JobAttemptResult.TRANSIENT_FAILURE.message(), processingJob.getErrorMessage());
        assertNull(processingJob.getScheduledAt());
    }

    @Test
    void recoverExpiredJobs_processingLeaseExpired_returnsJobToQueueWithBackoff() {
        when(jobRepository
                .findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                        eq(JobStatus.PROCESSING),
                        any(Instant.class),
                        eq(PageRequest.of(0, 1))
                ))
                .thenReturn(List.of(processingJob));

        int recoveredJobs = jobWorkerService.recoverExpiredJobs(1);

        assertEquals(1, recoveredJobs);
        assertEquals(JobStatus.QUEUED, processingJob.getStatus());
        assertEquals(
                Duration.ofMinutes(10),
                Duration.between(processingJob.getUpdatedAt(), processingJob.getScheduledAt())
        );
    }

    @Test
    void recoverExpiredJobs_maxAttemptsReached_marksJobAsFailed() {
        processingJob.setAttempts(MAX_ATTEMPTS);
        when(jobRepository
                .findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                        eq(JobStatus.PROCESSING),
                        any(Instant.class),
                        eq(PageRequest.of(0, 1))
                ))
                .thenReturn(List.of(processingJob));

        jobWorkerService.recoverExpiredJobs(1);

        assertEquals(JobStatus.FAILED, processingJob.getStatus());
        assertEquals("Processing deadline expired", processingJob.getErrorMessage());
        assertNull(processingJob.getScheduledAt());
    }

    @Test
    void completeAttempt_repositoryReturnsEmptyOptional_throwsJobNotFound() {
        String jobId = "job-123";
        int attempt = 1;
        when(jobRepository.findByIdAndStatusAndAttempts(
                jobId,
                JobStatus.PROCESSING,
                attempt
        ))
                .thenReturn(Optional.empty());

        JobNotFoundException exception = assertThrows(
                JobNotFoundException.class,
                () -> jobWorkerService.completeAttempt(
                        jobId,
                        attempt,
                        JobAttemptResult.SUCCESS
                )
        );

        assertEquals(
                "Job does not exist: job-123",
                exception.getMessage()
        );
        verify(jobRepository, times(1))
                .findByIdAndStatusAndAttempts(jobId, JobStatus.PROCESSING, attempt);
    }
}
