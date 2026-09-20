package com.adobe.printservice.service.metric;

import com.adobe.printservice.dto.metrics.MetricsResponseDTO;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import com.adobe.printservice.repository.RenderTemplateRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
public class MetricsService {

    private final JobRepository jobRepository;
    private final RenderTemplateRepository renderTemplateRepository;
    private final MetricsCacheService metricsCacheService;

    public MetricsService(
            JobRepository jobRepository,
            RenderTemplateRepository renderTemplateRepository,
            MetricsCacheService metricsCacheService
    ) {
        this.jobRepository = jobRepository;
        this.renderTemplateRepository = renderTemplateRepository;
        this.metricsCacheService = metricsCacheService;
    }

    public MetricsResponseDTO metrics() {
        return metricsCacheService.getMetrics();
    }

    /**
     * Rebuilds the metrics cache from PostgreSQL so Redis starts from the persisted source of truth.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void refreshCacheFromDatabase() {
        Map<JobStatus, Long> jobsByStatus = initializeStatusCounts();

        jobRepository.countJobsByStatus().forEach(
                count -> jobsByStatus.put(count.status(), count.total())
        );

        Long totalAttempts = jobRepository.sumAttempts();

        metricsCacheService.initialize(
                jobsByStatus,
                totalAttempts == null ? 0 : totalAttempts,
                jobRepository.countByAttemptsGreaterThan(1),
                renderTemplateRepository.count()
        );
    }

    private Map<JobStatus, Long> initializeStatusCounts() {
        Map<JobStatus, Long> statusCounts = new EnumMap<>(JobStatus.class);
        for (JobStatus status : JobStatus.values()) {
            statusCounts.put(status, 0L);
        }
        return statusCounts;
    }

}
