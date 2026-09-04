package com.adobe.printservice.service;

import com.adobe.printservice.model.JobAttemptResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class JobRenderService {

    private static final long RENDER_DELAY_MS = 500;
    private static final double FINAL_FAILURE_PROBABILITY = 0.10;

    private final double attemptFailureProbability;

    public JobRenderService(@Value("${jobs.worker.max-attempts:3}") int maxAttempts) {
        this.attemptFailureProbability = Math.pow(
                FINAL_FAILURE_PROBABILITY,
                1.0 / maxAttempts
        );
    }

    public JobAttemptResult render() {
        try {
            Thread.sleep(RENDER_DELAY_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return JobAttemptResult.INTERRUPTED;
        }

        return renderFails()
                ? JobAttemptResult.TRANSIENT_FAILURE
                : JobAttemptResult.SUCCESS;
    }

    protected boolean renderFails() {
        return ThreadLocalRandom.current().nextDouble() < attemptFailureProbability;
    }
}
