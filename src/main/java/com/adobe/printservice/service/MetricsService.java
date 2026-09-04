package com.adobe.printservice.service;

import com.adobe.printservice.dto.metrics.JobMetricsDTO;
import com.adobe.printservice.dto.metrics.MetricsResponseDTO;
import com.adobe.printservice.dto.metrics.TemplateMetricsDTO;
import com.adobe.printservice.dto.metrics.WorkerMetricsDTO;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import com.adobe.printservice.repository.RenderTemplateRepository;
import com.adobe.printservice.repository.projection.JobStatusCount;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
public class MetricsService {

    private final JobRepository jobRepository;
    private final RenderTemplateRepository renderTemplateRepository;
    private final ThreadPoolTaskExecutor jobTaskExecutor;

    public MetricsService(
            JobRepository jobRepository,
            RenderTemplateRepository renderTemplateRepository,
            ThreadPoolTaskExecutor jobTaskExecutor
    ) {
        this.jobRepository = jobRepository;
        this.renderTemplateRepository = renderTemplateRepository;
        this.jobTaskExecutor = jobTaskExecutor;
    }

    @Transactional(readOnly = true)
    public MetricsResponseDTO metrics() {
        Map<JobStatus, Long> jobsByStatus = emptyStatusCounts();
        for (JobStatusCount statusCount : jobRepository.countJobsByStatus()) {
            jobsByStatus.put(statusCount.status(), statusCount.total());
        }

        Long totalAttempts = jobRepository.sumAttempts();
        long totalJobs = jobsByStatus.values().stream().mapToLong(Long::longValue).sum();

        return new MetricsResponseDTO(
                new JobMetricsDTO(
                        totalJobs,
                        jobsByStatus,
                        totalAttempts == null ? 0 : totalAttempts,
                        jobRepository.countByAttemptsGreaterThan(1)
                ),
                new TemplateMetricsDTO(renderTemplateRepository.count()),
                workerMetrics()
        );
    }

    private Map<JobStatus, Long> emptyStatusCounts() {
        Map<JobStatus, Long> statusCounts = new EnumMap<>(JobStatus.class);
        for (JobStatus status : JobStatus.values()) {
            statusCounts.put(status, 0L);
        }
        return statusCounts;
    }

    private WorkerMetricsDTO workerMetrics() {
        return new WorkerMetricsDTO(
                jobTaskExecutor.getMaxPoolSize(),
                jobTaskExecutor.getPoolSize(),
                jobTaskExecutor.getActiveCount(),
                jobTaskExecutor.getQueueSize()
        );
    }
}
