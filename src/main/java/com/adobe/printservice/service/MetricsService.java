package com.adobe.printservice.service;

import com.adobe.printservice.dto.metrics.JobMetricsDTO;
import com.adobe.printservice.dto.metrics.MetricsResponseDTO;
import com.adobe.printservice.dto.metrics.TemplateMetricsDTO;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import com.adobe.printservice.repository.RenderTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
public class MetricsService {

    private final JobRepository jobRepository;
    private final RenderTemplateRepository renderTemplateRepository;

    public MetricsService(
            JobRepository jobRepository,
            RenderTemplateRepository renderTemplateRepository
    ) {
        this.jobRepository = jobRepository;
        this.renderTemplateRepository = renderTemplateRepository;
    }

    @Transactional(readOnly = true)
    public MetricsResponseDTO metrics() {
        Map<JobStatus, Long> jobsByStatus = initializeStatusCounts();
        jobRepository.countJobsByStatus().forEach(
                count -> jobsByStatus.put(count.status(), count.total())
        );

        Long totalAttempts = jobRepository.sumAttempts();
        long totalJobs = jobsByStatus.values().stream().mapToLong(Long::longValue).sum();

        JobMetricsDTO jobMetrics = new JobMetricsDTO(
                totalJobs,
                jobsByStatus,
                totalAttempts == null ? 0 : totalAttempts,
                jobRepository.countByAttemptsGreaterThan(1)
        );

        TemplateMetricsDTO templateMetrics = new TemplateMetricsDTO(renderTemplateRepository.count());

        return new MetricsResponseDTO(jobMetrics, templateMetrics);
    }

    private Map<JobStatus, Long> initializeStatusCounts() {
        Map<JobStatus, Long> statusCounts = new EnumMap<>(JobStatus.class);
        for (JobStatus status : JobStatus.values()) {
            statusCounts.put(status, 0L);
        }
        return statusCounts;
    }

}
