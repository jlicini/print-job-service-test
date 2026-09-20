package com.adobe.printservice.worker;

import com.adobe.printservice.config.JobWorkerProperties;
import com.adobe.printservice.model.JobAttemptResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class JobRenderer {

    private static final long RENDER_DELAY_MS = 500;
    private static final double FINAL_FAILURE_PROBABILITY = 0.10;

    private final double attemptFailureProbability;

    public JobRenderer(JobWorkerProperties properties) {
        this.attemptFailureProbability = Math.pow(
                FINAL_FAILURE_PROBABILITY,
                1.0 / properties.getMaxAttempts()
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

    private boolean renderFails() {
        return ThreadLocalRandom.current().nextDouble() < attemptFailureProbability;
    }
}
