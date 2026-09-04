package com.adobe.printservice.worker;

import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobAttemptResult;
import com.adobe.printservice.service.JobQueueService;
import com.adobe.printservice.service.JobRenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobWorker {

    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);

    private final JobQueueService jobQueueService;
    private final JobRenderService jobRenderService;

    public JobWorker(
            JobQueueService jobQueueService,
            JobRenderService jobRenderService
    ) {
        this.jobQueueService = jobQueueService;
        this.jobRenderService = jobRenderService;
    }

    @Scheduled(
            fixedDelayString = "${jobs.worker.poll-delay-ms}",
            initialDelayString = "1000"
    )
    public void processNextJob() {
        jobQueueService.claimNextJob().ifPresent(this::process);
    }

    private void process(Job job) {
        log.debug("Processing job {} (attempt {})", job.getId(), job.getAttempts());

        JobAttemptResult result = jobRenderService.render();
        jobQueueService.completeAttempt(job.getId(), result);

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
