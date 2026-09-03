package com.adobe.printservice.mapper;

import com.adobe.printservice.dto.JobRequestDTO;
import com.adobe.printservice.dto.JobResponseDTO;
import com.adobe.printservice.model.Job;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {

    public Job toEntity(JobRequestDTO dto) {
        Job job = new Job();
        job.setTemplateId(dto.templateId());
        job.setParameters(dto.parameters());
        return job;
    }

    public JobResponseDTO toResponse(Job job) {
        return new JobResponseDTO(
                job.getId(),
                job.getTemplateId(),
                job.getParameters(),
                job.getStatus(),
                job.getAttempts(),
                job.getErrorMessage(),
                job.getResultContent() != null,
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}