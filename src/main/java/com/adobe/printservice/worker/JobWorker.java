package com.adobe.printservice.worker;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobAttemptResult;
import com.adobe.printservice.service.JobWorkerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Looks for queued jobs and sends them to the worker thread pool.
 */
@Component
public class JobWorker {

    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);

    private final JobWorkerService jobWorkerService;
    private final JobRenderer jobRenderer;
    private final ThreadPoolTaskExecutor jobTaskExecutor;

    public JobWorker(
            JobWorkerService jobWorkerService,
            JobRenderer jobRenderer,
            ThreadPoolTaskExecutor jobTaskExecutor
    ) {
        this.jobWorkerService = jobWorkerService;
        this.jobRenderer = jobRenderer;
        this.jobTaskExecutor = jobTaskExecutor;
    }

    @Scheduled(
            fixedDelayString = "${jobs.worker.poll-delay-ms}",
            initialDelayString = "1000"
    )
    public void processJobs() {
        int recoveredJobs = jobWorkerService.recoverExpiredJobs(jobTaskExecutor.getMaxPoolSize());
        if (recoveredJobs > 0) {
            log.warn("Recovered {} jobs with an expired processing deadline", recoveredJobs);
        }

        jobWorkerService.claimNextJobs(jobTaskExecutor.getMaxPoolSize())
                .forEach(job -> jobTaskExecutor.execute(() -> process(job)));
    }

    private void process(Job job) {
        log.debug(
                "Thread {} processing job {} (attempt {})",
                Thread.currentThread().getName(),
                job.getId(),
                job.getAttempts()
        );

        JobAttemptResult result = jobRenderer.render();
        jobWorkerService.completeAttempt(job.getId(), job.getAttempts(), result);

        if (result == JobAttemptResult.SUCCESS) {
            log.debug("Job {} completed", job.getId());
        } else {
            log.warn(
                    "Job {} failed on attempt {}: {}",
                    job.getId(),
                    job.getAttempts(),
                    result.message()
            );
        }
    }

}
