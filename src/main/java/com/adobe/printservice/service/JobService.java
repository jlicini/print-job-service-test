package com.adobe.printservice.service;

import com.adobe.printservice.exception.JobStateConflictException;
import com.adobe.printservice.exception.JobNotFoundException;
import com.adobe.printservice.exception.TemplateNotFoundException;
import com.adobe.printservice.mapper.JobMapper;
import com.adobe.printservice.model.Job;
import com.adobe.printservice.model.JobStatus;
import com.adobe.printservice.repository.JobRepository;
import com.adobe.printservice.repository.RenderTemplateRepository;
import com.adobe.printservice.dto.JobRequestDTO;
import com.adobe.printservice.dto.JobResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final RenderTemplateRepository renderTemplateRepository;
    private final JobMapper jobMapper;

    public JobService(JobRepository jobRepository, RenderTemplateRepository renderTemplateRepository, JobMapper jobMapper) {
        this.jobRepository = jobRepository;
        this.renderTemplateRepository = renderTemplateRepository;
        this.jobMapper = jobMapper;
    }

    @Transactional
    public JobResponseDTO createJob(JobRequestDTO request) {
        if (!renderTemplateRepository.existsById(request.templateId())) {
            throw new TemplateNotFoundException(request.templateId());
        }

        Job job = jobMapper.toEntity(request);
        Job savedJob = jobRepository.save(job);
        return jobMapper.toResponse(savedJob);
    }

    @Transactional(readOnly = true)
    public JobResponseDTO getJob(String id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));

        return jobMapper.toResponse(job);
    }

    @Transactional(readOnly = true)
    public List<JobResponseDTO> getJobs(JobStatus status) {
        List<Job> jobs = status == null
                ? jobRepository.findAll()
                : jobRepository.findByStatus(status);

        return jobs.stream()
                .map(jobMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public String getJobResult(String id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));

        if (job.getStatus() != JobStatus.DONE) {
            throw JobStateConflictException.cannotFetchResult(
                    job.getId(),
                    job.getStatus(),
                    job.getErrorMessage()
            );
        }

        return job.getResultContent();
    }

}
