package com.adobe.printservice.service.metric;

import com.adobe.printservice.dto.metrics.JobMetricsDTO;
import com.adobe.printservice.dto.metrics.MetricsResponseDTO;
import com.adobe.printservice.dto.metrics.TemplateMetricsDTO;
import com.adobe.printservice.event.JobCreatedEvent;
import com.adobe.printservice.event.JobStatusChangedEvent;
import com.adobe.printservice.model.JobStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.EnumMap;
import java.util.Map;

@Service
public class MetricsCacheService {

    private static final String JOB_STATUS_KEY = "metrics:jobs:status";
    private static final String TOTAL_ATTEMPTS_KEY = "metrics:jobs:attempts";
    private static final String RETRIED_JOBS_KEY = "metrics:jobs:retried";
    private static final String TEMPLATE_COUNT_KEY = "metrics:templates:count";

    private final StringRedisTemplate redisTemplate;
    private final boolean redisEnabled;
    private final Map<JobStatus, Long> memoryStatusCounts = new EnumMap<>(JobStatus.class);
    private long memoryTotalAttempts;
    private long memoryRetriedJobs;
    private long memoryTemplates;

    public MetricsCacheService(
            StringRedisTemplate redisTemplate,
            @Value("${metrics.cache.type:redis}") String cacheType
    ) {
        this.redisTemplate = redisTemplate;
        this.redisEnabled = "redis".equalsIgnoreCase(cacheType);
        initializeMemoryStatusCounts();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJobCreated(JobCreatedEvent event) {
        jobCreated();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(JobStatusChangedEvent event) {
        transition(event.previousStatus(), event.newStatus());

        if (event.attemptStarted()) {
            attemptStarted(event.firstRetry());
        }
    }

    private void jobCreated() {
        incrementStatus(JobStatus.QUEUED, 1);
    }

    private void transition(JobStatus from, JobStatus to) {
        if (from == to) {
            return;
        }

        incrementStatus(from, -1);
        incrementStatus(to, 1);
    }

    private void attemptStarted(boolean firstRetry) {
        if (redisEnabled) {
            redisTemplate.opsForValue().increment(TOTAL_ATTEMPTS_KEY);
        } else {
            memoryTotalAttempts++;
        }

        if (firstRetry) {
            if (redisEnabled) {
                redisTemplate.opsForValue().increment(RETRIED_JOBS_KEY);
            } else {
                memoryRetriedJobs++;
            }
        }
    }

    public void initialize(
            Map<JobStatus, Long> jobsByStatus,
            long totalAttempts,
            long retriedJobs,
            long templates
    ) {
        if (!redisEnabled) {
            initializeMemoryStatusCounts();
            memoryStatusCounts.putAll(jobsByStatus);
            memoryTotalAttempts = totalAttempts;
            memoryRetriedJobs = retriedJobs;
            memoryTemplates = templates;
            return;
        }

        jobsByStatus.forEach((status, total) ->
                redisTemplate.opsForHash().put(
                        JOB_STATUS_KEY,
                        status.name(),
                        total.toString()
                )
        );

        redisTemplate.opsForValue().set(
                TOTAL_ATTEMPTS_KEY,
                Long.toString(totalAttempts)
        );
        redisTemplate.opsForValue().set(
                RETRIED_JOBS_KEY,
                Long.toString(retriedJobs)
        );
        redisTemplate.opsForValue().set(
                TEMPLATE_COUNT_KEY,
                Long.toString(templates)
        );
    }

    public MetricsResponseDTO getMetrics() {
        if (!redisEnabled) {
            Map<JobStatus, Long> statuses = new EnumMap<>(memoryStatusCounts);
            return toResponse(statuses, memoryTotalAttempts, memoryRetriedJobs, memoryTemplates);
        }

        Map<JobStatus, Long> statuses = new EnumMap<>(JobStatus.class);

        for (JobStatus status : JobStatus.values()) {
            Object value = redisTemplate.opsForHash()
                    .get(JOB_STATUS_KEY, status.name());

            statuses.put(
                    status,
                    value == null ? 0L : Long.parseLong(value.toString())
            );
        }

        return toResponse(
                statuses,
                getLong(TOTAL_ATTEMPTS_KEY),
                getLong(RETRIED_JOBS_KEY),
                getLong(TEMPLATE_COUNT_KEY)
        );
    }

    private MetricsResponseDTO toResponse(
            Map<JobStatus, Long> statuses,
            long totalAttempts,
            long retriedJobs,
            long templates
    ) {
        long totalJobs = statuses.values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();

        JobMetricsDTO jobs = new JobMetricsDTO(
                totalJobs,
                statuses,
                totalAttempts,
                retriedJobs
        );

        return new MetricsResponseDTO(
                jobs,
                new TemplateMetricsDTO(templates)
        );
    }

    private void incrementStatus(JobStatus status, long amount) {
        if (redisEnabled) {
            redisTemplate.opsForHash().increment(JOB_STATUS_KEY, status.name(), amount);
        } else {
            memoryStatusCounts.put(status, memoryStatusCounts.getOrDefault(status, 0L) + amount);
        }
    }

    private long getLong(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? 0L : Long.parseLong(value);
    }

    private void initializeMemoryStatusCounts() {
        for (JobStatus status : JobStatus.values()) {
            memoryStatusCounts.put(status, 0L);
        }
    }
}
