package com.adobe.printservice.service;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobAttemptResult;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobQueueServiceTest {

    private static final int MAX_ATTEMPTS = 3;

    @Mock
    private JobRepository jobRepository;

    private JobQueueService jobQueueService;
    private Job queuedJob;
    private Job processingJob;

    @BeforeEach
    void setUp() {
        jobQueueService = new JobQueueService(jobRepository, MAX_ATTEMPTS);

        queuedJob = new Job();
        queuedJob.setStatus(JobStatus.QUEUED);
        queuedJob.setErrorMessage("Previous error");

        processingJob = new Job();
        processingJob.setStatus(JobStatus.PROCESSING);
        processingJob.setAttempts(1);
    }

    @Test
    void claimNextJobs_queuedJob_returnsProcessingJob() {
        when(jobRepository.findByStatusOrderByCreatedAtAsc(
                JobStatus.QUEUED,
                PageRequest.of(0, 1)
        )).thenReturn(List.of(queuedJob));

        List<Job> claimedJobs = jobQueueService.claimNextJobs(1);

        assertEquals(1, claimedJobs.size());
        assertEquals(JobStatus.PROCESSING, queuedJob.getStatus());
        assertEquals(1, queuedJob.getAttempts());
        assertNull(queuedJob.getErrorMessage());
        verify(jobRepository).findByStatusOrderByCreatedAtAsc(
                JobStatus.QUEUED,
                PageRequest.of(0, 1)
        );
    }

    @Test
    void completeAttempt_successfulRender_marksJobAsDone() {
        when(jobRepository.findByIdAndStatus(processingJob.getId(), JobStatus.PROCESSING))
                .thenReturn(Optional.of(processingJob));

        jobQueueService.completeAttempt(processingJob.getId(), JobAttemptResult.SUCCESS);

        assertEquals(JobStatus.DONE, processingJob.getStatus());
        assertEquals(JobAttemptResult.SUCCESS.message(), processingJob.getResultContent());
        assertNull(processingJob.getErrorMessage());
    }

    @Test
    void completeAttempt_transientFailure_returnsJobToQueue() {
        when(jobRepository.findByIdAndStatus(processingJob.getId(), JobStatus.PROCESSING))
                .thenReturn(Optional.of(processingJob));

        jobQueueService.completeAttempt(processingJob.getId(), JobAttemptResult.TRANSIENT_FAILURE);

        assertEquals(JobStatus.QUEUED, processingJob.getStatus());
        assertNull(processingJob.getErrorMessage());
    }

    @Test
    void completeAttempt_maxAttemptsReached_marksJobAsFailed() {
        processingJob.setAttempts(MAX_ATTEMPTS);
        when(jobRepository.findByIdAndStatus(processingJob.getId(), JobStatus.PROCESSING))
                .thenReturn(Optional.of(processingJob));

        jobQueueService.completeAttempt(processingJob.getId(), JobAttemptResult.TRANSIENT_FAILURE);

        assertEquals(JobStatus.FAILED, processingJob.getStatus());
        assertEquals(JobAttemptResult.TRANSIENT_FAILURE.message(), processingJob.getErrorMessage());
    }
}
