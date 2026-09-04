package com.adobe.printservice.service;

import com.adobe.printservice.exception.JobStateConflictException;
import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobAttemptResult;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class JobQueueService {

    private final JobRepository jobRepository;
    private final int maxAttempts;

    public JobQueueService(
            JobRepository jobRepository,
            @Value("${jobs.worker.max-attempts:3}") int maxAttempts
    ) {
        this.jobRepository = jobRepository;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public List<Job> claimNextJobs(int limit) {
        List<Job> jobs = jobRepository.findByStatusOrderByCreatedAtAsc(
                JobStatus.QUEUED,
                PageRequest.of(0, limit)
        );

        jobs.forEach(job -> {
            job.setStatus(JobStatus.PROCESSING);
            job.setAttempts(job.getAttempts() + 1);
            job.setErrorMessage(null);
            job.setUpdatedAt(Instant.now());
        });

        return jobs;
    }

    @Transactional
    public void completeAttempt(String jobId, JobAttemptResult result) {
        Job job = findProcessingJob(jobId);
        job.setUpdatedAt(Instant.now());

        if (result == JobAttemptResult.SUCCESS) {
            job.setStatus(JobStatus.DONE);
            job.setResultContent(result.message());
            job.setErrorMessage(null);
        } else if (job.getAttempts() >= maxAttempts) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(result.message());
        } else {
            job.setStatus(JobStatus.QUEUED);
            job.setErrorMessage(null);
        }
    }

    private Job findProcessingJob(String jobId) {
        return jobRepository.findByIdAndStatus(jobId, JobStatus.PROCESSING)
                .orElseThrow(() -> JobStateConflictException.requiresStatus(
                        jobId,
                        JobStatus.PROCESSING
                ));
    }
}
