package com.adobe.printservice.web;

import com.adobe.printservice.service.JobService;
import com.adobe.printservice.dto.JobRequestDTO;
import com.adobe.printservice.dto.JobResponseDTO;
import com.adobe.printservice.model.JobStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/jobs")
public class JobResource {

    private final JobService jobService;

    public JobResource(JobService jobService) {
        this.jobService = jobService;
    }

    @Operation(summary = "Create a new job")
    @PostMapping
    public ResponseEntity<JobResponseDTO> createJob(@Valid @RequestBody JobRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.createJob(request));
    }

    @Operation(summary = "Get job by ID")
    @GetMapping("/{id}")
    public ResponseEntity<JobResponseDTO> getJob(@PathVariable String id) {
        return ResponseEntity.ok(jobService.getJob(id));
    }

    @Operation(summary = "Fetch a completed job result")
    @GetMapping(value = "/{id}/result", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getJobResult(@PathVariable String id) {
        return ResponseEntity.ok(jobService.getJobResult(id));
    }

    @Operation(summary = "List jobs, optionally filtered by status")
    @GetMapping
    public List<JobResponseDTO> getJobs(@RequestParam(required = false) JobStatus status) {
        return jobService.getJobs(status);
    }
}
